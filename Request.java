import java.io.Serializable;

public class Request implements Serializable {

    public enum RequestType {
        STOP, GET, PUT
    }
    private RequestType type;

    public Request(RequestType requestType) {
        this.type = requestType;
    }

    public RequestType getType() {
        return this.type;
    }

}
