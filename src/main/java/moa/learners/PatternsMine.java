/*
 *    IncMine.java
 *    Copyright (C) 2012 Universitat PolitÃ¨cnica de Catalunya
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
import static java.lang.System.in;
import moa.core.InstanceExample;
import moa.clusterers.clustream.WithKmeans;
import moa.clusterers.clustream.Clustream;
import moa.clusterers.Clusterer;
import moa.cluster.Clustering;
import moa.cluster.Cluster;
import moa.cluster.SphereCluster;
import com.yahoo.labs.samoa.instances.SparseInstance;
import java.util.Map.Entry;
import moa.streams.filters.ReplacingMissingValuesFilter;
import moa.utils.LCS;
import moa.utils.MapUtil;

public class PatternsMine extends AbstractLearner implements Observer {

    private static final long serialVersionUID = 1L;
    
    private IncMine2 incMine;
    private WithKmeans clusterer = new WithKmeans();
    private Map<Integer,UserModel> usermodels = new HashMap<Integer, UserModel>();
    private int minNumberOfChangesInUserModel = 50;
    private int microclusteringUpdatesCounter = 0;
    
    public IntOption numMinNumberOfChangesInUserModel = new IntOption("minNumOfChangesInUserModel", 'c',
            "The minimal number of changes in user model to perform next actualization of clusters", 1, 1,
            Integer.MAX_VALUE);
   
    public IntOption numPages = new IntOption("numPages", 'p',
            "The number of pages in web page. Each number represents one page.", 1, 1,
            Integer.MAX_VALUE);
    
    public IntOption windowSizeOption = new IntOption(
            "windowSize", 'w',
            "Size of the sliding window (in number of segments).", 10);
    
    public IntOption maxItemsetLengthOption = new IntOption(
            "maxItemsetLength", 'm',
            "Maximum length of frequent closed itemset to be considered.", -1);
    
    public IntOption numberOfGroupsOption = new IntOption(
            "numberOfGroups", 'g',
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
    private Clustering kmeansClustering;
    private int evaluationWindowSize = 3;
    private int numberOfRecommendedItems = 10;
     
    public PatternsMine(){
        super();
        int windowSizeOption = this.windowSizeOption.getValue();
        int maxItemsetLengthOption = this.maxItemsetLengthOption.getValue();
        int numberOfGroupsOption = this.numberOfGroupsOption.getValue();
        double minSupportOption = this.minSupportOption.getValue();
        double relaxationRateOption = this.relaxationRateOption.getValue();
        int fixedSegmentLengthOption = this.fixedSegmentLengthOption.getValue();
        
        this.clusterer = new WithKmeans();
        this.clusterer.kOption.setValue(10);
        this.clusterer.maxNumKernelsOption.setValue(50);
        this.clusterer.kernelRadiFactorOption.setValue(2);
        
    }
    
    @Override
    public void resetLearningImpl() {
        System.out.println("restart learning");
        this.incMine = new IncMine2( windowSizeOption.getValue(), maxItemsetLengthOption.getValue(),
                numberOfGroupsOption.getValue(), minSupportOption.getValue(),
                relaxationRateOption.getValue(),fixedSegmentLengthOption.getValue());
        this.incMine.resetLearning();
        this.clusterer.resetLearning();
    }
    
    @Override
    public void trainOnInstance(Example e) {
        // first update user model with new data
        Instance inst = (Instance) e.getData();
        UserModel um = updateUserModel(inst);
        if(um.getNumberOfChanges() > minNumberOfChangesInUserModel){
            um.setNumberOfChanges(0);
            // perform clustering with user model 
            Instance umInstance = um.toInstance(numPages.getValue());
            clusterer.trainOnInstance(umInstance);
            if(this.microclusteringUpdatesCounter++ > 50){
                this.microclusteringUpdatesCounter = 0;
                Clustering results = clusterer.getMicroClusteringResult(); // append group to instance that it belongs to...
                if(results != null){
                    AutoExpandVector<Cluster> clusters = results.getClustering();
                    if(clusters.size() > 0){
                        this.kmeansClustering = Clustream.kMeans(
                                this.numberOfGroupsOption.getValue(),
                                clusters);
                    }
                }
            }
        }
        double groupid = um.getGroupid(); // now update instance with groupid
        if(groupid > -1){   
            int nItems = inst.numValues();
            double[] attValues = new double[nItems];
            int[] indices = new int[nItems];
            attValues[0] = (int)groupid; 
            indices[0]   = 0;
            for(int idx = 1; idx < nItems; idx++){
                attValues[idx] = inst.value(idx);
                indices[idx] =  inst.index(idx);
            }
            Instance instanceWithGroupid = new SparseInstance(1.0,attValues,indices,nItems);
            InstanceExample instEx = new InstanceExample(instanceWithGroupid);
            incMine.trainOnInstance(instEx); // first train on instance with groupid - group
        }
        incMine.trainOnInstance(e);   // then train on instance without groupid - global
    }

    @Override
    public double[] getVotesForInstance(Example e) {
        // append group to instance that it belongs to...
        Instance session = (Instance)e.getData();
        ArrayList<Double> sessionArray = new ArrayList<>();
        for(int i = 0; i < session.numValues(); i++){
            sessionArray.add(i,session.value(i));
        }
        if(kmeansClustering != null){
            Instance inst = getUserModelInstanceFromExampleInstance(session);
            if(inst != null){
                UserModel um = getUserModelFromInstance(session);
                Cluster bestCluster = null;
                //double maxDist = Double.MAX_VALUE;
                double maxProb = 0.0;
                for(Cluster c : this.kmeansClustering.getClustering()){
                    SphereCluster cs = (SphereCluster) c;
                    double prob = cs.getInclusionProbability(inst);
                    double dist = cs.getCenterDistance(inst);
                    double radius = cs.getRadius();
                    double distToRadius = dist - radius;
                    if(prob > maxProb){
                        bestCluster = cs;
                        //maxDist = distToRadius;
                        maxProb = prob;
                        um.setGroupid(bestCluster.getId());
                        sessionArray.set(0,um.getGroupid());
                    }
                }
            }
        }
        
        
        
        // get window from actual instance
        List<Integer> window = new ArrayList<Integer>(); // window of length
        List<Integer> outOfWindow = new ArrayList<Integer>(); // out of window 
        if(this.evaluationWindowSize >= (sessionArray.size()-1)){
            return null;
        }
        for(int i = 2; i <= this.evaluationWindowSize + 1; i++){ // first item is groupid, 2nd uid
            window.add((int) Math.round(sessionArray.get(i)));
        }
        for(int i = this.evaluationWindowSize + 2; i< sessionArray.size(); i++){
            outOfWindow.add((int) Math.round(sessionArray.get(i)));
        }
        List<Integer> recommendations = new ArrayList<Integer>();
        // how to get all fcis found ?
        double maxWeight = 0;
        FCITable fciGlobal = this.incMine.fciTableGlobal;
        
        Iterator<SemiFCI> it = fciGlobal.iterator();
        Map<SemiFCI, Double> mapFciWeight = new HashMap<SemiFCI,Double>();
        int index = 0;
        while(it.hasNext()){
            SemiFCI fci = it.next();
            if(fci.size() > 1){
                List<Integer> items = fci.getItems();
                double lcsVal = LCS.computeLongestCommonSubset(items,window) / ((double)window.size());
                double support = 
                        ((double)fci.getApproximateSupport())/((double)this.fixedSegmentLengthOption.getValue());
                double val = support*0.5 + lcsVal*0.5;
                mapFciWeight.put(fci, val);
                //System.out.println(fci.toString());
                //System.out.println(fci.getApproximateSupport());
            }
            index++;
        }
        
//        if(sessionArray.get(0) > -1){  // if group has some fci append it to list
//            List<FCITable> fciTables = this.incMine.fciTablesGroups;
//            FCITable fciGroup = fciTables.get((int) Math.round(sessionArray.get(0)));
//            Iterator<SemiFCI> itG = fciGroup.iterator();
//            while(itG.hasNext()){
//                SemiFCI fci = itG.next();
//                if(fci.size() > 1){
//                    List<Integer> items = fci.getItems();
//                    double lcsVal = LCS.computeLongestCommonSubset(items,window);
//                    double support = fci.getApproximateSupport()/this.fixedSegmentLengthOption.getValue();
//                    double val = support*0.5 + lcsVal*0.5;
//                    mapFciWeight.put(fci, val);
//                }
//                
//            }
//            index++;
//        }
        Map<SemiFCI, Double> sortedByValue = MapUtil.sortByValue(mapFciWeight);
        index = 0;
        List<Integer> recs = new ArrayList<Integer>(); 
        for(Map.Entry<SemiFCI, Double> entry : sortedByValue.entrySet()) {
            SemiFCI key = entry.getKey();
            index++;
            List<Integer> items = key.getItems();
            Iterator<Integer> itItems = items.iterator();
            while(itItems.hasNext()){
                Integer item =  itItems.next();
                if(!window.contains(item) && !recs.contains(item)){  //
                    recs.add(item);
                    if(recs.size() >= this.numberOfRecommendedItems){
                        break;
                    }
                }
            }
            if(recs.size() >= this.numberOfRecommendedItems){
                 break;
            }
        }
        
        double lcsVal = LCS.computeLongestCommonSubset(outOfWindow, recs);
        double[] lcsValues = new double[1];
        lcsValues[0] = lcsVal;
        return lcsValues;
        
//        // get fcis for group
//         
        //this.incMine.fciTablesGroups;
        // then perform LCS and find recommendend items finally
//        allFcis = global_FCIs + groupFCIs
        // LCS
       
    }
    
    @Override
    public void trainOnInstanceImpl(Instance inst) {
        System.out.println("trainOnInstanceImpl");
    }

    @Override
    protected Measurement[] getModelMeasurementsImpl() {
        System.out.println("getModelMeasurementsImpl");
        return null;
    }

    @Override
    public void getModelDescription(StringBuilder out, int indent) {
        System.out.println("");
    }

    @Override
    public boolean isRandomizable() {
         return false;
    }

   

    @Override
    public Learner[] getSublearners() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public MOAObject getModel() {
         return null;
    }

    @Override
    public Prediction getPredictionForInstance(Example e) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void update(Observable o, Object arg) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    private UserModel updateUserModel(Instance inst){
        System.out.println("updating user model");
        int uid = (int)inst.value(1);
        if(usermodels.containsKey(uid)){
            UserModel um = usermodels.get(uid);
            Map<Integer,Integer> pageVisitsMap = um.getPageVisitsMap();
            for(int i = 2; i < inst.numValues(); i++){ // from i = 2 because first val is groupid and second uid
                int idx = (int)inst.value(i);
                if(pageVisitsMap.containsKey(idx)){
                    int actVal = pageVisitsMap.get(idx);
                    um.put(idx,actVal + 1);
                }else{
                    um.put(idx, 1);
                }
            }
             System.out.println("user model updated");
            return um;
        }else{
            UserModel um = new UserModel();
            um.setGroupid((int)inst.value(0));
            um.setId((int)inst.value(1));
            Map<Integer,Integer> pageVisitsMap = um.getPageVisitsMap();
            for(int i = 2; i < inst.numValues(); i++){ // from i = 2 because first val is groupid and second uid
                int idx = (int)inst.value(i);
                if(pageVisitsMap.containsKey(idx)){
                    int actVal = pageVisitsMap.get(idx);
                    um.put(idx,actVal + 1);
                }else{
                    um.put(idx, 1);
                }
            }
            usermodels.put(uid, um);
            return um;
        }
       
    }
    
    private Instance getUserModelInstanceFromExampleInstance(Instance inst){
        int uid = (int)inst.value(1);
        if(usermodels.containsKey(uid)){
            UserModel um = usermodels.get(uid);
            return um.toInstance(numPages.getValue());
        }else{
            return null;
        }
        
    }

    private UserModel getUserModelFromInstance(Instance inst) {
        int uid = (int)inst.value(1);
         if(usermodels.containsKey(uid)){
            UserModel um = usermodels.get(uid);
            return um;
        }else{
            return null;
        }
    }
   

}
