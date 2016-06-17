/*
 *    LearnEvaluateModel.java
 *    Copyright (C) 2007 University of Waikato, Hamilton, New Zealand
 *    @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 *    @author Albert Bifet (abifet at cs dot waikato dot ac dot nz)
 *
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program; if not, write to the Free Software
 *    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package moa.tasks;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;

import moa.learners.Learner;
import moa.core.Measurement;
import moa.core.ObjectRepository;
import moa.core.TimingUtils;
import moa.evaluation.ClassificationPerformanceEvaluator;
import moa.evaluation.LearningCurve;
import moa.evaluation.LearningEvaluation;
import moa.evaluation.PatternsRecommendationEvaluator;
import moa.options.ClassOption;
import com.github.javacliparser.FileOption;
import com.github.javacliparser.IntOption;
import moa.streams.InstanceStream;
import moa.streams.SessionsFileStream;
import moa.learners.PatternsMine;
import com.yahoo.labs.samoa.instances.Instance;
import moa.core.Example;


public class LearnEvaluateBehaviourPatterns extends MainTask {
    
    @Override
    public String getPurposeString() {
        return "Evaluates a classifier on a session stream by testing then "
                + "training with each example in sequence.";
    }
    
   private static final long serialVersionUID = 1L;
    
   public ClassOption learnerOption = new ClassOption("learner", 'l',
            "To train.", Learner.class, "IncMine");

   public ClassOption streamOption = new ClassOption("stream", 's',
            "Stream to learn from.", InstanceStream.class, "ArffFileStream");
   
  
    
    @Override
    protected Object doMainTask(TaskMonitor tm, ObjectRepository or) {
      
        Learner learner = new PatternsMine();
        learner.resetLearning();
	InstanceStream stream = (InstanceStream) getPreparedClassOption(this.streamOption);
	PatternsRecommendationEvaluator evaluator = 
                new PatternsRecommendationEvaluator();
       
        while (stream.hasMoreInstances()) {
            System.out.println("next instance");
            Example trainInst = stream.nextInstance();
            //System.out.println(trainInst);
            Example testInst = (Example) trainInst.copy();
             /* this returns array of ids of recommended items from actual 
                testInst learner will find with LCS most promising patterns
                and generate sets of recommendations*/
            double[] recommendations = learner.getVotesForInstance(testInst);
            evaluator.addResult(testInst, recommendations); // evaluator will evaluate recommendations and update metrics with given results 
            learner.trainOnInstance(trainInst); // this will start training proces - it means first update clustering and then find frequent patterns
        }
        return null;
    }

    @Override
    public Class<?> getTaskResultType() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    
}
