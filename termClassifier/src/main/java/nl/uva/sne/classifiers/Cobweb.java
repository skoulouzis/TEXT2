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
import weka.clusterers.FilteredClusterer;
import weka.core.Instances;
import weka.filters.unsupervised.attribute.Remove;

/**
 *
 * @author S. Koulouzis
 */
public class Cobweb implements Classifier {

    @Override
    public void configure(Properties properties) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Map<String, String> cluster(String inDir) throws IOException, ParseException {

        try {
            Instances data = ClusterUtils.terms2Instances(inDir);

            weka.clusterers.Cobweb clusterer = new weka.clusterers.Cobweb();
            
            return ClusterUtils.bulidClusters(clusterer, data, inDir);

        } catch (Exception ex) {
            Logger.getLogger(Cobweb.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

}
