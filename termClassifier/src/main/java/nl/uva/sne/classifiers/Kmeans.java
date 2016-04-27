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
import nl.uva.sne.commons.SemanticUtils;
import nl.uva.sne.commons.Term;
import nl.uva.sne.commons.TermFactory;
import org.apache.commons.io.FilenameUtils;
import org.json.simple.parser.ParseException;
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

            Logger.getLogger(Kmeans.class.getName()).log(Level.INFO, "Create documents");
            for (Term tv : terms) {
                try {
                    Set<String> doc = SemanticUtils.getDocument(tv);
                    allDocs.add(new ArrayList<>(doc));
                    docs.put(tv.getUID(), new ArrayList<>(doc));
                } catch (JWNLException ex) {
                    Logger.getLogger(Kmeans.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

            Logger.getLogger(Kmeans.class.getName()).log(Level.INFO, "Extract features");
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

            Logger.getLogger(Kmeans.class.getName()).log(Level.INFO, "Create Instances");
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
                data.add(inst);
                instancesMap.add(t);
                count++;
            }
            Logger.getLogger(Kmeans.class.getName()).log(Level.INFO, "Normalize vectors");
            Normalize filter = new Normalize();
            filter.setInputFormat(data);
            Instances dataset = Filter.useFilter(data, filter);

//        weka.clusterers.HierarchicalClusterer hc = new HierarchicalClusterer();
//        try {
//            hc.setOptions(new String[]{"-L", "COMPLETE"});
//            hc.setDebug(true);
//            hc.setNumClusters(2);
//
//            hc.setDistanceFunction(new EuclideanDistance());
//            hc.setDistanceIsBranchLength(true);
//            hc.buildClusterer(data);
//            hc.setPrintNewick(false);
//            System.out.println(hc.graph());
//            // Print Newick
//            hc.setPrintNewick(true);
//            System.out.println(hc.graph());
//            	// Let's try to show this clustered data!
//		JFrame mainFrame = new JFrame("Weka Test");
//		mainFrame.setSize(600, 400);
//		mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//		Container content = mainFrame.getContentPane();
//		content.setLayout(new GridLayout(1, 1));
//		
//		HierarchyVisualizer visualizer = new HierarchyVisualizer(hc.graph());
//		content.add(visualizer);
//		
//		mainFrame.setVisible(true);
//        } catch (Exception ex) {
//            Logger.getLogger(Kmeans.class.getName()).log(Level.SEVERE, null, ex);
//        }
            SimpleKMeans kmeans = new SimpleKMeans();
//            Random rand = new Random(System.currentTimeMillis());
//            int seed = rand.nextInt((Integer.MAX_VALUE - 100) + 1) + 100;
//            kmeans.setSeed(seed);

            Logger.getLogger(Kmeans.class.getName()).log(Level.INFO, "Start clusteing");
//important parameter to set: preserver order, number of cluster.
            kmeans.setPreserveInstancesOrder(true);

            kmeans.setNumClusters(numOfClusters);
            DistanceFunction df = null;
//            SimpleKMeans currently only supports the Euclidean and Manhattan distances.
            switch (distanceFunction) {
//                case "Minkowski":
//                    df = new MinkowskiDistance(data);
//                    break;
                case " Euclidean":
                    df = new EuclideanDistance(data);
                    break;
//                case "Chebyshev":
//                    df = new ChebyshevDistance(data);
//                    break;
                case "Manhattan":
                    df = new ManhattanDistance(data);
                    break;
                default:
                    df = new EuclideanDistance(data);
                    break;
            }

            kmeans.setDistanceFunction(df);

            kmeans.buildClusterer(dataset);
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
