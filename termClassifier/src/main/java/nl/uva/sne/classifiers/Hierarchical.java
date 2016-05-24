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
import weka.clusterers.HierarchicalClusterer;
import weka.core.Attribute;
import weka.core.ChebyshevDistance;
import weka.core.DenseInstance;
import weka.core.DistanceFunction;
import weka.core.EuclideanDistance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.ManhattanDistance;
import weka.core.MinkowskiDistance;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Normalize;
import weka.filters.unsupervised.attribute.RemoveType;

/**
 *
 * @author S. Koulouzis
 */
public class Hierarchical implements Classifier {

    private int numOfClusters;
    private String distanceFunction;

    @Override
    public void configure(Properties properties) {
        numOfClusters = Integer.valueOf(properties.getProperty("kmeans.num.of.clusters", "6"));
        distanceFunction = properties.getProperty("distance.function", "Euclidean");
        Logger.getLogger(Hierarchical.class.getName()).log(Level.INFO, "kmeans.num.of.clusters: {0}", numOfClusters);
    }

    @Override
    public Map<String, String> cluster(String inDir) throws IOException, ParseException {
        try {
            File dir = new File(inDir);

            List<Term> terms = new ArrayList<>();
            Logger.getLogger(Hierarchical.class.getName()).log(Level.INFO, "Create terms");
            for (File f : dir.listFiles()) {
                if (FilenameUtils.getExtension(f.getName()).endsWith("json")) {
                    terms.add(TermFactory.create(new FileReader(f)));
                }
            }

//            Instances data = ClusterUtils.terms2Instances(terms);
            Logger.getLogger(Hierarchical.class.getName()).log(Level.INFO, "Create documents");
            List<List<String>> allDocs = new ArrayList<>();
            Map<String, List<String>> docs = new HashMap<>();
            for (Term tv : terms) {
                try {
                    Set<String> doc = SemanticUtils.getDocument(tv);
                    allDocs.add(new ArrayList<>(doc));
                    docs.put(tv.getUID(), new ArrayList<>(doc));
                } catch (JWNLException ex) {
                    Logger.getLogger(Hierarchical.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

            Logger.getLogger(Hierarchical.class.getName()).log(Level.INFO, "Extract features");
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

            Logger.getLogger(Hierarchical.class.getName()).log(Level.INFO, "Create Instances");

            Instances data = new Instances("Rel", attributes, terms.size());
//            
            Map<String, Instance> instancesMap = new HashMap();
            for (String t : featureVectors.keySet()) {
                Map<String, Double> featureV = featureVectors.get(t);
                Instance inst = new DenseInstance(featureV.size());
                int index = 0;
                for (String w : featureV.keySet()) {
                    inst.setValue(index, featureV.get(w));
                    index++;
                }

                data.add(inst);
                instancesMap.put(t, inst);
            }
            Logger.getLogger(Hierarchical.class.getName()).log(Level.INFO, "Normalize vectors");

            
            Normalize filter = new Normalize();
            filter.setInputFormat(data);
            data = Filter.useFilter(data, filter);

            RemoveType af = new RemoveType();
            af.setInputFormat(data);
            
            data = Filter.useFilter(data, af);
            DistanceFunction df;
//            SimpleKMeans currently only supports the Euclidean and Manhattan distances.
            switch (distanceFunction) {
                case "Minkowski":
                    df = new MinkowskiDistance(data);
                    break;
                case "Euclidean":
                    df = new EuclideanDistance(data);
                    break;
                case "Chebyshev":
                    df = new ChebyshevDistance(data);
                    break;
                case "Manhattan":
                    df = new ManhattanDistance(data);
                    break;
                default:
                    df = new EuclideanDistance(data);
                    break;
            }

            Logger.getLogger(Hierarchical.class.getName()).log(Level.INFO, "Start clusteing");
            weka.clusterers.HierarchicalClusterer hc = new HierarchicalClusterer();
            hc.setOptions(new String[]{"-L", "COMPLETE"});
            hc.setDebug(true);
            hc.setNumClusters(numOfClusters);
            

            hc.setDistanceFunction(df);
            hc.setDistanceIsBranchLength(true);
            hc.buildClusterer(data);
            hc.setPrintNewick(false);

            Map<String, String> clusters = new HashMap<>();
            for (String s : instancesMap.keySet()) {
                Instance in = instancesMap.get(s);
                int theClass = hc.clusterInstance(in);
                clusters.put(inDir + File.separator + s, String.valueOf(theClass));
            }

            return clusters;

        } catch (Exception ex) {
            Logger.getLogger(Hierarchical.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

}
