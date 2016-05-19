/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.sne.commons;

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
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

/**
 *
 * @author S. Koulouzis
 */
public class ClusterUtils {

    public static Instances terms2Instances(List<Term> terms) throws IOException {
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
//                System.err.println(t + " " + featureV);
            featureVectors.put(t, featureV);
        }
        ArrayList<Attribute> attributes = new ArrayList<>();
        for (String t : allWords) {
            attributes.add(new Attribute(t));
        }

        Logger.getLogger(ClusterUtils.class.getName()).log(Level.INFO, "Create Instances");

        Instances data = new Instances("Rel", attributes, terms.size());

//        Map<String, Instance> instancesMap = new HashMap();
        for (String t : featureVectors.keySet()) {
            Map<String, Double> featureV = featureVectors.get(t);
            Instance inst = new DenseInstance(featureV.size());
            int index = 0;
            for (String w : featureV.keySet()) {
                inst.setValue(index, featureV.get(w));
                index++;
            }
            data.add(inst);
//            instancesMap.put(t, inst);
        }
        return data;
    }

}
