
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

    public static void main(String[] args) {
        try {
            Socket connection = setUpInteraction(args);
            interactWithServer(connection);
            connection.close();
        } catch(IOException e) {
            System.out.println("Failed to interact with server: Unexpected exception");
        } catch(Crypto.ConnectionException e) {
            System.out.println(e.getMessage());
        }
    }

    private static Socket setUpInteraction(String[] args) throws Crypto.ConnectionException {
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
        try {
            return sslFactory.createSocket(host, port);
        } catch(IOException e) {
            throw new Crypto.ConnectionException("Failed to create socket on client side.");
        }
    }

    private static void handleGetEncrypted(ObjectOutputStream objectOutputStream, ObjectInputStream objectInputStream,
                                           String[] splitCmd) throws Crypto.RetrievalException, Crypto.ConnectionException {
        try {
            objectOutputStream.writeObject(new GetMessage(splitCmd[1]));
            Message message = (Message) objectInputStream.readObject();
            if(message.getType() == Message.MessageType.ERROR) {
                System.out.println(((ErrorMessage) message).getException().getMessage());
            } else if(message.getType() == Message.MessageType.PUT) {
                PutMessage putMessage = (PutMessage) message;
                File file = new File(putMessage.getFileName());
                TransferCompleteMessage complete = Crypto.decryptFile(splitCmd[3], objectInputStream, file.getName());
                // TODO: can refactor this
                byte[] clientHash = Crypto.generateFileHash(Crypto.HASHING_ALGORITHM, file);
                byte[] serverHash = complete.getHash();

                if(!Arrays.equals(clientHash, serverHash)) {
                    System.out.println("Calculated hash did not match hash server sent.");
                    if(!file.delete()) {
                        System.out.println("Failed to remove corrupted file.");
                    }
                } else {
                    System.out.println("they matched.");
                }
            }
        } catch(Exception e) {
            try {
                objectOutputStream.writeObject(new ErrorMessage(e));
            } catch(IOException e1) {
                throw new Crypto.ConnectionException("Unexpected message.");
            }
            throw new Crypto.RetrievalException();
        }
    }

    private static void handlePutEncrypted(ObjectOutputStream objectOutputStream, String[] splitCmd) throws Crypto.SendException {
        File file = new File(splitCmd[1]);
        try {
            if(!file.exists() || !file.canRead()) {
                System.out.println("Failed to access file: " + splitCmd[1]);
                objectOutputStream.writeObject(new ErrorMessage(new PutMessage.PutFileNotFoundException()));
            } else {
                Crypto.sendFile(file, objectOutputStream, splitCmd[3], true);
                System.out.println("sent a put request");
            }
        } catch(Exception e) {
            throw new Crypto.SendException();
        }
    }

    private static void handleGetUnencrypted(ObjectOutputStream objectOutputStream, ObjectInputStream objectInputStream,
                                             String[] splitCmd) throws Crypto.RetrievalException, Crypto.ConnectionException {
        try {
            objectOutputStream.writeObject(new GetMessage(splitCmd[1]));
            Message message = (Message) objectInputStream.readObject();
            if(message.getType() == Message.MessageType.ERROR) {
                System.out.println(((ErrorMessage) message).getException().getMessage());
            } else if(message.getType() == Message.MessageType.PUT) {
                PutMessage putMessage = (PutMessage) message;
                File file = new File(putMessage.getFileName());
                TransferCompleteMessage complete = Crypto.consumeFile(objectInputStream, new FileOutputStream(file));

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
        } catch(Exception e) {
            try {
                objectOutputStream.writeObject(new ErrorMessage(e));
            } catch(IOException e1) {
                throw new Crypto.ConnectionException("Failed to write message.");
            }
            throw new Crypto.RetrievalException();
        }
    }

    private static void handlePutUnencrypted(ObjectOutputStream objectOutputStream, String[] splitCmd)
            throws Crypto.ConnectionException, Crypto.SendException {

        try {
            File file = new File(splitCmd[1]);
            if(!file.exists() || !file.canRead()) {
                System.out.println("Failed to access file: " + splitCmd[1]);
                objectOutputStream.writeObject(new ErrorMessage(new PutMessage.PutFileNotFoundException()));
            } else {
                Crypto.sendFile(file, objectOutputStream, null, true);
                System.out.println("sent a put request");
            }
        } catch(Exception e) {
            try {
                objectOutputStream.writeObject(new ErrorMessage(e));
            } catch(IOException e1) {
                throw new Crypto.ConnectionException("Failed to write message.");
            }
            throw new Crypto.SendException();
        }

    }

    private static void handleEncryptedCommand(String[] splitCmd, ObjectInputStream objectInputStream,
                                               ObjectOutputStream objectOutputStream) {
        if (splitCmd.length > 4) {
            System.out.println("Too many parameters for request with encryption.");
            System.out.println("Only filename, \"E\" or \"N\", and password required.");
        } else if (splitCmd.length < 4) {
            System.out.println("Missing parameters for request with encryption.");
            System.out.println("Filename, \"E\" or \"N\", and password required.");
        } else {
            System.out.println("pwd bytes : " + splitCmd[3].getBytes().length);
            if (splitCmd[3].getBytes().length != PASSWORD_LENGTH) {
                System.out.println("Password must be 8 bytes long.");
            } else if (splitCmd[0].equals("get")) {
                // Get with encryption
                try {
                    handleGetEncrypted(objectOutputStream, objectInputStream, splitCmd);
                } catch(Crypto.RetrievalException e) {
                    File file = new File(splitCmd[1]);
                    if(!file.delete()) {
                        System.out.println("Failed to remove corrupted file.");
                    }
                    System.out.println(e.getMessage());
                } catch(Crypto.ConnectionException e) {
                    System.out.println(e.getMessage());
                }
            } else {
                // Put with encryption
                try {
                    handlePutEncrypted(objectOutputStream, splitCmd);
                } catch(Crypto.SendException e) {
                    System.out.println(e.getMessage());
                }

            }
        }
    }

    private static void handleUnencryptedCommand(String[] splitCmd, ObjectInputStream objectInputStream,
                                                 ObjectOutputStream objectOutputStream) {
        if(splitCmd.length > 3) {
            System.out.println("Too many parameters for request without encryption.");
            System.out.println("Only filename and \"E\" or \"N\".");
        } else {
            if(splitCmd[0].equals("get")) {
                try {
                    // Get without encryption
                    handleGetUnencrypted(objectOutputStream, objectInputStream, splitCmd);
                } catch(Crypto.RetrievalException e) {
                    File file = new File(splitCmd[1]);
                    if(!file.delete()) {
                        System.out.println("Failed to remove corrupted file.");
                    }
                    System.out.println(e.getMessage());
                } catch(Crypto.ConnectionException e) {
                    System.out.println(e.getMessage());
                    System.exit(1);
                }
            } else {
                try {
                    // Put without encryption
                    handlePutUnencrypted(objectOutputStream, splitCmd);
                } catch(Crypto.ConnectionException e) {
                    System.out.println(e.getMessage());
                } catch(Crypto.SendException e) {
                    System.out.println(e.getMessage());
                }
            }
        }
    }

    private static void interactWithServer(Socket connection) throws Crypto.ConnectionException {
        // Create object streams for sending/reading Message objects
        ObjectOutputStream objectOutputStream = null;
        ObjectInputStream objectInputStream = null;
        try {
            objectOutputStream = new ObjectOutputStream(connection.getOutputStream());
            objectInputStream = new ObjectInputStream(connection.getInputStream());
        } catch(IOException e) {
            System.out.println("Failed to set up stream with server.");
            System.exit(1);
        }

        // Read in user commands until stop is requested
        Scanner input = new Scanner(System.in);
        while(true) {
            System.out.print("> ");
            String userCmd = input.nextLine();
            String[] splitCmd = userCmd.split(" ");
            for(String s : splitCmd) {
                System.out.println("cmd" + s);
            }

            if(splitCmd.length == 0) {
                System.out.println("Please enter a command.");
            } else {
                // Stop
                if(splitCmd[0].equals("stop")) {
                    if(splitCmd.length == 1) {
                        try {
                            objectOutputStream.writeObject(new StopMessage());
                        } catch(IOException e) {
                            throw new Crypto.ConnectionException("Failed to write stop message.");
                        }
                        System.out.println("Goodbye!");
                        break;
                    } else {
                        System.out.println("\"stop\" does not accept parameters. Try again.");
                    }
                } else if(splitCmd[0].equals("get") || splitCmd[0].equals("put")) {
                    if(splitCmd.length < 3) {
                        System.out.println("Missing parameters. Minimum of filename and \"E\" or \"N\" required.");

                        // with encryption
                    } else if(splitCmd[2].equals("E")) {
                        handleEncryptedCommand(splitCmd, objectInputStream, objectOutputStream);

                        // without encryption
                    } else if(splitCmd[2].equals("N")) {
                        handleUnencryptedCommand(splitCmd, objectInputStream, objectOutputStream);
                    } else {
                        System.out.println("Second parameter for get request must be \"E\" or \"N\"");
                    }
                } else {
                    System.out.println("Invalid command: " + splitCmd[0] + ". Expected \"stop\", \"put\", or \"get\".");
                }
            }
        }
    }
}
