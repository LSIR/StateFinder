#!/bin/bash

# go to bin directory
cd bin

# create jar for ComputeBaseline
echo Main-class: ch.epfl.lsir.spclust.executable.SpGetRaw > manifest.txt
jar cvfm SpGetRaw.jar manifest.txt ch/epfl/lsir/spclust org/apache 

# go to main project directory
cd ..

# bring the new jars
mv bin/SpGetRaw.jar .

