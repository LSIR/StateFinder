package ch.epfl.lsir.spclust;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Util class (some common functions) for Spclust
 * @author Tri Kurniawan Wijaya <tri-kurniawan.wijaya@epfl.ch>
 * @date Sun 16 Jun 2013 11:42:13 AM CEST 
 *
 */
public class Util {

	/**
 	 * @param args
	 */
	public static ArrayList<Integer> copyArrayList(ArrayList<Integer> arr) {
		ArrayList<Integer> res = new ArrayList<Integer>();
		for (int i=0; i<arr.size(); i++) {
			res.add(arr.get(i));
		}
		return res;
	}

	
	public static ArrayList<Double> copyArrayListDouble(ArrayList<Double> arr) {
		ArrayList<Double> res = new ArrayList<Double>();
		for (int i=0; i<arr.size(); i++) {
			res.add(arr.get(i));
		}
		return res;
	}
	
	public static ArrayList<ArrayList<Integer>> get8Neigbor(ArrayList<Integer> cell) {
		// we represent cell as an arraylist:
		// value j at index i means that 
		// at dimension i, this cell is located at coordinate j
		ArrayList<ArrayList<Integer>> neighbors = new ArrayList<ArrayList<Integer>>();
		
		// for the first dimension		
		int currCoordinate = cell.get(0);
		// add this element
		ArrayList<Integer> element = new ArrayList<Integer>();
		element.add(currCoordinate);
		neighbors.add(element);

		// add upper neighbor
		element = new ArrayList<Integer>();		
		element.add(currCoordinate+1);
		neighbors.add(element);

		// add lower neighbor
		element = new ArrayList<Integer>();		
		element.add(currCoordinate-1);
		neighbors.add(element);

		// numOfDimension is cell.size()
		for (int i=1; i<cell.size();i++) {
			int numPrevElements = neighbors.size();
			// pop all of the previous elements
			currCoordinate = cell.get(i);
			for (int j=0; j<numPrevElements; j++) {
				ArrayList<Integer> candidateElements = neighbors.remove(0);
				// insert the new ones
				ArrayList<Integer> newElements = Util.copyArrayList(candidateElements);
				newElements.add(currCoordinate);
				neighbors.add(newElements);
				
				newElements = Util.copyArrayList(candidateElements);
				newElements.add(currCoordinate+1);
				neighbors.add(newElements);

				newElements = Util.copyArrayList(candidateElements);
				newElements.add(currCoordinate-1);
				neighbors.add(newElements);
			}
		}
		
		// remove the original cell 
		neighbors.remove(neighbors.indexOf(cell));
		
		return neighbors;
	}

	/**
	 * Perform addition on ArrayList a+b
	 * @param a
	 * @param b
	 */
	public static ArrayList<Integer> additionArrayList(ArrayList<Integer> a, ArrayList<Integer> b) {
		ArrayList<Integer> result = copyArrayList(b);
		for (int i=0; i<a.size(); i++) {
			result.set(i, a.get(i) + b.get(i));
		}		
		return result;
	}

	public static ArrayList<Double> additionArrayListDouble(ArrayList<Double> a, ArrayList<Double> b) {
		ArrayList<Double> result = copyArrayListDouble(b);
		for (int i=0; i<a.size(); i++) {
			result.set(i, a.get(i) + b.get(i));
		}		
		return result;
	}

	/**
	 * Scalar multiplication between arr and value
	 * @param arr
	 * @param value
	 * @return
	 */
	public static ArrayList<Integer> multiplicationArrayList(ArrayList<Integer> arr, Integer multiplication) {
		ArrayList<Integer> result = copyArrayList(arr);
		for (int i=0; i<result.size(); i++) {
			result.set(i, result.get(i) * multiplication);
		}
		return result;
	}

	public static ArrayList<Double> multiplicationArrayListDouble(ArrayList<Double> arr, Integer multiplication) {
		ArrayList<Double> result = copyArrayListDouble(arr);
		for (int i=0; i<result.size(); i++) {
			result.set(i, result.get(i) * multiplication);
		}
		return result;
	}

	/**
	 * divide each element in arr with n.
	 * @param arr
	 * @param n
	 * @return
	 */
	public static ArrayList<Double> divisionArrayList(ArrayList<Integer> arr, int n) {
		ArrayList<Double> result = new ArrayList<Double>();
		for (int i=0; i<arr.size(); i++) {
			result.add( (arr.get(i)+0.0 ) / (n + 0.0) );
		}
		return result;
	}

	public static ArrayList<Double> divisionArrayListDouble(ArrayList<Double> arr, int n) {
		ArrayList<Double> result = new ArrayList<Double>();
		for (int i=0; i<arr.size(); i++) {
			result.add( (arr.get(i)+0.0 ) / (n + 0.0) );
		}
		return result;
	}

	/**
	 * Print message only if the bit in VERBOSE activates
	 * the bit in flagVerbose
	 * @param VERBOSE
	 * @param flagVerbose
	 * @param message
	 */
	public static void log(int VERBOSE, int flag, String message) {
		if ( (VERBOSE & flag) == flag) {
			System.err.println(message);
		}
	}
	
	/**
	 * Log HashMap, if whatever bit activated in flag is also activated in VERBOSE
	 * @param VERBOSE
	 * @param flag
	 * @param hmap
	 */
	public static void logHashMap(int VERBOSE, int flag, HashMap<?, ?> hmap) {
		// try to print gridIndexes
		
		for (Map.Entry< ? , ?> entry : hmap.entrySet()) {
		    
			log(VERBOSE, flag, entry.getKey() + " : " + entry.getValue());
		}
				
	}


	public static String arrToStr(int[] arr) {
		String result = "";
		if (arr.length > 0) {
			result = "[0]=" + arr[0];
		}
		for (int i=1; i<arr.length; i++) {
			result = result + ", [" + i + "]=" + arr[i];
		}
		return result;
	}


	public static String arrayToFlatStr(ArrayList<Double> arr) {
		String result = "";
		if (arr.size() > 0) {
			result = "" + arr.get(0);
		}
		for (int i=1; i<arr.size(); i++) {
			result = result + "," + arr.get(i);
		}
		return result;
	}
	
	public static void saveToFile(Object obj, String fileName) {
		try {
			FileOutputStream fileOut = new FileOutputStream(fileName);
			ObjectOutputStream out = new ObjectOutputStream(fileOut);
			out.writeObject(obj);
			out.close();
			fileOut.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	public static void outputClusterNumber(Spclust w, String fileName){
		try{
			FileWriter fw = new FileWriter(fileName);
			fw.write(""+w.getClusterNumber());
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}


	public static Spclust loadSpclustFromFile(String fileName) {
		try {
			FileInputStream fileIn = new FileInputStream(fileName);
			ObjectInputStream in = new ObjectInputStream(fileIn);
			Spclust sp = (Spclust) in.readObject();
			in.close();
			fileIn.close();
			return sp;
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	}

}
