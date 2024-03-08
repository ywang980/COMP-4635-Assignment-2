package UserAccountServer;

import java.io.Serializable;

public class ActiveGameData implements Serializable{
    private UserData userData;
    private boolean gameStatus;
    private String message;

    public ActiveGameData(UserData userData, boolean gameStatus, String message) {
        this.userData = userData;
        this.gameStatus = gameStatus;
        this.message = message;
    }

    public UserData getUserData(){
        return this.userData;
    }

    public boolean getGameStatus(){
        return this.gameStatus;
    }

    public void setGameStatus(boolean gameStatus){
        this.gameStatus = gameStatus;
    }

    public String getMessage(){
        return this.message;
    }

    public void setMessage(String message){
        this.message = message;
    }
}