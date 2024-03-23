package MLCMiner;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a EU-List structure, used by the MLC-Miner algorithm.
 * List of utility maps
 *
 * @see MLC-Miner
 * @see UtilityMap
 * @author Trinh D.D. Nguyen
 */
public class EUList {
	
	public List<UtilityMap> list;
	 
	public int maxSupport;
	
	public EUList() {
		maxSupport = 0;
		list = new ArrayList<UtilityMap>();
	}
	
	public EUList(EUList eul) {
		list = new ArrayList<UtilityMap>(eul.list);
		maxSupport = eul.maxSupport;
	}
	
	public void add(UtilityMap ul) {
		if (ul == null) return;
		int nsupp = ul.getSupport();
		if (maxSupport < nsupp) maxSupport = nsupp;
		list.add(ul);		
	}
	
	public int getMaxSupport() {
		return maxSupport;
	}
	
	public int size() {
		return list.size();
	}
	
	public UtilityMap get(int index) {
		return list.get(index);
	}
}
