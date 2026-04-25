import java.io.File;
import java.io.IOException;

/**
 * Classe de test principal.
 * Lance Machines et Executors via ProcessBuilder.
 * 
 * @author CHARAF Hassan
 */
public class Test {
    public static void main(String[] args) throws IOException, InterruptedException {
        String javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
        String cp = System.getProperty("java.class.path");

        System.out.println("  Compilation et Lancement du test distribué  ");
        //Machine
        ProcessBuilder pbM1 = new ProcessBuilder(javaBin, "-cp", cp, "Machine", 
            "--port", "6001", "--resource", "(A,20)", "--resource", "(B,10)");
        pbM1.inheritIO();
        Process m1 = pbM1.start();
        System.out.println("[Test] Machine 1 lancée sur 6001");

        ProcessBuilder pbM2 = new ProcessBuilder(javaBin, "-cp", cp, "Machine", 
            "--port", "6002", "--resource", "(C,20)");
        pbM2.inheritIO();
        Process m2 = pbM2.start();
        System.out.println("[Test] Machine 2 lancée sur 6002");

        Thread.sleep(1000);

        //Executor
        ProcessBuilder pbExec = new ProcessBuilder(javaBin, "-cp", cp, "Executor",
            "--reaction", "2A+1C->1B",
            "--machine", "127.0.0.1:6001",
            "--machine", "127.0.0.1:6002"
        );
        pbExec.inheritIO();
        Process exec = pbExec.start();
        System.out.println("[Test] Executor lancé. Transaction: 2A+1C->1B");

        // On laisse tourner pendant 10 sec avant d'arrêter le processus
        Thread.sleep(10000);

        System.out.println("  Arrêt du système  ");
        exec.destroy();
        m1.destroy();
        m2.destroy();
    }
}