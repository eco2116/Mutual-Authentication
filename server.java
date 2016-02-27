import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import java.io.*;
import java.net.PasswordAuthentication;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Evan O'Connor (eco2116)
 *
 * server.java
 */
public class server {

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        ServerSocketFactory sslFactory = SSLServerSocketFactory.getDefault();
        ServerSocket listenSocket = sslFactory.createServerSocket(1234);
        ((SSLServerSocket)listenSocket).setNeedClientAuth(true);

        Socket connection = null;
        while ((connection = listenSocket.accept()) != null) {
            InputStream in = connection.getInputStream();
            OutputStream out = connection.getOutputStream();

            ObjectInputStream objectInputStream = new ObjectInputStream(connection.getInputStream());
            Request cmd = (Request) objectInputStream.readObject();
            if(cmd.getType() == Request.RequestType.STOP) {
                System.out.println("Received a stop request from client. Exiting...");
                break;
            }

//            int c;
//            while ((c = in.read()) != -1) {
//                out.write(c);
//            }

            out.close();
            in.close();
            connection.close();
        }

    }


}
