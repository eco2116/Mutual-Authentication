import java.io.File;

public class PutMessage extends Message {

    private final String fileName;
    private final File file;
    private final byte[] hashArray;

    public PutMessage(String fileName) {
        super(MessageType.PUT);
        this.fileName = fileName;
        this.file = null;
        this.hashArray = null;
    }

    public PutMessage(String fileName, File file, byte[] hashArray) {
        super(MessageType.PUT);
        this.fileName = fileName;
        this.file = file;
        this.hashArray = hashArray;
    }

    public String getFileName() {
        return this.fileName;
    }

    public File getFile() { return this.file; }

    public byte[] getHashArray() { return this.hashArray; }

    public static class PutFileNotFoundException extends Exception {
        public PutFileNotFoundException() {
            super("Put request failed: could not access file.");
        }
    }

}
