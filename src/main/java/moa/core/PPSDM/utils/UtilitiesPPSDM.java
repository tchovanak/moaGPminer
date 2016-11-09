/*
 *    UtilitiesPPSDM.java
 *    Copyright (C) 2012 Universitat Politècnica de Catalunya
 *    @author Massimo Quadrana <max.square@gmail.com>
 *
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program; if not, write to the Free Software
 *    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package moa.core.PPSDM.utils;

import java.util.*;
import moa.core.FrequentItemset;
import moa.core.PPSDM.Configuration;
import moa.core.TimingUtils;


/*
    (Tomas Chovanak) Removed runOnStreamDriftMethod
*/
public class UtilitiesPPSDM {
    
     public static double computeLongestCommonSubset(List<Integer> list1, List<Integer> list2) {
        int M = list1.size();
        int N = list2.size();
        
        // compute length of LCS and all subproblems via dynamic programming
        double lcsVal = 0.0;
        for (Integer list11 : list1) {
            if (list2.contains(list11)) {
                lcsVal += 1.0;
            }
        }
        return lcsVal;
    }
     
     
    public static BitSet computeIntersection(List<Integer> list1, List<Integer> list2) {
        BitSet results = new BitSet();
        int i = 0;
        for (Integer item : list1) {
            if(list2.contains(item)){
                results.set(i, true);
            }else{
                results.set(i,false);
            }
            i++;
        }
        return results;
    }
    
    public static void computeLongestCommonSubsequence(List<Integer> list1, List<Integer> list2) {
        int M = list1.size();
        int N = list2.size();

        // opt[i][j] = length of LCS of x[i..M] and y[j..N]
        int[][] opt = new int[M+1][N+1];

        // compute length of LCS and all subproblems via dynamic programming
        for (int i = M-1; i >= 0; i--) {
            for (int j = N-1; j >= 0; j--) {
                if (list1.get(i).equals(list2.get(j)))
                    opt[i][j] = opt[i+1][j+1] + 1;
                else 
                    opt[i][j] = Math.max(opt[i+1][j], opt[i][j+1]);
            }
        }

        // recover LCS itself and print it to standard output
        int i = 0, j = 0;
        while(i < M && j < N) {
            if (list1.get(i).equals(list2.get(j))) {
                i++;
                j++;
            }
            else if (opt[i+1][j] >= opt[i][j+1]) i++;
            else                                 j++;
        }
    }

    public static double[] getActualTransSec() {
        long end = TimingUtils.getNanoCPUTimeOfCurrentThread();
        double tp =((double)(end - Configuration.STREAM_START_TIME) / 1e9);
        double transsec = Configuration.TRANSACTION_COUNTER/tp;
        double[] res = new double[2];
        res[0] = transsec; res[1] = tp;
        return res;
    }
    
    public static void configureMaxUpdateTime() {
        
        //long end = TimingUtils.getNanoCPUTimeOfCurrentThread();
//        double tp = Configuration.START_UPDATE_TIME/1e9 - Configuration.STREAM_START_TIME/1e9;
        double update = 
                (Configuration.TRANSACTION_COUNTER / Configuration.MIN_TRANSSEC ) - Configuration.START_UPDATE_TIME/1e9 + Configuration.STREAM_START_TIME/1e9;
        if(update < 0){
            update = 0;
        }
        Configuration.MAX_UPDATE_TIME = update;
    }
    
    public static double getUpdateProgress(){
        long end = TimingUtils.getNanoCPUTimeOfCurrentThread();
        double tp =((double)(end - Configuration.START_UPDATE_TIME) / 1e9);
        return tp/Configuration.MAX_UPDATE_TIME;
    }
    
    private UtilitiesPPSDM(){};
    
    /**
     * Computes the intersection of 2 integer ordered lists.
     * @param s1 shorter set 
     * @param s2 longer set
     * @return intersection result
     */
    public static List<Integer> intersect2orderedList(List<Integer> s1, List<Integer> s2) {
        
        if(s1.size() > s2.size())
            return intersect2orderedList(s2,s1);
        
        List<Integer> res = new ArrayList<>();
        int pos1 = 0, pos2 = 0;

        while(pos1 < s1.size() && pos2 < s2.size()) {
            
            if(s1.get(pos1).equals(s2.get(pos2))){
                res.add(s1.get(pos1));
                pos1++;
                pos2++;
            }else{
                if(s1.get(pos1) < s2.get(pos2))
                    pos1++;
                else
                    pos2++;
            }
        }

        return res;
    }
    
    
    /**
     * Computes the cumulative sum of the first k elements of the passed vector
     * @param vector integer vector
     * @param k number of elements
     * @return sum of the first k elements in vector
     */
    public static int cumSum(int[] vector, int k) {
        int sum = 0;
        
        k = k > vector.length-1 ? vector.length -1 : k;
        
        for(int i = 0; i <= k && i < vector.length ; i++)
            sum += vector[i];

        return sum;
    }
    
    public static int[] getIncMineMinSupportVector(double sigma, double r, int windowSize,int nSegments){
        
        int[] supVector = new int[windowSize];

        for(int k=0; k<windowSize; k++)
            supVector[k] =(int) Math.ceil(nSegments * sigma * ((1 - r) / windowSize * k + r));

        return supVector;
    }
    
    public static double mean(List<Double> values){
        double sum = 0;
        for(Double value:values)
            sum += value;
        
        return sum / (double) values.size();
    }
    
    public static List<FrequentItemset> last_fi_set;
    static public final long MEGABYTE_SIZE = 1024L * 1024L;
    public static double getMemoryUsage(){
        
        
        long usedMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        return (double)usedMemory / (double)MEGABYTE_SIZE;
    }
}
