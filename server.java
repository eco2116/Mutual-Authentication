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

    private static final String HASH_EXTENSION = ".sha256";

    public static void main(String[] args) throws IOException, ClassNotFoundException {

        //System.setProperty("javax.net.debug", "all");

        // TODO: figure out if its okay to have client.jks here - trust store? accepted certificates?
        System.setProperty("javax.net.ssl.trustStore", "client.jks");
        System.setProperty("javax.net.ssl.trustStorePassword", "password"); // TODO: better password?
        System.setProperty("javax.net.ssl.keyStore", "server.jks");
        System.setProperty("javax.net.ssl.keyStorePassword" , "password");

//        System.setProperty("javax.net.ssl.trustStore", "server.jks");
//        System.setProperty("javax.net.debug", "ssl");
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
                File hash = new File(file.getPath() + HASH_EXTENSION);

                // Check for existence of requested file and its hash
                if(!file.exists() || !file.canRead() || !hash.exists() || !hash.canRead()) {
                    objectOutputStream.writeObject(new ErrorMessage(
                            new GetMessage.GetFileNotFoundException()));
                } else {
                  objectOutputStream.writeObject(new GetMessage(fileName, file, hash));
                }
            } else if(cmd.getType() == Message.MessageType.PUT) {
                PutMessage putMessage = (PutMessage) cmd;

                String fileName = putMessage.getFileName();

                byte[] fileArray = putMessage.getFileArray();

                FileOutputStream fileOutputStream = new FileOutputStream(fileName);
                fileOutputStream.write(fileArray);
                fileOutputStream.close();

                // Write hash to disk
                byte[] hash = putMessage.getHashArray();
                fileOutputStream = new FileOutputStream(fileName + HASH_EXTENSION);
                fileOutputStream.write(hash);
                fileOutputStream.close();
            }
            connection.close();
        }

    }


}
