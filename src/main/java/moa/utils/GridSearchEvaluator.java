/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package moa.utils;

import com.yahoo.labs.samoa.instances.Instance;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import moa.core.Example;
import moa.core.TimingUtils;
import moa.evaluation.PatternsRecommendationEvaluator;
import moa.learners.PatternsMine3;
import moa.streams.SessionsFileStream;

/**
 * 
 * @author Tomas
 */
public class GridSearchEvaluator {

    
    
    private int id = 0;
    private SessionsFileStream stream = null;
    private String pathToOutputFile = null;
    private String pathToSummaryOutputFile = null;
    private int fromid = 0;
    private String pathToStream = null;
    private boolean grouping = false;
    private double mintranssec = 5;
    private boolean faster  = false;
    private double[] lastparams = new double[5];  //ms, rr, fsl, mncum, mnmu // if any of these isnt changed it cant be faster 
    
    
    private GridSearchEvaluator(int fromid, boolean grouping, double transsec) {
        this.fromid = fromid;
        this.grouping = grouping;
        this.mintranssec = transsec;
    }
    
    public void evaluate(List<Parameter> params){
        if(fromid > id){
            id++;
            return;
        }
        if(faster){
            if(lastparams[0] >= params.get(0).getValue() 
                    && lastparams[1] >= params.get(1).getValue()
                    && lastparams[2] >= params.get(2).getValue()
                    && lastparams[3] >= params.get(8).getValue()
                    && lastparams[4] >= params.get(9).getValue()){ // params werent changed to better
                faster = true;
                return;
            }else{
                faster = false;
            }
        }
        this.stream = new SessionsFileStream(this.pathToStream);
        FileWriter writer = null;
        PatternsMine3 learner = new PatternsMine3(this.grouping);
        learner.minSupportOption.setValue(params.remove(0).getValue());
        learner.relaxationRateOption.setValue(params.remove(0).getValue());
        learner.fixedSegmentLengthOption.setValue((int) (params.remove(0).getValue()));
        learner.maxItemsetLengthOption.setValue((int) params.remove(0).getValue());
        learner.windowSizeOption.setValue((int) params.remove(0).getValue());
        learner.numPages.setValue((int) params.remove(0).getValue());
        learner.evaluationWindowSizeOption.setValue((int) params.remove(0).getValue());
        learner.numberOfRecommendedItemsOption.setValue((int) params.remove(0).getValue());
        if(this.grouping){
            learner.numMinNumberOfChangesInUserModel.setValue((int) params.remove(0).getValue());
            learner.numMinNumberOfMicroclustersUpdates.setValue((int) params.remove(0).getValue());
            learner.numberOfGroupsOption.setValue((int) params.remove(0).getValue());
        }
        learner.groupFixedSegmentLengthOption.setValue(
                (int)(((double)learner.fixedSegmentLengthOption.getValue())/((double)learner.numberOfGroupsOption.getValue())));
        try {
           writer = new FileWriter(this.getPathToSummaryOutputFile(), true);
           writer.append(((Integer)id).toString());writer.append(',');
           writer.append(((Double)learner.minSupportOption.getValue()).toString());writer.append(',');
           writer.append(((Double)learner.relaxationRateOption.getValue()).toString());writer.append(',');
           writer.append(((Double)(double)learner.fixedSegmentLengthOption.getValue()).toString());writer.append(',');
           writer.append(((Double)(double)learner.maxItemsetLengthOption.getValue()).toString());writer.append(',');
           writer.append(((Double)(double)learner.windowSizeOption.getValue()).toString());writer.append(',');
           writer.append(((Double)(double)learner.numPages.getValue()).toString());writer.append(',');
           writer.append(((Double)(double)learner.evaluationWindowSizeOption.getValue()).toString());writer.append(',');
           writer.append(((Double)(double)learner.numberOfRecommendedItemsOption.getValue()).toString());writer.append(',');
           if(this.grouping){
               writer.append(((Double)(double) learner.numMinNumberOfChangesInUserModel.getValue()).toString());writer.append(',');
               writer.append(((Double)(double) learner.numMinNumberOfMicroclustersUpdates.getValue()).toString());writer.append(',');
               writer.append(((Double)(double) learner.numberOfGroupsOption.getValue()).toString());writer.append(',');
            }
           writer.flush();
        } catch (IOException ex) {
            Logger.getLogger(GridSearchEvaluator.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        
        learner.resetLearning();
        stream.prepareForUse();
        
        TimingUtils.enablePreciseTiming();
        
	
        PatternsRecommendationEvaluator evaluator = 
                new PatternsRecommendationEvaluator(this.getPathToOutputFile() + "results_part_G" + grouping + "_id_" + id++ + ".csv");
        double sumLCS = 0.0;
        int windowSize = learner.evaluationWindowSizeOption.getValue();
        int numberOfRecommendedItems = learner.numberOfRecommendedItemsOption.getValue();
        int counter = 0;
        //long start = TimingUtils.getNanoCPUTimeOfCurrentThread();
        long start = System.nanoTime();
        double transsec = 0.0;
        while (stream.hasMoreInstances()) {
            counter++;
            Example trainInst = stream.nextInstance();
            Example testInst = (Example) trainInst.copy();
             /* this returns array of ids of recommended items from actual 
                testInst learner will find with LCS most promising patterns
                and generate sets of recommendations*/
            double[] recommendations = learner.getVotesForInstance(testInst);
            evaluator.addResult(testInst, recommendations, windowSize, numberOfRecommendedItems, transsec, counter); // evaluator will evaluate recommendations and update metrics with given results     
            learner.trainOnInstance(trainInst); // this will start training proces - it means first update clustering and then find frequent patterns
            //long end = TimingUtils.getNanoCPUTimeOfCurrentThread();
            long end = System.nanoTime();
            double tp =((double)(end - start) / 1e9);
            transsec = counter/tp;
            System.out.println(counter);
            System.out.println(tp);
            System.out.println(transsec);
            if(counter % learner.fixedSegmentLengthOption.getValue() == 0 && transsec < this.mintranssec){
                try{
                    faster = true;
                    this.lastparams[0] = learner.minSupportOption.getValue();
                    this.lastparams[1] = learner.relaxationRateOption.getValue();
                    this.lastparams[2] = learner.fixedSegmentLengthOption.getValue();
                    this.lastparams[3] = learner.numMinNumberOfChangesInUserModel.getValue();
                    this.lastparams[4] = learner.numMinNumberOfMicroclustersUpdates.getValue();
                    writer.append('\n');
                    writer.close();
                    return; 
//                    if(!learner.increaseRelaxationRate(0.1)){
//                        writer.append('\n');
//                        writer.close();
//                        return;
//                    }  
                } catch (IOException ex) {
                    Logger.getLogger(GridSearchEvaluator.class.getName()).log(Level.SEVERE, null, ex);
                }  
            }
                
        }
       
        long end = System.nanoTime();
        double tp =((double)(end - start) / 1e9);
        transsec = counter/tp;
        double[] results = evaluator.getResults();
       
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
    
    public void startGridEvaluation(List<Parameter> params, List<Parameter> preparedParameters){
        if(params.isEmpty()){
            this.evaluate(preparedParameters);
        }else{
            List<Parameter> origParamsCopy = deepCopy(params);
            Parameter p = origParamsCopy.remove(0);
            if(p.getValue() > 0){
                origParamsCopy = deepCopy(params);
                Parameter p2 = origParamsCopy.remove(0);
                List<Parameter> copyParams = deepCopy(preparedParameters);
               
                    copyParams.add(p2);
                
                this.startGridEvaluation(origParamsCopy, copyParams);
            }else{
                double[] b = p.getBoundaries();
                for(double i = b[0]; i <= b[1]; i+= b[2]){
                      
                    origParamsCopy = deepCopy(params);
                    Parameter p2 = origParamsCopy.remove(0);
                    p2.setValue(i);
                    List<Parameter> copyParams = deepCopy(preparedParameters);
                    
                        copyParams.add(p2);
                    
                    this.startGridEvaluation(origParamsCopy, copyParams);
                }
                
            }
        }
    }
    
    private List<Parameter> deepCopy(List<Parameter> orig){
        List<Parameter> copy = new ArrayList<Parameter>(); 
        Iterator<Parameter> iterator = orig.iterator(); 
        while(iterator.hasNext()){ 
            try { 
                copy.add(iterator.next().clone());
            } catch (CloneNotSupportedException ex) {
                Logger.getLogger(GridSearchEvaluator.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return copy;
    }
    
    
    
    /**
     * Arguments:
     *  1. grouping: 0 or 1
     *  2. input file path - where session data is stored
    * @param args 
     */
    public static void main(String args[]){
        InputStream fileStream;
        BufferedReader fileReader = null;
        try {
            if(args.length > 0){
                fileStream = new FileInputStream(args[0]);
            }else{
                fileStream = new FileInputStream("g:\\workspace_DP2\\results_grid\\config\\start_configuration_grouping-test.csv");
            }
            
            fileReader = new BufferedReader(new InputStreamReader(fileStream));
            
        } catch (FileNotFoundException ex) {
            Logger.getLogger(GridSearchEvaluator.class.getName()).log(Level.SEVERE, null, ex);
        }
        if(fileReader == null){
            return;
        }
        List<Parameter> params = new ArrayList<>();
        try {
            String inputSessionFile = fileReader.readLine().split(",")[1].trim();
            String outputToDirectory = fileReader.readLine().split(",")[1].trim();
            int fromid = Integer.parseInt(fileReader.readLine().split(",")[1].trim());
            boolean grouping = Boolean.parseBoolean(fileReader.readLine().split(",")[1].trim());
            double transsec = Double.parseDouble(fileReader.readLine().split(",")[1].trim());
            double maxUpdateTime = Double.parseDouble(fileReader.readLine().split(",")[1].trim());
            Configuration.MAX_UPDATE_TIME = maxUpdateTime;
            for(String line = fileReader.readLine(); line != null; line = fileReader.readLine()) {
                String[] row = line.split(",");
                params.add(new Parameter(0, Double.parseDouble(row[1]), 
                           Double.parseDouble(row[2]), Double.parseDouble(row[3])));   
            }
            writeHeader(outputToDirectory + "summary_results_G" + grouping + ".csv", grouping);
            GridSearchEvaluator evaluator = new GridSearchEvaluator(fromid, grouping, transsec);
            evaluator.setPathToOutputFile(outputToDirectory);
            evaluator.setPathToInputFile(inputSessionFile);
            evaluator.setPathToSummaryOutputFile(outputToDirectory + "summary_results_G" + grouping + ".csv");
            List<Parameter> preparedParams = new ArrayList<>();
            evaluator.startGridEvaluation(params, preparedParams);  
        } catch (IOException ex) {
            Logger.getLogger(GridSearchEvaluator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private static void writeHeader(String path, boolean grouping) {
        try {
            try (FileWriter writer = new FileWriter(path, true)) {
                writer.append("fileid");
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
                if(grouping){
                    writer.append("numMinNumberOfChangesInUserModel");
                    writer.append(',');
                    writer.append("numMinNumberOfMicroclustersUpdates");
                    writer.append(',');
                    writer.append("numOfGroups");
                    writer.append(',');
                }
                writer.append("precision");
                writer.append(',');
                writer.append("movingAveragePrecision");
                writer.append(',');
                writer.append("hit count");
                writer.append(',');
                writer.append("recommended count");
                writer.append(',');
                writer.append("real recommended count");
                writer.append(',');
                writer.append("precision with real recommended count");
                writer.append(',');
                writer.append("number of succesfully recommended sessions");
                writer.append(',');
                writer.append("number of recommended sessions");
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

    public String getPathToOutputFile() {
        return pathToOutputFile;
    }

    public void setPathToOutputFile(String pathToOutpuFile) {
        this.pathToOutputFile = pathToOutpuFile;
    }
    
    public void setPathToSummaryOutputFile(String pathToOutpuFile) {
        this.pathToSummaryOutputFile = pathToOutpuFile;
    }

    public String getPathToSummaryOutputFile() {
        return pathToSummaryOutputFile;
    }
    
    public void setPathToInputFile(String path) {
        this.pathToStream = path;
    }
    
}
