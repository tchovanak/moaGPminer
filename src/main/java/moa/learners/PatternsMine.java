/*
 *    IncMine.java
 *    Copyright (C) 2012 Universitat Politècnica de Catalunya
 *    @author Massimo Quadrana <max.square@gmail.com>
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
package moa.learners;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import moa.MOAObject;
import moa.core.*;
import com.github.javacliparser.FloatOption;
import com.github.javacliparser.IntOption;
import com.github.javacliparser.MultiChoiceOption;
import com.yahoo.labs.samoa.instances.Prediction;
import com.yahoo.labs.samoa.instances.Instance;
import static java.lang.System.in;
import moa.core.InstanceExample;
import moa.clusterers.clustream.Clustream;
import moa.clusterers.Clusterer;
import moa.cluster.Clustering;
import moa.cluster.Cluster;

public class PatternsMine extends AbstractLearner implements Observer {

    private static final long serialVersionUID = 1L;
    
    
    Learner incMine = new IncMine();
    Clusterer clusterer = new Clustream();
     
    @Override
    public void resetLearningImpl() {
        System.out.println("resetLearning");
    }

    @Override
    public void trainOnInstance(Example e) {
        incMine.trainOnInstance(e);
    }

    @Override
    public double[] getVotesForInstance(Example e) {
        Clustering results = clusterer.getClusteringResult(); // append group to instance that it belongs to...
        if(results != null){
            Cluster bestCluster = null;
            Double maxProb = 0.0;
            for(Cluster c : results.getClustering()){
                double prob = c.getInclusionProbability((Instance)e.getData());
                if(prob >= maxProb){
                    bestCluster = c;
                }
            }
            System.out.println(bestCluster);
        }
        
	// then perform LCS and find recommendend items finally
//        allFcis = global_FCIs + groupFCIs
        // LCS
        return null;
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
    
   

}
