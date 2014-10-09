package ch.epfl.lsir.anomaly;

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
	
	public void add(Entries e) {
		dataset.add(e);
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
			fileNames.add(f.getName());
		}		
	}
	
	public void remove(int index) {
		dataset.remove(index);
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
				}
			}
		}
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
				String[] lineArray = line.split(",");
				Entry e = new Entry( Long.parseLong(lineArray[0]), Long.parseLong(lineArray[1]), Double.parseDouble(lineArray[2]) );
				content.add(e);
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