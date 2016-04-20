/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.sne.classifiers;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
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
import nl.uva.sne.commons.SemanticUtils;
import nl.uva.sne.commons.Term;
import nl.uva.sne.commons.TermFactory;
import org.apache.commons.io.FilenameUtils;
import org.json.simple.parser.ParseException;
import weka.clusterers.SimpleKMeans;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

/**
 *
 * @author S. Koulouzis
 */
public class Kmeans implements Classifier {

    @Override
    public void configure(Properties properties) {
        String stopwordsFile = properties.getProperty("stop.words", System.getProperty("user.home")
                + File.separator + "workspace" + File.separator + "termXtraction"
                + File.separator + "etc" + File.separator + "sropwords");
        SemanticUtils.stopwordsFile = stopwordsFile;
    }

    @Override
    public Map<String, String> cluster(String inDir) throws IOException, ParseException {
        File dir = new File(inDir);

        List<List<String>> allDocs = new ArrayList<>();
        Map<String, List<String>> docs = new HashMap<>();
        List<Term> terms = new ArrayList<>();
        for (File f : dir.listFiles()) {
            if (FilenameUtils.getExtension(f.getName()).endsWith("json")) {
                terms.add(TermFactory.create(f.getAbsolutePath()));
            }
        }

        for (Term tv : terms) {
            try {
                Set<String> doc = SemanticUtils.getDocument(tv);
                allDocs.add(new ArrayList<>(doc));
                docs.put(tv.getUID(), new ArrayList<>(doc));
            } catch (JWNLException ex) {
                Logger.getLogger(Kmeans.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

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
        for (String t : allWords) {
            attributes.add(new Attribute(t));
        }

        Instances data = new Instances("Rel", attributes, terms.size());
        List<String> instancesMap = new ArrayList<>();
        int count = 0;
        for (String t : featureVectors.keySet()) {
            Map<String, Double> featureV = featureVectors.get(t);
            Instance inst = new DenseInstance(featureV.size());
            int index = 0;
            for (String w : featureV.keySet()) {
                inst.setValue(index, featureV.get(w));
                index++;
            }
//            System.err.println(count + " " + t);
            data.add(inst);
            instancesMap.add(t);
            count++;
        }

        SimpleKMeans kmeans = new SimpleKMeans();
        kmeans.setSeed(10);

        //important parameter to set: preserver order, number of cluster.
        kmeans.setPreserveInstancesOrder(true);
        try {
            kmeans.setNumClusters(6);
            kmeans.buildClusterer(data);
            // This array returns the cluster number (starting with 0) for each instance
            // The array has as many elements as the number of instances
            int[] assignments = kmeans.getAssignments();

            int i = 0;
            Map<String, String> clusters = new HashMap<>();
            for (int clusterNum : assignments) {
//                System.out.printf("Instance %d -> Cluster %d \n", i, clusterNum);
                clusters.put(inDir + File.separator + instancesMap.get(i), String.valueOf(clusterNum));
//                System.err.println(inDir + File.separator + instancesMap.get(i) + "," + clusterNum);
                i++;
            }
            return clusters;
        } catch (Exception ex) {
            Logger.getLogger(Kmeans.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

}
