package DatabaseServer;

import java.sql.*;

public class SimpleSQLiteQuery {
    public static void main(String[] args) {
        String query = "SELECT * FROM word";

        // Use try-with-resources to manage database resources
        try (Connection c = DriverManager.getConnection("jdbc:sqlite:./DatabaseServer/data/wordDatabase.db");
                Statement stmt = c.createStatement();
                ResultSet rs = stmt.executeQuery(query)) {

            // Loop through the result set and print each word
            while (rs.next()) {
                String word = rs.getString("word");
                System.out.println(word);
            }
        } catch (SQLException e) {
            System.err.println("SQL Exception: " + e.getMessage());
        }
    }
}