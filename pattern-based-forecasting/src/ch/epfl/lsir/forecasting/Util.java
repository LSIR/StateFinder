package ch.epfl.lsir.forecasting;

import java.util.ArrayList;

/**
 * 
 * @author Tri Kurniawan Wijaya <tri-kurniawan.wijaya@epfl.ch>
 *
 */
public class Util {
	
	public static void logPrintln(int VERBOSE, int logFlag, Object s) {
		if ((VERBOSE & logFlag) == logFlag) System.err.println(s);
	}

	public static void logPrint(int VERBOSE, int logFlag, Object s) {
		if ((VERBOSE & logFlag) == logFlag) System.err.print(s);
	}
	
	public static ArrayList<Integer> copyArrayList(ArrayList<Integer> arr) {
		ArrayList<Integer> result = new ArrayList<Integer>();
		for (Integer e : arr) result.add(e);
		return result;
	}

	public static String arrayToStr(int[] arr) {
		
		if ( arr.length > 0 ) {
			
			String result="["+arr[0];
			for (int i=1; i<arr.length; i++) result = result + ", " + arr[i]; 			
			return result+"]";
			
		} else return "";
	}

	public static boolean isEqual(int[] a, int[] b) {
		if ( a.length != b.length ) return false;
		for (int i=0; i<a.length; i++) {
			if ( a[i] != b[i] ) {
				return false;
			}
		}
		return true;
	}
}
