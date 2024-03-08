package Temp;

import GameServer.Constants;
import GameServer.ServerInterface;
import UserAccountServer.UserData;
import UserAccountServer.ActiveGameData;

import java.io.*;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Scanner;

public class Client {

    public static void main(String[] args) {
        try {
            Registry registry = LocateRegistry.getRegistry("localhost", Constants.DEFAULT_RMI_PORT);
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
            e.printStackTrace();
        }
    }

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

    private static void serveUser(ServerInterface server, UserData userData) {
        Scanner scanner = new Scanner(System.in);
        String input;

        // Sentinel loop for the user menu
        do {
            System.out.println("\nUser: " + userData.getUsername());
            System.out.println("Score: " + userData.getScore());
            System.out.println(Constants.USER_MENU);

            input = scanner.nextLine().trim();
            try {
                if (input.equals(Constants.EXIT_CODE)) {
                    userData.getGameState().setState(Constants.IDLE_STATE);
                    server.saveGame(userData);
                    break;
                }
                userData = server.processUserInput(userData, input);
                if (userData.getGameState().getState().equals(Constants.PLAY_STATE)) {
                    playGame(server, userData);
                }
            } catch (RemoteException e) {
                handleError(server, userData, e);
            }
        } while (true);
    }

    private static void playGame(ServerInterface server, UserData userData) {
        Scanner scanner = new Scanner(System.in);
        ActiveGameData activeGameData = new ActiveGameData(userData, true, "");
        String input;

        while (activeGameData.getGameStatus()) {
            System.out.print("\n" + userData.getGameState().getPuzzle().getPuzzleString());
            System.out.println(Constants.GAME_MENU);
            System.out.println("Attempts remaining: " + userData.getGameState().getAttempts());

            input = scanner.nextLine();

            try {
                if (input.equals(Constants.SAVE_CODE)) {
                    server.saveGame(userData);
                    break;
                } else if (input.matches(Constants.NO_SPECIAL_CHAR_REGEX)) {
                    System.out.println("\nInvalid guess: " + input + ". Try again.");
                    continue;
                } else if (input.toCharArray()[0] == '?') {
                    System.out.println(server.processWordQuery(userData, input.substring(1)));
                    continue;
                } else {
                    activeGameData = server.processPuzzleGuess(userData, input);
                    userData = activeGameData.getUserData();
                    System.out.println(activeGameData.getMessage());
                    activeGameData.setMessage("");
                }
            } catch (RemoteException e) {
                // activeGameData.setGameStatus(true);
                handleError(server, userData, e);
            }
        }
        try{
            server.saveGame(userData);
        }
        catch (RemoteException e){
            System.out.println(e.getMessage());
        }

    }

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