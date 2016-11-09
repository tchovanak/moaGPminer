/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package moa.core.PPSDM.dto;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Tomas
 */
public class RecommendationResults {
    
    private List<Integer> recommendationsGGC = new ArrayList<>();
    private List<Integer> recommendationsGO = new ArrayList<>();
    private List<Integer> recommendationsOG = new ArrayList<>();
    private List<Integer> testWindow = new ArrayList<>();
    private Integer numOfRecommendedItems = null;

    public List<Integer> getRecommendationsGGC() {
        return recommendationsGGC;
    }

    public void setRecommendationsGGC(List<Integer> recommendationsGGC) {
        this.recommendationsGGC = recommendationsGGC;
    }

    public List<Integer> getRecommendationsGO() {
        return recommendationsGO;
    }

    public void setRecommendationsGO(List<Integer> recommendationsGO) {
        this.recommendationsGO = recommendationsGO;
    }

    public List<Integer> getRecommendationsOG() {
        return recommendationsOG;
    }

    public void setRecommendationsOG(List<Integer> recommendationsOG) {
        this.recommendationsOG = recommendationsOG;
    }

    public List<Integer> getTestWindow() {
        return testWindow;
    }

    public void setTestWindow(List<Integer> testWindow) {
        this.testWindow = testWindow;
    }

    public Integer getNumOfRecommendedItems() {
        return numOfRecommendedItems;
    }

    public void setNumOfRecommendedItems(Integer numOfRecommendedItems) {
        this.numOfRecommendedItems = numOfRecommendedItems;
    }

    public List<Integer> getFirstNRecommendationsGGC(int numRec) {
       if(this.recommendationsGGC.size() >= numRec){
           return this.recommendationsGGC.subList(0, numRec); 
        }else{
           return this.recommendationsGGC.subList(0, this.recommendationsGGC.size()); 
        }
    }

    public List<Integer> getFirstNRecommendationsGO(int numRec) {
        if(this.recommendationsGO.size() >= numRec){
           return this.recommendationsGO.subList(0, numRec); 
        }else{
           return this.recommendationsGO.subList(0, this.recommendationsGO.size()); 
        }
    }
    
    public List<Integer> getFirstNRecommendationsOG(int numRec) {
        if(this.recommendationsOG.size() >= numRec){
           return this.recommendationsOG.subList(0, numRec); 
        }else{
           return this.recommendationsOG.subList(0, this.recommendationsOG.size()); 
        }
    }
    
}
