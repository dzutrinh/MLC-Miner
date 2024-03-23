package MLCMiner;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

// Taxonomy
// --------
// Class to store a dataset's taxonomy on memory for direct access
public class Taxonomy {

	public HashMap<Integer, Integer> mapChildToParent;	// K: Child - V: Parent
	
	// default constructor
	public Taxonomy() { 				 
		mapChildToParent = new HashMap<Integer, Integer>();
	}

	// another constructor
	public Taxonomy(String filename, Dataset db) throws IOException { 
		mapChildToParent = new HashMap<Integer, Integer>();
		load(filename, db);
	}
	
	// add a tuple to the taxonomy 
	public void add(int p, int c) {
		mapChildToParent.put(c, p);
	}
	
	// load taxonomy from text file
	public void load(String filename, Dataset db) throws IOException {
		BufferedReader	reader = new BufferedReader(new FileReader(filename)); 
		String			line;

		try {
			while ((line = reader.readLine()) != null) {		// scanning through the text file
			
				if (line.isEmpty() == true || line.charAt(0)=='#' || line.charAt(0)=='@') 
					continue;									// skipping comments and empty lines
											
				String	tokens[] = line.split(",");				// splitting string using ','														
				int	child = Integer.parseInt(tokens[0]);		// child comes first								
				int parent = Integer.parseInt(tokens[1]);		// then its parent							

				if (parent > db.getMaxItem())
					db.setMaxItem(parent);

				add(parent, child);								// then add this tuple into the list
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			if(reader != null) reader.close(); 
		}
	}
	
	// access index-th parent
	public Integer parent(int child) {
		return mapChildToParent.get(child);
	}

	public Set<Integer> children() {
		return mapChildToParent.keySet();
	}
		
	// return the number of parent nodes in the taxonomy - for statistical purposes only
	public int parentCount() {
		ArrayList<Integer> parents = new ArrayList<>();
		ArrayList<Integer> v = new ArrayList<>(mapChildToParent.values());
		for (Integer i : v) {
			if (!parents.contains(i))
				parents.add(i);
		}
		return parents.size();
	}
}
