/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package moa.tasks;

import moa.core.PPSDM.enums.SortStrategiesEnum;
import moa.core.PPSDM.enums.RecommendStrategiesEnum;
import moa.core.PPSDM.Configuration;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import moa.MOAObject;
import moa.core.Example;
import moa.core.ObjectRepository;
import moa.core.TimingUtils;
import moa.evaluation.PPSDMRecommendationEvaluator;
import moa.core.PPSDM.FciValue;
import moa.learners.PersonalizedPatternsMiner;
import moa.streams.SessionsFileStream;
import moa.core.PPSDM.dto.RecommendationResults;
import moa.core.PPSDM.dto.SummaryResults;
import moa.core.PPSDM.utils.UtilitiesPPSDM;

/**
 * Task to evaluate one from configurations in grid during grid search. 
 * @author Tomas Chovanak
 */
public class GridSearchLearnEvaluatePPSDMTask implements Task {
    
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
    
    
    public GridSearchLearnEvaluatePPSDMTask(int id, int fromid, List<Parameter> params, 
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
        id++; // id is always incremented
        findChangeInParams(params);
        if(fromid >= id){
            return null;
        }

        // initialize and configure learner
        PersonalizedPatternsMiner learner = new PersonalizedPatternsMiner();

            grouping = true;
            boolean valid = configureLearnerWithParams(learner, params);
            // CHECK IF segment legnth and support is valid:
            if(!valid){
                return null;
            }

            updateLastParams(learner);
            // fromid is used to allow user restart evaluation from different point anytime
            
            this.stream = new SessionsFileStream(this.pathToStream);
            writeConfigurationToFile(this.pathToSummaryOutputFile, learner);
            learner.useGroupingOption.setValue(grouping); // change grouping option in learner
            learner.resetLearning();
            stream.prepareForUse();
            TimingUtils.enablePreciseTiming();
            PPSDMRecommendationEvaluator evaluator = 
                    new PPSDMRecommendationEvaluator(
                            this.pathToOutputFile + "results_G" + grouping + 
                                    "_id_" + id + ".csv");
            Configuration.TRANSACTION_COUNTER = 0;
            Configuration.STREAM_START_TIME = TimingUtils.getNanoCPUTimeOfCurrentThread();
            int windowSize = learner.evaluationWindowSizeOption.getValue();
            
            while (stream.hasMoreInstances()) {
                Configuration.TRANSACTION_COUNTER++;
                if(Configuration.TRANSACTION_COUNTER == Configuration.EXTRACT_PATTERNS_AT){
                    // NOW EXTRACT PATTERNS TO FILE
                    extractPatternsToFile(learner.extractPatterns(), this.pathToOutputFile + "patterns_" + id + ".csv");
                }
                Example trainInst = stream.nextInstance();
                /// FOR TESTING LONG TIME SURVIVING OF APPLICATION
                System.out.println(Configuration.TRANSACTION_COUNTER);
                double[] speedResults = UtilitiesPPSDM.getActualTransSec();

                Example testInst = (Example) trainInst.copy();
                if(Configuration.TRANSACTION_COUNTER > Configuration.START_EVALUATING_FROM){
                    RecommendationResults results = learner.getRecommendationsForInstance(testInst);
                    if(results != null)
                        evaluator.addResult(results, windowSize, speedResults[0], Configuration.TRANSACTION_COUNTER); // evaluator will evaluate recommendations and update metrics with given results     
                    
                }
                
                learner.trainOnInstance(trainInst); // this will start training proces - it means first update clustering and then find frequent patterns

            }
            
            double[] speedResults = UtilitiesPPSDM.getActualTransSec();
            SummaryResults results = evaluator.getResults();
            writeResultsToFile(results, speedResults[0], speedResults[1], 
                    Configuration.TRANSACTION_COUNTER);

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
                pwriter.append((fci.getItems()).toString().replaceAll(","," "));pwriter.append(','); 
                pwriter.append('\n');
            }
            pwriter.close();
        } catch (IOException ex) {
            Logger.getLogger(GridSearchLearnEvaluatePPSDMTask.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
   
    private void updateLastParams(PersonalizedPatternsMiner learner) {
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
    
    private boolean configureLearnerWithParams(PersonalizedPatternsMiner learner, List<Parameter> params){
        // RECOMMEND PARAMETERS
        Configuration.RECOMMEND_STRATEGY = RecommendStrategiesEnum.valueOf((int) params.get(0).getValue());
        Configuration.SORT_STRATEGY = SortStrategiesEnum.valueOf((int) params.get(1).getValue());
        learner.evaluationWindowSizeOption.setValue((int) params.get(2).getValue());
        learner.numberOfRecommendedItemsOption.setValue((int) params.get(3).getValue());
        //FPM PARAMETERS
        learner.minSupportOption.setValue(params.get(4).getValue());
        learner.relaxationRateOption.setValue(params.get(5).getValue());
        learner.fixedSegmentLengthOption.setValue((int) (params.get(6).getValue()));
        // CHECK IF MIN SUPPORT AND SEGMENT LENGTH ARE VALID
        if(learner.minSupportOption.getValue()*learner.fixedSegmentLengthOption.getValue() <= 1){
            return false;
        }
        learner.maxItemsetLengthOption.setValue((int) params.get(8).getValue());
        learner.windowSizeOption.setValue((int) params.get(9).getValue());
        // RESTRICTIONS PARAMETERS
        learner.maxNumPages.setValue((int) params.get(10).getValue());
        Configuration.DIMENSION_PAGES = learner.maxNumPages.getValue();
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
        Configuration.MAX_UPDATE_TIME = 20000;
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
        return true;
        
    }
    
    private void writeConfigurationToFile(String path, PersonalizedPatternsMiner learner){
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
            
            writer.append(toInternalDataString(results.getAllHitsGGC()));writer.append(',');
            writer.append(toInternalDataString(results.getRealRecommendedGGC()));writer.append(',');
            writer.append(toInternalDataString(results.getPrecisionGGC()));writer.append(',');
            writer.append(toInternalDataString(results.getRecallGGC()));writer.append(',');
            writer.append(toInternalDataString(results.getF1GGC()));writer.append(',');
            writer.append(toInternalDataString(results.getNdcgGGC()));writer.append(',');
            
            writer.append(toInternalDataString(results.getAllHitsGO()));writer.append(',');
            writer.append(toInternalDataString(results.getRealRecommendedGO()));writer.append(',');
            writer.append(toInternalDataString(results.getPrecisionGO()));writer.append(',');
            writer.append(toInternalDataString(results.getRecallGO()));writer.append(',');
            writer.append(toInternalDataString(results.getF1GO()));writer.append(',');
            writer.append(toInternalDataString(results.getNdcgGO()));writer.append(',');
            
            writer.append(toInternalDataString(results.getAllHitsOG()));writer.append(',');
            writer.append(toInternalDataString(results.getRealRecommendedOG()));writer.append(',');
            writer.append(toInternalDataString(results.getPrecisionOG()));writer.append(',');
            writer.append(toInternalDataString(results.getRecallOG()));writer.append(',');
            writer.append(toInternalDataString(results.getF1OG()));writer.append(',');
            writer.append(toInternalDataString(results.getNdcgOG()));writer.append(',');
            
            writer.append(toInternalDataString(results.getAllTestedItems()));writer.append(',');
            writer.append(toInternalDataString(results.getAllTestedTransactions()));writer.append(',');
            writer.append(toInternalDataString(results.getMaxRecommendedItems()));writer.append(',');
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

    private String toInternalDataString(int[] list) {
        StringBuilder strBuild = new StringBuilder();
        for(int i = 0; i < list.length; i++){
            strBuild.append(list[i]);
            if(i < list.length - 1){
               strBuild.append(":"); 
            }
        }
        return strBuild.toString();
    }
    
     private String toInternalDataString(double[] list) {
        StringBuilder strBuild = new StringBuilder();
        for(int i = 0; i < list.length; i++){
            strBuild.append(list[i]);
            if(i < list.length - 1){
               strBuild.append(":"); 
            }
        }
        return strBuild.toString();
    }

    
    
}
