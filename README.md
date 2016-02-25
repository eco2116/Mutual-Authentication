Evan O'Connor (eco2116)


http://stilius.net/java/java_ssl.php (TLS sockets)

http://commandlinefanatic.com/cgi-bin/showarticle.cgi?article=art042 (2-way)

RUN SERVER

java -Djavax.net.ssl.trustStore=client.jks -Djavax.net.ssl.trustStorePassword=password \
-Djavax.net.ssl.keyStore=server.jks -Djavax.net.ssl.keyStorePassword=password server

RUN CLIENT

java -Djavax.net.ssl.keyStore=client.jks -Djavax.net.ssl.keyStorePassword=password \
-Djavax.net.ssl.trustStore=server.jks -Djavax.net.ssl.trustStorePassword=password client

GENERATE SERVER CERTIFICATE

keytool -genkeypair -keystore server.jks -storepass password -alias server -keypass password

GENERATE CLIENT CERTIFICATE

keytool -genkeypair -keystore client.jks -storepass password -alias client -keypass password