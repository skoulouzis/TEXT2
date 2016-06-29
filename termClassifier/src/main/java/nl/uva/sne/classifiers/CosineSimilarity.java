/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.sne.classifiers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
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
import nl.uva.sne.commons.ClusterUtils;
import nl.uva.sne.commons.FileUtils;
import nl.uva.sne.commons.SemanticUtils;
import static nl.uva.sne.commons.SemanticUtils.cosineSimilarity;
import static nl.uva.sne.commons.SemanticUtils.tfIdf;
import nl.uva.sne.commons.Term;
import org.apache.commons.io.FilenameUtils;
import org.json.simple.parser.ParseException;

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
        Logger.getLogger(CosineSimilarity.class.getName()).log(Level.INFO, "trainDataDir: " + trainDataDir + " outDir: " + outDir);
        try {
            File dir = new File(trainDataDir);
            File[] classFolders = dir.listFiles();

            Map<String, Set<String>> classes = new HashMap<>();
            List<List<String>> allDocs = new ArrayList<>();
            for (File f : classFolders) {
                Logger.getLogger(CosineSimilarity.class.getName()).log(Level.INFO, "classFolder: " + f.getAbsolutePath());
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

            Logger.getLogger(CosineSimilarity.class.getName()).log(Level.INFO, "classes: " + classes);
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
                Logger.getLogger(CosineSimilarity.class.getName()).log(Level.INFO, "saving : " + outDir + File.separator + className + ".csv");
                FileUtils.writeDictionary2File(vector, outDir + File.separator + className + ".csv");
            }
        } catch (JWNLException ex) {
            Logger.getLogger(CosineSimilarity.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public Map<String, String> classify(String modelDir, String dataDirPath) throws IOException, ParseException, MalformedURLException {
//            for (String className : classFeatureVectors.keySet()) {
//                Map<String, Double> classVector = classFeatureVectors.get(className);
//                double max = Double.MIN_VALUE;
//                String winnerClass;
//                for (String docName : termVectors.keySet()) {
//                    Map<String, Double> tVector = termVectors.get(docName);
//                    Double similarity = cosineSimilarity(classVector, tVector);
//                    if (similarity > max) {
//                        max = similarity;
//                        winnerClass = className;
//                    }
//                }
//                classes.put(dataDirPath + File.separator + docName, className);
//            }
        return null;
    }

    @Override
    public void saveClusterFile(String modelDir, String dataDirPath, String filePath) throws IOException, ParseException {
        try {

            Map<String, Map<String, Double>> classesMap = buildClassesMap(modelDir, dataDirPath);

            double minScore = Double.MAX_VALUE;
            double maxScore = Double.MIN_VALUE;
            for (String docName : classesMap.keySet()) {
                StringBuilder line = new StringBuilder();
                line.append(docName).append(",");
                Map<String, Double> res = classesMap.get(docName);
                Set<String> classNames = res.keySet();
                for (String cName : classNames) {
                    Double score = res.get(cName);
                    if (score > maxScore) {
                        maxScore = score;
                    } else if (score < minScore) {
                        minScore = score;
                    }
                }
            }

            StringBuilder header = new StringBuilder();
            boolean headerSet = false;
            header.append("docName").append(",");

            try (PrintWriter out = new PrintWriter(filePath + File.separator + "resault.csv")) {
                for (String docName : classesMap.keySet()) {
                    StringBuilder line = new StringBuilder();
                    line.append(docName).append(",");
                    Map<String, Double> res = classesMap.get(docName);
                    Set<String> classNames = res.keySet();
                    for (String cName : classNames) {
                        if (!headerSet) {
                            header.append(cName).append(",");
                        }
                        Double score = res.get(cName);
                        double scaledValue = 2 + (score - minScore) * (5 - 2) / (maxScore - minScore);
//                        double scaledValue = (((maxScore - minScore) * (score - 2.0)) / (5.0 - 2.0)) + minScore;
                        line.append(Math.round(scaledValue)).append(",");
                    }
                    if (!headerSet) {
                        header.deleteCharAt(header.length() - 1);
                        header.setLength(header.length());
                        headerSet = true;
                        out.print(header + "\n");
                    }
                    line.deleteCharAt(line.length() - 1);
                    line.setLength(line.length());
//                    System.err.println(line);
                    out.print(line + "\n");
                }

            }
        } catch (JWNLException ex) {
            Logger.getLogger(CosineSimilarity.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private Map<String, Map<String, Double>> buildClassesMap(String modelDir, String dataDirPath) throws IOException, ParseException, JWNLException {
        File dir = new File(modelDir);
        File[] files = dir.listFiles();
        Map<String, Map<String, Double>> classFeatureVectors = new TreeMap<>();
        for (File featureFile : files) {
            if (FilenameUtils.getExtension(featureFile.getName()).endsWith("csv")) {
                try (BufferedReader br = new BufferedReader(new FileReader(featureFile))) {
                    String line;
                    Map<String, Double> classVector = new TreeMap<>();
                    while ((line = br.readLine()) != null) {
                        String[] parts = line.split(",");
                        String key = parts[0];
                        String val = parts[1];
//                        System.err.println(featureFile.getAbsolutePath() + " " + line);
                        classVector.put(key, Double.valueOf(val));
                    }
                    classFeatureVectors.put(FilenameUtils.removeExtension(featureFile.getName()), classVector);
                }
            }
        }

        File[] textFiles = new File(dataDirPath).listFiles();
        List<List<String>> allDocs = new ArrayList<>();
        Map<String, List<String>> docs = new HashMap<>();
        for (File f : textFiles) {
            if (FilenameUtils.getExtension(f.getName()).endsWith("txt")) {
                Logger.getLogger(CosineSimilarity.class.getName()).log(Level.INFO, "Processing {0}", f.getAbsolutePath());
                StringBuilder sb = new StringBuilder();
                try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                    for (String text; (text = br.readLine()) != null;) {
                        sb.append(text.toLowerCase()).append(" ");
                    }
                }
                Logger.getLogger(CosineSimilarity.class.getName()).log(Level.INFO, "Processing text{0}", sb.length());
                List<String> doc = SemanticUtils.tokenize(sb.toString(), true);
                allDocs.add(new ArrayList<>(doc));
                docs.put(f.getName(), new ArrayList<>(doc));
                Logger.getLogger(CosineSimilarity.class.getName()).log(Level.INFO, "Processed {0} documants", docs.size());
            }
        }
//        List<Term> terms = ClusterUtils.dir2Terms(dataDir.getAbsolutePath());
//
//        for (Term tv : terms) {
//            Set<String> doc = SemanticUtils.getDocument(tv);
//            allDocs.add(new ArrayList<>(doc));
//            docs.put(tv.getUID(), new ArrayList<>(doc));
//        }

        Map<String, Map<String, Double>> termVectors = new HashMap<>();
        for (String k : docs.keySet()) {
            Logger.getLogger(CosineSimilarity.class.getName()).log(Level.INFO, "Calculating tfidf for: {0}", k);
            List<String> doc = docs.get(k);
            Map<String, Double> featureVector = new TreeMap<>();
            for (String term : doc) {
                if (!featureVector.containsKey(term)) {
                    double tfidf = tfIdf(doc, allDocs, term);
                    featureVector.put(term, tfidf);
                }
            }
            termVectors.put(k, featureVector);
        }

        Map<String, Map<String, Double>> classesMap = new HashMap<>();
        for (String docName : termVectors.keySet()) {
            Logger.getLogger(CosineSimilarity.class.getName()).log(Level.INFO, "Setting score for: {0}", docName);
            Map<String, Double> scoreMap = new TreeMap<>();
            Map<String, Double> tVector = termVectors.get(docName);
            for (String className : classFeatureVectors.keySet()) {
                Map<String, Double> classVector = classFeatureVectors.get(className);
                Double similarity = cosineSimilarity(classVector, tVector);
                scoreMap.put(className, similarity);
            }
            classesMap.put(docName, scoreMap);
        }
        return classesMap;
    }
}
