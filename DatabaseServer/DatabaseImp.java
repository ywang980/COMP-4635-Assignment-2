package DatabaseServer;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.sql.*;

/**
 *
 * Implements the datbase logic
 */
public class DatabaseImp extends UnicastRemoteObject implements Database {

    private Connection c;

    public DatabaseImp() throws RemoteException, SQLException {
        super();
        establishDatabase();
    }

    /**
     *
     * Connects to SQL database
     * 
     * @throws SQLException
     */

    private void establishDatabase() throws SQLException {
        try {
            c = DriverManager.getConnection("jdbc:sqlite:./DatabaseServer/data/wordDatabase.db");
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Removes the specified word from the database.
     *
     * @param word - The word to be removed.
     */
    public void removeWord(String word) throws RemoteException, SQLException {
        String sql = "DELETE FROM word WHERE word = '" + word + "';";
        Statement stmt = c.createStatement();
        stmt.executeUpdate(sql);
    }

    /**
     * Searches for the specified word in the database.
     *
     * @param word The word to search for.
     * @return The word if found in the database, otherwise null.
     */
    public Boolean checkWord(String word) throws RemoteException, SQLException {

        String sql = "SELECT word FROM word WHERE word = '" + word + "' LIMIT 1;";
        Statement stmt = c.createStatement();
        ResultSet rs = stmt.executeQuery(sql);
        return rs.next();
    }

    /**
     * Adds the specified word to the database if it does not already exist.
     *
     * @param word The word to add to the database.
     */
    public void addWord(String word) throws RemoteException {
        String sqlInsert = "INSERT INTO word (word) VALUES (?)";
        try {
            PreparedStatement insertStmt = c.prepareStatement(sqlInsert);
            insertStmt.setString(1, word);
            insertStmt.executeUpdate();
            System.out.println("Word added successfully.");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    /**
     * Generates a random word from the database that contains the specified
     * substring.
     *
     * @param a - The substring to match in the generated word.
     * @return - A random word containing the specified substring, or an empty
     *         string if no such word is found.
     */
    public String randomWord(char a) throws RemoteException, SQLException {

        a = Character.toLowerCase(a);
        String sql = "SELECT word FROM word WHERE word like '%" + a + "%' ORDER BY RANDOM() LIMIT 1;";
        Statement stmt = c.createStatement();
        ResultSet rs = stmt.executeQuery(sql);
        if (rs.next()) {
            return rs.getString("word");
        } else {
            return ""; // Or handle the case where no word is found
        }
    }

    /**
     * Generates a random word from the database with the specified length.
     *
     * @param a - The length of the word to generate.
     * @return - A random word with the specified length, or an empty string if no
     *         such word is found.
     */
    public String randomWordLength(int a) throws RemoteException, SQLException {
        String sql = "SELECT word FROM word WHERE LENGTH(word) like '" + a + "' ORDER BY RANDOM() LIMIT 1;";
        Statement stmt = c.createStatement();
        ResultSet rs = stmt.executeQuery(sql);
        if (rs.next()) {
            return rs.getString("word");
        } else {
            return ""; // Or handle the case where no word is found
        }
    }
}
