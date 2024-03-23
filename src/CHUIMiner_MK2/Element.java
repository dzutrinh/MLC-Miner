package CHUIMiner_MK2;

public class Element {

	// we use double for ituils, rutils instead of int in the original utility list structure
	public final int tid ;		/** transaction id */   
	public final double iutils;	/** itemset utility */   
	public double rutils;		/** remaining utility */ 
	
	/**
	 * Constructor.
	 * @param tid  the transaction id
	 * @param iutils  the itemset utility
	 * @param rutils  the remaining utility
	 */
	public Element(int tid, Double iutils, Double rutils){
		this.tid = tid;
		this.iutils = iutils;
		this.rutils = rutils;
	}
}
