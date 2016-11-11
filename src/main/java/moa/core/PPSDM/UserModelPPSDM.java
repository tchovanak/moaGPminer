/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package moa.core.PPSDM;
import java.util.*;
import com.yahoo.labs.samoa.instances.SparseInstance;
import com.yahoo.labs.samoa.instances.Instance;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 *
 * @author Tomas Chovanak
 */
public class UserModelPPSDM {

    private int id = -1; // user id 
    private int numOfNewSessions = 0; // number of sessions after microcluster update was performed
    private Instance currentInstance = null;
    private BlockingQueue<List<Integer>> lastSessions = 
            new ArrayBlockingQueue<>(Configuration.MAX_USER_SESSIONS_HISTORY_IN_USER_MODEL); // queue of last n user sessions
    private double groupid = -1; // actual group user is part of
    private double distance = 0; // distance to his group center
    private int clusteringId = 0;
    
    public UserModelPPSDM(int id, int size){
        this.id = id;
        this.lastSessions = new ArrayBlockingQueue<>(size);
    }
    
    /*
        Updates user model with new instance representing one session
    */
    public void updateWithInstance(Instance inst) {
        List<Integer> session = new LinkedList<>();
        for(int i = 2; i < inst.numValues(); i++){
            session.add((int)Math.round(inst.value(i)));
        }
        if(!lastSessions.offer(session)){
            lastSessions.poll();
            lastSessions.offer(session);
        }
        this.numOfNewSessions++;
    }
    
    public Integer getNumOfNewSessions(){
        return numOfNewSessions;
    }

    public Instance getNewInstance(int nItems){
        numOfNewSessions = 0;
        double[] attValues = new double[nItems];
        int[] indices = new int[nItems];
        for(int idx = 0; idx < nItems; idx++){
            attValues[idx] = 0;
            indices[idx] = idx;
        }
        for(List<Integer> session: this.lastSessions){
            for(Integer i : session){
                if(i >= attValues.length ){
                    continue;
                }
                attValues[i]++;
            }
        }
        
        for(int i = 0; i < attValues.length; i++){
            attValues[i] = (double)attValues[i];///(double)this.lastSessions.size();
        }
        
        currentInstance = new SparseInstance(1.0,attValues,indices,nItems);
        return currentInstance;
        
    }
    
    public Instance getNewSparseInstance(int nItems){
        numOfNewSessions = 0;
        List<InstanceValue> instValues = new ArrayList<>();
        for(List<Integer> session: this.lastSessions){
            for(Integer i : session){
                if(i >= nItems ){
                    continue;
                }
                int ind = instValues.indexOf(new InstanceValue(i));
                if(ind >= 0){
                    InstanceValue instVal = instValues.get(ind);
                    instVal.setVal(instVal.getVal() + 1);
                }else{
                    InstanceValue instVal = new InstanceValue(i);
                    instVal.setVal(1);
                    instValues.add(instVal); 
                }
            }
        }
        Collections.sort(instValues);
        double[] attValuesArray = new double[instValues.size()];
        for(int i = 0; i < instValues.size(); i++) attValuesArray[i] = instValues.get(i).getVal();///this.lastSessions.size();
        int[] indicesArray = new int[instValues.size()];
        for(int i = 0; i < instValues.size(); i++) indicesArray[i] = instValues.get(i).getIndex(); 
        currentInstance = new SparseInstance(1.0,attValuesArray,indicesArray,nItems);
        return currentInstance;
    }
    
    
    public Instance getInstance(){
        return this.currentInstance;
    }
    
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
    
    public double getGroupid() {
        return groupid;
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
        if(this.groupid != groupid && this.groupid != -1.0){
            Configuration.GROUP_CHANGED_TIMES += 1;
        }
        if(this.groupid != -1.0){
            Configuration.GROUP_CHANGES += 1;
        }
        this.groupid = groupid;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }
    
    public int getClusteringId(){
        return clusteringId;
    }
    
    public void setClusteringId(int id){
        this.clusteringId = id;
    }
    
    @Override
    public boolean equals(Object obj) {
        UserModelPPSDM um = (UserModelPPSDM) obj;
        if(um.getId() != this.id){
            return super.equals(obj); //To change body of generated methods, choose Tools | Templates.
        }else{
            return true;
        }
    }    

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 89 * hash + this.id;
        hash = 89 * hash + this.numOfNewSessions;
        hash = 89 * hash + Objects.hashCode(this.currentInstance);
        hash = 89 * hash + Objects.hashCode(this.lastSessions);
        hash = 89 * hash + (int) (Double.doubleToLongBits(this.groupid) ^ (Double.doubleToLongBits(this.groupid) >>> 32));
        hash = 89 * hash + (int) (Double.doubleToLongBits(this.distance) ^ (Double.doubleToLongBits(this.distance) >>> 32));
        hash = 89 * hash + this.clusteringId;
        return hash;
    }
    
}
