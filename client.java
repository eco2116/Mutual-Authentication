
import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Arrays;
import java.util.Scanner;

/**
 * Evan O'Connor (eco2116)
 * client.java
 *
 * client is class that sends or receives encrypted/decrypted data and generates and/or verifies hashes of plaintext
 */

public class client {
// TODO: utf-8 encodable pwd, ascii/binary data, no spaces in file name or pwd
    private static final int PASSWORD_LENGTH = 8;
    private static final int NUM_ARGS = 6;

    public static void main(String[] args) {
        try {
            Socket connection = setUpInteraction(args); // Setup connection with server
            interactWithServer(connection); // Begin interaction with client
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
        // Validate input values
        InetAddress host = Crypto.validateIP(args[0]);
        int port = Crypto.validatePort(args[1]);
        String keyStore = Crypto.validateCertFileName(args[2]);
        String keyStorePassword = args[3];
        String trustStore = Crypto.validateCertFileName(args[4]);
        String trustStorePassword = args[5];

        // Set up system properties needed for mutual authentication
        System.setProperty("javax.net.ssl.keyStore", keyStore);
        System.setProperty("javax.net.ssl.keyStorePassword", keyStorePassword);
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
            // Send message indicating beginning of a get request
            objectOutputStream.writeObject(new GetMessage(splitCmd[1]));
            Message message = (Message) objectInputStream.readObject();

            // Error received from server
            if(message.getType() == Message.MessageType.ERROR) {
                System.out.println(((ErrorMessage) message).getException().getMessage());
            } else if(message.getType() == Message.MessageType.PUT) {

                // Server wants to send a file to client
                PutMessage putMessage = (PutMessage) message;
                File file = new File(putMessage.getFileName());

                // Decrypt the file sent from the server
                TransferCompleteMessage complete = Crypto.decryptFile(splitCmd[3], objectInputStream, file.getName());

                // Validate that calculated hash of plaintext matches hash that server sent
                byte[] clientHash = Crypto.generateFileHash(Crypto.HASHING_ALGORITHM, file);
                byte[] serverHash = complete.getHash();
                if(!Arrays.equals(clientHash, serverHash)) {
                    System.out.println("Calculated hash did not match hash server sent.");

                    if(!file.delete()) { // Remove temporary file because it was not validated
                        System.out.println("Failed to remove corrupted file.");
                    }
                } else {
                    System.out.println("Hashes matched. File written to disk.");
                }
            }
        } catch(Exception e) {
            try {
                // Alert server of an error
                objectOutputStream.writeObject(new ErrorMessage(e));
            } catch(IOException e1) {
                try {
                    objectInputStream.close();
                    objectOutputStream.close();
                } catch(IOException e2) {
                    throw new Crypto.ConnectionException("Failed to close streams.");
                }
                throw new Crypto.ConnectionException("Unexpected message.");
            }
            // Only notify client that retrieval failed; don't indicate why for security reasons
            throw new Crypto.RetrievalException();
        }
    }

    private static void handlePutEncrypted(ObjectOutputStream objectOutputStream, String[] splitCmd) throws Crypto.SendException {
        File file = new File(splitCmd[1]);
        try {
            if(!file.exists() || !file.canRead()) {
                System.out.println("Failed to access file: " + splitCmd[1]);
                // Notify server of error due to unreadable/nonexistent file
                objectOutputStream.writeObject(new ErrorMessage(new PutMessage.PutFileNotFoundException()));
            } else {
                // Send the file with encryption
                Crypto.sendFile(file, objectOutputStream, splitCmd[3], true);
            }
        } catch(Exception e) {
            throw new Crypto.SendException();
        }
    }

    private static void handleGetUnencrypted(ObjectOutputStream objectOutputStream, ObjectInputStream objectInputStream,
                                             String[] splitCmd) throws Crypto.RetrievalException, Crypto.ConnectionException {
        try {
            // Send server message indicating that client wants to get a file
            objectOutputStream.writeObject(new GetMessage(splitCmd[1]));
            Message message = (Message) objectInputStream.readObject();

            // Handle error sent from server
            if(message.getType() == Message.MessageType.ERROR) {
                System.out.println(((ErrorMessage) message).getException().getMessage());
            } else if(message.getType() == Message.MessageType.PUT) {

                // Server wants to send a message to client
                PutMessage putMessage = (PutMessage) message;
                File file = new File(putMessage.getFileName());

                // Read in unencrypted file from server
                TransferCompleteMessage complete = Crypto.consumeFile(objectInputStream, new FileOutputStream(file));

                // Generate a hash of the plaintext and compare it to the hash the server sent
                byte[] clientHash = Crypto.generateFileHash(Crypto.HASHING_ALGORITHM, file);
                byte[] serverHash = complete.getHash();

                if(!Arrays.equals(clientHash, serverHash)) {
                    if(!file.delete()) { // Do not write file if it was corrupted
                        System.out.println("Failed to remove corrupted file.");
                    }
                    System.out.println("Verification failed. Calculated hash did not match hash server sent.");
                } else {
                    System.out.println("Verification passed. File written to disk.");
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
            // If server could not send file, do not notify client of details of failure for security reasons
            throw new Crypto.RetrievalException();
        }
    }

    private static void handlePutUnencrypted(ObjectOutputStream objectOutputStream, String[] splitCmd)
            throws Crypto.ConnectionException, Crypto.SendException {

        try {
            // Make sure file exists on client-side
            File file = new File(splitCmd[1]);
            if(!file.exists() || !file.canRead()) {
                System.out.println("Failed to access file: " + splitCmd[1]);

                // Notify server if client failed to access file
                objectOutputStream.writeObject(new ErrorMessage(new PutMessage.PutFileNotFoundException()));
            } else {
                // Send the unencrypted file to the server - generate a new hash to send to server
                Crypto.sendFile(file, objectOutputStream, null, true);
            }
        } catch(Exception e) {
            try {
                // Notify client if put failed
                objectOutputStream.writeObject(new ErrorMessage(e));
            } catch(IOException e1) {
                throw new Crypto.ConnectionException("Failed to write message.");
            }
            // If failed to send file, do not notify client of details of failure for security reasons
            throw new Crypto.SendException();
        }
    }

    private static void handleEncryptedCommand(String[] splitCmd, ObjectInputStream objectInputStream,
                                               ObjectOutputStream objectOutputStream) {
        // Parse and validate command line arguments for encrypted put/get
        if (splitCmd.length > 4) {
            System.out.println("Too many parameters for request with encryption.");
            System.out.println("Only filename, \"E\" or \"N\", and password required.");
        } else if (splitCmd.length < 4) {
            System.out.println("Missing parameters for request with encryption.");
            System.out.println("Filename, \"E\" or \"N\", and password required.");
        } else {
            // Password must be 8 bytes long
            if (splitCmd[3].getBytes().length != PASSWORD_LENGTH) {
                System.out.println("Password must be 8 bytes long.");
            } else if (splitCmd[0].equals("get")) {
                // Get with encryption
                try {
                    handleGetEncrypted(objectOutputStream, objectInputStream, splitCmd);
                } catch(Crypto.RetrievalException e) {
                    File file = new File(splitCmd[1]);
                    if(!file.delete()) { // If get failed, do not write file to disk
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
        // Parse and validate command line arguments for unencrypted put/get
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
                    if(!file.delete()) { // If get failed, do not write the file to disk
                        System.out.println("Failed to remove corrupted file.");
                    }
                    System.out.println(e.getMessage());
                } catch(Crypto.ConnectionException e) {
                    System.out.println(e.getMessage());
                    try {
                        objectInputStream.close();
                        objectOutputStream.close();
                    } catch(IOException e1) {
                        System.out.println("Unexpected error. Exiting...");
                    }
                    System.exit(1); // Unavoidable exception. Close session.
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

            // Parse client input - note: no parameters can have spaces in them, or this will fail
            String[] splitCmd = userCmd.split(" ");

            if(splitCmd.length == 0) { // No input entered
                System.out.println("Please enter a command.");
            } else {
                // Stop request
                if(splitCmd[0].equals("stop")) {
                    if(splitCmd.length == 1) {
                        try {
                            // Notify server that client wants to end the session
                            objectOutputStream.writeObject(new StopMessage());
                            objectInputStream.close();
                            objectOutputStream.close();
                        } catch(IOException e) {
                            throw new Crypto.ConnectionException("Failed to write stop message.");
                        }
                        System.out.println("Goodbye!");
                        break;
                    } else {
                        System.out.println("\"stop\" does not accept parameters. Try again.");
                    }
                // Get or put request
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
