
public class PutRequest extends Request {

    private final boolean isEncrypted;
    private final String fileName;

    public PutRequest(boolean isEncrypted, String fileName) {
        super(RequestType.PUT);
        this.isEncrypted = isEncrypted;
        this.fileName = fileName;
    }

    public boolean isEncrypted() {
        return isEncrypted;
    }

    public String getFileName() {
        return this.fileName;
    }

}
