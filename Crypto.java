import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;
import java.security.spec.KeySpec;
import java.util.Arrays;

/**
 * Evan O'Connor (eco2116)
 * Crypto.java
 *
 * Crypto is a helper class with methods and custom exceptions related to sending/receiving data,
 * encryption/decryption, and hashing plaintext files
 */
public class Crypto {
    public static final String AES_SPEC = "AES";
    public static final String KEY_GENERATION_SPEC = "PBKDF2WithHmacSHA1";
    public static final String HASHING_ALGORITHM = "SHA-256";
    public static final String SHA_EXTENSION = ".sha256";

    public static final int AUTH_SIZE = 8;
    public static final int AUTH_ITERATIONS = 32768;
    public static final int SALT_SIZE = 16;

    private static final String CIPHER_SPEC = "AES/CBC/PKCS5Padding";
    private static final String ENCODING_TYPE = "UTF-8";
    private static final String CERTIFICATE_EXTENSION = ".jks";

    private static final int AES_KEY_LENGTH = 128;
    private static final int MAX_PORT = 65536;
    private static final int BITS_PER_BYTE = 8;
    private static final int IV_SIZE = 16;
    private static final int SHA_SIZE = 256;
    private static final int BUFFER_SIZE = 1024;

    public static byte[] generateByteArrayHash(String type, byte[] bytes) throws NoSuchAlgorithmException {

        // Initialize message digest for given hashing algorithm and file input stream
        MessageDigest messageDigest = MessageDigest.getInstance(type);

        // Perform hash on bytes
        messageDigest.update(bytes, 0, bytes.length);
        return messageDigest.digest();
    }

    public static byte[] generateFileHash(String type, File file) throws NoSuchAlgorithmException, IOException {

        FileInputStream fileInputStream = new FileInputStream(file);

        // Initialize message digest for given hashing algorithm and file input stream
        MessageDigest messageDigest = MessageDigest.getInstance(type);

        // Continue reading from file and hashing bytes
        byte[] buff = new byte[BUFFER_SIZE];
        int read;
        while((read = fileInputStream.read(buff)) >= 0) {
            messageDigest.update(buff, 0, read);
        }

        // Digest hashed bytes
        return messageDigest.digest();
    }

    public static void sendFile(File file, ObjectOutputStream objectOutputStream, String password, boolean generateHash) throws
            IOException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeySpecException,
            InvalidKeyException, InvalidParameterSpecException, IllegalBlockSizeException, BadPaddingException {

        // Alert other party that we are sending a file
        objectOutputStream.writeObject(new PutMessage(file.getName()));
        FileInputStream fileInputStream = new FileInputStream(file);

        byte[] buff = new byte[BUFFER_SIZE];
        int read;

        // Encrypt the file if password provided
        if(password != null) {

            // Use an empty salt so password same password can always decrypt file
            byte[] salt = new byte[SALT_SIZE];

            // Generate deterministic random number for key by hashing password with sha-256
            byte[] hashPwd =  generateByteArrayHash(HASHING_ALGORITHM, password.getBytes());
            char[] charHash = new String(hashPwd, ENCODING_TYPE).toCharArray();

            // Generate secret for encryption
            SecretKey secret = generateKeysFromPassword(AES_KEY_LENGTH, charHash, salt);
            Cipher encrCipher;

            // Initialize AES cipher
            encrCipher = Cipher.getInstance(CIPHER_SPEC);
            encrCipher.init(Cipher.ENCRYPT_MODE, secret);

            // Generate and send initialization vector
            byte[] iv = encrCipher.getParameters().getParameterSpec(IvParameterSpec.class).getIV();
            objectOutputStream.writeObject(new DataMessage(iv));
            objectOutputStream.reset();

            // Continue reading from file, encrypting and sending to other party
            byte[] encr;
            while ((read = fileInputStream.read(buff)) >= 0) {
                encr = encrCipher.update(buff, 0, read);
                if(encr != null) {
                    DataMessage dm = new DataMessage(encr);
                    objectOutputStream.writeObject(dm);

                    // Must reset object output stream due to caching
                    objectOutputStream.reset();
                }
            }
            // Perform encryption on final block
            encr = encrCipher.doFinal();
            if(encr != null) {
                DataMessage dm = new DataMessage(encr);
                objectOutputStream.writeObject(dm);
                objectOutputStream.reset();
            }

        } else { // Send file without encryption

            // Read in full chunks of data and send to other party; remaining bytes will be sent later
            while ((read = fileInputStream.read(buff)) >= 0) {
                if(read < BUFFER_SIZE) {
                    break;
                } else {
                    DataMessage dm = new DataMessage(buff);
                    objectOutputStream.writeObject(dm);
                    objectOutputStream.reset();
                    objectOutputStream.flush();
                }
            }
        }
        fileInputStream.close();

        byte[] hashBytes;
        if(generateHash) {
            // Generate hash of plaintext if the client called put
            hashBytes = Crypto.generateFileHash(Crypto.HASHING_ALGORITHM, file);
        } else {
            // Receive stored hash on server side
            fileInputStream = new FileInputStream(file.getName() + SHA_EXTENSION);
            hashBytes = new byte[SHA_SIZE / BITS_PER_BYTE];
            fileInputStream.read(hashBytes, 0, SHA_SIZE / BITS_PER_BYTE);
            fileInputStream.close();
        }

        if(read > 0) {
            // If partial data chunk remaining, include it in the final message
            objectOutputStream.writeObject(new TransferCompleteMessage(hashBytes, Arrays.copyOf(buff, read)));
        } else {
            // All data was digested in full chunks, no need to send extra data
            objectOutputStream.writeObject(new TransferCompleteMessage(hashBytes, null));
        }
    }

    public static TransferCompleteMessage consumeFile(ObjectInputStream objectInputStream, FileOutputStream fileOutputStream)
            throws IOException, ClassNotFoundException {

        // Read in either data or transfer complete method if no full data blocks were received
        Message msg = (Message) objectInputStream.readObject();
        while(msg.getType() == Message.MessageType.DATA) {

            // Continue to read in full chunks of data and write to disk
            DataMessage dataMessage = (DataMessage) msg;
            byte[] dataChunk = dataMessage.getData();
            fileOutputStream.write(dataChunk);
            msg = (Message) objectInputStream.readObject();
        }

        // Finished reading full data chunks
        TransferCompleteMessage complete = null;

        // Read remaining partial chunk of data if it exists and write to disk
        byte[] finalBytes = null;
        if(msg.getType() == Message.MessageType.TRANSFER_COMPLETE) {
            complete = (TransferCompleteMessage) msg;
            finalBytes = complete.getFinalData();
        }
        if(finalBytes != null) {
            fileOutputStream.write(finalBytes);
        }
        fileOutputStream.close();
        return complete;
    }

    public static TransferCompleteMessage decryptFile(String password, ObjectInputStream objectInputStream, String name)
            throws IOException, NoSuchAlgorithmException, InvalidKeySpecException, ClassNotFoundException, NoSuchPaddingException,
                InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {

        FileOutputStream fileOutputStream = new FileOutputStream(name);

        // Use empty salt so given password can always decrypt file
        byte[] saltBytes = new byte[Crypto.SALT_SIZE];

        // Deterministically hash the password and generate the secret key from it
        byte[] hashPwd =  generateByteArrayHash(HASHING_ALGORITHM, password.getBytes());
        char[] charHash = new String(hashPwd, ENCODING_TYPE).toCharArray();
        SecretKey secret = Crypto.generateKeysFromPassword(AES_KEY_LENGTH, charHash, saltBytes);

        // Read in first chunk of data which contains the initialization vector and more bytes of data
        DataMessage dataMessage = (DataMessage) objectInputStream.readObject();
        byte[] data = dataMessage.getData();
        byte[] iv = Arrays.copyOfRange(data, 0, IV_SIZE);
        byte[] rest = Arrays.copyOfRange(data, IV_SIZE, data.length);

        // Create decryption cipher using secret key and IV read
        Cipher decrpytCipher = Cipher.getInstance(Crypto.CIPHER_SPEC);
        decrpytCipher.init(Cipher.DECRYPT_MODE, secret, new IvParameterSpec(iv));

        // Decrypt the remaining bytes sent with the IV
        byte[] decrypt = decrpytCipher.update(rest, 0, rest.length);
        if(decrypt != null) {
            fileOutputStream.write(decrypt);
        }

        // Continuously read in chunks of data and decrypt
        Message msg;
        while((msg = (Message) objectInputStream.readObject()).getType() == Message.MessageType.DATA) {
            dataMessage = (DataMessage) msg;
            decrypt = decrpytCipher.update(dataMessage.getData(), 0, dataMessage.getData().length);
            fileOutputStream.write(decrypt);
        }
        fileOutputStream.flush();

        // Final message received with the verification hash and possibly some more bytes of data
        TransferCompleteMessage transferCompleteMessage = (TransferCompleteMessage) msg;

        // If it exists, decrypt and write remaining bytes
        byte[] finalData = transferCompleteMessage.getFinalData();
        decrypt = decrpytCipher.update(finalData, 0, finalData.length);
        if(decrypt != null) {
            fileOutputStream.write(decrypt);
        }
        fileOutputStream.flush();

        // Decrypt and write final block
        decrypt = decrpytCipher.doFinal();
        if(decrypt != null) {
            fileOutputStream.write(decrypt);
        }
        fileOutputStream.flush();
        fileOutputStream.close();
        return transferCompleteMessage;
    }

    public static SecretKey generateKeysFromPassword(int size, char[] pass, byte[] salt) throws NoSuchAlgorithmException,
            InvalidKeySpecException {

        // Initialize and generate secret keys from hashed password and salt
        SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance(KEY_GENERATION_SPEC);
        KeySpec keySpec = new PBEKeySpec(pass, salt, AUTH_ITERATIONS, size + AUTH_SIZE * BITS_PER_BYTE);
        SecretKey tmpKey = secretKeyFactory.generateSecret(keySpec);
        byte[] key = tmpKey.getEncoded();
        return new SecretKeySpec(Arrays.copyOfRange(key, AUTH_SIZE, key.length), AES_SPEC);
    }

    public static String validateCertFileName(String input) {
        // Check for correct file extension
        if(!input.endsWith(CERTIFICATE_EXTENSION)) {
            System.out.println("Invalid certificate file. Expected .jks extension.");
            System.exit(1);
        }
        // Check that file is readable
        File validate = new File(input);
        if(!validate.canRead()) {
            System.out.println("Cannot read from file: " + input);
            System.exit(1);
        }
        return input;
    }

    public static InetAddress validateIP(String input) {
        InetAddress address = null;
        try {
            // Check if host exists
            address = InetAddress.getByName(input);
        } catch (UnknownHostException e) {
            System.out.println("Could not find IP address/host name: " + input);
            System.exit(1);
        }
        return address;
    }

    public static int validatePort(String input) {
        int port = 0;
        try {
            // Check that port is an integer
            port = Integer.parseInt(input);
        } catch(NumberFormatException e) {
            System.out.println("Port must be an integer");
            System.exit(1);
        }
        // Check that port is in valid range
        if(port > MAX_PORT || port < 0) {
            System.out.println("Port value out of range. Must be <= " + MAX_PORT + " and > 0.");
            System.exit(1);
        }
        return port;
    }

    // Indicates an error related to streams and sockets
    public static class ConnectionException extends Exception {
        public ConnectionException(String msg) {
            super("Client/server connection failed: " + msg);
        }
    }

    // Indicates an error encountered while receiving a file
    public static class RetrievalException extends Exception {
        public RetrievalException() {
            super("Failed to retrieve file.");
        }
    }

    // Indicates an error encountered while sending a file
    public static class SendException extends Exception {
        public SendException() {
            super("Failed to send file.");
        }
    }

    // Indicates an error while sending a stop request
    public static class StopException extends Exception {
        public StopException() { super("Stop request received."); }
    }
}
