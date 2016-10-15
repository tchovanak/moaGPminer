/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package moa.tasks;

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
import moa.core.ObjectRepository;
import moa.core.TimingUtils;
import moa.evaluation.PatternsRecommendationEvaluator;
import moa.learners.PatternsMine3;
import moa.streams.SessionsFileStream;
import moa.utils.Configuration;
import com.github.javacliparser.StringOption;
import moa.utils.RecommendStrategiesEnum;
import moa.utils.SortStrategiesEnum;

/**
 * 
 * @author Tomas Chovanak
 */
public class GridSearchEvaluator extends MainTask {

    private int id = 0;
    private SessionsFileStream stream = null;
    public StringOption pathToConfigFile = new StringOption("pathToConfigFile", 'o',
            "Path to file where detail of configuration is stored.", "./");
    private String pathToStream = null;
    private int fromid = 0;
    private boolean grouping = false;
    private boolean faster  = false;
    private boolean fasterWithoutGrouping = false;
    private double[] lastparams = new double[21];  //ms, rr, fsl, mncum, mnmu // if any of these isnt changed it cant be faster 
    private FileWriter writer = null;
    private String pathToSummaryOutputFile = null;
    private String pathToOutputFile = null;
    private double changed = 0;
    
    public GridSearchEvaluator(int fromid) {
        this.fromid = fromid;
    }
    
    public void evaluate(List<Parameter> params){
        // fromid is used to allow user restart evaluation from different point 
        id++; // id is always incremented
        if(fromid >= id){
            return;
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
                return;
            }else{
                fasterWithoutGrouping = false;
            }
        }
        if(faster){
            if(checkLastParamsForFasterConfiguration(params)){ // params werent changed to better
                faster = true;
                return;
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
        double originalFCISETCOUNT = Configuration.MAX_FCI_SET_COUNT;
        double originalUPDATETIME = Configuration.MAX_UPDATE_TIME;
        for(int i = 0; i < 2; i++){
            configureLearnerWithParams(learner, params);
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
            //long start = TimingUtils.getNanoCPUTimeOfCurrentThread();
            long start = TimingUtils.getNanoCPUTimeOfCurrentThread();
            double transsec = 0.0;
            int windowSize = learner.evaluationWindowSizeOption.getValue();
            int numberOfRecommendedItems = learner.numberOfRecommendedItemsOption.getValue();
            updateLastParams(learner);
            while (stream.hasMoreInstances()) {
                counter++;
                Example trainInst = stream.nextInstance();
                if(!stream.hasMoreInstances()){
                    stream.restart();
                }
                Example testInst = (Example) trainInst.copy();
                double[] recommendations = learner.getVotesForInstance(testInst);
                evaluator.addResult(testInst, recommendations, windowSize, numberOfRecommendedItems, transsec, counter); // evaluator will evaluate recommendations and update metrics with given results     
                learner.trainOnInstance(trainInst); // this will start training proces - it means first update clustering and then find frequent patterns
                long end = TimingUtils.getNanoCPUTimeOfCurrentThread();
                //long end = System.nanoTime();
                double tp =((double)(end - start) / 1e9);
                transsec = counter/tp;
                // SPEED CONTROL PART
                if(transsec < Configuration.MIN_TRANSSEC * 10){
                    //Configuration.MAX_FCI_SET_COUNT = originalFCISETCOUNT * (transsec/((Configuration.DESIRED_TRANSSEC)*2));
                    Configuration.MAX_UPDATE_TIME = originalUPDATETIME * (transsec/(Configuration.DESIRED_TRANSSEC));
                }
                if(counter % learner.fixedSegmentLengthOption.getValue() == 0){
                    System.out.println(counter); // to see progress
                    if(transsec < Configuration.MIN_TRANSSEC){
                        try{
                            if(!grouping){
                                fasterWithoutGrouping = true;
                            }else{
                                faster = true;
                            }
                            updateLastParams(learner);
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
            if(!repeatWithGrouping){
                break;
            }else if(i == 0){
                id++;
                grouping = true;
            }
        }
        System.gc();
        
        
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
    
    
    @Override
    protected Object doMainTask(TaskMonitor tm, ObjectRepository or) {
        InputStream fileStream;
        BufferedReader fileReader = null;
        try {
            fileStream = new FileInputStream(this.pathToConfigFile.getValue());
            fileReader = new BufferedReader(new InputStreamReader(fileStream));
            
        } catch (FileNotFoundException ex) {
            Logger.getLogger(GridSearchEvaluator.class.getName()).log(Level.SEVERE, null, ex);
        }
        if(fileReader == null){
            return null;
        }
        startEvaluation(fileReader);
        return null;
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
    
    
    
    private boolean checkLastParamsForNotGroupingChange(List<Parameter> params){
        return changed >= 0 && changed <= 15; // params were changed so try with and without grouping, if other params are changed only try with grouping
        
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
            && lastparams[16] >= params.get(16).getValue() //minNumOfChangesInUserModel
            && lastparams[17] >= params.get(17).getValue()) //minNumOfChangesInMicrocluster
        { // params werent changed to better
            return true;
        }else{
            return false;
        }
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
        learner.maxItemsetLengthOption.setValue((int) params.get(8).getValue());
        learner.windowSizeOption.setValue((int) params.get(9).getValue());
        // RESTRICTIONS PARAMETERS
        learner.maxNumPages.setValue((int) params.get(10).getValue());
        learner.maxNumUserModels.setValue((int) params.get(11).getValue());
        Configuration.MAX_FCI_SET_COUNT = params.get(12).getValue();
        Configuration.DESIRED_TRANSSEC = params.get(13).getValue();
        Configuration.MIN_TRANSSEC = params.get(14).getValue();
        Configuration.MAX_UPDATE_TIME = params.get(15).getValue();
        // CLUSTERING PARAMETERS
        learner.numMinNumberOfChangesInUserModel.setValue((int) params.get(16).getValue());
        learner.numMinNumberOfMicroclustersUpdates.setValue((int) params.get(17).getValue());
        learner.numberOfGroupsOption.setValue((int) params.get(18).getValue());
        learner.maxNumKernelsOption.setValue((int) params.get(19).getValue());
        learner.kernelRadiFactorOption.setValue((int) params.get(20).getValue());
        
    }
    
    private static void writeHeader(String path) {
        try {
            try (FileWriter writer = new FileWriter(path, true)) {
                writer.append("FILE ID");writer.append(',');
                writer.append("USE GROUPING");writer.append(',');
                // RECOMMEND PARAMETERS
                writer.append("REC:RECOMMEND STRATEGY");writer.append(',');
                writer.append("REC:SORT STRATEGY");writer.append(',');
                writer.append("REC:EVALUATION WINDOW SIZE");writer.append(',');
                writer.append("REC:NUM OF RECOMMENDED ITEMS");writer.append(',');
                // INCMINE PARAMETERS
                writer.append("FPM:MIN SUPPORT");writer.append(',');
                writer.append("FPM:RELAXATION RATE");writer.append(','); 
                writer.append("FPM:FIXED SEGMENT LENGTH");writer.append(',');
                writer.append("FPM:GROUP FIXED SEGMENT LENGTH");writer.append(',');
                writer.append("FPM:MAX ITEMSET LENGTH");writer.append(',');
                writer.append("FPM:WINDOW SIZE");writer.append(',');
                // UNIVERSAL PARAMETERS - RESTRICTIONS
                writer.append("RES:MAX NUM PAGES");writer.append(',');
                writer.append("RES:MAX NUM USER MODELS");writer.append(',');
                writer.append("RES:FCI SET COUNT");writer.append(',');
                writer.append("RES:DESIRED TRANSACTIONS PER SECOND");writer.append(',');
                writer.append("RES:MIN TRANSACTIONS PER SECOND");writer.append(',');
                writer.append("RES:UPDATE TIME");writer.append(',');
                // CLUSTERING PARAMETERS
                writer.append("CLU:MIN NUM OF CHANGES IN USER MODEL");writer.append(',');
                writer.append("CLU:MIN NUM OF CHANGES IN MICROCLUSTERS");writer.append(',');
                writer.append("CLU:NUM OF GROUPS");writer.append(',');
                writer.append("CLU:NUM OF MICROKERNELS");writer.append(',');
                writer.append("CLU:KERNEL RADI FACTOR");writer.append(',');
                // RESULTS 
                writer.append("GG: HITS FROM GLOBAL");writer.append(',');
                writer.append("GG: HITS FROM GROUP");writer.append(',');
                writer.append("GG: ALL HITS");writer.append(',');
                writer.append("GG: REAL RECOMMENDED ITEMS");writer.append(',');
                writer.append("GO: ALL HITS");writer.append(',');
                writer.append("GO: REAL RECOMMENDED ITEMS");writer.append(',');
                writer.append("OG: ALL HITS");writer.append(',');
                writer.append("OG: REAL RECOMMENDED ITEMS");writer.append(',');
                writer.append("MAX RECOMMENDED ITEMS");writer.append(',');
                writer.append("DURATION IN SECONDS");writer.append(',');
                writer.append("TRANSACTIONS PER SECOND");writer.append(',');
                writer.append("NUM ANALYZED TRANSACTIONS");writer.append(',');
                writer.append('\n');
            }
        } catch (IOException ex) {
            Logger.getLogger(GridSearchEvaluator.class.getName()).log(Level.SEVERE, null, ex);
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
            writer.append(((Double)Configuration.DESIRED_TRANSSEC).toString());writer.append(',');
            writer.append(((Double)Configuration.MIN_TRANSSEC).toString());writer.append(',');
            writer.append(((Double)Configuration.MAX_UPDATE_TIME).toString());writer.append(',');
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
            writer.flush();
         } catch (IOException ex) {
             Logger.getLogger(GridSearchEvaluator.class.getName()).log(Level.SEVERE, null, ex);
         }
    }

    private void writeResultsToFile(double[] results, double transsec, double tp, int counter){
        try{
            for(double res :results){
                writer.append(((Double)res).toString());writer.append(',');
            }
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
        this.lastparams[13] = (Double)Configuration.DESIRED_TRANSSEC;
        this.lastparams[14] = (Double)Configuration.MIN_TRANSSEC;
        this.lastparams[15] = (Double)Configuration.MAX_UPDATE_TIME;
        
        this.lastparams[16] = learner.numMinNumberOfChangesInUserModel.getValue();
        this.lastparams[17] = learner.numMinNumberOfMicroclustersUpdates.getValue();
        this.lastparams[18] = learner.numberOfGroupsOption.getValue();
        this.lastparams[19] = learner.maxNumKernelsOption.getValue();
        this.lastparams[20] = learner.kernelRadiFactorOption.getValue();
    }

    
    
    private static void startEvaluation(BufferedReader fileReader) {
        List<Parameter> params = new ArrayList<>();
        try {
            String inputSessionFile = fileReader.readLine().split(",")[1].trim();
            String outputToDirectory = fileReader.readLine().split(",")[1].trim();
            int fromid = Integer.parseInt(fileReader.readLine().split(",")[1].trim());
            for(String line = fileReader.readLine(); line != null; line = fileReader.readLine()) {
                String[] row = line.split(",");
                params.add(new Parameter(0.0, Double.parseDouble(row[1].trim()), 
                           Double.parseDouble(row[2].trim()), Double.parseDouble(row[3].trim())));   
            }
            writeHeader(outputToDirectory + "summary_results.csv");
            GridSearchEvaluator evaluator = new GridSearchEvaluator(fromid);
            evaluator.setPathToOutputFile(outputToDirectory);
            evaluator.setPathToInputFile(inputSessionFile);
            evaluator.setPathToSummaryOutputFile(outputToDirectory + "summary_results.csv");
            List<Parameter> preparedParams = new ArrayList<>();
            evaluator.startGridEvaluation(params, preparedParams);  
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
        InputStream fileStream;
        BufferedReader fileReader = null;
        try {
            if(args.length > 0){
                fileStream = new FileInputStream(args[0]);
            }else{
                fileStream = new FileInputStream("g:\\workspace_DP2\\results_grid\\config\\config1_params_INIT.csv");
            }
            
            fileReader = new BufferedReader(new InputStreamReader(fileStream));
            
        } catch (FileNotFoundException ex) {
            Logger.getLogger(GridSearchEvaluator.class.getName()).log(Level.SEVERE, null, ex);
        }
        if(fileReader == null){
            return;
        }
        
        startEvaluation(fileReader);
        
    }
    
    public void setPathToInputFile(String path) {
        this.pathToStream = path;
    }
    
    public void setPathToStream(String pathToStream) {
        this.pathToStream = pathToStream;
    }

    public void setPathToSummaryOutputFile(String pathToSummaryOutputFile) {
        this.pathToSummaryOutputFile = pathToSummaryOutputFile;
    }

    public void setPathToOutputFile(String pathToOutputFile) {
        this.pathToOutputFile = pathToOutputFile;
    }

    @Override
    public Class<?> getTaskResultType() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
