
public class TransferCompleteMessage extends Message {

    private final byte[] finalData;
    private final byte[] hash;

    public TransferCompleteMessage(byte[] hash, byte[] finalData) {
        super(MessageType.TRANSFER_COMPLETE);
        this.hash = hash;
        this.finalData = finalData;
    }

    public byte[] getHash() { return hash; }

    public byte[] getFinalData() { return this.finalData; }
}