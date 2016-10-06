/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package moa.utils;

import java.util.*;

/**
 *
 * @author Tomas
 */
public class LCS {
    
    
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
     
     
}
