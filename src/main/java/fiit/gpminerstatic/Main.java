/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fiit.gpminerstatic;

import weka.associations.FPGrowth;
import weka.associations.AssociationRules;
import weka.core.Instance;
import weka.core.converters.CSVLoader;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import moa.streams.SessionsFileStream;
import weka.core.Attribute;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;

/**
 *
 * @author Tomas Chovanak
 */


public class Main {
    
    public static void main(String args[]){
        ArrayList<Attribute> attributes = new ArrayList<Attribute>();
        for(int i = 0; i < 1000; i++){
            attributes.add(new Attribute(String.valueOf(i)));
        }
        // load data from file into instances 
        SessionsFileStream stream = new SessionsFileStream("g:\\workspace_GPMiner\\data\\alef_sessions_aggregated.csv");
        Instances instances = new Instances("Instances", attributes, 1000);
        Enumeration<Instance> enumer =  instances.enumerateInstances();
        while(enumer.hasMoreElements()){
            instances.add(enumer.nextElement());
        }
        try{
            // make global patterns with fpgrowth alghoritm 
            FPGrowth fp = new FPGrowth();
            fp.buildAssociations(instances);
            AssociationRules assocRules = fp.getAssociationRules();
        } catch (Exception ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        
    }
    
}
