package MLCMiner;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 	 _____ __    _____     _____ _             
	|     |  |  |     |___|     |_|___ ___ ___ 
	| | | |  |__|   --|___| | | | |   | -_|  _|
	|_|_|_|_____|_____|   |_|_|_|_|_|_|___|_|  
                                           
	MLC-Miner: Multi-Level Closed high utility itemset Miner

	This algorithm extract multi-level closed high-utility itemsets from hierarchical datasets
	Author: Trinh D.D. Nguyen (dzutrinh at gmail dot com)
	
	Version 1.4a
	Last update: Feb, 2024

	Variations:
	- MLC-Miner      = CHUI-Miner + Taxonomy + RUCK 
	- MLC-Miner-EUCP = CHUI-Miner + Taxonomy + RUCK + ML-EUCP
	
	Changes:
	- Feb, 2022: v1.0  - Initial development, the algorithm is now known as CHUI-Miner**
	- Apr, 2022: v1.1  - Code cleanup, EUCP added, renamed to MLC-Miner.  
	- Jul, 2022: v1.2  - Better subsumption check, significantly boost the mining time. 
	- Aug, 2022: v1.3  - Code cleanup and some optimizations. 
	- Nov, 2023: v1.4  - Introducing Utility Map (UM), completely replacing Utility List.
					   - Even faster RUCK, hence major speed improvement
					   - EUCP is now extended into ML-EUCP, reducing memory usage
					   - Capable of handling datasets with 5M transactions
	- Feb, 2024: v1.4a - Faster construction of revised transactions 
	
	Dependencies: 
		- Dataset
		- Transaction 
		- Taxonomy 
		- Element 
		- UtilityMap
		- EUList
		
	Baseline algorithms:
		- CHUIMiner* (CHUIMinerMK2)
			. Taxonomy-enabled CHUI-Miner
 **/

// MLC-Miner algorithm
public class AlgoMLCMiner {
	
	public long	timerStart = 0;						// time stamps for benchmarking purpose 
	public long	timerStop = 0; 							
	public int		patternCount = 0; 				// multi-level HUIs found
	public int		candidateCount = 0;				// candidate high-utility itemsets counter
	public int		transCount = 0;					// number of transactions
	public double	minUtil = 0.0;					// minimum utility

	public boolean	useEUCPstrategy = true;			// EUCP pruning flag
	
	Map<Integer, Integer> mapItemToLevel;			// Item -> level hashmap
	Map<Integer, List<Integer>> mapItemToAncestor;	// Real taxonomy hashmap
	
	BufferedWriter		writer = null;				// file writer  
	Taxonomy			taxonomy = null;			// for describing the taxonomy of a dataset
	Dataset				dataset = null;				// quantitative transaction dataset
	ArrayList<int[]>	oldNameToNewNamesPerLevel;	// old name to new name mappings
	ArrayList<int[]>	newNamesToOldNamesPerLevel;	// new name to old name mappings
	List<Map<Integer, Map<Integer, Double>>> EUCSPerLevel;	
	
	ArrayList<EUList>	EULPerLevel;				// EU-List for all taxonomy levels
	double[] 			GWUs;						// storing TWU/GWU for each item
	int 				itemsCountPerLevel[];		// total items at each level

	public boolean		debugging = false;
	
	// represents an item and its utility in a transaction
	class Pair {
		int item = 0;
		double utility = 0.0;
	}

	// constructors
	public AlgoMLCMiner(boolean useEUCP) {
		useEUCPstrategy = useEUCP;
	}
	
	public AlgoMLCMiner() {
		useEUCPstrategy = true;
	}

	// ----------------------
	// MLC-MINER ALGORITHM
	// ----------------------
	// Params:
	//	- inputTransaction	[REQ]: source dataset containing transactions
	//	- inputTaxonomy		[REQ]: taxonomy of the given dataset
	//	- output			[OPT]: file to store the discovered patterns, null = no output.
	//	- minUtility		[REQ]: minimum utility threshold
	//  - maxTrans			[REQ]: total number of transaction to load (for scalability test)
	// Returns: None
	
	public void runAlgorithm(String inputTransaction, String inputTaxonomy, String output, double minUtility, int maxTrans) throws IOException {
		
		// initializations
		minUtil				= minUtility;
		mapItemToLevel		= new HashMap<Integer, Integer>();
		mapItemToAncestor	= new HashMap<Integer, List<Integer>>();
		
		if (useEUCPstrategy) {
			EUCSPerLevel = new ArrayList<Map<Integer, Map<Integer, Double>>>();
		}
				
		if (output != null)			// output to file ?
			writer = new BufferedWriter(new FileWriter(output));
		else
			writer = null;			// only return the pattern count

		timerStart = System.currentTimeMillis();
		
		// First dataset scan to calculate the TWU for each item.
		System.out.println("- First dataset scan...");		
		dataset = new Dataset(inputTransaction, maxTrans);	// should perform similar transaction merging here, too		
		transCount = dataset.getTransactions().size();
		taxonomy = new Taxonomy(inputTaxonomy, dataset);		
		int maxLevel = scanDatabaseFirstTime();	// taxonomy's depth
				
		// promising items per level
		ArrayList<ArrayList<Integer>> itemsToKeepPerLevel = new ArrayList<ArrayList<Integer>>();
		itemsCountPerLevel = new int[maxLevel];
		for (int i = 0; i < maxLevel; i++)
			itemsToKeepPerLevel.add(new ArrayList<Integer>());
		for (int item = 1; item < GWUs.length; item++)
			if (GWUs[item] >= minUtil) {
				itemsToKeepPerLevel.get(mapItemToLevel.get(item) - 1).add(item);
			}
		sort(itemsToKeepPerLevel, GWUs);
		
		oldNameToNewNamesPerLevel = new ArrayList<int[]>();
		newNamesToOldNamesPerLevel = new ArrayList<int[]>();

		for (int i = 0; i < maxLevel; i++) {
			ArrayList<Integer> itemsToKeep = itemsToKeepPerLevel.get(i);
			int itemsPerLevel = itemsToKeep.size();
			itemsCountPerLevel[i] = itemsPerLevel;
			
			if (useEUCPstrategy) {
				Map<Integer, Map<Integer, Double>>EUCS = new HashMap<Integer, Map<Integer, Double>>();
				EUCSPerLevel.add(EUCS);
			}
			
			int[] oldNameToNewNames = new int[dataset.getMaxItem() + 1];
			
			// This structure will store the old name corresponding to each new name
			int[] newNamesToOldNames = new int[dataset.getMaxItem() + 1];
			int currentName = 1;

			for (int j = 0; j < itemsPerLevel; j++) {	// for each item in increasing order of GWU				
				int item = itemsToKeep.get(j);			// get the item old name
				oldNameToNewNames[item] = currentName;	// give it the new name
				newNamesToOldNames[currentName] = item;	// remember its old name
				itemsToKeep.set(j, currentName);		// replace old name by the new name in the list of promising items
				currentName++;							// increment the current name
			}
			oldNameToNewNamesPerLevel.add(oldNameToNewNames);
			newNamesToOldNamesPerLevel.add(newNamesToOldNames);
		}
		
		// for faster accessing the utility lists, they are are stored using map as pair: 
		// <K: item; V: utility map associated to that item>
		Map<Integer, UtilityMap> mapItemToUM = new HashMap<Integer, UtilityMap>();
		for (int i = 0; i < maxLevel; i++) {
			ArrayList<Integer> itemsToKeep = itemsToKeepPerLevel.get(i);
			for (Integer item : itemsToKeep) {
				mapItemToUM.put(item, new UtilityMap(item));
			}
		}
		
		System.out.println("==== DATASET CHARACTERISTICS ====");		
		System.out.println(" Dataset: <" + inputTransaction + " | " + inputTaxonomy + ">");		
		System.out.println(" Utility: " + dataset.sumUtility);
		System.out.println(" |D|    : " + transCount);		
		System.out.println(" |GI|   : " + taxonomy.parentCount());
		System.out.println(" Depth  : " + maxLevel);			
		System.out.println(" T_max  : " + dataset.getMaxTransLength());
		System.out.println(" T_avg  : " + dataset.getAvgTransLength());
		System.out.println("=================================");

		System.out.println("- Second dataset scan...");

		EULPerLevel	= new ArrayList<>();	
		for (int i = 0; i < maxLevel; i++) {
			EULPerLevel.add(new EUList());			
			for (int j = 0; j < itemsCountPerLevel[i]; j++)
				EULPerLevel.get(i).add(new UtilityMap(j + 1));
		}
		
		for (Transaction t : dataset.transactions) {
			t.setLevelTransaction(maxLevel);
			t.removeUnpromisingItems(oldNameToNewNamesPerLevel, mapItemToAncestor, mapItemToLevel);
		}
				
		System.out.println("- Constructing UM/EUCS for " + maxLevel + " level(s)...");
		
		for (int tid = 0; tid < transCount; tid++) {
			Transaction tran = dataset.transactions.get(tid);		
			if (tran.items.length == 0) continue;			
			for (int i = 0; i < maxLevel; i++) {
				if (tran.listTransactionUtility.get(i) == 0) continue;

				double ru = 0;
				ArrayList<Integer> itemInTransactionInLevel = tran.listItemsPerLevel.get(i);
				ArrayList<Double> UtilityInTransactionInLevel = tran.listUtilitiesPerLevel.get(i);
				for (int j = itemInTransactionInLevel.size() - 1; j >= 0; j--) {
					int item = itemInTransactionInLevel.get(j);
					double nU = UtilityInTransactionInLevel.get(j);
					Element element = new Element(tid, nU, ru);					
					UtilityMap ulItem = EULPerLevel.get(i).get(item-1);
					if (ulItem != null) {
						ulItem.addElement(element);
					}
					ru = ru + nU;
				}

				// Build EUCS
				if (useEUCPstrategy) {				
					int count = itemInTransactionInLevel.size();
					double tu = tran.listTransactionUtility.get(i);
					for (int u = 0; u < count - 1; u++) {
						int itemU = itemInTransactionInLevel.get(u);

						Map<Integer, Double> mapFMAPItem = EUCSPerLevel.get(i).get(itemU);
						if (mapFMAPItem == null) {
							mapFMAPItem = new HashMap<Integer, Double>();
							EUCSPerLevel.get(i).put(itemU, mapFMAPItem);
						} // if
						
						for (int v = u + 1; v < count; v++) {
							Integer itemV = itemInTransactionInLevel.get(v);
							Double twuSum = mapFMAPItem.get(itemV);
							if(twuSum == null)
								mapFMAPItem.put(itemV, tu);
							else	
								mapFMAPItem.put(itemV, twuSum + tu);	
						}
					}
				}
			}
		};	

		// reduce memory usage
		mapItemToLevel = null;
		mapItemToAncestor = null;
		dataset = null;
		taxonomy = null;
		
		System.out.println("- MLCHUI mining...");
		
		for(int level = maxLevel-1; level >= 0; level--) {				
			EUList eul = EULPerLevel.get(level);
			genCHUI(true, new int[0], null, new EUList(), eul, level);
		}
		
		timerStop = System.currentTimeMillis();					// record end time
		if (writer != null)  writer.close();					// close the output file if present
		
		System.out.println("- Done.");
	}
	
	// First database scan to compute GWUs and determine the taxonomy's depth
	private int scanDatabaseFirstTime() {
		int maxLevel = 0;	
		GWUs = new double[dataset.getMaxItem() + 1];
		for (int tid = 0; tid < transCount; tid++) {
			
    		Transaction transaction = dataset.getTransactions().get(tid);
			ArrayList<Integer> anscestorExist = new ArrayList<Integer>();
			int[] transItems = transaction.getItems();
			
			double transactionUtility = transaction.getUtility();
					
			// for each item, add the transaction utility to its GWU
			for(int i = 0; i < transItems.length; i++) {
				int item = transItems[i];

				GWUs[item] += transactionUtility;
				ArrayList<Integer> ancestor = new ArrayList<Integer>();								
				ancestor.add(item);				
				
				if (mapItemToAncestor.get(item) == null) {						
					Integer itemCopy = item;		
					while (itemCopy != null) {
						Integer childItem = itemCopy;
						Integer parentItem = taxonomy.mapChildToParent.get(childItem);
						if (parentItem != null) {
							ancestor.add(parentItem);
							if (!anscestorExist.contains(parentItem)) {
								anscestorExist.add(parentItem);
								Double twuParent = GWUs[parentItem];
								twuParent = (twuParent == null) ? transactionUtility : transactionUtility + twuParent;
								GWUs[parentItem] = twuParent; 
							}
						}
						itemCopy = parentItem;
					}					
					
					int k = ancestor.size();
					for(int j = 0; j < ancestor.size(); j++, k--) {
						mapItemToLevel.put(ancestor.get(j), k);
						if (maxLevel < k) maxLevel = k; // save the taxonomy depth
					}
															
					for (int itemKey = 0; itemKey < ancestor.size();itemKey++) {
						List<Integer> itemValue = new ArrayList<>();
						for (int listValue = 0; listValue < ancestor.size(); listValue++)
							itemValue.add(ancestor.get(listValue));
						mapItemToAncestor.put(ancestor.get(itemKey), itemValue);
					}
				} 
				else {
					List<Integer> ancestorsList = mapItemToAncestor.get(item);
					
					for(int k = 0; k < ancestorsList.size(); k++) {
						if(!anscestorExist.contains(ancestorsList.get(k))) {
							anscestorExist.add(ancestorsList.get(k));
							Double twuParent = GWUs[ancestorsList.get(k)];
							twuParent += transaction.getUtility();
							GWUs[ancestorsList.get(k)] = twuParent;
						}
					}
				}
			} 			
		}
		return maxLevel;
	}
	
	private int compareItems(int item1, int item2) {			// compare items by their names
		return item1 - item2;
	}

	private void genCHUI(boolean firstTime, int [] closedSet, UtilityMap closedSetUL, EUList preset, EUList postset, int level) throws IOException {
		
		int isize = postset.size();							// L2: for all i in postset
		for (int i = 0; i < isize; i++) {
			UtilityMap iUL = postset.get(i);
			
			// L4: determine the tidset of the new generator 'closedset ∪ {i}'
			UtilityMap newgen_TIDs;
			if (!firstTime)									// if not first time running
				newgen_TIDs = construct(closedSetUL, iUL);	// intersect the tidset of closedset and the tidset of i
			else
				newgen_TIDs = iUL;							// iUL is its tidset
				
			// if newgen has high utility supersets, it's a promising candidate.
			if(isPromising(newgen_TIDs)) {

				// L5: if newgen is not a duplicate
				if(!isDuplicate(newgen_TIDs, preset)) {

					// L6: ClosedsetNew = closedset ∪ {i}, create the itemset for newgen
					int[] closedSetNew = appendItem(closedSet, iUL.item);	

					UtilityMap closedsetNewTIDs = newgen_TIDs; // CALCULATE TIDSET
					
					// L7 : PostsetNew = emptyset
					EUList newPost = new EUList();
					
					// for each item J in the postset
					boolean isHUI = true;
					int jsize = postset.size();
					for (int j = 0; j < jsize; j++) {
					
						UtilityMap jUL = postset.get(j);
						
						// if J is smaller than I according to the total order on items, we skip it
						if(jUL.item == iUL.item || compareItems(jUL.item, iUL.item) < 0) continue;

						// EUCP
						if (useEUCPstrategy && isPrunableByEUCS(iUL.item, jUL.item, level)) continue;
						
						candidateCount++;
						
						if(hasAllTIDS(jUL, newgen_TIDs)) {
							closedSetNew = appendItem(closedSetNew, jUL.item);	
							closedsetNewTIDs = construct(closedsetNewTIDs, jUL);
							
							if(!isPromising(closedsetNewTIDs)) {
								isHUI = false;
								break;
							}
						}
						else newPost.add(jUL);
					}
					
					if(isHUI) {
						// L15: write out Closed_setNew and its support
						if (minUtil <= closedsetNewTIDs.sumIutils)
							output(closedSetNew, closedsetNewTIDs.sumIutils, closedsetNewTIDs.getSupport(), level);
						
						// L16: recursive call, must make a copy of preset before the recursive call
						EUList newPre = new EUList(preset);
						genCHUI(false, closedSetNew, closedsetNewTIDs, newPre, newPost, level);
					}
					preset.add(iUL);	// L17: preset = preset ∪ {i}
				}
			}	
		}
	}
	
	// Find the element with a given tid in a utility map. Complexity: O(1)
	private Element elementByTID(UtilityMap ulist, int tid) {
		Element e = ulist.mapElements.get(tid);
		if (e != null)
			return e;
		return null; // not found
	}

	// join two utility-map. Complexity: O(|X|)
	private UtilityMap construct(UtilityMap x, UtilityMap y) {
		UtilityMap xy = new UtilityMap(y.item);			// create an empty utility list for pXY
		double tu = x.sumIutils + x.sumRutils;
		int isize = x.getSupport();
		List<Element> el = new ArrayList<>(x.mapElements.values());
		
		for (int i = 0; i < isize; i++) {			// O(|X|)
			Element ex = el.get(i);
			Element ey = elementByTID(y, ex.tid);	// find element ey in py with tid = ex.tid - O(1)
			if(ey == null){
				tu -= (ex.iutils + ex.rutils);		// LA-Prune
				if(tu < minUtil) return null;						
				continue;
			}
			Element e = new Element(ex.tid, ex.iutils + ey.iutils, ex.rutils - ey.iutils);
			xy.addElement(e);
		}
		return xy;
	}
		
	// Y = X ∪ {i}
	private int[] appendItem(int[] itemset, int item) {
		int [] newgen = new int[itemset.length+1];
		System.arraycopy(itemset, 0, newgen, 0, itemset.length);
		newgen[itemset.length] = item;
		return newgen;
	}

	// check for if newtid is subsumed by parent: TidSet(newtid) ⊆ TidSet(parent)
	private boolean isSubsumed(UtilityMap newtid, UtilityMap parent) {
		for (Integer i : newtid.mapElements.keySet()) {		// if newtid.keySet() ⊆ parent.keySet() ?
			if (!parent.hasTID(i)) {		// O(1)
				return false;
			}
		}
		return true;
	}
	
	private boolean isDuplicate(UtilityMap tidset, EUList prevset) {
		int tidsupp = tidset.getSupport();
		// Optimization 1a
		if (prevset.getMaxSupport() < tidsupp) return false;
		int size = prevset.size();
		for (int i = 0; i < size; i++) {		// L25: for each utility list in prevset
			UtilityMap ul = prevset.get(i);
			
			// Optimization 1b: best on long and dense datasets
			if (ul.getSupport() < tidsupp) continue;
	
			// Optimization 2: if tidset of newgen is included in tids of parent, subsumed
			if (isSubsumed(tidset, ul)) return true;
		}
		return false; // not subsumed, complexity: O(|prevset|x|tidset|)		
	}

	// test if an item is promising using the utility map
	private boolean isPromising(UtilityMap ul) {
		return	(ul != null) &&  (minUtil <= ul.sumIutils + ul.sumRutils);
	}

	// test if y ⊆ x
	private boolean hasAllTIDS(UtilityMap x, UtilityMap y) {
		int ysize = y.getSupport();
		if (x.getSupport() < ysize) return false;
		Set<Integer> tids = y.mapElements.keySet();
		for (Integer i : tids) 
			if (!x.hasTID(i)) return false;			
		return true; // Complexity: O(|y|)		
	}	
	
	// EUCP
	private boolean isPrunableByEUCS(int x, int y, int level) {
		if (!useEUCPstrategy)
			return false;
		
		Map<Integer, Double> mapTWUF = EUCSPerLevel.get(level).get(x);
		if(mapTWUF != null) {
			Double twuF = mapTWUF.get(y);
			if(twuF == null || twuF < minUtil)
				return true;
		}

		return false;
	}
	
	private void output(int[] itemset, double sumIutils, int support, int level) throws IOException {
		patternCount++; // increase the number of MLCHUIs found
		outputToFile(itemset, sumIutils, support, level);
	}
	
	private void outputToFile(int[] itemset, double sumIutils, int support, int level) throws IOException {

		if (writer == null) return;
		
		StringBuilder buffer = new StringBuilder();

		// append the prefix
		for (int i = 0; i < itemset.length; i++) {
			buffer.append(newNamesToOldNamesPerLevel.get(level)[itemset[i]]);
			buffer.append(' ');
		}
		buffer.append(" #SUP: ");
		buffer.append(support);			
		buffer.append(" #UTIL: ");
		buffer.append(sumIutils);	
		
		// write to file
		writer.write(buffer.toString());
		writer.newLine();
	}

    private double peakHeapUsage() {
    	double retVal = 0;
    	try {
            List<MemoryPoolMXBean> pools = ManagementFactory.getMemoryPoolMXBeans();
			double total = 0;
			for (MemoryPoolMXBean memoryPoolMXBean : pools) {
				if (memoryPoolMXBean.getType() == MemoryType.HEAP) {
					long peakUsed = memoryPoolMXBean.getPeakUsage().getUsed();
					total = total + peakUsed;
				}
			}
			retVal = total/1024/1024;
       } catch (Throwable t) {
            System.err.println("Exception: " + t);
       }
    	return retVal;
    }		
    
	public void printStatistics() {
		
		long algoRuntime = timerStop - timerStart;
		double algoMemUsage = peakHeapUsage();

		System.out.println("=============  MLC-MINER_LDB STATISTICS =============");
		System.out.println(" EUCP enabled      : " + (useEUCPstrategy ? "YES": "NO"));
		System.out.println(" Given minutil     : " + minUtil);
		System.out.println(" Approx. runtime   : " + algoRuntime + " ms ("+ algoRuntime/1000.0 +" s)");
		System.out.println(" Peak memory used  : " + algoMemUsage  + " MB");
		System.out.println(" Patterns found    : " + patternCount); 
		System.out.println(" Candidates count  : " + candidateCount);
		System.out.println("=====================================================");
	}
	
	public static void sort(ArrayList<ArrayList<Integer>> itemList, double[] ArrayTWU) {
		for (List<Integer> items : itemList) {
			for (int j = 1; j < items.size(); j++) {
				Integer itemJ = items.get(j);
				int i = j - 1;
				Integer itemI = items.get(i);

				// we compare the twu of items i and j
				double comparison = ArrayTWU[itemI] - ArrayTWU[itemJ];
				// if the twu is equal, we use the lexicographical order to decide whether i is
				// greater than j or not.
				if (comparison == 0) {
					comparison = itemI - itemJ;
				}

				while (comparison > 0) {
					items.set(i + 1, itemI);

					i--;
					if (i < 0) {
						break;
					}

					itemI = items.get(i);
					comparison = ArrayTWU[itemI] - ArrayTWU[itemJ];
					// if the twu is equal, we use the lexicographical order to decide whether i is
					// greater than j or not.
					if (comparison == 0) {
						comparison = itemI - itemJ;
					}
				}
				items.set(i + 1, itemJ);
			}
		}
	}	
}
