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
import weka.core.Instances;

/**
 *
 * @author S. Koulouzis
 */
public class J48 implements Classifier {

    @Override
    public void configure(Properties properties) {

    }

    @Override
    public Map<String, String> cluster(String inDir) throws IOException, ParseException {
        try {

            inDir = "/home/alogo/Downloads/trainData";

            Instances data = ClusterUtils.terms2Instances(inDir, true);

            weka.classifiers.trees.J48 j48 = new weka.classifiers.trees.J48();
            j48.setUnpruned(true);        // using an unpruned J48

            return ClusterUtils.bulidClusters(j48, data, inDir);

        } catch (Exception ex) {
            Logger.getLogger(J48.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

}
