/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package moa.learners;
import java.util.*;
import com.yahoo.labs.samoa.instances.SparseInstance;
import com.yahoo.labs.samoa.instances.Instance;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import moa.utils.Configuration;
import moa.utils.MapUtil;
/**
 *
 * @author Tomas
 */
public class UserModel {

    private int id = -1; // user id 
    private int numOfNewSessions = 0; // number of sessions after microcluster update was performed
    private Instance currentInstance = null;
    private BlockingQueue<List<Integer>> lastSessions = 
            new ArrayBlockingQueue<>(Configuration.MAX_USER_SESSIONS_HISTORY_IN_USER_MODEL); // queue of last n user sessions
    private double groupid = 0; // actual group user is part of
    private double distance = 0; // distance to his group center
    private int clusteringId = 0;
    
    public UserModel(int id, int size){
        this.id = id;
        this.lastSessions = new ArrayBlockingQueue<>(size);
    }
    
    /*
        Updates user model with new instance representing one session
    */
    public void updateWithInstance(Instance inst) {
        List<Integer> session = new LinkedList<Integer>();
        for(int i = 2; i < inst.numValues(); i++){
            session.add((int)Math.round(inst.value(i)));
        }
        if(!lastSessions.offer(session)){
            lastSessions.poll();
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
        currentInstance = new SparseInstance(1.0,attValues,indices,nItems);
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
        UserModel um = (UserModel) obj;
        if(um.getId() == this.id){
            return true;
        }else{
            return super.equals(obj); //To change body of generated methods, choose Tools | Templates.
        }
    }    
    
}
