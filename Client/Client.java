package Client;

import java.io.*;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Scanner;

import GameServer.Constants;
import GameServer.Server;
import GameServer.ServerInterface;
import UserAccountServer.UserData;
import UserAccountServer.ActiveGameData;

/**
 * The Client.
 */
public class Client {

    /**
     * Main method to start the client.
     *
     * @param args - Command-line arguments (not used).
     */
    public static void main(String[] args) {
        try {
            Registry registry = LocateRegistry.getRegistry("localhost", Constants.GAME_SERVER_PORT);
            ServerInterface server = (ServerInterface) registry.lookup("Server");
            String username = validateUserName(server);

            try {
                UserData userData = server.validateUserData(username);
                if (userData != null) {
                    System.out.println("Client: '" + userData.getUsername() + "' connected.");
                    serveUser(server, userData);
                }
            } catch (RemoteException e) {
                System.out.println(e.getMessage());
            } finally {
                if (username != null) {
                    server.logoutUser(username);
                }
                System.out.println("Connection successfully closed.");
            }
        } catch (Exception e) {
            System.out.println("Exception: " + e.getMessage());
            System.out.println("Connection error. Process will now terminate.");
        }
    }

    /**
     * Validates the username entered by the user.
     * Will repeatedly prompt user until valid username is provided.
     *
     * @param server - The ServerInterface object for server communication.
     * @return - The validated username.
     */
    private static String validateUserName(ServerInterface server) {
        Scanner scanner = new Scanner(System.in);
        String username = "";
        int loginResult;

        while (true) {
            try {
                System.out.println("\nWelcome to the crossword puzzle game. Please enter your username.");
                username = scanner.nextLine();
                loginResult = server.checkValidUser(username);
                break;
            } catch (RemoteException e) {
                System.out.println(e.getMessage());
            }
        }

        if (loginResult == 1) {
            System.out.println("\nLogging in as: " + username);
        } else {
            System.out.println("\nCreating new account: " + username);
        }
        return username;
    }

    /**
     * Serves the user by providing a menu and processing user input.
     *
     * @param server   - The ServerInterface object for server communication.
     * @param userData - The UserData object representing the user's data.
     */
    private static void serveUser(ServerInterface server, UserData userData) {
        Scanner scanner = new Scanner(System.in);
        String input;

        // Sentinel loop for the user menu
        do {
            // Print user menu
            System.out.println("\nUser: " + userData.getUsername());
            System.out.println("Score: " + userData.getScore());
            System.out.println(Constants.USER_MENU);

            input = scanner.nextLine().trim();
            try {
                // Save and exit if user input is "*Exit*""
                if (input.equals(Constants.EXIT_CODE)) {
                    userData.getGameState().setState(Constants.IDLE_STATE);
                    server.saveGame(userData);
                    break;
                }

                // Process user input, and proceed to gameplay menu if appropriate (command is
                // New Game or Continue)
                userData = server.processUserInput(userData, input);
                if (userData.getGameState().getState().equals(Constants.PLAY_STATE)) {
                    userData = playGame(server, userData);
                }
            } catch (RemoteException e) {
                handleError(server, userData, e);
                if (e.getMessage().contains("Connection refused")) {
                    break;
                }
            }
        } while (true);
    }

    /**
     * Plays the game by processing user input and updating the game state.
     *
     * Details: user input is a string which may not contain any of the following:
     * '+', '-', '.'
     * 
     * If string is 1 character, it is interpreted as a letter guess.
     * If string is 2+ characters, it is interpreted as a word guess.
     * If string begins with a '?', it is interpreted as a database query.
     * 
     * @param server   - The ServerInterface object for server communication.
     * @param userData - The UserData object representing the user's data.
     * @return - The UserData object after playing the game.
     */
    private static UserData playGame(ServerInterface server, UserData userData) {
        Scanner scanner = new Scanner(System.in);
        ActiveGameData activeGameData = new ActiveGameData(userData, true, "");
        String input;

        while (activeGameData.getGameStatus()) {
            System.out.print("\n" + userData.getGameState().getPuzzle().getPuzzleString());
            System.out.println(Constants.GAME_MENU);
            System.out.println("Attempts remaining: " + userData.getGameState().getAttempts());
            System.out.println("Previous guesses: " + userData.getGameState().listGuesses());

            input = scanner.nextLine();

            try {
                // Save and return to main menu if user input is "*Save*""
                if (input.equals(Constants.SAVE_CODE)) {
                    break;
                } else if (input.matches(Constants.NO_SPECIAL_CHAR_REGEX)) {
                    System.out.println("\nInvalid guess: " + input + ". Try again.");
                    continue;
                } else if (input.toCharArray()[0] == '?') {
                    System.out.println(server.processWordQuery(userData, input.substring(1)));
                    continue;
                } else {
                    if (userData.getGameState().checkUniqueGuess(input)) {
                        activeGameData = server.processPuzzleGuess(userData, input);
                        userData = activeGameData.getUserData();
                        System.out.println(activeGameData.getMessage());
                        activeGameData.setMessage("");
                        server.saveGame(userData);
                    } else
                        System.out.println("Already guessed that!");
                }
            } catch (RemoteException e) {
                handleError(server, userData, e);
                if (e.getMessage().contains("Connection refused")) {
                    break;
                }
            }
        }

        try {
            if (!activeGameData.getGameStatus()) {
                userData.getGameState().resetPuzzle();
            }

            server.saveGame(userData);
        } catch (RemoteException e) {
            handleError(server, userData, e);
        }
        return userData;
    }

    /**
     * Handles errors that occur during client-server communication.
     * Attempts to save user data if possible.
     *
     * @param server   - The ServerInterface object for server communication.
     * @param userData - The UserData object representing the user's data.
     * @param e        - The Exception that occurred.
     */
    private static void handleError(ServerInterface server, UserData userData, Exception e) {
        System.out.println("\nError: " + (e.getMessage()));
        try {
            userData.getGameState().setState(Constants.IDLE_STATE);
            server.saveGame(userData);
        } catch (IOException saveError) {
            if (!e.getMessage().equals(Constants.COULD_NOT_SAVE))
                System.out.println(Constants.COULD_NOT_SAVE);
        }
    }
}