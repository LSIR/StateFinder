#!/bin/bash

# preparation
cd lib
dir=org/apache
if [ ! -d "$dir" ]; then
	unzip -o commons-cli-1.2.jar
fi
cp -r org ../bin
cd ..

# go to bin directory
cd bin

# create jar for AnomalyDetection.jar
echo Main-class: ch.epfl.lsir.anomaly.AnomalyDetection > manifest.txt
jar cvfm AnomalyDetection.jar manifest.txt ch/epfl/lsir/anomaly org/apache
rm manifest.txt

# go to main project directory
cd ..

# bring the new jars
mv bin/AnomalyDetection.jar .

