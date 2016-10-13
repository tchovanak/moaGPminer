/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package moa.utils;

/**
 *
 * @author Tomas Chovanak
 */
public class Configuration {
    public static double MAX_UPDATE_TIME = 20000; // in ms
    public static double MAX_FCI_SET_COUNT = 50000;
    public static double MIN_TRANSSEC = 5;
    public static double DESIRED_TRANSSEC = 5;
    public static double MAX_VOTES = 1000;
    public static double A = 0.0;
    public static double B = 0.0;
    public static double C = 0.0;
    public static String INTERSECT_STRATEGY = "LCS";
    //public static String RECOMMEND_STRATEGY = "VOTES";
    public static RecommendStrategiesEnum RECOMMEND_STRATEGY = RecommendStrategiesEnum.VOTES;
    public static SortStrategiesEnum SORT_STRATEGY = SortStrategiesEnum.LCSANDSUPPORT;
}
