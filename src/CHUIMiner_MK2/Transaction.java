package CHUIMiner_MK2;
 
// This class represents a transaction from the quantitative database
public class Transaction {

    int[] items;				// list of items contained within the transaction
    double[] utilities;			// list of utilities associated to items within the transaction
    double transactionUtility; 	// the transaction utility value (TU)

    // main constructor
    public Transaction(int[] items, double[] utilities, double transactionUtility) {
    	this.items = items;
    	this.utilities = utilities;
    	this.transactionUtility = transactionUtility;
    }
    
    // returns the list of items in the transaction
    public int[] getItems() {
        return items;
    }
    
    // returns the list of utilities of items in the transaction
    public double[] getUtilities() {
        return utilities;
    }

    // returns the transaction utility value
	public double getUtility()
	{
		return transactionUtility;
	}

	// return the length of the transaction
	public long length()
	{
		return items.length;
	}
}
