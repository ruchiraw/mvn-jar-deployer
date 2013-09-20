mvn-jar-deployer
================

A tool to point and install set of maven compiled jars into local maven repository.

Usage
=====

Following command will install all maven compiled jars in /home/ruchira/wso2store-1.0.0 directory, into your local maven repository pointed by --repo.

java -jar mvn-jar-deployer-1.0-SNAPSHOT.jar --path /home/ruchira/wso2store-1.0.0 --repo /home/ruchira/.m2/repository
