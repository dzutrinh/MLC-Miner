package MLCMiner;

import java.util.HashMap;

//	UtilityMap
//	----------
//	UtilityMap structure, Utility-List and P-Set hybrid
//	Coded by Trinh D.D. Nguyen, 
public class UtilityMap {
	int		item;  			// the item
	double	sumIutils = 0;	// the sum of item utilities
	double	sumRutils = 0;	// the sum of remaining utilities
	HashMap<Integer, Element> mapElements = new HashMap<Integer, Element>();
	int size;
	
	// Constructor.
	// @param item the item that is used for this utility list
	public UtilityMap(int item){
		this.item = item;
		this.size = 0;
	}
	
	// Add an element to this utility list and update the sums at the same time.
	public void addElement(Element element){
		sumIutils += element.iutils;
		sumRutils += element.rutils;
		mapElements.put(element.tid, element);
		size++;
	}
	
	// Get the support of the itemset represented by this utility-list
	public int getSupport() {
		return size;
	}
	
	// Check for the presence of a TID 
	public boolean hasTID(Integer tid) {
		return mapElements.get(tid) != null;	// cost of O(1)
	}
	
	public String toString() {
		String result = " Item = [" + item + "] (sup = " + getSupport() + ")\n"; 

		for (Element e : mapElements.values()) {
			result += "  TID: " + String.format("%-6d", e.tid) + 
							   " | iutil = " + String.format("%8.2f", e.iutils) + 
							   " | rutil = " + String.format("%8.2f", e.rutils) + 
							   "\n";
		}
		result += "              | SUMIU = " + String.format("%8.2f", sumIutils) + 
				  " | SUMRU = " + String.format("%8.2f", sumRutils);
		return result;
	}
}
