import javax.net.SocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.net.Socket;

/**
 * Evan O'Connor (eco2116)
 *
 * client.java
 */

public class client {

    public static void main(String[] args) throws IOException {
        SocketFactory sslFactory = SSLSocketFactory.getDefault();
        Socket connection = sslFactory.createSocket("localhost", 1234);
        OutputStream out = connection.getOutputStream();
        InputStream in = connection.getInputStream();

        out.write("abc".getBytes());

        int c;
        while((c = in.read()) != -1) {
            System.out.print((char) c);
        }
        in.close();
        out.close();
        connection.close();
    }
}
