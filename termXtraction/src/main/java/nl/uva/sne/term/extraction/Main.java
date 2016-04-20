/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.sne.term.extraction;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import nl.uva.sne.commons.FileUtils;
import nl.uva.sne.commons.ValueComparator;
import nl.uva.sne.extractors.SortTerms;
import nl.uva.sne.extractors.TermExtractor;

/**
 *
 * @author S. Koulouzis
 */
public class Main {

    public static String propertiesPath = "termXtraction.properties";

    public static void main(String args[]) {
        String className = null;
        boolean extractTerms = false;
        String textDocs = null;
        String dictionaryOut = null;
        String dictionaryIn = null;
        boolean sort = false;

        if (args != null) {
            for (int i = 0; i < args.length; i++) {
// -e nl.uva.sne.extractors.LuceneExtractor $HOME/Downloads/textdocs $HOME/Downloads/textdocs/dictionary.csv
                if (args[i].equals("-e")) {
                    extractTerms = true;
                    className = args[i + 1];
                    textDocs = args[i + 2];
                    dictionaryOut = args[i + 3];
                    break;
                }
                // -s nl.uva.sne.extractors.IDFSort $HOME/Downloads/textdocs/dictionary.csv $HOME/Downloads/allAds/ $HOME/Downloads/textdocs/dictionaryIDF.csv
                if (args[i].equals("-s")) {
                    sort = true;
                    className = args[i + 1];
                    dictionaryIn = args[i + 2];
                    textDocs = args[i + 3];
                    dictionaryOut = args[i + 4];
                    break;
                }

            }
            String props = args[args.length - 1];
            if (props.endsWith(".properties")) {
                propertiesPath = props;
            }
        }

//            String className = "nl.uva.sne.extractors.JtopiaExtractor";
//        className = "nl.uva.sne.extractors.LuceneExtractor";
        try {
            Class c = Class.forName(className);
            Object obj = c.newInstance();
            if (extractTerms) {
                TermExtractor termExtractor = (TermExtractor) obj;
                termExtractor.configure(getProperties());
                Map<String, Double> terms = termExtractor.termXtraction(textDocs);
                writeDictionary2File(terms, dictionaryOut);
            }

            //            className = "nl.uva.sne.extractors.IDFSort";
//            className = "nl.uva.sne.extractors.TFRatio";
//            className = "nl.uva.sne.extractors.TFIDF";
            if (sort) {
                SortTerms sorter = (SortTerms) obj;
                Map<String, Double> terms = FileUtils.csv2Map(dictionaryIn);
                terms = sorter.sort(terms, textDocs);
                writeDictionary2File(terms, dictionaryOut);
            }
        } catch (Exception ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static void writeDictionary2File(Map<String, Double> keywordsDictionaray, String outkeywordsDictionarayFile) throws FileNotFoundException {
        ValueComparator bvc = new ValueComparator(keywordsDictionaray);
        Map<String, Double> sorted_map = new TreeMap(bvc);
        sorted_map.putAll(keywordsDictionaray);

        try (PrintWriter out = new PrintWriter(outkeywordsDictionarayFile)) {
            for (String key : sorted_map.keySet()) {
                Double value = keywordsDictionaray.get(key);
                out.print(key + "," + value + "\n");
            }
        }
    }

    private static Properties getProperties() throws IOException {
        InputStream in = null;
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            in = classLoader.getResourceAsStream(propertiesPath);
            Properties properties = new Properties();
            System.err.println(propertiesPath);
            System.err.println(in);
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
