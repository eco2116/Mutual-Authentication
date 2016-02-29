
public class DataMessage extends Message {

    private byte[] data;
    public int number;

    public DataMessage(byte[] data) {
        super(MessageType.DATA);
        this.data = data;
    }

    public byte[] getData() { return this.data; }
}
