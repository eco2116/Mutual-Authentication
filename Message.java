import java.io.Serializable;

/**
 * Evan O'Connor (eco2116)
 * Message.java
 *
 * Message is a serializable application layer message parent class to convey from one party to another
 * what type of interaction is desired by the sender.
 */
public class Message implements Serializable {

    private static final long serialVersionUID = 42L;

    public enum MessageType {
        STOP, GET, PUT, ERROR, DATA, TRANSFER_COMPLETE
    }
    private MessageType type;

    public Message(MessageType messageType) {
        this.type = messageType;
    }

    public MessageType getType() {
        return this.type;
    }

}
