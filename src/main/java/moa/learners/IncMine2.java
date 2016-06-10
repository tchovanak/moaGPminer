/*
 *    IncMine.java
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
package moa.learners;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import moa.MOAObject;
import moa.core.*;
import com.github.javacliparser.FloatOption;
import com.github.javacliparser.IntOption;
import com.github.javacliparser.MultiChoiceOption;
import com.yahoo.labs.samoa.instances.Prediction;
import com.yahoo.labs.samoa.instances.Instance;
import moa.core.InstanceExample;

public class IncMine2 extends AbstractLearner implements Observer {
    
    private static final long serialVersionUID = 1L;

    private class Subset {
        protected List<Integer> itemset;
        protected int startIndex;
        protected boolean skipSubsetsNotInL;
        public Subset(List<Integer> itemset, int startIndex, boolean skipSubsetsNotInL)
        {
            this.itemset = itemset;
            this.startIndex = startIndex;
            this.skipSubsetsNotInL = skipSubsetsNotInL;
        }

    }

    public IntOption windowSizeOption = new IntOption(
            "windowSize", 'w',
            "Size of the sliding window (in number of segments).", 10);
    
    public IntOption maxItemsetLengthOption = new IntOption(
            "maxItemsetLength", 'm',
            "Maximum length of frequent closed itemset to be considered.", -1);
    
    public IntOption numberOfGroupsOption = new IntOption(
            "numberOfGroups", 'm',
            "Number of groups to be created from users.", 2);
    
    public FloatOption minSupportOption = new FloatOption(
            "minSupport", 's',
            "Minimum support.", 0.1, 0, 1);
    
    public FloatOption relaxationRateOption = new FloatOption(
            "relaxationRate", 'r',
            "Relaxation Rate.", 0.5, 0, 1);
    
    public IntOption fixedSegmentLengthOption = new IntOption(
            "fixedSegmentLength", 'l',
            "Fixed Segment Length.", 1000);
    
    public static int windowSize;
    public static int numberOfGroups;
    protected double r;
    protected double sigma;
    
    
    protected FCITable fciTableGlobal;
    protected ArrayList<FCITable> fciTablesGroups;
    protected SlidingWindowManager swmGlobal;
    protected ArrayList<SlidingWindowManager> swmGroups;
    protected int[] minsup;
        
    protected boolean preciseCPUTiming;
    protected long evaluateStartTime;
    
    private long startUpadateTime;
    private long endUpdateTime;
    
    @Override
    public void resetLearningImpl() {
        this.preciseCPUTiming = TimingUtils.enablePreciseTiming();
        this.evaluateStartTime = TimingUtils.getNanoCPUTimeOfCurrentThread();
       
        this.fciTableGlobal = new FCITable();
        //prepares FCI table foreach group
        for(int i = 0; i < this.numberOfGroupsOption.getValue(); i++){ 
            fciTablesGroups.set(i, new FCITable());
        }
        IncMine2.windowSize = this.windowSizeOption.getValue();
        IncMine2.numberOfGroups = this.numberOfGroupsOption.getValue();
        this.sigma = this.minSupportOption.getValue();
        this.r = this.relaxationRateOption.getValue();
        
        double min_sup = new BigDecimal(this.r*this.sigma).setScale(8, RoundingMode.DOWN).doubleValue(); //necessary to correct double rounding error

        this.swmGlobal = new FixedLengthWindowManager(min_sup, this.maxItemsetLengthOption.getValue(), this.fixedSegmentLengthOption.getValue());
        this.swmGlobal.deleteObservers();
        this.swmGlobal.addObserver(this);        
        // prepares sliding window for each group
        for(int i = 0; i < IncMine2.numberOfGroups; i++){
            this.swmGroups.set(i, new FixedLengthWindowManager(min_sup, this.maxItemsetLengthOption.getValue(), this.fixedSegmentLengthOption.getValue()));
            this.swmGroups.get(i).deleteObservers();
            this.swmGroups.get(i).addObserver(this);        
        }
    }

    @Override
    public void trainOnInstanceImpl(Instance inst) {
        this.swmGlobal.addInstance(inst);
        this.swmGroups.get(inst.index(0)).addInstance(inst); // on index 0 there should be group id prepended before session data
    }
    
    @Override
    public void trainOnInstance(Example e) {
        Instance inst = (Instance)e.getData();
        this.swmGlobal.addInstance(inst);
        this.swmGroups.get(inst.index(0)).addInstance(inst); // on index 0 there should be group id prepended before session data
        
    }

    @Override
    protected Measurement[] getModelMeasurementsImpl() {
        long evaluateTime = TimingUtils.getNanoCPUTimeOfCurrentThread();
        double time = TimingUtils.nanoTimeToSeconds(evaluateTime - this.evaluateStartTime);
        List<Measurement> measurementList = new LinkedList<Measurement>();
        measurementList.add(new Measurement("model total memory (Megabytes)",
                Runtime.getRuntime().totalMemory() / (1024 * 1024)));
        measurementList.add(new Measurement("model time (" + (preciseCPUTiming ? "cpu " : "") + "seconds)", time));
        measurementList.add(new Measurement("number of approximate frequent closed itemsets", this.fciTable.size()));
        return measurementList.toArray(new Measurement[measurementList.size()]);
    }

    @Override
    public void getModelDescription(StringBuilder out, int indent) {
        // Output closed frequent graphs
        StringUtils.appendIndented(out, indent, "Number of closed frequent frequent graphs: ");
        out.append(this.fciTableGlobal.size());
        StringUtils.appendNewline(out);
        out.append(this.toString());
    }

    public boolean isRandomizable() {
        return false;
    }

    public MOAObject getModel() {
        return null;
    }
    
    /**
     * Update the FCITable and the InvertedFCIIndex to keep semiFCIs up to date
     * @param o
     * @param arg
     */
    public void update(Observable o, Object arg) {

        this.fciTableGlobal.nAdded = 0;
        this.fciTableGlobal.nRemoved = 0;
        for(int i = 0; i < IncMine2.numberOfGroups; i++){
            this.fciTablesGroups.get(i).nAdded = 0;
            this.fciTablesGroups.get(i).nRemoved = 0;
        }
        
        this.startUpadateTime = TimingUtils.getNanoCPUTimeOfCurrentThread();
        int lastSegmentLenght = (Integer)arg;

        this.minsup = Utilities.getIncMineMinSupportVector(sigma,r,windowSize,lastSegmentLenght);
        
        //for each FCI in the last segment in size ascending order
        for(SemiFCI fci:this.swmGlobal.getFCI()) {
            
            SemiFCIid fciId = this.fciTableGlobal.select(fci.getItems());
            boolean newfci = false;
            
            if(fciId.isValid()) {
                //fci is already in the FCITable
                this.fciTableGlobal.getFCI(fciId).pushSupport(fci.currentSupport());
                computeK(fciId, 0);                
            }else{
                //fci is not in the FCITable yet
                newfci = true;
                //set semiFCI support to support of his SFS (last segment excluded)
                SemiFCIid sfsId = this.fciTableGlobal.selectSFS(fci, false);
                
                if(sfsId.isValid()) {
                    int[] fciSupVector = fci.getSupports();
                    int[] sfsSupVector = this.fciTable.getFCI(sfsId).getSupports(); 
                    //note: the SFS has not been updated yet! so his old support goes from index 0 to length-2
                    if(fciSupVector.length > 1){
                        System.arraycopy(sfsSupVector, 0, fciSupVector, 1, fciSupVector.length - 2);
                        fci.setSupports(fciSupVector);
                    }
                }
                //add a new entry to the table and update the inverted index
                fciId = fciTable.addSemiFCI(fci);
                computeK(fciId, 0);
            }
            
            if(fci.size() > 1)
                enumerateSubsets(new Subset(fci.getItems(),0,false), new ArrayList<Integer>(), fci.getSupports(), fciId, newfci);
                
        }
        
        this.fciTable.clearNewItemsetsTable();
        
        //iterate in size-descending order over the entire FCITable to remove unfrequent semiFCIs
        for(Iterator<SemiFCI> iter = this.fciTable.iterator(); iter.hasNext(); ) { 
            SemiFCI s = iter.next();
            if(!s.isUpdated()) {
                s.pushSupport(0);
                s.setUpdated(false);
                int k = computeK(s.getId(), 0);
                
                if (k == -1){
                    this.fciTable.removeSemiFCI(s.getId(), iter);
                }
                else{
                    SemiFCIid sfsId = this.fciTable.selectSFS(s, true);
                    if(sfsId.isValid())
                        this.fciTable.removeSemiFCI(s.getId(), iter);
                }
            }else{
                s.setUpdated(false);
            }

        }       
                
        this.endUpdateTime = TimingUtils.getNanoCPUTimeOfCurrentThread();
        System.out.println("Update done in " + this.getUpdateTime()/1e6 + " ms.");
        System.out.println(fciTable.size() + " SemiFCIs actually stored\n");
        
    }

    /**
     * Enumerates all the proper subsets of the passed itemset with skip of repeated subsets.
     * It allows to skip subsets of semiFCI that have been already updated.
     * @param origSubset original subset to be enumerated
     * @param skipList index of the subsets to be skipped
     * @param supersetSupportVector 
     * @param originalFCI id of the original SemiFCI
     * @param newFCI true if the SemiFCI is a new FCI for the window, false otherwise
     */
    private void enumerateSubsets(Subset origSubset, List<Integer> skipList, int[] supersetSupportVector, SemiFCIid originalFCIid, boolean newFCI)
    {
        List<Subset> subList = new ArrayList<Subset>();
        List<Integer> blackList = new ArrayList<Integer>();
        

        for(int removeIndex = origSubset.startIndex; removeIndex < origSubset.itemset.size(); removeIndex++) {
            if(skipList.contains(removeIndex)) { //don't process subsets of an already updated SemiFCI
                if(removeIndex > 0)     blackList.add(removeIndex-1);
                continue;
            }

            //create subset
            List<Integer> reducedItemset = new ArrayList<Integer>(origSubset.itemset);
            reducedItemset.remove(reducedItemset.size()-removeIndex-1);
            Subset newSubset = new 
                    Subset(reducedItemset, removeIndex, origSubset.skipSubsetsNotInL);
            SemiFCIid id = this.fciTable.select(newSubset.itemset);

            if(id.isValid()) {//subset in L
                
                SemiFCI subFCI = this.fciTable.getFCI(id); 
                if(subFCI.isUpdated()) {
                    if(removeIndex > 0) blackList.add(removeIndex-1); //its subsets don't have to be updated
                
                }else {

                    updateSubsetInL(id, supersetSupportVector, originalFCIid);
                    subList.add(newSubset);
                    
                    supersetSupportVector = subFCI.getSupports();

                    if(newFCI) newSubset.skipSubsetsNotInL = true;
                }
                
            }
            else {//subset not in L
                if(!newSubset.skipSubsetsNotInL)
                    subList.add(newSubset);
                if(newFCI)
                    updateSubsetNotInL(newSubset.itemset, originalFCIid);
            }
        }
        for(Subset s:subList) {
            if(s.itemset.size() == 1) return;
            enumerateSubsets(s, blackList, supersetSupportVector, originalFCIid, newFCI);
        }
    }
    

    /**
     * Updates the support vector of the proper subset passed of a fciId. It also checks
     * if closure condition holds looking at the most recent superset support. If it doesn't hold
     * delete the SemifFCI.
     * @param subsetId id of the subset to be updated
     * @param supersetSupportVector support vector of the most recently processed superset
     * @param fciId id of the original SemiFCI
     */
    private void updateSubsetInL(SemiFCIid subsetId, int[] supersetSupportVector, SemiFCIid fciId) {

        SemiFCI fci = this.fciTable.getFCI(fciId);

        this.fciTable.getFCI(subsetId).pushSupport(fci.currentSupport());
        
        int k = computeK(subsetId, 1);

        if(k == -1){
            this.fciTable.removeSemiFCI(subsetId);
        }else{
            //checking closure property
            if(this.fciTable.getFCI(subsetId).getApproximateSupport(k) == Utilities.cumSum(supersetSupportVector, k))
                this.fciTable.removeSemiFCI(subsetId);
        }
    }
    
    /**
     * Updates a subset that isn't included in the actual semiFCIs set.
     * @param subset subset to be added
     * @param superFCIid original semiFCI
     */
    private void updateSubsetNotInL(List<Integer> subset, SemiFCIid superFCIid)
    {
        SemiFCI superFCI = this.fciTable.getFCI(superFCIid);
        SemiFCIid sfsId = this.fciTable.selectSFS(subset, superFCI.getItems());

        if(sfsId.isValid()) {
            int[] supportVector = this.fciTable.getFCI(sfsId).getSupports();
            supportVector[0] = superFCI.getSupports()[0];
            
            SemiFCI subFCI = new SemiFCI(subset, 0);
            subFCI.setSupports(supportVector);
            
            int k = subFCI.computeK(this.minsup,1);
            if(k > -1)
                this.fciTable.addSemiFCI(subFCI);
            
        }
    }
    
    /**
     * Calls computeK method the passed semiFCI and deletes it if no longer holds
     * the conditions to be maintained in the window
     * @param id id of the semiFCI
     * @param startK intial k value
     * @return value of k
     */
    public int computeK(SemiFCIid id, int startK) {
        int k = this.fciTable.computeK(id, this.minsup, startK);
//        if(k == -1)
//            this.fciTable.removeSemiFCI(id);
        return k;
    }
    
    public int getNumFCIs(){
        return this.fciTable.size();
    }
    
    public long getUpdateTime(){
        return this.endUpdateTime - this.startUpadateTime;
    }
    
    public int getNAdded(){
        return this.fciTable.nAdded;
    }
    
    public int getNRemoved(){
        return this.fciTable.nRemoved;
    }
    
    public Iterator<SemiFCI> getApproximateFCIIterator(){
        return this.fciTable.iterator();
    }
    
     

    @Override
    public double[] getVotesForInstance(Example e) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Learner[] getSublearners() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Prediction getPredictionForInstance(Example e) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    @Override
    public String toString() {
        return this.fciTable.toString();
    }

}
