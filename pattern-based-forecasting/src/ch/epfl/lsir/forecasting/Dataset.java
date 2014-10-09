package ch.epfl.lsir.forecasting;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

/**
 * 
 * @author Tri Kurniawan Wijaya <tri-kurniawan.wijaya@epfl.ch>
 *
 */
public class Dataset{
	
	ArrayList<Entries> dataset;
	ArrayList<String> fileNames;
	
	public Dataset(){
		dataset= new ArrayList<Entries>();
		fileNames = new ArrayList<String>();
	}

	public int size() {
		return dataset.size();
	}
	
	public Entries get(int index) {
		return dataset.get(index);
	}
	
	public String getName(int index) {
		return fileNames.get(index);
	}
	
	public void add(Entries e) {
		dataset.add(e);
	}

	public void add(Entries e, String name) {
		dataset.add(e);
		fileNames.add(name);
	}

	/**
	 * Ordered by timeStart.
	 */
	public void sortAsc(){
		for (int i=0; i<dataset.size()-1; i++) {
			for (int j=i+1; j<dataset.size(); j++) {
				// get the i-th element, get the first entry, 
				// get the j-th element, get the first entry, compare their timeStarts
				if ( dataset.get(i).get(0).timeStart > dataset.get(j).get(0).timeStart ) {
					// swap
					Entries temp = dataset.get(i);
					dataset.set(i, dataset.get(j));
					dataset.set(j, temp);
					
					String tempStr = fileNames.get(i);
					fileNames.set(i, fileNames.get(j));
					fileNames.set(j, tempStr);
				}
			}
		}
	}
	
	/**
	 * The result is not ordered
	 * @param dirInputStr
	 */
	public void loadDirectory(String dirInputStr) {
		File dirInput = new File(dirInputStr);
		for (File f : dirInput.listFiles()) {
			Entries e = new Entries ( f.getAbsolutePath() );
			// sort e by timeStart
			//e.sortTimeStartAsc();
			dataset.add(e);
			fileNames.add( f.getName() );
		}		
	}
	
	public void remove(int index) {
		dataset.remove(index);
		fileNames.remove(index);
	}
}



class Entries{
	
	private ArrayList<Entry> content;
	
	public Entries(ArrayList<Entry> other) {
		 content = new ArrayList<Entry>();
		 for (int i=0; i<other.size(); i++) {
			 content.add(other.get(i));
		 }
	}
	
	public Entries(String fileName) {
		fileToEntry(fileName);
	}
	
	public Entries(){
		content = new ArrayList<Entry>();
	}
	
	public void add(Entry e) {
		content.add(e);
	}
	
	public Entry get(int index) {
		return content.get(index);
	}
	
	public int size() {
		return content.size();
	}
	
	public void fileToEntry(String fileName) {		
		try {
			BufferedReader in = new BufferedReader(new FileReader(fileName));
			content = new ArrayList<Entry>();
			String line;
			while ( (line=in.readLine())!=null ) {
				if ( ! line.equalsIgnoreCase("null") ) { 
					String[] lineArray = line.split(",");
					Entry e = new Entry( Long.parseLong(lineArray[0]), Long.parseLong(lineArray[1]), Double.parseDouble(lineArray[2]) );
					content.add(e);
				}
			}
			in.close();
		} catch ( NumberFormatException | IOException e1) {
			e1.printStackTrace();
		}		
	}	
	
	public String toString(){
		return content.toString();
	}

	public void sortTimeStartAsc(){
		for (int i=0; i<content.size()-1; i++) {
			for (int j=i+1; j<content.size(); j++) {
				// get the i-th element, get the j-th element, compare their timeStarts
				if ( content.get(i).timeStart > content.get(j).timeStart ) {
					// swap
					Entry temp = content.get(i);
					content.set(i, content.get(j));
					content.set(j, temp);
				}
			}
		}
		
	}

	/**
	 * The entries are assumed to be ordered by timeStart
	 * Entries is a sequence of entry (ts, te, value), hence time series.
	 * @param other
	 * @return
	 */
	public Double getDistance(int distanceType, Entries other, int VERBOSE) {
		
		int logFlag = 0b10;
		
		// if one of the entries contains nothing, then we cannot 
		// compute the distance
		if (other.size()==0) return null;
		if (this.size() ==0) return null;
		
		// say, we have two timeSeries 1 and 2
		// our assumption timeEnd(i) = timeStart(i+1)
		long initStart1 = this.get(0).timeStart;
		long initStart2 = other.get(0).timeStart;
		
		long len1 = this.get(this.size()-1).timeEnd - initStart1;
		long len2 = other.get(other.size()-1).timeEnd - initStart2;
		
		if (len1 != len2 ) {
			System.err.print("[Dataset.getDistance] Error: Both entries should have the same length.");
			System.exit(1);
		}
		
		Util.logPrintln(VERBOSE, logFlag, "compute distance, length: "+len1);

		// current entry under consideration
		int cur1 = 0;
		int cur2 = 0;
		long min1 = this.get(cur1).timeStart - initStart1;
		long min2 = other.get(cur2).timeStart - initStart2;
		
		if (min1 != min2 ) {
			System.err.print("[Dataset.getDistance] Error: Problem in initial time start.");
			System.exit(1);
		}
		
		long nextMin1 = this.get(cur1).timeEnd - initStart1;
		long nextMin2 = other.get(cur2).timeEnd - initStart2;
		double sym1Double = this.get(cur1).value;
		double sym2Double = other.get(cur2).value;
		
		// choose either min1 or min2 (both should have the same value at this point, i.e., 0)
		long post = min1;
		
		double dist = 0.0;
		while (true) {
			if ( nextMin1 < nextMin2 ) {
				
				// compute symbol differences
				long length = nextMin1 - post;
				dist = dist + computeDistance(distanceType, sym1Double, sym2Double, length);
				
				// update the post
				post = nextMin1;
				Util.logPrintln(VERBOSE, logFlag, "post on ts1: "+ (post+initStart1));
				
				// increase our lookup cursor on timeSeries1
				cur1 ++;

				// update info on timeSeries 1
				min1 = this.get(cur1).timeStart - initStart1;
				nextMin1 = this.get(cur1).timeEnd - initStart1;
				sym1Double = (int) (this.get(cur1).value);
				
			} else {
				

				long length = nextMin2 - post;
				dist = dist + computeDistance(distanceType, sym1Double, sym2Double, length);
				
				post = nextMin2;
				Util.logPrintln(VERBOSE, logFlag, "post on ts2: "+ (post+initStart2) + ", "+(nextMin2 + initStart2));
				
				// increase our lookup cursor on timeSeries2
				cur2 ++;
				if ( cur2 >= other.size() ) break;
				
				min2 = other.get(cur2).timeStart - initStart2;
				nextMin2 = other.get(cur2).timeEnd - initStart2;
				sym2Double = (int) (other.get(cur2).value);
				
			}
		}
		
		return dist;
	}

	private double computeDistance(int distanceType, double sym1Double, double sym2Double, long length) {
		double dist = 0.0;
		if ( distanceType == Constants.SYMBOLIC) {
			int sym1Int = (int) sym1Double;
			int sym2Int = (int) sym2Double;
			if (sym1Int != sym2Int ) dist = length;
		} else 
		if ( distanceType == Constants.REALVALUES) {
			double diff = Math.abs(sym1Double - sym2Double);
			dist = length * diff;
		}
		return dist;
	}
	
}



class Entry{
	long timeStart;
	long timeEnd;
	double value;
	public Entry(long timeStart, long timeEnd, double value){
		this.timeStart = timeStart;
		this.timeEnd = timeEnd;
		this.value = value;
	}
	
	public String toString() {
		return "["+timeStart+","+timeEnd+","+value+"]";
	}
}