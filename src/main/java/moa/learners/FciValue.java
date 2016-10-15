/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package moa.learners;
import java.util.logging.Level;
import java.util.logging.Logger;
import moa.core.SemiFCI;
import moa.utils.Configuration;
/**
 *
 * @author Tomas
 */
class FciValue implements Comparable {
    
    private double lcsVal = 0.0;
    private double support = 0.0;
    private double value = 0.0;
    public boolean groupFlag = false;
    private int groupid = -1;
    private int preference = 0;
    private SemiFCI fci = null;
    private double minSupport = 0.01;

    
    
    public void computeValue(double lcsVal, double support, int preference, double minSupport){
        this.value = Configuration.A*lcsVal + Configuration.B*support; //- preference*Configuration.C;
        this.lcsVal = lcsVal;
        this.support = support;
        this.minSupport = minSupport;
    }
    
    public SemiFCI getFci() {
        try {
            return (SemiFCI) fci.clone();
        } catch (CloneNotSupportedException ex) {
            Logger.getLogger(FciValue.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
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
//       FciValue other = (FciValue)o;
//       if(this.lcsVal < other.getLcsVal()){
//           return -1;
//       }else if(this.lcsVal == other.getLcsVal()){
//           if(this.support < other.getSupport()){
//               return -1;
//           }else if(this.support == other.getSupport()){
//               return 0;
//           }else{
//               return 1;
//           }
//       }else{
//           return 1;
//       }
//       if(Configuration.SORT_STRATEGY == "PREFER_GLOBAL"){
//               
//       }
//       if(Configuration.SORT_STRATEGY == "PREFER_GROUP"){
//               
//       }
//       if(Configuration.SORT_STRATEGY == "PREFER_SUPPORT"){
//               
//       }
         //if(Configuration.SORT_STRATEGY == "PREFER_SUPPORT"){
//               
//       }
       // 2nd option

       if(Configuration.SORT_STRATEGY.equals("PREFER_SUPPORT")){
//            if(this.support > minSupport && other.getSupport() < minSupport){
//                return -1;
//            }else if(this.support < minSupport && other.getSupport() > minSupport){
//                return 1;
//            }
            if(this.lcsVal < other.getLcsVal()){
                return 1;
            }else if(this.lcsVal == other.getLcsVal()){
//                     if(this.preference > other.getPreference()){
//                         return 1;
//                     }else if(this.preference == other.getPreference()){
                if(this.support < other.getSupport()){
                    return 1;
                }else if (this.support == other.getSupport()){
                    return 0;
                }else{
                    return -1;
                }
//                     }else{
//                         return -1;
//                     }
            }else{
                return -1;
            }
       }
       
       if(Configuration.SORT_STRATEGY.equals("PREFER_VALUE")){
//            if(this.support > minSupport && other.getSupport() < minSupport){
//                return -1;
//            }else if(this.support < minSupport && other.getSupport() > minSupport){
//                return 1;
//            }
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

    public void setGroupFciFlag(boolean b) {
        this.groupFlag = b;
    }
    
    public boolean getGroupFciFlag(){
        return this.groupFlag;
    }

    public int getPreference() {
        return preference;
    }

    public void setPreference(int preference) {
        this.preference = preference;
    }

    void computeValue(double lcsVal, double support) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    

    
    
}
