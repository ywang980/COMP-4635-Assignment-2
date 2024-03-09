package DatabaseServer;
import java.rmi.*;



public class testClient {



    private static final String SERVERHOST = "localhost";

    public static void main(String[] args) {
        try {
            //Obtain a reference to the object from the registry
            Database database = (Database)Naming.lookup("rmi://localhost:6999/DatabaseService");
            // Under the hood when the reference to the remote object is obtained
            // Code for the client side stub is received and now the stub on this
            // machine is actually handling the communication with the remote object on
            // on the server.

            //Use the above reference to invoke the remote object�s method...
            database.removeWord("efefqef");
            System.out.println("Message received: " + database.randomWord('k'));
        } catch(Exception ex) {
            System.out.println(ex);
        }
}}
