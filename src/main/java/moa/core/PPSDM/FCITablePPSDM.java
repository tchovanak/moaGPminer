/*
 *    FCITable.java
 *    Copyright (C) 2012 Universitat Politècnica de Catalunya
 *    @author Massimo Quadrana <max.square@gmail.com>
 *
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program; if not, write to the Free Software
 *    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package moa.core.PPSDM;

import java.io.Serializable;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import moa.core.InvertedFCIIndex;
import moa.core.SemiFCI;
import moa.core.SemiFCIid;
import moa.utils.PPSDM.UtilitiesPPSDM;

/*
   Changes > Author: Tomas Chovanak
   Added clearTable method to remove null  and empty fields from table and inverted index.
*/
public class FCITablePPSDM implements Iterable<SemiFCI>, Serializable  {
    
    private static final long serialVersionUID = 1L;

    protected class FCIArray implements Iterable<SemiFCI>, Serializable {    
        private static final long serialVersionUID = 4500840981452151513L;
        private ArrayList<SemiFCI> itemsets;
        private LinkedList<Integer> garbageQueue;
        private HashSet<Integer> goodPositions; //we store here the positions in the array containing a semiFCI to speedup iteration over the FCIArray
        private int size; //local variable to store the real number of semiFCIs stored
        
        /**
        * Default constructor. Constructs an istance of FCIarray with an empty set
        * of itemstes and an empty garbage queue.
        */
        public FCIArray() {
            this.itemsets = new ArrayList<>();
            this.garbageQueue = new LinkedList<>();
            this.goodPositions = new HashSet<>();
            this.size = 0;
        }

        /**
        * Add an itemset to the FCIArray and returns the ID assigned.
        * and each semi-FCI in the FCI-array is assigned an ID
        * 
        * @param itemset itemset to be added
        * @return id assigned to the itemset
        */
        public SemiFCIid addSemiFCI(SemiFCI itemset) {
            int pos;
            if(!this.garbageQueue.isEmpty()){
                pos = garbageQueue.poll();
                itemset.getIdOriginal().setPosition(pos);
                this.itemsets.set(pos, itemset);
            }else{
                pos = this.itemsets.size();
                itemset.getIdOriginal().setPosition(pos);
                this.itemsets.add(itemset);
            }
            this.size++;
            this.goodPositions.add(pos);
            return itemset.getId();
            
        }

        /**
        * Remove a semiFCI from the table by putting it in the garbage queue.
        * @param position id of the semiFCI in the array
        */
        public void removeSemiFCI(int position) {
            //add this position to the garbage queue
            //if(!this.garbageQueue.contains(position))
            this.garbageQueue.add(position);
            this.goodPositions.remove(position);
            this.itemsets.set(position, null); //free up memory associated to ne removed semiFCI
            this.size--;
        }

        /**
        * Returns a copy of the semiFCI in the passed position
        *
        * @param position position of the semiFCI in the FCIArray
        * @return semiFCI in the desired position
        */
        public SemiFCI getFCI(int position) {

            if(!this.garbageQueue.contains(position))
                try {
                    return (SemiFCI) this.itemsets.get(position).clone();
                } catch (CloneNotSupportedException ex) {
                    Logger.getLogger(FCITablePPSDM.class.getName()).log(Level.SEVERE, null, ex);
                    return null;
                }
            else
                return null;
        }
        
        /**
        * Returns a original of the semiFCI in the passed position
        *
        * @param position position of the semiFCI in the FCIArray
        * @return semiFCI in the desired position
        */
        public SemiFCI getFCIOriginal(int position) {
            if(!this.garbageQueue.contains(position))
                return this.itemsets.get(position);
            else
                return null;
        }

        /**
        * Size of the FCIArray
        * @return the number of SemiFCI in the FCIArray
        */
        public int size(){
            return this.size;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            for(SemiFCI fci:this.itemsets){
                sb.append(fci.toString());
                sb.append("\n");
            }
            return sb.toString();
        }

        @Override
        public Iterator<SemiFCI> iterator() {
            return new FCIArray.FCIArrayIterator();
        }
        
        public Iterator<SemiFCI> iteratorOriginal() {
            return new FCIArray.FCIArrayIteratorOriginal();
        }

        class FCIArrayIterator implements Iterator<SemiFCI> {
            private Iterator<Integer> goodPosIterator = goodPositions.iterator();
            private int currentPosition;
            @Override
            public boolean hasNext() {
                return this.goodPosIterator.hasNext();
            }

            @Override
            public SemiFCI next() {
                if(!this.hasNext()) throw new NoSuchElementException();
                    currentPosition = this.goodPosIterator.next();
                try {
                    return (SemiFCI) ((SemiFCI) itemsets.get(currentPosition)).clone();
                } catch (CloneNotSupportedException ex) {
                    Logger.getLogger(FCITablePPSDM.class.getName()).log(Level.SEVERE, null, ex);
                    throw new NoSuchElementException();
                }
            }

            @Override
            public void remove() {
                this.goodPosIterator.remove();
                garbageQueue.add(currentPosition);
                itemsets.set(currentPosition, null); //free up memory associated to ne removed semiFCI
                size--;
            }

        }
        
        class FCIArrayIteratorOriginal implements Iterator<SemiFCI> {
            
            private Iterator<Integer> goodPosIterator = goodPositions.iterator();
            private int currentPosition;

            @Override
            public boolean hasNext() {
                return this.goodPosIterator.hasNext();
            }

            @Override
            public SemiFCI next() {
                if(!this.hasNext()) throw new NoSuchElementException();
                currentPosition = this.goodPosIterator.next();
                return itemsets.get(currentPosition);
                
            }

            @Override
            public void remove() {
                this.goodPosIterator.remove();
                garbageQueue.add(currentPosition);
                itemsets.set(currentPosition, null); //free up memory associated to ne removed semiFCI
                size--;
            }

        }
    }
    
    /*
            The set L is partitioned accordingly
            to the size of the semi-FCIs in the last window WL. Each partition is stored
            in an array, called FCI-array, 
    */
    private HashMap<Integer, FCIArray> table;
    private InvertedFCIIndex invertedIndex;
    private int maxFCISize;
    
    public int nAdded = 0;
    public int nRemoved = 0;

    /**
     * Default constructor. Creates a new FCITable with an empty Inverted Index
     * associated.
     */
    public FCITablePPSDM() {
        this.maxFCISize = 0;
        this.table = new HashMap<>();
        this.invertedIndex = new InvertedFCIIndex();
    }

    /**
     * Add an itemset to the FCITable and returns the assigned ID
     * @param itemset itemset to be added to the table
     * @return id assigned to the passed itemset
     */
    public SemiFCIid addSemiFCI(SemiFCI itemset) {
        int itemsetSize = itemset.getId().getDimension();
        if(itemsetSize > this.maxFCISize)
            this.maxFCISize = itemsetSize;
        if(!this.table.containsKey(itemsetSize))
            this.table.put(itemsetSize, new FCIArray());
        this.table.get(itemsetSize).addSemiFCI(itemset);
        this.invertedIndex.addSemiFCI(itemset);
        this.nAdded++;
        return itemset.getId();
        
    }

    /**
     * Removes a semiFCI from the table given the corresponding ID.
     * @param id id of the semiFCI to be removed
     */
    public void removeSemiFCI(SemiFCIid id) {
        this.invertedIndex.removeSemiFCI(getFCI(id));
        this.table.get(id.getDimension()).removeSemiFCI(id.getPosition());
        if(this.table.get(id.getDimension()).size() == 0){
            this.table.remove(id.getDimension());
        }
        this.nRemoved++;
    }
    
    /**
     * Removes a semiFCI from the table given the corresponding ID (via iterator).
     * @param id id of the semiFCI to be removed
     */
    public void removeSemiFCI(SemiFCIid id, Iterator<SemiFCI> iter) {
        this.invertedIndex.removeSemiFCI(getFCIOriginal(id));
        //this.table.get(id.getDimension()).removeSemiFCI(id.getPosition());
        iter.remove();
        this.nRemoved++;
    }
    
    /*
        Removes nulls from table and inverted index
    */
    public void clearTable(){
        HashMap<Integer, FCIArray> table = new HashMap<>();
        InvertedFCIIndex invertedIndex = new InvertedFCIIndex();
        for(Map.Entry<Integer,FCIArray> tableEntry : this.table.entrySet()){
            if(tableEntry != null && tableEntry.getValue() != null){
                Iterator<SemiFCI> iterator = tableEntry.getValue().iterator();
                FCIArray newFciAray = new FCIArray();
                while(iterator.hasNext()){
                    SemiFCI sfci = iterator.next();
                    newFciAray.addSemiFCI(sfci);
                    invertedIndex.addSemiFCI(sfci);
                }
                table.put(tableEntry.getKey(), newFciAray);
            }
        }
        this.invertedIndex = invertedIndex;
        this.table = table;
        this.nAdded = 0;
        this.nRemoved = 0;
        //this.invertedIndex.clearIndex();
    }

    /**
     * Returns the instance of the semiFCI associated to the passed id.
     * @param id id of the semiFCI
     * @return Instance of the semiFCI if present in the table, null otherwise
     */    
    public SemiFCI getFCI(SemiFCIid id) {
        return this.table.get(id.getDimension()).getFCI(id.getPosition());
    }
    
    /**
     * Returns the instance of the semiFCI associated to the passed id.
     * @param id id of the semiFCI
     * @return Instance of the semiFCI if present in the table, null otherwise
     */
    
    public SemiFCI getFCIOriginal(SemiFCIid id) {
        return this.table.get(id.getDimension()).getFCIOriginal(id.getPosition());
    }

    /**
     * Passed a semiFCI id, computes the maximum k (>k1) for which the support greater
     * than the corresponding value in the minimum support vector.
     *
     * @param id id of the semiFCI
     * @param supVector minimum support vector
     * @param k1 starting value for k
     * @return maximum k value
     */
    public int computeK(SemiFCIid id, int[] supVector, int startK) {    
        return this.table.get(id.getDimension()).getFCIOriginal(id.getPosition()).computeK(supVector, startK);
    }

    /**
     * Returns the ID in the FCI table of the passed itemset (-1 if no entry is found)
     * @param itemset itemset to be selected
     * @return id of the semiFCI with the passed itemset
     */
    public SemiFCIid select(List<Integer> itemset) {
        List<Integer> positionList = this.invertedIndex.join(itemset,itemset.size());
        if(positionList.isEmpty())
            return new SemiFCIid(itemset.size(),-1);
        else
            return new SemiFCIid(itemset.size(), positionList.get(0));
    }

    /**
     * Returns the ID of the Smallest semiFCI Superset of the passed itemset
     * @param itemset itemeset to be selected
     * @return id of the semiFCI that is SFS of the passed itemset
     */
//    public SemiFCIid selectSFS(SemiFCI fci, boolean checkSupport) {
//        
//        List<SemiFCIid> sfsList = this.invertedIndex.selectSFS(fci.items);
//        
//        if(sfsList.isEmpty())
//            return new SemiFCIid (-1,-1);
//        else{
//            if(!checkSupport)
//                return sfsList.get(0);
//            else{
//                //use support information to check closure property
//                for(SemiFCIid sfsId:sfsList){
//                    SemiFCI sfsCandidate = this.getFCI(sfsId);
//                    if(sfsCandidate.getApproximateSupport(sfsCandidate.getKValue())==
//                            fci.getApproximateSupport(sfsCandidate.getKValue()))
//                        return sfsId;
//                }
//            }
//        }
//    }
    
    
    public SemiFCIid selectSFS(SemiFCI fci, boolean checkSupport) {   
        for(int sfsSize = fci.size()+1; sfsSize < this.maxFCISize; sfsSize++){
            List<Integer> sfsPosList = this.invertedIndex.join(fci.getItems(),sfsSize);
            if(!sfsPosList.isEmpty()){
                if(!checkSupport)
                    return new SemiFCIid(sfsSize, sfsPosList.get(0));
                else{
                    for(Integer pos:sfsPosList){
                        SemiFCI sfsCandidate = this.getFCI(new SemiFCIid(sfsSize, pos));
                        //use support information to check closure property
                        if(sfsCandidate.getApproximateSupport(sfsCandidate.getKValue())==
                            fci.getApproximateSupport(sfsCandidate.getKValue()))                    
                        return sfsCandidate.getId();
                    }
                }
            }
        }
        //no sfs found!
        return new SemiFCIid(-1,-1);
    }
    
    public SemiFCIid selectSFS(List<Integer> subSetItemset, List<Integer> superSetItemset){
        for(int sfsSize = subSetItemset.size()+1; sfsSize < superSetItemset.size(); sfsSize++){
            List<Integer> sfsPosList = this.invertedIndex.join(subSetItemset,sfsSize);
            
            for(Integer pos:sfsPosList){
                SemiFCI sfsCandidate = this.getFCI(new SemiFCIid(sfsSize, pos));
                //check intersection property necessary in UpdateSubsetNotInL procedure
                if(UtilitiesPPSDM.intersect2orderedList(sfsCandidate.getItems(), subSetItemset).equals(subSetItemset))
                    return sfsCandidate.getId();
            }
        }
        //no sfs found!
        return new SemiFCIid(-1,-1);
    }
    
    /**
     * Prepare the table to be able to process the next segment
     */
    public void clearNewItemsetsTable() {
        this.invertedIndex.clearNewItemsetsTable();
    }
    
    /**
     * Size of the FCITable
     * @return the number of SemiFCI currently stored in the table
     */
    public int size(){
        int size = 0;
        for(FCIArray array:this.table.values())
            size += array.size();
        
        return size;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for(SemiFCI fci:this){
            sb.append(fci.toString()).append("\n");
        }
        return sb.toString();
    }

    /**
     * Provides a size descending iterator of the semiFCIs stored in the table
     * at the moment.
     * 
     * @return semiFCIs size descending iterator
     */
    @Override
    public Iterator<SemiFCI> iterator() {
        return new FCITableIterator(true);
    }
    
    public Iterator<SemiFCI> iteratorOriginal() {
        return new FCITableIterator(false);
    }

    class FCITableIterator implements Iterator<SemiFCI>{
        private ArrayList<Integer> keys;
        private Iterator<SemiFCI> currentIterator;
        private int currentKeyIndex;
        private boolean hasNextElement = false;
        private boolean cloning = false;
        public FCITableIterator(boolean cloning) {
            this.cloning = cloning;
            this.keys = new ArrayList<Integer>(table.keySet());
            Collections.sort(keys);
            this.currentKeyIndex = keys.size()-1;
            if(this.currentKeyIndex>=0){
                if(cloning){
                    this.currentIterator = (Iterator<SemiFCI>) table.get(keys.get(currentKeyIndex)).iterator();
                }else{
                    this.currentIterator = (Iterator<SemiFCI>) table.get(keys.get(currentKeyIndex)).iteratorOriginal();
                }
            } 
        }
        
        public boolean hasNext() {
            checkForNextElement();
            return this.hasNextElement;
        }

        public SemiFCI next() {
            
            return this.currentIterator.next();
        }
        
        public void remove() {
            this.currentIterator.remove();
        }
        
        private void checkForNextElement()
        {
            if(this.currentIterator == null){
                this.hasNextElement = false;
                return;
            }
            if(this.currentIterator.hasNext())
                this.hasNextElement = true;
            else
            {
                this.currentKeyIndex--;
                this.hasNextElement = false;
                while(this.currentKeyIndex>=0)
                {   
                    if(cloning){
                        this.currentIterator = (Iterator<SemiFCI>) table.get(this.keys.get(this.currentKeyIndex)).iterator();
                    }else{
                        this.currentIterator = (Iterator<SemiFCI>) table.get(this.keys.get(this.currentKeyIndex)).iteratorOriginal();
                    }
                    
                    if(this.currentIterator.hasNext())
                    {
                        this.hasNextElement = true;
                        return;
                    }
                    this.currentKeyIndex--;
                }    
            }
        }
    }
}
