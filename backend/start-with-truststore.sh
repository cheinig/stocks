#!/bin/bash

# Start Spring Boot with custom truststore
./mvnw spring-boot:run \
  -Dspring-boot.run.jvmArguments="\
    -Djavax.net.ssl.trustStore=/tmp/heinig-truststore.jks \
    -Djavax.net.ssl.trustStorePassword=changeit \
    -Djavax.net.ssl.trustStoreType=JKS"
