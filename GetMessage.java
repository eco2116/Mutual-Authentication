
public class GetMessage extends Message {

    private final String fileName;

    public GetMessage(String fileName) {
        super(MessageType.GET);
        this.fileName = fileName;
    }

    public String getFileName() {
        return this.fileName;
    }
}
