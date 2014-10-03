package ch.epfl.lsir.spclust.executable;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import ch.epfl.lsir.spclust.Util;
import ch.epfl.lsir.spclust.Spclust;

/**
 * 
 * Compute cluster configuration from raw data 
 * @author Tri Kurniawan Wijaya <tri-kurniawan.wijaya@epfl.ch>
 * @date Thu 20 Jun 2013 10:08:30 PM IST 
 *
 */
public class SpComputeModel {

	/**
	 * @param args
	 * @throws ParseException 
	 */
	public static void main(String[] args) throws ParseException {
		// Parse available options
		Options opts = createOptions();
		CommandLineParser parser = new PosixParser();
		CommandLine cmd = parser.parse(opts, args);
		// if help needed
		if (cmd.hasOption("h") || args.length==0) {
			HelpFormatter help = new HelpFormatter();
			help.setWidth(100);
			// 1. from file (raw data training)--> (gridLabel and centroid) in file, i.e., fileModel
			// parameter: fileTraining, numDimension, gridSize, threshold fileModel
			String helpString = "java -jar SpComputeModel.jar [OPTIONS] FILETRAINING NUMDIMENSION GRIDSIZE THRESHOLD FILEMODEL\n" 
					+ "Example: java -jar SpComputeModel.jar input-raw.txt 2 25 2 model.sp\n"
					+ "FILETRAINING is comma-separated values of timeseries (timestampStart,timestampEnd,value1,value2,...).\n"
					+ "NUMDIMENSION is the dimension of the timeseries in FILETRAINING.\n"
					+ "GRIDSIZE is the width of the grid for each dimension.\n" 
					+ "THRESHOLD is the minimum number of data points for a cluster to be significant.\n"
					+ "FILEMODEL is the file output of the cluster configuration, can be used to convert " +
					"some timeseries into symbols or convert some symbols into raw data.\n"		
					+ "\n OPTIONS: \n";
			help.printHelp(helpString, opts);
			return;
		} 
		int VERBOSE = 0;
		if (cmd.hasOption("v")){
			VERBOSE=0b01;
		} 
		
		String fileTraining = args[args.length-5]; 
		int numDimension = Integer.parseInt(args[args.length-4]);
		int gridSize = Integer.parseInt(args[args.length-3]);
		int threshold = Integer.parseInt(args[args.length-2]);
		String fileModel = args[args.length-1]; 

		Spclust sp = new Spclust(VERBOSE);
		sp.cluster(fileTraining, numDimension, gridSize, threshold);
		Util.saveToFile(sp, fileModel);
		Util.outputClusterNumber(sp, fileModel+"n");

	}

	public static Options createOptions(){
		Options options = new Options();
		options.addOption("v", "verbose", false, "Print some debugging information.");
		options.addOption("h", "help", false, "Help. Print this message.");		
		return options;	
	}

}
