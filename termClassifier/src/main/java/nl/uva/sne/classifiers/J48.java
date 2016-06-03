/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.sne.classifiers;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import nl.uva.sne.commons.ClusterUtils;
import org.json.simple.parser.ParseException;
import weka.classifiers.meta.FilteredClassifier;
import weka.core.Instance;
import weka.core.Instances;
import weka.filters.unsupervised.attribute.Remove;

/**
 *
 * @author S. Koulouzis
 */
public class J48 implements Classifier {

    @Override
    public void configure(Properties properties) {

    }

    @Override
    public void trainModel(String trainDataDir, String outDir) throws IOException, ParseException {

        try {
            Instances trainData = ClusterUtils.terms2Instances(trainDataDir, true);

            weka.classifiers.trees.J48 j48 = new weka.classifiers.trees.J48();
            j48.setUnpruned(true);

            ClusterUtils.train(j48, trainData, outDir);

            Logger.getLogger(J48.class.getName()).log(Level.INFO, "Model saved in {0}", outDir);
        } catch (Exception ex) {
            Logger.getLogger(J48.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public Map<String, String> classify(String model, String dataDir) throws IOException, ParseException {
        try {
            weka.classifiers.trees.J48 cls = (weka.classifiers.trees.J48) weka.core.SerializationHelper.read(model);
            
            Instances testData = ClusterUtils.terms2Instances(dataDir, true);
            
             return ClusterUtils.classify(testData,cls);
            
      
        } catch (Exception ex) {
            Logger.getLogger(J48.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

}
