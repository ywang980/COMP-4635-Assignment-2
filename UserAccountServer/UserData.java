package UserAccountServer;

import GameServer.GameState;

/**
 * Represents user data, including username, score, and the game state object.
 */
public class UserData {
    private final String username;
    private int score;
    private GameState gameState;

    /**
     * Constructs UserData object from a string containing user data.
     * 
     * @param data - The string containing user data.
     */
    public UserData(String data) {

        int usernameStartIndex = data.indexOf("Username;") + "Username;".length();
        int usernameEndIndex = data.indexOf("\n", usernameStartIndex);
        this.username = data.substring(usernameStartIndex, usernameEndIndex);

        int scoreStartIndex = data.indexOf("Score;") + "Score;".length();
        int scoreEndIndex = data.indexOf("\n", scoreStartIndex);
        this.score = Integer.parseInt(data.substring(scoreStartIndex, scoreEndIndex));

        int gameStateIndex = data.indexOf("\n", data.indexOf("\n", scoreEndIndex));

        if (data.contains("State;Play")) {
            this.gameState = new GameState(data.substring(gameStateIndex));
        } else {
            this.gameState = new GameState();
        }
    }

    /**
     * Constructs a new USerData object with default values.
     * 
     * @param username       - The username for the new UserData object.
     * @param defaultAccount - not used, exists to indicate that this is the
     *                       default, empty UserData constructor.
     */
    public UserData(String username, boolean defaultAccount) {
        this.username = username;
        this.score = 0;
        this.gameState = new GameState();
    }

    /**
     * Gets the username associated with this user data
     * 
     * @return - The username.
     */
    public String getUsername() {
        return this.username;
    }

    /**
     * Gets the score associated with this user data
     * 
     * @return - The score.
     */
    public int getScore() {
        return this.score;
    }

    /**
     * Increments the score associated with this user data by 1.
     */
    public void incrementScore() {
        this.score += 1;
    }

    /**
     * Gets the game state associated with this user data.
     * 
     * @return - The game state.
     */
    public GameState getGameState() {
        return this.gameState;
    }

    /**
     * Sets the game state associated with this user data.
     * 
     * @param gameState - The game state to set.
     */
    public void setGameState(GameState gameState) {
        this.gameState = gameState;
    }

    /**
     * Gets a string representation of the user data.
     * 
     * @return - A string containing the user data.
     */
    public String getUserDataString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Username;").append(this.username).append("\n");
        stringBuilder.append("Score;").append(this.score).append("\n");
        stringBuilder.append(this.gameState.getGameStateString());

        String userDataString = stringBuilder.toString();
        return userDataString;
    }
}