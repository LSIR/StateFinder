package ch.epfl.lsir.forecasting;

public class Constants {
	public static int REALVALUES = 1;
	public static int SYMBOLIC = 2;
	public static int LOG_NONE 			= 0b000000;
	public static int LOG_INFO 			= 0b000001; // 1
	public static int LOG_KMEDOIDS 		= 0b000010; // 2
	public static int LOG_SILHOUETTE 	= 0b000100; // 4
	public static int LOG_DUNNINDEX	 	= 0b001000; // 8
	public static int LOG_DAVIESB	 	= 0b010000; // 16
	public static int LOG_PREDICTOR	 	= 0b100000; // 32
}	
