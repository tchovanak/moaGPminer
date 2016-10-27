/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package moa.core.PPSDM;

import java.util.Map;

/**
 *
 * @author Tomas
 */
public class GroupCounter {
    
    public static int getCountOfAllUsers(){
        int count = 0;
        for(int i = 0; i < GroupCounter.groupscounters.length; i++){
            count += GroupCounter.groupscounters[i];
        }
        return count;
    }
    
    public static int[] groupscounters; 
    
}
