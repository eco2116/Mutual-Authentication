import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.net.Socket;
import java.util.Scanner;

/**
 * Evan O'Connor (eco2116)
 *
 * client.java
 */

public class client {

    private static final int PASSWORD_LENGTH = 8;

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        SocketFactory sslFactory = SSLSocketFactory.getDefault();
        Socket connection = sslFactory.createSocket("localhost", 1234);
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
                            System.out.println("Only filename, \"\"E\" or \"N\", and password required.");
                            continue;
                        } else if (splitCmd.length < 4) {
                            System.out.println("Missing parameters for request with encryption.");
                            System.out.println("Filename, \"\"E\" or \"N\", and password required.");
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
                            System.out.println("Only filename and \"\"E\" or \"N\".");
                            continue;
                        } else {
                            if(splitCmd[0].equals("get")) {
                                objectOutputStream.writeObject(new GetMessage(splitCmd[1]));
                                GetMessage getMessage = (GetMessage) objectInputStream.readObject();
                                
                            } else {
                                objectOutputStream.writeObject(new PutMessage(splitCmd[1]));
                            }
                        }
                    } else {
                        System.out.println("Second parameter for get request must be \"\"E\" or \"N\"");
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
