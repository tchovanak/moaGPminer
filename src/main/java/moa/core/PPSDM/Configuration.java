/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package moa.core.PPSDM;

import moa.core.PPSDM.enums.ClusteringMethodsEnum;
import moa.core.PPSDM.enums.DistanceMetricsEnum;
import moa.core.PPSDM.enums.RecommendStrategiesEnum;
import moa.core.PPSDM.enums.SortStrategiesEnum;

/**
 * Static configuration of PPSDM method with default values
 * @author Tomas Chovanak
 */
public class Configuration {
    /*
        Maximal time of semifci's update after segment of data
    */
    public static double MAX_UPDATE_TIME = 20000; 
    /*
        Restriction of maximal number of fci items in memory
    */
    public static double MAX_FCI_SET_COUNT = 500000; 
    /*
        Speed restriction 
    */
    public static double MIN_TRANSSEC = 5;
    /*
        Speed parameter real numbers from 0 up. 
        Higher the speed param is higher the transaction/second speed is.
    */
    public static double SPEED_PARAM = 1.0;
    /*
        From which transaction should evaluation start 
    */
    public static int START_EVALUATING_FROM = 1000;
    
    /*
        (Experimental) FciValue parameters constants 
    */
    public static double A = 0.0;
    public static double B = 0.0;
    public static double C = 0.0;
    
    /*
        (Experimental) How to compute similarity between evaluation window and existing fcis
    */
    public static String INTERSECT_STRATEGY = "LCS";
    
    /*
        Recommendation strategy BEST WINS or VOTING
    */
    public static RecommendStrategiesEnum RECOMMEND_STRATEGY = RecommendStrategiesEnum.VOTES;
    
    /*
        Strategy of sorting fcis 
    */
    public static SortStrategiesEnum SORT_STRATEGY = SortStrategiesEnum.LCSANDSUPPORT;
    
    /*
        On which transaction semi-fcis snapshot should be made
    */
    public static int EXTRACT_PATTERNS_AT = 10000;
    
    /*
        Maximum number of user sessions stored in user model history
    */
    public static int MAX_USER_SESSIONS_HISTORY_IN_USER_MODEL = 5;
    
    public static int MAX_INSTANCES_IN_MICROCLUSTER = 100;
    
    /*
        Maximum difference in clustering ids of curent clustering and clustering id 
        in user model. When difference is greater than user model is deleted.
    */
    public static int MAX_DIFFERENCE_OF_CLUSTERING_ID = 50;
    
    /*
        Distance metric used with clustering
    */
    public static DistanceMetricsEnum DISTANCE_METRIC = DistanceMetricsEnum.COSINE;
    
    /*
        Clustering module used 
    */
    public static ClusteringMethodsEnum CLUSTERING_METHOD = ClusteringMethodsEnum.DENSTREAM;
    
    public static int TRANSACTION_COUNTER = 0;
    
    public static double STREAM_START_TIME = 0;
    
    public static double START_UPDATE_TIME = 0;
    
    public static int GROUP_COUNTER = 0;
    
    public static double GROUP_CHANGES = 0;
 
    public static double GROUP_CHANGED_TIMES = 0;
    public static int DIMENSION_PAGES;
    
    
}
