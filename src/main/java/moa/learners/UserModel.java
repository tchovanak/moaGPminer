/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package moa.learners;
import java.util.*;
import com.yahoo.labs.samoa.instances.SparseInstance;
import com.yahoo.labs.samoa.instances.Instance;
import java.util.concurrent.ConcurrentHashMap;
import moa.utils.MapUtil;
/**
 *
 * @author Tomas
 */
public class UserModel {

    private int id = -1;
    private Map<Integer,Double> pageVisitsMap = new ConcurrentHashMap<Integer,Double>();
    private Map<Integer,Double> distancesToGroups = new ConcurrentHashMap<Integer,Double>();
    private int numberOfChanges = 0;
    private double groupid = 0;
    private double distance = 0;
    private boolean sorted = false;
    
    
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Map<Integer, Double> getPageVisitsMap() {
        return pageVisitsMap;
    }

    public void setPageVisitsMap(Map<Integer, Double> pageVisitsMap) {
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
    
    public void addGroup(Double groupid, Double distance){
        this.distancesToGroups.put((Integer)((int)Math.round(groupid)), distance);
    }
    
    public void clearGroupids() {
        this.sorted = false;
        this.distancesToGroups.clear();
    }
    
    
    

    public void setGroupid(Double groupid) {
        if(this.groupid != groupid && groupid != -1 && groupid != null){
            if(this.groupid != -1 && GroupCounter.groupscounters.length > (int)this.groupid){
                GroupCounter.groupscounters[(int)this.groupid]--;
            }
            if(this.groupid != -1 && GroupCounter.groupscounters.length > groupid.intValue()){
                GroupCounter.groupscounters[groupid.intValue()]++;
            }
        }
        this.groupid = groupid;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }
    
    public Instance toInstance(int numberOfDims){
        int nItems = numberOfDims;
        double[] attValues = new double[nItems];
        int[] indices = new int[nItems];
        for(int idx = 0; idx < nItems; idx++){
            attValues[idx] = 0;
            indices[idx] = idx;
        }
        for(Map.Entry<Integer,Double> entry: this.pageVisitsMap.entrySet()){
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

    public void put(int idx, double i) {
        this.numberOfChanges++;
        pageVisitsMap.put(idx,i);
    }

    public void aging() {
        Iterator it = this.pageVisitsMap.entrySet().iterator();
        while(it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            Double val =  (Double) pair.getValue();
            val = val*0.8;
            if(val < 0.5){
                this.pageVisitsMap.remove((Integer)pair.getKey());
            }else{
                this.pageVisitsMap.put((Integer)pair.getKey(), val);
            }
        }
    }

    private void sortGroupids() { 
        this.distancesToGroups = MapUtil.sortByValue(this.distancesToGroups);
        this.sorted = true;
    }
    
    public List<Integer> getGroupids() {
        this.sortGroupids();
        List<Integer> groupids = new ArrayList<Integer>();
        for(Map.Entry gid : this.distancesToGroups.entrySet()){
            groupids.add((Integer)gid.getKey());
        }
        return groupids;
    }

    

    
    
}
