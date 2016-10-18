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
 * @author Tomas Chovanak
 */
public class PatternsRecommendationEvaluator extends AbstractMOAObject{

    private final Queue<Double> window = new LinkedList<>();
    private int maxRecommendedItems = 0; 
    
    private int ggHits =  0;  // all items hitted by group + global patterns
    private int ggRealRecommendedItems = 0; // number of items that were really recommended to user
    private int ggHitsGlobal =  0;  // all items hitted by group + global patterns
    private int ggHitsGroup =  0;  // all items hitted by group + global patterns
    
    private int goHits =  0;  // all items hitted by only global patterns
    private int goRealRecommendedItems = 0; // number of items that were really recommended to user
    
    private int ogHits =  0;  // all items hitted by only group patterns
    private int ogRealRecommendedItems = 0; // number of items that were really recommended to user
    
    private String outputFile;
    private FileWriter writer = null;

    public PatternsRecommendationEvaluator(String outputFile)  {
        this.outputFile = outputFile;
        try {
            this.writer = new FileWriter(outputFile);
            writer.append("TRANSACTION ID");writer.append(',');  // transaction id
            writer.append("TRANSACTION LENGTH");writer.append(',');  // transaction length
            writer.append("TEST LENGTH");writer.append(',');  // evaluation test length
            writer.append("GG GLOBAL INDIVIDUAL HITS");writer.append(',');  // number of hits from global
            writer.append("GG GROUP INDIVIDUAL HITS");writer.append(',');  // number of hits from group
            writer.append("GO INDIVIDUAL HITS"); writer.append(',');  // number of hits from global
            writer.append("OG INDIVIDUAL HITS"); writer.append(',');  // number of hits from group
            writer.append("GG SUM ALL HITS");writer.append(',');  // number of hits from global + group
            writer.append("GG SUM REAL RECOMMENDED");writer.append(',');  // number of hits from only group
            writer.append("GO SUM ALL HITS");writer.append(',');  // number of hits from global + group
            writer.append("GO SUM REAL RECOMMENDED");writer.append(',');  // number of hits from only group
            writer.append("OG SUM ALL HITS");writer.append(',');  // number of hits from global + group
            writer.append("OG SUM REAL RECOMMENDED");writer.append(',');  // number of hits from only group
            writer.append("MAX RECOMMENDED ITEMS");writer.append(','); // number of items that should be recommended maxi
            writer.append("TRANSSEC");writer.append('\n');
            writer.flush();
        } catch (IOException ex) {
            Logger.getLogger(PatternsRecommendationEvaluator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void writeResultsToFile(Integer counter, Integer sessionLength, Integer windowSize,
                                    Double lggGlobalHits, Double lggGroupHits, Double lgoHits,
                                    Double logHits, Double transsec) {
        try {
            if(this.writer == null)
                this.writer = new FileWriter(outputFile);
            writer.append(counter.toString());writer.append(',');  // transaction id
            writer.append(sessionLength.toString());writer.append(',');  // transaction length
            writer.append(((Integer)(sessionLength - windowSize)).toString());writer.append(',');  // test length
            writer.append((lggGlobalHits).toString());writer.append(',');  // number of really recommended items
            writer.append((lggGroupHits).toString());writer.append(',');  // number of hits from global
            writer.append((lgoHits).toString()); writer.append(','); // number of hits from group
            writer.append((logHits).toString()); writer.append(',');  // number of hits from global
            writer.append(((Integer)ggHits).toString());writer.append(',');  // number of hits from group
            writer.append(((Integer)ggRealRecommendedItems).toString());writer.append(',');  // number of hits from group
            writer.append(((Integer)goHits).toString());writer.append(',');  // number of hits from group
            writer.append(((Integer)goRealRecommendedItems).toString());writer.append(',');  // number of hits from group
            writer.append(((Integer)ogHits).toString());writer.append(',');  // number of hits from group
            writer.append(((Integer)ogRealRecommendedItems).toString()); writer.append(',');  // number of hits from group
            writer.append(((Integer)maxRecommendedItems).toString()); writer.append(','); // number of hits from group
            writer.append((transsec).toString());writer.append('\n');
            writer.flush();
        } catch (IOException ex) {
            Logger.getLogger(PatternsRecommendationEvaluator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    
    public void addResult(Example instance, double[] recommendations, int windowSize, 
            int numberOfRecommendedItems, double transsec, Integer counter) {
        double lggGlobalHits = 0;
        double lggGroupHits = 0;
        double lggAllHits = 0;
        double lggCnt = 0;
        double lgoHits = 0;
        double lgoCnt = 0;
        double logHits = 0;
        double logCnt = 0;
        Instance session = (Instance)instance.getData();
        Integer sessionLength = (Integer)(session.numValues() - 2);
        this.maxRecommendedItems += numberOfRecommendedItems;
        if(recommendations != null){
            lggGlobalHits = recommendations[0];
            lggGroupHits = recommendations[1];
            lggAllHits =  recommendations[2];
            lggCnt =  recommendations[3];
            lgoHits = recommendations[4];
            lgoCnt = recommendations[5];
            logHits = recommendations[6];
            logCnt = recommendations[7];
            if(lggCnt > 0){
                this.ggHits += lggAllHits;
                this.ggHitsGlobal += lggGlobalHits;
                this.ggHitsGroup += lggGroupHits;
                this.ggRealRecommendedItems += lggCnt;
            }
            if(lgoCnt > 0){
                this.goHits += lgoHits;
                this.goRealRecommendedItems += lgoCnt;
            }
            if(logCnt > 0){
                this.ogHits += logHits;
                this.ogRealRecommendedItems += logCnt;
            }
        }   
        writeResultsToFile(counter, sessionLength,(Integer)windowSize, 
                (Double)lggGlobalHits,(Double)lggGroupHits,(Double)lgoHits,
                (Double)logHits, transsec);
        
	
    }
    
    public double[] getResults() {
        double[] results = new double[9];
        results[0] = ggHitsGlobal;
        results[1] = ggHitsGroup;
        results[2] = ggHits;
        results[3] = ggRealRecommendedItems;
        results[4] = goHits;
        results[5] = goRealRecommendedItems;
        results[6] = ogHits;
        results[7] = ogRealRecommendedItems;
        results[8] = maxRecommendedItems;
        try {
            writer.close();
            writer = null;
        } catch (IOException ex) {
            Logger.getLogger(PatternsRecommendationEvaluator.class.getName()).log(Level.SEVERE, null, ex);
        }
        return results;
    }
    
    @Override
    public void getDescription(StringBuilder sb, int i) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    public void addResult(Example testInst, double[] recommendations, int windowSize, int numberOfRecommendedItems, double transsec) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    
}
