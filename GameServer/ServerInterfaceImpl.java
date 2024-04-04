package GameServer;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import UserAccountServer.UserAccountService;
import UserAccountServer.UserData;
import UserAccountServer.ActiveGameData;
import UserAccountServer.GameState;

import DatabaseServer.Database;

/**
 * The ServerInterfaceImpl class implements the ServerInterface.
 */
public class ServerInterfaceImpl extends UnicastRemoteObject implements ServerInterface {

    private static Database database;
    private ConcurrentHashMap<Integer, Object> idempotancyMap = new ConcurrentHashMap<>();

    private static int sequence;

    /**
     * Constructs a ServerInterfaceImpl object.
     *
     * @throws RemoteException - if there is an issue with remote communication.
     */
    public ServerInterfaceImpl(int seq) throws RemoteException {
        super();

        sequence=seq;


        try {
            connectToDatabase();
        } catch (Exception e) {
            System.out.println("Database offline");
        }
    }

    /**
     * Establishes a connection to the remote database service.
     *
     * @throws RemoteException - if there is an issue with remote
     *                         communication while attempting to connect to
     *                         the database.
     */
    public static void connectToDatabase() throws RemoteException {
        try {
            Registry registry = LocateRegistry.getRegistry("localhost", Constants.WDBS_PORT);
            database = (Database) registry.lookup("DatabaseService");
            System.out.println("Connected to Database");
        } catch (RemoteException e) {
            throw new RemoteException(Constants.CANT_COMMUNICATE_WDBS, e);
        } catch (NotBoundException e) {
            throw new RemoteException(e.getMessage());
        }
    }

    /**
     * Checks if a user is valid by attempting to log them in via the
     * UserAccountService.
     *
     * @param username - The username to check for validity.
     * @return - 1 if the user is registered and not currently logged in,
     *         - 2 if the user is not registered and not logged in, and is now
     *         registered and logged in,
     *         - 0 if the user is already logged in.
     * @throws RemoteException - if there is an issue with remote communication.
     */
    public int checkValidUser(String username, int seq) throws RemoteException {

        if(!idempotancyMap.containsKey(seq)){
        try {
            Registry registry = LocateRegistry.getRegistry("localhost", Constants.UAS_PORT);
            UserAccountService userAccountService = (UserAccountService) registry.lookup("UserAccountService");
            int loginResult = userAccountService.login(username.trim());

            if (loginResult == 0) {
                throw new RemoteException(Constants.DUPLICATE_LOGIN);
            } else
                idempotancyMap.put(seq, loginResult);
                return loginResult;


        } catch (Exception e) {
            throw new RemoteException(Constants.CANT_COMMUNICATE_UAS, e);
        }}

        else{

            return (int) idempotancyMap.get(seq);
        }
    }

    /**
     * Fetches and validates user data associated with the specified username.
     *
     * @param username - The username for which to validate user data.
     * @return - The UserData associated with the specified username.
     * @throws RemoteException - if there is an issue with remote communication.
     */
    public UserData validateUserData(String username, int seq) throws RemoteException {

        if(!idempotancyMap.containsKey(seq)){
        try {
            Registry registry = LocateRegistry.getRegistry("localhost", Constants.UAS_PORT);
            UserAccountService userAccountService = (UserAccountService) registry.lookup("UserAccountService");
            String userDataString = userAccountService.load(username);
            UserData userData=new UserData(userDataString);
            idempotancyMap.put(seq, userData);
            return userData;


        } catch (Exception e) {
            throw new RemoteException(Constants.CANT_COMMUNICATE_UAS, e);
        }}
        else{
            return (UserData) idempotancyMap.get(seq);
        }

    }

    /**
     * Saves game data associated with the specified UserData.
     *
     * @param userData - The UserData containing the game data to save.
     * @param sequence
     * @throws RemoteException - if there is an issue with remote communication or
     *                         saving the game data.
     */
    public void saveGame(UserData userData, int sequence) throws RemoteException {
        try {
            Registry registry = LocateRegistry.getRegistry("localhost", Constants.UAS_PORT);
            UserAccountService userAccountService = (UserAccountService) registry.lookup("UserAccountService");
            int saveResult = userAccountService.save(userData.getUsername(), userData.getUserDataString());

            if (saveResult == 0) {
                throw new RemoteException(Constants.COULD_NOT_SAVE);
            }
        } catch (Exception e) {
            throw new RemoteException(Constants.COULD_NOT_SAVE, e);
        }
    }

    /**
     * Logs out the specified user.
     *
     * @param username - The username of the user to log out.
     * @throws RemoteException - if there is an issue with remote communication or
     *                         logging out the user.
     */
    public void logoutUser(String username, int seq) throws RemoteException {


        try {
            Registry registry = LocateRegistry.getRegistry("localhost", Constants.UAS_PORT);
            UserAccountService userAccountService = (UserAccountService) registry.lookup("UserAccountService");
            int logoutResult = userAccountService.logout(username.trim());

            if (logoutResult == 0) {
                throw new RemoteException("Failed to log out user: " + username);
            }
        } catch (Exception e) {
            throw new RemoteException(Constants.CANT_COMMUNICATE_UAS, e);
        }


    }

    /**
     * Validates the heartbeat signal for the specified user with the User Account Server (UAS).
     * This method sends a heartbeat signal to the UAS for the specified user to indicate
     * the continued activity of the user.
     *
     * @param username The username of the user for whom the heartbeat signal is validated.
     * @throws RemoteException If an error occurs during remote communication with the UAS.
     */
    public void validateHeartbeat(String username) throws RemoteException {
        try {
            Registry registry = LocateRegistry.getRegistry("localhost", Constants.UAS_PORT);
            UserAccountService userAccountService = (UserAccountService) registry.lookup("UserAccountService");
            userAccountService.validateHeartbeat(username);
        } catch (Exception e) {
            throw new RemoteException(Constants.CANT_COMMUNICATE_UAS, e);
        }
    }

    /**
     * Processes user input and performs actions based on the input.
     *
     * Details: user input interpreted as command-argument 2-tuple,
     * separated by a ';'. E.g., Add;dog.
     * 
     * @param userData - The UserData object representing the user's data.
     * @param input    - The user input to process.
     * @return - The updated UserData object after processing the input.
     * @throws RemoteException - if there is an issue with remote communication or
     *                         processing the input.
     */
    public UserData processUserInput(UserData userData, String input, int seq)
            throws RemoteException {

        if(!idempotancyMap.containsKey(seq)){
        String[] tokenizedInput = input.split(";");
        if (tokenizedInput.length <= 1)
            throw new RemoteException(Constants.INVALID_COMMAND_SYNTAX);

        String command = tokenizedInput[0];
        String argument = tokenizedInput[1];

        processCommand(userData, command, argument);
        saveGame(userData, sequence);
        return userData;}

        else return (UserData) idempotancyMap.get(seq);
    }

    /**
     * Executes a specific game command.
     *
     * Details: if the word database microservice cannot be reached,
     * will attempt to reconnect 1 more time before throwing a RemoteException
     * to the client caller.
     * 
     * 
     * @param userData - The UserData object representing the user's data.
     * @param command  - The command to execute.
     * @param argument - The argument associated with the command.
     * @throws RemoteException - if there is an issue with remote communication.
     */
    private  void processCommand(UserData userData, String command, String argument)
            throws RemoteException {
        switch (command) {
            // Add word to database
            case "Add": {
                try {
                    database.addWord(argument);
                    userData.getGameState().setState(Constants.IDLE_STATE);
                } catch (RemoteException e) {
                    reconnectDatabase(e);
                }
                break;
            }
            // Remove word from database
            case "Remove": {
                try {
                    database.removeWord(argument);
                    userData.getGameState().setState(Constants.IDLE_STATE);
                } catch (RemoteException | SQLException e) {
                    reconnectDatabase(e);
                }
                break;
            }
            // Start new game with specified word count
            // Argument must be an integer string from 2-15, inclusive
            case "New Game": {
                processNewGame(userData, argument);
                break;
            }
            // Continue existing game; argument may be any non-empty string
            case "Continue": {
                if (userData.getGameState().getPuzzle() != null) {
                    userData.getGameState().setState(Constants.PLAY_STATE);
                    break;
                }
                throw new RemoteException(Constants.NO_EXISTING_GAME);
            }
            default: {
                throw new RemoteException(Constants.INVALID_COMMAND_SYNTAX);
            }
        }
    }

    /**
     * Attempts to reconnect to the database in case of a communication error.
     *
     * @param exception - The exception that triggered the reconnection attempt.
     * @throws RemoteException - if there is an issue with remote communication
     *                         while attempting to reconnect.
     */
    private static void reconnectDatabase(Exception exception) throws RemoteException {
        try {
            connectToDatabase();
        } catch (RemoteException e) {
            throw new RemoteException(e.getMessage(), exception);
        }
    }

    /**
     * Processes the initiation of a new game with the specified word count.
     *
     * @param userData - The UserData object representing the user's data.
     * @param argument - The argument specifying the word count for the new game.
     * @throws RemoteException - if there is an issue with remote communication
     *                         while attempting to generate the new game.
     */
    private void processNewGame(UserData userData, String argument)
            throws RemoteException {
        try {
            int wordCount = Integer.parseInt(argument.strip());
            if (wordCount < 2 || wordCount > Constants.MAX_WORD_COUNT) {
                throw new RemoteException(Constants.WORD_COUNT_NOT_IN_RANGE);
            }

            createNewGame(userData, wordCount);
            userData.getGameState().setState(Constants.PLAY_STATE);
        } catch (NumberFormatException e) {
            throw new RemoteException(Constants.INVALID_WORD_COUNT);
        }
    }

    /**
     * Create a new game by requesting a stem word and a list of (valid) leaf words
     * from the word database microservice, then save/update the user data.
     * 
     * Details: leaves are generated as follows:
     * 
     * 1. A set of random, unique indices in the stem are selected
     * to determine where to connect the leaves from
     * 2. Leaves are fetched - choose a word that has at least 1 character
     * matching the stem's character at the specified index
     * 3. Each leaf is inserted at a random connecting index, starting
     * with the leaf that has the least matches possible
     * 4. Each leaf's matches is updated and sorted by count
     * 5. Repeat step 3-4 until all leaves are inserted
     * 
     * E.g.
     * -The stem is cat
     * -The word count is 3 --> 2 leaves needed
     * -Connecting leaf indices (randomly) chosen are: 0, 2 --> 'c', 't'
     * -Leaves generated are: cute, soccer
     * -Soccer may only be inserted at index 0, cute may be inserted at 0 or 2
     * -Soccer is inserted first, which 'c' in soccer to connect to stem with
     * is randomly chosen
     * -Soccer is inserted at index 0, cute may now only be inserted at 2
     * 
     * If it is somehow impossible to generate a valid crossword puzzle
     * from the chosen stem due to insufficient matching leaves, a new stem
     * will be selected.
     * 
     * @param userData  - The UserData object representing the user's data.
     * @param wordCount - Number of words in the crossword puzzle.
     * @throws RemoteException - if there is an issue with remote communication or
     *                         saving the game data.
     */
    private  void createNewGame(UserData userData, int wordCount)
            throws RemoteException {
        String words[] = generateWordList(wordCount);

        // Number of attempts is either twice the word count, or the maximum
        // word count allowed, whichever is less
        int attempts = Math.min(words.length * 2, Constants.MAX_WORD_COUNT);

        userData.setGameState(new GameState(attempts, words));
        saveGame(userData, sequence);
    }

    /**
     * Generates a list of words for a crossword puzzle, by requesting a stem word
     * and a list of (valid) leaf words from the word database microservice.
     * 
     * Details: if it is somehow impossible to generate a valid crossword puzzle
     * from the chosen stem due to insufficient matching leaves, a new stem will
     * be selected until a valid puzzle can be generated.
     * 
     * @param wordCount - Number of words in the crossword puzzle.
     * @return an array of strings representing the generated words for the
     *         crossword puzzle.
     * @throws RemoteException - if there is an issue with remote communication
     *                         in fetching the stem or a leaf.
     */
    private static String[] generateWordList(int wordCount) throws RemoteException {
        while (true) {
            ArrayList<String> wordsList = new ArrayList<>();
            String stem = fetchStem(wordCount - 1);
            wordsList.add(stem);

            ArrayList<Integer> leafIndicesList = generateLeafIndices(wordCount, stem);
            if (populateLeaves(leafIndicesList, stem, wordsList)) {
                return wordsList.toArray(new String[0]);
            }
        }
    }

    /**
     * Fetches the stem with a specified minimum length from the database.
     *
     * @param minimumLength - Minimum stem length.
     * @return - The stem fetched from the database.
     * @throws RemoteException - if there is an issue with remote communication in
     *                         fetching the stem.
     */
    private static String fetchStem(int minimumLength) throws RemoteException {
        if (minimumLength == 1)
            minimumLength++;

        try {
            return database.randomWordLength(minimumLength);
        } catch (RemoteException | SQLException e) {
            throw new RemoteException(Constants.CANT_COMMUNICATE_WDBS);
        }
    }

    /**
     * Generates random leaf indices based on the given word count and stem.
     *
     * @param wordCount - The number of words in the puzzle.
     * @param stem      - The stem string used for generating leaf indices.
     * @return - An ArrayList containing randomly generated leaf indices.
     */
    private static ArrayList<Integer> generateLeafIndices(int wordCount, String stem) {
        Set<Integer> leafIndices = new HashSet<>();
        while (leafIndices.size() < wordCount - 1) {
            leafIndices.add(new Random().nextInt(stem.length()));
        }
        return new ArrayList<>(leafIndices);
    }

    /**
     * Insert leaves of a crossword puzzle.
     * 
     * Details: insufficient matching leaves is determined when either of the
     * following scenarios occur:
     * 
     * 1. Leaf fetched is empty - no matching leaf possible.
     * 2. The word database microservice returns 5 consecutive leaves - matching
     * leaves exist but are insufficient.
     * 
     * @param leafIndicesList - An ArrayList containing randomly generated leaf
     *                        indices.
     * @param stem            - The stem string used for generating leaf indices.
     * @param wordsList       - An Arraylist containing the stem and leaves.
     * @return - A boolean indicating if it possible to generate a valid list of
     *         unique leaves for the given stem.
     * @throws RemoteException - if there is an issue with remote communication in
     *                         fetching a leaf.
     */
    private static boolean populateLeaves(ArrayList<Integer> leafIndicesList, String stem,
            ArrayList<String> wordsList) throws RemoteException {

        String leaf = "";
        int consecutiveDuplicateLeaf = 0;

        for (int i = 0; i < leafIndicesList.size(); i++) {
            int index = leafIndicesList.get(i).intValue();
            char connectingCharacter = stem.toCharArray()[index];

            do {
                leaf = fetchLeaf(connectingCharacter);
                consecutiveDuplicateLeaf++;

                // Can't create crossword puzzle, exit and return 'unsuccesful' flag
                if (consecutiveDuplicateLeaf > 5 || leaf.equals("")) {
                    return false;
                }
            } while (wordsList.contains(leaf));

            wordsList.add(leaf);
            consecutiveDuplicateLeaf = 0;
        }
        return true;
    }

    /**
     * Fetches the leaf associated with the specified matching character from the
     * database.
     *
     * @param matchingCharacter - The character to match against in the leaf.
     * @return - The leaf fetched from the database.
     * @throws RemoteException - if there is an issue with remote communication in
     *                         fetching the leaf.
     */
    private static String fetchLeaf(char matchingCharacter) throws RemoteException {
        try {
            return database.randomWord(matchingCharacter);
        } catch (RemoteException | SQLException e) {
            throw new RemoteException(Constants.CANT_COMMUNICATE_WDBS);
        }
    }

    /**
     * Processes a word query to check if the word is in the database or the puzzle
     * word list.
     *
     * @param userData - The UserData object representing the user's data.
     * @param input    - The word query input to process.
     * @return - A message indicating whether the word is found in the database or
     *         the puzzle word list.
     * @throws RemoteException - if there is an issue with remote communication or
     *                         processing the query.
     */
    public String processWordQuery(UserData userData, String input, int seq) throws RemoteException {

        if (!idempotancyMap.containsKey(seq)) {
            boolean found = false;

            // Check if input in database first
            try {
                found = database.checkWord(input);
            } catch (RemoteException | SQLException e) {
                throw new RemoteException(Constants.CANT_COMMUNICATE_WDBS);
            }

            if (found)
                return "\nThe word: " + input + " is in the database.";

            // Check if input in word list used to construct puzzle, in case of
            // unfortunate delete timing (i.e., user created a game with a specific
            // word, but then another user deleted said word after)

            String[] words = userData.getGameState().getWords();
            for (int i = 0; i < words.length; i++) {
                if (words[i].equals(input)) {
                    return "\nThe word: " + input + " is in the database.";
                }
            }

            return "\nThe word: " + input + " is not in the database.";}

        return (String) idempotancyMap.get(seq);
        }

    /**
     * Processes a user's guess for the puzzle.
     *
     * @param userData - The UserData object representing the user's data.
     * @param input    - The user's guess input to process.
     * @return - An ActiveGameData object containing updated user data and game
     *         status indicating whether game is ongoing.
     * @throws RemoteException - if there is an issue with remote communication or
     *                         processing the guess.
     */
    public ActiveGameData processPuzzleGuess(UserData userData, String input, int seq) throws RemoteException {

        if(!idempotancyMap.containsKey(seq)){
        String message = "";
        GameState gameState = userData.getGameState();
        gameState.appendNewGuess(input);

        boolean successfulGuess = gameState.getPuzzle().updatePuzzleGrid(input);
        gameState.decrementAttempts();
        if (successfulGuess) {
            message = "\n*Successful guess: '" + input + "'. Puzzle updated.";
        } else {
            message = "\n*Unsuccessful guess: '" + input + "'.";
        }

        // Check victory condition
        if (gameState.getPuzzle().checkPuzzleSolved()) {
            gameState.setState(Constants.IDLE_STATE);
            message += "\nYou win!";
            userData.incrementScore();
            sequence++;
            return new ActiveGameData(userData, false, message);

        }

        // Check defeat condition
        if (gameState.getAttempts() == 0) {
            gameState.setState(Constants.IDLE_STATE);
            message += "\nYou lose!";
            sequence++;
            return new ActiveGameData(userData, false, message);
        }
        return new ActiveGameData(userData, true, message);
    }
        return (ActiveGameData) idempotancyMap.get(seq);
    }
}
