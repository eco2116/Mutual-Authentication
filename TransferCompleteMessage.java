/**
 * Evan O'Connor (eco2116)
 * TransferCompleteMessage.java
 *
 * TransferCompleteMessage is an application layer message to convey that the final message of a given interaction.
 * It contains the hash of the plaintext and possibly extra data bytes from the transferred file.
 */
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