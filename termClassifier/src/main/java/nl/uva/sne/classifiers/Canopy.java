package nl.uva.sne.classifiers;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import nl.uva.sne.commons.ClusterUtils;
import org.json.simple.parser.ParseException;
import weka.clusterers.ClusterEvaluation;
import weka.clusterers.FilteredClusterer;
import weka.core.Instances;
import weka.filters.unsupervised.attribute.Remove;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author S. Koulouzis
 */
public class Canopy implements Classifier {

    private Integer numOfClusters;

    @Override
    public void configure(Properties properties) {
        numOfClusters = Integer.valueOf(properties.getProperty("kmeans.num.of.clusters", "6"));
    }

    @Override
    public Map<String, String> cluster(String inDir) throws IOException, ParseException {
        try {
            Instances data = ClusterUtils.terms2Instances(inDir,false);

            weka.clusterers.Canopy clusterer = new weka.clusterers.Canopy();

            clusterer.setNumClusters(numOfClusters);

            return ClusterUtils.bulidClusters(clusterer, data, inDir);

        } catch (Exception ex) {
            Logger.getLogger(Canopy.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

}
