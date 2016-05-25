/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.sne.classifiers;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import nl.uva.sne.commons.ClusterUtils;
import nl.uva.sne.commons.Term;
import nl.uva.sne.commons.TermFactory;
import org.apache.commons.io.FilenameUtils;
import org.json.simple.parser.ParseException;
import weka.clusterers.FilteredClusterer;
import weka.core.Instances;
import weka.filters.unsupervised.attribute.Remove;

/**
 *
 * @author S. Koulouzis
 */
public class EM implements Classifier {

    private Integer numOfClusters;

    @Override
    public void configure(Properties properties) {
        numOfClusters = Integer.valueOf(properties.getProperty("kmeans.num.of.clusters", "6"));
//        distanceFunction = properties.getProperty("distance.function", "Euclidean");
        Logger.getLogger(Hierarchical.class.getName()).log(Level.INFO, "kmeans.num.of.clusters: {0}", numOfClusters);
    }

    @Override
    public Map<String, String> cluster(String inDir) throws IOException, ParseException {
        try {
            Instances data = ClusterUtils.terms2Instances(inDir);

            weka.clusterers.EM clusterer = new weka.clusterers.EM();

            clusterer.setMaxIterations(10000);
            clusterer.setNumClusters(numOfClusters);
            clusterer.setNumKMeansRuns(10000);

            return ClusterUtils.bulidClusters(clusterer, data, inDir);

        } catch (Exception ex) {
            Logger.getLogger(EM.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;

    }

}
