package GameServer;

import java.rmi.Remote;
import java.rmi.RemoteException;

import UserAccountServer.UserData;
import UserAccountServer.ActiveGameData;

/**
 * The ServerInterface interface defines remote methods for server interactions.
 */
public interface ServerInterface extends Remote {

    /**
     * Checks the validity of a user.
     *
     * @param username - The username to check.
     * @return - 1 if the user is registered and not currently logged in,
     *         - 2 if the user is not registered and not logged in and is now
     *         registered and logged in,
     *         - 0 if the user is already logged in.
     * @throws RemoteException - if there is an issue with remote communication.
     */
    int checkValidUser(String username) throws RemoteException;

    /**
     * Fetches and validates user data associated with the specified username.
     *
     * @param username - The username for which to validate user data.
     * @return - The UserData associated with the specified username.
     * @throws RemoteException - if there is an issue with remote communication.
     */
    UserData validateUserData(String username) throws RemoteException;

    /**
     * Saves game data associated with the specified UserData.
     *
     * @param userData - The UserData containing the game data to save.
     * @throws RemoteException - if there is an issue with remote communication or
     *                         saving the game data.
     */
    void saveGame(UserData userData) throws RemoteException;

    /**
     * Logs out the specified user.
     *
     * @param username - The username of the user to log out.
     * @throws RemoteException - if there is an issue with remote communication or
     *                         logging out the user.
     */
    void logoutUser(String username) throws RemoteException;

    /**
     * Processes user input and performs actions based on the input.
     *
     * Details: user input interpreted as command-argument 2-tuple,
     * separated by a ';'. E.g., Add;dog.
     *
     * @param userData - The UserData object representing the user's data.
     * @param input    - The user input to process.
     * @return - The updated UserData object after processing the input.
     * @throws RemoteException - if there is an issue with remote communication or
     *                         processing the input.
     */
    UserData processUserInput(UserData userData, String input) throws RemoteException;

    /**
     * Processes a word query to check if the word is in the database or the puzzle
     * word list.
     *
     * @param userData - The UserData object representing the user's data.
     * @param input    - The word query input to process.
     * @return - A message indicating whether the word is found in the database or
     *         the puzzle word list.
     * @throws RemoteException - if there is an issue with remote communication or
     *                         processing the query.
     */
    String processWordQuery(UserData userData, String input) throws RemoteException;

    /**
     * Processes a user's guess for the puzzle.
     *
     * @param userData - The UserData object representing the user's data.
     * @param input    - The user's guess input to process.
     * @return - An ActiveGameData object containing updated user data and game
     *         status.
     * @throws RemoteException - if there is an issue with remote communication or
     *                         processing the guess.
     */
    ActiveGameData processPuzzleGuess(UserData userData, String input) throws RemoteException;
}