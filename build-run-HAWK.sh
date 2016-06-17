#!/bin/bash -x

#ref: https://github.com/AKSW/hawk

mvn clean package -DskipTests
java -jar target/hawk-*.jar
