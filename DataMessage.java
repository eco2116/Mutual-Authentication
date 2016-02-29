
public class DataMessage extends Message {

    private byte[] data;

    public DataMessage(byte[] data) {
        super(MessageType.DATA);
        this.data = data;
    }

    public byte[] getData() { return this.data; }
}
