package CHUIMiner_MK2;

import java.util.Arrays;

 
public class Itemset {
	public int[] itemset;
	double utility;
	int support;

	public Itemset(int[] itemset, double utility, int support) {
		this.itemset = itemset;
		this.utility = utility;
		this.support = support;
	}

//	@Override
	public String toString() {
		return Arrays.toString(itemset) + " utility : " + utility + " support:  " + support;
	}

}
