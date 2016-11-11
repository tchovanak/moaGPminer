package moa.learners;

import moa.core.PPSDM.dto.RecommendationResults;
import moa.core.PPSDM.FciValue;
import moa.core.PPSDM.GroupCounter;
import moa.core.PPSDM.UserModelPPSDM;
import java.util.*;
import moa.MOAObject;
import moa.core.*;
import com.github.javacliparser.FloatOption;
import com.github.javacliparser.IntOption;
import com.github.javacliparser.FlagOption;
import com.yahoo.labs.samoa.instances.Prediction;
import com.yahoo.labs.samoa.instances.Instance;
import moa.core.InstanceExample;
import moa.clusterers.clustream.PPSDM.WithKmeansPPSDM;
import moa.cluster.Clustering;
import moa.cluster.Cluster;
import com.yahoo.labs.samoa.instances.SparseInstance;
import java.util.concurrent.ConcurrentHashMap;
import moa.cluster.PPSDM.SphereClusterPPSDM;
import moa.clusterers.denstream.WithDBSCAN;
import moa.clusterers.macro.NonConvexCluster;
import moa.core.PPSDM.Configuration;
import moa.core.PPSDM.FCITablePPSDM;
import moa.core.PPSDM.utils.MapUtil;
import moa.core.PPSDM.utils.UtilitiesPPSDM;

/*
    This class defines new AbstractLearner that performs clustering of users to groups and 
    mining of global and group frequent itemsets.
    
*/
public class PersonalizedPatternsMiner extends AbstractLearner implements Observer {

    private static final long serialVersionUID = 1L;
    private PersonalizedIncMine incMine;
    private WithKmeansPPSDM clusterer = new WithKmeansPPSDM();
    private Map<Integer,UserModelPPSDM> usermodels = new ConcurrentHashMap<>();
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
    private List<Integer> recsOnlyFromGlobal = new ArrayList<>();
    private List<Integer> recsOnlyFromGroup = new ArrayList<>();
    private List<Integer> recsCombined = new ArrayList<>();
    
    private Clustering kmeansClustering;
    private Clustering dbscanClustering;
    private Clustering cleanedKmeansClustering;
    private int clusteringId = 0;
    private int cntOnlyGroup;
    private int cntOnlyGlobal;
    private WithDBSCAN clustererDBSCAN;
    
     
    public PersonalizedPatternsMiner(){
        super();
        this.clusterer = new WithKmeansPPSDM();
        this.clusterer.kOption.setValue(numberOfGroupsOption.getValue());
        this.clusterer.maxNumKernelsOption.setValue(this.maxNumKernelsOption.getValue());
        this.clusterer.kernelRadiFactorOption.setValue(this.kernelRadiFactorOption.getValue());
    }

    public PersonalizedPatternsMiner(boolean grouping) {
        super();
        this.useGroupingOption.setValue(grouping);
        this.clusterer = new WithKmeansPPSDM();
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
        this.incMine = new PersonalizedIncMine(windowSizeOption.getValue(), maxItemsetLengthOption.getValue(),
                numberOfGroupsOption.getValue(), minSupportOption.getValue(),
                relaxationRateOption.getValue(),fixedSegmentLengthOption.getValue(), 
                groupFixedSegmentLengthOption.getValue());
        this.incMine.resetLearning();
        this.clusterer =  new WithKmeansPPSDM();
        this.clusterer.kOption.setValue(numberOfGroupsOption.getValue());
        this.clusterer.maxNumKernelsOption.setValue(100);
        this.clusterer.kernelRadiFactorOption.setValue(2);
        this.clusterer.resetLearning();
        this.clustererDBSCAN =  new WithDBSCAN();
        this.clustererDBSCAN.resetLearning();
        usermodels.clear();
        usermodels = new ConcurrentHashMap<>();
        System.gc(); // force garbage collection
    }
    
    private void performMacroclusteringClustream(){
        // B7: PERFORM MACROCLUSTERING
        Clustering results = clusterer.getMicroClusteringResult();
        if(results != null){
            AutoExpandVector<Cluster> clusters = results.getClustering();
            if(clusters.size() > 0){
                // save new clustering
                List<Clustering> clusterings;
                if(this.kmeansClustering == null){
                    clusterings =
                            clusterer.kMeans_rand(this.numberOfGroupsOption.getValue(),results);
                    this.kmeansClustering = clusterings.get(0);
                    this.cleanedKmeansClustering = clusterings.get(1);
                }else{
                    clusterings =
                            clusterer.kMeans_gta(this.numberOfGroupsOption.getValue(),results, cleanedKmeansClustering, kmeansClustering);
                    this.kmeansClustering = clusterings.get(0);
                    this.cleanedKmeansClustering = clusterings.get(1);
                }
                System.out.println("CLUSTERING-------------------------------------------------------" + 
                this.kmeansClustering.getClustering().size());
                this.clusteringId++;
            }
        }
    }
    
    
    private void performMacroclusteringDenstream(){
        this.dbscanClustering = clustererDBSCAN.getClusteringResult();
        System.out.println("CLUSTERING-------------------------------------------------------" + 
                this.dbscanClustering.getClustering().size());
        Configuration.GROUP_COUNTER = this.dbscanClustering.getClustering().size();
        this.clusteringId++;
        if(this.incMine.fciTablesGroups.size() < dbscanClustering.size()){
            for(int i = this.incMine.fciTablesGroups.size(); 
                    i < dbscanClustering.getClustering().size(); i++){
                this.incMine.addFciTable();
            }
        }
    }
    
    @Override
    public void trainOnInstance(Example e) {
        // B3: USER MODEL UPDATE
        Instance inst = (Instance) e.getData();
        if(useGroupingOption.isSet()){
            UserModelPPSDM um = updateUserModel(inst.copy());
            if(um.getNumOfNewSessions() > this.numMinNumberOfChangesInUserModel.getValue()){
                //Instance umInstance = um.getNewInstance(this.maxNumPages.getValue());
                // B4: NULLING USER MODEL CHANGES NUMBER
                Instance umInstance = um.getNewSparseInstance(this.maxNumPages.getValue());
                // B5: UPDATE MICROCLUSTERS AND INCREMENT MICROCLUSTERS CHANGES
                switch(Configuration.CLUSTERING_METHOD){
                    case CLUSTREAM:
                        clusterer.trainOnInstance(umInstance);
                        break;
                    case DENSTREAM:
                        clustererDBSCAN.trainOnInstance(umInstance);
                        break;
                }
                
                //clusterer.trainOnSparseInstance(umInstance);
                this.microclusteringUpdatesCounter++;
                if(this.microclusteringUpdatesCounter > this.numMinNumberOfMicroclustersUpdates.getValue()){
                    // perform macroclustering
                    // B6: NULLING MICROCLUSTERS UPDATES COUNTER
                    this.microclusteringUpdatesCounter = 0;
                    // B7: PERFORM MACROCLUSTERING
                    switch(Configuration.CLUSTERING_METHOD){
                        case CLUSTREAM:
                            performMacroclusteringClustream();
                            break;
                        case DENSTREAM:
                            performMacroclusteringDenstream();
                            break;
                    }
                    // B8 : CLEAR OLD USER MODELS
                    this.clearUserModels();
                }
            }
            switch(Configuration.CLUSTERING_METHOD){
                case CLUSTREAM:
                    updateGroupingInUserModelClustream(um);
                    break;
                case DENSTREAM:
                    updateGroupingInUserModelDenstream(um);
                    break;
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
                incMine.trainOnInstance(instEx.copy()); // first train on instance with groupid - group
            }
        }
        incMine.trainOnInstance(e.copy());   // then train on instance without groupid - global
    }
    
    
    private void updateGroupingInUserModelDenstream(UserModelPPSDM um) {
        if(this.clusteringId > um.getClusteringId()){
            // in um there is not set group for actual clustering so we need to set it now
            Instance umInstance = um.getInstance();   
            if(umInstance == null){
                return;
            }
            List<Cluster> clusters = null;
            if(dbscanClustering != null){  // if already clustering was performed
                clusters = this.dbscanClustering.getClustering();
            }
            for(Cluster c : clusters){
                NonConvexCluster cs = (NonConvexCluster) c; 
                double prob = cs.getInclusionProbability(umInstance);
                NonConvexCluster bestCluster;
                double minProb = 0.0;
                if(prob > 0.0 && prob > minProb){
                    bestCluster = cs;
                    minProb = prob;
                    um.setGroupid(bestCluster.getId());
                    um.setDistance(0);
                }
                um.setClusteringId(this.clusteringId);
            }
        }
    }
    
    
    private void updateGroupingInUserModelClustream(UserModelPPSDM um) {
        if(this.clusteringId > um.getClusteringId()){
            // in um there is not set group for actual clustering so we need to set it now
            Instance umInstance = um.getInstance();   
            if(umInstance == null){
                return;
            }
            List<Cluster> clusters = null;
            if(kmeansClustering != null){  // if already clustering was performed
                clusters = this.kmeansClustering.getClustering();
            }
            Cluster bestCluster;
            double minDist = Double.MAX_VALUE;
            for(Cluster c : clusters){
                SphereClusterPPSDM cs = (SphereClusterPPSDM) c; // kmeans produce sphere clusters
                //double dist = cs.getCenterDistance(umInstance)/cs.getRadius();
                double dist = cs.getCenterDistance(umInstance);
                if(dist < 1.0 && dist < minDist){
                    bestCluster = cs;
                    minDist = dist;
                    um.setGroupid(bestCluster.getId());
                    um.setDistance(dist);
                } 
            }
            um.setClusteringId(this.clusteringId);
        }
    }

    
    public RecommendationResults getRecommendationsForInstance(Example e) {
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

        if((evaluationWindowSize >= (sessionArray.size()-2))){ 
            return null; // this is when session array is too short - it is ignored.
        }
        
        for(int i = 2; i <= evaluationWindowSize + 1; i++){ // first item is groupid, 2nd uid
            window.add((int) Math.round(sessionArray.get(i)));
        }
        
        // maximum number of evaluated future items is the same as number of recommended items.
        for(int i = evaluationWindowSize + 2, j = 0; i < sessionArray.size(); i++){
            outOfWindow.add((int) Math.round(sessionArray.get(i)));
        }
        
        //to get all fcis found 
        List<FciValue> mapFciWeight = new LinkedList<>();
        List<FciValue> mapFciWeightGroup = new LinkedList<>();
        List<FciValue> mapFciWeightGlobal = new LinkedList<>(); 
        Iterator<FrequentItemset> itFis = this.incMine.fciTableGlobal.getSemiFcis().iterator();
        
        while(itFis.hasNext()){
            FrequentItemset fi = itFis.next();
            if(fi.getSize() > 1){
                List<Integer> items = fi.getItems();
                double hitsVal = this.computeSimilarity(items,window);
                if(hitsVal == 0.0){
                    continue;
                }
                FciValue fciVal = new FciValue();
                fciVal.setItems(fi.getItems());
                fciVal.computeValue(hitsVal, fi.getSupportDouble());
                mapFciWeight.add(fciVal);
                mapFciWeightGlobal.add(fciVal);
            }
        }
        boolean clusteringPerformed = false;
        switch(Configuration.CLUSTERING_METHOD){
            case CLUSTREAM:
                clusteringPerformed = (kmeansClustering != null);
                break;
            case DENSTREAM:
                clusteringPerformed = (dbscanClustering != null);
                break;
        }
        if(useGroupingOption.isSet() && clusteringPerformed){
            // if already clustering was performed
            Double groupid = -1.0;
            UserModelPPSDM um = getUserModelFromInstance(session);
            double distance = 0;
            if(um != null){
                distance = um.getDistance();
                groupid = um.getGroupid(); // groupids sorted by preference
            }else{
                sessionArray.set(0,-1.0);
            }            
            //         This next block performs the same with group fcis. 
            if(groupid != -1.0){
                Iterator<FrequentItemset> itFisG = 
                        this.incMine.fciTablesGroups.get((int) Math.round(groupid)).getSemiFcis().iterator();
                while(itFisG.hasNext()){
                    FrequentItemset fi = itFisG.next();
                    if(fi.getSize() > 1){
                        List<Integer> items = fi.getItems();
                        double hitsVal = this.computeSimilarity(items,window);
                        if(hitsVal == 0.0){
                            continue;
                        }
                        FciValue fciVal = new FciValue();
                        fciVal.setItems(fi.getItems());
                        fciVal.setDistance(distance);
                        fciVal.computeValue(hitsVal, fi.getSupportDouble());
                        mapFciWeight.add(fciVal);
                        mapFciWeightGroup.add(fciVal);
                    }
                }
            }
        }
        
        // all fcis found have to be sorted descending by its support and similarity.
        
        Collections.sort(mapFciWeight);
        Collections.sort(mapFciWeightGroup);
        Collections.sort(mapFciWeightGlobal);
        
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
        RecommendationResults results = new RecommendationResults();
        results.setTestWindow(outOfWindow);
        results.setNumOfRecommendedItems(this.numberOfRecommendedItemsOption.getValue());
        results.setRecommendationsGGC(recsCombined);
        results.setRecommendationsGO(recsOnlyFromGlobal);
        results.setRecommendationsOG(recsOnlyFromGroup);
        return results;
    }
    
    public List<FciValue> extractPatterns(){
        
        FCITablePPSDM fciTableGlobal = this.incMine.fciTableGlobal;
        List<FCITablePPSDM> fciTablesGroup = this.incMine.fciTablesGroups;
        List<FciValue> allPatterns = new ArrayList<>();
        
        fciTableGlobal.computeFcis(this.minSupportOption.getValue(),
                this.fixedSegmentLengthOption.getValue());
        Iterator<FrequentItemset> itGlob = fciTableGlobal.getFcis().iterator();
        while(itGlob.hasNext()){
            FrequentItemset sfci = itGlob.next();
            //double support = this.calculateSupport(sfci);
            //if(support >= this.minSupportOption.getValue()){
                FciValue fciVal = new FciValue();
                fciVal.setItems(sfci.getItems());
                fciVal.setSupport(sfci.getSupportDouble());
                fciVal.setGroupid(-1);
                allPatterns.add(fciVal);
            //}
        }
        int groupid = 0;
        for(FCITablePPSDM gTable: fciTablesGroup){
            gTable.computeFcis(this.minSupportOption.getValue(),
                this.fixedSegmentLengthOption.getValue());
            Iterator<FrequentItemset> itGr = gTable.getFcis().iterator();
            while(itGr.hasNext()){
                FrequentItemset sfci = itGr.next();
                //double support = this.calculateSupport(sfci);
                //if(support >= this.minSupportOption.getValue()){
                    FciValue fciVal = new FciValue();
                    fciVal.setItems(sfci.getItems());
                    //fciVal.setFci(sfci);
                    fciVal.setSupport(sfci.getSupportDouble());
                    fciVal.setGroupid(groupid);
                    allPatterns.add(fciVal);
                //}
            }
            groupid++;
        }
        return allPatterns;
    }
    
    
    private double calculateSupport(SemiFCI fci){
        double approxSupport = (double)fci.getApproximateSupport();
        double support = approxSupport/
                ((double)(this.fixedSegmentLengthOption.getValue())*
                (double)(this.windowSizeOption.getValue())); 
        return support;
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
    
    private UserModelPPSDM updateUserModel(Instance inst){
        int uid = (int)inst.value(1);
        if(usermodels.containsKey(uid)){
            UserModelPPSDM um = usermodels.get(uid);
            um.updateWithInstance(inst);
            return um;
        }else{
            UserModelPPSDM um = new UserModelPPSDM((int)inst.value(1), 
                    this.numMinNumberOfChangesInUserModel.getValue());
            um.updateWithInstance(inst);
            usermodels.put(uid, um);
            return um;
        }
    }
    
    private UserModelPPSDM getUserModelFromInstance(Instance inst) {
        int uid = (int)inst.value(1);
         if(usermodels.containsKey(uid)){
            UserModelPPSDM um = usermodels.get(uid);
            return um;
        }else{
            return null;
        }
    }
    
    private double computeSimilarity(List<Integer> items, List<Integer> window) {
        return ((double)UtilitiesPPSDM.computeLongestCommonSubset(items,window)) / 
                ((double)window.size());
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
        
        while(itGlobal.hasNext() || itGroup.hasNext()){
            if(itGlobal.hasNext()){
               FciValue fci = itGlobal.next();
               Iterator<Integer> itFciItems = fci.getItems().iterator();
               while(itFciItems.hasNext()){
                   Integer item = itFciItems.next();         
                   if(mapItemsVotes.containsKey(item)){   
                       Double newVal = mapItemsVotes.get(item) + fci.getLcsVal()*fci.getSupport();
                       mapItemsVotes.put(item, newVal);
                   }else{
                       if(!window.contains(item)){
                           Double newVal =  fci.getLcsVal()*fci.getSupport();
                           mapItemsVotes.put(item, newVal);
                       }
                   }
                   if(mapItemsVotesOnlyGlobal.containsKey(item)){
                        Double newVal = mapItemsVotesOnlyGlobal.get(item) + fci.getLcsVal()*fci.getSupport();
                        mapItemsVotesOnlyGlobal.put(item, newVal);
                   }else{
                        if(!window.contains(item)){
                            Double newVal =  fci.getLcsVal()*fci.getSupport();
                            mapItemsVotesOnlyGlobal.put(item, newVal);
                        }
                   }
               }
            }
            if(itGroup.hasNext()){
               FciValue fci = itGroup.next();
               Iterator<Integer> itFciItems = fci.getItems().iterator();
               while(itFciItems.hasNext()){
                   Integer item = itFciItems.next();
                    double dist = fci.getDistance();
                    if(dist == 0.0){
                        dist = 1.0;
                    }else{
                        if(dist < 0){
                            dist = -dist;
                        }
                        dist = 1.0-dist;
                    }
                   if(mapItemsVotes.containsKey(item)){
                       Double newVal = mapItemsVotes.get(item) + fci.getLcsVal()*fci.getSupport()*(dist);
                       mapItemsVotes.put(item, newVal);
                   }else{
                       if(!window.contains(item)){
                           Double newVal =  fci.getLcsVal()*fci.getSupport()*(dist);
                           mapItemsVotes.put(item, newVal);
                       }
                   }  
                   if(mapItemsVotesOnlyGroup.containsKey(item)){
                       Double newVal = mapItemsVotesOnlyGroup.get(item) + fci.getLcsVal()*fci.getSupport()*dist;
                       mapItemsVotesOnlyGroup.put(item, newVal);
                   }else{
                       if(!window.contains(item)){
                           Double newVal =  fci.getLcsVal()*fci.getSupport()*dist;
                           mapItemsVotesOnlyGroup.put(item, newVal);
                       }
                   }   
               }
            }
        }
        mapItemsVotes = MapUtil.sortByValue(mapItemsVotes);
        mapItemsVotesOnlyGlobal = MapUtil.sortByValue(mapItemsVotesOnlyGlobal);
        mapItemsVotesOnlyGroup = MapUtil.sortByValue(mapItemsVotesOnlyGroup);
        recsCombined = new ArrayList<>();
        recsOnlyFromGlobal = new ArrayList<>();
        recsOnlyFromGroup = new ArrayList<>();
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
        recsCombined = new ArrayList<>();
        recsOnlyFromGroup = new ArrayList<>();
        recsOnlyFromGlobal = new ArrayList<>();
        int numRecommendedItems = this.numberOfRecommendedItemsOption.getValue();
        
        for(FciValue fciVal : mapFciWeight) {
            SemiFCI key = fciVal.getFci();
            List<Integer> items = key.getItems();
            Iterator<Integer> itItems = items.iterator();
            while(itItems.hasNext()){
                Integer item =  itItems.next();
                if(!window.contains(item) && !recsCombined.contains(item)){  // create unique recommendations
                    recsCombined.add(item);
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

    /**
     * Removes old user models - if it has clustering id older than minimal user model updates
     */
    public void clearUserModels() {
        for(Map.Entry<Integer, UserModelPPSDM> entry : this.usermodels.entrySet()){
            UserModelPPSDM model = entry.getValue();
            if(this.clusteringId - model.getClusteringId() > Configuration.MAX_DIFFERENCE_OF_CLUSTERING_ID){
                // delete 
                this.usermodels.remove(entry.getKey());
            }
        }
    }

    @Override
    public double[] getVotesForInstance(Example e) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
