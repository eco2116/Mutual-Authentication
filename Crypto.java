import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Crypto {

    public static final int BUFF_SIZE = 1024 * 1024;
    public static final String HASHING_ALGORITHM = "SHA-256";

    public static byte[] generateHash(String type, byte[] fileBytes) throws NoSuchAlgorithmException, IOException {

        // Initialize message digest for given hashing algorithm and file input stream
        MessageDigest messageDigest = MessageDigest.getInstance(type);
//        FileInputStream fis = new FileInputStream(file);
//
//        // Read plaintext in chunks and update the message digest
//        byte[] buffer = new byte[BUFF_SIZE];
//        int read;
//        while((read = fis.read(buffer)) != -1) {
//            messageDigest.update(buffer, 0, read);
//        }
        messageDigest.update(fileBytes, 0, fileBytes.length);
        // Finished using file input stream
//        fis.close();

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

}
