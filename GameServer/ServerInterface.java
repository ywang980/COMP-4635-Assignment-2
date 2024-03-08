package GameServer;

import java.rmi.Remote;
import java.rmi.RemoteException;

import UserAccountServer.UserData;
import UserAccountServer.ActiveGameData;

public interface ServerInterface extends Remote {
    int checkValidUser(String username) throws RemoteException;

    UserData validateUserData(String username) throws RemoteException;

    void saveGame(UserData userData) throws RemoteException;

    void logoutUser(String username) throws RemoteException;

    UserData processUserInput(UserData userData, String input) throws RemoteException;

    String processWordQuery(UserData userData, String input) throws RemoteException;

    ActiveGameData processPuzzleGuess(UserData userData, String input) throws RemoteException;
}