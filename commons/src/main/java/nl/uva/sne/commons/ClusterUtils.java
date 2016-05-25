/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.sne.commons;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.didion.jwnl.JWNLException;
import weka.clusterers.FilteredClusterer;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Normalize;
import org.apache.commons.io.FilenameUtils;
import weka.clusterers.ClusterEvaluation;
import weka.clusterers.Clusterer;
import weka.filters.unsupervised.attribute.Remove;

/**
 *
 * @author S. Koulouzis
 */
public class ClusterUtils {

    public static Instances terms2Instances(String inDir) throws IOException, Exception {
        File dir = new File(inDir);

        List<Term> terms = new ArrayList<>();
        Logger.getLogger(ClusterUtils.class.getName()).log(Level.INFO, "Create terms");
        for (File f : dir.listFiles()) {
            if (FilenameUtils.getExtension(f.getName()).endsWith("json")) {
                terms.add(TermFactory.create(new FileReader(f)));
            }
        }

        Logger.getLogger(ClusterUtils.class.getName()).log(Level.INFO, "Create documents");

        List<List<String>> allDocs = new ArrayList<>();
        Map<String, List<String>> docs = new HashMap<>();
        for (Term tv : terms) {
            try {
                Set<String> doc = SemanticUtils.getDocument(tv);
                allDocs.add(new ArrayList<>(doc));
                docs.put(tv.getUID(), new ArrayList<>(doc));
            } catch (JWNLException ex) {
                Logger.getLogger(ClusterUtils.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        Logger.getLogger(ClusterUtils.class.getName()).log(Level.INFO, "Extract features");
        Set<String> allWords = new HashSet<>();
        Map<String, Map<String, Double>> featureVectors = new HashMap<>();
        for (String k : docs.keySet()) {
            List<String> doc = docs.get(k);
            Map<String, Double> featureVector = new TreeMap<>();
            for (String term : doc) {
                allWords.add(term);
                if (!featureVector.containsKey(term)) {
                    double score = SemanticUtils.tfIdf(doc, allDocs, term);
                    featureVector.put(term, score);
                }
            }
            featureVectors.put(k, featureVector);
        }

        for (String t : featureVectors.keySet()) {
            Map<String, Double> featureV = featureVectors.get(t);
            for (String word : allWords) {
                if (!featureV.containsKey(word)) {
                    featureV.put(word, 0.0);
                }
            }
            featureVectors.put(t, featureV);
        }

        ArrayList<Attribute> attributes = new ArrayList<>();
        attributes.add(new Attribute("UID", (ArrayList<String>) null));
        for (String t : allWords) {
            attributes.add(new Attribute(t));
        }

        Logger.getLogger(ClusterUtils.class.getName()).log(Level.INFO, "Create Instances");

        Instances data = new Instances("Rel", attributes, terms.size());

//        Map<String, Instance> instancesMap = new HashMap();
        for (String t : featureVectors.keySet()) {
            Map<String, Double> featureV = featureVectors.get(t);
            double[] vals = new double[data.numAttributes()];
            vals[0] = data.attribute(0).addStringValue(t);
            int index = 1;
            for (String w : featureV.keySet()) {
                vals[index] = featureV.get(w);
                index++;
            }
            data.add(new DenseInstance(1.0, vals));

        }

        Logger.getLogger(ClusterUtils.class.getName()).log(Level.INFO, "Normalize vectors");
        Normalize filter = new Normalize();
        filter.setInputFormat(data);
        data = Filter.useFilter(data, filter);
        return data;
    }

    public static Map<String, String> bulidClusters(Clusterer clusterer, Instances data, String inDir) throws Exception {

        FilteredClusterer fc = new FilteredClusterer();
        String[] options = new String[2];
        options[0] = "-R"; // "range"
        options[1] = "1"; // we want to ignore the attribute that is in the position '1'
        Remove remove = new Remove(); // new instance of filter
        remove.setOptions(options); // set options

        fc.setFilter(remove); //add filter to remove attributes
        fc.setClusterer(clusterer); //bind FilteredClusterer to original clusterer
        fc.buildClusterer(data);

        Map<String, String> clusters = new HashMap<>();
        for (int i = 0; i < data.numInstances(); i++) {
            Instance inst = data.instance(i);
            int theClass = fc.clusterInstance(inst);
            String s = data.attribute(0).value(i);
            clusters.put(inDir + File.separator + s, String.valueOf(theClass));
//            System.err.println(s + " is in cluster " + theClass);
        }
        ClusterEvaluation eval = new ClusterEvaluation();
        eval.setClusterer(fc);                                   // the cluster to evaluate
        eval.evaluateClusterer(data);                                // data to evaluate the clusterer on

        Logger.getLogger(ClusterUtils.class.getName()).log(Level.INFO, "# of clusters: {0}", eval.getNumClusters());
        Logger.getLogger(ClusterUtils.class.getName()).log(Level.INFO, "clusterResults: {0}", eval.clusterResultsToString());

        return clusters;
    }

}
