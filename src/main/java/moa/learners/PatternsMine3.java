package moa.learners;

import java.util.*;
import moa.MOAObject;
import moa.core.*;
import com.github.javacliparser.FloatOption;
import com.github.javacliparser.IntOption;
import com.github.javacliparser.FlagOption;
import com.yahoo.labs.samoa.instances.Prediction;
import com.yahoo.labs.samoa.instances.Instance;
import moa.core.InstanceExample;
import moa.clusterers.clustream.WithKmeans;
import moa.clusterers.clustream.Clustream;
import moa.cluster.Clustering;
import moa.cluster.Cluster;
import moa.cluster.SphereCluster;
import com.yahoo.labs.samoa.instances.SparseInstance;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import moa.utils.Configuration;
import moa.utils.LCS;
import moa.utils.MapUtil;

/*
    This class defines new AbstractLearner that performs clustering of users to groups and 
    mining of global and group frequent itemsets.
    
*/
public class PatternsMine3 extends AbstractLearner implements Observer {

    private static final long serialVersionUID = 1L;
    
    private IncMine2 incMine;
    private WithKmeans clusterer = new WithKmeans();
    private Map<Integer,UserModel> usermodels = new ConcurrentHashMap<Integer, UserModel>();
    private int microclusteringUpdatesCounter = 0;
    
    public IntOption numMinNumberOfChangesInUserModel = new IntOption("minNumOfChangesInUserModel", 'c',
            "The minimal number of changes in user model to perform next actualization of clusters", 1, 1,
            Integer.MAX_VALUE);
    
    public IntOption numMinNumberOfMicroclustersUpdates = new IntOption("minNumOfMicroclustersUpdates", 'c',
            "The minimal number of microcluster updates to perform next kmeans of microclusters", 1, 1,
            Integer.MAX_VALUE);
   
    public IntOption maxNumPages = new IntOption("numPages", 'p',
            "The number of pages in web page. Each number represents one page.", 1, 1,
            Integer.MAX_VALUE);
    
    public IntOption maxNumUserModels = new IntOption("numUserModels", 'p',
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
    
    public IntOption groupFixedSegmentLengthOption = new IntOption(
            "groupFixedSegmentLength", 'l',
            "Group Fixed Segment Length.", 200);
    
    public IntOption numberOfRecommendedItemsOption = new IntOption(
            "numberOfRecommendedItems", 'n',
            "Number of item recommended.", 5);
    
    public IntOption evaluationWindowSizeOption = new IntOption(
            "evaluationWindowSize", 'e',
            "Size of evaluation window.", 3);
    
    public IntOption maxNumKernelsOption = new IntOption(
            "maxNumKernels", 't',
            "Maximum number of microkernels in clustream alghoritm.", 100);
    
    public IntOption kernelRadiFactorOption = new IntOption(
            "kernelRadiFactor", 'k',
            "Kernel radius factor number", 2);
    
    public FlagOption useGroupingOption = new FlagOption(
            "useGroupingFlag", 'f',
            "If flag is set grouping is used.");
    
    
    private int cntAll = 0;
    private List<Integer> recsGlobal = new ArrayList<Integer>();
    private List<Integer> recsGroup = new ArrayList<Integer>();
    private List<Integer> recsOnlyFromGlobal = new ArrayList<Integer>();
    private List<Integer> recsOnlyFromGroup = new ArrayList<Integer>();
    private List<Integer> recsCombined = new ArrayList<Integer>();
    
    private Clustering kmeansClustering;
    private int cntOnlyGroup;
    private int cntOnlyGlobal;
     
    public PatternsMine3(){
        super();
        this.clusterer = new WithKmeans();
        this.clusterer.kOption.setValue(numberOfGroupsOption.getValue());
        this.clusterer.maxNumKernelsOption.setValue(this.maxNumKernelsOption.getValue());
        this.clusterer.kernelRadiFactorOption.setValue(this.kernelRadiFactorOption.getValue());
    }

    public PatternsMine3(boolean grouping) {
        super();
        this.useGroupingOption.setValue(grouping);
        this.clusterer = new WithKmeans();
        this.clusterer.kOption.setValue(numberOfGroupsOption.getValue());
        this.clusterer.maxNumKernelsOption.setValue(this.maxNumKernelsOption.getValue());
        this.clusterer.kernelRadiFactorOption.setValue(this.kernelRadiFactorOption.getValue());
    }
    
    @Override
    public void resetLearningImpl() {
        GroupCounter.groupscounters = new int [numberOfGroupsOption.getValue() + 1];
        for(int i = 0; i < GroupCounter.groupscounters.length; i++){
            GroupCounter.groupscounters[i] = 0;
        }
        this.incMine = new IncMine2(windowSizeOption.getValue(), maxItemsetLengthOption.getValue(),
                numberOfGroupsOption.getValue(), minSupportOption.getValue(),
                relaxationRateOption.getValue(),fixedSegmentLengthOption.getValue(), 
                groupFixedSegmentLengthOption.getValue());
        this.incMine.resetLearning();
        this.clusterer =  new WithKmeans();
        this.clusterer.kOption.setValue(numberOfGroupsOption.getValue());
        this.clusterer.maxNumKernelsOption.setValue(100);
        this.clusterer.kernelRadiFactorOption.setValue(2);
        this.clusterer.resetLearning();
        usermodels.clear();
        usermodels = new ConcurrentHashMap<Integer, UserModel>();
        System.gc(); // force garbage collection
    }
    
    @Override
    public void trainOnInstance(Example e) {
        // first update user model with new data
        Instance inst = (Instance) e.getData();
        if(useGroupingOption.isSet()){
            UserModel um = updateUserModel(inst);
            if(um.getNumberOfChanges() > this.numMinNumberOfChangesInUserModel.getValue()){
                um.setNumberOfChanges(0);
                // perform clustering with user model 
                Instance umInstance = um.toInstance(maxNumPages.getValue());
                clusterer.trainOnInstance(umInstance);
                if(this.microclusteringUpdatesCounter++ > this.numMinNumberOfMicroclustersUpdates.getValue()){
                    this.microclusteringUpdatesCounter = 0;
                    Clustering results = clusterer.getMicroClusteringResult(); // append group to instance that it belongs to...
                    if(results != null){
                        AutoExpandVector<Cluster> clusters = results.getClustering();
                        if(clusters.size() > 0){
                            // save new clustering
                            this.kmeansClustering = Clustream.kMeans(
                                    this.numberOfGroupsOption.getValue(),
                                    clusters);
                            updateGroupidsInUserModels();
                        }
                    }
                }
                
            }
            double groupid = um.getGroupid(); // now update instance with groupid from user model
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
        }
        incMine.trainOnInstance(e);   // then train on instance without groupid - global
    }

    @Override
    public double[] getVotesForInstance(Example e) {
        // append group to instance that it belongs to...
        Instance session = (Instance)e.getData();
        // we need to copy instance data to sessionArray where we can modify them 
        // because data in Instance cannot be changed i dont know why...
        ArrayList<Double> sessionArray = new ArrayList<>(); 
        for(int i = 0; i < session.numValues(); i++){
            sessionArray.add(i,session.value(i)); 
        }
               
        int evaluationWindowSize = evaluationWindowSizeOption.getValue();
        // TESTING Instance - how it performs on recommendation.
        // get window from actual instance
        List<Integer> window = new ArrayList<>(); // items inside window 
        List<Integer> outOfWindow = new ArrayList<>(); // items out of window 
        if(evaluationWindowSize >= (sessionArray.size()-2)){ 
            return null; // this is when session array is too short - it is ignored.
        }
        for(int i = 2; i <= evaluationWindowSize + 1; i++){ // first item is groupid, 2nd uid
            window.add((int) Math.round(sessionArray.get(i)));
        }
        // maximum number of evaluated future items is the same as number of recommended items.
        for(int i = evaluationWindowSize + 2, j = 0; i < sessionArray.size(); i++){
            outOfWindow.add((int) Math.round(sessionArray.get(i)));
        }
        List<Integer> recommendations = new ArrayList<>();
        // how to get all fcis found ?
        Iterator<SemiFCI> it = this.incMine.fciTableGlobal.iterator();
        
        List<FciValue> mapFciWeight = new LinkedList<FciValue>();
        List<FciValue> mapFciWeightGroup = new LinkedList<FciValue>();
        List<FciValue> mapFciWeightGlobal = new LinkedList<FciValue>();
        
        while(it.hasNext()){
            SemiFCI fci = null;
            try {
                fci = (SemiFCI) it.next().clone();
            } catch (CloneNotSupportedException ex) {
                Logger.getLogger(PatternsMine3.class.getName()).log(Level.SEVERE, null, ex);
            }
            if(fci.size() > 1){
                List<Integer> items = fci.getItems();
                double hitsVal = this.computeSimilarity(items,window);
                if(hitsVal == 0.0){
                    continue;
                }
                double approximateSupport = (double)fci.getApproximateSupport();
                double support = 
                       approximateSupport/
                        ((double)(this.fixedSegmentLengthOption.getValue()))*((double)(this.windowSizeOption.getValue()));
                if(support < this.minSupportOption.getValue()){
                    continue;
                }
                FciValue fciVal = new FciValue();
                fciVal.setFci(fci);
                fciVal.computeValue(hitsVal, support, 0, minSupportOption.getValue());
                mapFciWeight.add(fciVal);
                mapFciWeightGlobal.add(fciVal);
            }
        }
        
        if(useGroupingOption.isSet() && kmeansClustering != null){
            // if already clustering was performed
            Double groupid = -1.0;
            UserModel um = getUserModelFromInstance(session);
            if(um != null){
                groupid = um.getGroupid(); // groupids sorted by preference
            }else{
                sessionArray.set(0,-1.0);
            }
            //         This next block performs the same with group fcis. 
            //         This can be commented out to test performance without group fcis.
//            int preference = 0;
//            for(double groupid : groupids){
            if(groupid != -1.0){
                Iterator<SemiFCI> itG  = this.incMine.fciTablesGroups.get((int) Math.round(groupid)).iterator();
                while(itG.hasNext()){
                    SemiFCI fci = null;
                    try {
                        fci = (SemiFCI) itG.next().clone();
                    } catch (CloneNotSupportedException ex) {
                        Logger.getLogger(PatternsMine3.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    if(fci.size() > 1){
                        List<Integer> items = fci.getItems();
                        double hitsVal = this.computeSimilarity(items,window);
                        if(hitsVal == 0.0){
                            continue;
                        }
                        double approximateSupport = (double)fci.getApproximateSupport();
                        double support = 
                               approximateSupport/
                                ((double)(this.fixedSegmentLengthOption.getValue()))*((double)(this.windowSizeOption.getValue()));
                        if(support < this.minSupportOption.getValue()){
                            continue;
                        }
                        FciValue fciVal = new FciValue();
                        fciVal.setGroupFciFlag(true);
                       
                            ///fciVal.setPreference(preference);
                        fciVal.setFci( fci);
                      
                        fciVal.computeValue(hitsVal, support, 0, minSupportOption.getValue());
                        mapFciWeight.add(fciVal);
                        mapFciWeightGroup.add(fciVal);
                    }
                }
            }
                
//            }
        }
        
        // another solution every time take best 2 patterns from group and from global
        // prefer items that are both in global and group pattern.
        
        // all fcis found have to be sorted descending by its support and similarity.
        //Map<SemiFCI, FciValue> sortedByValue = MapUtil.sortByValue(mapFciWeight);
        Collections.sort(mapFciWeight);
        Collections.sort(mapFciWeightGroup);
        Collections.sort(mapFciWeightGlobal);
        recsGlobal = new ArrayList<Integer>();
        recsGroup = new ArrayList<Integer>();
        recsCombined = new ArrayList<Integer>();
        switch (Configuration.RECOMMEND_STRATEGY) {
            case VOTES:
                generateRecsVoteStrategy(mapFciWeightGlobal,
                        mapFciWeightGroup, window);
                break;
            case FIRST_WINS:
                generateRecsFirstWinsStrategy(mapFciWeight, mapFciWeightGlobal,
                        mapFciWeightGroup, window);
                break;
        }
        
        
        double lcsValFromGlobal = LCS.computeLongestCommonSubset(outOfWindow, recsGlobal);
        double lcsValFromGroup = LCS.computeLongestCommonSubset(outOfWindow, recsGroup);
        double lcsValCombined = LCS.computeLongestCommonSubset(outOfWindow, recsCombined);
        double lcsValOnlyFromGlobal = LCS.computeLongestCommonSubset(outOfWindow, recsOnlyFromGlobal);
        double lcsValOnlyFromGroup = LCS.computeLongestCommonSubset(outOfWindow, recsOnlyFromGroup);
        // lcsVal contains number of items that are same in outOfWindow and recs
        double[] lcsValues = new double[8];
        lcsValues[0] = lcsValFromGlobal; // in future the window will slide over actual session. 
        lcsValues[1] = lcsValFromGroup;
        lcsValues[2] = lcsValCombined;
        lcsValues[3] = cntAll;   // Now only one set of recommended items is created
        lcsValues[4] = lcsValOnlyFromGlobal; // in future the window will slide over actual session. 
        lcsValues[5] = cntOnlyGlobal; // in future the window will slide over actual session.         lcsValues[1] = lcsValFromGroup; 
        lcsValues[6] = lcsValOnlyFromGroup; 
        lcsValues[7] = cntOnlyGroup;
        return lcsValues;
        
    }
    
    public void updateGroupidsInUserModels() {
        List<Cluster> clusters = null;
        if(kmeansClustering != null){  // if already clustering was performed
            clusters = this.kmeansClustering.getClustering();
        }
        Iterator it = this.usermodels.entrySet().iterator();
        while(it.hasNext()) {
            
            Map.Entry pair = (Map.Entry)it.next();
            UserModel um =  (UserModel) pair.getValue();
            um.clearGroupids();
            Instance inst = um.toInstance(maxNumPages.getValue()); 
            Cluster bestCluster = null;
            double minDist = 1.0;
            for(Cluster c : clusters){
                SphereCluster cs = (SphereCluster) c; // kmeans produce sphere clusters
                double prob = cs.getInclusionProbability(inst);
                double dist = cs.getCenterDistance(inst)/cs.getRadius();
                //um.addGroup(cs.getId(), dist);
                if(prob == 1.0 && dist < minDist){
                    bestCluster = cs;
                    minDist = dist;
                    um.setGroupid(bestCluster.getId());
                    um.setDistance(dist);
                }
            }
            um.aging();
            if(um.getPageVisitsMap().size() < 2){
                usermodels.remove((Integer) pair.getKey());
            }
        }
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
        if(useGroupingOption.isSet()){
            int uid = (int)inst.value(1);
            if(usermodels.containsKey(uid)){
                UserModel um = usermodels.get(uid);
                Map<Integer,Double> pageVisitsMap = um.getPageVisitsMap();
                for(int i = 2; i < inst.numValues(); i++){ // from i = 2 because first val is groupid and second uid
                    int idx = (int)inst.value(i);
                    if(pageVisitsMap.containsKey(idx)){
                        double actVal = pageVisitsMap.get(idx);
                        um.put(idx,actVal + 1);
                    }else{
                        um.put(idx, 1);
                    }
                }
                return um;
            }else{
                UserModel um = new UserModel();
                um.setGroupid(inst.value(0));
                um.setId((int)inst.value(1));
                Map<Integer,Double> pageVisitsMap = um.getPageVisitsMap();
                for(int i = 2; i < inst.numValues(); i++){ // from i = 2 because first val is groupid and second uid
                    int idx = (int)inst.value(i);
                    if(pageVisitsMap.containsKey(idx)){
                        double actVal = pageVisitsMap.get(idx);
                        um.put(idx,actVal + 1.0);
                    }else{
                        um.put(idx, 1.0);
                    }
                }
                usermodels.put(uid, um);
                return um;
            }
        }
        return null;
    }
    
//    private Instance getUserModelInstanceFromExampleInstance(Instance inst){
//        int uid = (int)inst.value(1);
//        if(usermodels.containsKey(uid)){
//            UserModel um = usermodels.get(uid);
//            return um.toInstance(numPages.getValue());
//        }else{
//            return null;
//        }
//    }

    private UserModel getUserModelFromInstance(Instance inst) {
        int uid = (int)inst.value(1);
         if(usermodels.containsKey(uid)){
            UserModel um = usermodels.get(uid);
            return um;
        }else{
            return null;
        }
    }
    
    /*
        Increases relaxation rate in case data mining is too slow
    */
    public boolean increaseRelaxationRate(double d) {
        double newValue = this.relaxationRateOption.getValue() + d;
        if(newValue > 1.0){
            return false;
        }
        this.relaxationRateOption.setValue(newValue);
        this.incMine.setRelaxationRate(d);
        return true;
        
    }

    private double computeSimilarity(List<Integer> items, List<Integer> window) {
        double hitsVal = 0.0;
        switch (Configuration.INTERSECT_STRATEGY) {
            case "LCS":
                hitsVal = ((double)LCS.computeLongestCommonSubset(items,window)) / ((double)window.size());
                break;
            case "INTERSECT":
                double len = window.size();
                window.retainAll(items);
                hitsVal = window.size() / len;
                break;
        }
        return hitsVal;
    }

    private void generateRecsVoteStrategy(
                                    List<FciValue> mapFciWeightGlobal,
                                    List<FciValue> mapFciWeightGroup, 
                                    List<Integer> window) {
        Map<Integer, Double> mapItemsVotes = new HashMap<>();
        Map<Integer, Double> mapItemsVotesOnlyGlobal = new HashMap<>();
        Map<Integer, Double> mapItemsVotesOnlyGroup = new HashMap<>();
        Iterator<FciValue> itGlobal = mapFciWeightGlobal.iterator();
        Iterator<FciValue> itGroup = mapFciWeightGroup.iterator();
//        int cntVotes = 0;
//        
        while(itGlobal.hasNext() || itGroup.hasNext()){
            if(itGlobal.hasNext()){
               FciValue fci = itGlobal.next();
               Iterator<Integer> itFciItems = fci.getFci().getItems().iterator();
               double distance = 1.0;
               int all = fci.getFci().size();
               while(itFciItems.hasNext()){
                   distance = distance - (distance/all);
                   Integer item = itFciItems.next();
                   if(mapItemsVotes.containsKey(item)){
                       Double newVal = mapItemsVotes.get(item) + fci.getLcsVal()*fci.getSupport();
                       mapItemsVotes.put(item, newVal);
                       mapItemsVotesOnlyGlobal.put(item, newVal);
                   }else{
                       if(!window.contains(item)){
                           Double newVal =  fci.getLcsVal()*fci.getSupport();
                           mapItemsVotes.put(item, newVal);
                           mapItemsVotesOnlyGlobal.put(item, newVal);
//                           cntVotes++;
//                           if(cntVotes > Configuration.MAX_VOTES){
//                                break;
//                           }
                       }
                       
                   }
                   
               }
            }
            if(itGroup.hasNext()){
               FciValue fci = itGroup.next();
               Iterator<Integer> itFciItems = fci.getFci().getItems().iterator();
               while(itFciItems.hasNext()){
                   Integer item = itFciItems.next();
                   if(mapItemsVotes.containsKey(item)){
                       Double newVal = mapItemsVotes.get(item) + fci.getLcsVal()*fci.getSupport();
                       mapItemsVotes.put(item, newVal);
                       mapItemsVotesOnlyGroup.put(item, newVal);
                   }else{
                       if(!window.contains(item)){
                           Double newVal =  fci.getLcsVal()*fci.getSupport();
                           mapItemsVotes.put(item, newVal);
                           mapItemsVotesOnlyGroup.put(item, newVal);
//                           cntVotes++;
//                           if(cntVotes > Configuration.MAX_VOTES){
//                                break;
//                           }
                       }
                   }    
               }
            }
//            if(cntVotes > Configuration.MAX_VOTES){
//                break;
//            }
        }
        mapItemsVotes = MapUtil.sortByValue(mapItemsVotes);
        mapItemsVotesOnlyGlobal = MapUtil.sortByValue(mapItemsVotesOnlyGlobal);
        mapItemsVotesOnlyGroup = MapUtil.sortByValue(mapItemsVotesOnlyGroup);
        recsCombined = new ArrayList<Integer>();
        recsGlobal = new ArrayList<Integer>();
        recsGroup = new ArrayList<Integer>();
        recsOnlyFromGlobal = new ArrayList<Integer>();
        recsOnlyFromGroup = new ArrayList<Integer>();
        recsGroup = new ArrayList<Integer>();
        int numRecommendedItems = this.numberOfRecommendedItemsOption.getValue();
        cntAll = 0;
        cntOnlyGlobal = 0;
        cntOnlyGroup = 0;
        for(Map.Entry<Integer,Double> e : mapItemsVotes.entrySet()) {
            Integer item = e.getKey();
            recsCombined.add(item);
            cntAll++;
            if(cntAll >= numRecommendedItems){
                break;
            }       
        }
        for(Map.Entry<Integer,Double> e : mapItemsVotesOnlyGlobal.entrySet()) {
            Integer item = e.getKey();
            recsOnlyFromGlobal.add(item);
            cntOnlyGlobal++;
            if(cntOnlyGlobal >= numRecommendedItems){
                break;
            } 
        }
        for(Map.Entry<Integer,Double> e : mapItemsVotesOnlyGroup.entrySet()) {
            Integer item = e.getKey();
            recsOnlyFromGroup.add(item); 
            cntOnlyGroup++;
            if(cntOnlyGroup >= numRecommendedItems){
                break;
            } 
        }
        
    }

    private void generateRecsFirstWinsStrategy(List<FciValue> mapFciWeight,
                                    List<FciValue> mapFciWeightGlobal, 
                                    List<FciValue> mapFciWeightGroup, 
                                    List<Integer> window) {
        cntAll = 0;
        cntOnlyGroup = 0; 
        cntOnlyGlobal = 0;
        recsGroup = new ArrayList<>();
        recsGlobal = new ArrayList<>();
        int numRecommendedItems = this.numberOfRecommendedItemsOption.getValue();
        
        for(FciValue fciVal : mapFciWeight) {
            SemiFCI key = fciVal.getFci();
            List<Integer> items = key.getItems();
            Iterator<Integer> itItems = items.iterator();
            while(itItems.hasNext()){
                Integer item =  itItems.next();
                if(!window.contains(item) && !recsGroup.contains(item) 
                        && !recsGlobal.contains(item)){  // create unique recommendations
                    if(fciVal.getGroupFciFlag()){
                        recsGroup.add(item);
                    }else{
                        recsGlobal.add(item);
                    }
                    cntAll++;
                    if(cntAll >= numRecommendedItems){
                        break;
                    }
                }
            }
            if(cntAll >=  numRecommendedItems){
                 break;
            }
        }
        
        recsOnlyFromGroup = new ArrayList<>(); 
        for(FciValue fciVal : mapFciWeightGroup) {
            SemiFCI key = fciVal.getFci();
            List<Integer> items = key.getItems();
            Iterator<Integer> itItems = items.iterator();
            while(itItems.hasNext()){
                Integer item =  itItems.next();
                if(!window.contains(item) && !recsOnlyFromGroup.contains(item)){  // create unique recommendations
                    recsOnlyFromGroup.add(item);
                    cntOnlyGroup++;
                    if(cntOnlyGroup >=  numRecommendedItems){
                        break;
                    }
                }
            }
            if(cntOnlyGroup >=  numRecommendedItems){
                 break;
            }
        }
        
        recsOnlyFromGlobal = new ArrayList<>(); 
        for(FciValue fciVal : mapFciWeightGlobal) {
            SemiFCI key = fciVal.getFci();
            List<Integer> items = key.getItems();
            Iterator<Integer> itItems = items.iterator();
            while(itItems.hasNext()){
                Integer item =  itItems.next();
                if(!window.contains(item) && !recsOnlyFromGlobal.contains(item)){  // create unique recommendations
                    recsOnlyFromGlobal.add(item);
                    cntOnlyGlobal++;
                    if(cntOnlyGlobal >=  numRecommendedItems){
                        break;
                    }
                }
            }
            if(cntOnlyGlobal >=  numRecommendedItems){
                 break;
            }
        }
    }
   

}
