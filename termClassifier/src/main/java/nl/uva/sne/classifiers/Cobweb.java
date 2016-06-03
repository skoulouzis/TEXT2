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
public class Cobweb implements Clusterer {

    @Override
    public void configure(Properties properties) {
    }

    @Override
    public Map<String, String> cluster(String inDir) throws IOException, ParseException {

        try {
            Instances data = ClusterUtils.terms2Instances(inDir,false);

            weka.clusterers.Cobweb clusterer = new weka.clusterers.Cobweb();
//             Acuity is set to be the minimal standard deviation of a cluster attribute. The default value of acuity is 0.1.
// clusterer.setAcuity(0.9);
//         Cuttoff is set to be minimal category utility. The default value of Cutoff is 0.002
// clusterer.setCutoff(0.01);

            return ClusterUtils.bulidClusters(clusterer, data, inDir);

        } catch (Exception ex) {
            Logger.getLogger(Cobweb.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

}
