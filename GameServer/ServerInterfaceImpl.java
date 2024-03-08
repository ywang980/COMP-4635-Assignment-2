package GameServer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import UserAccountServer.UserAccountService;
import UserAccountServer.UserData;
import UserAccountServer.ActiveGameData;
import UserAccountServer.GameState;

/**
 * The ServerInterfaceImpl class implements the ServerInterface.
 */
public class ServerInterfaceImpl extends UnicastRemoteObject implements ServerInterface {

    /**
     * Constructs a ServerInterfaceImpl object.
     *
     * @throws RemoteException - if there is an issue with remote communication.
     */
    public ServerInterfaceImpl() throws RemoteException {
        super();
    }

    /**
     * Checks if a user is valid by attempting to log them in via the UserAccountService.
     *
     * @param username - The username to check for validity.
     * @return - 1 if the user is registered and not currently logged in,
     *         - 2 if the user is not registered and not logged in, and is now registered and logged in,
     *         - 0 if the user is already logged in.
     * @throws RemoteException - if there is an issue with remote communication.
     */
    public int checkValidUser(String username) throws RemoteException {
        try {
            Registry registry = LocateRegistry.getRegistry("localhost", 1099);
            UserAccountService userAccountService = (UserAccountService) registry.lookup("UserAccountService");
            int loginResult = userAccountService.login(username.trim());

            if (loginResult == 0) {
                throw new RemoteException(Constants.DUPLICATE_LOGIN);
            } else
                return loginResult;
        } catch (Exception e) {
            throw new RemoteException(Constants.CANT_COMMUNICATE_UAS, e);
        }
    }

    /**
     * Validates user data associated with the specified username.
     *
     * @param username - The username for which to validate user data.
     * @return - The UserData associated with the specified username.
     * @throws RemoteException - if there is an issue with remote communication.
     */
    public UserData validateUserData(String username) throws RemoteException {
        try {
            Registry registry = LocateRegistry.getRegistry("localhost", 1099);
            UserAccountService userAccountService = (UserAccountService) registry.lookup("UserAccountService");
            String userDataString = userAccountService.load(username);
            return new UserData(userDataString);

        } catch (Exception e) {
            throw new RemoteException(Constants.CANT_COMMUNICATE_UAS, e);
        }
    }

    /**
     * Saves game data associated with the specified UserData.
     *
     * @param userData - The UserData containing the game data to save.
     * @throws RemoteException - if there is an issue with remote communication or saving the game data.
     */
    public void saveGame(UserData userData) throws RemoteException {
        try {
            Registry registry = LocateRegistry.getRegistry("localhost", 1099);
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
     * @throws RemoteException - if there is an issue with remote communication or logging out the user.
     */
    public void logoutUser(String username) throws RemoteException {
        try {
            Registry registry = LocateRegistry.getRegistry("localhost", 1099);
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
     * Processes user input and performs actions based on the input.
     *
     * @param userData - The UserData object representing the user's data.
     * @param input - The user input to process.
     * @return - The updated UserData object after processing the input.
     * @throws RemoteException - if there is an issue with remote communication or processing the input.
     */
    public UserData processUserInput(UserData userData, String input) throws RemoteException {
        String[] tokenizedInput = input.split(";");
        if (tokenizedInput.length <= 1)
            throw new RemoteException(Constants.INVALID_COMMAND_SYNTAX);

        String command = tokenizedInput[0];
        String argument = tokenizedInput[1];

        switch (command) {
            // Add word to database
            case "Add": {
                contactDatabase('A', argument);
                userData.getGameState().setState(Constants.IDLE_STATE);
                break;
            }
            // Remove word from database
            case "Remove": {
                contactDatabase('B', argument);
                userData.getGameState().setState(Constants.IDLE_STATE);
                break;
            }
            // Start new game with specified word count
            // Argument must be an integer from 2-15, inclusive
            case "New Game": {
                try {
                    int wordCount = Integer.parseInt(argument);
                    if (wordCount < 2 || wordCount > Constants.MAX_WORD_COUNT) {
                        throw new RemoteException(Constants.WORD_COUNT_NOT_IN_RANGE);
                    }

                    createNewGame(userData, wordCount);
                    userData.getGameState().setState(Constants.PLAY_STATE);
                    break;
                    // playGame(in, out, userData);
                } catch (NumberFormatException e) {
                    throw new RemoteException(Constants.INVALID_WORD_COUNT);
                }
            }
            // Continue existing game; argument may be any non-empty string
            case "Continue": {
                if (userData.getGameState().getPuzzle() != null) {
                    userData.getGameState().setState(Constants.PLAY_STATE);
                    break;
                }
                throw new RemoteException(Constants.NO_EXISTING_GAME);
            }
            default:
                throw new RemoteException(Constants.INVALID_COMMAND_SYNTAX);
        }

        saveGame(userData);
        return userData;
    }

    /**
     * Contacts the database server to perform a specified command with the given payload.
     *
     * @param command - The command to be performed.
     * @param payload - The payload associated with the command.
     * @return - The response received from the database server.
     * @throws RemoteException - if there is an issue with remote communication or contacting the database.
     */
    private static String contactDatabase(char command, String payload) throws RemoteException {
        int wordServerPort = 8002;

        try {
            DatagramSocket wordSocket = new DatagramSocket();
            String request = String.valueOf(command) + ";" + payload;
            byte[] requestBuf = new byte[Constants.BUFFER_LIMIT];
            requestBuf = request.getBytes();

            InetAddress address = InetAddress.getByName("localhost");
            DatagramPacket packet = new DatagramPacket(requestBuf, requestBuf.length,
                    address, wordServerPort);
            wordSocket.send(packet);

            byte[] responseBuf = new byte[Constants.BUFFER_LIMIT];
            packet = new DatagramPacket(responseBuf, responseBuf.length);
            wordSocket.receive(packet);

            String word = new String(packet.getData(), 0, packet.getLength());
            return word;

        } catch (SocketException e) {
            throw new RemoteException(Constants.SOCKET_ERROR_OPEN_WORD);
        } catch (UnknownHostException e) {
            throw new RemoteException();
        } catch (IOException e) {
            throw new RemoteException(Constants.CANT_COMMUNICATE_WDBS);
        }
    }

    /**
     * Create a new game, then save/update the user data.
     */
    private static void createNewGame(UserData userData, int wordCount) throws RemoteException {
        String words[] = generateWordList(wordCount);

        // Number of attempts is either twice the word count, or the maximum
        // word count allowed, whichever is less
        int attempts = Math.min(words.length * 2, Constants.MAX_WORD_COUNT);

        userData.setGameState(new GameState(attempts, words));
        new ServerInterfaceImpl().saveGame(userData);
    }

    /**
     * Generate a new game by requesting a stem word and a list of (valid)
     * leaf words from the word database microservice.
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
     * Fetches the stem associated with the specified integer from the database.
     *
     * @param a - The integer value for which to fetch the stem.
     * @return - The stem fetched from the database.
     * @throws RemoteException - if there is an issue with remote communication or fetching the stem.
     */
    private static String fetchStem(int a) throws RemoteException {
        return contactDatabase('E', String.valueOf(a));
    }

    /**
     * Generates random leaf indices based on the given word count and stem.
     *
     * @param wordCount - The number of words in the puzzle.
     * @param stem - The stem string used for generating leaf indices.
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
     * Fetches the leaf associated with the specified matching character from the database.
     *
     * @param matchingCharacter - The character to match against in the leaf.
     * @return - The leaf fetched from the database.
     * @throws RemoteException - if there is an issue with remote communication or fetching the leaf.
     */
    private static String fetchLeaf(char matchingCharacter) throws RemoteException {
        String charToString = String.valueOf(matchingCharacter);
        return contactDatabase('D', charToString);
    }

    /**
     * Processes a word query to check if the word is in the database or the puzzle word list.
     *
     * @param userData - The UserData object representing the user's data.
     * @param input - The word query input to process.
     * @return - A message indicating whether the word is found in the database or the puzzle word list.
     * @throws RemoteException - if there is an issue with remote communication or processing the query.
     */
    public String processWordQuery(UserData userData, String input) throws RemoteException {
        boolean found = false;

        // Check if input in database first
        found = contactDatabase('C', input
                .replaceAll("\\?", "")).equals("1");

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

        return "\nThe word: " + input + " is not in the database.";
    }

    /**
     * Processes a user's guess for the puzzle.
     *
     * @param userData - The UserData object representing the user's data.
     * @param input - The user's guess input to process.
     * @return - An ActiveGameData object containing updated user data and game status.
     * @throws RemoteException - if there is an issue with remote communication or processing the guess.
     */
    public ActiveGameData processPuzzleGuess(UserData userData, String input) throws RemoteException {
        String message = "";
        GameState gameState = userData.getGameState();

        boolean successfulGuess = gameState.getPuzzle().updatePuzzleGrid(input);
        gameState.decrementAttempts();

        if (successfulGuess) {
            message = "\n*Successful guess: '" + input + "'. Puzzle updated.";
        } else {
            message = "\n*Unsuccessful guess: '" + input + "'.";
        }

        if (gameState.getPuzzle().checkPuzzleSolved()) {
            gameState.setState(Constants.IDLE_STATE);
            message += "\nYou win!";
            userData.incrementScore();
            return new ActiveGameData(userData, false, message);
        }

        if (gameState.getAttempts() == 0) {
            gameState.setState(Constants.IDLE_STATE);
            message += "\nYou lose!";
            return new ActiveGameData(userData, false, message);
        }
        return new ActiveGameData(userData, true, message);
    }
}
