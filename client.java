
import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Scanner;

/**
 * Evan O'Connor (eco2116)
 *
 * client.java
 */

public class client {

    private static final int PASSWORD_LENGTH = 8;
    private static final int NUM_ARGS = 6;

    public static void main(String[] args) throws Exception {

        // Validate command line arguments
        if(args.length != NUM_ARGS) {
            System.out.println("Incorrect number of arguments.");
            System.out.println("Usage: java client <host> <port> <keyStore> <keyStorePassword> <trustStorePassword> <trustStore>");
            System.exit(1);
        }
        InetAddress host = Crypto.validateIP(args[0]);
        int port = Crypto.validatePort(args[1]);
        String keyStore = Crypto.validateCertFileName(args[2]);
        String keyStorePassword = args[3];
        String trustStore = Crypto.validateCertFileName(args[4]);
        String trustStorePassword = args[5];

        //System.setProperty("javax.net.debug", "all");

        // Set up system properties needed for mutual authentication
        System.setProperty("javax.net.ssl.keyStore", keyStore);
        System.setProperty("javax.net.ssl.keyStorePassword", keyStorePassword); // TODO: better password?
        System.setProperty("javax.net.ssl.trustStore", trustStore);
        System.setProperty("javax.net.ssl.trustStorePassword", trustStorePassword);

        // Create SSL sockets and streams
        SocketFactory sslFactory = SSLSocketFactory.getDefault();
        Socket connection = sslFactory.createSocket(host, port);
        OutputStream out = connection.getOutputStream();
        InputStream in = connection.getInputStream();

        // Create object streams for sending/reading Message objects
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(connection.getOutputStream());
        ObjectInputStream objectInputStream = new ObjectInputStream(connection.getInputStream());

        // Read in user commands until stop is requested
        Scanner input = new Scanner(System.in);
        while(true) {
            System.out.print("> ");
            String userCmd = input.nextLine();
            String[] splitCmd = userCmd.split(" ");
            if(splitCmd.length == 0) {
                System.out.println("Please enter a command.");
            } else {

                // Stop
                if(splitCmd[0].equals("stop")) {
                    if(splitCmd.length == 1) {
                        objectOutputStream.writeObject(new StopMessage());
                        System.out.println("Goodbye!");
                        break;
                    } else {
                        System.out.println("\"stop\" does not accept parameters. Try again.");
                        continue;
                    }

                } else if(splitCmd[0].equals("get") || splitCmd[0].equals("put")) {
                    if(splitCmd.length < 3) {
                        System.out.println("Missing parameters. Minimum of filename and \"E\" or \"N\" required.");
                        continue;

                    // with encryption
                    } else if(splitCmd[2].equals("E")) {
                        if (splitCmd.length > 4) {
                            System.out.println("Too many parameters for request with encryption.");
                            System.out.println("Only filename, \"E\" or \"N\", and password required.");
                            continue;
                        } else if (splitCmd.length < 4) {
                            System.out.println("Missing parameters for request with encryption.");
                            System.out.println("Filename, \"E\" or \"N\", and password required.");
                            continue;
                        } else {
                            if (splitCmd[3].length() != PASSWORD_LENGTH) {
                                System.out.println("Password must be 8 characters long.");
                                continue;
                            } else if (splitCmd[0].equals("get")) {
                                objectOutputStream.writeObject(new GetMessage(splitCmd[1]));
                                Message message = (Message) objectInputStream.readObject();
                                if(message.getType() == Message.MessageType.ERROR) {
                                    System.out.println(((ErrorMessage) message).getException().getMessage());
                                    continue;
                                } else if(message.getType() == Message.MessageType.PUT) {
                                    PutMessage putMessage = (PutMessage) message;
                                    File file = new File(putMessage.getFileName());
                                    TransferCompleteMessage complete = Crypto.decryptFile(splitCmd[3],
                                            objectInputStream, file.getName());
                                    // TODO: can refactor this
                                    byte[] clientHash = Crypto.generateFileHash(Crypto.HASHING_ALGORITHM, file);
                                    byte[] serverHash = complete.getHash();

                                    if(!Arrays.equals(clientHash, serverHash)) {
                                        // TODO: delete file
                                        System.out.println("Calculated hash did not match hash server sent.");
                                    } else {
                                        System.out.println("they matched.");
                                    }
                                }


                            } else {
                                File file = new File(splitCmd[1]);
                                if(!file.exists() || !file.canRead()) {
                                    System.out.println("Failed to access file: " + splitCmd[1]);
                                    objectOutputStream.writeObject(new ErrorMessage(new PutMessage.PutFileNotFoundException()));
                                    continue;
                                } else {
                                    Crypto.sendFile(file, objectOutputStream, splitCmd[3], true);
                                    System.out.println("sent a put request");
                                }
                            }
                        }
                    // without encryption
                    } else if(splitCmd[2].equals("N")) {
                        if(splitCmd.length > 3) {
                            System.out.println("Too many parameters for request without encryption.");
                            System.out.println("Only filename and \"E\" or \"N\".");
                            continue;
                        } else {
                            if(splitCmd[0].equals("get")) {
                                objectOutputStream.writeObject(new GetMessage(splitCmd[1]));
                                Message message = (Message) objectInputStream.readObject();
                                if(message.getType() == Message.MessageType.ERROR) {
                                    System.out.println(((ErrorMessage) message).getException().getMessage());
                                    continue;
                                } else if(message.getType() == Message.MessageType.PUT) {
                                    PutMessage putMessage = (PutMessage) message;
                                    File file = new File(putMessage.getFileName());
                                    TransferCompleteMessage complete = Crypto.consumeFile(objectInputStream,
                                            new FileOutputStream(file));


                                    byte[] clientHash = Crypto.generateFileHash(Crypto.HASHING_ALGORITHM, file);
                                    byte[] serverHash = complete.getHash();

                                    if(!Arrays.equals(clientHash, serverHash)) {
                                        System.out.println("Calculated hash did not match hash server sent.");
                                    } else {
                                        System.out.println("they matched.");
                                    }
                                } else {
                                    System.out.println("Did not understand message from server.");
                                }

                            // Put without encryption
                            } else {
                                File file = new File(splitCmd[1]);
                                if(!file.exists() || !file.canRead()) {
                                    System.out.println("Failed to access file: " + splitCmd[1]);
                                    objectOutputStream.writeObject(new ErrorMessage(new PutMessage.PutFileNotFoundException()));
                                    continue;
                                } else {
                                    Crypto.sendFile(file, objectOutputStream, null, true);
                                    System.out.println("sent a put request");
                                }
                            }
                        }
                    } else {
                        System.out.println("Second parameter for get request must be \"E\" or \"N\"");
                        continue;
                    }
                } else {
                    System.out.println("Invalid command: " + splitCmd[0] + ". Expected \"stop\", \"put\", or \"get\".");
                }
            }

        }
        in.close();
        out.close();
        connection.close();
    }


}
