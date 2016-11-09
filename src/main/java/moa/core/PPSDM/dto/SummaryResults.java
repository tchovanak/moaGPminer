/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package moa.core.PPSDM.dto;

/**
 * 
 * @author Tomas Chovanak
 */
public class SummaryResults {
    
    private int[] allHitsGGC;
    private int[] realRecommendedGGC;
    private double[] precisionGGC;
    private double[] recallGGC;
    private double[] f1GGC;
    private double[] ndcgGGC;
    private int[] allHitsGO;
    private int[] realRecommendedGO;
    private double[] precisionGO;
    private double[] recallGO;
    private double[] f1GO;
    private double[] ndcgGO;
    private int[] allHitsOG;
    private int[] realRecommendedOG;
    private double[] precisionOG;
    private double[] recallOG;
    private double[] f1OG;
    private double[] ndcgOG;
    private int[] allTestedItems;
    private int[] allTestedTransactions;
    private int[] maxRecommendedItems;
    private int[] numRecommendedItems;

    public SummaryResults(int[] numRecommendedItems) {
        this.numRecommendedItems = numRecommendedItems;
    }

    public int[] getAllHitsGGC() {
        return allHitsGGC;
    }

    public void setAllHitsGGC(int[] allHitsGGC) {
        this.allHitsGGC = allHitsGGC;
    }

    public int[] getRealRecommendedGGC() {
        return realRecommendedGGC;
    }

    public void setRealRecommendedGGC(int[] realRecommendedGGC) {
        this.realRecommendedGGC = realRecommendedGGC;
    }

    public double[] getPrecisionGGC() {
        return precisionGGC;
    }

    public void setPrecisionGGC(double[] precisionGGC) {
        this.precisionGGC = precisionGGC;
    }

    public double[] getRecallGGC() {
        return recallGGC;
    }

    public void setRecallGGC(double[] recallGGC) {
        this.recallGGC = recallGGC;
    }

    public double[] getF1GGC() {
        return f1GGC;
    }

    public void setF1GGC(double[] f1GGC) {
        this.f1GGC = f1GGC;
    }

    public double[] getNdcgGGC() {
        return ndcgGGC;
    }

    public void setNdcgGGC(double[] ndcgGGC) {
        this.ndcgGGC = ndcgGGC;
    }

    public int[] getAllHitsGO() {
        return allHitsGO;
    }

    public void setAllHitsGO(int[] allHitsGO) {
        this.allHitsGO = allHitsGO;
    }

    public int[] getRealRecommendedGO() {
        return realRecommendedGO;
    }

    public void setRealRecommendedGO(int[] realRecommendedGO) {
        this.realRecommendedGO = realRecommendedGO;
    }

    public double[] getPrecisionGO() {
        return precisionGO;
    }

    public void setPrecisionGO(double[] precisionGO) {
        this.precisionGO = precisionGO;
    }

    public double[] getRecallGO() {
        return recallGO;
    }

    public void setRecallGO(double[] recallGO) {
        this.recallGO = recallGO;
    }

    public double[] getF1GO() {
        return f1GO;
    }

    public void setF1GO(double[] f1GO) {
        this.f1GO = f1GO;
    }

    public double[] getNdcgGO() {
        return ndcgGO;
    }

    public void setNdcgGO(double[] ndcgGO) {
        this.ndcgGO = ndcgGO;
    }

    public int[] getAllHitsOG() {
        return allHitsOG;
    }

    public void setAllHitsOG(int[] allHitsOG) {
        this.allHitsOG = allHitsOG;
    }

    public int[] getRealRecommendedOG() {
        return realRecommendedOG;
    }

    public void setRealRecommendedOG(int[] realRecommendedOG) {
        this.realRecommendedOG = realRecommendedOG;
    }

    public double[] getPrecisionOG() {
        return precisionOG;
    }

    public void setPrecisionOG(double[] precisionOG) {
        this.precisionOG = precisionOG;
    }

    public double[] getRecallOG() {
        return recallOG;
    }

    public void setRecallOG(double[] recallOG) {
        this.recallOG = recallOG;
    }

    public double[] getF1OG() {
        return f1OG;
    }

    public void setF1OG(double[] f1OG) {
        this.f1OG = f1OG;
    }

    public double[] getNdcgOG() {
        return ndcgOG;
    }

    public void setNdcgOG(double[] ndcgOG) {
        this.ndcgOG = ndcgOG;
    }

    public int[] getAllTestedItems() {
        return allTestedItems;
    }

    public void setAllTestedItems(int[] allTestedItems) {
        this.allTestedItems = allTestedItems;
    }

    public int[] getAllTestedTransactions() {
        return allTestedTransactions;
    }

    public void setAllTestedTransactions(int[] allTestedTransactions) {
        this.allTestedTransactions = allTestedTransactions;
    }

    public int[] getMaxRecommendedItems() {
        return maxRecommendedItems;
    }

    public void setMaxRecommendedItems(int[] maxRecommendedItems) {
        this.maxRecommendedItems = maxRecommendedItems;
    }

    public int[] getNumRecommendedItems() {
        return numRecommendedItems;
    }

    public void setNumRecommendedItems(int[] numRecommendedItems) {
        this.numRecommendedItems = numRecommendedItems;
    }
    
}
