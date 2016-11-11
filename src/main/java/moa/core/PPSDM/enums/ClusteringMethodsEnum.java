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
public enum ClusteringMethodsEnum {
    
    CLUSTREAM(1), DENSTREAM(2);

    private int distNo;

    private static Map<Integer, ClusteringMethodsEnum> map = 
            new HashMap<>();

    static {
        for (ClusteringMethodsEnum stratEnum : ClusteringMethodsEnum.values()) {
            map.put(stratEnum.distNo, stratEnum);
        }
    }

    private ClusteringMethodsEnum(final int strat) { distNo = strat; }

    public static ClusteringMethodsEnum valueOf(int stratNo) {
        return map.get(stratNo);
    }
    
    public int value() {
        return distNo;
    }

}
