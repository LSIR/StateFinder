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
 * Convert the symbols to the centroids of the cluster represented by the symbol.
 * 
 * @author Tri Kurniawan Wijaya <tri-kurniawan.wijaya@epfl.ch>
 * @date Thu 20 Jun 2013 10:33:30 PM IST 
 *
 */
public class SpComputeSymbols {

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
			// 2. from file (gridLabel and centroid) + (raw data all/testing) --> symbol in file, i.e., fileTesting.out
			// parameter: fileModel fileTesting fileSymbols
			String helpString = "java -jar SpComputeSymbols.jar [OPTIONS] FILEMODEL FILETESTING FILESYMBOLS\n" + 
					"Example: java -jar SpComputeSymbols.jar model.sp testing-raw.txt sym.txt\n" +
					"FILEMODEL is the cluster configuration file, can be obtained using spComputeModel.jar\n" +
					"FILETESTING is a timeseries file, comma-separated values of (timestampStart,timestampEnd,value1,value2,...)\n" +
					"FILESYMBOLS is the result file, comma-separated values of (timestampStart, timestampEnd, symbol).\n" +
					"=======================\n" +
					"SpComputeSymbols converts the values (value1,value2, ...) into a symbol, based" +
					" on the cluster configuration in the FILEMODEL. " +		
					"\n OPTIONS: \n";
			help.printHelp(helpString, opts);
			return;
		} 
		int VERBOSE = 0;
		if (cmd.hasOption("v")){
			VERBOSE=0b01;
		} 
		

		String fileModel = args[args.length-3]; 
		String fileTesting = args[args.length-2]; 
		String fileSymbols = args[args.length-1]; 

		Spclust Wave2 = Util.loadSpclustFromFile(fileModel);
		if (VERBOSE > 0) System.err.println(Wave2);
		Wave2.applyCluster(fileTesting, fileSymbols );

		
	}

	public static Options createOptions(){
		Options options = new Options();
		options.addOption("v", "verbose", false, "Print some debugging information.");
		options.addOption("h", "help", false, "Help. Print this message.");		
		return options;	
	}

}
