
public class ErrorMessage extends Message {

    private final Exception exception;

    public ErrorMessage(Exception exception) {
        super(MessageType.ERROR);
        this.exception = exception;
    }

}
