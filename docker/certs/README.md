# Certificates for testing

## Introduction

This folder contains pre-built self-signed certificates used for testing. Only the `*.conf` and `*.ext` files are written by hand. All other files can be deleted and regenerated with the below command if necessary. However, since all these files are checked into git you don't have to.

## Procedure

```sh
rm -f *.key
rm -f *.crt
rm -f *.csr
rm -f *.srl
rm -f *.der
rm -f *.jks

# Create Root CA key & crt
openssl req -x509 -sha256 -days 9999 -nodes -config 01-rootCA.conf -newkey rsa:4096 -keyout 01-rootCA.key -out 01-rootCA.crt

# Create Intermediate-A CA key & csr
openssl req -new -keyout 02-intermediate-a.key -nodes -out 02-intermediate-a.csr -config 02-intermediate-a.conf

# Create Intermediate-A CA crt (signed by rootCA)
openssl x509 -req -days 9999 -sha256 -in 02-intermediate-a.csr -CA 01-rootCA.crt -CAkey 01-rootCA.key -CAcreateserial -out 02-intermediate-a.crt -extfile 02-intermediate-a.ext

# Create Intermediate-B CA key & csr
openssl req -new -keyout 03-intermediate-b.key -nodes -out 03-intermediate-b.csr -config 03-intermediate-b.conf

# Create Intermediate-B CA crt (signed by intermediate-a)
openssl x509 -req -days 9999 -sha256 -in 03-intermediate-b.csr -CA 02-intermediate-a.crt -CAkey 02-intermediate-a.key -CAcreateserial -out 03-intermediate-b.crt -extfile 03-intermediate-b.ext

# Create Server key & csr
openssl req -new -keyout 04-server.key -nodes -out 04-server.csr -config 04-server.conf

# Create Server crt (signed by intermediate-b)
openssl x509 -req -days 9999 -sha256 -in 04-server.csr -CA 03-intermediate-b.crt -CAkey 03-intermediate-b.key -CAcreateserial -out 04-server.crt -extfile 04-server.ext

# Build the certificate chain for the server certificate
cat 04-server.crt 03-intermediate-b.crt 02-intermediate-a.crt > 04-server-chain.crt

# Build bundle containing all CA certificates
cat 01-rootCA.crt 02-intermediate-a.crt 03-intermediate-b.crt > cabundle.crt

# Convert PEM certificates to DER (for java truststore)
openssl x509 -in 01-rootCA.crt -outform der -out 01-rootCA.der
openssl x509 -in 02-intermediate-a.crt -outform der -out 02-intermediate-a.der
openssl x509 -in 03-intermediate-b.crt -outform der -out 03-intermediate-b.der

# build java truststore
keytool -import -file 01-rootCA.der -alias rootCA -keystore truststore.jks -storepass changeit --noprompt
keytool -import -file 02-intermediate-a.der -alias intermediate-a -keystore truststore.jks -storepass changeit --noprompt
keytool -import -file 03-intermediate-b.der -alias intermediate-b -keystore truststore.jks -storepass changeit --noprompt
keytool -list -keystore truststore.jks --storepass changeit

chmod ugo+r *.key
```
