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
        
        SessionsFileStream stream = new SessionsFileStream("g:\\workspace_GPMiner\\data\\alef_sessions_aggregated.csv");
        
        PatternsMine learner = new PatternsMine();
        learner.minSupportOption.setValue(0.1);
        learner.relaxationRateOption.setValue(0.5);
        learner.fixedSegmentLengthOption.setValue(500);
        learner.maxItemsetLengthOption.setValue(20);
        learner.windowSizeOption.setValue(10);
        learner.numPages.setValue(5000);
        learner.numMinNumberOfChangesInUserModel.setValue(20);
        learner.numMinNumberOfMicroclustersUpdates.setValue(50);
        learner.evaluationWindowSizeOption.setValue(3);
        learner.numberOfRecommendedItemsOption.setValue(10);
        learner.resetLearning();
        
        stream.prepareForUse();
        TimingUtils.enablePreciseTiming();
        long start = TimingUtils.getNanoCPUTimeOfCurrentThread();
	PatternsRecommendationEvaluator evaluator = 
                new PatternsRecommendationEvaluator();
        double sumLCS = 0.0;
        int windowSize = learner.evaluationWindowSizeOption.getValue();
        int numberOfRecommendedItems = learner.numberOfRecommendedItemsOption.getValue();
        while (stream.hasMoreInstances()) {
            Example trainInst = stream.nextInstance();
            Example testInst = (Example) trainInst.copy();
             /* this returns array of ids of recommended items from actual 
                testInst learner will find with LCS most promising patterns
                and generate sets of recommendations*/
            double[] recommendations = learner.getVotesForInstance(testInst);
            evaluator.addResult(testInst, recommendations, windowSize, numberOfRecommendedItems); // evaluator will evaluate recommendations and update metrics with given results 
            learner.trainOnInstance(trainInst); // this will start training proces - it means first update clustering and then find frequent patterns
        }
        
        long end = TimingUtils.getNanoCPUTimeOfCurrentThread();
        double tp = 1e5/ ((double)(end - start) / 1e9);
        
        System.out.println(tp + "trans/sec");
    }
    
}
