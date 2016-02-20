import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Evan O'Connor (eco2116)
 *
 * server.java
 */
public class server {
/*
 public static void main(String[] args) throws IOException {

    ServerSocketFactory fact = SSLServerSocketFactory.getDefault();
    ServerSocket listen = fact.createServerSocket(1234);


    Socket connection = null;
    while ((connection = listen.accept()) != null)  {
      InputStream in = connection.getInputStream();
      OutputStream out = connection.getOutputStream();

      int c;
      while ((c = in.read()) != -1) {
        out.write(c);
      }
			out.close();
			in.close();
			connection.close();
    }
 */
    public static void main(String[] args) {

        args[0] = "8888";

        // TODO: two way

        SSLServerSocket sslServerSocket = null;
        SSLSocket sslSocket = null;
        InputStream inputStream = null;
        InputStreamReader inputStreamReader = null;
        BufferedReader bufferedReader = null;

        try {
            // Open up TLS socket
            SSLServerSocketFactory sslServerSocketFactory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
            sslServerSocket = (SSLServerSocket) sslServerSocketFactory.createServerSocket(Integer.getInteger(args[0]));
            sslSocket = (SSLSocket) sslServerSocket.accept();

            inputStream = sslSocket.getInputStream();
            inputStreamReader = new InputStreamReader(inputStream);
            bufferedReader = new BufferedReader(inputStreamReader);

            // Write to client
            String s;
            while((s = bufferedReader.readLine()) != null) {
                System.out.println(s);
                System.out.flush();
            }

        } catch(IOException e) {
            System.out.println("Failed to set up TLS sockets and streams.");
        } finally {
            closeStreamsAndSockets(sslServerSocket, sslSocket, inputStream, inputStreamReader, bufferedReader);
        }
    }

    public static void closeStreamsAndSockets(SSLServerSocket sslServerSocket, SSLSocket sslSocket, InputStream inputStream,
                                              InputStreamReader inputStreamReader, BufferedReader bufferedReader) {
        try {
            sslServerSocket.close();
            sslSocket.close();
            inputStream.close();
            inputStreamReader.close();
            bufferedReader.close();
        } catch(IOException e) {
            System.out.println("Failed to close streams and sockets");
        }


    }

}
