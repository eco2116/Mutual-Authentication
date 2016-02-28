
public class PutMessage extends Message {

    private final String fileName;
    private final byte[] fileBytes;
    private final byte[] hashBytes;

    public PutMessage(String fileName) {
        super(MessageType.PUT);
        this.fileName = fileName;
        this.fileBytes = null;
        this.hashBytes = null;
    }

    public PutMessage(String fileName, byte[] fileBytes, byte[] hashBytes) {
        super(MessageType.PUT);
        this.fileName = fileName;
        this.fileBytes = fileBytes;
        this.hashBytes = hashBytes;
    }

    public String getFileName() {
        return this.fileName;
    }

    public byte[] getFileBytes() { return this.fileBytes; }

    public byte[] getHashBytes() { return this.hashBytes; }

    public static class PutFileNotFoundException extends Exception {
        public PutFileNotFoundException() {
            super("Put request failed: could not access file.");
        }
    }

}
