package MLCMiner;

public class Element {

	public final int tid ;		/** transaction id */   
	public final double iutils;	/** itemset utility */   
	public double rutils;		/** remaining utility */ 
	
	public Element(int tid, Double iutils, Double rutils) {
		this.tid = tid;
		this.iutils = iutils;
		this.rutils = rutils;
	}
		
}
