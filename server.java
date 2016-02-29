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
    private static final int NUM_ARGS = 5;

    public static void main(String[] args)  {

        // Validate command line arguments
        if(args.length != NUM_ARGS) {
            System.out.println("Incorrect number of arguments.");
            System.out.println("Usage: java server <port> <keyStore> <keyStorePassword> <trustStore> <trustStorePassword>");
            System.exit(1);
        }

        int port = Crypto.validatePort(args[0]);
        String keyStore = Crypto.validateCertFileName(args[1]);
        String keyStorePassword = args[2];
        String trustStore = Crypto.validateCertFileName(args[3]);
        String trustStorePassword = args[4];

        //System.setProperty("javax.net.debug", "all");

        System.setProperty("javax.net.ssl.trustStore", trustStore);
        System.setProperty("javax.net.ssl.trustStorePassword", trustStorePassword);
        System.setProperty("javax.net.ssl.keyStore", keyStore);
        System.setProperty("javax.net.ssl.keyStorePassword" , keyStorePassword);

        try {
            ServerSocketFactory sslFactory = SSLServerSocketFactory.getDefault();
            ServerSocket listenSocket = sslFactory.createServerSocket(port);
            ((SSLServerSocket)listenSocket).setNeedClientAuth(true);

            Socket connection = null;
            while ((connection = listenSocket.accept()) != null) {
                //while(true) {
                ObjectOutputStream objectOutputStream = new ObjectOutputStream(connection.getOutputStream());
                ObjectInputStream objectInputStream = new ObjectInputStream(connection.getInputStream());

                while(true) {
                    Message cmd = (Message) objectInputStream.readObject();
                    if (cmd.getType() == Message.MessageType.STOP) {
                        System.out.println("Received a stop request from client. Exiting...");
                        connection.close();
                        // TODO: close stuff
                        break;
                    } else if (cmd.getType() == Message.MessageType.GET) {
                        String fileName = ((GetMessage) cmd).getFileName();
                        File file = new File(fileName);
                        File hash = new File(file.getPath() + HASH_EXTENSION);

                        // Check for existence of requested file and its hash
                        if (!file.exists() || !file.canRead() || !hash.exists() || !hash.canRead()) {
                            System.out.println("server file access error.");
                            objectOutputStream.writeObject(new ErrorMessage(
                                    new GetMessage.GetFileNotFoundException()));
                        } else {
                            Crypto.sendFile(file, objectOutputStream, null, false);
                            System.out.println("writing get to client.");
                        }
                    } else if (cmd.getType() == Message.MessageType.PUT) {

                        PutMessage putMessage = (PutMessage) cmd;
                        String fileName = putMessage.getFileName();
                        FileOutputStream fileOutputStream = new FileOutputStream(fileName);

                        TransferCompleteMessage complete = Crypto.consumeFile(objectInputStream, fileOutputStream);
                        fileOutputStream = new FileOutputStream(fileName + ".sha256");
                        fileOutputStream.write(complete.getHash());
                        System.out.println("wrote hash");

                    }
                }
                objectInputStream.close();
                objectOutputStream.close();
                connection.close();
            }

        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}
