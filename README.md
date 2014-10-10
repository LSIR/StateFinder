StateFinder
===========

This repository contains the code of the algorithms described in the paper: Online Unsupervised State Recognition in Sensor Data, J.Eberle, T.K. Wijaya, K.Aberer, submitted to PerCom 2015


Setup
------

jre (1.6+) and python 2.7 

python needs the following additonal packages:
 - numpy (1.7.1)
 - scipy (0.12)
 - argparse (1.2.1)
 - python-Levenshtein (0.11.2)

Running
--------

To run StateFinder on the sample dataset given in Statefinder/input-example/data.csv


```shell

python Executable.py find_states_spclust input-example/data.csv data 1 1 20 10 0.9 0.9 0.2

```

Parameters are defined in the Workflow.py