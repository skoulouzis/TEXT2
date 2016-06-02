/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.sne.classifiers;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import nl.uva.sne.commons.ClusterUtils;
import org.json.simple.parser.ParseException;

import weka.clusterers.HierarchicalClusterer;
import weka.core.ChebyshevDistance;
import weka.core.DistanceFunction;
import weka.core.EuclideanDistance;
import weka.core.Instances;
import weka.core.ManhattanDistance;
import weka.core.MinkowskiDistance;

/**
 *
 * @author S. Koulouzis
 */
public class Hierarchical implements Classifier {

    private int numOfClusters;
    private String distanceFunction;

    @Override
    public void configure(Properties properties) {
        numOfClusters = Integer.valueOf(properties.getProperty("kmeans.num.of.clusters", "6"));
        distanceFunction = properties.getProperty("distance.function", "Euclidean");
        Logger.getLogger(Hierarchical.class.getName()).log(Level.INFO, "kmeans.num.of.clusters: {0}", numOfClusters);
    }

    @Override
    public Map<String, String> cluster(String inDir) throws IOException, ParseException {
        try {

            Instances data = ClusterUtils.terms2Instances(inDir,false);

            DistanceFunction df;
//            SimpleKMeans currently only supports the Euclidean and Manhattan distances.
            switch (distanceFunction) {
                case "Minkowski":
                    df = new MinkowskiDistance(data);
                    break;
                case "Euclidean":
                    df = new EuclideanDistance(data);
                    break;
                case "Chebyshev":
                    df = new ChebyshevDistance(data);
                    break;
                case "Manhattan":
                    df = new ManhattanDistance(data);
                    break;
                default:
                    df = new EuclideanDistance(data);
                    break;
            }

            Logger.getLogger(Hierarchical.class.getName()).log(Level.INFO, "Start clusteing");
            

            weka.clusterers.HierarchicalClusterer clusterer = new HierarchicalClusterer();
            clusterer.setOptions(new String[]{"-L", "COMPLETE"});
            clusterer.setDebug(true);
            clusterer.setNumClusters(numOfClusters);
            clusterer.setDistanceFunction(df);
            clusterer.setDistanceIsBranchLength(true);
            clusterer.setPrintNewick(false);
            
            return ClusterUtils.bulidClusters(clusterer, data, inDir);

        } catch (Exception ex) {
            Logger.getLogger(Hierarchical.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

}
