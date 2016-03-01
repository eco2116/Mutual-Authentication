/**
 * Evan O'Connor (eco2116)
 * PutMessage.java
 *
 * PutMessage is an application layer message to notify the other party of the desire to begin sending data
 */
public class PutMessage extends Message {

    private final String fileName;

    public PutMessage(String fileName) {
        super(MessageType.PUT);
        this.fileName = fileName;
    }

    public String getFileName() {
        return this.fileName;
    }

    public static class PutFileNotFoundException extends Exception {
        public PutFileNotFoundException() {
            super("Put request failed: could not access file.");
        }
    }

}
