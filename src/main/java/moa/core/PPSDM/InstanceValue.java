/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package moa.core.PPSDM;

/**
 *
 * @author Tomas
 */
public class InstanceValue implements Comparable {
    
    private int index = 0;
    private double val = 0.0;

    public InstanceValue(int ind) {
        index = ind;
    }
    
    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public double getVal() {
        return val;
    }

    public void setVal(double val) {
        this.val = val;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final InstanceValue other = (InstanceValue) obj;
        if (this.index != other.index) {
            return false;
        }
        return true;
    }

    @Override
    public int compareTo(Object o) {
        if (o == null) {
            return 1;
        }
        if (getClass() != o.getClass()) {
            return 1;
        }
        final InstanceValue other = (InstanceValue) o;
        if(this.index > other.index){
            return 1;
        }else if(this.index == other.index){
            return 0;
        }else{
            return -1;
        }
    }
    
    
    
}
