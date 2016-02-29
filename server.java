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

    public static void main(String[] args) {
        try {
            ServerSocket listenSocket = setUpInteraction(args);
            interactWithClient(listenSocket);
        } catch(Crypto.ConnectionException e) {
            System.out.println(e.getMessage());
        } catch(Crypto.StopException e) {
            System.out.println(e.getMessage());
        }
    }

    private static ServerSocket setUpInteraction(String[] args) throws Crypto.ConnectionException {
        // Validate number of command line arguments
        if(args.length != NUM_ARGS) {
            System.out.println("Incorrect number of arguments.");
            System.out.println("Usage: java server <port> <keyStore> <keyStorePassword> <trustStore> <trustStorePassword>");
            System.exit(1);
        }

        // Validate values of command line arguments
        int port = Crypto.validatePort(args[0]);
        String keyStore = Crypto.validateCertFileName(args[1]);
        String keyStorePassword = args[2];
        String trustStore = Crypto.validateCertFileName(args[3]);
        String trustStorePassword = args[4];

        // Initialize system properties required for mutual authentication
        System.setProperty("javax.net.ssl.trustStore", trustStore);
        System.setProperty("javax.net.ssl.trustStorePassword", trustStorePassword);
        System.setProperty("javax.net.ssl.keyStore", keyStore);
        System.setProperty("javax.net.ssl.keyStorePassword" , keyStorePassword);

        // Setup up server socket
        ServerSocket listenSocket;
        ServerSocketFactory sslFactory = SSLServerSocketFactory.getDefault();
        try {
            listenSocket = sslFactory.createServerSocket(port);
            ((SSLServerSocket)listenSocket).setNeedClientAuth(true);
        } catch(IOException e) {
            throw new Crypto.ConnectionException("Failed to create server socket.");
        }
        return listenSocket;
    }

    private static void interactWithClient(ServerSocket listenSocket) throws Crypto.ConnectionException,
            Crypto.StopException {

        Socket connection = null;
        ObjectInputStream objectInputStream = null;
        ObjectOutputStream objectOutputStream = null;
        try {
            while ((connection = listenSocket.accept()) != null) {
                objectOutputStream = new ObjectOutputStream(connection.getOutputStream());
                objectInputStream = new ObjectInputStream(connection.getInputStream());

                // TODO: switch
                while (true) {
                    Message cmd = (Message) objectInputStream.readObject();
                    if (cmd.getType() == Message.MessageType.STOP) {
                        objectInputStream.close();
                        objectOutputStream.close();
                        connection.close();
                        // TODO: close stuff
                        throw new Crypto.StopException();
                    } else if (cmd.getType() == Message.MessageType.GET) {
                        try {
                            handleSendFile(cmd, objectOutputStream);
                        } catch(Crypto.SendException e) {
                            objectOutputStream.writeObject(new ErrorMessage(e));
                        }

                    } else if (cmd.getType() == Message.MessageType.PUT) {
                        try {
                            handleWriteFile(cmd, objectInputStream, objectOutputStream);
                        } catch(Crypto.RetrievalException e) {
                            objectOutputStream.writeObject(new ErrorMessage(e));
                        }
                    }
                }
            }
        } catch(IOException e) {
            throw new Crypto.ConnectionException("Failed to accept connection.");
        } catch(ClassNotFoundException e) {
            throw new Crypto.ConnectionException("Unexpected message encountered.");
        } finally {
            try {
                objectInputStream.close();
                objectOutputStream.close();
                connection.close();
            } catch(IOException e) {
                throw new Crypto.ConnectionException("Failed to close streams/sockets.");
            }
        }
    }

    private static void handleSendFile(Message cmd, ObjectOutputStream objectOutputStream) throws Crypto.SendException {
        String fileName = ((GetMessage) cmd).getFileName();
        File file = new File(fileName);
        File hash = new File(file.getPath() + HASH_EXTENSION);

        // Check for existence of requested file and its hash
        try {
            if (!file.exists() || !file.canRead() || !hash.exists() || !hash.canRead()) {
                System.out.println("server file access error.");
                objectOutputStream.writeObject(new ErrorMessage(new Crypto.SendException()));
            } else {
                // Send the file to the client
                Crypto.sendFile(file, objectOutputStream, null, false);
            }
        } catch(Exception e) {
            try {
                objectOutputStream.writeObject(new ErrorMessage(new Crypto.RetrievalException()));
            } catch(IOException e1) {
                throw new Crypto.SendException();
            }
        }
        System.out.println("writing get to client.");
    }

    private static void handleWriteFile(Message cmd, ObjectInputStream objectInputStream, ObjectOutputStream objectOutputStream)
            throws Crypto.RetrievalException {

        PutMessage putMessage = (PutMessage) cmd;
        String fileName = putMessage.getFileName();

        try {
            FileOutputStream fileOutputStream = new FileOutputStream(fileName);

            TransferCompleteMessage complete = Crypto.consumeFile(objectInputStream, fileOutputStream);
            fileOutputStream = new FileOutputStream(fileName + ".sha256");
            fileOutputStream.write(complete.getHash());
        } catch(Exception e) {
            try {
                objectOutputStream.writeObject(new ErrorMessage(new Crypto.SendException()));
            } catch(IOException e1) {
                throw new Crypto.RetrievalException();
            }

        }
        System.out.println("wrote hash");
    }
}
