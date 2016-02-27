import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Crypto {

    public static final int BUFF_SIZE = 1024 * 1024;
    public static final String HASHING_ALGORITHM = "SHA-256";

    public static byte[] generateHash(String type, File file) throws NoSuchAlgorithmException, IOException {

        // Initialize message digest for given hashing algorithm and file input stream
        MessageDigest messageDigest = MessageDigest.getInstance(type);
        FileInputStream fis = new FileInputStream(file);

        // Read plaintext in chunks and update the message digest
        byte[] buffer = new byte[BUFF_SIZE];
        int read;
        while((read = fis.read(buffer)) != -1) {
            messageDigest.update(buffer, 0, read);
        }
        // Finished using file input stream
        fis.close();

        // Digest hashed bytes
        return messageDigest.digest();
    }

}
