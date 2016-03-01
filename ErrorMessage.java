/**
 * Evan O'Connor (eco2116)
 * ErrorMessage.java
 *
 * ErrorMessage is an application layer message to notify client/server of internal errors encountered during interaction
 */
public class ErrorMessage extends Message {

    private final Exception exception;

    public ErrorMessage(Exception exception) {
        super(MessageType.ERROR);
        this.exception = exception;
    }

    public Exception getException() {
        return this.exception;
    }

}
