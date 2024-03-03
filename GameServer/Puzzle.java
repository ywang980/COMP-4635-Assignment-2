package GameServer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Random;

/**
 * Represents a puzzle with a stem, dimensions, and two 2D char grids.
 */
class Puzzle {
    private String stem;

    // Puzzle height and width (i.e., rows/columns, respectively)
    private int rows;
    private int columns;

    /*
     * Each puzzle has 2 2-D char "grids"
     * 1 to represent the current puzzle state displayed to the player
     * 1 to represent the solved puzzle
     */
    private char[][] puzzleGrid;
    private char[][] solutionGrid;

    /**
     * Constructs a Puzzle object with the given words.
     * 
     * @param words - An array of words used to create the puzzle.
     */
    public Puzzle(String[] words) {
        this.stem = words[0].toLowerCase();
        this.rows = stem.length();

        int longestLeafLength = this.findLongestLeafLength(words);
        this.columns = longestLeafLength * 2 + 1;
        if(longestLeafLength % 2 == 0){
            this.columns++;
        }

        this.puzzleGrid = createDefaultGrid();
        this.solutionGrid = createDefaultGrid();
        populateSolutionGrid(words);
        initializePuzzleGrid();
    }

    /**
     * Constructs a Puzzle object with the given stem and puzzle data.
     * 
     * @param stem       - The stem of the puzzle.
     * @param puzzleData - The puzzle data containing the grid strings.
     */
    public Puzzle(String stem, String puzzleData) {
        this.stem = stem;

        String[] gridStrings = puzzleData.split("\\$");

        String gridString = gridStrings[0].trim();
        this.rows = gridString.split("\n").length;
        this.columns = gridString.indexOf('\n') - 1;
        this.puzzleGrid = convertStringToGrid(gridString, this.rows, this.columns);

        if (gridStrings.length > 1) {
            String solutionString = gridStrings[1].trim();
            this.solutionGrid = convertStringToGrid(solutionString, this.rows, this.columns);
        }
    }

    /**
     * Finds the length of the longest word (leaf) in the given array of words.
     * 
     * @param words - An array of words.
     * @return - The length of the longest word in the array.
     */
    private int findLongestLeafLength(String[] words) {
        int length = 0;
        for (int i = 1; i < words.length; i++)
            if (words[i].length() > length)
                length = words[i].length();
        return length;
    }

    /**
     * Initializes a grid where each row contains a series of '*' terminated with a
     * '+'.
     * 
     * @return - The initialized grid.
     */
    private char[][] createDefaultGrid() {
        char[][] grid = new char[this.rows][this.columns];
        for (int i = 0; i < this.rows; i++) {
            for (int j = 0; j < this.columns; j++)
                if (j == this.columns - 1) {
                    grid[i][j] = '+';
                } else {
                    grid[i][j] = '.';
                }
        }
        return grid;
    }

    /**
     * Populates the solution grid using an array of words.
     * 
     * @param words - An array of words.
     */
    private void populateSolutionGrid(String[] words) {

        for (int i = 0; i < words.length; i++) {
            words[i] = words[i].toLowerCase(); // For case-insensitive search
        }

        // Insert stem
        char[] stemArray = words[0].toCharArray();
        int stemColumn = (this.columns - 2) / 2;
        for (int i = 0; i < stemArray.length; i++) {
            this.solutionGrid[i][stemColumn] = stemArray[i];
        }

        // Construct/sort Leaf ArrayList
        String[] leaves = new String[words.length - 1];
        for (int i = 1; i < words.length; i++) {
            leaves[i - 1] = words[i];
        }
        ArrayList<Leaf> matchingRows = findMatchingRows(stemArray, leaves);
        Collections.sort(matchingRows, Comparator
                .comparingInt(leaf -> leaf.getMatchingIndices().size()));

        for (int i = 0; i < matchingRows.size(); i++) {
            // Find random valid row in Stem
            Leaf currentLeaf = matchingRows.get(i);
            ArrayList<Integer> currentMatches = currentLeaf.getMatchingIndices();
            int randomMatchingRow = currentMatches
                    .get(new Random().nextInt(currentMatches.size()));

            // Insert Leaf at random valid column, remove index, resort Leaf ArrayList
            insertLeaf(currentLeaf, stemArray[randomMatchingRow],
                    randomMatchingRow, stemColumn);
            for (int j = i; j < leaves.length; j++) {
                matchingRows.get(j).removeIndex(Integer.valueOf(randomMatchingRow));
            }
            Collections.sort(matchingRows, Comparator
                    .comparingInt(leaf -> leaf.getMatchingIndices().size()));
        }
    }

    /**
     * Represents a Leaf object associated with a word and its matching stem
     * indices.
     */
    private class Leaf {
        private String word;
        private ArrayList<Integer> matchingStemIndices;

        public Leaf(char[] stemArray, String leafWord) {
            this.word = leafWord;
            this.matchingStemIndices = new ArrayList<>();

            /*
             * Find all indices through which the Leaf
             * may be connected to the Stem
             */
            for (int i = 0; i < stemArray.length; i++) {
                if (word.contains(String.valueOf(stemArray[i]))) {
                    this.matchingStemIndices.add(i);
                }
            }
        }

        public String getWord() {
            return this.word;
        }

        public ArrayList<Integer> getMatchingIndices() {
            return this.matchingStemIndices;
        }

        public void removeIndex(Integer Index) {
            this.matchingStemIndices.remove(Index);
        }
    }

    /**
     * Finds and returns a list of Leaf objects with matching stem indices for each
     * leaf word.
     * 
     * @param stemArray - The array representing the stem.
     * @param leaves    - An array of leaf words.
     * @return - A list of Leaf objects with matching stem indices for each leaf
     *         word.
     */
    private ArrayList<Leaf> findMatchingRows(char[] stemArray, String[] leaves) {
        ArrayList<Leaf> matchingRows = new ArrayList<>();

        for (int i = 0; i < leaves.length; i++) {
            matchingRows.add(new Leaf(stemArray, leaves[i]));
        }
        return matchingRows;
    }

    /**
     * Inserts a Leaf at a specified row and random valid column.
     * 
     * @param leaf              The Leaf object to insert.
     * @param matchingCharacter The character in the leaf word that matches the
     *                          stem.
     * @param matchingRow       The row in the solution grid where the leaf will be
     *                          inserted.
     * @param stemColumn        The column of the stem in the solution grid.
     */
    private void insertLeaf(Leaf leaf, char matchingCharacter,
            int matchingRow, int stemColumn) {
        char[] leafArray = leaf.getWord().toCharArray();
        ArrayList<Integer> matchingIndices = new ArrayList<>();
        for (int i = 0; i < leafArray.length; i++) {
            if (leafArray[i] == matchingCharacter)
                matchingIndices.add(i);
        }
        int randomMatchingIndex = matchingIndices
                .get(new Random().nextInt(matchingIndices.size()));

        /*
         * (Horizontal) offset is x indices left of the Stem (central)
         * column, where x is the number of characters preceding the
         * connecting character
         */
        int offset = stemColumn - randomMatchingIndex;
        for (int i = 0; i < leafArray.length; i++) {
            this.solutionGrid[matchingRow][i + offset] = leafArray[i];
        }
    }

    /**
     * Constructs the initial puzzle grid.
     * The initial puzzle grid is a copy of the solution grid, where every
     * "word" character (i.e., every character that isn't a '.' or a '+')
     * is replaced with a '-'.
     */
    private void initializePuzzleGrid() {
        for (int i = 0; i < this.rows; i++) {
            String row = new String(this.solutionGrid[i]);
            row = row.replaceAll("[^.+]", "-");
            this.puzzleGrid[i] = row.toCharArray();
        }
    }

    /**
     * Converts a string representation of a grid into a 2D char array.
     * 
     * @param gridString - The string representation of the grid.
     * @param rows       - The number of rows in the grid.
     * @param columns    - The number of columns in the grid.
     * @return - The 2D char array representation of the grid.
     */
    private char[][] convertStringToGrid(String gridString, int rows, int columns) {
        String[] grid1D = gridString.split("\n");
        char[][] grid2D = new char[rows][columns];
        for (int i = 0; i < grid1D.length; i++) {
            grid2D[i] = grid1D[i].toCharArray();
        }

        return grid2D;
    }

    /**
     * Updates the puzzle grid in response to user input.
     * The user may guess either a character or a word.
     * Case 1: If the user input is a single character (character guess), all
     * occurrences of
     * the character in the solution grid will be revealed
     * Case 2: If the user input consists of 2 or more characters (word guess), the
     * stem and
     * leaves will be checked for a match. If a match is found, it will be revealed
     * A flag indicating a successful reveal (i.e., a puzzle update) is returned.
     * This flag is also
     * set to true if the user guesses something that is already revealed.
     * 
     * @param input - The user's input.
     * @return - true if the puzzle grid was updated successfully, false otherwise.
     */
    public boolean updatePuzzleGrid(String input) {
        input = input.toLowerCase(); // For case-insensitive search
        char[] inputArray = input.toCharArray();
        boolean updated = false;
        // Single character input
        if (input.length() == 1) {
            for (int i = 0; i < this.rows; i++) {
                for (int j = 0; j < this.columns; j++) {
                    if (this.solutionGrid[i][j] == inputArray[0]) {
                        this.puzzleGrid[i][j] = inputArray[0];
                        updated = true;
                    }
                }
            }
        } else {
            // Multiple character input - stem match
            if (input.equals(this.stem)) {
                for (int i = 0; i < this.rows; i++) {
                    this.puzzleGrid[i][(this.columns - 2) / 2] = inputArray[i];
                }
                return true;
            }

            // Multiple character input - leaf match
            for (int i = 0; i < this.rows; i++) {
                String leaf = new String(this.solutionGrid[i])
                        .replaceAll("[.+]", "");
                if (leaf.equals(input)) {
                    this.puzzleGrid[i] = this.solutionGrid[i];
                    return true;
                }
            }
        }
        return updated;
    }

    /**
     * Checks if the puzzle is complete (i.e., puzzle grid matches the solution
     * grid).
     * 
     * @return - true if the puzzle is solved, false otherwise.
     */
    public boolean checkPuzzleSolved() {
        return (gridToString(this.puzzleGrid).equals(gridToString(this.solutionGrid)));
    }

    /**
     * Exports the puzzle as a string.
     * 
     * @return - The string representation of the puzzle grid.
     */
    public String getPuzzleString() {
        return gridToString(this.puzzleGrid);
    }

    /**
     * Exports the solved puzzle as a string.
     * 
     * @return - The string representation of the solution grid.
     */
    public String getSolutionString() {
        return gridToString(this.solutionGrid);
    }

    /**
     * Converts a 2D char array grid into a string.
     * 
     * @param grid - The 2D char array grid to convert.
     * @return - The string representation of the grid.
     */
    private String gridToString(char[][] grid) {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < this.rows; i++) {
            stringBuilder.append(grid[i]).append("\n");
        }
        return stringBuilder.toString();
    }
}