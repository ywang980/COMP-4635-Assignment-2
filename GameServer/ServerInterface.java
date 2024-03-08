package GameServer;

import UserAccountServer.UserData;
import UserAccountServer.ActiveGameData;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ServerInterface extends Remote {
    int checkValidUser(String username) throws RemoteException;

    UserData validateUserData(String username) throws RemoteException;

    void saveGame(UserData userData) throws RemoteException;

    void logoutUser(String username) throws RemoteException;

    UserData processUserInput(UserData userData, String input) throws RemoteException;

    String processWordQuery(UserData userData, String input) throws RemoteException;

    ActiveGameData processPuzzleGuess(UserData userData, String input) throws RemoteException;
}