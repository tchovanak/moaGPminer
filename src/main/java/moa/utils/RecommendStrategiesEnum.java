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
public enum RecommendStrategiesEnum {
    
    VOTES(1), FIRST_WINS(2);

    private int stratNo;

    private static Map<Integer, RecommendStrategiesEnum> map = 
            new HashMap<>();

    static {
        for (RecommendStrategiesEnum stratEnum : RecommendStrategiesEnum.values()) {
            map.put(stratEnum.stratNo, stratEnum);
        }
    }

    private RecommendStrategiesEnum(final int strat) { stratNo = strat; }

    public static RecommendStrategiesEnum valueOf(int stratNo) {
        return map.get(stratNo);
    }
    
    public int value() {
        return stratNo;
    }

}
