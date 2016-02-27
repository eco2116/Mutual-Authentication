import javax.net.SocketFactory;
import javax.net.ssl.SSLSocket;
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

    public static void main(String[] args) throws IOException {
        SocketFactory sslFactory = SSLSocketFactory.getDefault();
        Socket connection = sslFactory.createSocket("localhost", 1234);
        OutputStream out = connection.getOutputStream();
        InputStream in = connection.getInputStream();

        ObjectOutputStream objectOutputStream = new ObjectOutputStream(connection.getOutputStream());

        Scanner input = new Scanner(System.in);
        while(true) {
            System.out.print("> ");
            String userCmd = input.nextLine();
            String[] splitCmd = userCmd.split(" ");
            if(splitCmd.length == 0) {
                System.out.println("Please enter a command.");
                continue;
            } else if(splitCmd.length == 1) {
                if(!splitCmd[0].equals("stop")) {
                    System.out.println("Did not understand that command.");
                } else {
                    objectOutputStream.writeObject(new StopRequest());
                    System.out.println("Goodbye!");
                    break;
                }
            }

        }

//        out.write("abc".getBytes());
//
//        int c;
//        while((c = in.read()) != -1) {
//            System.out.print((char) c);
//        }
        in.close();
        out.close();
        connection.close();
    }
}
