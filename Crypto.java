import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
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

    public static byte[] generateHash(String type, byte[] bytes) throws NoSuchAlgorithmException, IOException {

        // Initialize message digest for given hashing algorithm and file input stream
        MessageDigest messageDigest = MessageDigest.getInstance(type);

        // TODO: is this okay? no loop?
        messageDigest.update(bytes, 0, bytes.length);

        // Digest hashed bytes
        return messageDigest.digest();
    }

    public static byte[] extractBytesFromFile(File file) throws IOException {
        InputStream inputStream = new FileInputStream(file);

        long length = file.length();
        if(length > Integer.MAX_VALUE) {
            // TODO: File is too large - throw put exception
        }

        byte[] fileBytes = new byte[(int) length];

        int offset = 0;
        int read = 0;
        while(offset < fileBytes.length &&
                (read = inputStream.read(fileBytes, offset, fileBytes.length - offset)) >= 0) {
            offset += read;
        }
        if(offset < fileBytes.length) {
            // TODO - put exception
            throw new IOException("Failed to completely read file " + file.getName());
        }
        inputStream.close();
        return fileBytes;
    }

    public static void sendFile(File file, ObjectOutputStream objectOutputStream, String password) throws Exception {

        long size = file.length();
        System.out.println("total length " + size);

        objectOutputStream.writeObject(new PutMessage(file.getName(), size));
        FileInputStream fileInputStream = new FileInputStream(file);

        byte[] buff = new byte[BUFFER_SIZE];
        int read;

        if(password != null) {
            byte[] salt = new byte[SALT_SIZE];
            byte[] hashPwd =  generateHash(HASHING_ALGORITHM, password.getBytes());
            char[] charHash = new String(hashPwd, "UTF-8").toCharArray();

            Keys secret = generateKeysFromPassword(AES_KEY_LENGTH, charHash, salt);

            Cipher encrCipher;

            // Initialize AES cipher
            encrCipher = Cipher.getInstance(CIPHER_SPEC);
            encrCipher.init(Cipher.ENCRYPT_MODE, secret.encr);

            // Generate initialization vector
            byte[] iv = encrCipher.getParameters().getParameterSpec(IvParameterSpec.class).getIV();
            System.out.println("iv size" + iv.length);

            objectOutputStream.writeObject(new DataMessage(iv));
            objectOutputStream.reset();

            byte[] encr;

            while ((read = fileInputStream.read(buff)) >= 0) {
                System.out.println("encr read " + read);
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
                System.out.println("read " + read);

                if(read < BUFFER_SIZE) {
                    System.out.println("less than buffer");
                    break;
                } else {
                    System.out.println("writing data msg");
                    System.out.println(buff[0]);
                    DataMessage dm = new DataMessage(buff);
                    objectOutputStream.writeObject(dm);
                    objectOutputStream.reset();
                    objectOutputStream.flush();
                }
            }
        }

        byte[] fileBytes = Crypto.extractBytesFromFile(file);
        byte[] hashBytes = Crypto.generateHash(Crypto.HASHING_ALGORITHM, fileBytes);
        if(read > 0) {
            objectOutputStream.writeObject(new TransferCompleteMessage(hashBytes, Arrays.copyOf(buff, read)));
        } else {
            System.out.println("null final bytes");
            objectOutputStream.writeObject(new TransferCompleteMessage(hashBytes, null));
        }
        System.out.println("wrote transfer complete ");
        fileInputStream.close();
    }

    public static TransferCompleteMessage consumeFile(ObjectInputStream objectInputStream,
                                                      FileOutputStream fileOutputStream) throws Exception {

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
            //fileOutputStream.flush();

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
        return complete;
    }

    public static TransferCompleteMessage decryptFile(char[] password, ObjectInputStream objectInputStream,
                                    String name) throws Exception {

        FileOutputStream fileOutputStream = new FileOutputStream(name);

        // Read in salt, keys, and authentication password
        byte[] saltBytes = new byte[Crypto.SALT_SIZE];

        Crypto.Keys keys = Crypto.generateKeysFromPassword(AES_KEY_LENGTH, password, saltBytes);

        DataMessage dataMessage = (DataMessage) objectInputStream.readObject();
        byte[] data = dataMessage.getData();
        byte[] iv = Arrays.copyOfRange(data, 0, 16);
        byte[] rest = Arrays.copyOfRange(data, 16, data.length);

        System.out.println("decryption received iv of size : " + iv.length);

        Cipher decrpytCipher = Cipher.getInstance(Crypto.CIPHER_SPEC);
        decrpytCipher.init(Cipher.DECRYPT_MODE, keys.encr, new IvParameterSpec(iv));

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

            if(decrypt != null) {
                fileOutputStream.write(decrypt);
            }
        }
        fileOutputStream.flush();

        TransferCompleteMessage transferCompleteMessage = (TransferCompleteMessage) objectInputStream.readObject();

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

    public static Keys generateKeysFromPassword(int size, char[] pass, byte[] salt) throws NoSuchAlgorithmException,
            InvalidKeySpecException {

        // Initialize and generate secret keys from password and pseudorandom salt
        SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance(KEY_GENERATION_SPEC);
        KeySpec keySpec = new PBEKeySpec(pass, salt, AUTH_ITERATIONS, size + AUTH_SIZE * 8);
        SecretKey tmpKey = secretKeyFactory.generateSecret(keySpec);
        byte[] key = tmpKey.getEncoded();

        // Save encryption and authorization keys in crypto.Keys static storage class
        SecretKey auth = new SecretKeySpec(Arrays.copyOfRange(key, 0, AUTH_SIZE), AES_SPEC);
        SecretKey enc = new SecretKeySpec(Arrays.copyOfRange(key, AUTH_SIZE, key.length), AES_SPEC);
        return new Keys(enc, auth);
    }
    // TODO: do we even need both keys?
    // Class to store pair of encryption and authentication keys
    public static class Keys {
        public final SecretKey encr, auth;
        public Keys(SecretKey encr, SecretKey auth) {
            this.encr = encr;
            this.auth = auth;
        }
    }
}
