package GameServer;

import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;

/**
 * The Server class contains the main method to start the server.
 */
public class Server {

    /**
     * Main method to start the server.
     *
     * @param args - Command-line arguments (not used).
     */
    public static void main(String args[]) {
        try {
            ServerInterfaceImpl serverObject = new ServerInterfaceImpl();
            Registry registry = LocateRegistry.createRegistry(Constants.GAME_SERVER_PORT);
            registry.rebind("Server", serverObject);
            System.out.println("Listening for incoming requests...");
        } catch (Exception e) {
            System.err.println("Exception: " + e.toString());
            e.printStackTrace();
        }
    }
}