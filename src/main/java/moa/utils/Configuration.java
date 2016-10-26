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
    public static double MAX_FCI_SET_COUNT = 500000;
    public static double MIN_TRANSSEC = 5;
    public static double SPEED_PARAM = 5;
    public static double MAX_VOTES = 1000;
    public static int START_EVALUATING_FROM = 1000;
    public static double A = 0.0;
    public static double B = 0.0;
    public static double C = 0.0;
    public static String INTERSECT_STRATEGY = "LCS";
    //public static String RECOMMEND_STRATEGY = "VOTES";
    public static RecommendStrategiesEnum RECOMMEND_STRATEGY = RecommendStrategiesEnum.VOTES;
    public static SortStrategiesEnum SORT_STRATEGY = SortStrategiesEnum.LCSANDSUPPORT;
    public static int EXTRACT_PATTERNS_AT = 20000;
    public static int MAX_USER_SESSIONS_HISTORY_IN_USER_MODEL = 5;
    public static int MAX_DIFFERENCE_OF_CLUSTERING_ID = 5;
}
