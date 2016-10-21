/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package moa.learners;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Tomas
 */
public class SummaryResults {
    
    private int allHitsGGC = 0;
    private int realRecommendedGGC = 0;
    private double precisionGGC = 0;
    private double recallGGC = 0;
    private double f1GGC = 0;
    private double ndcgGGC = 0;
    private int allHitsGO = 0;
    private int realRecommendedGO = 0;
    private double precisionGO = 0;
    private double recallGO = 0;
    private double f1GO = 0;
    private double ndcgGO = 0;
    private int allHitsOG = 0;
    private int realRecommendedOG = 0;
    private double precisionOG = 0;
    private double recallOG = 0;
    private double f1OG = 0;
    private double ndcgOG = 0;
    private int allTestedItems = 0;
    private int allTestedTransactions = 0;
    private int maxRecommendedItems = 0;

    public Integer getAllHitsGGC() {
        return allHitsGGC;
    }

    public void setAllHitsGGC(int allHitsGGC) {
        this.allHitsGGC = allHitsGGC;
    }

    public Integer getRealRecommendedGGC() {
        return realRecommendedGGC;
    }

    public void setRealRecommendedGGC(int realRecommendedGGC) {
        this.realRecommendedGGC = realRecommendedGGC;
    }
   
    public Double getPrecisionGGC() {
        return precisionGGC;
    }

    public void setPrecisionGGC(double precisionGGC) {
        this.precisionGGC = precisionGGC;
    }

    public Double getRecallGGC() {
        return recallGGC;
    }

    public void setRecallGGC(double recallGGC) {
        this.recallGGC = recallGGC;
    }

    public Double getF1GGC() {
        return f1GGC;
    }

    public void setF1GGC(double f1GGC) {
        this.f1GGC = f1GGC;
    }

    public Double getNdcgGGC() {
        return ndcgGGC;
    }

    public void setNdcgGGC(double ndcgGGC) {
        this.ndcgGGC = ndcgGGC;
    }

    public Integer getAllHitsGO() {
        return allHitsGO;
    }

    public void setAllHitsGO(int allHitsGO) {
        this.allHitsGO = allHitsGO;
    }

    public Integer getRealRecommendedGO() {
        return realRecommendedGO;
    }

    public void setRealRecommendedGO(int realRecommendedGO) {
        this.realRecommendedGO = realRecommendedGO;
    }

  

    public Double getPrecisionGO() {
        return precisionGO;
    }

    public void setPrecisionGO(double precisionGO) {
        this.precisionGO = precisionGO;
    }

    public Double getRecallGO() {
        return recallGO;
    }

    public void setRecallGO(double recallGO) {
        this.recallGO = recallGO;
    }

    public Double getF1GO() {
        return f1GO;
    }

    public void setF1GO(double f1GO) {
        this.f1GO = f1GO;
    }

    public Double getNdcgGO() {
        return ndcgGO;
    }

    public void setNdcgGO(double ndcgGO) {
        this.ndcgGO = ndcgGO;
    }

    public Integer getAllHitsOG() {
        return allHitsOG;
    }

    public void setAllHitsOG(int allHitsOG) {
        this.allHitsOG = allHitsOG;
    }

    public Integer getRealRecommendedOG() {
        return realRecommendedOG;
    }

    public void setRealRecommendedOG(int realRecommendedOG) {
        this.realRecommendedOG = realRecommendedOG;
    }

   


    public Double getPrecisionOG() {
        return precisionOG;
    }

    public void setPrecisionOG(double precisionOG) {
        this.precisionOG = precisionOG;
    }

    public Double getRecallOG() {
        return recallOG;
    }

    public void setRecallOG(double recallOG) {
        this.recallOG = recallOG;
    }

    public Double getF1OG() {
        return f1OG;
    }

    public void setF1OG(double f1OG) {
        this.f1OG = f1OG;
    }

    public Double getNdcgOG() {
        return ndcgOG;
    }

    public void setNdcgOG(double ndcgOG) {
        this.ndcgOG = ndcgOG;
    }


    public Integer getMaxRecommendedItems() {
        return maxRecommendedItems;
    }

    public void setMaxRecommendedItems(int maxRecommendedItems) {
        this.maxRecommendedItems = maxRecommendedItems;
    }

    public Integer getAllTestedTransactions() {
        return allTestedTransactions;
    }

    public void setAllTestedTransactions(int allTestedTransactions) {
        this.allTestedTransactions = allTestedTransactions;
    }

    public void getMaxRecommendedItems(int maxRecommendedItems) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public Integer getAllTestedItems() {
        return allTestedItems;
    }

    public void setAllTestedItems(int allTestedItems) {
        this.allTestedItems = allTestedItems;
    }
   
    
    
    
    
}
