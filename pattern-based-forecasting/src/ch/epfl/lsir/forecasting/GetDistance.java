package ch.epfl.lsir.forecasting;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

public class GetDistance {

	/**
	 * @param args
	 * @throws ParseException 
	 */
	public static void main(String[] args) throws ParseException {
		// TODO Auto-generated method stub
		
		// entries are assumed to be sorted
		// TODO give option if the entry are not sorted?
		int VERBOSE = Constants.LOG_NONE;
		
		// Parse available options
		Options opts = createOptions();
		CommandLineParser parser = new PosixParser();
		CommandLine cmd = parser.parse(opts, args);

		// if help needed
		if (cmd.hasOption("h") || args.length==0) {
			HelpFormatter help = new HelpFormatter();
			help.setWidth(100);
			String helpString = "java -jar GetDistance.jar [OPTIONS] FILE1 FILE2\n" 
					+ "Example: java -jar GetDistance.jar distance-input-ex1.csv distance-input-ex2.csv\n" +
					"Compute the distance between FILE1 and FILE2. \n" +
					"FILE1 and FILE2 is a timeseries file of form (TIMESTART, TIMEEND, VALUE). " +
					"VALUE is of real-values." +
					"\n OPTIONS: \n";
			help.printHelp(helpString, opts);
			return;
		} 

		String file1 = args[args.length-2];
		String file2 = args[args.length-1];
		
		Entries e1 = new Entries(file1);
		Entries e2 = new Entries(file2);
	
        int type = cmd.hasOption("a") ? Constants.SYMBOLIC : Constants.REALVALUES;
        // compute the difference using hamming distance
		Double diff = e1.getDistance(type, e2, VERBOSE);
        
		// compute the time length
		Long timeLen;
		if (diff == null) timeLen = null;
			else timeLen = e1.get(e1.size()-1).timeEnd - e1.get(0).timeStart;

		// compute normalized distance
		Double normalized;
		if ( diff == null ) normalized=null;
			else normalized = diff/timeLen; 
				
		if (cmd.hasOption("s")){ 
		
			System.out.println(normalized);
		
		} else {
			System.out.println("total-distance (d) = "+diff);
			System.out.println("total-time-length (l) = "+timeLen);
			System.out.println("normalized-distance (d/l) = "+normalized);
		}
		
	}
	public static Options createOptions(){
		Options options = new Options();
		options.addOption("h", "help", false, "Help. Print this message.");		
		options.addOption("s", "short", false, "Output only the normalized distance by length.");	
		options.addOption("a", "hamming", false, "Use hamming distance to compare symbols.");
		return options;	
	}

}
 
