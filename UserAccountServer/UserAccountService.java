package UserAccountServer;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface UserAccountService extends Remote {
    int login(String username) throws RemoteException;
    int logout(String username) throws RemoteException;
    String load(String username) throws RemoteException;
    int save(String username, String data) throws RemoteException;
}
