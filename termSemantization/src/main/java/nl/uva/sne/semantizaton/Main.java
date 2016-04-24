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
import nl.uva.sne.commons.FileUtils;
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
    private static String props;

    public static void main(String args[]) {
        String allTermsDictionary = null, filterredDictionary = null, outDir = null;
        if (args != null) {

            allTermsDictionary = args[0];
            filterredDictionary = args[1];
            outDir = args[2];
            
            props = args[args.length - 1];
            if (props.endsWith(".properties")) {
                propertiesPath = props;
            }
        }

        try {
//          $HOME/textdocs/dictionaryAll.csv $HOME/textdocs/term_dictionaryPOS_expert_validation.csv $HOME/Downloads/jsonTerms

            String className = "nl.uva.sne.semantizators.BabelNet";
            Class c = Class.forName(className);
            Object obj = c.newInstance();
            Semantizator semantizator = (Semantizator) obj;

            semantizator.configure(FileUtils.getProperties(propertiesPath));

            List<Term> terms = semantizator.semnatizeTerms(allTermsDictionary, filterredDictionary);
            
            writeTerms2Json(terms, outDir);

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
