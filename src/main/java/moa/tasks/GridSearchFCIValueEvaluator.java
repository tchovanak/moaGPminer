/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package moa.tasks;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import moa.core.Example;
import moa.core.TimingUtils;
import moa.evaluation.PatternsRecommendationEvaluator;
import moa.learners.PatternsMine3;
import moa.streams.SessionsFileStream;
import moa.utils.Configuration;

/**
 * 
 * @author Tomas Chovanak
 */
public class GridSearchFCIValueEvaluator  {
   
    private boolean faster = false;
    private int id = 0;
    private SessionsFileStream stream = null;
    private FileWriter writer = null;
    private boolean grouping = true;
    
    public void evaluate(){
        // fromid is used to allow user restart evaluation from different point 
        id++; // id is always incremented
        // initialize and configure learner
        PatternsMine3 learner = new PatternsMine3(true);
        configureLearnerWithParams(learner);
        this.stream = new SessionsFileStream("g:\\workspace_DP2\\Preprocessing\\alef\\alef_sessions_aggregated.csv");
        writeConfigurationToFile("g:\\workspace_DP2\\results_grid\\alef\\", learner);
        learner.useGroupingOption.setValue(grouping); // change grouping option in learner
        learner.resetLearning();
        stream.prepareForUse();
        TimingUtils.enablePreciseTiming();
        PatternsRecommendationEvaluator evaluator = 
                new PatternsRecommendationEvaluator(
                         "g:\\workspace_DP2\\results_grid\\alef\\" + "results_fcivalue" + 
                                "_id_" + id + ".csv");
        int counter = 0;
        long start = TimingUtils.getNanoCPUTimeOfCurrentThread();
        double transsec = 0.0;
        int windowSize = learner.evaluationWindowSizeOption.getValue();
        int numberOfRecommendedItems = learner.numberOfRecommendedItemsOption.getValue();
        while (stream.hasMoreInstances()) {
            counter++;
            Example trainInst = stream.nextInstance();
            Example testInst = (Example) trainInst.copy();
            double[] recommendations = learner.getVotesForInstance(testInst);
            evaluator.addResult(testInst, recommendations, windowSize, numberOfRecommendedItems, transsec, counter); // evaluator will evaluate recommendations and update metrics with given results     
            learner.trainOnInstance(trainInst); // this will start training proces - it means first update clustering and then find frequent patterns
            long end = TimingUtils.getNanoCPUTimeOfCurrentThread();
            //long end = System.nanoTime();
            double tp =((double)(end - start) / 1e9);
            transsec = counter/tp;
            if(counter % learner.fixedSegmentLengthOption.getValue() == 0){
                System.out.println(counter); // to see progress
                if(transsec < 5.0){
                    try{
                        writer.append('\n');
                        writer.close();
                        return; 
                    } catch (IOException ex) {
                        Logger.getLogger(GridSearchEvaluator.class.getName()).log(Level.SEVERE, null, ex);
                    }  
                }   
            }
        }
        long end = TimingUtils.getNanoCPUTimeOfCurrentThread();
        double tp =((double)(end - start) / 1e9);
        transsec = counter/tp;
        double[] results = evaluator.getResults();
        writeResultsToFile(results, transsec, tp, counter);   
    }
    
    
    private void writeConfigurationToFile(String path, PatternsMine3 learner){
        try {
            this.writer = new FileWriter("g:\\workspace_DP2\\results_grid\\alef\\summary_results_fci_value.csv", true);
            writer.append(((Integer)id).toString());writer.append(',');
            writer.append(((Boolean)this.grouping).toString());writer.append(',');
            writer.append((Configuration.RECOMMEND_STRATEGY).toString());writer.append(',');
            writer.append((Configuration.SORT_STRATEGY).toString());writer.append(',');
            writer.append(((Double)learner.minSupportOption.getValue()).toString());writer.append(',');
            writer.append(((Double)learner.relaxationRateOption.getValue()).toString());writer.append(',');
            writer.append(((Double)(double)learner.fixedSegmentLengthOption.getValue()).toString());writer.append(',');
            writer.append(((Double)(double)learner.maxItemsetLengthOption.getValue()).toString());writer.append(',');
            writer.append(((Double)(double)learner.windowSizeOption.getValue()).toString());writer.append(',');
            writer.append(((Double)(double)learner.maxNumPages.getValue()).toString());writer.append(',');
            writer.append(((Double)(double)learner.evaluationWindowSizeOption.getValue()).toString());writer.append(',');
            writer.append(((Double)(double)learner.numberOfRecommendedItemsOption.getValue()).toString());writer.append(',');
            if(this.grouping){
                writer.append(((Double)(double) learner.numMinNumberOfChangesInUserModel.getValue()).toString());writer.append(',');
                writer.append(((Double)(double) learner.numMinNumberOfMicroclustersUpdates.getValue()).toString());writer.append(',');
                writer.append(((Double)(double) learner.numberOfGroupsOption.getValue()).toString());writer.append(',');
             }else{
                writer.append("No grouping");writer.append(',');
                writer.append("No grouping");writer.append(',');
                writer.append("No grouping");writer.append(',');
            }
            writer.append(((Double)Configuration.A).toString());writer.append(',');
            writer.append(((Double)Configuration.B).toString());writer.append(',');
            writer.append(((Double)Configuration.C).toString());writer.append(',');
            writer.flush();
         } catch (IOException ex) {
             Logger.getLogger(GridSearchEvaluator.class.getName()).log(Level.SEVERE, null, ex);
         }
    }
    
    private void configureLearnerWithParams(PatternsMine3 learner){
        learner.minSupportOption.setValue(0.05);
        learner.relaxationRateOption.setValue(0.5);
        learner.fixedSegmentLengthOption.setValue(50);
        learner.maxItemsetLengthOption.setValue(10);
        learner.windowSizeOption.setValue(10);
        learner.maxNumPages.setValue(2080);
        learner.evaluationWindowSizeOption.setValue(1);
        learner.numberOfRecommendedItemsOption.setValue(1);
        learner.numMinNumberOfChangesInUserModel.setValue(50);
        learner.numMinNumberOfMicroclustersUpdates.setValue(50);
        learner.numberOfGroupsOption.setValue(2);
        
        learner.groupFixedSegmentLengthOption.setValue(
                (int)(((double)learner.fixedSegmentLengthOption.getValue())/((double)learner.numberOfGroupsOption.getValue())));
    }
    
    private void writeResultsToFile(double[] results, double transsec, double tp, int counter){
        try{
                writer.append(((Double)results[0]).toString());
                writer.append(',');
                writer.append(((Double)results[1]).toString());
                writer.append(',');
                writer.append(((Double)results[2]).toString());
                writer.append(',');
                writer.append(((Double)results[3]).toString());
                writer.append(',');
                writer.append(((Double)results[4]).toString());
                writer.append(',');
                writer.append(((Double)results[5]).toString());
                writer.append(',');
                writer.append(((Double)results[6]).toString());
                writer.append(',');
                writer.append(((Double)results[7]).toString());
                writer.append(',');
                writer.append(((Double)results[8]).toString());
                writer.append(',');
                writer.append(((Double)results[9]).toString());
                writer.append(',');
                writer.append(((Double)results[10]).toString());
                writer.append(',');
                writer.append(((Double)results[11]).toString());
                writer.append(',');
                writer.append(((Double)results[12]).toString());
                writer.append(',');
                writer.append(((Double)results[13]).toString());
                writer.append(',');
                writer.append(((Double)results[14]).toString());
                writer.append(',');
                writer.append(((Double)transsec).toString());
                writer.append(',');
                writer.append(((Double)tp).toString());
                writer.append(',');
                writer.append(((Integer)counter).toString());
                writer.append('\n');
                writer.close();
            } catch (IOException ex) {
                Logger.getLogger(GridSearchEvaluator.class.getName()).log(Level.SEVERE, null, ex);
            }
    }
    
    
    /**
     * Arguments:
     *  1. config file: path to config file where parameters for grid search are declared
    * @param args 
     */
    public static void main(String args[]){
        startEvaluation();
    }
    
    private static void writeHeader(String path, boolean grouping) {
        try {
            try (FileWriter writer = new FileWriter(path, true)) {
                writer.append("fileid");
                writer.append(',');
                writer.append("grouping");
                writer.append(',');
                writer.append("recommend strategy");
                writer.append(',');
                writer.append("sort strategy");
                writer.append(',');
                writer.append("minSupportOption");
                writer.append(',');
                writer.append("relaxationRateOption");
                writer.append(','); 
                writer.append("fixedSegmentLengthOption");
                writer.append(',');
                writer.append("maxItemsetLengthOption");
                writer.append(',');
                writer.append("windowSizeOption");
                writer.append(',');
                writer.append("numPages");
                writer.append(',');
                writer.append("evaluationWindowSizeOption");
                writer.append(',');
                writer.append("numberOfRecommendedItemsOption");
                writer.append(',');
                writer.append("numMinNumberOfChangesInUserModel");
                writer.append(',');
                writer.append("numMinNumberOfMicroclustersUpdates");
                writer.append(',');
                writer.append("numOfGroups");
                writer.append(',');
                writer.append("A");
                writer.append(',');
                writer.append("B");
                writer.append(',');
                writer.append("C");
                writer.append(',');
                writer.append("precGG1");
                writer.append(',');
                writer.append("precGG2");
                writer.append(',');
                writer.append("precGO1");
                writer.append(',');
                writer.append("precGO2");
                writer.append(',');
                writer.append("precOG1");
                writer.append(',');
                writer.append("precOG2");
                writer.append(',');
                writer.append("GG hits from global patterns");
                writer.append(',');
                writer.append("GG hits from group patterns");
                writer.append(',');
                writer.append("GG all hits");
                writer.append(',');
                writer.append("GG real recommended items");
                writer.append(',');
                writer.append("GO all hits");
                writer.append(',');
                writer.append("GO real recommended items");
                writer.append(',');
                writer.append("OG all hits");
                writer.append(',');
                writer.append("OG real recommended items");
                writer.append(',');
                writer.append("max recommended items");
                writer.append(',');
                writer.append("transaction per second");
                writer.append(',');
                writer.append("seconds");
                writer.append(',');
                writer.append("transactions");
                writer.append('\n');
            }
        } catch (IOException ex) {
            Logger.getLogger(GridSearchEvaluator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private static void writeHeader(String path) {
        try {
            try (FileWriter writer = new FileWriter(path, true)) {
                writer.append("fileid");
                writer.append(',');
                writer.append("grouping");
                writer.append(',');
                writer.append("minSupportOption");
                writer.append(',');
                writer.append("relaxationRateOption");
                writer.append(','); 
                writer.append("fixedSegmentLengthOption");
                writer.append(',');
                writer.append("maxItemsetLengthOption");
                writer.append(',');
                writer.append("windowSizeOption");
                writer.append(',');
                writer.append("numPages");
                writer.append(',');
                writer.append("evaluationWindowSizeOption");
                writer.append(',');
                writer.append("numberOfRecommendedItemsOption");
                writer.append(',');
                writer.append("numMinNumberOfChangesInUserModel");
                writer.append(',');
                writer.append("numMinNumberOfMicroclustersUpdates");
                writer.append(',');
                writer.append("numOfGroups");
                writer.append(',');
                writer.append("A");
                writer.append(',');
                writer.append("B");
                writer.append(',');
                writer.append("C");
                writer.append(',');
                writer.append("precGG1");
                writer.append(',');
                writer.append("precGG2");
                writer.append(',');
                writer.append("precGO1");
                writer.append(',');
                writer.append("precGO2");
                writer.append(',');
                writer.append("precOG1");
                writer.append(',');
                writer.append("GG hits from global patterns");
                writer.append(',');
                writer.append("GG hits from group patterns");
                writer.append(',');
                writer.append("GG all hits");
                writer.append(',');
                writer.append("GG real recommended items");
                writer.append(',');
                writer.append("GO all hits");
                writer.append(',');
                writer.append("GO real recommended items");
                writer.append(',');
                writer.append("OG all hits");
                writer.append(',');
                writer.append("OG real recommended items");
                writer.append(',');
                writer.append("max recommended items");
                writer.append(',');
                writer.append("transaction per second");
                writer.append(',');
                writer.append("seconds");
                writer.append(',');
                writer.append("transactions");
                writer.append('\n');
            }
        } catch (IOException ex) {
            Logger.getLogger(GridSearchEvaluator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private static void startEvaluation() {
        List<Parameter> params = new ArrayList<>();   
        writeHeader("g:\\workspace_DP2\\results_grid\\alef\\summary_results_fci_value.csv");
        GridSearchFCIValueEvaluator evaluator = new GridSearchFCIValueEvaluator(); 
        for(double a = 0.1; a <= 1.0; a += 0.1){
            for(double b = 0.1; b <= 1.0; b += 0.1){
                    Configuration.A = a;
                    Configuration.B = b;
                    evaluator.evaluate();
            }
        }
        
    }
   
}
