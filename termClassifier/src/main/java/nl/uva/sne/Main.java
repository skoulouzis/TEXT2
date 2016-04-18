/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.sne;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import nl.uva.sne.classifiers.Classifier;
import java.util.logging.Level;
import java.util.logging.Logger;
import nl.uva.sne.classifiers.SemanticUtils;
import nl.uva.sne.classifiers.Term;
import nl.uva.sne.classifiers.TermFactory;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.json.simple.parser.ParseException;

/**
 *
 * @author S. Koulouzis
 */
public class Main {

    public static String propertiesPath = "cluster.properties";

    public static void main(String args[]) {
        try {
            String className = "nl.uva.sne.classifiers.Kmeans";
            Class c = Class.forName(className);
            Object obj = c.newInstance();
            Classifier classifier = (Classifier) obj;

            classifier.configure(getProperties());

//            Map<String, String> cluster = classifier.cluster("/home/alogo/Downloads/jsonTerms");
//            copyTerms2Clusters(cluster, "/home/alogo/Downloads/jsonTerms");
//            writeClustersToOneFile("/home/alogo/Downloads/jsonTerms/0", "/home/alogo/Downloads/jsonTerms/0.txt");
//            writeClustersToOneFile("/home/alogo/Downloads/jsonTerms/1", "/home/alogo/Downloads/jsonTerms/1.txt");
            writeClustersToOneFile("/home/alogo/Downloads/jsonTerms/4", "/home/alogo/Downloads/jsonTerms/4.txt");

        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | IOException | ParseException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static Properties getProperties() throws IOException {
        InputStream in = null;
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            in = classLoader.getResourceAsStream(propertiesPath);
            Properties properties = new Properties();
            properties.load(in);

            return properties;
        } catch (IOException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            if (in != null) {
                in.close();
            }
        }
        return null;
    }

    private static void copyTerms2Clusters(Map<String, String> cluster, String outputDir) throws IOException {
        for (String fileName : cluster.keySet()) {
            File dir = new File(outputDir + File.separator + cluster.get(fileName));
            if (!dir.exists()) {
                dir.mkdir();
            }
            File file = new File(fileName + ".json");
            FileUtils.copyFile(file, new File(dir.getAbsolutePath() + File.separator + file.getName()));
        }
    }

    private static void writeClustersToOneFile(String inDir, String outkeywordsFile) throws IOException, ParseException {
        File dir = new File(inDir);
        StringBuilder sb = new StringBuilder();
        for (File f : dir.listFiles()) {
            if (FilenameUtils.getExtension(f.getName()).endsWith("json")) {
                Term tv = TermFactory.create(f.getAbsolutePath());
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
}
