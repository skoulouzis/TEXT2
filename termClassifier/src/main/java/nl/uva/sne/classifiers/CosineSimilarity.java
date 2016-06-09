/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.sne.classifiers;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.didion.jwnl.JWNLException;
import nl.uva.sne.commons.ClusterUtils;
import nl.uva.sne.commons.FileUtils;
import nl.uva.sne.commons.SemanticUtils;
import nl.uva.sne.commons.Term;
import org.json.simple.parser.ParseException;
import weka.core.DistanceFunction;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.NormalizableDistance;
import weka.core.Option;
import weka.core.neighboursearch.PerformanceStats;

/**
 *
 * @author S. Koulouzis
 */
public class CosineSimilarity implements Classifier {

    @Override
    public void configure(Properties properties) {

    }

    @Override
    public void trainModel(String trainDataDir, String outDir) throws IOException, ParseException, MalformedURLException {
        try {
            File dir = new File(trainDataDir);
            File[] classFolders = dir.listFiles();

            Map<String, Set<String>> classes = new HashMap<>();
            List<List<String>> allDocs = new ArrayList<>();
            for (File f : classFolders) {
                if (f.isDirectory()) {
                    List<Term> terms = ClusterUtils.dir2Terms(f.getAbsolutePath());
                    Set<String> set = new HashSet<>();
                    for (Term tv : terms) {
                        Set<String> doc = SemanticUtils.getDocument(tv);
                        allDocs.add(new ArrayList<>(doc));
                        set.addAll(doc);
                    }
                    classes.put(f.getName(), set);
                }

            }

            Map<String, Map<String, Double>> featureVectors = new HashMap<>();
            for (String k : classes.keySet()) {
                Set<String> doc = classes.get(k);
                Map<String, Double> featureVector = new TreeMap<>();
                for (String term : doc) {

                    if (!featureVector.containsKey(term)) {
                        List<String> listDoc = new ArrayList<>();
                        listDoc.addAll(doc);
                        double score = SemanticUtils.tfIdf(listDoc, allDocs, term);
                        featureVector.put(term, score);
                    }
                }
                featureVectors.put(k, featureVector);
            }

            for (String className : featureVectors.keySet()) {
                Map<String, Double> vector = featureVectors.get(className);
                FileUtils.writeDictionary2File(vector, outDir + File.separator + className + ".csv");
            }
        } catch (JWNLException ex) {
            Logger.getLogger(CosineSimilarity.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public Map<String, String> classify(String model, String dataDir) throws IOException, ParseException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
