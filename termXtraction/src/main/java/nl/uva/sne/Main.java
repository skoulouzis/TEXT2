/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.sne;

import nl.uva.sne.extractors.ValueComparator;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import nl.uva.sne.extractors.TermExtractor;

/**
 *
 * @author S. Koulouzis
 */
public class Main {

    public static String propertiesPath = "termXtraction.properties";

    public static void main(String args[]) {

        try {
            String className = "nl.uva.sne.extractors.JtopiaExtractor";
//            String className = "nl.uva.sne.extractors.LuceneExtractor";
            Class c = Class.forName(className);
            Object obj = c.newInstance();
            TermExtractor termExtractor = (TermExtractor) obj;

            termExtractor.configure(getProperties());
//            Map<String, Integer> terms = termExtractor.termXtraction("/home/alogo/Downloads/textdocs");
//            writeDictionary2File(terms, "/home/alogo/Downloads/textdocs/dictionary.csv");

            Map<String, Integer> terms = termExtractor.termXtraction("/home/alogo/Downloads/jsonTerms/");
            writeDictionary2File(terms, "/home/alogo/Downloads/textdocs/clusterNames.csv");

        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | IOException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static void writeDictionary2File(Map<String, Integer> keywordsDictionaray, String outkeywordsDictionarayFile) throws FileNotFoundException {
        ValueComparator bvc = new ValueComparator(keywordsDictionaray);
        Map<String, Integer> sorted_map = new TreeMap(bvc);
        sorted_map.putAll(keywordsDictionaray);

        try (PrintWriter out = new PrintWriter(outkeywordsDictionarayFile)) {
            for (String key : sorted_map.keySet()) {
                out.print(key + "," + keywordsDictionaray.get(key) + "\n");
            }
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
}
