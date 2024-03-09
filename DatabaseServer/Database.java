package DatabaseServer;

import java.sql.*;
import java.rmi.Remote;


import java.rmi.RemoteException;



public interface Database extends Remote {



    public void removeWord(String word) throws RemoteException, SQLException;

    public Boolean checkWord(String word) throws RemoteException, SQLException;
    public void addWord(String word) throws RemoteException;

    public String randomWord(char a) throws RemoteException, SQLException;

    public String randomWordLength(int a) throws RemoteException, SQLException;


}
