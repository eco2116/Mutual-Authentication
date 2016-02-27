import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import java.io.*;
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

            ObjectInputStream objectInputStream = new ObjectInputStream(connection.getInputStream());
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(connection.getOutputStream());

            Message cmd = (Message) objectInputStream.readObject();
            if(cmd.getType() == Message.MessageType.STOP) {
                System.out.println("Received a stop request from client. Exiting...");
                connection.close();
                // TODO: close stuff
                break;
            } else if(cmd.getType() == Message.MessageType.GET) {
                String fileName = ((GetMessage) cmd).getFileName();
                File file = new File(fileName);
                File hash = new File(file.getPath() + ".sha256");

                // Check for existence of requested file and its hash
                if(!file.exists() || !file.canRead() || !hash.exists() || !hash.canRead()) {
                    objectOutputStream.writeObject(new ErrorMessage(
                            new GetMessage.GetFileNotFoundException("File cannot be retrieved.")));
                } else {
                  objectOutputStream.writeObject(new GetMessage(fileName, file, hash));
                }
            }

            connection.close();
        }

    }


}
