package ch.epfl.lsir.spclust;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;



/**
 * Simple implementation of Spclust algorithm
 * with no wavelet filter (no hat, no Daubechies filter)
 * this is fine, because the output of this code is the 
 * (max-margin) separator between cluster
 * 
 * Note: if our output is the clusters -- then we might want to 
 * implement the wavelet filter 
 *
 * We consider multidimensional grid. A cell is represented 
 * as an arraylist. Value j at index i means that at 
 * dimension i, this cell is located at coordinate j.  
 * 
 * @author Tri Kurniawan Wijaya <tri-kurniawan.wijaya@epfl.ch>
 * @date Sun 16 Jun 2013 11:42:13 AM CEST 
 * 
 */
public class Spclust implements Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private int VERBOSE;
	
	private int numDimension;
	private int gridSize;
	
	private HashMap<ArrayList<Integer>, Integer> gridCount; // contains the number of data points per grid
	private HashMap<ArrayList<Integer>, Integer> gridLabel;
	private HashMap<ArrayList<Integer>, ArrayList<Double>> gridCentroids;
	private HashMap<Integer, ArrayList<Double>> clusterCentroids;
	
	private int[] maxList; // max value for each dimension
	private int[] minList; // min value for each dimension
	private int[] dimSize; // count of grid for each dimension
	
	public Spclust() {
		// default = no verbose
		VERBOSE = 0;
	}
	
	public Spclust(int VERBOSE) {
		this.VERBOSE = VERBOSE;
	}
	
	public void setVerbose(int VERBOSE) {
		this.VERBOSE = VERBOSE;
	}

	public void cluster(String inputFile, int numDimension, int gridSize, int threshold) {
		
		// initialize internal variable
		this.numDimension = numDimension;
		this.gridSize = gridSize;
		
		maxList = new int[numDimension];
		minList = new int[numDimension];	
		dimSize = new int[numDimension];	

		
		// 1. ReadFile and count the points
		//    fill in: data, maxList, and minList
		HashMap<ArrayList<Integer>, Integer> data = parseInputFile(inputFile);
		Util.log(VERBOSE, 0b01, "num-dimension: " + numDimension + "\n" +  
				"max-points: " + Util.arrToStr(maxList) + "\n" +
				"min-points: " + Util.arrToStr(minList) );
		
		// 2. Grid-ing - put every point count into the grid		
		// try to print gridCount
		computeGridCountAndCentroids(data);
		
		Util.log(VERBOSE, 0b01,"grid-count: ");
		Util.logHashMap(VERBOSE, 0b01, gridCount);
		
		
		// 3. apply the filter/threshold
		//    because we do not apply any filter here, then we simply apply the threshold
		HashMap<ArrayList<Integer>, Integer> gridSign = getGridSignificance(threshold);
		
		Util.log(VERBOSE, 0b01,"grid-sign: ");
		Util.logHashMap(VERBOSE, 0b01, gridSign);

		// 4. Cluster (connected-component) labeling
		/**
		 * put cluster label on the connected component
		 * note: we skip the filtering process,
		 *       this is equal to apply filter [0,0,1,0,0] 
		 * when we do the connected component labeling, 
		 * at the same time, we obtained the separators 
		 */
		gridLabeling(gridSign);
		Util.log(VERBOSE, 0b01,"grid-labeling: ");
		Util.logHashMap(VERBOSE, 0b01, gridLabel);
	
		// 5. expand the cluster
		expandCluster();
		Util.log(VERBOSE, 0b01,"cluster-map: ");
		Util.logHashMap(VERBOSE, 0b01, gridLabel);

		// print grid-centroids		
		computeClusterCentroids();
		Util.log(VERBOSE, 0b01,"cluster-centroids: ");
		Util.logHashMap(VERBOSE, 0b01, clusterCentroids);
	
	}
	
	private void computeClusterCentroids() {
		
		HashMap<Integer, ArrayList<Double>> sumClusterCentroids = new HashMap<Integer, ArrayList<Double>>();
		HashMap<Integer, Integer> memberCount = new HashMap<Integer, Integer>(); 
		for (Map.Entry<ArrayList<Integer>, Integer> entry : gridLabel.entrySet()) {
			// get the grid Index  
			ArrayList<Integer> gridIdx = entry.getKey();
			
			// get the cluster Id
			Integer clusterId = entry.getValue();
			
			// find in the clusterCentroids
			if ( sumClusterCentroids.containsKey(clusterId) ) {
				// if found...
				// get the previous cluster centroids
				ArrayList<Double> oldCentroids = sumClusterCentroids.get(clusterId);
				int oldCount = memberCount.get(clusterId);
				
				// get the new grid centroid
				if ( gridCentroids.containsKey(gridIdx) ) {
					ArrayList<Double> gridCentroid = gridCentroids.get(gridIdx);
					int countPoints = gridCount.get(gridIdx);
					sumClusterCentroids.put(clusterId, Util.additionArrayListDouble(oldCentroids, Util.multiplicationArrayListDouble(gridCentroid, countPoints)));
					memberCount.put(clusterId, oldCount + countPoints);
				}
				
			} else {
				// if not found, put this as the first time
				// multiply the gridCentroids with the gridCount
				if ( gridCentroids.containsKey(gridIdx) ) {
					ArrayList<Double> gridCentroid = gridCentroids.get(gridIdx);
					int countPoints = gridCount.get(gridIdx);
				
					memberCount.put(clusterId, countPoints);
					sumClusterCentroids.put(clusterId, Util.multiplicationArrayListDouble(gridCentroid, countPoints));
				}
			}
					    
		} // end for
		
		clusterCentroids = new HashMap<Integer, ArrayList<Double>>();
		for (Map.Entry<Integer, ArrayList<Double>> entry : sumClusterCentroids.entrySet()) {
			int clusterId = entry.getKey();
			ArrayList<Double> sumCentroid = entry.getValue();
			int count = memberCount.get(clusterId);
			clusterCentroids.put(clusterId, Util.divisionArrayListDouble(sumCentroid, count));
		}
		

		
	}

	private HashMap<ArrayList<Integer>, Integer> getGridSignificance(int threshold) {
		HashMap<ArrayList<Integer>, Integer> gridSign = new HashMap<ArrayList<Integer>, Integer>();
		// iterate over all elements of data		
		for (Map.Entry<ArrayList<Integer>, Integer> entry : gridCount.entrySet()) {
			// get the elements of data  
			ArrayList<Integer> key = entry.getKey();
		    Integer value = entry.getValue();
		    
		    if ( value >= threshold ) {
		    	// keep it
		    	gridSign.put(key, 1);
		    } else {
		    	//gridSign.put(key, 0);
		    }
		}
		return gridSign;
	}
	
	/**
	 * Compute the score for each grid, i.e.,
	 * compute how many points located in each grid
	 * @return
	 */
	private void computeGridCountAndCentroids(HashMap<ArrayList<Integer>, Integer> data) {
		// create the number of grid for each dimension
		dimSize = new int[numDimension]; // the number of grids for each dimension
		for (int i=0; i<numDimension; i++) {
			int rangeDimension = maxList[i] - minList[i];
			dimSize[i] = rangeDimension / gridSize + 1;
		}
		
		
		// we can store gridCount inside Hashmap as:
		// key = [d1, d2, d3] --> arrayList
		// value = count --> the count of points in grid[d1,d2,d3] --> grid score		
		gridCount = new HashMap<ArrayList<Integer>,Integer>();   
		HashMap<ArrayList<Integer>,ArrayList<Integer>> gridSumPoints = new HashMap<ArrayList<Integer>,ArrayList<Integer>>();   

		// iterate over all elements of data
		for (Map.Entry<ArrayList<Integer>, Integer> entry : data.entrySet()) {
		    
			// get the elements of data  
			ArrayList<Integer> key = entry.getKey();
		    Integer value = entry.getValue();
		    
		    // find out the index of each key (or points)
		    ArrayList<Integer> gridIndexes = intToGridIndex(key); 

		    // put into gridCount
		    if ( gridCount.containsKey(gridIndexes) ) {
		    	// adding additional (number of) points
		    	int prevValue = gridCount.get(gridIndexes); 
		    	gridCount.put(gridIndexes, prevValue+value);
		    	
		    	ArrayList<Integer> prevSum = gridSumPoints.get(gridIndexes);
		    	gridSumPoints.put(gridIndexes,Util.additionArrayList(prevSum, Util.multiplicationArrayList(key, value)));
		    } else {
		    	
		    	// put the first number of points here
		    	gridCount.put(gridIndexes, value);
		    	
		    	// first sum of coordinate points 
		    	gridSumPoints.put(gridIndexes, Util.multiplicationArrayList(key,value));
		    }		    
		    
		}
		
		// finalize the grid centroids
		gridCentroids = new HashMap<ArrayList<Integer>, ArrayList<Double> > ();
		for (Map.Entry<ArrayList<Integer>, ArrayList<Integer>> entry : gridSumPoints.entrySet()) {
			ArrayList<Integer> key = entry.getKey();
			int count = gridCount.get(key); 
			ArrayList<Integer> initialCentroids = gridSumPoints.get(key);
			gridCentroids.put(key, Util.divisionArrayList(initialCentroids, count) );
		}
		
	}
	
	
	private ArrayList<Integer> intToGridIndex(ArrayList<Integer> points) {
	    // find out the index of each key (or points)
	    ArrayList<Integer> gridIndexes = new ArrayList<Integer>();		    
	    for (int i=0; i<numDimension; i++) {
	    	int gridIdx = ( points.get(i) - minList[i] ) / gridSize ;
	    	gridIndexes.add( gridIdx );
	    }
	    return gridIndexes;
	}
	
	private void gridLabeling(HashMap<ArrayList<Integer>, Integer> gridSign) {
		
		// gridLabel will contain the cluster label for each grid
		gridLabel = new HashMap<ArrayList<Integer>, Integer>(); 
		
		// iterate over all elements of data
		int clusterCounter = -1;
		// the entry of gridSign (whose values always 1) are the significant grid 
		// (having gridCount > threshold)
		for (Map.Entry<ArrayList<Integer>, Integer> entry : gridSign.entrySet()) {			
			// get the elements of data  
			ArrayList<Integer> key = entry.getKey();
		    if (! gridLabel.containsKey(key)) {
				// if this has not been labeled yet
		    	clusterCounter ++;
		    	// Open this for the old version: 4 neighbors 
		    	//startNewLabeling(key, clusterCounter);
		    	// new version: 8 neighbors labeling
		    	startLabeling(key, clusterCounter, gridSign);
		    }
		}
	}

		
	/**
	 * Label the connected cell from the startCell by clusterId
	 * @param startCell
	 * @param clusterId
	 */
	private void startLabeling(ArrayList<Integer> startCell, int clusterId, HashMap<ArrayList<Integer>, Integer> gridSign){
		
		// if the startCell has not included in the cluster, add it
		if ( !gridLabel.containsKey(startCell) ) {
			gridLabel.put(startCell, clusterId);
		}
		
		// create a queue for other to-be-examined cells 
		ArrayList<ArrayList<Integer>> queueCell = new ArrayList<ArrayList<Integer>>();
		queueCell.add(startCell);
		
		while ( queueCell.size() > 0 ) {
			// pop the first elements under consideration
			ArrayList<Integer> currCell = queueCell.remove(0);
			
			// find the neighbors of startCell
			ArrayList<ArrayList<Integer>> neighbors = Util.get8Neigbor(currCell);
			
			// look over the neighbor if there is significant cell, but unclustered yet
			for (int i=0; i<neighbors.size(); i++) {
				ArrayList<Integer> neighbor = neighbors.get(i);
				if ( gridSign.containsKey(neighbor) && !gridLabel.containsKey(neighbor)) {
					gridLabel.put(neighbor, clusterId);
					queueCell.add(neighbor);
				}
			}
			
		}
	}

	
	// 	loop until grid labeling has size of all dimension
	private void expandCluster(){
		ArrayList<ArrayList<Integer>> queue = new ArrayList<ArrayList<Integer>>();
		// add all in gridLabel to the queue
		for (ArrayList<Integer> key : gridLabel.keySet()) {
			queue.add(key);
		}
		
		while ( queue.size() > 0 ) {
			ArrayList<Integer> cell = queue.remove(0);
			
			// all neighbors should have this cluster Id
			int clusterId = gridLabel.get(cell);
			
			// search for all neighboring cells
			ArrayList<ArrayList<Integer>> neighbors = Util.get8Neigbor(cell);
			for (ArrayList<Integer> n : neighbors) {
				
				// check if the cell is valud
				if ( ! isValidCell(n) ) continue;
				
				// if has not been marked yet
				if (! gridLabel.containsKey(n)) {
					gridLabel.put(n, clusterId);
					queue.add(n);
				}
			}
		}
		
	}
	
	
	private boolean isValidCell(ArrayList<Integer> cell) {
		if (cell.size() != numDimension ) return false;
		for (int i=0; i<numDimension; i++ ) {
			// coordinate start at 0
			if ( cell.get(i) < 0 ) return false;
			// coordinate end at dimSize[i] -1 
			if (cell.get(i) >= dimSize[i]) return false;
		}
		return true;
	}
	
	
	
	/**
	 * Parse the input file to get data, maxList, and minList 
	 * @param inputFile
	 */	
	private HashMap<ArrayList<Integer>, Integer> parseInputFile(String inputFile) {
		
		try {
			HashMap<ArrayList<Integer>, Integer> data = new HashMap<ArrayList<Integer>, Integer>(); 
			// read file input
			BufferedReader in = new BufferedReader (new FileReader(inputFile));
			
			// read the first line to initialize the max and min list
			String line;
			if ( (line=in.readLine()) != null) {
				ArrayList<Integer> points = parseLine(line);

				// put in the data map, with counter=1
				data.put(points, 1);
				
				// first point, put in max and min list
				for (int i=0; i<numDimension; i++) {
					maxList[i]=points.get(i);
					minList[i]=points.get(i);
				}
				
			}

			// continue read for the next data
			while ((line=in.readLine())!=null) {
				ArrayList<Integer> points = parseLine(line);

				// put in the data map, increase the counter
				if ( data.containsKey(points) ) {
					int count = data.get(points);
					data.put(points, count+1);
				} else {
					data.put(points, 1);
				}
				
				// update max and min list
				for (int i=0; i<numDimension; i++) {
					if ( points.get(i) > maxList[i] ) {
						maxList[i] = points.get(i);
					} else 					
					if ( points.get(i) < minList[i] ) {
						minList[i] = points.get(i);		
					}
				}			
			}
			in.close();
			
			return data;
			
		} catch (IOException e) {
			e.printStackTrace();
		} 
			
		// if there is something fail
		return null;
	}

	
	/**
	 * Split a line into a list of integer 
	 * @param line
	 * @return
	 */
	private ArrayList<Integer> parseLine(String line){
		String[] lineArray = line.split(",");
		ArrayList<Integer> points = new ArrayList<Integer>();

		// the first index is the timestamp, skip it (start from 1)
		for (int i=2; i<(numDimension+2); i++) {
			// get the points
			points.add(Integer.parseInt(lineArray[i]));
		}
		return points;
	}
	

	public String toString() {
		String result;
		result = "num-dimension: " + numDimension + "\n" +  
				"grid-size: " + gridSize + "\n" +
				"min-points: " + Util.arrToStr(minList) + "\n" +
				"max-points: " + Util.arrToStr(maxList) + "\n" +
				"dimensions: " + Util.arrToStr(dimSize) + "\n" +
				"grid-label: " + gridLabel + "\n" +
				// "grid-centroids: " + gridCentroids + "\n" +
				"cluster-centroids: " + clusterCentroids;
		
		return result;
	}
	
	public void applyCluster(String fileInput, String fileOutput) {
		try {
			BufferedReader in = new BufferedReader (new FileReader(fileInput));
			PrintWriter out = new PrintWriter(fileOutput);
			String line;
			String[] lineArray;
			int countLine = 0;
			while ( ( line=in.readLine() ) != null ) {
				countLine++;
				lineArray = line.split(",");
				// minus 1 because we have timestamp as the first column.
				int numDim = lineArray.length-2;
				// check the number of dimension
				if ( numDim != this.numDimension ) {
					System.err.println("[WARNING] Spclust.applyCluster: " +
							numDim + " dimensional values read in line " + countLine +
							", dimension expected: " + numDimension );
				}
				// put each value in an array lists 
				ArrayList<Integer> values = new ArrayList<Integer> ();
				for (int i=2; i<lineArray.length; i++) {
					values.add(Integer.parseInt(lineArray[i]));
				}
				
				// convert each value to a grid index
				ArrayList<Integer> gridIndex = intToGridIndex(values);
				
				// if there are some index outside the original coordinate space
				// we set them either to 0 or to maximum coordinate in that dimension
				indexCompaction(gridIndex);
				
				// find out which cluster it is
				int clusterId = gridLabel.get(gridIndex);
				
				// write it : keep the timestamp
				out.println(lineArray[0]+","+lineArray[1]+","+clusterId);
			}
			in.close();
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	private void indexCompaction(ArrayList<Integer> cell) {
		// idx at dimension i starts from 0 and ends at numDimension[i] - 1
		// cell size should be equal to numDimension
		for (int i=0; i<cell.size(); i++) {
			if (cell.get(i) < 0) 
				cell.set(i, 0);
			else if (cell.get(i) >= dimSize[i]) 
				cell.set(i, dimSize[i]-1);
		}
		
	}

	public void getCentroids(String fileSymbols, String fileCentroids) {
		try {
			BufferedReader in = new BufferedReader (new FileReader(fileSymbols));
			PrintWriter out = new PrintWriter(fileCentroids);
			String line;
			String[] lineArray;
			while ( ( line=in.readLine() ) != null ) {
				lineArray = line.split(",");
				
				int clusterId = Integer.parseInt( lineArray[2] );
				
				ArrayList<Double> centroids = clusterCentroids.get(clusterId);
				
				// write it : keep the timestamp
				out.println( lineArray[0] + "," + lineArray[1] + "," +Util.arrayToFlatStr(centroids) );
			}
			in.close();
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public int getClusterNumber() {
		return clusterCentroids.size();
	}
	
}
