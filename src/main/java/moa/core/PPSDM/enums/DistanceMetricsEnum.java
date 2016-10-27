/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package moa.core.PPSDM.enums;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Tomas Chovanak
 */
public enum DistanceMetricsEnum {
    
    EUCLIDEAN(1), PEARSON(2);

    private int distNo;

    private static Map<Integer, DistanceMetricsEnum> map = 
            new HashMap<>();

    static {
        for (DistanceMetricsEnum stratEnum : DistanceMetricsEnum.values()) {
            map.put(stratEnum.distNo, stratEnum);
        }
    }

    private DistanceMetricsEnum(final int strat) { distNo = strat; }

    public static DistanceMetricsEnum valueOf(int stratNo) {
        return map.get(stratNo);
    }
    
    public int value() {
        return distNo;
    }

}
