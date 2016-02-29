
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Arrays;
import java.util.Random;
import java.util.Scanner;

/**
 * Evan O'Connor (eco2116)
 *
 * client.java
 */

public class client {

    public static final String AES_SPEC = "AES";
    public static final String KEY_GENERATION_SPEC = "PBKDF2WithHmacSHA1";

    public static final int AUTH_SIZE = 8;
    public static final int AUTH_ITERATIONS = 32768;
    private static final int PASSWORD_LENGTH = 8;

    public static void main(String[] args) throws Exception {

        //System.setProperty("javax.net.debug", "all");

        System.setProperty("javax.net.ssl.keyStore", "client.jks");
        System.setProperty("javax.net.ssl.keyStorePassword", "password"); // TODO: better password?
        System.setProperty("javax.net.ssl.trustStore", "server.jks");
        System.setProperty("javax.net.ssl.trustStorePassword", "password");

        SocketFactory sslFactory = SSLSocketFactory.getDefault();
        Socket connection = sslFactory.createSocket(InetAddress.getByName(args[0]), 1234);
        OutputStream out = connection.getOutputStream();
        InputStream in = connection.getInputStream();

        ObjectOutputStream objectOutputStream = new ObjectOutputStream(connection.getOutputStream());
        ObjectInputStream objectInputStream = new ObjectInputStream(connection.getInputStream());

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
                                    TransferCompleteMessage complete = Crypto.decryptFile(splitCmd[3].toCharArray(),
                                            objectInputStream, file.getName());
                                    // TODO: can refactor this
                                    byte[] fileBytes = Crypto.extractBytesFromFile(file);
                                    byte[] clientHash = Crypto.generateHash(Crypto.HASHING_ALGORITHM, fileBytes);
                                    byte[] serverHash = complete.getHash();

                                    if(!Arrays.equals(clientHash, serverHash)) {
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
//                                    byte[] hash = Crypto.generateHash(Crypto.HASHING_ALGORITHM, Crypto.extractBytesFromFile(file));
//                                    byte[] fileBytes = Crypto.extractBytesFromFile(file);
//                                    objectOutputStream.writeObject(new PutMessage(file.getName(), fileBytes, hash));
                                    Crypto.sendFile(file, objectOutputStream, splitCmd[3]);
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

                                    byte[] fileBytes = Crypto.extractBytesFromFile(file);
                                    byte[] clientHash = Crypto.generateHash(Crypto.HASHING_ALGORITHM, fileBytes);
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
//                                    byte[] hash = Crypto.generateHash(Crypto.HASHING_ALGORITHM, Crypto.extractBytesFromFile(file));
//                                    byte[] fileBytes = Crypto.extractBytesFromFile(file);
//                                    objectOutputStream.writeObject(new PutMessage(file.getName(), fileBytes, hash));
                                    Crypto.sendFile(file, objectOutputStream, null);
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


    public static Crypto.Keys generateKeysFromPassword(int size, char[] pass, byte[] salt) throws NoSuchAlgorithmException,
            InvalidKeySpecException {

        // Initialize and generate secret keys from password and pseudorandom salt
        SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance(KEY_GENERATION_SPEC);
        KeySpec keySpec = new PBEKeySpec(pass, salt, AUTH_ITERATIONS, size + AUTH_SIZE * 8);
        SecretKey tmpKey = secretKeyFactory.generateSecret(keySpec);
        byte[] key = tmpKey.getEncoded();

        // Save encryption and authorization keys in crypto.Keys static storage class
        SecretKey auth = new SecretKeySpec(Arrays.copyOfRange(key, 0, AUTH_SIZE), AES_SPEC);
        SecretKey enc = new SecretKeySpec(Arrays.copyOfRange(key, AUTH_SIZE, key.length), AES_SPEC);
        return new Crypto.Keys(enc, auth);
    }
}
