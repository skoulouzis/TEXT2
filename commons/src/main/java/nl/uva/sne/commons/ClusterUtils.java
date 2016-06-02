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
import org.carrot2.core.Document;
import org.carrot2.core.LanguageCode;
import org.json.simple.parser.ParseException;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.meta.FilteredClassifier;
import weka.classifiers.trees.J48;
import weka.clusterers.ClusterEvaluation;
import weka.clusterers.Clusterer;
import weka.filters.unsupervised.attribute.Remove;

/**
 *
 * @author S. Koulouzis
 */
public class ClusterUtils {

    public static Instances terms2Instances(String inDir, boolean setClasses) throws IOException, Exception {
        if (!setClasses) {
            return createInstances(inDir);
        } else {
            return createInstancesWithClasses(inDir);
        }

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

    public static List<org.carrot2.core.Document> terms2Documets(String inDir) throws IOException, ParseException {
        List<Term> terms = dir2Terms(inDir);
        List<org.carrot2.core.Document> docs = new ArrayList<>();
        for (Term t : terms) {
            String title = t.getLemma();
            String summary = "";
            for (String s : t.getGlosses()) {
                summary += s;
            }
            String url = t.getUrl();
            String uid = t.getUID();
            Document d = new org.carrot2.core.Document(title, summary.replaceAll("_", " "), url, LanguageCode.ENGLISH, uid);
            docs.add(d);
        }

        return docs;
    }

    private static List<Term> dir2Terms(String inDir) throws IOException, ParseException {
        File dir = new File(inDir);

        List<Term> terms = new ArrayList<>();
        Logger.getLogger(ClusterUtils.class.getName()).log(Level.INFO, "Create terms");
        for (File f : dir.listFiles()) {
            if (FilenameUtils.getExtension(f.getName()).endsWith("json")) {
                terms.add(TermFactory.create(new FileReader(f)));
            }
        }

        return terms;
    }

    public static Map<String, String> bulidClusters(Classifier classifier, Instances data, String inDir) throws Exception {

        FilteredClassifier fc = new FilteredClassifier();
        String[] options = new String[2];
        options[0] = "-R"; // "range"
        options[1] = "1"; // we want to ignore the attribute that is in the position '1'
        Remove remove = new Remove(); // new instance of filter
        remove.setOptions(options); // set options

        fc.setFilter(remove); //add filter to remove attributes
        fc.setClassifier(classifier);
        fc.buildClassifier(data);

        Map<String, String> clusters = new HashMap<>();
        for (int i = 0; i < data.numInstances(); i++) {
            Instance inst = data.instance(i);
            int predicted = (int) fc.classifyInstance(inst);
            String theClass = data.classAttribute().value(predicted);
            String id = inst.toString().split(",")[0];
//           
        }
        Evaluation eTest = new Evaluation(data);
        eTest.evaluateModel(fc, data);

        String strSummary = eTest.toSummaryString();
        System.out.println(strSummary);

        Instances testData = ClusterUtils.terms2Instances("/home/alogo/Downloads/jsonTerms", false);

        for (int i = 0; i < testData.numInstances(); i++) {
            Instance inst = testData.get(i);
            inst.setDataset(data);
        }

        for (int i = 0; i < testData.numInstances(); i++) {
            Instance inst = testData.get(i);
            String s = testData.attribute(0).value(i);
            double predicted = fc.classifyInstance(inst);
//           String id = inst.toString().split(",")[0];
            // Classify instance.
            String theClss = data.classAttribute().value((int) predicted);
            System.err.println(s + " classified as : "
                    + theClss);
            clusters.put(inDir + File.separator + s, theClss);
        }
        return clusters;
    }

    private static Instances createInstances(String inDir) throws Exception {
        List<Term> terms = dir2Terms(inDir);

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

//        for (String t : featureVectors.keySet()) {
//            Map<String, Double> featureV = featureVectors.get(t);
//            for (String word : allWords) {
//                if (!featureV.containsKey(word)) {
//                    featureV.put(word, 0.0);
//                }
//            }
//            featureVectors.put(t, featureV);
//        }
        ArrayList<Attribute> attributes = new ArrayList<>();
        attributes.add(new Attribute("UID", (ArrayList<String>) null));
        for (String t : allWords) {
            attributes.add(new Attribute(t));
        }

        Logger.getLogger(ClusterUtils.class.getName()).log(Level.INFO, "Create Instances");

        Instances data = new Instances("Rel", attributes, terms.size());

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

    private static Instances createInstancesWithClasses(String inDir) throws IOException, ParseException, Exception {
        File dir = new File(inDir);
        File[] classFolders = dir.listFiles();

        List<List<String>> allDocs = new ArrayList<>();
        Map<String, List<String>> docs = new HashMap<>();
        Set<String> classes = new HashSet<>();
        for (File f : classFolders) {
            if (f.isDirectory()) {
                List<Term> terms = dir2Terms(f.getAbsolutePath());
                classes.add(f.getName());
                for (Term tv : terms) {
                    Set<String> doc = SemanticUtils.getDocument(tv);
                    allDocs.add(new ArrayList<>(doc));
                    docs.put(tv.getUID() + "," + f.getName(), new ArrayList<>(doc));
                }
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

        ArrayList<Attribute> attributes = buildAttributes(allWords, classes);

        Instances data = new Instances("Rel", attributes, docs.size());
        data.setClassIndex(1);

        for (String t : featureVectors.keySet()) {
            String[] parts = t.split(",");
            String id = parts[0];
            String theClass = parts[1];
            int index = 0;
            double[] vals = new double[data.numAttributes()];
            vals[index] = data.attribute(0).addStringValue(id);
            index++;
            Map<String, Double> featureV = featureVectors.get(t);
            for (String w : featureV.keySet()) {
                vals[index] = featureV.get(w);
                index++;
            }
            DenseInstance inst = new DenseInstance(1.0, vals);
            inst.setDataset(data);
            inst.setClassValue(theClass);
            data.add(inst);
        }
//        System.err.println(data);
        return data;

    }

    private static ArrayList<Attribute> buildAttributes(Set<String> wordVector, Set<String> classes) {
        ArrayList<Attribute> attributes = new ArrayList<>();
        attributes.add(new Attribute("UID", (ArrayList<String>) null));
        List<String> fvClassVal = new ArrayList<>();
        for (String c : classes) {
            fvClassVal.add(c);
        }
        Attribute classAttribute = new Attribute("theClass", fvClassVal);
        attributes.add(classAttribute);

        for (String t : wordVector) {
            attributes.add(new Attribute(t));
        }

        return attributes;
    }

}
