package ch.epfl.lsir.spclust;


/**
 * 
 * @author Tri Kurniawan Wijaya <tri-kurniawan.wijaya@epfl.ch>
 *  @date Sun 16 Jun 2013 11:42:13 AM CEST 
 *
 */
public class ComputeSpclust {

	/**
	 *
	 * @param args
	 */
	public static void main(String[] args) {
		// FOR FASTER PROCESSING, WE CHOOSE TO WORK WITH INTEGER
		// SO IF YOU HAVE DOUBLE, SUCH AS GPS, YOU NEED TO MULTIPLY THEM

		// INPUT IS A SERIES OF VALUES
		// 1. from file (raw data training)--> (gridLabel and centroid) in file
		Spclust sp = new Spclust(0b01);
		int numDimension = 2;
		int gridSize = 25;
		int threshold = 2;
		String fileTraining = "test1-d2-gs25-t2.txt"; 
		String fileTesting = "test1-d2-gs25-t2.txt";
		String fileModel = "sp-d2.txt"; 
		
		// 1. from file (raw data training)--> (gridLabel and centroid) in file, i.e., fileModel
		// parameter: fileTraining, numDimension, gridSize, threshold fileModel
		sp.cluster(fileTraining, numDimension, gridSize, threshold);
		Util.saveToFile(sp, fileModel);
		
		
		// 2. from file (gridLabel and centroid) + (raw data all/testing) --> symbol in file, i.e., fileTesting.out
		// parameter: fileModel fileTesting fileSymbols
		Spclust Wave2 = Util.loadSpclustFromFile(fileModel);
		System.err.println("\nWave 2: "+Wave2);
		Wave2.applyCluster(fileTesting, fileTesting +".out" );
		
		// 3. from file (symbol) + (gridLabel and centroid) --> raw/real-values in file, i.e.,
		// parameter: fileModel fileSymbols fileCentroids
		Spclust Wave3 = Util.loadSpclustFromFile(fileModel);
		System.err.println("\nWave 3: "+Wave3);
		Wave3.getCentroids(fileTesting+".out", fileTesting +".ctd" );
		
		
		
		/* test for h1 channel microwave
		int numDimension = 1;
		int gridSize = 10;
		int threshold = 10;
		sp.cluster("h1_c11.txt", numDimension, gridSize, threshold);
		*/
		System.err.println("done.");
		
	}



}

