/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package moa.utils;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Tomas
 */
public enum SortStrategiesEnum {
    
    LCSANDSUPPORT(1), FCIVALUE(2);

    private int stratNo;

    private static Map<Integer, SortStrategiesEnum> map = 
            new HashMap<>();

    static {
        for (SortStrategiesEnum stratEnum : SortStrategiesEnum.values()) {
            map.put(stratEnum.stratNo, stratEnum);
        }
    }

    private SortStrategiesEnum(final int strat) { stratNo = strat; }

    public static SortStrategiesEnum valueOf(int stratNo) {
        return map.get(stratNo);
    }
    
    public int value() {
        return stratNo;
    }
}
