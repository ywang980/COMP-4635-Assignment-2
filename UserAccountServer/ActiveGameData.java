package UserAccountServer;

import java.io.Serializable;

/**
 * The ActiveGameData class represents data associated with an active game session.
 */
public class ActiveGameData implements Serializable{
    private UserData userData;
    private boolean gameStatus;
    private String message;

    /**
     * Constructs an ActiveGameData object with the specified user data, game status, and message.
     *
     * @param userData - The UserData associated with the game session.
     * @param gameStatus - The status of the game session.
     * @param message - The message associated with the game session.
     */
    public ActiveGameData(UserData userData, boolean gameStatus, String message) {
        this.userData = userData;
        this.gameStatus = gameStatus;
        this.message = message;
    }

    /**
     * Gets the UserData associated with the game session.
     *
     * @return - The UserData associated with the game session.
     */
    public UserData getUserData(){
        return this.userData;
    }

    /**
     * Gets the status of the game session.
     *
     * @return - The status of the game session.
     */
    public boolean getGameStatus(){
        return this.gameStatus;
    }

    /**
     * Sets the status of the game session.
     *
     * @param gameStatus - The status of the game session.
     */
    public void setGameStatus(boolean gameStatus){
        this.gameStatus = gameStatus;
    }

    /**
     * Gets the message associated with the game session.
     *
     * @return - The message associated with the game session.
     */
    public String getMessage(){
        return this.message;
    }

    /**
     * Sets the message associated with the game session.
     *
     * @param message - The message associated with the game session.
     */
    public void setMessage(String message){
        this.message = message;
    }
}