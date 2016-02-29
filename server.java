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

    public static void main(String[] args)  {

        //System.setProperty("javax.net.debug", "all");

        // TODO: figure out if its okay to have client.jks here - trust store? accepted certificates?
        System.setProperty("javax.net.ssl.trustStore", "client.jks");
        System.setProperty("javax.net.ssl.trustStorePassword", "password"); // TODO: better password?
        System.setProperty("javax.net.ssl.keyStore", "server.jks");
        System.setProperty("javax.net.ssl.keyStorePassword" , "password");

        try {
            ServerSocketFactory sslFactory = SSLServerSocketFactory.getDefault();
            ServerSocket listenSocket = sslFactory.createServerSocket(1234);
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
                            System.out.println("writing get to client.");
                            byte[] fileBytes = Crypto.extractBytesFromFile(file);
                            byte[] hashBytes = Crypto.extractBytesFromFile(hash);
                            objectOutputStream.writeObject(new GetMessage(fileName, fileBytes, hashBytes));
                        }
                    } else if (cmd.getType() == Message.MessageType.PUT) {

                        PutMessage putMessage = (PutMessage) cmd;
                        long size = putMessage.getTotalSize();
                        String fileName = putMessage.getFileName();
                        FileOutputStream fileOutputStream = new FileOutputStream(fileName + "copy");

                        System.out.println("put message recvd");

                        Message msg = (Message) objectInputStream.readObject();

                        while(msg.getType() == Message.MessageType.DATA) {
                            System.out.println("while msg type " + msg.getType());
                            System.out.println("data recvd ");

                            DataMessage dataMessage = (DataMessage) msg;
                            byte[] dataChunk = dataMessage.getData();
                            System.out.println("data number " + dataMessage.number);
                            System.out.println("wrote " + dataChunk.length);
                            fileOutputStream.write(dataChunk);
                            fileOutputStream.flush();

                            msg = (Message) objectInputStream.readObject();
                        }
                        System.out.println("msg type " + msg.getType());
                        TransferCompleteMessage complete = null;
                        if(msg.getType() == Message.MessageType.TRANSFER_COMPLETE) {
                            System.out.println("if transfer complete");
                            complete = (TransferCompleteMessage) msg;
                        }

                        System.out.println("transfer complete");
                        byte[] finalBytes = complete.getFinalData();

                        if(finalBytes != null) {
                            System.out.println("final bytes length " + finalBytes.length);
                            System.out.println("writing final bytes");
                            fileOutputStream.write(finalBytes);
                        }
                        System.out.println("wrote file");

                        fileOutputStream.close();

//                        fileOutputStream = new FileOutputStream(fileName + ".sha256");
//                        fileOutputStream.write(complete.getHash());
//                        System.out.println("wrote hash");

                    }
                }

                //}
                objectInputStream.close();
                objectOutputStream.close();
                connection.close();
            }

        } catch(Exception e) {
            e.printStackTrace();
        }
    }


}
