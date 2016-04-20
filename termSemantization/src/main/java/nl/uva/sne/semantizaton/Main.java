/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.sne.semantizaton;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import nl.uva.sne.semantizators.Semantizator;
import nl.uva.sne.commons.Term;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

/**
 *
 * @author S. Koulouzis
 */
public class Main {

    public static String propertiesPath = "semantization.properties";

    public static void main(String args[]) {

        try {
            String className = "nl.uva.sne.semantizators.BabelNet";
            Class c = Class.forName(className);
            Object obj = c.newInstance();
            Semantizator semantizator = (Semantizator) obj;

            semantizator.configure(getProperties());
            String in = "/home/alogo/Downloads/textdocs/dictionaryMix.csv";
            List<Term> terms = semantizator.semnatizeTerms(in);

            writeTerms2Json(terms, "/home/alogo/Downloads/jsonTerms/");

        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | IOException | ParseException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static void writeTerms2Json(List<Term> terms, String outputDir) throws IOException {

        for (Term t : terms) {
            JSONObject json = term2Json(t);
            try (FileWriter file = new FileWriter(outputDir + File.separator + t.getUID() + ".json")) {
                file.write(json.toJSONString());
                file.flush();
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

    private static JSONObject term2Json(Term t) {
        JSONObject obj = new JSONObject();
        obj.put("uid", t.getUID());
        obj.put("lemma", t.getLemma());
        obj.put("alternativeLables", t.getAlternativeLables());
        obj.put("broaderUIDS", t.getBroaderUIDS());
        obj.put("categories", t.getCategories());
        obj.put("foreignKey", t.getForeignKey());
        obj.put("glosses", t.getGlosses());
        obj.put("isFromDictionary", t.getIsFromDictionary());
//                    obj.put("narrower", t.getNarrower());
        obj.put("narrowerUIDS", t.getNarrowerUIDS());
//  obj.put("narrowerUIDS", t.getSynonyms());
        return obj;
    }
}
