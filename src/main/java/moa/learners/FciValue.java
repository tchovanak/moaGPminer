/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package moa.learners;
import moa.core.SemiFCI;
/**
 *
 * @author Tomas
 */
class FciValue implements Comparable {
    private double lcsVal = 0.0;
    private double support = 0.0;
    private double value = 0.0;
    public boolean groupFlag = false;
    private SemiFCI fci = null;

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
    
    public void computeValue(double lcsVal, double support){
        this.value = lcsVal + 0.1*support;
        this.lcsVal = lcsVal;
        this.support = support;
    }

    public double getValue() {
        return value;
    }
    
    

    @Override
    public int compareTo(Object o) {
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
       
       // 2nd option
       FciValue other = (FciValue)o;
       if(this.lcsVal < other.getLcsVal()){
           return 1;
       }else if(this.lcsVal == other.getLcsVal()){
//           if(!this.groupFlag && other.groupFlag){ // prefer not group patterns
//                return -1;
//           }else if(this.groupFlag && !other.groupFlag){
//               return 1;
//           }else{
               if(this.support < other.getSupport()){
                    return 1;
                }else if (this.support == other.getSupport()){
                    return 0;
                }else{
                    return -1;
                }
//           } 
       }else{
           return -1;
       }
       
       
//       if(this.value < other.getValue()){
//           return -1;
//       }else if(this.value > other.getValue()){
//           return 1;
//       }else{
//           return 0;
//       }
    }

    public void setGroupFciFlag(boolean b) {
        this.groupFlag = b;
    }
    
    public boolean getGroupFciFlag(){
        return this.groupFlag;
    }
    

    
    
}
