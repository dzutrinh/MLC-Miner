package MLCMiner;

import java.io.IOException;

public class TestMLCMiner {

	public static void main(String [] args) throws IOException {
		
		String	dataset = "sample";				// name of your dataset
		String	trans = dataset + "_trans.txt";	// automatically identify one with transactions
		String	tax = dataset + "_tax.txt";		// and its taxonomy
		double	minutil = 40;					// user-specified minutil value

		boolean	eucp = false;					// EUCP enabling flag

		AlgoMLCMiner algo = new AlgoMLCMiner(eucp);
		algo.runAlgorithm(trans, tax, "output_mlc.txt", minutil, Integer.MAX_VALUE);
		algo.printStatistics();
	}
	
}
