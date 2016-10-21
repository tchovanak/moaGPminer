/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package moa.tasks;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import moa.MOAObject;
import moa.core.Example;
import moa.core.ObjectRepository;
import moa.core.SemiFCI;
import moa.core.TimingUtils;
import moa.evaluation.PatternsRecommendationEvaluator;
import moa.learners.FciValue;
import moa.learners.PatternsMine3;
import moa.streams.SessionsFileStream;
import moa.utils.Configuration;
import moa.utils.RecommendStrategiesEnum;
import moa.utils.SortStrategiesEnum;
import moa.learners.RecommendationResults;
import moa.learners.SummaryResults;

/**
 *
 * @author Tomas
 */
public class GPLearnEvaluateTask implements Task {
    
    private int id;
    private int fromid;
    private boolean fasterWithoutGrouping;
    private boolean faster;
    private List<Parameter> params;
    private boolean grouping;
    private FileWriter writer;
    private SessionsFileStream stream ;
    private String pathToStream ;
    private String pathToSummaryOutputFile;
    private String pathToOutputFile;
    private double changed;
    private double[] lastparams;

    
    public GPLearnEvaluateTask(int id, int fromid, List<Parameter> params, 
            double[] lastparams,
            String pathToStream, String pathToSummaryOutputFile, 
            String pathToOutputFile,
            boolean grouping, boolean faster,  boolean fasterWithoutGrouping
            ) {
        this.id = id;
        this.fromid = fromid;
        this.fasterWithoutGrouping = fasterWithoutGrouping;
        this.faster = faster;
        this.params = params;
        this.grouping = grouping;
        this.pathToStream = pathToStream;
        this.pathToSummaryOutputFile = pathToSummaryOutputFile;
        this.pathToOutputFile = pathToOutputFile;
        this.lastparams = lastparams;
    }

    @Override
    public Class<?> getTaskResultType() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Object doTask() {
        // fromid is used to allow user restart evaluation from different point 
        id++; // id is always incremented
        if(fromid >= id){
            return null;
        }
        findChangeInParams(params);
        /*
            if there was raised flag in previous evaluation that parameter configuration
            is too slow, then this condition is used to check if any of desired 
            parameters that are possibly speeding up alghoritm were changed, if so 
            then new configuration is accepted as possibly faster, thus tried.
        */
        if(this.fasterWithoutGrouping){ 
             if(checkLastParamsForFasterWithoutGroupingConfiguration(params)){ // params werent changed to better
                fasterWithoutGrouping = true;
                return null;
            }else{
                fasterWithoutGrouping = false;
            }
        }
        if(faster){
            if(checkLastParamsForFasterConfiguration(params)){ // params werent changed to better
                faster = true;
                return null;
            }else{
                faster = false;
            }
        }
        boolean repeatWithGrouping = false;
        // some of not grouping params was changed so try with and without grouping, 
        // if other params are changed only try with grouping
        if(checkLastParamsForNotGroupingChange(params)){ 
            repeatWithGrouping = true;
            grouping = false;
        }else{
            grouping = true;
        }
        
        // initialize and configure learner
        PatternsMine3 learner = new PatternsMine3();
        for(int i = 0; i < 2; i++){
            this.grouping = true;
            configureLearnerWithParams(learner, params);
            double originalUPDATETIME = 
                    (learner.fixedSegmentLengthOption.getValue() 
                    * (learner.minSupportOption.getValue()) * 100 ) / Configuration.SPEED_PARAM;
            Configuration.MAX_UPDATE_TIME = originalUPDATETIME;
            this.stream = new SessionsFileStream(this.pathToStream);
            writeConfigurationToFile(this.pathToSummaryOutputFile, learner);
            learner.useGroupingOption.setValue(grouping); // change grouping option in learner
            learner.resetLearning();
            stream.prepareForUse();
            TimingUtils.enablePreciseTiming();
            PatternsRecommendationEvaluator evaluator = 
                    new PatternsRecommendationEvaluator(
                            this.pathToOutputFile + "results_G" + grouping + 
                                    "_id_" + id + ".csv");
            int counter = 0;
            long start = TimingUtils.getNanoCPUTimeOfCurrentThread();
            double transsec = 0.0;
            int windowSize = learner.evaluationWindowSizeOption.getValue();
            updateLastParams(learner);
            while (stream.hasMoreInstances()) {
                counter++;
                if(counter == Configuration.EXTRACT_PATTERNS_AT){
                    // NOW EXTRACT PATTERNS TO FILE
                    extractPatternsToFile(learner.extractPatterns(), this.pathToOutputFile + "patterns_" + id + ".csv");
                }
                Example trainInst = stream.nextInstance();
                /// FOR TESTING LONG TIME SURVIVING OF APPLICATION
//                if(!stream.hasMoreInstances()){
//                    stream.restart();
//                }
                Example testInst = (Example) trainInst.copy();
                if(counter > Configuration.START_EVALUATING_FROM){
                    RecommendationResults results = learner.getRecommendationsForInstance(testInst);
                    if(results != null)
                        evaluator.addResult(results, windowSize, transsec, counter); // evaluator will evaluate recommendations and update metrics with given results     
                }
                
                learner.trainOnInstance(trainInst); // this will start training proces - it means first update clustering and then find frequent patterns
                long end = TimingUtils.getNanoCPUTimeOfCurrentThread();
                //long end = System.nanoTime();
                double tp =((double)(end - start) / 1e9);
                transsec = counter/tp;
                // SPEED CONTROL PART
                //if(transsec < Configuration.SPEED_PARAM * 10){
                    //Configuration.MAX_FCI_SET_COUNT = originalFCISETCOUNT * (transsec/((Configuration.DESIRED_TRANSSEC)*2));
                if(counter > 2 * learner.fixedSegmentLengthOption.getValue()){
                    double diff = ((transsec - Configuration.MIN_TRANSSEC) > 0)? (transsec - Configuration.MIN_TRANSSEC) : (Configuration.MIN_TRANSSEC - transsec) ;                     

                    Configuration.MAX_UPDATE_TIME = (originalUPDATETIME * Math.log(diff)) / 
                            Configuration.SPEED_PARAM;
                    //System.out.println(Configuration.MAX_UPDATE_TIME);
                }
                //}
                
                if(counter % learner.fixedSegmentLengthOption.getValue() == 0){
                    System.out.println(counter); // to see progress
                    if(transsec < Configuration.MIN_TRANSSEC/2){
                        if(!grouping){
                            fasterWithoutGrouping = true;
                        }else{
                            faster = true;
                        }
                        updateLastParams(learner);
                        return null;
                    }   
                }
            }
            long end = TimingUtils.getNanoCPUTimeOfCurrentThread();
            double tp =((double)(end - start) / 1e9);
            transsec = counter/tp;
            SummaryResults results = evaluator.getResults();
            writeResultsToFile(results, transsec, tp, counter);
            
            
            
            if(!repeatWithGrouping){
                break;
            }else if(i == 0){
                id++;
                grouping = true;
            }
        }
        System.gc();
        return null;
    }
    
    private void extractPatternsToFile(List<FciValue> allPatterns, String pathToFile) {
        try {
            FileWriter pwriter = new FileWriter(pathToFile, true);
            pwriter.append("GROUPID");pwriter.append(',');
            pwriter.append("SUPPORT");pwriter.append(',');
            pwriter.append("ITEMS");pwriter.append(',');
            pwriter.append('\n');
            for(FciValue fci : allPatterns){
                pwriter.append(((Integer)fci.getGroupid()).toString());pwriter.append(','); 
                pwriter.append(((Double)fci.getSupport()).toString());pwriter.append(','); 
                pwriter.append((fci.getFci().getItems()).toString().replaceAll(","," "));pwriter.append(','); 
                pwriter.append('\n');
            }
            pwriter.close();
        } catch (IOException ex) {
            Logger.getLogger(GPLearnEvaluateTask.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
     private boolean checkLastParamsForNotGroupingChange(List<Parameter> params){
        return changed >= 0 && changed < 15; // params were changed so try with and without grouping, if other params are changed only try with grouping
        
    }
    
    private boolean checkLastParamsForFasterWithoutGroupingConfiguration(List<Parameter> params){
        if(lastparams[4] >= params.get(4).getValue()  //minSupport   
            && lastparams[5] >= params.get(5).getValue() //relaxationRate
            && lastparams[6] >= params.get(6).getValue() //fixedSegmentLength
        )
        { // params werent changed to better
            return true;
        }else{
            return false;
        }
    }
    
    private boolean checkLastParamsForFasterConfiguration(List<Parameter> params){
        if(
            lastparams[4] >= params.get(4).getValue()  //minSupport
            && lastparams[5] >= params.get(5).getValue() //relaxationRate
            && lastparams[6] >= params.get(6).getValue() //fixedSegmentLength
            && lastparams[15] >= params.get(15).getValue() //minNumOfChangesInUserModel
            && lastparams[16] >= params.get(16).getValue()) //minNumOfChangesInMicrocluster
        { // params werent changed to better
            return true;
        }else{
            return false;
        }
    }
    
    private void updateLastParams(PatternsMine3 learner) {
        
        this.lastparams[0] = Configuration.RECOMMEND_STRATEGY.value();
        this.lastparams[1] = Configuration.SORT_STRATEGY.value();
        this.lastparams[2] = learner.evaluationWindowSizeOption.getValue();
        this.lastparams[3] = learner.numberOfRecommendedItemsOption.getValue();
        
        this.lastparams[4] = learner.minSupportOption.getValue();
        this.lastparams[5] = learner.relaxationRateOption.getValue();
        this.lastparams[6] = learner.fixedSegmentLengthOption.getValue();
        this.lastparams[7] = learner.groupFixedSegmentLengthOption.getValue();
        this.lastparams[8] = learner.maxItemsetLengthOption.getValue();
        this.lastparams[9] = learner.windowSizeOption.getValue();
        
        this.lastparams[10] = learner.maxNumPages.getValue();
        this.lastparams[11] = learner.maxNumUserModels.getValue();
        this.lastparams[12] = (Double)Configuration.MAX_FCI_SET_COUNT;
        this.lastparams[13] = (Double)Configuration.SPEED_PARAM;
        this.lastparams[14] = (Double)Configuration.MIN_TRANSSEC;
        
        this.lastparams[15] = learner.numMinNumberOfChangesInUserModel.getValue();
        this.lastparams[16] = learner.numMinNumberOfMicroclustersUpdates.getValue();
        this.lastparams[17] = learner.numberOfGroupsOption.getValue();
        this.lastparams[18] = learner.maxNumKernelsOption.getValue();
        this.lastparams[19] = learner.kernelRadiFactorOption.getValue();
    }
    
    private void configureLearnerWithParams(PatternsMine3 learner, List<Parameter> params){
        // RECOMMEND PARAMETERS
        Configuration.RECOMMEND_STRATEGY = RecommendStrategiesEnum.valueOf((int) params.get(0).getValue());
        Configuration.SORT_STRATEGY = SortStrategiesEnum.valueOf((int) params.get(1).getValue());
        learner.evaluationWindowSizeOption.setValue((int) params.get(2).getValue());
        learner.numberOfRecommendedItemsOption.setValue((int) params.get(3).getValue());
        //FPM PARAMETERS
        learner.minSupportOption.setValue(params.get(4).getValue());
        learner.relaxationRateOption.setValue(params.get(5).getValue());
        learner.fixedSegmentLengthOption.setValue((int) (params.get(6).getValue()));
        
        learner.maxItemsetLengthOption.setValue((int) params.get(8).getValue());
        learner.windowSizeOption.setValue((int) params.get(9).getValue());
        // RESTRICTIONS PARAMETERS
        learner.maxNumPages.setValue((int) params.get(10).getValue());
        learner.maxNumUserModels.setValue((int) params.get(11).getValue());
        Configuration.MAX_FCI_SET_COUNT = params.get(12).getValue();
        Configuration.SPEED_PARAM = params.get(13).getValue();
        Configuration.MIN_TRANSSEC = params.get(14).getValue();
        // CLUSTERING PARAMETERS
        learner.numMinNumberOfChangesInUserModel.setValue((int) params.get(15).getValue());
        learner.numMinNumberOfMicroclustersUpdates.setValue((int) params.get(16).getValue());
        learner.numberOfGroupsOption.setValue((int) params.get(17).getValue());
        learner.maxNumKernelsOption.setValue((int) params.get(18).getValue());
        learner.kernelRadiFactorOption.setValue((int) params.get(19).getValue());
        Configuration.START_EVALUATING_FROM = (int) params.get(20).getValue();
        
        double gfsl = params.get(7).getValue();
        if(gfsl > 0){
            learner.groupFixedSegmentLengthOption.setValue((int) params.get(7).getValue());
        }else if(gfsl == 0){
            learner.groupFixedSegmentLengthOption.setValue(
                (int)(((double)learner.fixedSegmentLengthOption.getValue())/
                        ((double)learner.numberOfGroupsOption.getValue())));
        }else{
            learner.groupFixedSegmentLengthOption.setValue(learner.fixedSegmentLengthOption.getValue());
        }
        
    }
    
    private void writeConfigurationToFile(String path, PatternsMine3 learner){
        try {
            this.writer = new FileWriter(this.pathToSummaryOutputFile, true);
            writer.append(((Integer)id).toString());writer.append(',');
            writer.append(((Boolean)this.grouping).toString());writer.append(',');
            // RECOMMEND PARAMETERS
            writer.append(Configuration.RECOMMEND_STRATEGY.toString());writer.append(',');
            writer.append(Configuration.SORT_STRATEGY.toString());writer.append(',');
            writer.append(((Double)(double)learner.evaluationWindowSizeOption.getValue()).toString());writer.append(',');
            writer.append(((Double)(double)learner.numberOfRecommendedItemsOption.getValue()).toString());writer.append(',');
            // INCMINE PARAMETERS
            writer.append(((Double)learner.minSupportOption.getValue()).toString());writer.append(',');
            writer.append(((Double)learner.relaxationRateOption.getValue()).toString());writer.append(',');
            writer.append(((Double)(double)learner.fixedSegmentLengthOption.getValue()).toString());writer.append(',');
            writer.append(((Double)(double)learner.groupFixedSegmentLengthOption.getValue()).toString());writer.append(',');
            writer.append(((Double)(double)learner.maxItemsetLengthOption.getValue()).toString());writer.append(',');
            writer.append(((Double)(double)learner.windowSizeOption.getValue()).toString());writer.append(',');
            
            //UNIVERSAL PARAMETERS
            writer.append(((Double)(double)learner.maxNumPages.getValue()).toString());writer.append(',');
            writer.append(((Double)(double)learner.maxNumUserModels.getValue()).toString());writer.append(',');
            writer.append(((Double)Configuration.MAX_FCI_SET_COUNT).toString());writer.append(',');
            writer.append(((Double)Configuration.SPEED_PARAM).toString());writer.append(',');
            writer.append(((Double)Configuration.MIN_TRANSSEC).toString());writer.append(',');
            writer.append(((Double)Configuration.MAX_UPDATE_TIME).toString());writer.append(',');
            writer.append(((Integer)Configuration.START_EVALUATING_FROM).toString());writer.append(',');
            // CLUSTERING PARAMETERS
            if(this.grouping){
                writer.append(((Double)(double) learner.numMinNumberOfChangesInUserModel.getValue()).toString());writer.append(',');
                writer.append(((Double)(double) learner.numMinNumberOfMicroclustersUpdates.getValue()).toString());writer.append(',');
                writer.append(((Double)(double) learner.numberOfGroupsOption.getValue()).toString());writer.append(',');
                writer.append(((Double)(double) learner.maxNumKernelsOption.getValue()).toString());writer.append(',');
                writer.append(((Double)(double) learner.kernelRadiFactorOption.getValue()).toString());writer.append(',');
            }else{
                writer.append("NG");writer.append(',');
                writer.append("NG");writer.append(',');
                writer.append("NG");writer.append(',');
                writer.append("NG");writer.append(',');
                writer.append("NG");writer.append(',');
            }
            writer.close();
            writer = null;
         } catch (IOException ex) {
             Logger.getLogger(GridSearchEvaluator.class.getName()).log(Level.SEVERE, null, ex);
         }
    }

    private void writeResultsToFile(SummaryResults results, double transsec, double tp, int counter){
        try{
            this.writer = new FileWriter(this.pathToSummaryOutputFile, true);
            
            writer.append(results.getAllHitsGGC().toString());writer.append(',');
            writer.append(results.getRealRecommendedGGC().toString());writer.append(',');
            writer.append(results.getPrecisionGGC().toString());writer.append(',');
            writer.append(results.getRecallGGC().toString());writer.append(',');
            writer.append(results.getF1GGC().toString());writer.append(',');
            writer.append(results.getNdcgGGC().toString());writer.append(',');
            
            writer.append(results.getAllHitsGO().toString());writer.append(',');
            writer.append(results.getRealRecommendedGO().toString());writer.append(',');
            writer.append(results.getPrecisionGO().toString());writer.append(',');
            writer.append(results.getRecallGO().toString());writer.append(',');
            writer.append(results.getF1GO().toString());writer.append(',');
            writer.append(results.getNdcgGO().toString());writer.append(',');
            
            writer.append(results.getAllHitsOG().toString());writer.append(',');
            writer.append(results.getRealRecommendedOG().toString());writer.append(',');
            writer.append(results.getPrecisionOG().toString());writer.append(',');
            writer.append(results.getRecallOG().toString());writer.append(',');
            writer.append(results.getF1OG().toString());writer.append(',');
            writer.append(results.getNdcgOG().toString());writer.append(',');
            
            writer.append(results.getAllTestedItems().toString());writer.append(',');
            writer.append(results.getAllTestedTransactions().toString());writer.append(',');
            writer.append(results.getMaxRecommendedItems().toString());writer.append(',');
            writer.append(((Double)tp).toString());writer.append(',');
            writer.append(((Double)transsec).toString());writer.append(',');
            writer.append(((Integer)counter).toString());writer.append('\n');
            writer.close();
        } catch (IOException ex) {
            Logger.getLogger(GridSearchEvaluator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void findChangeInParams(List<Parameter> params){
        changed = -1;
        for(int i = 0; i < this.lastparams.length; i++){
            if(i == 7){ // ignore group segment length
                continue;
            }
            if(this.lastparams[i] != params.get(i).getValue()){changed = i; break;}
        }
    }

    @Override
    public Object doTask(TaskMonitor tm, ObjectRepository or) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int measureByteSize() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    @Override
    public MOAObject copy() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void getDescription(StringBuilder sb, int i) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public int getId() {
        return id;
    }

    public boolean isFasterWithoutGrouping() {
        return fasterWithoutGrouping;
    }

    public boolean isFaster() {
        return faster;
    }

    public boolean isGrouping() {
        return grouping;
    }

    public double[] getLastparams() {
        return lastparams;
    }

    
    
}
