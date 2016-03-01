/**
 * Evan O'Connor (eco2116)
 * GetMessage.java
 *
 * GetMessage is an application layer message to notify the other party of the desire to begin receiving data
 */
public class GetMessage extends Message {

    private final String fileName;

    public GetMessage(String fileName) {
        super(MessageType.GET);
        this.fileName = fileName;
    }

    public String getFileName() {
        return this.fileName;
    }
}
