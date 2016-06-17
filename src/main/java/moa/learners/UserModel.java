/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package moa.learners;
import java.util.*;
import com.yahoo.labs.samoa.instances.SparseInstance;
import com.yahoo.labs.samoa.instances.Instance;
/**
 *
 * @author Tomas
 */
public class UserModel {

    private int id = -1;
    private Map<Integer,Integer> pageVisitsMap = new HashMap<Integer,Integer>();
    private int numberOfChanges = 0;
    private double groupid = 0;
    
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Map<Integer, Integer> getPageVisitsMap() {
        return pageVisitsMap;
    }

    public void setPageVisitsMap(Map<Integer, Integer> pageVisitsMap) {
        this.pageVisitsMap = pageVisitsMap;
    }

    public int getNumberOfChanges() {
        return numberOfChanges;
    }

    public void setNumberOfChanges(int numberOfChanges) {
        this.numberOfChanges = numberOfChanges;
    }

    public double getGroupid() {
        return groupid;
    }

    public void setGroupid(double groupid) {
        this.groupid = groupid;
    }
    
    
    
    
    public Instance toInstance(int numberOfDims){
        int nItems = numberOfDims;
        double[] attValues = new double[nItems];
        int[] indices = new int[nItems];
        for(int idx = 0; idx < nItems; idx++){
            attValues[idx] = 0;
            indices[idx] = idx;
        }
        for(Map.Entry<Integer,Integer> entry: this.pageVisitsMap.entrySet()){
            if(entry.getKey() < nItems){
                attValues[entry.getKey()] = entry.getValue(); 
            }
        }
        Instance umInstance = new SparseInstance(1.0,attValues,indices,nItems);
        return umInstance;
    }
    
    @Override
    public boolean equals(Object obj) {
        UserModel um = (UserModel) obj;
        if(um.getId() == this.id){
            return true;
        }else{
            return super.equals(obj); //To change body of generated methods, choose Tools | Templates.
        }
    }

    void put(int idx, int i) {
        this.numberOfChanges++;
        pageVisitsMap.put(idx,i);
    }
    
}
