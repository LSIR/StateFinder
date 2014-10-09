package ch.epfl.lsir.anomaly;

/**
 * 
 * @author Tri Kurniawan Wijaya <tri-kurniawan.wijaya@epfl.ch>
 * 
 */
public class Log {
	
	public static void println(int VERBOSE, int logFlag, Object s) {
		if ((VERBOSE & logFlag) == logFlag) System.err.println(s);
	}

	public static void print(int VERBOSE, int logFlag, Object s) {
		if ((VERBOSE & logFlag) == logFlag) System.err.print(s);
	}
	
}
