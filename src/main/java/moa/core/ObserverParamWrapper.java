/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package moa.core;

/**
 *
 * @author Tomas
 */
public class ObserverParamWrapper {
    private int segmentLength = 0;
    private int groupid = -1; // if -1 it is global

    public int getSegmentLength() {
        return segmentLength;
    }

    public void setSegmentLength(int segmentLength) {
        this.segmentLength = segmentLength;
    }

    public int getGroupid() {
        return groupid;
    }

    public void setGroupid(int groupid) {
        this.groupid = groupid;
    }
    
    
}
