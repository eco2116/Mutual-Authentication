import java.io.Serializable;

public class Message implements Serializable {

    private static final long serialVersionUID = 42L;

    public enum MessageType {
        STOP, GET, PUT, ERROR
    }
    private MessageType type;

    public Message(MessageType messageType) {
        this.type = messageType;
    }

    public MessageType getType() {
        return this.type;
    }

}
