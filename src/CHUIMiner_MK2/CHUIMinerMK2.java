package CHUIMiner_MK2;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
	CHUI-Miner**: Simply extends the CHUI-Miner algorithm to work on taxonomy datasets 

	This algorithm extends the CHUI-Miner algorithm to mine multi-level closed high-utility itemsets
	Author: Trinh D.D. Nguyen (dzutrinh at gmail dot com)
	https://github.com/dzutrinh
	
	Version 1.0
	Last update: Dec, 2023

 **/

	public class CHUIMinerMK2 {
		
		public long	timerStart = 0;					// time stamps for benchmarking purpose 
		public long	timerStop = 0; 							
		public int		patternCount = 0; 				// multi-level HUIs found
		public int		candidateCount = 0;				// candidate high-utility itemsets counter
		public double	minUtil = 0.0;					// minimum utility
		
		Map<Integer, Integer> mapItemToLevel;			// Item -> level hashmap
		Map<Integer, Double> mapItemToGWU;				// Map to remember the GWU/TWU of each item	
		Map<Integer, List<Integer>> mapItemToAncestor;	// Real taxonomy hashmap

		List<List<Itemset>> listItemsetsBySize = null;	// The set of multi-level closed high utility itemsets (MLCHUIs) ordered by their support
		BufferedWriter writer = null;					// file writer  
		Taxonomy taxonomy;								// for describing the taxonomy of a dataset
		
		class Pair {									// represents an item and its utility in a transaction
			int item = 0;
			double utility = 0.0;
		}

		public CHUIMinerMK2() {
		}

		// ----------------------
		// CHUI-Miner** ALGORITHM
		// ----------------------
		// Params:
		//	- inputTransaction	[REQ]: source dataset containing transactions
		//	- inputTaxonomy		[REQ]: taxonomy of the given dataset
		//	- output			[OPT]: file to store the discovered patterns, null = no output.
		//	- minUtility		[REQ]: minimum utility threshold
		// Returns: None
		public void runAlgorithm(String inputTransaction, String inputTaxonomy, String output, Double minUtility, int maxTrans) throws IOException {
			
			// initializations
			mapItemToGWU		= new HashMap<Integer, Double>();		
			mapItemToLevel		= new HashMap<Integer, Integer>();
			mapItemToAncestor	= new HashMap<Integer, List<Integer>>();		
			taxonomy			= new Taxonomy(inputTaxonomy);
			minUtil				= minUtility;

			if (output != null)			// output to file ?
				writer = new BufferedWriter(new FileWriter(output));
			else {
				writer = null;			// only return the pattern count
				listItemsetsBySize = new ArrayList<List<Itemset>>();
			}
		
			timerStart = System.currentTimeMillis();
			
			// first dataset scan to calculate the TWU of each item.
			System.out.println("- First dataset scan...");
			Dataset	dataset = new Dataset(inputTransaction, maxTrans);	// should perform similar transaction merging here, too
			
			for (int tid = 0; tid < dataset.getTransactions().size(); tid++) {
	    		Transaction transaction = dataset.getTransactions().get(tid);
				ArrayList<Integer> ancestantExist = new ArrayList<Integer>();
				
				for(int i = 0; i < transaction.getItems().length; i++) {		// for each item, add the transaction utility to its TWU
					Integer item = transaction.getItems()[i];
					double transactionUtility = transaction.getUtility();
					Double twu = mapItemToGWU.get(item);						// get the current TWU of that item

					// add the utility of the item in the current transaction to its twu
					twu = (twu == null) ?  transactionUtility : twu + transactionUtility;
					ArrayList<Integer> ancestor = new ArrayList<Integer>();
					
					ancestor.add(item);				
					mapItemToGWU.put(item, twu);								
					if (mapItemToAncestor.get(item) == null) {						
						Integer itemCopy = item;							
						for (int m = 0; m < taxonomy.size(); m++) {
							Integer childItem = taxonomy.child(m);								
							Integer parentItem = taxonomy.parent(m);
							if (childItem.intValue() == itemCopy.intValue()) {
								ancestor.add(parentItem);
								if (!ancestantExist.contains(parentItem)) {
									ancestantExist.add(parentItem);
									Double twuParent = mapItemToGWU.get(parentItem);
									twuParent = (twuParent == null) ? transactionUtility: transactionUtility + twuParent;
									mapItemToGWU.put(parentItem, twuParent);
								} // if
								itemCopy = parentItem;
							} // if
						} // for m
							
						int k = ancestor.size();
						for(int j = 0; j < ancestor.size(); j++, k--) {
							mapItemToLevel.put(ancestor.get(j), k);
						}
							
						for (int itemKey = 0; itemKey < ancestor.size();itemKey++) {
							List<Integer> itemValue = new ArrayList<>();
							for (int listValue = 0; listValue < ancestor.size(); listValue++) {
								itemValue.add(ancestor.get(listValue));
							}								
							mapItemToAncestor.put(ancestor.get(itemKey), itemValue);
						}
					} // if 
					else {
						List<Integer> listAncestorOfItem = mapItemToAncestor.get(item);
						for(int k=0;k<listAncestorOfItem.size();k++) {
							if(!ancestantExist.contains(listAncestorOfItem.get(k))) {
								ancestantExist.add(listAncestorOfItem.get(k));
								Double twuParent = mapItemToGWU.get(listAncestorOfItem.get(k));
								twuParent = (twuParent == null) ? transaction.getUtility() : twuParent + transaction.getUtility();
								mapItemToGWU.put(listAncestorOfItem.get(k), twuParent);
							} // if
						} // for
					} // else
				} // for i 			
			} // for tid
			
			List<List<UtilityList>> ulLists = new ArrayList<>();			// for storing ULs of items having TWU >= minutil.
			// for faster accessing the utility lists, they are are stored using map as pair: 
			// <KEY: item, VALUE: utility list associated to that item>
			Map<Integer, UtilityList> mapItemToUtilityList = new HashMap<Integer, UtilityList>();

			for (Integer item: mapItemToGWU.keySet()) {						// for each item
				if (mapItemToGWU.get(item) >= this.minUtil) {				// if the item is promising (TWU >= minutil)
					UtilityList uList = new UtilityList(item);				// create an empty utility list that will be filled later.
					mapItemToUtilityList.put(item, uList);					// add the item to the list of high TWU items
				} // if
				else {
					List<Integer> listAncestorOfItem = mapItemToAncestor.get(item);
					for (int k=0; k < listAncestorOfItem.size(); k++) {
						if (mapItemToGWU.get(listAncestorOfItem.get(k)) >= this.minUtil) {
							List<Integer> itemList = new ArrayList<Integer>();
							itemList.add(item);
							UtilityList tuList = new UtilityList(item);
							mapItemToUtilityList.put(item, tuList);
							break;
						} // if
					} // for k
				} // else
			} // for item

			// DEBUGGING
			/*
			for (Integer item: mapItemToGWU.keySet()) {						// for each item
				System.out.println(item + ": TWU=" + mapItemToGWU.get(item));
			}
			*/
			
			int maxLevel = getMaxLevel(mapItemToLevel);		// the taxonomy is stable now, just use this value.
			
			List<List<List<Pair>>> revisedTransaction = new ArrayList<>();
			List<List<List<Integer>>> checkItemExist = new ArrayList<>();
			for (int i = 0; i < maxLevel; i++) {
				
				List<List<Pair>> revisedTransactionTemp = new ArrayList<>();
				List<List<Integer>> checkItemExistTemp = new ArrayList<>();
			
				for (int j = 0; j < dataset.getTransactions().size(); j++) {
					List<Pair> rrTemp = new ArrayList<Pair>();
					List<Integer> ctTemp = new ArrayList<Integer>();
					revisedTransactionTemp.add(rrTemp);
					checkItemExistTemp.add(ctTemp);
				} // for j
					
				revisedTransaction.add(revisedTransactionTemp);
				checkItemExist.add(checkItemExistTemp);			
			} // for i

			System.out.println("==== DATASET CHARACTERISTICS ====");		
			System.out.println(" Dataset: <" + inputTransaction + " / " + inputTaxonomy + ">");			
			System.out.println(" |D|    : " + dataset.getTransactions().size());			
			System.out.println(" |GI|   : " + taxonomy.parentCount());
			System.out.println(" Depth  : " + maxLevel);			
			System.out.println(" T_max  : " + dataset.getMaxTransLength());
			System.out.println(" T_avg  : " + dataset.getAvgTransLength());
			System.out.println("=================================");

			System.out.println("- Second dataset scan...");
			for (int tid = 0; tid < dataset.getTransactions().size(); tid++) {
	    		Transaction transaction  = dataset.getTransactions().get(tid);
	    		int[] items = transaction.getItems();
	    		double[] utilityValues = transaction.getUtilities();    		
	    		double [] remainingUtility = new double[maxLevel];				
				double [] newTWU = new double[maxLevel];
				
				for (int i = 0; i < items.length; i++) {
					Integer item = transaction.getItems()[i];
					Pair pair = new Pair();
					pair.item = item;
					pair.utility = utilityValues[i];
					
					if (mapItemToGWU.get(pair.item) >= this.minUtil) {
						int itemLevel = mapItemToLevel.get(pair.item);
						revisedTransaction.get(itemLevel-1).get(tid).add(pair);
						checkItemExist.get(itemLevel-1).get(tid).add(pair.item);
						remainingUtility[itemLevel-1] += pair.utility;
						newTWU[itemLevel-1] += pair.utility;
						
						Pair itemCopyPair = new Pair();
						itemCopyPair.item = pair.item;
						itemCopyPair.utility = pair.utility;
						
						List<Integer> ancestorOfItem = mapItemToAncestor.get(itemCopyPair.item);		
						for (int k = 1; k < ancestorOfItem.size(); k++) {
							Pair parentItemPair = new Pair();
							parentItemPair.item = ancestorOfItem.get(k);
							int parentItemLevel = mapItemToLevel.get(parentItemPair.item);
							if (checkItemExist.get(parentItemLevel-1).get(tid).contains(parentItemPair.item)) {										
								int index = checkItemExist.get(parentItemLevel-1).get(tid).indexOf(parentItemPair.item);
								double utilityOfOldParent = revisedTransaction.get(parentItemLevel-1).get(tid).get(index).utility;
										
								Pair pairTemp = new Pair();
								pairTemp.item = parentItemPair.item;
								pairTemp.utility = utilityOfOldParent + pair.utility;
								revisedTransaction.get(parentItemLevel-1).get(tid).set(index, pairTemp);
								itemCopyPair = pairTemp;
										
								remainingUtility[parentItemLevel-1] += pair.utility;
								newTWU[parentItemLevel-1] += pair.utility;
							} // if
							else {
								checkItemExist.get(parentItemLevel-1).get(tid).add(parentItemPair.item);
								parentItemPair.utility = itemCopyPair.utility;
								revisedTransaction.get(parentItemLevel-1).get(tid).add(parentItemPair);
								itemCopyPair = parentItemPair;
										
								remainingUtility[parentItemLevel-1] += parentItemPair.utility;
								newTWU[parentItemLevel-1] += parentItemPair.utility;
							} //else
						} // for k
					} // if 
					else {
						boolean useTWU = false;
						
						Pair itemCopyPair = new Pair();
						itemCopyPair.item = pair.item;
						itemCopyPair.utility = pair.utility;
							
						for (int m = 0; m < taxonomy.size(); m++) {	
							Pair parentItemPair = new Pair();
							Integer childItem = taxonomy.child(m);
							parentItemPair.item = taxonomy.parent(m);
								
							if (childItem.equals(itemCopyPair.item)) {
								if (mapItemToGWU.get(parentItemPair.item) >= this.minUtil) {

									useTWU = true;
									int parentItemLevel = mapItemToLevel.get(parentItemPair.item);
									if (checkItemExist.get(parentItemLevel-1).get(tid).contains(parentItemPair.item)) {
										int index = checkItemExist.get(parentItemLevel-1).get(tid).indexOf(parentItemPair.item);
										double utilityOfOldParent = revisedTransaction.get(parentItemLevel-1).get(tid).get(index).utility;
											
										Pair pairTemp = new Pair();
										pairTemp.item = parentItemPair.item;
										pairTemp.utility = utilityOfOldParent + pair.utility;
										revisedTransaction.get(parentItemLevel-1).get(tid).set(index, pairTemp);
										itemCopyPair = pairTemp;
											
										remainingUtility[parentItemLevel-1] += pair.utility;
										newTWU[parentItemLevel-1] += pair.utility;
									} // if
									else {
										checkItemExist.get(parentItemLevel-1).get(tid).add(parentItemPair.item);
										parentItemPair.utility = pair.utility;
										revisedTransaction.get(parentItemLevel-1).get(tid).add(parentItemPair);
										itemCopyPair = parentItemPair;
											
										remainingUtility[parentItemLevel-1] += parentItemPair.utility;
										newTWU[parentItemLevel-1] += parentItemPair.utility;
									} // else
								} // if 
								else	itemCopyPair = parentItemPair;
							} // if
						} // for
						
						if (useTWU) {
							int itemLevel = mapItemToLevel.get(pair.item);						
							revisedTransaction.get(itemLevel-1).get(tid).add(pair);
							checkItemExist.get(itemLevel-1).get(tid).add(pair.item);
							remainingUtility[itemLevel-1] += pair.utility;
							newTWU[itemLevel-1] += pair.utility;
						} // if
					} // else
				} // for i
			
				for (int i = 0; i < maxLevel; i++) {				// sort the transactions
					Collections.sort(revisedTransaction.get(i).get(tid), new Comparator<Pair>() {
						public int compare(Pair o1, Pair o2) { return compareItems(o1.item, o2.item); 
					}});
				} // for i
				
				for(int levels = maxLevel-1; levels >= 0; levels--) {
					for(int i = 0; i < revisedTransaction.get(levels).get(tid).size(); i++) {
						Pair pair = revisedTransaction.get(levels).get(tid).get(i);
						
						remainingUtility[levels] = remainingUtility[levels] - pair.utility;		// subtract the utility of this item from the remaining utility
						UtilityList utilityListOfItem = mapItemToUtilityList.get(pair.item);	// get the utility list of this item
						
						// add new element to the utility list of this item corresponding to this transaction
						Element element = new Element(tid, pair.utility, remainingUtility[levels]);					
						if (utilityListOfItem != null) utilityListOfItem.addElement(element);
					} // for i
				} // for level		
			} // for tid
			
			System.out.println("- Constructing utility lists...");
			for(int i = 0; i < maxLevel; i++) {
				List<UtilityList> UtilityListOfILevel = new ArrayList<>();
				for (Integer item: mapItemToGWU.keySet()){				
					if (mapItemToGWU.get(item) >= this.minUtil){	// if the item is promising  (TWU >= minUtil)					
						if (mapItemToLevel.get(item) == i+1) {						
							UtilityList uList = mapItemToUtilityList.get(item);	// create an empty Utility List that we will fill later.
							UtilityListOfILevel.add(uList);	// add the item to the list of high TWU items
						} // if
					} // if
				} // for item
				
				ulLists.add(UtilityListOfILevel);
				
				// sort the list based on item's TWU in ascending order
				Collections.sort(ulLists.get(i), new Comparator<UtilityList>(){
					public int compare(UtilityList o1, UtilityList o2) {
						return compareItems(o1.item, o2.item);		// compare the TWU of the items
					}});
			} // for i
		
			System.out.println("- MLCHUI mining...");
			
			for(int i = 0; i < maxLevel;i++) {						// Mine the database recursively
				chuiMiner(true, new int[0], null, new ArrayList<UtilityList>(), ulLists.get(i));
			} // for i
		
			if (writer != null)  writer.close();					// close the output file if present

			timerStop = System.currentTimeMillis();					// record end time
			
			System.out.println("- Done.");
		}
		
		private int compareItems(int item1, int item2) {			// compare items by their TWU
			int compare = (int) (mapItemToGWU.get(item1) - mapItemToGWU.get(item2));	
			return (compare == 0) ? item1 - item2 : compare;		// if the same, use the lexical order otherwise use the TWU
		}
			
		// ==================================================================
		// WARNING: EXTREMELY EXPENSIVE OPERATION, USE IT WISELY IN YOUR CODE
		// ==================================================================
	    private static Integer getMaxLevel(Map<Integer, Integer> map) {		// returns the maximum level of the taxonomy.
	        if (map == null) return null;
	        int length = map.size();
	        Collection<Integer> c = map.values();
	        Object[] obj = c.toArray();
	        Arrays.sort(obj);												// computational expensive
	        return Integer.parseInt(obj[length-1].toString());
	    }
		
		// Do a binary search to find the element with a given tid in a utility list
		private Element findElementWithTID(UtilityList ulist, int tid) {
			List<Element> list = ulist.elements;
			
			// perform a binary search to check if  the subset appears in  level k-1.
	        int first = 0;
	        int last = list.size() - 1;
	       
	        // the binary search
	        while( first <= last )
	        {
	        	int middle = ( first + last ) >>> 1; // divide by 2

	            if(list.get(middle).tid < tid){
	            	first = middle + 1;  //  the itemset compared is larger than the subset according to the lexical order
	            }
	            else if(list.get(middle).tid > tid){
	            	last = middle - 1; //  the itemset compared is smaller than the subset  is smaller according to the lexical order
	            }
	            else{
	            	return list.get(middle);
	            }
	        }
			return null;
		}

		// ==================================================================================
		// =================================== CHUI-MINER ===================================
		// ==================================================================================

		private void chuiMiner(boolean firstTime, int [] closedSet, UtilityList closedSetUL, 
				List<UtilityList> preset, List<UtilityList> postset) throws IOException {
			
			//L2: for all i in postset
			for (UtilityList iUL : postset) {
				// L4 Calculate the tidset of the new GENERATOR "closedset U {i}"
				UtilityList newgen_TIDs;
				// if the first time
				if(firstTime) newgen_TIDs = iUL;	// it is the tidset of it
				else {
					// otherwise we intersect the tidset of closedset and the tidset of i
					newgen_TIDs = construct(closedSetUL, iUL);
				}

				// if newgen has high utility supersets
				if(isPassingHUIPruning(newgen_TIDs)){
					// L3: newgen = closedset U {i}
					// Create the itemset for newgen
					int[] newGen = appendItem(closedSet, iUL.item);	
					
					// L5:  if newgen is not a duplicate
					if(isDuplicate(newgen_TIDs, preset) == false){
						// L6: ClosedsetNew = newGen
						int[] closedSetNew = newGen;	

						// calculate tidset
						UtilityList closedsetNewTIDs = newgen_TIDs;
						
						// L7 : PostsetNew = emptyset
						List<UtilityList> postsetNew = new ArrayList<UtilityList>();
						
						// for each item J in the postset
						boolean passedHUIPruning = true;
						for(UtilityList jUL : postset) {
							// if J is smaller than I according to the total order on items, we skip it
							if(jUL.item.equals(iUL.item) || compareItems(jUL.item, iUL.item) < 0) continue;
							
							candidateCount++;
							
							if(containsAllTIDS(jUL, newgen_TIDs)) {
								closedSetNew = appendItem(closedSetNew, jUL.item);	
								closedsetNewTIDs = construct(closedsetNewTIDs, jUL);
								
								if(isPassingHUIPruning(closedsetNewTIDs) == false) {
									passedHUIPruning = false;
									break;
								}
							}
							else postsetNew.add(jUL);
						}
						
						if(passedHUIPruning) {
							// L15: write out Closed_setNew and its support
							if(closedsetNewTIDs.sumIutils >= minUtil)
								fileStore(closedSetNew, closedsetNewTIDs.sumIutils, closedsetNewTIDs.elements.size());
							
							// L16: recursive call, must make a copy of preset before the recursive call
							List<UtilityList> presetNew = new ArrayList<UtilityList>(preset);
							chuiMiner(false, closedSetNew, closedsetNewTIDs, presetNew, postsetNew);
						}
						
						// L17: Preset = Preset U {i}
						preset.add(iUL);
					}
				}	
			}
		}

		private UtilityList construct(UtilityList pX, UtilityList pY) {
			
			// create an empy utility list for pXY
			UtilityList uXE = new UtilityList(pY.item);

			double totalUtility = pX.sumIutils + pX.sumRutils;
			
			// for each element in the utility list of pX
			for(Element eX : pX.elements){
				// do a binary search to find element ey in py with tid = ex.tid
				Element eY = findElementWithTID(pY, eX.tid);
				if(eY == null){
					totalUtility -= (eX.iutils + eX.rutils);
					if(totalUtility < minUtil) {
						return null;
					}
					continue;
				}
				// Create the new element
				// TRICKY PART :  WE NEED TO SUBTRACT  ELMX.RUTIL - ELME.iutil
				// THIS IS BECAUSE DCI  DOES NOT ADD ITEMS TO AN ITEMSET ACCORDING TO THE TOTAL ORDER
				Element elmXe = new Element(eX.tid, eX.iutils + eY.iutils, eX.rutils - eY.iutils);
				// add the new element to the utility list of pXY
				uXE.addElement(elmXe);
			}
			// return the utility list of Xe.
			return uXE;
		}
			
		private int[] appendItem(int[] itemset, int item) {
			int [] newgen = new int[itemset.length+1];
			System.arraycopy(itemset, 0, newgen, 0, itemset.length);
			newgen[itemset.length] = item;
			return newgen;
		}
	
		private boolean isDuplicate(UtilityList newgenTIDs, List<UtilityList> preset) {
			// L25
			// for each integer j in preset
			for(UtilityList j : preset){				
				// for each element in the utility list of pX
				boolean containsAll = true;
				for(Element elmX : newgenTIDs.elements){
					// do a binary search to find element ey in py with tid = ex.tid
					Element elmE = findElementWithTID(j, elmX.tid);
					if(elmE == null){
						containsAll = false;
						break;
					}
				}
				// L26 :  
				// If tidset of newgen is included in tids of j, return true
				if(containsAll){
					// IMPORTANT
					// NOTE THAT IN ORIGINAL PAPER THEY WROTE FALSE, BUT IT SHOULD BE TRUE
					return true; 
				}
			}
			return false;  // NOTE THAT IN ORIGINAL PAPER THEY WROTE TRUE, BUT IT SHOULD BE FALSE
		}

		private boolean isPassingHUIPruning(UtilityList utilitylist) {
			if (utilitylist != null)
				return utilitylist.sumIutils +  utilitylist.sumRutils >= minUtil;
			else
				return false;
		}

		private boolean containsAllTIDS(UtilityList ul1, UtilityList ul2) {
			// for each integer j in preset
			for(Element elmX : ul2.elements) {
				// do a binary search to find element ey in py with tid = ex.tid
				Element elmE = findElementWithTID(ul1, elmX.tid);
				if(elmE == null) {
					return false;
				}
			}
			return true;
		}	
		
		public void memStore(int [] itemset, double sumIutils, int support) {
			// if the itemset is larger than the largest CHUI found until now
			if(itemset.length >= listItemsetsBySize.size()) {
				// create some new list in the structure for storing CHUIs to store the itemset
				int i= listItemsetsBySize.size();
				while(i <= itemset.length) {
					listItemsetsBySize.add(new ArrayList<Itemset>());
					i++;
				}
			}
			// add the itemset to the list of MLCHUIs having the same size
			List<Itemset> listToAdd = listItemsetsBySize.get(itemset.length);
			listToAdd.add(new Itemset(itemset, sumIutils, support));
		}
		
		private void fileStore(int[] itemset, double sumIutils, int support) throws IOException {
			patternCount++; // increase the number of MLCHUIs found
			 
			// if the user chose to save to memory
			if(writer == null) {
				//memStore(itemset, sumIutils, support);
			}
			else {
				// If the user decide to save to file, create a string buffer
				StringBuilder buffer = new StringBuilder();
				for (int i = 0; i < itemset.length; i++) { // append the prefix
					buffer.append(itemset[i]);
					buffer.append(' ');
				}
		
				buffer.append(" #SUP: ");	// append the support value
				buffer.append(support);			
				buffer.append(" #UTIL: ");	// append the utility value
				buffer.append(sumIutils);	
				writer.write(buffer.toString());	// write to file
				writer.newLine();
			}
		}

	    private double peakHeapUsage()
	    {
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
		
		public void printLogo() {	// simply print out the logo, pretty useless stuff
			System.out.println("====================");
			System.out.println(" CHUI-Miner Mark II ");
			System.out.println("====================");
			System.out.println();
		}
		
		public void printStatistics() throws IOException {
			long runtime = timerStop - timerStart;
			System.out.println("============= CHUI-MINER** STATISTICS =============");
			System.out.println(" Given minutil     : " + this.minUtil);
			System.out.println(" Approx runtime    : " + runtime + " ms ("+ runtime/1000.0 +" s)");
			System.out.println(" Peak memory used  : " + this.peakHeapUsage()  + " MB");
			System.out.println(" Pattern found     : " + this.patternCount); 
			System.out.println(" Candidate count   : " + this.candidateCount);
			System.out.println("===================================================");
		}
}
