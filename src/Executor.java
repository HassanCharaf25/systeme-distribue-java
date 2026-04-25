import java.io.*;
import java.net.*;
import java.util.*;
import java.util.logging.*;

/**
 * Cette classe représente le Coordinateur (Executor) du système distribué.
 * Elle est responsable de la sélection et de l'exécution atomique des réactions chimiques
 * sur l'ensemble des machines participantes.
 *
 * Choix de conception et Architecture :
 * 
 * Protocole réseau : Agit en tant que Client TCP
 * Utilisation de DataInputStream et DataOutputStream pour envoyer les commandes (PREPARE, COMMIT, ABORT) et recevoir les votes
 *
 * Gestion de la concurrence : Conçu pour s'exécuter en tant que processus indépendant capable de tourner en parallèle d'autres exécuteurs pour simuler la charge
 *
 * Gestion des données :Maintien temporaire de l'état de la transaction (sockets verrouillées) durant le cycle de vie d'une réaction pour identifier les participants
 *
 * Atomicité : Implémentation stricte du protocole Two-Phase Commit (2PC) pour garantir que la transaction est soit totalement validée (COMMIT), soit totalement annulée (ABORT)
 *
 * @author CHARAF Hassan
 */

public class Executor {
    private static final Logger logger = Logger.getLogger(Executor.class.getName());
    private List<Reaction> reactions = new ArrayList<>();
    private List<String> machines;
    private Random rand = new Random();

    private int total = 0;
    private int committed = 0;

    public Executor(List<String> reactionStrings, List<String> machines) {
        for (String s : reactionStrings) {
            this.reactions.add(new Reaction(s));
        }
        this.machines = machines;
        setupLogger();
    }
    // configure le système de logs, écrit dans le terminal et dans des fichiers.log
    private void setupLogger() {
        try {
            LogManager.getLogManager().reset();
            logger.setLevel(Level.INFO);
            java.util.logging.Formatter fmt = new SimpleFormatter() {
                private static final String format = "[%1$tF %1$tT] [Executor] %2$s %n";
                @Override
                public synchronized String format(LogRecord lr) {
                    return String.format(format, new Date(lr.getMillis()), lr.getMessage());
                }
            };
            ConsoleHandler ch = new ConsoleHandler();
            ch.setFormatter(fmt);
            logger.addHandler(ch);
            FileHandler fh = new FileHandler("executor.log", true);
            fh.setFormatter(fmt);
            logger.addHandler(fh);
        } catch (IOException e) {
            System.err.println("Erreur logger executor: " + e.getMessage());
        }
    }

    public void start() {
        logger.info("Executor prêt. Cibles: " + machines);
        while (true) {
            Reaction r = reactions.get(rand.nextInt(reactions.size()));
            boolean success = execute2PC(r);
            
            total++;
            if (success) committed++;
            
            if (total % 10 == 0) 
                logger.info(String.format("STATS C/T: %d/%d (%.2f%%)", committed, total, (double)committed/total*100));

            try { Thread.sleep(500 + rand.nextInt(500)); } catch (InterruptedException e) {}
        }
    }

    private boolean execute2PC(Reaction reaction) {
        logger.info("--- Début Transaction: " + reaction + " ---");
        
        Map<String, Socket> lockedSockets = new HashMap<>();
        List<Socket> allSockets = new ArrayList<>();
        boolean phase1OK = true;

        for (Ressource input : reaction.getConsommables()) {
            boolean locked = false;
            for (String addr : machines) {
                if (locked) break;
                try {
                    String[] ipPort = addr.split(":");
                    Socket s = new Socket(ipPort[0], Integer.parseInt(ipPort[1]));
                    allSockets.add(s);
                    
                    DataOutputStream out = new DataOutputStream(s.getOutputStream());
                    DataInputStream in = new DataInputStream(s.getInputStream());

                    logger.info("Envoi PREPARE (" + input.getNom() + " x" + input.getQuantite() + ") vers " + addr);
                    
                    out.writeUTF("PREPARE " + input.getNom() + " " + input.getQuantite());
                    String resp = in.readUTF();

                    if ("YES".equals(resp)) {
                        lockedSockets.put(input.getNom(), s);
                        locked = true;
                        logger.info("-> Réponse YES de " + addr);
                    } else {
                        logger.warning("-> Réponse NO de " + addr);
                    }
                } catch (IOException e) { 
                    logger.warning("Erreur connexion vers " + addr);
                }
            }
            if (!locked) {
                phase1OK = false;
                break;
            }
        }

        try {
            if (phase1OK) {
                for (Ressource input : reaction.getConsommables()) {
                    Socket s = lockedSockets.get(input.getNom());
                    String target = s.getInetAddress().getHostAddress() + ":" + s.getPort();
                    logger.info("Envoi COMMIT CONSUME vers " + target);
                    
                    send(s, "COMMIT CONSUME " + input.getNom() + " " + input.getQuantite());
                }
                if (!allSockets.isEmpty()) {
                    Socket prodSocket = allSockets.get(0);
                    String target = prodSocket.getInetAddress().getHostAddress() + ":" + prodSocket.getPort();
                    for (Ressource output : reaction.getProduits()) {
                        logger.info("Envoi COMMIT PRODUCE vers " + target);
                        send(prodSocket, "COMMIT PRODUCE " + output.getNom() + " " + output.getQuantite());
                    }
                }
                logger.info("Transaction COMMITTED");
                closeAll(allSockets);
                return true;
            } else {
                logger.warning("Phase 1 échouée. Envoi ABORT général.");
                for (Socket s : lockedSockets.values()) {
                    send(s, "ABORT");
                }
                closeAll(allSockets);
                return false;
            }
        } catch (IOException e) {
            logger.severe("Crash Phase 2: " + e.getMessage());
            return false;
        }
    }

    private void send(Socket s, String msg) throws IOException {
        DataOutputStream out = new DataOutputStream(s.getOutputStream());
        DataInputStream in = new DataInputStream(s.getInputStream());
        out.writeUTF(msg);
        in.readUTF();
    }

    private void closeAll(List<Socket> sockets) {
        for (Socket s : sockets) try { s.close(); } catch (IOException e) {}
    }

    public static void main(String[] args) {
        List<String> reactions = new ArrayList<>();
        List<String> machines = new ArrayList<>();
        for (int i = 0; i < args.length; i++) {
            if ("--reaction".equals(args[i])) reactions.add(args[++i]);
            if ("--machine".equals(args[i])) machines.add(args[++i]);
        }
        new Executor(reactions, machines).start();
    }
}