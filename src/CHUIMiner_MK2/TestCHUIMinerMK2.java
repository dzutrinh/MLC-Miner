package CHUIMiner_MK2;

import java.io.IOException;

// TEST DRIVE FOR CHUI-Miner* (Mark 2) algorithm - Baseline for MLC-Miner comparisons
public class TestCHUIMinerMK2 {

	public static void main(String [] args) throws IOException {
		
		String	dataset = "sample";			// name of your dataset
		String	trans = dataset + "_trans.txt";	// automatically identify one with transactions
		String	tax = dataset + "_tax.txt";		// and its taxonomy
		double	minutil = 40;					// user-specified minutil value

		CHUIMinerMK2 algo = new CHUIMinerMK2();
		algo.runAlgorithm(trans, tax, "output_chuiminer_mk2.txt", minutil, Integer.MAX_VALUE);
		algo.printStatistics();
	}
	
}
