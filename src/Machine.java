import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;

/**
 * Cette classe représente un serveur dans le système distribué
 * 
 * Choix de conception et architecture :
 * 
 * Protocole réseau : Communication via Sockets TCP et utilisation de DataInputStream et DataOutputStream pour l'échange de messages textuels
 * 
 * Gestion de la concurrence : Utilisation de threads à l'aide de newCachedThreadPool() et non pas newFixedThreadPool() pour eviter les files d'attente en cas de nb executeurs > nb de pool fixé
 * 
 * Gestion des données : utilisation de ConcurrentHashMap pour garantir la Thread-Safety
 * 
 * Atomicité : Protection des réservations et consommations via synchronized pour éviter les races conditions
 * 
 * @author CHARAF Hassan
 */
public class Machine {
    private static final Logger logger = Logger.getLogger(Machine.class.getName());
    
    private int port;
    private final Map<String, Integer> stock = new ConcurrentHashMap<>();
    private final Map<String, Integer> lockedStock = new ConcurrentHashMap<>();

    public Machine(int port, Map<String, Integer> ressourcesInitiales) {
        this.port = port;
        this.stock.putAll(ressourcesInitiales);
        setupLogger();
    }
    // configure le système de logs, écrit dans le terminal et dans des fichiers.log
    private void setupLogger() {
        try {
            LogManager.getLogManager().reset();
            logger.setLevel(Level.INFO);
            java.util.logging.Formatter myFormatter = new SimpleFormatter() {
                private static final String format = "[%1$tF %1$tT] [Machine-%2$d] %3$s %n";
                @Override
                public synchronized String format(LogRecord lr) {
                    return String.format(format, new Date(lr.getMillis()), port, lr.getMessage());
                }
            };
            ConsoleHandler ch = new ConsoleHandler();
            ch.setFormatter(myFormatter);
            logger.addHandler(ch);
            FileHandler fh = new FileHandler("machine_" + port + ".log");
            fh.setFormatter(myFormatter);
            logger.addHandler(fh);
        } catch (IOException e) {
            System.err.println("Erreur création log: " + e.getMessage());
        }
    }
    // Démarre le serveur TCP et donne la tache de chaque connexion entrante à un thread
    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            logger.info("Démarrage sur le port " + port + ". Stock initial: " + stock);
            ExecutorService pool = Executors.newCachedThreadPool();
            
            while (true) {
                pool.execute(new ServerTask(serverSocket.accept()));
            }
        } catch (IOException e) {
            logger.severe("Erreur serveur: " + e.getMessage());
        }
    }

    private class ServerTask implements Runnable {
        private final Socket socket;
        public ServerTask(Socket s) { this.socket = s; }

        @Override
        public void run() {
            try (DataInputStream in = new DataInputStream(socket.getInputStream());
                 DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {

                String clientInfo = socket.getInetAddress().getHostAddress() + ":" + socket.getPort();
                List<Ressource> sessionLocks = new ArrayList<>();

                while (true) {
                    String line;
                    try {
                        line = in.readUTF();
                    } catch (EOFException e) {
                        break;
                    }

                    String[] parts = line.split(" ");
                    String cmd = parts[0];

                    synchronized (Machine.this) {
                        if ("PREPARE".equals(cmd)) {
                            String name = parts[1];
                            int qty = Integer.parseInt(parts[2]);
                            
                            int current = stock.getOrDefault(name, 0);
                            int locked = lockedStock.getOrDefault(name, 0);

                            if (current - locked >= qty) {
                                lockedStock.put(name, locked + qty);
                                sessionLocks.add(new Ressource(name, qty));
                                out.writeUTF("YES");
                                logger.info("PREPARE REÇU de " + clientInfo + " -> Locked " + qty + " " + name);
                            } else {
                                out.writeUTF("NO");
                                logger.warning("PREPARE ECHEC de " + clientInfo + " -> Manque de " + name);
                            }
                        } 
                        else if ("COMMIT".equals(cmd)) {
                            String sub = parts[1];
                            String name = parts[2];
                            int qty = Integer.parseInt(parts[3]);

                            if ("CONSUME".equals(sub)) {
                                stock.put(name, stock.get(name) - qty);
                                lockedStock.put(name, lockedStock.get(name) - qty);
                                logger.info("COMMIT CONSUME par " + clientInfo + " -> Consommé " + qty + " " + name + " (Reste: " + stock.get(name) + ")");
                            } else {
                                stock.put(name, stock.getOrDefault(name, 0) + qty);
                                logger.info("COMMIT PRODUCE par " + clientInfo + " -> Produit " + qty + " " + name + " (Total: " + stock.get(name) + ")");
                            }
                            sessionLocks.clear();
                            out.writeUTF("ACK");
                        } 
                        else if ("ABORT".equals(cmd)) {
                            for (Ressource r : sessionLocks) {
                                int l = lockedStock.get(r.getNom());
                                lockedStock.put(r.getNom(), l - r.getQuantite());
                            }
                            sessionLocks.clear();
                            logger.info("ABORT REÇU de " + clientInfo + " -> Verrous relâchés.");
                            out.writeUTF("ACK");
                        }
                    }
                }
            } catch (IOException e) {

            }
        }
    }
    // analyse les arguments de la ligne de commande pour démarrer la machine
    public static void main(String[] args) {
        int port = 0;
        Map<String, Integer> res = new HashMap<>();
        for (int i = 0; i < args.length; i++) {
            if ("--port".equals(args[i])) port = Integer.parseInt(args[++i]);
            if ("--resource".equals(args[i])) {
                String val = args[++i].replaceAll("[()]", "");
                String[] p = val.split(",");
                res.put(p[0].trim(), Integer.parseInt(p[1].trim()));
            }
        }
        new Machine(port, res).start();
    }
}