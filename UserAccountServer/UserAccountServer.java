package UserAccountServer;

import java.io.*;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

import GameServer.Constants;

/**
 * Represents a server managing user accounts and handling client interactions.
 */
public class UserAccountServer extends UnicastRemoteObject implements UserAccountService {

    private static List<String> userAccounts;
    private static Set<String> loggedInUsers;

    /**
     * The main method is the entry point of the UserAccountServer application.
     * It initializes the RMI Registry, creates an instance of UserAccountService,
     * and binds it to the Registry.
     *
     * @param args - The command-line arguments (not used).
     */
    public static void main(String[] args) {
        try {
            LocateRegistry.createRegistry(Constants.UAS_PORT);

            // Now get the registry reference
            Registry registry = LocateRegistry.getRegistry("localhost", Constants.UAS_PORT);

            UserAccountService userAccountService = new UserAccountServer();
            registry.rebind("UserAccountService", userAccountService);

            System.out.println("UserAccountServer is running...");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Constructs a UserAccountServer instance.
     * This constructor initializes the server by loading existing user accounts
     * and creating an empty set for logged-in users.
     *
     * @throws RemoteException - if there is an issue with remote communication.
     */
    public UserAccountServer() throws RemoteException {
        super();
        loadUserAccounts();
        loggedInUsers = new HashSet<>();
    }

    /**
     * Loads user accounts from the file into the userAccounts list and populates
     * the loggedInUsers set.
     * User accounts are loaded from the specified directory containing text files
     * with user data.
     */
    private static void loadUserAccounts() {
        userAccounts = new ArrayList<>();

        File directory = new File(Constants.USER_DATA_DIRECTORY);
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile() && file.getName().endsWith(".txt")) {
                    userAccounts.add(file.getName().replace(".txt", ""));
                }
            }
        }
    }

    /**
     * Checks if a user is already registered
     *
     * @param username - The username to check for registration
     * @return - 1 if the user is registered and not currently logged in,
     *         - 2 if the user is not registered and not logged in.
     *         - 0 if the user is logged in.
     * @throws RemoteException - if there is an issue with remote communication.
     */
    public synchronized int login(String username) throws RemoteException {
        if (userAccounts.contains((username.trim())) && !loggedInUsers.contains(username.trim())) {
            loggedInUsers.add(username);
            return 1;
        } else if (!userAccounts.contains(username.trim()) && !loggedInUsers.contains(username.trim())) {
            loggedInUsers.add(username);
            userAccounts.add(username);
            return 2;
        } else {
            return 0;
        }
    }

    /**
     * Logs out a user if they are currently logged in.
     * 
     * @param username - The username of the account to log out.
     * @return - 1 if the user was logged out successfully.
     *         - 0 if the user was not logged in.
     * @throws RemoteException - if there is an issue with remote communication.
     */
    public synchronized int logout(String username) throws RemoteException {
        if (loggedInUsers.contains(username.trim())) {
            loggedInUsers.remove(username.trim());
            return 1;
        } else {
            return 0;
        }
    }

    /**
     * Loads user data from the file associated with the specified username.
     * If the file does not exist, a new file is created with default user data.
     * 
     * @param username - The username for which to load user data.
     * @return - A string containing the user data loaded from the file, or default
     *         user data if the file is newly created.
     * @throws RemoteException - If an I/O error occurs while creating the file or
     *                         reading from it.
     */
    public synchronized String load(String username) throws RemoteException {
        String filePath = Constants.USER_DATA_DIRECTORY + username + ".txt";
        File userDatafile = new File(filePath);
        try {
            if (!userDatafile.exists()) {
                userDatafile.createNewFile();
                UserData userData = new UserData(username, true);
                String userDataString = userData.getUserDataString();
                save(username, userDataString);
                return userDataString;
            }
        } catch (IOException e) {
            throw new RemoteException(Constants.CANT_CREATE_USER_FILE);
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(userDatafile))) {
            StringBuilder userDataBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                userDataBuilder.append(line).append("\n");
            }
            return userDataBuilder.toString();
        } catch (IOException e) {
            throw new RemoteException("Failed to read user file.", e);
        }
    }

    /**
     * Saves user data associated with the specified username to a file.
     * 
     * @param username - The username for which to save user data.
     * @param data     - The user data to save.
     * @return - 1 if the user data was saved successfully.
     *         - 0 if an error occurred while saving.
     * @throws RemoteException - if there is an issue with remote communication.
     */
    public synchronized int save(String username, String data) throws RemoteException {
        File userDataFile = new File(Constants.USER_DATA_DIRECTORY +
                username + ".txt");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(userDataFile))) {
            writer.write(data);
            return 1;
        } catch (IOException e) {
            e.printStackTrace();
            return 0;
        }
    }
}