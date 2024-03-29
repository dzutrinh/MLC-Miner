package MLCMiner;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

//This class represents a quantitative database
public class Dataset {
	
	List<Transaction> transactions;	
	private int maxItem = 0;
	private long maxTransLength = 0;
	private long sumTransLength = 0;
	public long sumUtility = 0;

	// main constructor
    public Dataset(String datasetPath, int maximumTransactionCount) throws IOException {

        transactions = new ArrayList<Transaction>();
        BufferedReader br = new BufferedReader(new FileReader(datasetPath));
        String line;
        int i = 0;

        while((line = br.readLine()) != null) { 
			if (line.isEmpty() == true ||
				line.charAt(0) == '#' ||  
				line.charAt(0) == '@') continue;
			i++;
			transactions.add(createTransaction(line));
	    	if(i == maximumTransactionCount) break;
        }
        br.close();
    }

    // create a transaction object from a string read from the input file
    private Transaction createTransaction(String line) {
    	
    	String[] split = line.split(":");								// split the line into tokens using ":"    	
    	double transactionUtility = Double.parseDouble(split[1]);		// Get the transaction utility
        String[] itemsString = split[0].split(" ");						// Get the list of items
        String[] itemsUtilitiesString = split[2].split(" ");			// Get the list of item utilities
        int[] items = new  int[itemsString.length];						// store the items and their utilities
        double[] utilities = new  double[itemsString.length];
        
        for (int i = 0; i < items.length; i++) {						// for each item        	
        	items[i] = Integer.parseInt(itemsString[i]);				// store that item        	
        	utilities[i] = Double.parseDouble(itemsUtilitiesString[i]);	// and its utility in that transaction
            if(items[i] > maxItem) maxItem = items[i];					// determine the largest item name
        }

        if (maxTransLength < items.length)	maxTransLength = items.length;
        sumTransLength += items.length;
        sumUtility += transactionUtility;
        
        return new Transaction(items, utilities, transactionUtility);	// create the transaction 
    }

    // returns the list of all transactions
    public List<Transaction> getTransactions() {
        return transactions;
    }

    // returns the largest item within the database
    public int getMaxItem() {
        return maxItem;
    }

    public void setMaxItem(int newMax) {
    	this.maxItem = newMax;
    }
    
    // returns the maximum transaction length
    public long getMaxTransLength() {
    	return maxTransLength;
    }

    // returns the average transaction length
    public double getAvgTransLength() {
    	return (double) sumTransLength / transactions.size(); 
    }
}
