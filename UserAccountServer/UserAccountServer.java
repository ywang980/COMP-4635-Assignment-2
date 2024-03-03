package UserAccountServer;

import GameServer.Constants;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Represents a server managing user accounts and handling client interactions.
 */
public class UserAccountServer {

    private static final Integer THREAD_COUNT = 20;
    private static List<String> userAccounts;
    private static Set<String> loggedInUsers;

    /**
     * Static initializer block to load user accounts from file.
     */
    static {
        loadUserAccounts();
        loggedInUsers = new HashSet<>();
    }

    /**
     * Main entry point for running the UserAccountServer.
     * Starts the server on the specified port and accepts incoming connections.
     */
    public static void main(String[] args) {

        int port = Constants.UAS_PORT;

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("UserAccountServer is running...");
            ExecutorService threadPool = Executors.newFixedThreadPool(THREAD_COUNT);
            while (true) {
                Socket socket = serverSocket.accept();
                // System.out.println("Connection established with game server.");
                threadPool.submit(() -> handleConnection(socket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Loads user accounts from the file into the userAccounts list and populates
     * the loggedInUsers set.
     * User accounts are loaded from the specified directory containing text files
     * with user data.
     */
    private static void loadUserAccounts() {
        userAccounts = new ArrayList<>();
        // loggedInUsers = new HashSet<>();

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
     *         - 0 if the user is not logged in and not registered.
     */
    private static synchronized int login(String username) {
        loadUserAccounts();
        if (userAccounts.contains(username.trim()) &&
                !loggedInUsers.contains(username.trim())) {
            loggedInUsers.add(username);
            return 1;
        } else if (!userAccounts.contains(username.trim()) &&
                !loggedInUsers.contains(username.trim())) {
            loggedInUsers.add(username);
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
     */
    private static synchronized int logout(String username) {
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
     *         user data if
     *         the file is newly created.
     * @throws IOException - If an I/O error occurs while creating the file or
     *                     reading from it.
     */
    private static synchronized String load(String username) throws IOException {
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
            throw new IOException(Constants.CANT_CREATE_USER_FILE);
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(userDatafile))) {
            StringBuilder userDataBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                userDataBuilder.append(line).append("\n");
            }
            return userDataBuilder.toString();
        }
    }

    /**
     * Saves user data associated with the specified username to a file.
     * 
     * @param username - The username for which to save user data.
     * @param data     - The user data to save.
     * @return - 1 if the user data was saved successfully.
     *         - 0 if an error occurred while saving.
     */
    private static synchronized int save(String username, String data) {
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

    /**
     * Handles a connection with a client socket by performing requested operations
     * such as login,
     * logout, load, or save.
     * 
     * @param socket - The socket representing the connection with the client.
     */
    private static void handleConnection(Socket socket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))) {

            int result = -1;
            String stringResult = "";

            String input = in.readLine();
            String[] parts = input.split(";");
            if (parts.length == 2) {
                String operation = parts[0].trim();
                String username = parts[1].trim();

                switch (operation) {
                    case "login" -> result = login(username);
                    case "logout" -> result = logout(username);
                    case "load" -> stringResult = load(username);
                    case "save" -> {
                        StringBuilder dataBuilder = new StringBuilder();
                        String line;
                        while ((line = in.readLine()) != null && !line.isEmpty()) {
                            dataBuilder.append(line).append("\n");
                        }
                        result = save(username, dataBuilder.toString());
                    }
                }

                if (result != -1) {
                    out.write(String.valueOf(result));
                } else {
                    out.write(stringResult);
                }
                out.newLine();
                out.flush();
            }
        } catch (SocketException e) {
            System.out.println("Connection closed");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}