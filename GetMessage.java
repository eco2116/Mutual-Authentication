import java.io.File;

public class GetMessage extends Message {

    private final String fileName;
    private final byte[] fileBytes;
    private final byte[] hashBytes;

    public GetMessage(String fileName) {
        super(MessageType.GET);
        this.fileName = fileName;
        this.fileBytes = null;
        this.hashBytes = null;
    }

    public GetMessage(String fileName, byte[] fileBytes, byte[] hashBytes) {
        super(MessageType.GET);
        this.fileName = fileName;
        this.fileBytes = fileBytes;
        this.hashBytes = hashBytes;
    }

    public String getFileName() {
        return this.fileName;
    }

    public byte[] getFileBytes() { return this.fileBytes; }

    public byte[] getHashBytes() { return this.hashBytes; }

}
