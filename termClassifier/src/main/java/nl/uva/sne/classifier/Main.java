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
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import nl.uva.sne.classifiers.Classifier;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.didion.jwnl.JWNLException;
import nl.uva.sne.classifiers.Kmeans;
import nl.uva.sne.commons.FileUtils;
import nl.uva.sne.commons.SemanticUtils;
import nl.uva.sne.commons.Term;
import nl.uva.sne.commons.TermFactory;
import nl.uva.sne.commons.ValueComparator;
import nl.uva.sne.extractors.JtopiaExtractor;
import nl.uva.sne.extractors.TermExtractor;
import static nl.uva.sne.term.extraction.Main.propertiesPath;
import org.apache.commons.io.FilenameUtils;
import org.json.simple.parser.ParseException;

/**
 *
 * @author S. Koulouzis
 */
public class Main {

    public static String propertiesPath = "cluster.properties";
    private static String props;

    public static void main(String args[]) {
        boolean cluster = false;
        boolean name = false;
        String className = null;
        String jsonTermsDir = null;
        String clustersOutDir = null;
        if (args != null) {
            for (int i = 0; i < args.length; i++) {
//           -c nl.uva.sne.classifiers.Kmeans $HOME/Downloads/jsonTerms  $HOME/Downloads/clusters
                if (args[i].equals("-c")) {
                    cluster = true;
                    className = args[i + 1];
                    jsonTermsDir = args[i + 2];
                    clustersOutDir = args[i + 3];
                    break;
                }
                if (args[i].equals("-n")) {
                    name = true;
                    clustersOutDir = args[i + 1];
                    break;
                }
            }
            props = args[args.length - 1];
            if (props.endsWith(".properties")) {
                propertiesPath = props;
            }
        }

        try {
            if (cluster) {
                className = "nl.uva.sne.classifiers.Kmeans";
                Class c = Class.forName(className);
                Object obj = c.newInstance();
                Classifier classifier = (Classifier) obj;
                classifier.configure(FileUtils.getProperties(propertiesPath));

                Map<String, String> theCluster = classifier.cluster(jsonTermsDir);
                copyTerms2Clusters(theCluster, clustersOutDir);
            }
            if (name) {
                writeClustersToOneFile(clustersOutDir);
                nameClusters(clustersOutDir);
            }

        } catch (Exception ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static void copyTerms2Clusters(Map<String, String> cluster, String outputDir) throws IOException {
        for (String fileName : cluster.keySet()) {
            File dir = new File(outputDir + File.separator + cluster.get(fileName));
            if (!dir.exists()) {
                dir.mkdir();
            }
            File file = new File(fileName + ".json");
            org.apache.commons.io.FileUtils.copyFile(file, new File(dir.getAbsolutePath() + File.separator + file.getName()));
        }
    }

    private static void writeClustersToOneFile(String inDir, String outkeywordsFile) throws IOException, ParseException, JWNLException {
        File dir = new File(inDir);
        StringBuilder sb = new StringBuilder();
        for (File f : dir.listFiles()) {
            if (FilenameUtils.getExtension(f.getName()).endsWith("json")) {
                Logger.getLogger(Main.class.getName()).log(Level.INFO, "Processing {0}", f.getAbsolutePath());
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
//                ValueComparator bvc = new ValueComparator(terms);
//                Map<String, Double> sorted_map = new TreeMap(bvc);
//                sorted_map.putAll(terms);
                FileUtils.writeDictionary2File(terms, clustersOutDir + f.getName() + ".csv");
            }
        }
    }
}
