/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package moa.utils;

/**
 *
 * @author Tomas
 */
class Parameter {
    
    private double value;
    private double[] boundaries  = new double[3];

    public Parameter(double value,double low,double top,double step) {
        this.value = value;
        boundaries[0] = low;
        boundaries[1] = top;
        boundaries[2] = step;
    }
    
    public double[] getBoundaries(){
        return boundaries;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    @Override
    protected Parameter clone() throws CloneNotSupportedException {
        Parameter clone = new Parameter(this.value, this.boundaries[0], this.boundaries[1], this.boundaries[2]);
        return clone;
    }

   
    
   
    
}
