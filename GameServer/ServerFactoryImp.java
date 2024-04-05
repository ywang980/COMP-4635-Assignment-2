package GameServer;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

/**
 *
 * This generates game servers to deliver to clients.
 */
public class ServerFactoryImp extends UnicastRemoteObject implements ServerFactory {
    protected ServerFactoryImp() throws RemoteException {
        super();
    }

    @Override
    public ServerInterface createServer(int sequence) throws RemoteException {
        return new ServerInterfaceImpl(sequence);
    }
}