/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.sne.classifier;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.didion.jwnl.JWNLException;
import nl.uva.sne.classifiers.Classifier;
import nl.uva.sne.commons.FileUtils;
import nl.uva.sne.commons.SemanticUtils;
import nl.uva.sne.commons.Term;
import nl.uva.sne.commons.TermFactory;
import nl.uva.sne.commons.ValueComparator;
import nl.uva.sne.extractors.JtopiaExtractor;
import nl.uva.sne.extractors.TermExtractor;
import org.apache.commons.io.FilenameUtils;
import org.json.simple.parser.ParseException;
import nl.uva.sne.classifiers.Clusterer;

/**
 *
 * @author S. Koulouzis
 */
public class Main {

    public static String propertiesPath = "cluster.properties";
    private static String props;

    public static void main(String args[]) {
        boolean cluster = false, name = false, train = false;
        String className = null;
        String inDir = null, secondInDir = null, outDir = null;

        if (args != null) {
//            for (int i = 0; i < args.length; i++) {
//           -c nl.uva.sne.classifiers.Kmeans $HOME/Downloads/jsonTerms  $HOME/Downloads/clusters
            if (args[0].equals("-c")) {
                cluster = true;
                className = args[1];
                inDir = args[2];
//                if (args.length == 4) {/home/alogo
                outDir = args[3];
//                } 
//                else if (args.length == 5) {
//                    secondInDir = args[3];
//                    outDir = args[4];
//                }

//                    break;
            }
            if (args[0].equals("-n")) {
                name = true;
                outDir = args[1];
//                    break;
            }
            if (args[0].equals("-t")) {
                train = true;
                className = args[1];
                inDir = args[2];
                outDir = args[3];
//                    break;
            }
        }
        props = args[args.length - 1];
        if (props.endsWith(".properties")) {
            propertiesPath = props;
        }
//        }

        try {
            if (cluster) {
//                className = "nl.uva.sne.classifiers.Kmeans";
//                      className = "nl.uva.sne.classifiers.Hierarchical";
//className = "nl.uva.sne.classifiers.EM";

                Class c = Class.forName(className);
                Object obj = c.newInstance();
                try {
                    Clusterer classifier = (Clusterer) obj;

                    classifier.configure(FileUtils.getProperties(propertiesPath));

                    Map<String, String> theCluster = classifier.cluster(inDir);
                    copyTerms2Clusters(theCluster, outDir);

                    nameClusterFolders(outDir);

                } catch (ClassCastException ex) {
//                    -c nl.uva.sne.classifiers.CosineSimilarity $HOME/Downloads/D2.2_Table_14CompetencesGroups $HOME/Downloads/textdocs $HOME/Downloads/D2.2_Table_14CompetencesGroups/
                    Classifier classifier = (Classifier) obj;
                    classifier.configure(FileUtils.getProperties(propertiesPath));

                    classifier.saveClusterFile(inDir, secondInDir, outDir);
                    Map<String, String> theCluster = classifier.classify(inDir, secondInDir);
                    if (theCluster != null) {
                        copyTerms2Clusters(theCluster, outDir);
                    }

                }

            }
            if (name) {
                writeClustersToOneFile(outDir);
                nameClusters(outDir);
            }
            if (train) {
//                 -t nl.uva.sne.classifiers.CosineSimilarity $HOME/Downloads/D2.2_Table_14CompetencesGroups $HOME/Downloads/D2.2_Table_14CompetencesGroups
                Class c = Class.forName(className);
                Object obj = c.newInstance();

                Classifier classifier = (Classifier) obj;
                classifier.configure(FileUtils.getProperties(propertiesPath));
                classifier.trainModel(inDir, outDir);

            }

        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | IOException | ParseException | JWNLException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static void copyTerms2Clusters(Map<String, String> cluster, String outputDir) throws IOException, ParseException {
        for (String fileName : cluster.keySet()) {
            File dir = new File(outputDir + File.separator + "Cluster-" + cluster.get(fileName));
            if (!dir.exists()) {
                org.apache.commons.io.FileUtils.forceMkdir(dir);
            }
            File file = new File(fileName + ".json");

            Term t = TermFactory.create(file);
            File term = new File(dir.getAbsolutePath() + File.separator + t.getLemma() + ".term");
            org.apache.commons.io.FileUtils.touch(term);
            org.apache.commons.io.FileUtils.copyFile(file, new File(dir.getAbsolutePath() + File.separator + file.getName()));
        }
    }

    private static void writeClustersToOneFile(String inDir, String outkeywordsFile) throws IOException, ParseException, JWNLException {
        File dir = new File(inDir);
        StringBuilder sb = new StringBuilder();
        for (File f : dir.listFiles()) {
            if (FilenameUtils.getExtension(f.getName()).endsWith("json")) {
                Term tv;
                try (FileReader fr = new FileReader(f)) {
                    tv = TermFactory.create(fr);
                }

                Set<String> doc = SemanticUtils.getDocument(tv);
                for (String s : doc) {
                    sb.append(s).append(" ");
                }
            }
        }
        try (PrintWriter out = new PrintWriter(outkeywordsFile)) {
            out.print(sb.toString());
        }

    }

    private static void writeClustersToOneFile(String clustersOutDir) throws IOException, ParseException, JWNLException {
        File dir = new File(clustersOutDir);
        for (File f : dir.listFiles()) {
            if (f.isDirectory()) {
                Logger.getLogger(Main.class.getName()).log(Level.INFO, "Processing {0}", f.getAbsolutePath());
                writeClustersToOneFile(f.getAbsolutePath(), dir.getAbsolutePath() + File.separator + " " + f.getName() + ".txt");
            }
        }
    }

    private static void nameClusters(String clustersOutDir) throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException {
//                   String className = "nl.uva.sne.extractors.JtopiaExtractor";
        String className = "nl.uva.sne.extractors.LuceneExtractor";
        Class c = Class.forName(className);

        Object obj = c.newInstance();
        TermExtractor termExtractor = (TermExtractor) obj;
        termExtractor.configure(FileUtils.getProperties(propertiesPath));

        JtopiaExtractor e = new JtopiaExtractor();
        e.configure(FileUtils.getProperties(propertiesPath));
        File dir = new File(clustersOutDir);
        for (File f : dir.listFiles()) {
            if (FilenameUtils.getExtension(f.getName()).endsWith("txt")) {
                Logger.getLogger(Main.class.getName()).log(Level.INFO, "Processing {0}", f.getAbsolutePath());
                Map<String, Double> terms = termExtractor.termXtraction(f.getAbsolutePath());
                ValueComparator bvc = new ValueComparator(terms);
                Map<String, Double> sorted_map = new TreeMap(bvc);
                sorted_map.putAll(terms);
                StringBuilder name = new StringBuilder();
                int count = 0;
                for (String s : sorted_map.keySet()) {
                    name.append(s).append("_");
                    if (count > 4) {
                        break;
                    }
                    count++;
                }
                Logger.getLogger(Main.class.getName()).log(Level.INFO, "Writing cluster: {0}", name.toString());
                FileUtils.writeDictionary2File(terms, clustersOutDir + File.separator + FilenameUtils.removeExtension(f.getName()) + name.toString() + ".csv");
            }
        }
    }

    private static void nameClusterFolders(String clustersOutDir) {
        File clusterDir = new File(clustersOutDir);
        Map<File, File> fromTo = new HashMap<>();
        for (File cluster : clusterDir.listFiles()) {
            if (cluster.isDirectory()) {
                int count = 0;
                String termName = null;
                for (File f : cluster.listFiles()) {
                    if (FilenameUtils.getExtension(f.getName()).endsWith("term")) {
                        count++;
                        termName = FilenameUtils.removeExtension(f.getName());
                    }
                }
                if (count == 1) {
                    fromTo.put(cluster, new File(cluster.getParent() + File.separator + termName));
                }
            }
        }
        for (File f : fromTo.keySet()) {
            f.renameTo(fromTo.get(f));
        }
    }
}
