package tp01_disque;

import commun.*;
import java.io.File;
import java.nio.ByteBuffer;

public class DiskManagerTests {

    public static void main(String[] args) {
        System.out.println("=== Début des tests du DiskManager ===");

        String testDir = "./test_db_dir";
        int pageSize = 4096; // 4 Ko par page

        // Nettoyage préalable au cas où le dossier existe déjà
        cleanDirectory(new File(testDir));

        DiskManager dm = new DiskManager();

        try {
            // 1. Test de l'initialisation
            System.out.println("\n[TEST 1] Initialisation du DiskManager...");
            dm.Init(testDir, pageSize);
            System.out.println("-> OK : DiskManager initialisé avec succès.");
            System.out.println("   ActivePageCount = " + dm.GetActivePageCount() + " (attendu: 0)");
            System.out.println("   PageSize = " + dm.GetPageSize() + " (attendu: " + pageSize + ")");

            // 2. Test de l'allocation de pages
            System.out.println("\n[TEST 2] Allocation de pages...");
            IPageId p1 = dm.AllocPage();
            IPageId p2 = dm.AllocPage();
            System.out.println("-> OK : Pages allouées avec succès.");
            System.out.println("   ID page 1 = " + ((PageId) p1).getPageId() + " (attendu: 0)");
            System.out.println("   ID page 2 = " + ((PageId) p2).getPageId() + " (attendu: 1)");
            System.out.println("   ActivePageCount = " + dm.GetActivePageCount() + " (attendu: 2)");

            // 3. Test de l'écriture dans une page
            System.out.println("\n[TEST 3] Écriture dans la page p1...");
            ByteBuffer writeBuffer = ByteBuffer.allocate(pageSize);
            String message = "Bonjour, ceci est un test de base de données !";
            writeBuffer.put(message.getBytes());
            writeBuffer.flip(); // Préparer le buffer pour la lecture

            dm.WritePage(p1, writeBuffer);
            System.out.println("-> OK : Données écrites dans la page p1.");

            // 4. Test de la lecture de la page
            System.out.println("\n[TEST 4] Lecture de la page p1...");
            ByteBuffer readBuffer = ByteBuffer.allocate(pageSize);
            dm.ReadPage(p1, readBuffer);
            readBuffer.flip();

            byte[] readBytes = new byte[message.getBytes().length];
            readBuffer.get(readBytes);
            String messageLu = new String(readBytes);
            System.out.println("-> OK : Données lues depuis la page p1.");
            System.out.println("   Message lu : \"" + messageLu + "\"");

            // 5. Test de la sauvegarde (Save)
            System.out.println("\n[TEST 5] Sauvegarde (Save)...");
            dm.Save();
            System.out.println("-> OK : Sauvegarde effectuée sans erreur.");

            // 6. Test de la désallocation
            System.out.println("\n[TEST 6] Désallocation de la page p2...");
            dm.DeallocPage(p2);
            System.out.println("-> OK : Page p2 désallouée.");

            // 7. Test de réutilisation d'une page désallouée
            System.out.println("\n[TEST 7] Allocation d'une nouvelle page (doit réutiliser p2)...");
            IPageId p3 = dm.AllocPage();
            System.out.println("-> OK : Nouvelle page allouée.");
            System.out.println("   ID page 3 = " + ((PageId) p3).getPageId() + " (doit réutiliser l'ID 1)");

            System.out.println("\n=== TOUS LES TESTS SONT PASSÉS AVEC SUCCÈS ! ===");

        } catch (DiskManagerException e) {
            System.err.println("\n[ERREUR TEST] Une exception DiskManagerException a été levée : " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Nettoyage final du dossier de test
            cleanDirectory(new File(testDir));
        }
    }

    // Méthode utilitaire pour supprimer proprement le dossier de test
    private static void cleanDirectory(File dir) {
        if (dir.exists()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File f : files) {
                    f.delete();
                }
            }
            dir.delete();
        }
    }
}