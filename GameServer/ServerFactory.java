package GameServer;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ServerFactory extends Remote {
    ServerInterface createServer(int sequence) throws RemoteException;
}