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
 * Compute symbols from timeseries and cluster configuration.
 * 
 * Note: The optimal approach will be to revert each data points
 * into its original raw values. But we cannot do that because
 * we do not store it (because of our main goal to reduce 
 * storage space). 
 *    
 * @author Tri Kurniawan Wijaya <tri-kurniawan.wijaya@epfl.ch>
 * @date Thu 20 Jun 2013 10:50:30 PM IST 
 *
 */
public class SpGetRaw {

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
			help.setWidth(90);
			// 3. from file (symbol) + (gridLabel and centroid) --> raw/real-values in file, i.e.,
			// parameter: fileModel fileSymbols fileCentroids
			String helpString = "java -jar SpGetRaw.jar [OPTIONS] FILEMODEL FILESYMBOLS FILECENTROIDS\n" + 
					"Example: java -jar SpGetRaw.jar model.sp sym.txt raw-centroids.txt\n" +
					"FILEMODEL is the cluster configuration file, can be obtained using SpComputeModel.\n" +
					"FILESYMBOLS is a comma-separated values of (timestampStart,timestampEnd,symbol), can be obtained using SpComputeSymbols.\n" +
					"FILECENTROIDS is the result file, comma-separated values of (timestampStart,timestampEnd,value1,value2, ...) " +
					"obtained from FILESYMBOLS by converting each symbol to its cluster centroids." +
					"Note that each symbol represents a cluster (or: symbol = cluster id)." +
					"\n OPTIONS: \n";
			help.printHelp(helpString, opts);
			return;
		} 
		int VERBOSE = 0;
		if (cmd.hasOption("v")){
			VERBOSE=0b01;
		} 
		

		String fileModel = args[args.length-3]; 
		String fileSymbols = args[args.length-2]; 
		String fileCentroids = args[args.length-1]; 

		Spclust Wave = Util.loadSpclustFromFile(fileModel);
		if (VERBOSE > 0) System.err.println(Wave);
		Wave.getCentroids(fileSymbols, fileCentroids);
		
	}

	public static Options createOptions(){
		Options options = new Options();
		options.addOption("v", "verbose", false, "Print some debugging information.");
		options.addOption("h", "help", false, "Help. Print this message.");		
		return options;	
	}

}
