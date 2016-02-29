
public class DataMessage extends Message {

    private final byte[] data;
    public int number;

    public DataMessage(byte[] data, int number) {
        super(MessageType.DATA);
        this.data = data;
        this.number = number;
    }

    public byte[] getData() { return this.data; }
}
