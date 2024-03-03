package GameServer;

public class Constants {
        public static final int BUFFER_LIMIT = 1000; // UDP buffer limit
        public static final String USAGE = "java Game [Port] [Word Microservice Port]";

        public static final String USER_DATA_DIRECTORY = "./UserData/";
        public static final int UAS_PORT = 8081; // Port of user account microservice

        public static final int MAX_WORD_COUNT = 15;
        public static final int UDP_TIMEOUT = 1000;

        // Various key codes to faciliate user menu navigation
        public static final String MESSAGE_END_DELIM = "\n*End of Message*";
        public static final String EXIT_CODE = "*Exit*";
        public static final String SAVE_CODE = "*Save*";

        // Regex to prevent user from guessing strings with '+', '-', or '.'
        public static final String NO_SPECIAL_CHAR_REGEX = ".*[+\\-\\.].*";

        /*
         * User menu and game menu. User is either playing a game or idle.
         * Indicated by IDLE_STATE/PLAY_STATE value in GameState object's
         * state property.
         */
        public static final String USER_MENU = "\nEnter a command from the list below " +
                        "(each command must adhere to the specified syntax - CASE SENSITIVE):\n" +
                        "Add;WordName              //Add a word to the database.\n" +
                        "Remove;WordName           //Remove a word from the database.\n" +
                        "New Game;x                //Start a new game with x words.\n" +
                        "Continue;*                //Continue existing game.\n" +
                        "*Exit*                    //Exit Game.";

        public static final String GAME_MENU = "\nEnter a command from the list below " +
                        "(each command must adhere to the specified syntax - CASE SENSITIVE):\n" +
                        "Letter                    //Guess a letter.\n" +
                        "WordName                  //Guess a word.\n" +
                        "?WordName                 //Check if word exists in database.\n" +
                        "*Save*                    //Save and return to main menu.";

        public static final String IDLE_STATE = "Idle";
        public static final String PLAY_STATE = "Play";

        /*
         * Special file delimiter. Used to indicate end of puzzle grid
         * and start of solution grid in user game data.
         */
        public static final String SOLUTION_GRID_DELIM = "$";

        // Error messages regarding network issues
        public static final String SOCKET_ERROR_OPEN_WORD = "Could not create socket to word database microservice.";
        public static final String SOCKET_ERROR_OPEN_GAMESERVER = "Could not create server socket.";
        public static final String SOCKET_ERROR_CLOSE = "Error while attempting to close socket.";
        public static final String CANT_COMMUNICATE_CLIENT = "Error: could not communicate with client.";
        public static final String CANT_COMMUNICATE_UAS = "Error, could not contact user account microservice.";
        public static final String CANT_COMMUNICATE_WDBS = "Could not contact word database microservice.\n" +
                        "Returning to previous menu.";

        // Error messages regarding user login
        public static final String NO_CLIENT_INPUT = "No client input.";
        public static final String DUPLICATE_LOGIN = "User already logged in.";

        // Error messages regarding loading user data
        public static final String CANT_CREATE_USER_FILE = "Could not create user data file.";
        public static final String PREMATURE_EOF = "End of file reached prematurely.";
        public static final String VALUE_PARSE_FAIL = "Failed to parse value in user data.";
        public static final String NON_INTEGER_VALUE = "Parsed value is not an integer.";

        // Error messages regarding invalid user input
        public static final String INVALID_COMMAND_SYNTAX = "Invalid command syntax. Try again.";
        public static final String WORD_COUNT_NOT_IN_RANGE = "Word count argument exceeds allowed range.";
        public static final String INVALID_WORD_COUNT = "Word count argument is not a number.";
        public static final String NO_EXISTING_GAME = "No existing game found.";
}