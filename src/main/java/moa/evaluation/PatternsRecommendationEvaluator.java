/*
 *    BasicClassificationPerformanceEvaluator.java
 *    Copyright (C) 2007 University of Waikato, Hamilton, New Zealand
 *    @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 *    @author Albert Bifet (abifet at cs dot waikato dot ac dot nz)
 *
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program. If not, see <http://www.gnu.org/licenses/>.
 *    
 */
package moa.evaluation;

import moa.AbstractMOAObject;
import moa.core.Measurement;
import weka.core.Utils;
import moa.core.Example;
import com.yahoo.labs.samoa.instances.Instance;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Pattern mining evaluator that performs basic incremental evaluation.
 *
 * @author Tomas Chovanak
 */
public class PatternsRecommendationEvaluator extends AbstractMOAObject{

    private final Queue<Double> window = new LinkedList<>();
    private int sumLCS =  0;
    private int allItems = 0;
    private int windowSize = 1000;
    private double sumWindow = 0.0;
    
    private void addNum(double num) {
        sumWindow += num;
        window.add(num);
        if (window.size() > windowSize) {
            sumWindow -= window.remove();
        }
    }
    
    private double getAvg() {
        if (window.isEmpty()) return 0; // technically the average is undefined
        return sumWindow / window.size();
    }
    
    public void addResult(Example instance, double[] recommendations, int windowSize, 
            int numberOfRecommendedItems){
        if(recommendations != null){
            double lcsVal = 0;
            for(int i = 0; i < recommendations.length; i++){
                 lcsVal += recommendations[i];
            }
            this.sumLCS += lcsVal;
            double all = ((Instance)instance.getData()).numValues() - windowSize;
            if(all > numberOfRecommendedItems){
                all = numberOfRecommendedItems;
            }
            allItems += all;
            double prec = lcsVal/all;
            addNum(prec);
        }
        System.out.println("TP : " + sumLCS);
        System.out.println("ALL : " + allItems);
        System.out.println("PRECISION : " + (((double)sumLCS)/((double)allItems)));
        System.out.println("MOVING AVERAGE: " + getAvg());
    }
    
    @Override
    public void getDescription(StringBuilder sb, int i) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    
}
