package UserAccountServer;

import java.io.*;

import GameServer.Constants;

/**
 * Represents the state of the game.
 */
public class GameState implements Serializable {
    private String state;
    private int attempts;
    private String[] words;
    private Puzzle puzzle;

    /**
     * Constructs a game state with an idle state.
     */
    public GameState() {
        this.state = Constants.IDLE_STATE;
        this.puzzle = null;
    }

    /**
     * Constructs a game state with a play state.
     * 
     * @param attempts - The number of attempts.
     * @param words    - The array of words.
     */
    public GameState(int attempts, String[] words) {
        this.state = Constants.PLAY_STATE;
        this.attempts = attempts;
        this.words = words;
        this.puzzle = new Puzzle(this.words);
    }

    /**
     * Constructs a game state from serialized data.
     * 
     * @param data - The serialized data representing the game state.
     */
    public GameState(String data) {
        String[] lines = data.trim().split("\n");
        this.state = lines[0].split(";")[1];

        // Single line GameState indicates no ongoing game (i.e. no attempts
        // count/Puzzle object)
        if (lines.length == 1) {
            this.puzzle = null;
        } else {
            this.attempts = Integer.parseInt(lines[1].split(";")[1]);
            String wordsData = lines[2].split(";")[1];
            this.words = wordsData.split(",");

            StringBuilder puzzleData = new StringBuilder();
            for (int i = 3; i < lines.length; i++) {
                puzzleData.append(lines[i]).append("\n");
            }
            this.puzzle = new Puzzle(this.words[0], puzzleData.toString());
        }
    }

    /**
     * Gets the state of the game.
     * 
     * @return - The state of the game.
     */
    public String getState() {
        return this.state;
    }

    /**
     * Sets the state of the game.
     * 
     * @param state - The state to set.
     */
    public void setState(String state) {
        this.state = state;
    }

    /**
     * Gets the number of attempts remaining.
     * 
     * @return - The number of attempts remaining.
     */
    public int getAttempts() {
        return this.attempts;
    }

    /**
     * Decrements the number of attempts remaining.
     */
    public void decrementAttempts() {
        this.attempts--;
    }

    /**
     * Gets the array of words.
     * 
     * @return - The array of words.
     */
    public String[] getWords() {
        return this.words;
    }

    /**
     * Gets the puzzle.
     * 
     * @return - The puzzle.
     */
    public Puzzle getPuzzle() {
        return this.puzzle;
    }

    /**
     * Reset puzzle to null after a completed game.
     */
    public void resetPuzzle() {
        this.puzzle = null;
    }

    /**
     * Gets the string representation of the game state.
     * 
     * @return - The string representation of the game state.
     */
    public String getGameStateString() {
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("State;").append(this.state).append("\n");

        if (this.puzzle != null) {
            stringBuilder.append("Attempts;").append(this.attempts).append("\n");

            stringBuilder.append("Words;");
            for (int i = 0; i < this.words.length; i++) {
                stringBuilder.append(this.words[i]);
                if (i < words.length - 1) {
                    stringBuilder.append(", ");
                }
            }

            stringBuilder.append("\n").append(this.puzzle.getPuzzleString());
            stringBuilder.append("$\n").append(this.puzzle.getSolutionString());
        }
        String gameStateString = stringBuilder.toString();
        return gameStateString;
    }
}