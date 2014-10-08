#!/bin/zsh

# only tested with zsh

mkdir -p outputs/sample/data-raw
mkdir -p outputs/sample/data-symbol
mkdir -p outputs/sample/data-rle
mkdir -p outputs/sample/data-statefinder
python Executable.py split_file_by datasets/sample/data.csv outputs/sample/data-raw/ 0 3600
python Executable.py find_states_spclust sample data.csv data 1 1 20 10 0.9 0.9 0.2
for k in rle symbol data-statefinder
do 
    cd outputs/sample/data-$k
    rm *.frc
    java -jar ../../../pattern-based-forecasting/Forecast.jar -o . 2 0.2 3
    for j in file*.frc
    do
        #hamming distance
        java -jar ../../../pattern-based-forecasting/GetDistance2.jar -a ${j:0:-4} $j >> h_dist.tmp
        #levenstein distance
        python ../../../Executable.py get_distance ${j:0:-4} $j 1 >> l_dist.tmp
    done
    cd ../../../
done

cd outputs/sample/data-raw
rm *.frc*
java -jar ../../../pattern-based-forecasting/Forecast.jar -o . 1 0.2 3
for j in day*.frc
do
   # euclidean distance
   java -jar ../../../pattern-based-forecasting/GetDistance.jar ${j:0:-4} $j >> e_dist.tmp
done