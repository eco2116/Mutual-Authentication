
public class PutMessage extends Message {

    private final String fileName;
    private final long totalSize;

    public PutMessage(String fileName, long totalSize) {
        super(MessageType.PUT);
        this.fileName = fileName;
        this.totalSize = totalSize;
    }

    public String getFileName() {
        return this.fileName;
    }

    public long getTotalSize() { return this.totalSize; }

    public static class PutFileNotFoundException extends Exception {
        public PutFileNotFoundException() {
            super("Put request failed: could not access file.");
        }
    }

}
