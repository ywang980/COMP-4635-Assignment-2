package GameServer;

import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;

public class Server {
    public static void main(String args[]) {
        try {
            ServerInterfaceImpl serverObject = new ServerInterfaceImpl();
            Registry registry = LocateRegistry.createRegistry(Constants.DEFAULT_RMI_PORT);
            registry.rebind("Server", serverObject);
            System.out.println("Listening for incoming requests...");
        } catch (Exception e) {
            System.err.println("Exception: " + e.toString());
            e.printStackTrace();
        }
    }
}