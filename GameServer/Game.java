package GameServer;

import UserAccountServer.UserData;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class Game {

    private static DatagramSocket wordSocket;
    private static int wordServerPort;

    /*
     * The "central" server is connected to the other components of
     * the system as follows:
     * 
     * 1. Game Client - dedicated TCP connection.
     * 2. User Account Microservice - dedicated TCP connection/request (i.e.,
     * load/save client data, validate client login, etc.).
     * 3. Word Database Microservice - single UDP port with a timeout
     * value of 10s.
     * 
     * If a microservice cannot be reached, the client connection
     * is kept active, and the menu is resent.
     * 
     * A therad pool with 20 threads is used to service each incoming request.
     */
    public static void main(String[] args) {

        if (args.length != 2) {
            System.out.println(Constants.USAGE);
            System.exit(1);
        }
        int port = Integer.parseInt(args[0]);
        wordServerPort = Integer.parseInt(args[1]);

        try {
            ServerSocket serverSocket = new ServerSocket(port);
            wordSocket = new DatagramSocket();
            wordSocket.setSoTimeout(Constants.UDP_TIMEOUT);
            ExecutorService fixedThreadPool = Executors.newFixedThreadPool(20);
            System.out.println("Listening for incoming requests...");

            while (true) {
                fixedThreadPool.execute(new newGameHandler(serverSocket.accept()));
            }
        } catch (SocketException e) {
            System.out.println(Constants.SOCKET_ERROR_OPEN_WORD);
        } catch (IOException e) {
            System.out.println(Constants.SOCKET_ERROR_OPEN_GAMESERVER);
        }
    }

    private static class newGameHandler implements Runnable {
        private Socket clientSocket;

        public newGameHandler(Socket socket) {
            this.clientSocket = socket;
        }

        public void run() {
            try {
                handleClient(clientSocket);
            } catch (IOException e) {
                System.out.println(Constants.SOCKET_ERROR_CLOSE);
                System.out.println(e.getMessage());
            }
        }

        /*
         * First the client's username is authenticated. Upon successful
         * authentication (i.e., no communication error with the account
         * microservice nor duplicate login), the client data is loaded. If that
         * is also succesful, the client may proceed to the game.
         * 
         * Exception Handling: connection issues between the client and game server.
         * Will attempt to log user out, and print a corresponding error message.
         */
        private static void handleClient(Socket clientSocket) throws IOException {
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintStream out = new PrintStream(clientSocket.getOutputStream());
            System.out.println("Incoming connection request detected.");

            String username = null;

            try {
                username = validateUsername(clientSocket, in, out);
                UserData userData = validateUserData(out, username);

                if (userData != null) {
                    System.out.println("Client: '" + userData.getUsername() + "' connected.");
                    serveUser(in, out, userData);
                }
            } catch (IOException e) {
                System.out.println(Constants.CANT_COMMUNICATE_CLIENT);
            } finally {
                if (username != null) {
                    logoutUser(username, out);
                }
                in.close();
                out.close();
                System.out.println("Connection successfully closed.");
            }
        }

        /*
         * Repeatedly prompt the client for their username until a valid username
         * (i.e., for a user that isn't already online) is provided.
         * 
         * Note: if the account microservice is offline, any username supplied by
         * the client will be flagged as invalid.
         */
        private static String validateUsername(Socket clientSocket,
                BufferedReader in, PrintStream out) throws IOException {

            String username = "";
            boolean validUserName = false;

            while (!validUserName) {
                username = promptUserName(clientSocket, in, out);
                try {
                    validUserName = checkValidUser(username, out);
                } catch (Exceptions.DuplicateLoginException e) {
                    out.println("\nError:" + e.getMessage());
                    out.println("Try again.");
                }
            }
            return username;
        }

        /*
         * Prompt the client for a non-empty string as their username.
         */
        private static String promptUserName(Socket clientSocket,
                BufferedReader in, PrintStream out) throws IOException {

            out.println("\nWelcome to the crossword puzzle game. Please enter your username."
                    + Constants.MESSAGE_END_DELIM);
            String username = in.readLine();

            if (username == null) {
                throw new IOException(Constants.NO_CLIENT_INPUT);
            }
            return username;
        }

        /*
         * Open a new TCP connection with the user account microservice to
         * check if the client's supplied username is valid.
         * Case 0: invalid user - already logged in.
         * Case 1: existing user.
         * Case 2: new user - automatically created/registered.
         */
        private static boolean checkValidUser(String username, PrintStream out)
                throws Exceptions.DuplicateLoginException {

            try (Socket accountSocket = new Socket("localhost", Constants.UAS_PORT)) {
                BufferedWriter dataOut = new BufferedWriter(new OutputStreamWriter(accountSocket.getOutputStream()));

                String output = "login;" + username.trim();
                dataOut.write(output);
                dataOut.newLine();
                dataOut.flush();

                BufferedReader in = new BufferedReader(new InputStreamReader(accountSocket.getInputStream()));
                int loginResult = Integer.parseInt(in.readLine());
                if (loginResult == 0) {
                    throw new Exceptions().new DuplicateLoginException(Constants.DUPLICATE_LOGIN);
                } else {
                    if (loginResult == 1) {
                        out.println("\nLogging in as: " + username);
                    } else {
                        out.println("\nCreating new account: " + username);
                    }
                    return true;
                }
            } catch (IOException e) {
                out.println(Constants.CANT_COMMUNICATE_UAS);
                return false;
            }
        }

        /*
         * Open a new TCP connection with the user account microservice to fetch
         * the username's associated data as a string, and attempt to construct a
         * UserData object from it.
         * 
         * Exception Handling: connection issues between the game server and
         * the user account microservice. Will return user to menu prompting for
         * their username with a corresponding error message.
         */
        private static UserData validateUserData(PrintStream out, String username) {
            UserData userData = null;
            try (Socket accountSocket = new Socket("localhost", Constants.UAS_PORT)) {
                BufferedWriter dataOut = new BufferedWriter(new OutputStreamWriter(accountSocket.getOutputStream()));

                String output = "load;" + username;
                dataOut.write(output);
                dataOut.newLine();
                dataOut.flush();

                BufferedReader in = new BufferedReader(new InputStreamReader(accountSocket.getInputStream()));
                StringBuilder userDataBuilder = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) {
                    userDataBuilder.append(line).append("\n");
                }
                userData = new UserData(userDataBuilder.toString());
            } catch (IOException e) {
                out.println(Constants.CANT_COMMUNICATE_UAS);
                e.printStackTrace();
            }
            return userData;
        }

        /*
         * Upon successful login and load of user data, the user will be given 2
         * menus through which they may interact with the game.
         * 
         * Menu 1: the User Menu - the user may start a new game, continue their
         * existing game, or add/remove words from the database. To exit the
         * connection, the user should enter the key string: "*Exit*".
         * 
         * Menu 2: the Game Menu - opened whenever the user plays a game. Here, the
         * user may guess a word, a letter, or query a word to see if it exists
         * within the database. To return to the User Menu, the user should enter the
         * key string "*Save*".
         * 
         * Exception Handling: connection issues between the game server and the
         * word database microservice, or invalid user input. In either case, will
         * return user to User Menu with a corresponding error message.
         */
        private static void serveUser(BufferedReader in, PrintStream out, UserData userData)
                throws IOException {
            String input;

            try {
                // Sentinel loop for the user menu
                do {
                    out.println("\nUser: " + userData.getUsername());
                    out.println("Score: " + userData.getScore());
                    out.println(Constants.USER_MENU + Constants.MESSAGE_END_DELIM);
                    input = in.readLine().trim();
                    try {
                        if (input.equals(Constants.EXIT_CODE)) {
                            saveGame(userData);
                            break;
                        }
                        boolean existingGame = userData.getGameState().getState()
                                .equals(Constants.PLAY_STATE);

                        processUserInput(in, out, userData, input, existingGame);
                    } catch (SocketTimeoutException e) {
                        // Handle IO exception if user input invalid/can't contact
                        // word database microservice
                        handleError(out, userData, new IOException(Constants.CANT_COMMUNICATE_WDBS));
                    } catch (IOException e) {
                        // Handle IO exception if user input invalid/can't contact
                        // word database microservice
                        handleError(out, userData, e);
                    }
                } while (true);
            } catch (IOException e) {
                System.out.println("Error: could not communicate with client.");
            }
        }

        /*
         * Process user input for interacting with the User Menu.
         * 
         * Details: user input is tokenized in the following format: command;argument.
         */
        private static void processUserInput(BufferedReader in, PrintStream out,
                UserData userData, String input, boolean existingGame) throws IOException {

            // Tokenize user input
            String[] tokenizedInput = input.split(";");
            if (tokenizedInput.length <= 1)
                throw new IOException(Constants.INVALID_COMMAND_SYNTAX);

            String command = tokenizedInput[0];
            String argument = tokenizedInput[1];

            // Handle various commands
            switch (command) {
                // Add word to database
                case "Add": {
                    out.println("\n" + contactDatabase('A', argument));
                    break;
                }
                // Remove word from database
                case "Remove": {
                    out.println("\n" + contactDatabase('B', argument));
                    break;
                }
                // Start new game with specified word count
                // Argument must be an integer from 2-15, inclusive
                case "New Game": {
                    try {
                        int wordCount = Integer.parseInt(argument);
                        if (wordCount < 2 || wordCount > Constants.MAX_WORD_COUNT) {
                            throw new IOException(Constants.WORD_COUNT_NOT_IN_RANGE);
                        }

                        createNewGame(userData, wordCount);
                        playGame(in, out, userData);
                    } catch (NumberFormatException e) {
                        throw new IOException(Constants.INVALID_WORD_COUNT);
                    }
                    break;
                }
                // Continue existing game; argument may be any non-empty string
                case "Continue": {
                    if (existingGame) {
                        playGame(in, out, userData);
                    } else {
                        throw new IOException(Constants.NO_EXISTING_GAME);
                    }
                    break;
                }
                default:
                    throw new IOException(Constants.INVALID_COMMAND_SYNTAX);
            }
        }

        /*
         * Send a request to the word database microservice in the command;argument
         * format.
         * 
         * Exception Handling: unknown host IP address. Will throw an IOException
         * that is propagated up to and caught in handleClient.
         */
        private static String contactDatabase(char command, String payload) throws IOException {
            try {
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
            } catch (UnknownHostException e) {
                throw new IOException("Could not determine IP address of host.");
            }
        }

        /*
         * Create a new game, then save/update the user data.
         */
        private static void createNewGame(UserData userData, int wordCount) throws IOException {
            String words[] = generateWordList(wordCount);

            // Number of attempts is either twice the word count, or the maximum
            // word count allowed, whichever is less
            int attempts = Math.min(words.length * 2, Constants.MAX_WORD_COUNT);

            userData.setGameState(new GameState(attempts, words));
            saveGame(userData);
        }

        /*
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
        private static String[] generateWordList(int wordCount) throws IOException {
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

        private static String fetchStem(int a) throws IOException {
            return contactDatabase('E', String.valueOf(a));
        }

        private static ArrayList<Integer> generateLeafIndices(int wordCount, String stem) {
            Set<Integer> leafIndices = new HashSet<>();
            while (leafIndices.size() < wordCount - 1) {
                leafIndices.add(new Random().nextInt(stem.length()));
            }
            return new ArrayList<>(leafIndices);
        }

        /*
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
                ArrayList<String> wordsList) throws IOException {

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

        private static String fetchLeaf(char matchingCharacter) throws IOException {
            String charToString = String.valueOf(matchingCharacter);
            return contactDatabase('D', charToString);
        }

        /*
         * Process user input for interacting with the Game Menu.
         * 
         * Details: the user may guess either a character or a word (any string with 2+
         * characters is interpreted as a word). The user may also query a word to see
         * if it exists within the database by prefixing their input with a '?'.
         * 
         * Exception Handling: connection issues between the game server and the
         * word database microservice, or invalid user input. In either case, will
         * return user to Game Menu with a corresponding error message.
         */
        private static void playGame(BufferedReader in, PrintStream out, UserData userData)
                throws IOException {
            GameState gameState = userData.getGameState();
            gameState.setState(Constants.PLAY_STATE);

            String input;
            int gameOver = 0;

            // Sentinel loop for the game menu
            while (gameOver == 0) {
                try {
                    out.print("\n" + gameState.getPuzzle().getPuzzleString());
                    input = getValidInput(in, out, gameState);
                    gameOver = processGameInput(in, out, gameState, input);
                } catch (SocketTimeoutException e) {
                    handleError(out, userData, new IOException(Constants.CANT_COMMUNICATE_WDBS));
                } catch (IOException e) {
                    gameOver = 0;
                    handleError(out, userData, e);
                }
            }

            if (gameOver == 2) {
                userData.incrementScore();
            }
            saveGame(userData);
        }

        /*
         * Repeatedly Validate and process user game input until the user enters
         * the key string "*Save*"".
         */
        private static String getValidInput(BufferedReader in, PrintStream out, GameState gameState)
                throws IOException {
            String input = "";

            do {
                out.println(Constants.GAME_MENU);
                out.println("Attempts remaining: " + gameState.getAttempts()
                        + Constants.MESSAGE_END_DELIM);

                input = in.readLine().trim();
                // Prevent user from entering certain special characters required to
                // represent puzzle
                if (input.matches(Constants.NO_SPECIAL_CHAR_REGEX)) {
                    throw new IOException("\nInvalid guess: " + input + ". Try again.");
                } else
                    return input;
            } while (true);
        }

        private static int processGameInput(BufferedReader in, PrintStream out,
                GameState gameState, String input) throws IOException {
            // Save command
            if (input.equals(Constants.SAVE_CODE)) {
                return 1;
            }

            // Query case
            else if (input.toCharArray()[0] == '?') {
                if (processWordQuery(in, out, gameState, input.substring(1))) {
                    out.println("\nThe word: " + input + " is in the database.");
                } else {
                    out.println("\nThe word: " + input + " is not in the database.");
                }
                return 0;
            }

            else {
                return processPuzzleGuess(in, out, gameState, input);
            }
        }

        /*
         * Check if a word exists within the database.
         */
        private static Boolean processWordQuery(BufferedReader in, PrintStream out,
                GameState gameState, String input) throws IOException {

            boolean found = false;

            // Check if input in database first
            found = contactDatabase('C', input
                    .replaceAll("\\?", "")).equals("1");

            // Check if input in word list used to construct puzzle, in case of
            // of unfortunate delete timing (i.e., user created a game with a specific
            // word, but then another user deleted said word after)
            if (!found) {
                String[] words = gameState.getWords();
                for (int i = 0; i < words.length; i++) {
                    if (words[i].equals(input)) {
                        return true;
                    }
                }
            }
            return found;
        }

        /*
         * Process user's puzzle guess, updating attempts and checking
         * for conditions of victory/defeat.
         * 
         * Return value is 0, 1, or 2, indicating to continue game, a loss,
         * or a win, respectively.
         */
        private static int processPuzzleGuess(BufferedReader in, PrintStream out,
                GameState gameState, String input) {
            boolean successfulGuess = gameState.getPuzzle().updatePuzzleGrid(input);
            gameState.decrementAttempts();

            if (successfulGuess) {
                out.println("\n*Successful guess: '" + input + "'. Puzzle updated.");
            }
            else{
                out.println("\n*Unsuccessful guess: '" + input + "'.");
            }

            if (gameState.getPuzzle().checkPuzzleSolved()) {
                gameState.setState(Constants.IDLE_STATE);
                out.println("You win!");
                return 2;
            }

            if (gameState.getAttempts() == 0) {
                gameState.setState(Constants.IDLE_STATE);
                out.println("You lose!");
                return 1;
            }

            // if (successfulGuess) {
            //     out.println("\n*Successful guess: '" + input + "'. Puzzle updated.");
            //     if (gameState.getPuzzle().checkPuzzleSolved()) {
            //         gameState.setState(Constants.IDLE_STATE);
            //         out.println("You win!");
            //         return 2;
            //     }
            // } else {
            //     out.println("\n*Unsuccessful guess: '" + input + "'.");
            //     if (gameState.getAttempts() == 0) {
            //         gameState.setState(Constants.IDLE_STATE);
            //         out.println("You lose!");
            //         return 1;
            //     }
            // }
            return 0;
        }

        /*
         * Open a new TCP connection with the user account microservice to save the
         * username's associated data as a string.
         * 
         * Exception Handling: connection issues between the game server and
         * the user account microservice. Will return user to menu prompting for
         * their username with a corresponding error message.
         */
        private static void saveGame(UserData userData) throws IOException {

            try (Socket accountSocket = new Socket("localhost", Constants.UAS_PORT)) {
                BufferedWriter dataOut = new BufferedWriter(new OutputStreamWriter(accountSocket.getOutputStream()));

                String output = "save;" + userData.getUsername();
                dataOut.write(output + "\n" + userData.getUserDataString());
                dataOut.newLine();
                dataOut.flush();

                BufferedReader in = new BufferedReader(new InputStreamReader(accountSocket.getInputStream()));
                int saveResult = Integer.parseInt(in.readLine());
                if (saveResult == 0) {
                    throw new IOException("Couldn't save game.");
                }
            } catch (IOException e) {
                throw new IOException("Couldn't save game.");
            }
        }

        /*
         * Open a new TCP connection with the user account microservice to log
         * a user out.
         * 
         * Exception Handling: connection issues between the game server and
         * the user account microservice. Will return user to menu prompting for
         * their username with a corresponding error message.
         */
        private static void logoutUser(String username, PrintStream out) {
            try (Socket accountSocket = new Socket("localhost", Constants.UAS_PORT)) {
                BufferedWriter dataOut = new BufferedWriter(new OutputStreamWriter(accountSocket.getOutputStream()));

                String output = "logout;" + username.trim();
                dataOut.write(output);
                dataOut.newLine();
                dataOut.flush();

                BufferedReader inLogout = new BufferedReader(new InputStreamReader(accountSocket.getInputStream()));
                int logoutResult = Integer.parseInt(inLogout.readLine());
                if (logoutResult == 0) {
                    out.println("Failed to log out user: " + username);
                } else {
                    out.println("Logging out: " + username);
                }
            } catch (IOException e) {
                out.println("Error: Could not communicate with user account server.");
                e.printStackTrace();
            }
        }

        /*
         * In the event of an error, attempt to save the user's data before
         * returning them to their previous menu with a corresponding error message.
         */
        private static void handleError(PrintStream out, UserData userData, Exception e) {
            out.println("\nError: " + (e.getMessage()));
            try {
                saveGame(userData);
            } catch (IOException saveError) {
                out.println("Could not save user data.");
            }
        }
    }
}