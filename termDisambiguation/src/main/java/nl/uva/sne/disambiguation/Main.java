/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.sne.disambiguation;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import nl.uva.sne.commons.FileUtils;
import nl.uva.sne.commons.Term;
import nl.uva.sne.commons.TermFactory;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import nl.uva.sne.disambiguators.DisambiguatorImpl;
import org.apache.commons.io.FileDeleteStrategy;

/**
 *
 * @author S. Koulouzis
 */
public class Main {

    public static String propertiesPath = "disambiguation.properties";
    private static String props;

    public static void main(String args[]) throws IOException {
        String filterredDictionary = null, outDir = null;
        if (args != null) {

            filterredDictionary = args[0];
            outDir = args[1];

            props = args[args.length - 1];
            if (props.endsWith(".properties")) {
                propertiesPath = props;
            }
        }
        List<Term> terms = null;
        Properties properties = FileUtils.getProperties(propertiesPath);
        try {
//          $HOME/textdocs/dictionaryAll.csv $HOME/textdocs/term_dictionaryPOS_expert_validation.csv $HOME/Downloads/jsonTerms

//            String className = "nl.uva.sne.disambiguators.BabelNet";
//            String className = "nl.uva.sne.disambiguators.Wikipedia";
//            String className = "nl.uva.sne.disambiguators.Wikidata";
//            String className = "nl.uva.sne.disambiguators.WordNet";
            String className = "nl.uva.sne.disambiguators.MetaDisambiguator";

            Class c = Class.forName(className);
            Object obj = c.newInstance();
            DisambiguatorImpl disambiguator = (DisambiguatorImpl) obj;
            disambiguator.configure(properties);
            terms = disambiguator.disambiguateTerms(filterredDictionary);

        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | IOException | ParseException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            if (terms != null) {
                try {
                    writeTerms2Json(terms, outDir);
                } catch (IOException ex) {
                    Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            String cachePath = properties.getProperty("cache.path");
            File cacheFolder = new File(cachePath).getParentFile();
            if (cacheFolder.exists()) {
                for (File file : cacheFolder.listFiles()) {
                    if (file.getAbsolutePath().endsWith(".lock")) {
                        FileDeleteStrategy.FORCE.delete(file);
                    }
                }
            }
        }
    }

    private static void writeTerms2Json(List<Term> terms, String outputDir) throws IOException {

        for (Term t : terms) {
            JSONObject json = TermFactory.term2Json(t);
            try (FileWriter file = new FileWriter(outputDir + File.separator + t.getUID() + ".json")) {
                file.write(json.toJSONString());
                file.flush();
            }
        }
    }

}
