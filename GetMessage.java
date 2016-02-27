import java.io.File;

public class GetMessage extends Message {

    private final String fileName;
    private final File file;
    private final File hash;

    public GetMessage(String fileName) {
        super(MessageType.GET);
        this.fileName = fileName;
        this.file = null;
        this.hash = null;
    }

    public GetMessage(String fileName, File file, File hash) {
        super(MessageType.GET);
        this.fileName = fileName;
        this.file = file;
        this.hash = hash;
    }

    public String getFileName() {
        return this.fileName;
    }

    public File getFile() { return this.file; }

    public File getHash() { return this.hash; }

    public static class GetFileNotFoundException extends Exception {
        GetFileNotFoundException() {
            super("Get request failed: Could not access file.");
        }
    }
}
