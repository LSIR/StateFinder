package ch.epfl.lsir.forecasting;

import java.util.ArrayList;
import java.util.Random;

public class Cluster {
	// static int logFlag = 0b10;

	public static double kMedoids(int k, Dataset trainingSet, double[][] distance, int VERBOSE, int randSeed, ArrayList<Integer> medoidsResult, ArrayList<ArrayList<Integer>> clustersResult){

		int logFlag = Constants.LOG_KMEDOIDS;
		//Random rand = new Random(randomSeed);
		Random rand = new Random(randSeed);
		
		
		// choose random data point 
		ArrayList<Integer> medoids = new ArrayList<Integer>();
		while (medoids.size()<k) {
			int idx = (int) (rand.nextDouble() * trainingSet.size());
			if ( medoids.indexOf(idx) < 0 ) medoids.add(idx);
		}
		
		Util.logPrintln(VERBOSE, logFlag, "Initial medoids: "+medoids);
		
		while (true) {
			
			// create cluster assignment:
			ArrayList<ArrayList<Integer>> clusters = new ArrayList<ArrayList<Integer>>();
			for (int i=0; i<k; i++) {
				ArrayList<Integer> cluster = new ArrayList<Integer> ();
				// add the medoid as the first member of this cluster
				cluster.add(medoids.get(i));
				clusters.add( cluster );
			}

			// cluster assignment for other points
			for (int i=0; i<trainingSet.size(); i++) {
				if (medoids.indexOf(i) >= 0) continue;
				// compute distance for this data points to each medoids,
				// select the min one
				double minDist = distance[i][medoids.get(0)];
				int clusterLabel = 0;
				for (int j=1; j<k; j++) {
					if ( distance[i][medoids.get(j)] < minDist ) {
						// update the cluster label;
						minDist = distance[i][medoids.get(j)];
						clusterLabel = j;
					}
				}
				// set the cluster label
				clusters.get(clusterLabel).add(i);
			}
					
			// print assignments:
			for (int i=0; i<k; i++) {
				Util.logPrintln(VERBOSE, logFlag, "cluster "+i+": "+clusters.get(i));
			}
			
			// compute the distance of the initial cluster configuration
			ArrayList<Double> intraClusterDist = new ArrayList<Double>();
			for (int i=0; i<k; i++) {			
				intraClusterDist.add( sumRowAgainstColumn(distance, medoids.get(i), clusters.get(i)) );
			}
			// print initial distance
			Util.logPrintln(VERBOSE, logFlag, "intra-cluster distance: "+intraClusterDist);
			
			// update centroids
			ArrayList<Integer> newMedoids = Util.copyArrayList(medoids);
			
			// for each medoids
			for (int i=0; i<k; i++) {
				double minDist = intraClusterDist.get(i); 
				int currMedoids = medoids.get(i);
				
				// for each other points
				for (int j=0; j<clusters.get(i).size(); j++){
					if ( clusters.get(i).get(j) == medoids.get(i) ) continue;
					// process only if this is not medoids
					double currDist = sumRowAgainstColumn(distance, clusters.get(i).get(j), clusters.get(i));
					if ( currDist < minDist ) {
						minDist = currDist;
						currMedoids = clusters.get(i).get(j);
					}
				}
				// after all inspection
				newMedoids.set(i,currMedoids);
			}
			
			// print the new cluster configuration and its intra-cluster distance
			Util.logPrintln(VERBOSE, logFlag, "current medoids: "+newMedoids);
			ArrayList<Double> newIntraClusterDist = new ArrayList<Double>();
			for (int i=0; i<k; i++) {			
				newIntraClusterDist.add( sumRowAgainstColumn(distance, newMedoids.get(i), clusters.get(i)) );
			}
			Util.logPrintln(VERBOSE, logFlag, "new intra-cluster distance: "+newIntraClusterDist);
			
			// if no change, we stop
			if (newMedoids.equals(medoids)) {
				double sumDist = 0;
				for (int i=0; i<k; i++) {
					sumDist = sumDist + newIntraClusterDist.get(i);
				}
				for (int i=0; i<k;i++) {
					medoidsResult.add(medoids.get(i));
					clustersResult.add(clusters.get(i));
				}
				return sumDist;
			}
			
			medoids = newMedoids;		
		}
		
	}

	public static double sumRowAgainstColumn(double[][] distance, int row, ArrayList<Integer> cols) {
		double sum = 0;
		for (int i=0; i<cols.size(); i++) {
			sum = sum + distance[row][cols.get(i)];
		}
		return sum;
				
	}

	
	public static double getSilhouette(Dataset trainingSet, ArrayList<ArrayList<Integer>> clusters, double[][] distance, int VERBOSE) {
		int logFlag = Constants.LOG_SILHOUETTE;
		/** initial phase **/
		// build an inverted index of cluster assg, i.e., clusterAssignment[objectNo] = clusterId;
		int[] clusterAssignment = new int[trainingSet.size()];
		for (int i=0; i<clusters.size(); i++) {
			for (int j=0; j<clusters.get(i).size(); j++){
				clusterAssignment[clusters.get(i).get(j)] = i;	
			}			
		}
	
		
		/** compute silhouette(i) **/
		//ArrayList<Double> silh = new ArrayList<Double> ();
		double sumSilh = 0.0;
		for (int i=0; i<trainingSet.size(); i++) {
			// compute a(i)
			double ai = (sumRowAgainstColumn(distance, i, clusters.get(clusterAssignment[i])) + 0.0)/ (clusters.get(clusterAssignment[i]).size() + 0.0);
			Util.logPrintln(VERBOSE, logFlag, "item "+i+", ai:"+ai+", clusterId: "+clusterAssignment[i]+", fellow-cluster: "+ clusters.get(clusterAssignment[i]));
			
			// compute b(i)
			// average dissimilarity with other clusters
			double minBi = Double.MAX_VALUE;
			for (int j=0; j<clusters.size(); j++) {
				if (j!=clusterAssignment[i]) {
					double bis = (sumRowAgainstColumn(distance, i, clusters.get(j)) + 0.0 ) / clusters.get(j).size();
					if ( bis < minBi ) minBi = bis; 
				}
			}
			
			Double maxAiBi = Math.max(ai, minBi);
			double silh = ( (minBi - ai) / maxAiBi);
			sumSilh += silh;
			Util.logPrintln(VERBOSE, logFlag, ai+","+minBi + ", " + (minBi - ai) +"," + silh);
						
		}
		
		double silhouette = sumSilh / trainingSet.size();
		Util.logPrintln(VERBOSE, logFlag, "silhouette: "+silhouette);
		return silhouette;
		
	}

	private static double interClusterDistanceTwoClosestPoints(ArrayList<Integer> cluster1, ArrayList<Integer> cluster2, double[][] distance, int VERBOSE, int logFlag) {
		double minDist = Double.MAX_VALUE;
		for (int x=0; x<cluster1.size(); x++) {
			for (int y=0; y<cluster2.size(); y++) {
				double dist = distance[cluster1.get(x)][cluster2.get(y)];
				if ( dist < minDist ) {
					minDist = dist;
				}
				Util.logPrintln(VERBOSE, logFlag, "dist["+cluster1.get(x)+","+cluster2.get(y)+"]: "+dist+", interClusterDist: "+minDist);
			}
		}
		return minDist;
	}
	
	
	public static double getDunnIndex(ArrayList<ArrayList<Integer>> clusters, double[][] distance, int VERBOSE) {
		
		int logFlag = Constants.LOG_DUNNINDEX;
		
		// get the min inter cluster distance
		double MINDIST = Double.MAX_VALUE;
		for (int i=0; i<clusters.size()-1; i++){
			for (int j=i+1; j<clusters.size(); j++) {
				// test for each element in each cluster
				ArrayList<Integer> clusterI = clusters.get(i);
				ArrayList<Integer> clusterJ = clusters.get(j);
				
				double minDist = interClusterDistanceTwoClosestPoints(clusterI, clusterJ, distance, VERBOSE, logFlag);
				
				if ( minDist < MINDIST ) {
					MINDIST = minDist;
				}
				Util.logPrintln(VERBOSE, logFlag, "DUNN: minDist: "+minDist+", MINDIST: "+MINDIST);
				
			}
		}
		
		// get the max intra cluster distance
		double MAXDIST = Double.MIN_VALUE;
		for (int i=0; i<clusters.size(); i++) {
			double dist = diameterTwoFarthestPoints(clusters.get(i), distance, VERBOSE, logFlag);
			if ( dist > MAXDIST ) {
				MAXDIST = dist;
			}			
			Util.logPrintln(VERBOSE, logFlag, "DUNN: maxDist: "+dist+", MAXDIST: "+MAXDIST);
		}
		return MINDIST / MAXDIST;
	}

	
	public static double diameterTwoFarthestPoints(ArrayList<Integer> cluster, double[][] distance, int VERBOSE, int logFlag){
		double maxDist = Double.MIN_VALUE;
		for (int x=0; x<cluster.size()-1; x++) {
			for (int y=x+1; y<cluster.size(); y++) {
				double dist = distance[cluster.get(x)][cluster.get(y)];
				if ( dist > maxDist ) {
					maxDist = dist;
				}
				Util.logPrintln(VERBOSE, logFlag, "dist["+cluster.get(x)+","+cluster.get(y)+"]: "+dist+", diameter: "+maxDist);
			}
		}
		return maxDist;
	}
	
	
	public static double diameterAverageDistanceFromMedoid(int medoid, ArrayList<Integer> cluster, double[][] distance, int VERBOSE, int logFlag) {
		double totalDist = 0.0;
		for (int i=0; i<cluster.size(); i++) {
			double dist = distance[cluster.get(i)][medoid];
			totalDist += dist;
		}
		return totalDist/cluster.size();
	}
	
	
	public static double getDaviesBouldin(ArrayList<Integer> medoids, ArrayList<ArrayList<Integer>> clusters, double[][] distance,
			int VERBOSE) {
		int logFlag = Constants.LOG_DAVIESB;
		double totalMaxDist = 0;
		for (int i=0; i<clusters.size()-1; i++) {
			// get max fi compare to the others
			double maxDist = 0;
			for (int j=i+1; j<clusters.size(); j++) {
				double diameterI = diameterAverageDistanceFromMedoid(medoids.get(i), clusters.get(i), distance, VERBOSE, logFlag);
				double diameterJ = diameterAverageDistanceFromMedoid(medoids.get(j), clusters.get(j), distance, VERBOSE, logFlag);
				double interDist = interClusterDistanceTwoClosestPoints(clusters.get(i), clusters.get(j), distance, VERBOSE, logFlag);
				double dist = ( diameterI + diameterJ ) / interDist;
				if ( dist > maxDist ) maxDist = dist;
				Util.logPrintln(VERBOSE, logFlag, "f("+i+","+j+"): "+dist+", sum of f(i,j): "+maxDist);
			}
			totalMaxDist += maxDist;
		}
		return totalMaxDist / clusters.size();
	}

	
}
