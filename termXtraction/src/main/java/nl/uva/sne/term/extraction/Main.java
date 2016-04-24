/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.sne.term.extraction;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import nl.uva.sne.commons.FileUtils;
import nl.uva.sne.extractors.SortTerms;
import nl.uva.sne.extractors.TermExtractor;

/**
 *
 * @author S. Koulouzis
 */
public class Main {

    public static String propertiesPath = "termXtraction.properties";
    private static String props;

    public static void main(String args[]) {
        String className = null;
        boolean extractTerms = false;
        String textDocs = null;
        String dictionaryOut = null;
        String dictionaryIn1 = null;
        String dictionaryIn2 = null;
        boolean sort = false;
        boolean merge = false;

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
                // -s nl.uva.sne.extractors.IDFSort $HOME/Downloads/textdocs/dictionary.csv $HOME/Downloads/textdocs $HOME/Downloads/textdocs/dictionaryIDF.csv
                // -s nl.uva.sne.extractors.TFRatio $HOME/Downloads/textdocs/dictionary.csv $HOME/Downloads/textdocs $HOME/Downloads/textdocs/dictionaryTFRatio.csv
                // -s nl.uva.sne.extractors.TFIDF $HOME/Downloads/textdocs/dictionary.csv $HOME/Downloads/textdocs $HOME/Downloads/textdocs/dictionaryTFIDF.csv
                if (args[i].equals("-s")) {
                    sort = true;
                    className = args[i + 1];
                    dictionaryIn1 = args[i + 2];
                    textDocs = args[i + 3];
                    dictionaryOut = args[i + 4];
                    break;
                }
//                 -m $HOME/Downloads/textdocs/dictionaryTopia.csv $HOME/Downloads/textdocs/dictionaryLucine.csv $HOME/Downloads/textdocs/dictionary.csv 
                if (args[i].equals("-m")) {
                    dictionaryIn1 = args[i + 1];
                    dictionaryIn2 = args[i + 2];
                    dictionaryOut = args[i + 3];
                    merge = true;
                }

            }
            props = args[args.length - 1];
            if (props.endsWith(".properties")) {
                propertiesPath = props;
            }
        }

//            String className = "nl.uva.sne.extractors.JtopiaExtractor";
//        className = "nl.uva.sne.extractors.LuceneExtractor";
        try {

            if (extractTerms) {
                Class c = Class.forName(className);
                Object obj = c.newInstance();
                TermExtractor termExtractor = (TermExtractor) obj;
                termExtractor.configure(FileUtils.getProperties(propertiesPath));
                Map<String, Double> terms = termExtractor.termXtraction(textDocs);
                FileUtils.writeDictionary2File(terms, dictionaryOut);
            }

            //            className = "nl.uva.sne.extractors.IDFSort";
//            className = "nl.uva.sne.extractors.TFRatio";
//            className = "nl.uva.sne.extractors.TFIDF";
            if (sort) {
                Class c = Class.forName(className);
                Object obj = c.newInstance();
                SortTerms sorter = (SortTerms) obj;
                Map<String, Double> terms = FileUtils.csv2Map(dictionaryIn1);
                terms = sorter.sort(terms, textDocs);
                FileUtils.writeDictionary2File(terms, dictionaryOut);
            }

            if (merge) {
                Map<String, Double> correctTF = FileUtils.csv2Map(dictionaryIn1);
                Map<String, Double> otherMap = FileUtils.csv2Map(dictionaryIn2);
                Map<String, Double> out = FileUtils.mergeDictionaries(correctTF, otherMap);
                FileUtils.writeDictionary2File(out, dictionaryOut);
            }
        } catch (Exception ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
