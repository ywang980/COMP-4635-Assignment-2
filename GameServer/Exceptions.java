package GameServer;

class Exceptions {
    public class DuplicateLoginException extends Exception {
        public DuplicateLoginException(String message) {
            super(message);
        }
    }
}