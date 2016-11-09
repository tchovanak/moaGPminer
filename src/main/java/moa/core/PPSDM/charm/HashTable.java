package moa.core.PPSDM.charm;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class represents a hash table
 * 
 *
 * Copyright (c) 2008-2012 Philippe Fournier-Viger
 * 
 * This file is part of the SPMF DATA MINING SOFTWARE
 * (http://www.philippe-fournier-viger.com/spmf).
 *
 * SPMF is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * SPMF is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with SPMF.  If not, see <http://www.gnu.org/licenses/>.
 */
public class HashTable {
	
	int size;
        int count = 0;
	List<Itemset>[] table;
	
	public HashTable(int size){
		this.size = size;
		table = new ArrayList[size];
	}
	public boolean containsSupersetOf(Itemset itemsetObject) {
		int hashcode = hashCode(itemsetObject);
		if(table[hashcode] ==  null){
			return false;
		}
		for(Object object : table[hashcode]){
			Itemset itemsetObject2 = (Itemset)object;
			if(itemsetObject2.getItems().size() == itemsetObject.getItems().size() &&
					itemsetObject2.getItems().containsAll(itemsetObject.getItems())
					){  // FIXED BUG 2010-10: containsAll instead of contains.
				return true;
			}
		}
		return false;
	}
	public void put(Itemset itemsetObject) {
                this.count++;
		int hashcode = hashCode(itemsetObject);
		if(table[hashcode] ==  null){
			table[hashcode] = new ArrayList<Itemset>();
		}
            try {
                table[hashcode].add((Itemset) itemsetObject.clone());
            } catch (CloneNotSupportedException ex) {
                Logger.getLogger(HashTable.class.getName()).log(Level.SEVERE, null, ex);
            }
	}
	
	public int hashCode(Itemset itemsetObject){
		int hashcode =0;
                BitSet tidset = itemsetObject.getTidset();
//		for (int bit = bitset.nextSetBit(0); bit >= 0; bit = bitset.nextSetBit(bit+1)) {
		for (int tid=tidset.nextSetBit(0); tid >= 0; tid = tidset.nextSetBit(tid+1)) {
			hashcode += tid;
	    }
		return (hashcode % size);
	}
}
