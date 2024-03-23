package MLCMiner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// This class represents a transaction from the quantitative database
public class Transaction {

    int[] items;				// list of items contained within the transaction
    double[] utilities;			// list of utilities associated to items within the transaction
    double transactionUtility; 	// the transaction utility value (TU)
    
    // adjust to fix with total items per datasets if needed
    public final static int bufLen = 100; 
	public static int[] bufItems = new int[bufLen];
	public static double[] bufUtils = new double[bufLen];
	
	public ArrayList<ArrayList<Integer>> listItemsPerLevel = new ArrayList<ArrayList<Integer>>();
	public ArrayList<ArrayList<Double>> listUtilitiesPerLevel = new ArrayList<ArrayList<Double>>();
	public ArrayList<Double> listTransactionUtility = new ArrayList<Double>();
    
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
	public double getUtility() {
		return transactionUtility;
	}

    // returns the transaction utility value
	public void setUtility(double tu) {
		this.transactionUtility = tu;
	}
	
	// return the length of the transaction
	public long length() {
		return items.length;
	}
	
	public void setLevelTransaction(int maxLevel) {
		for (int i = 0; i < maxLevel; i++) {
			listItemsPerLevel.add(new ArrayList<Integer>());
			listUtilitiesPerLevel.add(new ArrayList<Double>());
			listTransactionUtility.add(0d);	
		}
	}
	
	public void AddItemToTransaction(int level,int item,double utility) {
		listItemsPerLevel.get(level-1).add(item);
		listUtilitiesPerLevel.get(level-1).add(utility);
		listTransactionUtility.set(level-1, listTransactionUtility.get(level-1)+utility);
	}
	
	public void removeUnpromisingItems(ArrayList<int[]> oldNamesToNewNames, Map<Integer, List<Integer>> mapItemToAncestor,Map<Integer, Integer> mapItemToLevel) {
    	Map<Integer,Double> mapItemToUtility = new HashMap<Integer, Double>();
    	for(int j = 0; j < items.length;j++) {
    		int item = items[j];    		
    		// Convert from old name to new name
    		mapItemToUtility.put(item, utilities[j]);
    		List<Integer> listParent = mapItemToAncestor.get(item);
    		for (int k = 1; k < listParent.size(); k++) {
				int parentItem = listParent.get(k);
				Double UtilityOfParent = mapItemToUtility.get(parentItem);
				if (UtilityOfParent == null)
					UtilityOfParent = utilities[j];
				else
					UtilityOfParent += utilities[j];
				mapItemToUtility.put(parentItem, UtilityOfParent);
			}
    	}
    	for (int j : mapItemToUtility.keySet()) {
			int level = mapItemToLevel.get(j);
			if (oldNamesToNewNames.get(level-1)[j]!=0)  {
				this.listItemsPerLevel.get(level-1).add(oldNamesToNewNames.get(level-1)[j]);
				this.listUtilitiesPerLevel.get(level-1).add(mapItemToUtility.get(j));
				this.listTransactionUtility.set(level-1,this.listTransactionUtility.get(level-1)+mapItemToUtility.get(j));
			}
		}
    	sort();	// sort by increasing GWU values
	}

	// A utility function to swap two elements
    private void swap(ArrayList<Integer> itemsList, ArrayList<Double> utilitiesList, int i, int j) {
        int tempi = itemsList.get(i);
        itemsList.set(i, itemsList.get(j));
        itemsList.set(j, tempi);

        double tempf = utilitiesList.get(i);
        utilitiesList.set(i, utilitiesList.get(j));
        utilitiesList.set(j, tempf);
    }
 
    private int partition(ArrayList<Integer> itemsList, ArrayList<Double> utilitiesList, int low, int high)  {
        int pivot = itemsList.get(high);
        int i = (low - 1);
        for (int j = low; j < high; j++) {
             if (itemsList.get(j) <= pivot) {
                i++;
                swap(itemsList, utilitiesList, i, j);
            }
        }
        swap(itemsList, utilitiesList, i + 1, high);
        return (i + 1);
    }
 
    private void quickSort(ArrayList<Integer> itemsList, ArrayList<Double> utilitiesList, int low, int high) {
        if (low < high) { 
            int pi = partition(itemsList, utilitiesList, low, high);
            quickSort(itemsList, utilitiesList, low, pi - 1);
            quickSort(itemsList, utilitiesList, pi + 1, high);
        }
    }
    
	public void sort(){
		for (int level = 0; level < listItemsPerLevel.size(); level++) {
			ArrayList<Integer> itemsList = listItemsPerLevel.get(level);
			ArrayList<Double> utilitiesList = listUtilitiesPerLevel.get(level);	
			quickSort(itemsList, utilitiesList, 0, itemsList.size()-1);
		}
	}	
}
