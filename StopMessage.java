/**
 * Evan O'Connor (eco2116)
 * StopMessage.java
 *
 * StopMessage is an application layer message to notify the desire to stop the client/server connection
 */
public class StopMessage extends Message {

    public StopMessage() {
        super(MessageType.STOP);
    }

}
