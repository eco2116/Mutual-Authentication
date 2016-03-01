Evan O'Connor (eco2116)
Network Security
Programming Assignment 2

RESOURCES:

For mutual authentication over TLS socket in Java:
http://commandlinefanatic.com/cgi-bin/showarticle.cgi?article=art042

For reading files into byte arrays:
http://www.java-tips.org/java-se-tips-100019/18-java-io/19-reading-a-file-into-a-byte-array.html

For AES encryption/decryption:
https://www.owasp.org/index.php/Using_the_Java_Cryptographic_Extensions#AES_Encryption_and_Decryption

COMPILING MY PROGRAM:

javac *.java

RUNNING THE SERVER:

Usage:
java server <port> <keyStore> <keyStorePassword> <trustStore> <trustStorePassword>

Example:
java server 1234 server.jks password client.jks password

RUNNING THE CLIENT:

Usage:
java client <host> <port> <keyStore> <keyStorePassword> <trustStorePassword> <trustStore>

Example:
java client vienna.clic.cs.columbia.edu 1234 client.jks password server.jks password

GENERATING SERVER CERTIFICATE:

Example:
keytool -genkeypair -keystore server.jks -storepass password -alias server -keypass password -keyalg RSA \
-sigalg SHA256withRSA

GENERATE CLIENT CERTIFICATE:

Example:
keytool -genkeypair -keystore client.jks -storepass password -alias client -keypass password -keyalg RSA \
-sigalg SHA256withRSA

NOTES ON CERTIFICATES:

The passwords entered when creating the certificates must match the appropriate keyStorePassword and
trustStorePassword provided as command line arguments for the server and the client. The client and server
must be able to access their own certificate files (keyStore) and the certificate that will be trusted
if received from another party (trustStore). These are .jks files because I used keytool to generate them.
Certificates are self-signed. The certificates use RSA and SHA256.

NOTES ON PASSWORD:

Password hash must be able to be UTF-8 encodable.

NOTES ON COMMAND LINE ARGUMENTS:

None of the command line arguments can contain spaces (file name, password) because the client will not be
able to correctly parse them, thinking that they are separate inputs.

NOTES ON TRANSFERABLE DATA:

This program accepts ASCII and binary data files of dynamic size.

NOTES ON MUTUAL AUTHENTICATION TESTING:

The directory Screenshots contains screenshots of Wireshark output while monitoring the server/client
mutual authentication with certificates.

NOTES ON IMPLEMENTATION:

My client and server main methods perform the functionality as described in the assignment description.
I chose to create my own application layer protocol to encapsulate various interactions between client and
server. I did this by creating serializable message classes that store various data or metadata. This allowed
me to send/receive these messages over object output/input streams. I needed to reset these streams so that
no caching would prevent the same data from being transmitted. Any recoverable errors in transmission
will not halt the entire connection. The Crypto helper class I created is used to house various encryption/decryption,
hashing methods, and custom exceptions used to encapsulate various internal errors that may not be appropriate
to transmit to the user for security reasons.
