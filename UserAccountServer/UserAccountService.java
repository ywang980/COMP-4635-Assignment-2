package UserAccountServer;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * The UserAccountService interface defines the remote methods for managing user
 * accounts.
 */
public interface UserAccountService extends Remote {

    /**
     * Checks if a user is already registered
     *
     * @param username - The username to check for registration
     * @return - 1 if the user is registered and not currently logged in,
     *         - 2 if the user is not registered and not logged in.
     *         - 0 if the user is logged in.
     * @throws RemoteException - if there is an issue with remote communication.
     */
    int login(String username) throws RemoteException;

    /**
     * Logs out a user if they are currently logged in.
     *
     * @param username - The username of the account to log out.
     * @return - 1 if the user was logged out successfully.
     *         - 0 if the user was not logged in.
     * @throws RemoteException - if there is an issue with remote communication.
     */
    int logout(String username) throws RemoteException;

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
    String load(String username) throws RemoteException;

    /**
     * Saves user data associated with the specified username to a file.
     *
     * @param username - The username for which to save user data.
     * @param data     - The user data to save.
     * @return - 1 if the user data was saved successfully.
     *         - 0 if an error occurred while saving.
     * @throws RemoteException - if there is an issue with remote communication.
     */
    int save(String username, String data) throws RemoteException;
}