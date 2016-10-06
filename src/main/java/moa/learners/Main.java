package moa.learners;


import moa.core.Example;
import moa.core.TimingUtils;
import moa.evaluation.PatternsRecommendationEvaluator;
import moa.streams.InstanceStream;
import moa.streams.SessionsFileStream;

/*
    Serves to test and debug patterns mine learner.
    @Author : Tomas Chovanak
*/
public class Main {
    
    public static void main(String args[]){
        
        SessionsFileStream stream = new SessionsFileStream("g:\\workspace_DP2\\Preprocessing\\alef\\alef_sessions_aggregated.csv");
        //SessionsFileStream stream = new SessionsFileStream("g:\\workspace_GPMiner\\data\\sessions_sme.csv");
        //SessionsFileStream stream = new SessionsFileStream("g:\\workspace_GPMiner\\data\\aggregated_sessions_dailyherald.csv");
        //SessionsFileStream stream = new SessionsFileStream("g:\\workspace_GPMiner\\data\\aggregated_sessions_nasa.csv");
        //SessionsFileStream stream = new SessionsFileStream("g:\\workspace_GPMiner\\data\\aggregated_sessions_sacbee.csv");
        
        PatternsMine3 learner = new PatternsMine3(false);
        learner.minSupportOption.setValue(0.1);
        learner.relaxationRateOption.setValue(0.9);
        learner.fixedSegmentLengthOption.setValue(100);
        learner.maxItemsetLengthOption.setValue(20);
        learner.windowSizeOption.setValue(1);
        learner.numPages.setValue(2080);
        learner.numMinNumberOfChangesInUserModel.setValue(15);
        learner.numMinNumberOfMicroclustersUpdates.setValue(100);
        learner.evaluationWindowSizeOption.setValue(2);
        learner.numberOfRecommendedItemsOption.setValue(5);
        learner.numberOfGroupsOption.setValue(4);
        learner.resetLearning();
        
        stream.prepareForUse();
        TimingUtils.enablePreciseTiming();
	PatternsRecommendationEvaluator evaluator = 
                new PatternsRecommendationEvaluator("g:\\workspace_DP2\\results_grid\\alef\\test.csv");
        double sumLCS = 0.0;
        int windowSize = learner.evaluationWindowSizeOption.getValue();
        int numberOfRecommendedItems = learner.numberOfRecommendedItemsOption.getValue();
        int counter = 0;
        long start = System.nanoTime();
        double transsec = 0;
        while (stream.hasMoreInstances()) {
            Example trainInst = stream.nextInstance();
            Example testInst = (Example) trainInst.copy();
             /* this returns array of ids of recommended items from actual 
                testInst learner will find with LCS most promising patterns
                and generate sets of recommendations*/
            double[] recommendations = learner.getVotesForInstance(testInst);
            evaluator.addResult(testInst, recommendations, windowSize, numberOfRecommendedItems, transsec); // evaluator will evaluate recommendations and update metrics with given results 
            learner.trainOnInstance(trainInst); // this will start training proces - it means first update clustering and then find frequent patterns
            long end = System.nanoTime();
            counter++;
            double tp =((double)(end - start) / 1e9);
            transsec = counter/tp;
            System.out.println(counter);
            System.out.println(tp);
            System.out.println(transsec);
        }
        
        long end = TimingUtils.getNanoCPUTimeOfCurrentThread();
        double tp = 1e5/ ((double)(end - start) / 1e9);
        
        System.out.println(tp + "trans/sec");
    }
    
}
