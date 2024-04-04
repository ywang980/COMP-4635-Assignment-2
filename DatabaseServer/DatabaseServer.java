package DatabaseServer;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import GameServer.Constants;

/**
 * Establishes the database and registers it
 */
public class DatabaseServer {
    public static void main(String[] args) {
        try {
            LocateRegistry.createRegistry(Constants.WDBS_PORT);
            Registry registry = LocateRegistry.getRegistry("localhost", Constants.WDBS_PORT);

            DatabaseImp database = new DatabaseImp();
            registry.rebind("DatabaseService", database);

            System.out.println("RMI server is running...");
        } catch (Exception e) {
            System.err.println("Server exception: " + e.toString());
        }
    }
}