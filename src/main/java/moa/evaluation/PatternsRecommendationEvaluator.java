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

import com.yahoo.labs.samoa.instances.Instance;
import moa.AbstractMOAObject;
import moa.core.Example;
import java.util.LinkedList;
import java.util.Queue;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Pattern mining evaluator that performs basic incremental evaluation.
 *
 * @author Tomas Chovanak
 */
public class PatternsRecommendationEvaluator extends AbstractMOAObject{

    private final Queue<Double> window = new LinkedList<>();
    private int sumLCS =  0;
    private int allItems = 0;
    private int numberOfRecommendedSessions = 0;
    private int numberOfSuccessfullyRecommendedSessions = 0;
    private int realRecommendedItems = 0;
    private int windowSize = 1000;
    private double sumWindow = 0.0;
    private FileWriter writer = null;

    public PatternsRecommendationEvaluator(String outputFile)  {
        try {
            this.writer = new FileWriter(outputFile);
        } catch (IOException ex) {
            Logger.getLogger(PatternsRecommendationEvaluator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
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
            int numberOfRecommendedItems, double transsec, Integer counter) {
        double lcsVal = 0;
        double numRecommendedItems = 0;
        Instance session = (Instance)instance.getData();
        Integer sessionLength = (Integer)(session.numValues() - 2);
        allItems += numberOfRecommendedItems;
        if(recommendations != null){
            lcsVal = recommendations[0];
            numRecommendedItems = recommendations[1];
            if(numRecommendedItems > 0){
                this.sumLCS += lcsVal;
                realRecommendedItems += numRecommendedItems;
                this.numberOfRecommendedSessions += 1;
                double prec = lcsVal/numberOfRecommendedItems;
                if(prec > 0){
                    this.numberOfSuccessfullyRecommendedSessions += 1;
                }
                addNum(prec);
            }
        }
        Double restSessionElements = sessionLength - windowSize - lcsVal;
        Double prec1 = (((double)sumLCS)/((double)allItems));
        Double prec2 = (((double)sumLCS)/((double)realRecommendedItems));
        Double ma = getAvg();
        //System.out.println("TP : " + sumLCS);
        //System.out.println("ALL : " + allItems);
        //System.out.println("PRECISION : " + prec);
        //System.out.println("MOVING AVERAGE: " + getAvg());
        try {
            writer.append(counter.toString());  // transaction id
            writer.append(',');
            writer.append(sessionLength.toString());  // transaction length
            writer.append(',');
            writer.append(((Integer)(windowSize)).toString());  // test length
            writer.append(',');
            writer.append(((Double)numRecommendedItems).toString());  // number of really recommended items
            writer.append(',');
            writer.append(((Double)lcsVal).toString());  // number of hits 
            writer.append(',');
            writer.append(prec1.toString());   // summary precision 
            writer.append(',');
            writer.append(prec2.toString());   // summary precision 
            writer.append(',');
            writer.append(ma.toString());   // moving average
            writer.append(',');
            writer.append(((Integer)sumLCS).toString()); // summary LCS
            writer.append(',');
            writer.append(((Integer)allItems).toString());
            writer.append(',');
            writer.append(((Integer)realRecommendedItems).toString());
            writer.append(',');
            writer.append(((Double)transsec).toString());
            writer.append('\n');
        } catch (IOException ex) {
            Logger.getLogger(PatternsRecommendationEvaluator.class.getName()).log(Level.SEVERE, null, ex);
        }
	
    }
    
    @Override
    public void getDescription(StringBuilder sb, int i) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public double[] getResults() {
        Double prec = (((double)sumLCS)/((double)allItems));
        Double prec2 = (((double)sumLCS)/((double)realRecommendedItems));
        Double ma = getAvg();
        double[] results = new double[8];
        results[0] = prec; results[1] = ma; results[2] = sumLCS; results[3] = allItems; results[4] = realRecommendedItems;
        results[5] = prec2;
        results[6] = this.numberOfSuccessfullyRecommendedSessions;
        results[7] = this.numberOfRecommendedSessions;
        return results;
    }

    public int getAllItems() {
        return allItems;
    }

    public double getSumWindow() {
        return sumWindow;
    }

    public void addResult(Example testInst, double[] recommendations, int windowSize, int numberOfRecommendedItems, double transsec) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    
    

    
}
