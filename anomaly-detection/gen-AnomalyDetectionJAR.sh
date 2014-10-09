#!/bin/bash

# Before running the script: 
# 1 Compile the source file, we assume the *.class files are in bin directory
# 2 unzip lib/commons-cli-1.2.jar
# 3 move the resulting org directory to bin directory

# go to bin directory
cd bin

# create jar for AnomalyDetection.jar
echo Main-class: ch.epfl.lsir.anomaly.AnomalyDetection > manifest.txt
jar cvfm AnomalyDetection.jar manifest.txt ch/epfl/lsir/anomaly org/apache

# go to main project directory
cd ..

# bring the new jars
mv bin/AnomalyDetection.jar .

