
import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Scanner;

/**
 * Evan O'Connor (eco2116)
 *
 * client.java
 */

public class client {

    private static final int PASSWORD_LENGTH = 8;

    public static void main(String[] args) throws IOException, ClassNotFoundException, NoSuchAlgorithmException {
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
                                objectOutputStream.writeObject(new PutMessage(splitCmd[1]));
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
                                    byte[] clientHash = Crypto.generateHash(Crypto.HASHING_ALGORITHM, getMessage.getFile());

                                    File hashFile = getMessage.getHash();
                                    byte[] serverHash = new byte[(int) getMessage.getHash().length()];
                                    FileInputStream fileInputStream = new FileInputStream(hashFile);
                                    fileInputStream.read(serverHash);
                                    fileInputStream.close();

                                    if(!Arrays.equals(clientHash, serverHash)) {
                                        System.out.println("Calculated hash did not match hash server sent.");
                                    } else {

                                        byte[] writeFile = new byte[(int) getMessage.getFile().length()];
                                        fileInputStream = new FileInputStream(getMessage.getFile());
                                        fileInputStream.read(writeFile);
                                        fileInputStream.close();

                                        FileOutputStream fileOutputStream = new FileOutputStream(getMessage.getFileName());
                                        fileOutputStream.write(writeFile);
                                        fileOutputStream.close();
                                        System.out.println("Wrote new file ");

                                    }
                                } else {
                                    System.out.println("Did not understand message from server.");
                                }

                            } else {
                                File file = new File(splitCmd[1]);
                                if(!file.exists() || !file.canRead()) {
                                    System.out.println("Failed to access file: " + splitCmd[1]);
                                    objectOutputStream.writeObject(new ErrorMessage(new PutMessage.PutFileNotFoundException()));
                                    continue;
                                } else {
                                    byte[] hash = Crypto.generateHash(Crypto.HASHING_ALGORITHM, file);
                                    objectOutputStream.writeObject(new PutMessage(file.getName(), file, hash));
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
}
