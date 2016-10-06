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
import com.yahoo.labs.samoa.instances.Prediction;
import com.yahoo.labs.samoa.instances.Instance;
import java.util.logging.Level;
import java.util.logging.Logger;
import moa.utils.Configuration;


/*
    (Tomas Chovanak) Modified IncMine alghoritm to mine simultanly group and global 
     frequent itemsets of users.
*/
public class IncMine2 extends AbstractLearner implements Observer {
    
    private static final long serialVersionUID = 1L;

    IncMine2(int value, int value0, int value1, double value2, double value3, int value4) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    void setRelaxationRate(double d) {
        this.r = d;
    }
    
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
    
    private int windowSizeOption;
    private int maxItemsetLengthOption;
    private int numberOfGroupsOption; 
    private double minSupportOption;
    private double relaxationRateOption;
    private int fixedSegmentLengthOption;
    private int groupFixedSegmentLengthOption;

    
    public IncMine2(int windowSizeOption,int maxItemsetLengthOption,
            int numberOfGroupsOption, double minSupportOption,
            double relaxationRateOption, int fixedSegmentLengthOption, 
            int groupFixedSegmentLengthOption){
        this.windowSizeOption = windowSizeOption;
        this.maxItemsetLengthOption = maxItemsetLengthOption;
        this.numberOfGroupsOption = numberOfGroupsOption; 
        this.minSupportOption = minSupportOption;
        this.relaxationRateOption = relaxationRateOption;
        this.fixedSegmentLengthOption = fixedSegmentLengthOption;
        this.groupFixedSegmentLengthOption = groupFixedSegmentLengthOption;

    }
    
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
        System.out.println("reset incmine2");
        this.preciseCPUTiming = TimingUtils.enablePreciseTiming();
        this.evaluateStartTime = TimingUtils.getNanoCPUTimeOfCurrentThread();
       
        this.fciTableGlobal = new FCITable();
        this.fciTablesGroups = new ArrayList<FCITable>();
        //prepares FCI table foreach group
        for(int i = 0; i < this.numberOfGroupsOption; i++){ 
            fciTablesGroups.add(i, new FCITable());
        }
        IncMine2.windowSize = this.windowSizeOption;
        IncMine2.numberOfGroups = this.numberOfGroupsOption;
        this.sigma = this.minSupportOption;
        this.r = this.relaxationRateOption;
        
        double min_sup = new BigDecimal(this.r*this.sigma).setScale(8, RoundingMode.DOWN).doubleValue(); //necessary to correct double rounding error

        this.swmGlobal = new FixedLengthWindowManager(min_sup, this.maxItemsetLengthOption, this.fixedSegmentLengthOption);
        this.swmGlobal.deleteObservers();
        this.swmGlobal.addObserver(this);  
        this.swmGroups = new ArrayList<SlidingWindowManager>();
        
        double groupMinSupport = min_sup;
        // prepares sliding window for each group
        for(int i = 0; i < this.numberOfGroupsOption; i++){
            this.swmGroups.add(i, new FixedLengthWindowManager(groupMinSupport, 
                    this.maxItemsetLengthOption, this.groupFixedSegmentLengthOption ));
            this.swmGroups.get(i).deleteObservers();
            this.swmGroups.get(i).addObserver(this);        
        }
    }

    @Override
    public void trainOnInstanceImpl(Instance inst) {
        this.swmGlobal.addInstance(inst);
        this.swmGroups.get((int)inst.value(0)).addInstance(inst); // on index 0 there should be group id prepended before session data
    }
    
    @Override
    public void trainOnInstance(Example e) {
        Instance inst = (Instance)e.getData();
        if(inst.numValues() <= 1){
            return;
        }
        int groupid = (int)inst.value(0);// on index 0 there should be group id prepended before session data
        if(groupid > -1){
            this.swmGroups.get(groupid).addInstance(inst); 
        }else{
            this.swmGlobal.addInstance(inst);
        }
        
    }

    @Override
    protected Measurement[] getModelMeasurementsImpl() {
        long evaluateTime = TimingUtils.getNanoCPUTimeOfCurrentThread();
        double time = TimingUtils.nanoTimeToSeconds(evaluateTime - this.evaluateStartTime);
        List<Measurement> measurementList = new LinkedList<Measurement>();
        measurementList.add(new Measurement("model total memory (Megabytes)",
                Runtime.getRuntime().totalMemory() / (1024 * 1024)));
        measurementList.add(new Measurement("model time (" + (preciseCPUTiming ? "cpu " : "") + "seconds)", time));
        measurementList.add(new Measurement("number of approximate frequent closed itemsets", this.fciTableGlobal.size()));
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
        
        ObserverParamWrapper param = (ObserverParamWrapper) arg;
        int groupid = param.getGroupid();
        FCITable fciTable = null;
        if(groupid == -1){
            fciTable = this.fciTableGlobal;
        }else{
            fciTable = this.fciTablesGroups.get(groupid);
           
        }
        fciTable.nAdded = 0;
        fciTable.nRemoved = 0;
        
        this.startUpadateTime = TimingUtils.getNanoCPUTimeOfCurrentThread();
        int lastSegmentLenght = param.getSegmentLength();

        this.minsup = Utilities.getIncMineMinSupportVector(sigma,r,windowSize,lastSegmentLenght);
        List<SemiFCI> semiFCIs = null;
        
        try {
            if(groupid == -1){
                semiFCIs = this.swmGlobal.getFCI();
            }else{
                semiFCIs = this.swmGroups.get(groupid).getFCI();
            }
        } catch (Exception ex) {
            Logger.getLogger(IncMine2.class.getName()).log(Level.SEVERE, null, ex);
            return;
        }
        
//        //iterate in size-descending order over the entire FCITable to remove unfrequent semiFCIs
//        for(Iterator<SemiFCI> iter =  fciTable.iterator(); iter.hasNext(); ) {
//            SemiFCI s = iter.next();
//            if(!s.isUpdated()) {
//                s.pushSupport(0);
//                s.setUpdated(false);
//                int k = computeK(s.getId(), 0, fciTable);
//                if (k == -1){
//                    fciTable.removeSemiFCI(s.getId(), iter);
//                }
//                else{
//                    SemiFCIid sfsId = fciTable.selectSFS(s, true);
//                    if(sfsId.isValid())
//                        fciTable.removeSemiFCI(s.getId(), iter);
//                }
//            }else{
//                s.setUpdated(false);
//            }
//
//        }  

        //for each FCI in the last segment in size ascending order
        for(SemiFCI fci: semiFCIs) {
            if(fciTable.size() > Configuration.MAX_SEMI_FCI_SET_COUNT){
                break;
            }
            if(this.getUpdateTime()/1e6 > Configuration.MAX_UPDATE_TIME){
                System.out.println("OUT OF TIME");
                return;
            }
            if(fci.getItems().size() == 1){
                continue;
            }
            SemiFCIid fciId = fciTable.select(fci.getItems());
            boolean newfci = false;
            
            if(fciId.isValid()) {
                //fci is already in the FCITable
                fciTable.getFCI(fciId).pushSupport(fci.currentSupport());
                computeK(fciId, 0, fciTable);                
            }else{
                //fci is not in the FCITable yet
                newfci = true;
                //set semiFCI support to support of his SFS (last segment excluded)
                SemiFCIid sfsId = fciTable.selectSFS(fci, false);
                
                if(sfsId.isValid()) {
                    int[] fciSupVector = fci.getSupports();
                    int[] sfsSupVector = fciTable.getFCI(sfsId).getSupports(); 
                    //note: the SFS has not been updated yet! so his old support goes from index 0 to length-2
                    if(fciSupVector.length > 1){
                        System.arraycopy(sfsSupVector, 0, fciSupVector, 1, 
                                fciSupVector.length - 2);
                        fci.setSupports(fciSupVector);
                    }
                }
                //add a new entry to the table and update the inverted index
                fciId = fciTable.addSemiFCI(fci);
                computeK(fciId, 0, fciTable);
            }
            
            if(fci.size() > 1){
                enumerateSubsets(new Subset(fci.getItems(),0,false),
                        new ArrayList<Integer>(), fci.getSupports(), fciId, 
                        newfci, fciTable);
            }    
               
        }
        
        fciTable.clearNewItemsetsTable();
        
        //iterate in size-descending order over the entire FCITable to remove unfrequent semiFCIs
        for(Iterator<SemiFCI> iter =  fciTable.iterator(); iter.hasNext(); ) {
            SemiFCI s = iter.next();
            if(!s.isUpdated()) {
                s.pushSupport(0);
                s.setUpdated(false);
                int k = computeK(s.getId(), 0, fciTable);
                if (k == -1){
                    fciTable.removeSemiFCI(s.getId(), iter);
                }
                else{
                    SemiFCIid sfsId = fciTable.selectSFS(s, true);
                    if(sfsId.isValid())
                        fciTable.removeSemiFCI(s.getId(), iter);
                }
            }else{
                s.setUpdated(false);
            }

        }       
                
        
        double ms = this.getUpdateTime()/1e6;
       
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
    private void enumerateSubsets(Subset origSubset, List<Integer> skipList, 
            int[] supersetSupportVector, SemiFCIid originalFCIid, boolean newFCI, 
            FCITable fciTable)
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
            SemiFCIid id = fciTable.select(newSubset.itemset);

            if(id.isValid()) {//subset in L
                SemiFCI subFCI = fciTable.getFCI(id); 
                if(subFCI.isUpdated()) {
                    if(removeIndex > 0) blackList.add(removeIndex-1); //its subsets don't have to be updated
                }else {
                    updateSubsetInL(id, supersetSupportVector, originalFCIid, fciTable);
                    subList.add(newSubset);
                    supersetSupportVector = subFCI.getSupports();
                    if(newFCI) newSubset.skipSubsetsNotInL = true;
                }
            }
            else {//subset not in L
                if(!newSubset.skipSubsetsNotInL)
                    subList.add(newSubset);
                if(newFCI)
                    updateSubsetNotInL(newSubset.itemset, originalFCIid, fciTable);
            }
        }
        
        for(Subset s:subList) {
            if(s.itemset.size() == 1) return;
            enumerateSubsets(s, blackList, supersetSupportVector, originalFCIid,
                             newFCI, fciTable);
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
    private void updateSubsetInL(SemiFCIid subsetId, int[] supersetSupportVector,
            SemiFCIid fciId, FCITable fciTable) {

        SemiFCI fci = fciTable.getFCI(fciId);

        fciTable.getFCI(subsetId).pushSupport(fci.currentSupport());
        
        int k = computeK(subsetId, 1, fciTable);

        if(k == -1){
            fciTable.removeSemiFCI(subsetId);
        }else{
            //checking closure property
            if(fciTable.getFCI(subsetId).getApproximateSupport(k) == Utilities.cumSum(supersetSupportVector, k))
                fciTable.removeSemiFCI(subsetId);
        }
    }
    
    /**
     * Updates a subset that isn't included in the actual semiFCIs set.
     * @param subset subset to be added
     * @param superFCIid original semiFCI
     */
    private void updateSubsetNotInL(List<Integer> subset, SemiFCIid superFCIid, 
                                    FCITable fciTable)
    {
        SemiFCI superFCI = fciTable.getFCI(superFCIid);
        SemiFCIid sfsId = fciTable.selectSFS(subset, superFCI.getItems());

        if(sfsId.isValid()) {
            int[] supportVector = fciTable.getFCI(sfsId).getSupports();
            supportVector[0] = superFCI.getSupports()[0];
            
            SemiFCI subFCI = new SemiFCI(subset, 0);
            subFCI.setSupports(supportVector);
            
            int k = subFCI.computeK(this.minsup,1);
            if(k > -1)
                fciTable.addSemiFCI(subFCI);
            
        }
    }
    
    /**
     * Calls computeK method the passed semiFCI and deletes it if no longer holds
     * the conditions to be maintained in the window
     * @param id id of the semiFCI
     * @param startK intial k value
     * @return value of k
     */
    public int computeK(SemiFCIid id, int startK, FCITable fciTable) {
        int k = fciTable.computeK(id, this.minsup, startK);
//        if(k == -1)
//            this.fciTable.removeSemiFCI(id);
        return k;
    }
    
    public int getNumFCIs(FCITable fciTable){
        return fciTable.size();
    }
    
    public long getUpdateTime(){
        this.endUpdateTime = TimingUtils.getNanoCPUTimeOfCurrentThread();
        return this.endUpdateTime - this.startUpadateTime;
    }
    
    public int getNAdded(FCITable fciTable){
        return fciTable.nAdded;
    }
    
    public int getNRemoved(FCITable fciTable){
        return fciTable.nRemoved;
    }
    
    public Iterator<SemiFCI> getApproximateFCIIterator(FCITable fciTable){
        return fciTable.iterator();
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
        return fciTableGlobal.toString();
    }

}
