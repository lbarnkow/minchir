# Certificates for testing

## Introduction

This folder contains pre-built self-signed certificates used for testing. Only the `*.conf` and `*.ext` files are written by hand. All other files can be deleted and regenerated with the below command if necessary. However, since all these files are checked into git you don't have to.

All certificates and private keys are in PEM encoding / format, see [IETF-RFC-7468](https://www.rfc-editor.org/rfc/rfc7468).

**⚠️ CAUTION ⚠️:** Do not re-use these certificates / private keys in other contexts. When you need to setup your own CA / certificates for production use, please do consult an expert on how to do this properly (using HSMs, separating CA private keys from server private keys, etc)!

These certificates form a certificate chain one might encounter in actual production settings within corporate networks using their own private CA (certificate authority). The used certificates are as follows:

* a private root CA (`01-rootCA`),
* the first intermediate CA (`02-intermediate-a`) signed by the root CA,
* the second intermediate CA (`03-intermediate-b`) signed by the first intermediate CA,
* the server certificate (`04-server`) signed by the second intermediate CA.

Ideally minchir would only need to trust the private root CA for any connection to a server using the server certificate to authenticate itself. However, to establish trust with the actual server minchir needs to know all certificates in the chain of trust. This means the server must either (a) present the full certificate chain (i.e. its server certificate, as well as, the two intermediate CA certificates) _or_ (b) minchir needs to trust all the CAs in the chain. Both use cases are prepared here, however, only case (a) is used during integration tests.

* (a) see file `04-server-chain.crt`
* (b) see file `cabundle.crt`

## Procedure

```sh
rm -f *.key
rm -f *.crt
rm -f *.csr
rm -f *.srl

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

# make private keys world-readable (for ease of use in docker compose)
chmod ugo+r *.key
```
