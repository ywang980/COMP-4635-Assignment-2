package DatabaseServer;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;


/**
 *
 *
 * Establishes the database and registers it
 */
public class DatabaseServer {
    public static void main(String[] args) {
        try {
            LocateRegistry.createRegistry(6999);

            // Now get the registry reference
            Registry registry = LocateRegistry.getRegistry("localhost", 6999);

            DatabaseImp database = new DatabaseImp();
            registry.rebind("DatabaseService", database);

            System.out.println("RMI server is running...");
        } catch (Exception e) {
            System.err.println("Server exception: " + e.toString());
            e.printStackTrace();
        }
    }
}