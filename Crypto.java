
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

public class Crypto {
    public static final String AES_SPEC = "AES";
    public static final String KEY_GENERATION_SPEC = "PBKDF2WithHmacSHA1";
    private static final String CIPHER_SPEC = "AES/CBC/PKCS5Padding";
    private static final int AES_KEY_LENGTH = 128;

    public static final int AUTH_SIZE = 8;
    public static final int AUTH_ITERATIONS = 32768;
    public static final int SALT_SIZE = 16;

    public static final String HASHING_ALGORITHM = "SHA-256";
    private static final int BUFFER_SIZE = 1024;

    public static byte[] generateByteArrayHash(String type, byte[] bytes) throws NoSuchAlgorithmException {

        // Initialize message digest for given hashing algorithm and file input stream
        MessageDigest messageDigest = MessageDigest.getInstance(type);
        messageDigest.update(bytes, 0, bytes.length);
        return messageDigest.digest();

    }

    public static byte[] generateFileHash(String type, File file) throws NoSuchAlgorithmException, IOException {

        FileInputStream fileInputStream = new FileInputStream(file);

        // Initialize message digest for given hashing algorithm and file input stream
        MessageDigest messageDigest = MessageDigest.getInstance(type);

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

        long size = file.length();
        objectOutputStream.writeObject(new PutMessage(file.getName()));
        FileInputStream fileInputStream = new FileInputStream(file);

        byte[] buff = new byte[BUFFER_SIZE];
        int read;

        if(password != null) {
            byte[] salt = new byte[SALT_SIZE];
            byte[] hashPwd =  generateByteArrayHash(HASHING_ALGORITHM, password.getBytes());
            char[] charHash = new String(hashPwd, "UTF-8").toCharArray();

            SecretKey secret = generateKeysFromPassword(AES_KEY_LENGTH, charHash, salt);

            Cipher encrCipher;

            // Initialize AES cipher
            encrCipher = Cipher.getInstance(CIPHER_SPEC);
            encrCipher.init(Cipher.ENCRYPT_MODE, secret);

            // Generate initialization vector
            byte[] iv = encrCipher.getParameters().getParameterSpec(IvParameterSpec.class).getIV();

            objectOutputStream.writeObject(new DataMessage(iv));
            objectOutputStream.reset();

            byte[] encr;

            while ((read = fileInputStream.read(buff)) >= 0) {
                encr = encrCipher.update(buff, 0, read);
                if(encr != null) {
                    DataMessage dm = new DataMessage(encr);
                    objectOutputStream.writeObject(dm);
                    objectOutputStream.reset();
                }
            }
            // Final encryption block
            encr = encrCipher.doFinal();
            if(encr != null) {
                DataMessage dm = new DataMessage(encr);
                objectOutputStream.writeObject(dm);
                objectOutputStream.reset();
            }

        } else {
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
        byte[] hashBytes = null;
        if(generateHash) {
            hashBytes = Crypto.generateFileHash(Crypto.HASHING_ALGORITHM, file);
        } else {
            fileInputStream = new FileInputStream(file.getName() + ".sha256");
            hashBytes = new byte[32];
            fileInputStream.read(hashBytes, 0, 32);
            fileInputStream.close();
        }

        if(read > 0) {
            objectOutputStream.writeObject(new TransferCompleteMessage(hashBytes, Arrays.copyOf(buff, read)));
        } else {
            objectOutputStream.writeObject(new TransferCompleteMessage(hashBytes, null));
        }
    }

    public static TransferCompleteMessage consumeFile(ObjectInputStream objectInputStream, FileOutputStream fileOutputStream)
            throws IOException, ClassNotFoundException {

        Message msg = (Message) objectInputStream.readObject();
        while(msg.getType() == Message.MessageType.DATA) {

            DataMessage dataMessage = (DataMessage) msg;
            byte[] dataChunk = dataMessage.getData();
            fileOutputStream.write(dataChunk);
            msg = (Message) objectInputStream.readObject();
        }
        TransferCompleteMessage complete = null;
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

        // Read in salt, keys, and authentication password
        byte[] saltBytes = new byte[Crypto.SALT_SIZE];
        byte[] hashPwd =  generateByteArrayHash(HASHING_ALGORITHM, password.getBytes());
        char[] charHash = new String(hashPwd, "UTF-8").toCharArray();

        SecretKey secret = Crypto.generateKeysFromPassword(AES_KEY_LENGTH, charHash, saltBytes);

        DataMessage dataMessage = (DataMessage) objectInputStream.readObject();
        byte[] data = dataMessage.getData();
        byte[] iv = Arrays.copyOfRange(data, 0, 16);
        byte[] rest = Arrays.copyOfRange(data, 16, data.length);

        Cipher decrpytCipher = Cipher.getInstance(Crypto.CIPHER_SPEC);
        decrpytCipher.init(Cipher.DECRYPT_MODE, secret, new IvParameterSpec(iv));

        // Use a buffer to decrypt and write to disk
        byte[] buff = new byte[Crypto.BUFFER_SIZE];
        int read;
        byte[] decrypt = decrpytCipher.update(rest, 0, rest.length);
        if(decrypt != null) {
            fileOutputStream.write(decrypt);
        }

        Message msg;
        while((msg = (Message) objectInputStream.readObject()).getType() == Message.MessageType.DATA) {
            dataMessage = (DataMessage) msg;
            decrypt = decrpytCipher.update(dataMessage.getData(), 0, dataMessage.getData().length);
            fileOutputStream.write(decrypt);

        }
        fileOutputStream.flush();
        TransferCompleteMessage transferCompleteMessage = (TransferCompleteMessage) msg;

        byte[] finalData = transferCompleteMessage.getFinalData();
        decrypt = decrpytCipher.update(finalData, 0, finalData.length);
        if(decrypt != null) {
            fileOutputStream.write(decrypt);
        }
        fileOutputStream.flush();

        // Decrypt final block
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
        KeySpec keySpec = new PBEKeySpec(pass, salt, AUTH_ITERATIONS, size + AUTH_SIZE * 8);
        SecretKey tmpKey = secretKeyFactory.generateSecret(keySpec);
        byte[] key = tmpKey.getEncoded();

        return new SecretKeySpec(Arrays.copyOfRange(key, AUTH_SIZE, key.length), AES_SPEC);
    }

    public static String validateCertFileName(String input) {
        if(!input.endsWith(".jks")) {
            System.out.println("Invalid certificate file. Expected .jks extension.");
            System.exit(1);
        }
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
            port = Integer.parseInt(input);
        } catch(NumberFormatException e) {
            System.out.println("Port must be an integer");
            System.exit(1);
        }
        if(port > 65536) {
            System.out.println("Port value out of range. Must be <= 6535");
            System.exit(1);
        }
        return port;
    }

    public static class ConnectionException extends Exception {
        public ConnectionException(String msg) {
            super("Client/server connection failed: " + msg);
        }
    }

    public static class RetrievalException extends Exception {
        public RetrievalException() {
            super("Failed to retrieve file.");
        }
    }

    public static class SendException extends Exception {
        public SendException() {
            super("Failed to send file.");
        }
    }

    public static class StopException extends Exception {
        public StopException() { super("Stop request received."); }
    }
}
