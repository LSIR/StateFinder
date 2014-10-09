package ch.epfl.lsir.anomaly;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;



public class AnomalyDetection {

	/**
	 * @param args
	 * @throws ParseException 
	 * @throws FileNotFoundException 
	 */
	public static void main(String[] args) throws ParseException, FileNotFoundException {

		/** begin input **/ 
		//int VERBOSE = Constants.LOG_INFO | Constants.LOG_DEBUG;
		int VERBOSE = Constants.LOG_NONE;
		String dirTrainingStr = "anomaly-training";
		String dirTestStr = "anomaly-test";
		int secondsInTimeSlot = 3600; // set 3600 for hourly, 1800 for 30 minutes
		/** end input **/ 

		// Parse available options
		Options opts = createOptions();
		CommandLineParser parser = new PosixParser();
		CommandLine cmd = parser.parse(opts, args);

		// if help needed
		if (cmd.hasOption("h") || args.length==0) {
			HelpFormatter help = new HelpFormatter();
			help.setWidth(90);
			String helpString = "java -jar AnomalyDetection.jar [OPTIONS] DIRTRAINING DIRTEST SECONDSINTIMESLOT\n" 
					+ "Example: java -jar AnomalyDetection.jar anomaly-training anomaly-test 3600\n"
					+"We build a training model from DIRTRAINING as the 'normal' model. " +
					"We build a test model from DIRTEST. Our goal is to test whether the " +
					"test model contains anomaly behavior or not. " +
					"We compare the behaviour in the training model " +
					"with the test model for each timeslot. " +
					"The number of timeslots in a day is determined by SECONDINTIMESLOTS. " +
					"SECONDINTIMESLOTS=3600 means 24 hourly timeslots. "+
					"The output is how different the test model compared " +
					"to the training model for each timeslot in a day. The higher " +
					"the value, the higher the confidence of the time slot being an anomaly. \n" +
					"** DIRTRAINING is a directory for training set, provides the normal behaviour. " +
					"A file is one day behavior, that is a sequence of timeseries (TIMESTART, TIMEEND, VALUE). " +
					"We treat VALUE as symbol, i.e., nominal value " +
					"(suitable for symbolic, rle of symbolic, or state-clustering representation).\n" +
					"** DIRTEST is a directory for test set, provide the to-be-tested behavior. " +
					"When more than one file provided in this directory, it means that we want to test" +
					"an aggregate behavior over days. \n" +
					"** SECONDSINTIMESLOT is the resolution of the anomaly detection. " +
					"For hourly anomaly detection, set this to 3600." +
					"\n OPTIONS: \n";
			help.printHelp(helpString, opts);
			return;
		} 
		if (cmd.hasOption("v")){
			VERBOSE = Integer.parseInt( cmd.getOptionValue("v") );
		}
		
		/** parse available input **/
		dirTrainingStr = args[args.length-3];
		dirTestStr = args[args.length-2];
		secondsInTimeSlot = Integer.parseInt( args[args.length-1] ); // set 3600 for hourly, 1800 for 30 minutes


		// record start time
		long execStart = (new Date()).getTime();

		
		/** load the dataset **/
		int numTimeSlots = 86400 / secondsInTimeSlot;
		Dataset trainingSet = new Dataset();
		trainingSet.loadDirectory(dirTrainingStr);
		trainingSet.sortAsc();
		Dataset testSet = new Dataset();
		testSet.loadDirectory(dirTestStr);
		Log.println(VERBOSE, Constants.LOG_DEBUG, "training set size: "+trainingSet.size());
		Log.println(VERBOSE, Constants.LOG_DEBUG, "test set size: "+testSet.size());

		
		/** compute the model **/
		// compute the distribution in the training set
		Distribution[] distribTraining = new Distribution[numTimeSlots];
		for (int i=0; i<numTimeSlots; i++) distribTraining[i] = new Distribution(secondsInTimeSlot);
		for ( int i=0; i<trainingSet.size(); i++) {
			// cut one day in hourly time slots;
			ArrayList<Entries> OneDayEntries = cutEntries(trainingSet.get(i), secondsInTimeSlot);
			for (int h=0; h<numTimeSlots; h++) {
				distribTraining[h].insert(OneDayEntries.get(h));
			}
		}
		// try to print distrib
		Log.println(VERBOSE, Constants.LOG_DEBUG, "distribution of training set: ");
		for (int i=0; i<distribTraining.length; i++) Log.println(VERBOSE, Constants.LOG_DEBUG, i+": "+distribTraining[i]);
		
		// compute the distribution in the test set
		Distribution[] distribTest = new Distribution[numTimeSlots];
		for (int i=0; i<numTimeSlots; i++) distribTest[i] = new Distribution(secondsInTimeSlot);
		for ( int i=0; i<testSet.size(); i++) {
			// cut one day in hourly time slots;
			ArrayList<Entries> OneDayEntries = cutEntries(testSet.get(i), secondsInTimeSlot);
			for (int h=0; h<numTimeSlots; h++) {
				distribTest[h].insert(OneDayEntries.get(h));
			}
		}
		// try to print distrib
		Log.println(VERBOSE, Constants.LOG_DEBUG, "distribution of test set: ");
		for (int i=0; i<distribTest.length; i++) Log.println(VERBOSE, Constants.LOG_DEBUG, i+": "+distribTest[i]);

		
		/** compute the error of the test set **/
		if (cmd.hasOption("o")) {
			PrintWriter out = new PrintWriter(cmd.getOptionValue("o"));
			out.println("time-slot,anomaly-confidence");
			for (int h=0; h<numTimeSlots; h++) {
				out.println(h+","+distribTest[h].distance( distribTraining[h] ));
			}
			out.close();
		} else {
			System.out.println("time-slot,anomaly-confidence");
			for (int h=0; h<numTimeSlots; h++) {
				System.out.println(h+","+distribTest[h].distance( distribTraining[h] ));
			}
		}
		
		System.err.println("done (" + ( (new Date()).getTime() - execStart ) / 1000.0 + " sec)."  );

	}
	
	public static Options createOptions(){
		Options options = new Options();
		options.addOption("v", "verbose", true, "Print some debugging information [2, 4,or 6].");
		options.addOption("o", "output", true, "Write output to a file.");
		options.addOption("h", "help", false, "Help. Print this message.");		
		return options;	
	}

	
	
	/**
	 * We cut entries into several entries, where each resulting entries is in cutInSeconds long.
	 * @param entries
	 * @param cutInSeconds
	 * @return an array of Entries 
	 */
	private static ArrayList<Entries> cutEntries(Entries entries, long cutInSeconds) {
		if (entries.size() == 0) return null;

		ArrayList<Entries> result = new ArrayList<Entries>();
		
		// get the min timeStart and the max timeEnd, 
		// just in case it is not sorted
		long minTimeStart = Long.MAX_VALUE;
		long maxTimeEnd = Long.MIN_VALUE;
		for (int i=0; i<entries.size(); i++) {
			if ( entries.get(i).timeStart < minTimeStart ) minTimeStart = entries.get(i).timeStart;
			if ( entries.get(i).timeEnd > maxTimeEnd ) maxTimeEnd = entries.get(i).timeEnd;
		}
		
		// create an empty collection
		int arrLen = (int) (((maxTimeEnd - minTimeStart-1) / cutInSeconds ) +1);
		for (int i=0; i<arrLen; i++) {
			Entries e = new Entries();
			result.add(e);
		}
		
		//System.err.println("minTimeStart: " + minTimeStart);
		//System.err.println("maxTimeEnd: " + maxTimeEnd);
		// process each entry in the entries
		for (int i=0; i<entries.size(); i++) {
			long timeStart = entries.get(i).timeStart;
			long timeEnd = entries.get(i).timeEnd;
			// find out in which index they belong to
			int startIdx = (int) ((timeStart - minTimeStart) / cutInSeconds);
			int endIdx = (int) ((timeEnd - minTimeStart-1) / cutInSeconds);
			//System.err.println((timeStart-minTimeStart)+"["+startIdx + "], "+ (timeEnd-minTimeStart) +"[" + endIdx+"]");
			//System.err.println((timeStart)+"["+startIdx + "], "+ (timeEnd) +"[" + endIdx+"]");
			// if both of them are in the same idx
			if (startIdx == endIdx) {
				// add the entry 
				result.get(startIdx).add(entries.get(i));
			} else {
				// add the startIdx
				Entry oldEntry = entries.get(i);
				long endOfStartIdx = (startIdx+1) * cutInSeconds;
				endOfStartIdx += minTimeStart;
				Entry newEntry = new Entry(oldEntry.timeStart, endOfStartIdx, oldEntry.value);
				result.get(startIdx).add(newEntry);
				
				// add all in the middle
				
				for (int j=startIdx+1; j<endIdx; j++) {
					newEntry = new Entry (minTimeStart+(j*cutInSeconds), minTimeStart+((j+1) * cutInSeconds), oldEntry.value);
					result.get(j).add(newEntry);
				}
				
				
				// add the endIdx
				long startOfEndIdx = endIdx * cutInSeconds;
				startOfEndIdx += minTimeStart; 
				newEntry = new Entry(startOfEndIdx, oldEntry.timeEnd, oldEntry.value);
				result.get(endIdx).add(newEntry);
			}
		}
		
		// System.err.println(result);
		return result;
				
	}
	
}


class Distribution{
	
	HashMap<Integer, Long> distribution;
	int normalize;
	int lengthInSeconds;
	public Distribution(int lengthInSeconds){
		this.distribution = new HashMap<Integer, Long>();
		this.normalize = 0;
		this.lengthInSeconds = lengthInSeconds;
	}
	
	public void insert(Entries entries ){
		for (int i=0; i<entries.size(); i++) {
			Entry e = entries.get(i);
			int symbol = (int) (e.value);
			long length = e.timeEnd - e.timeStart;
			// check if this value exist before
			if (distribution.containsKey(symbol)) {
				long prevLength = distribution.get(symbol);
				distribution.put(symbol, prevLength+length);
			} else {
				// if not
				distribution.put(symbol, length);
			}
		}
		normalize=normalize+lengthInSeconds;
	}
	
	public String toString() {
		String r = "";
		for (Map.Entry<Integer, Long> e : distribution.entrySet()) {
			r = r+","+e.getKey()+":"+(e.getValue()/(normalize+0.0));
		}
		return "["+r.substring(1)+"]";
	}
	
	/**
	 * Get a distribution of a symbol
	 * @param symbol
	 * @return
	 */
	public double getDistribution(int symbol){
		if ( distribution.containsKey(symbol) ) {
			return distribution.get(symbol) / (normalize+0.0);
		} else {
			return 0.0;
		}
	}
	
	public double distance(Distribution other) {
		// similarity = the amount of intersection between this and other
		// we compute 1-similarity
		double totalSimilarity = 0.0;
		for (Map.Entry<Integer, Long> e : distribution.entrySet()) {
			int symbol = e.getKey();
			double distrib = e.getValue() / (normalize+0.0);
			double otherDistrib = other.getDistribution(symbol);
			double similarity = Math.min(distrib, otherDistrib);
			totalSimilarity += similarity;
		}
		return 1-totalSimilarity;
	}
}