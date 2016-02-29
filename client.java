
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
    private static final int BUFFER_SIZE = 1024;

    public static void main(String[] args) throws IOException, ClassNotFoundException, NoSuchAlgorithmException {

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
                            } else if (splitCmd[0].equals("get")){
                                objectOutputStream.writeObject(new GetMessage(splitCmd[1]));
                            } else {
                                //objectOutputStream.writeObject(new PutMessage(splitCmd[1]));
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
                                } else if(message.getType() == Message.MessageType.GET) {
                                    GetMessage getMessage = (GetMessage) message;
                                    System.out.println("Get received " + getMessage.getFileName());
                                    byte[] fileBytes = getMessage.getFileBytes();
                                    byte[] clientHash = Crypto.generateHash(Crypto.HASHING_ALGORITHM, fileBytes);
                                    byte[] serverHash = getMessage.getHashBytes();

                                    if(!Arrays.equals(clientHash, serverHash)) {
                                        System.out.println("Calculated hash did not match hash server sent.");
                                    } else {
                                        FileOutputStream fileOutputStream = new FileOutputStream(getMessage.getFileName());
                                        fileOutputStream.write(fileBytes);
                                        fileOutputStream.close();
                                        System.out.println("Wrote new file ");

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
                                    sendFile(file, objectOutputStream);
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

    private static byte[] generateRandomSalt(int size, String password) {
        Random random = new Random();
        byte[] saltBytes = new byte[size];

        random.nextBytes(password.getBytes());
        return saltBytes;
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

    private static void sendFile(File file, ObjectOutputStream objectOutputStream) throws IOException, NoSuchAlgorithmException {

        long size = file.length();
        System.out.println("total length " + size);
        objectOutputStream.writeObject(new PutMessage(file.getName(), size));

        FileInputStream fileInputStream = new FileInputStream(file);
        byte[] buff = new byte[BUFFER_SIZE];
        int read;
        int count = 0;
        while ((read = fileInputStream.read(buff)) > 0) {
            System.out.println("read " + read);
            if(read < BUFFER_SIZE) {
                System.out.println("less than buffer");
                break;
            } else {
                System.out.println("writing data msg");
                objectOutputStream.writeObject(new DataMessage(buff, count++));
            }
        }
        System.out.println("out of while loop and read is " + read);
        byte[] fileBytes = Crypto.extractBytesFromFile(file);
        byte[] hashBytes = Crypto.generateHash(Crypto.HASHING_ALGORITHM, fileBytes);
        if(read > 0) {
            objectOutputStream.writeObject(new TransferCompleteMessage(hashBytes, Arrays.copyOf(buff, read)));
        } else {
            System.out.println("null final bytes");
            objectOutputStream.writeObject(new TransferCompleteMessage(hashBytes, null));
        }
        System.out.println("wrote transfer complete ");

    }

    /*
    private static void encryptFile(int keySize, char[] pass, InputStream inputStream, OutputStream outputStream, long fileSize)
            throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, InvalidParameterSpecException,
                    IOException, IllegalBlockSizeException, BadPaddingException, InvalidKeySpecException {

        // Send server the size in bytes of the encrypted file to be read
        byte[] bytes = ByteBuffer.allocate(Long.SIZE / Byte.SIZE).putLong(fileSize + PAD_SIZE).array();
        outputStream.write(bytes);

        // Generate salt and keys (for authentication and encryption)
        byte[] salt = generateRandomSalt(crypto.SALT_SIZE);
        crypto.Keys secret = crypto.generateKeysFromPassword(keySize, pass, salt);

        Cipher encrCipher;

        // Initialize AES cipher
        encrCipher = Cipher.getInstance(CIPHER_SPEC);
        encrCipher.init(Cipher.ENCRYPT_MODE, secret.encr);

        // Generate initialization vector
        byte[] iv = encrCipher.getParameters().getParameterSpec(IvParameterSpec.class).getIV();

        // Send authentication and AES initialization data
        outputStream.write(salt);
        outputStream.write(secret.auth.getEncoded());
        outputStream.write(iv);

        // Use a buffer to send chunks of encrypted data to server
        byte[] buff = new byte[BUFF_SIZE];
        int read;
        byte[] encr;

        while ((read = inputStream.read(buff)) > 0) {
            encr = encrCipher.update(buff, 0, read);
            if(encr != null) {
                outputStream.write(encr);
            }
        }
        // Final encryption block
        encr = encrCipher.doFinal();
        if(encr != null) {
            outputStream.write(encr);
        }
    }
     */

}
