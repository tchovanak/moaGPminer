/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package moa.core.PPSDM;
import java.util.List;
import moa.core.SemiFCI;

/**
 *  (Experimental) Class represents FCI wrapper with calculated value from its support and 
 *  intersection with current session window. It is used to sort semi fcis prioritly.
 * @author Tomas Chovanak
 */
public class FciValue implements Comparable {
    
    private double lcsVal = 0.0;
    private double support = 0.0;
    private double value = 0.0;
    private int groupid = -1;
    private SemiFCI fci = null;
    private double distance = 0.0;
    private List<Integer> items;
    
    public void computeValue(double lcsVal, double support, int preference){
        this.value = Configuration.A*lcsVal + Configuration.B*support; //- preference*Configuration.C;
        this.lcsVal = lcsVal;
        this.support = support;
       
    }

    public List<Integer> getItems() {
        return items;
    }

    public void setItems(List<Integer> items) {
        this.items = items;
    }
    public SemiFCI getFci() {
        return fci;
    }

    public void setFci(SemiFCI fci) {
        this.fci = fci;
    }
            
    public double getLcsVal() {
        return lcsVal;
    }

    public void setLcsVal(double lcsVal) {
        this.lcsVal = lcsVal;    
    }

    public double getSupport() {
        return support;
    }

    public void setSupport(double support) {
        this.support = support;
    }

    public double getValue() {
        return value;
    }

    public int getGroupid() {
        return groupid;
    }

    public void setGroupid(int groupid) {
        this.groupid = groupid;
    }

    @Override
    public int compareTo(Object o) {
        FciValue other = (FciValue)o;
       if(Configuration.SORT_STRATEGY.equals("PREFER_SUPPORT")){
            if(this.lcsVal < other.getLcsVal()){
                return 1;
            }else if(this.lcsVal == other.getLcsVal()){

                if(this.support < other.getSupport()){
                    return 1;
                }else if (this.support == other.getSupport()){
                    return 0;
                }else{
                    return -1;
                }

            }else{
                return -1;
            }
       }
       if(Configuration.SORT_STRATEGY.equals("PREFER_VALUE")){

            if(this.value < other.getValue()){
                return 1;
            }else if(this.value > other.getValue()){
                return -1;
            }else{
                return 0;
            }
       }
       return 0;
       
    }
    
    public void setDistance(double distance) {
       this.distance = distance;
    }
    
    public double getDistance(){
        return this.distance;
    }
    
}
