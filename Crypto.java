import javax.crypto.SecretKey;
import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class Crypto {

    public static final String HASHING_ALGORITHM = "SHA-256";
    private static final int BUFFER_SIZE = 1024;

    public static byte[] generateHash(String type, byte[] fileBytes) throws NoSuchAlgorithmException, IOException {

        // Initialize message digest for given hashing algorithm and file input stream
        MessageDigest messageDigest = MessageDigest.getInstance(type);

        // TODO: is this okay? no loop?
        messageDigest.update(fileBytes, 0, fileBytes.length);

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

    public static void sendFile(File file, ObjectOutputStream objectOutputStream) throws IOException, NoSuchAlgorithmException {

        long size = file.length();
        System.out.println("total length " + size);

        objectOutputStream.writeObject(new PutMessage(file.getName(), size));
        FileInputStream fileInputStream = new FileInputStream(file);

        byte[] buff = new byte[BUFFER_SIZE];
        int read;
        int count = 0;
        while ((read = fileInputStream.read(buff)) >= 0) {
            System.out.println("read " + read);

            if(read < BUFFER_SIZE) {
                System.out.println("less than buffer");
                break;
            } else {
                System.out.println("writing data msg");
                System.out.println(buff[0]);
                DataMessage dm = new DataMessage(buff, count++);
                objectOutputStream.writeObject(dm);
                objectOutputStream.reset();
                objectOutputStream.flush();
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

    // Class to store pair of encryption and authentication keys
    public static class Keys {
        public final SecretKey encr, auth;
        public Keys(SecretKey encr, SecretKey auth) {
            this.encr = encr;
            this.auth = auth;
        }
    }
}
