/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.sne.disambiguation;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import org.semanticweb.skos.AddAssertion;
import org.semanticweb.skos.SKOSChange;
import org.semanticweb.skos.SKOSChangeException;
import org.semanticweb.skos.SKOSConceptScheme;
import org.semanticweb.skos.SKOSCreationException;
import org.semanticweb.skos.SKOSEntityAssertion;
import org.semanticweb.skos.SKOSStorageException;
import org.semanticweb.skosapibinding.SKOSFormatExt;

/**
 *
 * @author S. Koulouzis
 */
public class Main {

    public static String propertiesPath = "disambiguation.properties";
    private static String props;

    public static void main(String args[]) throws IOException, Exception {
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
                    terms = buildGraph(terms, null);
                    export2SKOS(terms, outDir + File.separator + "taxonomy.rdf", String.valueOf(1));
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

        Logger.getLogger(Main.class.getName()).log(Level.INFO, "-----Done-----");
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

    private static List<Term> buildGraph(List<Term> allTerms, Map<String, Term> termMap) throws Exception {
        if (termMap == null) {
            termMap = new HashMap<>();
        }
        for (Term tv : allTerms) {
            List<Term> broader = tv.getBroader();
            if (broader != null) {
                for (Term b : broader) {
                    b.addNarrowerUID(tv.getUID());
                    b.addNarrower(tv);
                }
                buildGraph(broader, termMap);
            }
            Term tmp = termMap.get(tv.getUID());
            if (tmp != null) {
                tv = TermFactory.merge(tmp, tv);
            }
            termMap.put(tv.getUID(), tv);
        }
        return new ArrayList<>(termMap.values());
    }

    private static void export2SKOS(List<Term> allTerms, String fileName, String version) throws SKOSCreationException, IOException, SKOSChangeException, SKOSStorageException {
        URI uri = URI.create(SkosUtils.SKOS_URI + "v" + version);
        SKOSConceptScheme scheme = SkosUtils.getSKOSDataFactory().getSKOSConceptScheme(uri);
        List<SKOSChange> change = new ArrayList<>();
        SKOSEntityAssertion schemaAss = SkosUtils.getSKOSDataFactory().getSKOSEntityAssertion(scheme);
        change.add(new AddAssertion(SkosUtils.getSKOSDataset(), schemaAss));

        for (Term tv : allTerms) {
            if (tv.getBroader() == null || tv.getBroader().isEmpty()) {
                change.addAll(SkosUtils.create(tv, "EN", true, version));
            } else {
                change.addAll(SkosUtils.create(tv, "EN", false, version));
            }
        }
        SkosUtils.getSKOSManager().applyChanges(change);
        SkosUtils.getSKOSManager().save(SkosUtils.getSKOSDataset(), SKOSFormatExt.RDFXML, new File(fileName).toURI());
    }
}
