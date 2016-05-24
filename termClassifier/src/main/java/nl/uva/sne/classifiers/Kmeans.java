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
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.didion.jwnl.JWNLException;
import nl.uva.sne.commons.ClusterUtils;
import nl.uva.sne.commons.SemanticUtils;
import nl.uva.sne.commons.Term;
import nl.uva.sne.commons.TermFactory;
import org.apache.commons.io.FilenameUtils;
import org.json.simple.parser.ParseException;
import weka.classifiers.meta.FilteredClassifier;
import weka.clusterers.FilteredClusterer;
import weka.clusterers.SimpleKMeans;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.DistanceFunction;
import weka.core.EuclideanDistance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.ManhattanDistance;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Normalize;
import weka.filters.unsupervised.attribute.Remove;
import weka.filters.unsupervised.attribute.RemoveType;

/**
 *
 * @author S. Koulouzis
 */
public class Kmeans implements Classifier {

    private int numOfClusters;
    private String distanceFunction;

    @Override
    public void configure(Properties properties) {
        numOfClusters = Integer.valueOf(properties.getProperty("kmeans.num.of.clusters", "6"));
        distanceFunction = properties.getProperty("distance.function", "Euclidean");
        Logger.getLogger(Kmeans.class.getName()).log(Level.INFO, "kmeans.num.of.clusters: {0}", numOfClusters);
    }

    @Override
    public Map<String, String> cluster(String inDir) throws IOException, ParseException {
        try {

            File dir = new File(inDir);

            List<List<String>> allDocs = new ArrayList<>();
            Map<String, List<String>> docs = new HashMap<>();
            List<Term> terms = new ArrayList<>();
            Logger.getLogger(Kmeans.class.getName()).log(Level.INFO, "Create terms");
            for (File f : dir.listFiles()) {
                if (FilenameUtils.getExtension(f.getName()).endsWith("json")) {
                    terms.add(TermFactory.create(new FileReader(f)));
                }
            }
            Instances data = ClusterUtils.terms2Instances(terms);
//
//            Logger.getLogger(Kmeans.class.getName()).log(Level.INFO, "Create documents");
//            for (Term tv : terms) {
//                try {
//                    Set<String> doc = SemanticUtils.getDocument(tv);
//                    allDocs.add(new ArrayList<>(doc));
//                    docs.put(tv.getUID(), new ArrayList<>(doc));
//                } catch (JWNLException ex) {
//                    Logger.getLogger(Kmeans.class.getName()).log(Level.SEVERE, null, ex);
//                }
//            }
//
//            Logger.getLogger(Kmeans.class.getName()).log(Level.INFO, "Extract features");
//            Set<String> allWords = new HashSet<>();
//            Map<String, Map<String, Double>> featureVectors = new HashMap<>();
//            for (String k : docs.keySet()) {
//                List<String> doc = docs.get(k);
//                Map<String, Double> featureVector = new TreeMap<>();
//                for (String term : doc) {
//                    allWords.add(term);
//
//                    if (!featureVector.containsKey(term)) {
//                        double score = SemanticUtils.tfIdf(doc, allDocs, term);
//                        featureVector.put(term, score);
//                    }
//                }
//                featureVectors.put(k, featureVector);
//            }
//            for (String t : featureVectors.keySet()) {
//                Map<String, Double> featureV = featureVectors.get(t);
//                for (String word : allWords) {
//                    if (!featureV.containsKey(word)) {
//                        featureV.put(word, 0.0);
//                    }
//                }
////                System.err.println(t + " " + featureV);
//                featureVectors.put(t, featureV);
//            }
//            ArrayList<Attribute> attributes = new ArrayList<>();
//            for (String t : allWords) {
//                attributes.add(new Attribute(t));
//            }
//
//            Logger.getLogger(Kmeans.class.getName()).log(Level.INFO, "Create Instances");
//            Instances data = new Instances("Rel", attributes, terms.size());
//           
////            Map<String, Instance> instancesMap = new HashMap();
//            for (String t : featureVectors.keySet()) {
//                Map<String, Double> featureV = featureVectors.get(t);
//                Instance inst = new DenseInstance(featureV.size());
//                int index = 0;
//                for (String w : featureV.keySet()) {
//                    inst.setValue(index, featureV.get(w));
//                    index++;
//                }
//                data.add(inst);
////                instancesMap.put(t, inst);
//            }

            Logger.getLogger(Kmeans.class.getName()).log(Level.INFO, "Normalize vectors");
            Normalize filter = new Normalize();
            filter.setInputFormat(data);
            data = Filter.useFilter(data, filter);
            DistanceFunction df;

//            SimpleKMeans currently only supports the Euclidean and Manhattan distances.
            switch (distanceFunction) {
                case "Euclidean":
                    df = new EuclideanDistance(data);
                    break;
                case "Manhattan":
                    df = new ManhattanDistance(data);
                    break;
                default:
                    df = new EuclideanDistance(data);
                    break;
            }

            FilteredClusterer fc = new FilteredClusterer();
            SimpleKMeans kmeans = new SimpleKMeans();
            String[] options = new String[2];
            options[0] = "-R"; // "range"
            options[1] = "1"; // we want to ignore the attribute that is in the position '1'
            Remove remove = new Remove(); // new instance of filter
            remove.setOptions(options); // set options

            remove.setInputFormat(data); // inform filter about dataset
            fc.setFilter(remove); //add filter to remove attributes
            fc.setClusterer(kmeans); //bind FilteredClusterer to original clusterer

            Random rand = new Random(System.currentTimeMillis());
            int seed = rand.nextInt((Integer.MAX_VALUE - 1000000) + 1) + 1000000;
            kmeans.setSeed(seed);
            kmeans.setMaxIterations(1000000000);
            Logger.getLogger(Kmeans.class.getName()).log(Level.INFO, "Start clusteing");
            kmeans.setPreserveInstancesOrder(true);

            kmeans.setNumClusters(numOfClusters);
            kmeans.setDistanceFunction(df);
            fc.buildClusterer(data);
// This array returns the cluster number (starting with 0) for each instance
// The array has as many elements as the number of instances
//            int[] assignments = kmeans.getAssignments();

//            for (String s : instancesMap.keySet()) {
//                Instance in = instancesMap.get(s);
//                int theClass = kmeans.clusterInstance(in);
//                System.err.println(s + " is in " + theClass);
//                clusters.put(inDir + File.separator + s, String.valueOf(theClass));
//            }
//            int j = 0;
//            for (int clusterNum : assignments) {
//                System.out.printf("Instance %d -> Cluster %d \n", j, clusterNum);
//                Instance ins = data.get(j);
//                j++;
//            }
            Map<String, String> clusters = new HashMap<>();
            for (int i = 0; i < data.numInstances(); i++) {
                Instance inst = data.instance(i);
//                Attribute att = inst.attribute(0);
//                System.err.println(inst.attribute(0).value(0));
                int theClass = fc.clusterInstance(inst);
                String s = data.attribute(0).value(i);
                clusters.put(inDir + File.separator + s, String.valueOf(theClass));
                System.err.println(s + " is in cluster " + theClass);

            }

            return clusters;

        } catch (Exception ex) {
            Logger.getLogger(Kmeans.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

}
