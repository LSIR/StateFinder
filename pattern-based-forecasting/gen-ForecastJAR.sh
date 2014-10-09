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

# create jar for Forecast.jar
echo Main-class: ch.epfl.lsir.forecasting.Forecast > manifest.txt
jar cvfm Forecast.jar manifest.txt ch/epfl/lsir/forecasting org/apache
rm manifest.txt

# go to main project directory
cd ..

# bring the new jars
mv bin/Forecast.jar .

