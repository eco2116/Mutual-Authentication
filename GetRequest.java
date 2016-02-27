
public class GetRequest extends Request {

    private final boolean isEncrypted;
    private final String fileName;

    public GetRequest(boolean isEncrypted, String fileName) {
        super(RequestType.GET);
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
