/**
 * Evan O'Connor (eco2116)
 * DataMessage.java
 *
 * DataMessage is an application layer message to encapsulate bytes of data to transmit
 */
public class DataMessage extends Message {

    private byte[] data;

    public DataMessage(byte[] data) {
        super(MessageType.DATA);
        this.data = data;
    }

    public byte[] getData() { return this.data; }
}
