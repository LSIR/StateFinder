package ch.epfl.lsir.forecasting;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

/**
 * 
 * @author Tri Kurniawan Wijaya <tri-kurniawan.wijaya@epfl.ch>
 *
 */
public class Forecast {

	/**
	 * @param args
	 * @throws IOException 
	 * @throws NumberFormatException 
	 * @throws ParseException 
	 */
	public static void main(String[] args) throws NumberFormatException, IOException, ParseException {
		/*
		 * 1. load data
		 * 2. divide into 60 training set, 20 validation set, 20 test set
		 * 3. create a module for k-medoids(int k)
		 * 4. create a module to get silhouette, dunn index, and davies-bouldin index(cluster configuration)
		 * 5. 
		 */


		/** begin input **/ 
		int randSeed=1;

		//int VERBOSE = Constants.LOG_PREDICTOR;
		int VERBOSE = 0b0;
		
		String dirInputStr = "input-example";
		int datasetType = Constants.SYMBOLIC;
		
		double testSetProportion = 0.2;
		int historyWindow=3;

		/** end input **/ 
		
		// Parse available options
		Options opts = createOptions();
		CommandLineParser parser = new PosixParser();
		CommandLine cmd = parser.parse(opts, args);
		
		// if help needed
		if (cmd.hasOption("h") || args.length==0) {
			HelpFormatter help = new HelpFormatter();
			help.setWidth(90);
			String helpString = "java -jar Forecast.jar [OPTIONS] DIRINPUT DATATYPE TESTPERCENTAGE HISTORYWINDOW\n" 
					+ "Example: java -jar Forecast.jar input-example 2 0.2 3\n"
					+ "** DIRINPUT is directory of dataset: files of timeseries (TIMESTART, TIMEEND, VALUE). " +
					"One day data per file. " +
					"This code will order the files by its first TIMESTART. " +
					"However, we assume that the timeseries inside the files are ordered ascending by TIMESTART. " +
					"Assuming that the first and the last files might " +
					"contain timeseries less than a day, we discard them.\n"
					+ "** DATATYPE is 1 if VALUE is real value, 2 if VALUE is symbol, such as symbolic, rle of symbolic, or state-clustering representation.\n"
					+ "** TESTPERCENTAGE is the percentage of the dataset files that are used for testing. " +
					"We take TESTPERCENTAGE starts from the last files in the dataset (note that we order the " +
					"files by its first TIMESTART). We do one day-ahead forecasting. " +
					"After forecast a day in the test set, this day is then added to the training set " +
					"for forecasting the next day.\n" 
					+ "** HISTORYWINDOW is the length of day before the test day to be considered " +
					"in the pattern search. \n" +
					"\n OPTIONS: \n";
			help.printHelp(helpString, opts);
			return;
		} 

		dirInputStr = args[args.length-4];
		datasetType = Integer.parseInt( args[args.length-3] );		
		testSetProportion = Double.parseDouble( args[args.length-2] );
		historyWindow= Integer.parseInt( args[args.length-1] );

		if (cmd.hasOption("v")){
			VERBOSE = Integer.parseInt( cmd.getOptionValue("v") );
		}
		
		if (cmd.hasOption("r")){
			randSeed = Integer.parseInt( cmd.getOptionValue("r") );
		}
		// record start time
		long execStart = (new Date()).getTime();

		
		/** initialization phase **/
		// load the dataset
		Dataset dataset = new Dataset();
		dataset.loadDirectory(dirInputStr);
		dataset.sortAsc();
		
		// discard the first and the last entry
		dataset.remove(dataset.size()-1);
		dataset.remove(0);
		
		
		for (int i=0; i<dataset.size(); i++) 
			Util.logPrintln(VERBOSE, 0b01, "data "+i+": "+dataset.get(i));
		
		// divide into training, validation, and test set: 60, 20, 20
		int datasetLen = dataset.size();
		int valLen = (int) (datasetLen * 0.0);
		int testLen = (int) (datasetLen * testSetProportion);
		int trainingLen = datasetLen - valLen - testLen;
		
		Util.logPrintln(VERBOSE, 0b01, "dataset length: "+datasetLen);
		Util.logPrintln(VERBOSE, 0b01, "training set length: "+trainingLen);
		Util.logPrintln(VERBOSE, 0b01, "validation set length: "+valLen);
		Util.logPrintln(VERBOSE, 0b01, "test set length: "+testLen);

		// create training, validation, and test set
		Dataset trainingSet = new Dataset();
		for (int i=0; i<trainingLen; i++)  {
			trainingSet.add(dataset.get(i));
		}
		
		// we do not use this validation set for now
		Dataset validationSet = new Dataset();
		for (int i=trainingLen; i<trainingLen+valLen; i++) 	validationSet.add(dataset.get(i));
		
		Dataset testSet = new Dataset();
		//..ArrayList<String> testNames = new ArrayList<String>();
		for (int i=trainingLen+valLen; i<dataset.size(); i++) {
			testSet.add(dataset.get(i), dataset.getName(i));
			//..testNames.add();
		}
		
		// we compute always day-ahead forecasting
		
		String s = "test-name,prediction";
		boolean outFile = false;
		if ( cmd.hasOption("o") ) outFile = true;
		while (testSet.size() > 0) {
			Entries prediction = patternBasedForecasting(trainingSet, testSet.get(0), datasetType, historyWindow,VERBOSE, randSeed);
			s = testSet.getName(0)+","+prediction;
			if (outFile==true) {
				System.err.println("forecasting "+testSet.getName(0));
				PrintWriter out = new PrintWriter(testSet.getName(0)+".frc");
				if (prediction==null) out.println("null");
				else {
					for (int i=0; i<prediction.size();i++)
						out.println(prediction.get(i).timeStart+","+prediction.get(i).timeEnd+","+prediction.get(i).value);
				}
				out.close();
			} else {
				System.out.println(s);				
			}
			trainingSet.add(testSet.get(0));
			testSet.remove(0);
			//..testNames.remove(0);
		}
		
		
		System.err.println("done (" + ( (new Date()).getTime() - execStart ) / 1000.0 + " sec)."  );

	}
	
	public static Options createOptions(){
		Options options = new Options();
		options.addOption("v", "verbose", true, "Print some debugging information [1=LOG_INFO, 2=LOG_KMEDOIS, 4=LOG_SILHOUETTE, " +
				"8=LOG_DUNNINDEX, 16=LOG_DAVIESB, 32=LOG_PREDICTOR].");
		options.addOption("o", "output", false, "Also write the result into a file. The name of the file result is file test name + '.frc' ");
		options.addOption("r", "randomSeed", true, "Set the random seed.");
		options.addOption("h", "help", false, "Help. Print this message.");		
		return options;	
	}

	private static Entries patternBasedForecasting(Dataset trainingSet, Entries testInstance, int datasetType, int historyWindow, 
			int VERBOSE, int randSeed){
		Random rand = new Random(randSeed);
		// compute distance between data
		// create distance matrix
		double[][] distance = new double[trainingSet.size()][trainingSet.size()];
		for (int i=0; i<trainingSet.size()-1; i++) {
			for (int j=i+1; j<trainingSet.size(); j++) {
				double dist = trainingSet.get(i).getDistance(datasetType, trainingSet.get(j), VERBOSE);
				distance[i][j] = dist;
				distance[j][i] = dist;
			}
		}
		
		// print the distance matrix
		for (int i=0; i<trainingSet.size();i++) {
			Util.logPrint(VERBOSE, 0b01, "distance data "+i+": ");
			for (int j=0; j<trainingSet.size();j++) {
				Util.logPrint(VERBOSE, 0b01, distance[i][j]+",");
			}
			Util.logPrintln(VERBOSE, 0b01, "");
		}

		
		/** start k-medoids clustering **/
		int numIter = 500;
		int[] k = {2,3,4,5,6,7,8,9,10};
		//int[] k = {2,3,4,5};
		ArrayListKeyValue silhouette = new ArrayListKeyValue();
		ArrayListKeyValue dunnIndex = new ArrayListKeyValue();
		ArrayListKeyValue daviesBouldin = new ArrayListKeyValue();
		
		// to store all of the results
		ArrayList<int[]> clusterers = new ArrayList<int[]>(); 
		
		for (int j=0; j<k.length; j++) {
			double minDist = Double.MAX_VALUE;
			ArrayList<ArrayList<Integer>> bestClusters = null;
			ArrayList<Integer> bestMedoids = null;
			for (int i=0; i<numIter; i++) {
				ArrayList<ArrayList<Integer>> clusters = new ArrayList<ArrayList<Integer>>();
				ArrayList<Integer> medoids = new ArrayList<Integer>();
				double dist = Cluster.kMedoids(k[j], trainingSet, distance, 0b01, rand.nextInt(), medoids, clusters);
				if  ( dist < minDist ) {
					minDist = dist;
					bestMedoids = medoids;
					bestClusters = clusters;
				}
				Util.logPrintln(VERBOSE, 0b10, "iteration: "+i + ", distance: "+dist+" [minDist: "+minDist+"], medoids: "+medoids + ", clusters: "+clusters);
			}
			Util.logPrintln(VERBOSE, Constants.LOG_INFO, "k: "+k[j]);
			Util.logPrintln(VERBOSE, Constants.LOG_INFO, "final medoids: "+ bestMedoids) ;
			Util.logPrintln(VERBOSE, Constants.LOG_INFO, "final clusters: "+bestClusters);			
			
			// store the cluster
			clusterers.add( invertedIndex(bestClusters, trainingSet.size()) );
			
			// get silhouette index 
			double silh = Cluster.getSilhouette(trainingSet, bestClusters, distance, VERBOSE);
			silhouette.add(k[j], silh);
			Util.logPrintln(VERBOSE, Constants.LOG_INFO, "Silhouette index: " + silh);
			
			
			// get dunn index
			double dunn = Cluster.getDunnIndex(bestClusters, distance, VERBOSE);
			dunnIndex.add(k[j], dunn);
			Util.logPrintln(VERBOSE, Constants.LOG_INFO, "Dunn index: " + dunn);
			
			// get davies-bouldin index
			double daviesB = Cluster.getDaviesBouldin(bestMedoids, bestClusters, distance, VERBOSE);
			daviesBouldin.add(k[j], daviesB);
			Util.logPrintln(VERBOSE, Constants.LOG_INFO, "Davies-Bouldin index: " + daviesB);
			
		}
		
		// sort each value from good to bad
		silhouette.sortValueDesc();
		dunnIndex.sortValueDesc();
		daviesBouldin.sortValueAsc();
		Util.logPrintln(VERBOSE, Constants.LOG_INFO, silhouette);
		Util.logPrintln(VERBOSE, Constants.LOG_INFO, dunnIndex);
		Util.logPrintln(VERBOSE, Constants.LOG_INFO, daviesBouldin);
		
		int ctr=0;
		Counts count = new Counts();
		Integer bestK = null;
		while (true) {
			count.insert( silhouette.get(ctr).key ) ;
			count.insert( dunnIndex.get(ctr).key ) ;
			count.insert( daviesBouldin.get(ctr).key ) ;
			bestK = count.getMajorityGreaterThan();
			if  ( bestK != null ) break;
			bestK = count.getVoteAtLeast(3); // if there are any k that got 3 votes already
			if  ( bestK != null ) break;
			ctr++;
			if  ( ctr >= silhouette.size()) break;
			
		}
		// get the best k
		Util.logPrintln(VERBOSE, Constants.LOG_INFO, "best K: "+bestK);
		if ( bestK==null ) return null;
		
		// label each training set using the label from k = 2;
		int[] finalCluster = clusterers.get( bestK-2 );
		Util.logPrintln(VERBOSE, Constants.LOG_INFO, "final label: "+Util.arrayToStr(finalCluster));
		
		
		// get all predictors
		ArrayList<Integer> predictors = getPredictor(historyWindow, finalCluster, VERBOSE);
		Util.logPrintln(VERBOSE, Constants.LOG_INFO, "predictors-candidates: "+predictors);
		
		// get the medoid of all predictors as the prediction instance
		int predictionI = getMedoid(predictors, distance, VERBOSE, Constants.LOG_PREDICTOR);
		Util.logPrintln(VERBOSE, Constants.LOG_INFO, "prediction-instance: "+predictionI);
		if ( predictionI == -1 ) return null;
		Entries predictionE = createPrediction(trainingSet.get(predictionI), testInstance.get(0).timeStart);
		Util.logPrintln(VERBOSE, Constants.LOG_INFO, "test-instance: "+testInstance);
		Util.logPrintln(VERBOSE, Constants.LOG_INFO,"prediction result: "+predictionE);
		//System.err.println("distance(test-instance, prediction): "+predictionE.getDistance(datasetType, testInstance, Constants.LOG_PREDICTOR));
		return predictionE;
	}
	
	/**
	 * Create a prediction instance based on the prediction entry and the time start of the test instance.  
	 * @param predictionEntry
	 * @param timeStart
	 * @return
	 */
	private static Entries createPrediction(Entries predictionEntry, long timeStart) {
		Entries result = new Entries();
		long diff = timeStart - predictionEntry.get(0).timeStart;
		for (int i=0; i<predictionEntry.size(); i++) {
			long ts = predictionEntry.get(i).timeStart + diff;
			long te = predictionEntry.get(i).timeEnd + diff;
			double value = predictionEntry.get(i).value;
			Entry e = new Entry(ts, te, value);
			result.add(e);
		}
		return result;
	}

	
	/**
	 * get a medoid of a set of points in points
	 * @param points
	 * @param distance
	 * @param VERBOSE
	 * @param logFlag
	 * @return
	 */
	private static int getMedoid(ArrayList<Integer> points, double[][] distance, int VERBOSE, int logFlag) {
		
		double minDist = Double.MAX_VALUE;
		int medoid = -1; 
		for (int i=0; i<points.size(); i++) {
			double dist = Cluster.sumRowAgainstColumn(distance, points.get(i), points);
			if ( dist < minDist ) {
				minDist = dist;
				medoid = points.get(i);
			}
			Util.logPrintln(VERBOSE, logFlag, "points: " + points.get(i) + ", dist: " + dist + " [minDist: "+minDist+", medoid: "+medoid+"]");
		}
		return medoid;
	}

	/**
	 * 
	 * @param windowLen
	 * @param finalCluster
	 * @param VERBOSE
	 * @return an array of index of the predictors in the training set
	 */
	private static ArrayList<Integer> getPredictor(int windowLen, int[] finalCluster, int VERBOSE) {
		int logFlag = Constants.LOG_PREDICTOR;
		// get the last W
		int[] keySequence = new int[windowLen];
		for (int i=finalCluster.length-windowLen; i<finalCluster.length; i++) {
			keySequence[i-finalCluster.length+windowLen] = finalCluster[i];
		}
		Util.logPrintln(VERBOSE, logFlag, "keySeq: "+Util.arrayToStr(keySequence));
		
		// search for predictor
		// the result is the index in the training set
		ArrayList<Integer> result = new ArrayList<Integer>();
		for (int i=0; i<finalCluster.length-windowLen; i++) {
			// move forward W element from i if we can get the same sequence as keySequence
			int[] newSeq = new int[windowLen]; 
			for (int j=0; j<windowLen; j++ ){
				newSeq[j] = finalCluster[i+j];
			}
			boolean equal = Util.isEqual(keySequence, newSeq);
			Util.logPrintln(VERBOSE, logFlag, "newSeq: " + Util.arrayToStr(newSeq)+": "+equal);
			if ( equal==true )	result.add(i+windowLen);
		}
		return result;
	}

	/**
	 * 
	 * @param cluster
	 * @param size total element in the clusters
	 * @return
	 */
	public static int[] invertedIndex(ArrayList<ArrayList<Integer>> clusters, int size) {
		int[] result = new int[size];
		for (int i=0; i<clusters.size(); i++) {
			for (int e : clusters.get(i)){
				result[ e ] = i;
			}
		}
		return result;
	}
	
}

class Counts{
	HashMap<Integer, Integer> counts;
	public Counts(){
		counts = new HashMap<Integer, Integer>();
	}
	
	public Integer getVoteAtLeast(int lowBound) {
		for (Map.Entry<Integer, Integer> e : counts.entrySet()) {
			if (e.getValue()>=lowBound) {
				// we simply return the first one
				return e.getKey();
			}
		}
		return null;
	}

	public String toString() {
		return counts.toString();
	}

	public void insert(int value) {
		if ( counts.containsKey(value) ) {
			int count = counts.get(value);
			counts.put(value, count+1);
		} else {
			counts.put(value, 1);
		}
	}
	
	public Integer getMajorityGreaterThan(){
		int maxKey = Integer.MIN_VALUE;
		int maxValue = Integer.MIN_VALUE;
		boolean greaterThan = false;
		for (Map.Entry<Integer, Integer> e : counts.entrySet()) {
			if ( e.getValue() > maxValue ) {
				maxValue = e.getValue();
				maxKey = e.getKey();
				greaterThan = true;
			}
			else if (e.getValue() == maxValue) {
				greaterThan = false;
			}
		}
		if ( greaterThan == true ) 
			return maxKey;
		else 
			return null;
	}
}


class ArrayListKeyValue {
	ArrayList<KeyValue> content;
	
	public ArrayListKeyValue() {
		content = new ArrayList<KeyValue>();
	}
	
	public void add (int key, double value){
		KeyValue e = new KeyValue(key, value);
		content.add(e);
	}
	
	public KeyValue get(int index){
		return content.get(index);
	}
	
	public int size() {
		return content.size();
	}
	
	public void sortValueAsc(){
		for (int i=0; i<content.size()-1; i++) {
			for (int j=i+1; j<content.size(); j++) {
				if ( content.get(i).value > content.get(j).value ) {
					// swap
					KeyValue temp = content.get(i);
					content.set(i, content.get(j));
					content.set(j, temp);
				}
			}
		}
	}
	
	public void sortValueDesc(){
		for (int i=0; i<content.size()-1; i++) {
			for (int j=i+1; j<content.size(); j++) {
				if ( content.get(i).value < content.get(j).value ) {
					// swap
					KeyValue temp = content.get(i);
					content.set(i, content.get(j));
					content.set(j, temp);
				}
			}
		}
	}

	public String toString(){
		return content.toString();
	}
}

class KeyValue {
	int key;
	double value;
	
	public KeyValue(int key, double value){
		this.key = key;
		this.value = value;
	}
	
	public String toString() {
		return "["+key+","+value+"]";
	}
}


