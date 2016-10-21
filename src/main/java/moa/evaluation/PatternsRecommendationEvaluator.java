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
import java.util.BitSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import moa.learners.RecommendationResults;
import moa.learners.SummaryResults;
import moa.utils.LCS;

/**
 * Pattern mining evaluator that performs basic incremental evaluation. 
 * @author Tomas Chovanak
 */
public class PatternsRecommendationEvaluator extends AbstractMOAObject{

    private final Queue<Double> window = new LinkedList<>();
    private int ggSumAllHits = 0;
    private int ggSumRealRecommended = 0;
    
    private int goSumAllHits = 0;
    private int goSumRealRecommended = 0;
   
    private int ogSumAllHits = 0;
    private int ogSumRealRecommended = 0;
    
    private int maxRecommendedItems = 0;
    private int allTestedItems = 0;
    
    private double ggSumNDCG = 0;
    private double ggSumPrecision = 0;
    private double ggSumRecall = 0;
    private double goSumNDCG = 0;
    private double goSumPrecision = 0;
    private double goSumRecall = 0;
    private double ogSumNDCG = 0;
    private double ogSumPrecision = 0;
    private double ogSumRecall = 0;
    
    private int numOfTestedTransactions = 0;
    
    private String outputFile;
    private FileWriter writer = null;

    public PatternsRecommendationEvaluator(String outputFile)  {
        this.outputFile = outputFile;
        try {
            this.writer = new FileWriter(outputFile);
            writer.append("TRANSACTION ID");writer.append(',');  // transaction id
            writer.append("TOTAL LENGTH");writer.append(',');  // transaction length
            writer.append("TEST LENGTH");writer.append(',');  // evaluation test length
            writer.append("H111");writer.append(',');  // evaluation test length
            writer.append("H110");writer.append(',');  // evaluation test length
            writer.append("H101");writer.append(',');  // evaluation test length
            writer.append("H100");writer.append(',');  // evaluation test length
            writer.append("H011");writer.append(',');  // evaluation test length
            writer.append("H010");writer.append(',');  // evaluation test length
            writer.append("H001");writer.append(',');  // evaluation test length
            writer.append("H000");writer.append(',');  // evaluation test length
            writer.append("PRECISION GG");writer.append(',');  // evaluation test length
            writer.append("RECALL GG");writer.append(',');  // evaluation test length
            writer.append("NDCG GG");writer.append(',');  // evaluation test length
            writer.append("PRECISION GO");writer.append(',');  // evaluation test length
            writer.append("RECALL GO");writer.append(',');  // evaluation test length
            writer.append("NDCG GO");writer.append(',');  // evaluation test length
            writer.append("TRANSSEC");writer.append('\n');
            writer.flush();
        } catch (IOException ex) {
            Logger.getLogger(PatternsRecommendationEvaluator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void writeResultsToFile(Integer counter, Integer testSize, Integer windowSize,
                                    BitSet h111, BitSet h110,BitSet h101,
                                    BitSet h100, BitSet h011,BitSet h010,
                                    BitSet h001, BitSet h000,
                                    Double transsec) {
        try {
            if(this.writer == null)
                this.writer = new FileWriter(outputFile);
            writer.append(counter.toString());writer.append(',');  // transaction id
            writer.append(((Integer)(testSize + windowSize)).toString());writer.append(',');  // transaction length
            writer.append(testSize.toString());writer.append(',');  // test length
            writer.append(((Integer)h111.cardinality()).toString());writer.append(',');  // test length
            writer.append(((Integer)h110.cardinality()).toString());writer.append(',');  // test length
            writer.append(((Integer)h101.cardinality()).toString());writer.append(',');  // test length
            writer.append(((Integer)h100.cardinality()).toString());writer.append(',');  // test length
            writer.append(((Integer)h011.cardinality()).toString());writer.append(',');  // test length
            writer.append(((Integer)h010.cardinality()).toString());writer.append(',');  // test length
            writer.append(((Integer)h001.cardinality()).toString());writer.append(',');  // test length
            writer.append(((Integer)h000.cardinality()).toString());writer.append(',');  // test length
            Double ggprecision = ggSumPrecision / this.numOfTestedTransactions;
            writer.append((ggprecision).toString());writer.append(','); 
            Double ggrecall = ggSumRecall / this.numOfTestedTransactions;
            writer.append((ggrecall).toString());writer.append(',');
            Double ggndcg = ggSumNDCG / this.numOfTestedTransactions;
            writer.append((ggndcg).toString());writer.append(','); 
            Double goprecision = goSumPrecision / this.numOfTestedTransactions;
            writer.append((goprecision).toString());writer.append(','); 
            Double gorecall = goSumRecall / this.numOfTestedTransactions;
            writer.append((gorecall).toString());writer.append(',');
            Double gondcg = goSumNDCG / this.numOfTestedTransactions;
            writer.append((gondcg).toString());writer.append(','); 
            // number of hits from group
            writer.append((transsec).toString());writer.append('\n');
            writer.flush();
        } catch (IOException ex) {
            Logger.getLogger(PatternsRecommendationEvaluator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    
    public void addResult(RecommendationResults recs, int windowSize, 
            double transsec, Integer counter) {
        
        this.maxRecommendedItems += recs.getNumOfRecommendedItems();
        
        BitSet intersectGGC = LCS.computeIntersection(recs.getRecommendationsGGC(),recs.getTestWindow());
        BitSet intersectGO = LCS.computeIntersection(recs.getRecommendationsGO(),recs.getTestWindow());
        BitSet intersectOG = LCS.computeIntersection(recs.getRecommendationsOG(), recs.getTestWindow());
        
        BitSet h111 = (BitSet)intersectGGC.clone();
        h111.and(intersectGO);
        h111.and(intersectOG);
        
        BitSet h110 = (BitSet)intersectGO.clone();
        h110.and(intersectOG);
        h110.andNot(intersectGGC);
        
        BitSet h101 = (BitSet)intersectGO.clone();
        h101.andNot(intersectOG);
        h101.and(intersectGGC);
        
        BitSet h100 = (BitSet)intersectGO.clone();
        h100.andNot(intersectOG);
        h100.andNot(intersectGGC);
        
        BitSet h011 = ((BitSet)intersectGO.clone());
        h011.flip(0, recs.getNumOfRecommendedItems());
        h011.and(intersectOG);
        h011.and(intersectGGC);
        
        BitSet h010 = ((BitSet)intersectGO.clone());
        h010.flip(0, recs.getNumOfRecommendedItems());
        h010.and(intersectOG);
        h010.andNot(intersectGGC);
        
        BitSet h001 = ((BitSet)intersectGO.clone());
        h001.flip(0, recs.getNumOfRecommendedItems());
        h001.andNot(intersectOG);
        h001.and(intersectGGC);
        
        
        BitSet h000 = ((BitSet)intersectGO.clone());
        h000.flip(0, recs.getNumOfRecommendedItems());
        h000.andNot(intersectOG);
        h000.andNot(intersectGGC);
        
        double idcg = 1;
        for(int i = 1; i < recs.getNumOfRecommendedItems(); i++){
            idcg += 1/Math.log(i + 1);
        }
        double dcgGG = 0;
        for(int i = 0; i < intersectGGC.size();i++){
            if(i == 0 && intersectGGC.get(0)){
                dcgGG += 1;
            }else{
                if(intersectGGC.get(i)){
                    dcgGG += 1/Math.log(i + 1);
                }
            }
        }
        double dcgGO = 0;
        for(int i = 0; i < intersectGO.size();i++){
            if(i == 0 && intersectGO.get(0)){
                dcgGO += 1;
            }else{
                if(intersectGO.get(i)){
                    dcgGO += 1/Math.log(i + 1);
                }
            }
        }
        double dcgOG = 0;
        for(int i = 0; i < intersectOG.size();i++){
            if(i == 0 && intersectOG.get(0)){
                dcgOG += 1;
            }else{
                if(intersectOG.get(i)){
                    dcgOG += 1/Math.log(i + 1);
                }
            }
        }
        
        this.ggSumAllHits += intersectGGC.cardinality();
        this.ggSumRealRecommended += recs.getRecommendationsGGC().size();
        this.goSumAllHits += intersectGO.cardinality();
        this.goSumRealRecommended += recs.getRecommendationsGO().size();
        this.ogSumAllHits += intersectOG.cardinality();
        this.ogSumRealRecommended += recs.getRecommendationsOG().size();
        this.allTestedItems += recs.getTestWindow().size();
        this.numOfTestedTransactions += 1;
        this.ggSumNDCG += dcgGG/idcg;
        this.goSumNDCG += dcgGO/idcg;
        this.ogSumNDCG += dcgOG/idcg;
        this.ggSumPrecision += (double)intersectGGC.cardinality()/(double)recs.getNumOfRecommendedItems();
        this.goSumPrecision += (double)intersectGO.cardinality()/(double)recs.getNumOfRecommendedItems();
        this.ogSumPrecision += (double)intersectOG.cardinality()/(double)recs.getNumOfRecommendedItems();
        this.ggSumRecall += (double)intersectGGC.cardinality()/(double)recs.getTestWindow().size();
        this.goSumRecall += (double)intersectGO.cardinality()/(double)recs.getTestWindow().size();
        this.ogSumRecall += (double)intersectOG.cardinality()/(double)recs.getTestWindow().size();
        
        
        writeResultsToFile(counter, recs.getTestWindow().size(), windowSize,
            h111, h110, h101, h100, h011, h010, h001, h000, transsec
        );
        
	
    }
    
    public SummaryResults getResults() {
        SummaryResults results = new SummaryResults();
        results.setAllHitsGGC(ggSumAllHits);
        results.setAllHitsGO(goSumAllHits);
        results.setAllHitsOG(ogSumAllHits);
        
        double ggprecision = ggSumPrecision / this.numOfTestedTransactions;
        double ggrecall = ggSumRecall / this.numOfTestedTransactions;
        double ggF1 = computeF1(ggprecision, ggrecall);
        double ndcgGG = ggSumNDCG/numOfTestedTransactions;
        
        double goprecision = goSumPrecision / this.numOfTestedTransactions;
        double gorecall = goSumRecall / this.numOfTestedTransactions;
        double goF1 = computeF1(goprecision, gorecall);
        double ndcgGO = goSumNDCG/numOfTestedTransactions;
        
        double ogprecision = ogSumPrecision / this.numOfTestedTransactions;
        double ogrecall = ogSumRecall / this.numOfTestedTransactions;
        double ogF1 = computeF1(ogprecision, ogrecall);
        double ndcgOG = ogSumNDCG/numOfTestedTransactions;
        
        results.setAllTestedItems(allTestedItems);
        results.setAllTestedTransactions(numOfTestedTransactions);
        results.setF1GGC(ggF1);
        results.setF1GO(goF1);
        results.setF1OG(ogF1);
        results.setMaxRecommendedItems(maxRecommendedItems);
        results.setNdcgGGC(ndcgGG);
        results.setNdcgGO(ndcgGO);
        results.setNdcgOG(ndcgOG);
        results.setPrecisionGGC(ggprecision);
        results.setPrecisionGO(goprecision);
        results.setPrecisionOG(ogprecision);
        results.setRealRecommendedGGC(ggSumRealRecommended);
        results.setRealRecommendedGO(goSumRealRecommended);
        results.setRealRecommendedOG(ogSumRealRecommended);
        results.setRecallGGC(ggrecall);
        results.setRecallGO(gorecall);
        results.setRecallOG(ogrecall);
        
        try {
            writer.close();
            writer = null;
        } catch (IOException ex) {
            Logger.getLogger(PatternsRecommendationEvaluator.class.getName()).log(Level.SEVERE, null, ex);
        }
        return results;
    }
    
    private double computeF1(double p, double r) {
        double result = 0;
        result = 2*(p*r)/(p+r);
        return result;
    }
    
    @Override
    public void getDescription(StringBuilder sb, int i) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    public void addResult(Example testInst, double[] recommendations, int windowSize, int numberOfRecommendedItems, double transsec) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

   

    
}
