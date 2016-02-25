import javax.net.SocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.net.Socket;

/**
 * Evan O'Connor (eco2116)
 *
 * client.java
 */
/*
public class SSLClient  {
  public static void main(String[] args) throws IOException {
    SocketFactory fact = SSLSocketFactory.getDefault();
    Socket conn = fact.createSocket("localhost", 1234);
    OutputStream out = conn.getOutputStream();
    InputStream in = conn.getInputStream();
    out.write("abc".getBytes());
    int c;
    while ((c = in.read()) != -1) {
      System.out.print((char) c);
    }
    in.close();
    out.close();
    conn.close();
  }
 */
public class client {

    public static void main(String[] args) throws IOException {
        SocketFactory sslFactory = SSLSocketFactory.getDefault();
        Socket connection = sslFactory.createSocket("localhost", 1234);
        OutputStream out = connection.getOutputStream();
        InputStream in = connection.getInputStream();

        out.write("abc".getBytes());

        int c;
        while((c = in.read()) != -1) {
            System.out.print((char) c);
        }
        in.close();
        out.close();
        connection.close();
    }


//    public static void main(String[] args) {
//        args[0] = "8888";
//        String ip = "localhost";
//
//        SSLSocket sslSocket = null;
//        InputStream inputStream = System.in;
//        InputStreamReader inputStreamReader = null;
//        OutputStream outputStream = null;
//        OutputStreamWriter outputStreamWriter = null;
//        BufferedWriter bufferedWriter = null;
//
//        try {
//            // Create a new TLS socket
//            SSLSocketFactory sslSocketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
//            sslSocket = (SSLSocket) sslSocketFactory.createSocket(ip, Integer.getInteger(args[0]));
//
//            // Open up streams
//            inputStreamReader = new InputStreamReader(inputStream);
//            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
//            outputStream = sslSocket.getOutputStream();
//            outputStreamWriter = new OutputStreamWriter(outputStream);
//            bufferedWriter = new BufferedWriter(outputStreamWriter);
//
//            // Read in data
//            String s;
//            while((s = bufferedReader.readLine()) != null) {
//                bufferedWriter.write(s + '\n');
//                bufferedWriter.flush();
//            }
//
//        } catch(IOException e) {
//            System.out.println("Failed to set up TLS sockets and streams.");
//        } finally {
//            closeStreamsAndSockets(sslSocket, inputStreamReader, inputStream, outputStream, outputStreamWriter, bufferedWriter);
//        }
//
//    }
//
//    public static void closeStreamsAndSockets(SSLSocket sslSocket, InputStreamReader inputStreamReader, InputStream inputStream,
//                                              OutputStream outputStream, OutputStreamWriter outputStreamWriter,
//                                              BufferedWriter bufferedWriter) {
//        try {
//            sslSocket.close();
//            inputStreamReader.close();
//            inputStream.close();
//            outputStream.close();
//            outputStreamWriter.close();
//            bufferedWriter.close();
//        } catch(IOException e) {
//            System.out.println("Failed to close streams and sockets.");
//        }
//    }
}
