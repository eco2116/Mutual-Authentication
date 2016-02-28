
public class PutMessage extends Message {

    private final String fileName;
    private final byte[] fileArray;
    private final byte[] hashArray;

    public PutMessage(String fileName) {
        super(MessageType.PUT);
        this.fileName = fileName;
        this.fileArray = null;
        this.hashArray = null;
    }

    public PutMessage(String fileName, byte[] fileArray, byte[] hashArray) {
        super(MessageType.PUT);
        this.fileName = fileName;
        this.fileArray = fileArray;
        this.hashArray = hashArray;
    }

    public String getFileName() {
        return this.fileName;
    }

    public byte[] getFileArray() { return this.fileArray; }

    public byte[] getHashArray() { return this.hashArray; }

    public static class PutFileNotFoundException extends Exception {
        public PutFileNotFoundException() {
            super("Put request failed: could not access file.");
        }
    }

}
