#! /bin/sh

rm *.cer
rm *.ks

./createKeystore.sh driver
./createKeystore.sh client
./createKeystore.sh node

./exportCert.sh driver
./exportCert.sh client
./exportCert.sh node

# create client trust store with driver certificate
./createUpdateTruststore.sh driver client
# add node certificate to the client trust store so node management can work
# (necessary for the admin console for example)
#./createUpdateTruststore.sh node client

# create node trust store with driver certificate
./createUpdateTruststore.sh driver node

# create driver trust store with client and node certificates
./createUpdateTruststore.sh node driver
# create driver_client trust store with client certificate
./createUpdateTruststore.sh client driver_client

# convert node keystore and trustore to BKS format for use with Android
./toBKS.sh node