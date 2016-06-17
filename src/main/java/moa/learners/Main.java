package moa.learners;


import moa.core.Example;
import moa.core.TimingUtils;
import moa.evaluation.PatternsRecommendationEvaluator;
import moa.streams.InstanceStream;
import moa.streams.SessionsFileStream;

public class Main {
    
    public static void main(String args[]){
        //ZakiFileStream stream = new ZakiFileStream("C:\\merge-script\\stream1_stream2_drift-o0.25-l0.001.data");
        //ZakiFileStream stream = new ZakiFileStream("C:\\cygwin\\home\\Massimo\\n1000t15i10p6.data");
        //LEDGenerator stream = new LEDGenerator();
        SessionsFileStream stream = new SessionsFileStream("g:\\workspace_GPMiner\\data\\alef_sessions_aggregated.csv");
        
        PatternsMine learner = new PatternsMine();
        learner.minSupportOption.setValue(0.1);
        learner.relaxationRateOption.setValue(0.5);
        learner.fixedSegmentLengthOption.setValue(500);
        learner.maxItemsetLengthOption.setValue(20);
        learner.windowSizeOption.setValue(10);
        learner.numPages.setValue(5000);
        learner.numMinNumberOfChangesInUserModel.setValue(20);
        learner.resetLearning();
        
        stream.prepareForUse();
        TimingUtils.enablePreciseTiming();
        long start = TimingUtils.getNanoCPUTimeOfCurrentThread();
	PatternsRecommendationEvaluator evaluator = 
                new PatternsRecommendationEvaluator();
        double sumLCS = 0.0;
        while (stream.hasMoreInstances()) {
            Example trainInst = stream.nextInstance();
            // System.out.println(trainInst);
            Example testInst = (Example) trainInst.copy();
             /* this returns array of ids of recommended items from actual 
                testInst learner will find with LCS most promising patterns
                and generate sets of recommendations*/
            double[] recommendations = learner.getVotesForInstance(testInst);
            if(recommendations != null){
                sumLCS += recommendations[0];
            }
            evaluator.addResult(testInst, recommendations); // evaluator will evaluate recommendations and update metrics with given results 
            learner.trainOnInstance(trainInst); // this will start training proces - it means first update clustering and then find frequent patterns
        }
        
        long end = TimingUtils.getNanoCPUTimeOfCurrentThread();
        double tp = 1e5/ ((double)(end - start) / 1e9);
        
        System.out.println(tp + "trans/sec");
    }
    
}
