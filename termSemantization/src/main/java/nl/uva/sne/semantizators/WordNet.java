/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.sne.semantizators;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.didion.jwnl.JWNL;
import net.didion.jwnl.JWNLException;
import net.didion.jwnl.data.IndexWord;
import net.didion.jwnl.data.IndexWordSet;
import net.didion.jwnl.data.PointerType;
import net.didion.jwnl.data.PointerUtils;
import net.didion.jwnl.data.Synset;
import net.didion.jwnl.data.list.PointerTargetTree;
import net.didion.jwnl.dictionary.Dictionary;
import nl.uva.sne.commons.SemanticUtils;
import nl.uva.sne.commons.Term;
import org.json.simple.parser.ParseException;

/**
 *
 * @author S. Koulouzis
 */
public class WordNet implements Semantizatior {

    private static Dictionary wordNetdictionary;
    private Double minimumSimilarity;
    private Integer limit;

    @Override
    public List<Term> semnatizeTerms(String allTermsDictionary, String filterredDictionary) throws IOException, ParseException {
        List<Term> terms = new ArrayList<>();
        File dictionary = new File(filterredDictionary);
        int count = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(dictionary))) {
            for (String line; (line = br.readLine()) != null;) {
                String[] parts = line.split(",");
                String term = parts[0];
//                Integer score = Integer.valueOf(parts[1]);
                if (term.length() >= 1) {
                    count++;
                    if (count > limit) {
                        break;
                    }
                    Term tt = getTerm(term, allTermsDictionary, minimumSimilarity);
                    if (tt != null) {
                        terms.add(tt);
                    }
                }
            }
        } catch (Exception ex) {
            Logger.getLogger(SemanticUtils.class.getName()).log(Level.WARNING, null, ex);
            return terms;
        }
        return terms;
    }

    @Override
    public void configure(Properties properties) {
        String propFile = properties.getProperty("word.net.conf.xml", "file_properties.xml");
        try (FileInputStream fis = new FileInputStream(propFile)) {
            JWNL.initialize(fis);
        } catch (IOException | JWNLException ex) {
            Logger.getLogger(WordNet.class.getName()).log(Level.SEVERE, null, ex);
        }
        minimumSimilarity = Double.valueOf(properties.getProperty("minimum.similarity", "0,3"));
        limit = Integer.valueOf(properties.getProperty("num.of.terms", "5"));
    }

    @Override
    public Term getTerm(String term, String allTermsDictionaryPath, double minimumSimilarity) throws IOException, ParseException, JWNLException {
        wordNetdictionary = getWordNetDictionary();
        IndexWordSet ws = wordNetdictionary.lookupAllIndexWords(term);
        Set<Term> possibleTerms = new HashSet<>();
        for (IndexWord iw : ws.getIndexWordArray()) {
            Term t = new Term(iw.getLemma().toLowerCase());
            t.setOriginalTerm(term);
            t.setUID(String.valueOf(Math.abs(iw.hashCode())));
            List<String> glosses = new ArrayList<>();
            for (Synset s : iw.getSenses()) {
                glosses.add(s.getGloss().toLowerCase());
                t.setGlosses(glosses);
//                PointerTargetTree tree = getRelatedTree(s, 1, PointerType.HYPERNYM);
            }
            possibleTerms.add(t);
        }
        Term dis = SemanticUtils.disambiguate(term, possibleTerms, allTermsDictionaryPath, minimumSimilarity, true);
        if (dis == null) {
            Logger.getLogger(WordNet.class.getName()).log(Level.INFO, "Couldn''''t figure out what ''{0}'' means", term);
        } else {
            Logger.getLogger(WordNet.class.getName()).log(Level.INFO, "Term: {0}. Confidence: {1}", new Object[]{dis, dis.getConfidence()});
        }
        return dis;

    }

    private PointerTargetTree getRelatedTree(Synset sense, int depth, PointerType type) throws JWNLException {
        PointerTargetTree relatedTree;
        // Call a different function based on what type of relationship you are looking for
        if (type == PointerType.HYPERNYM) {
            relatedTree = PointerUtils.getInstance().getHypernymTree(sense, depth);
        } else if (type == PointerType.HYPONYM) {
            relatedTree = PointerUtils.getInstance().getHyponymTree(sense, depth);
        } else {
            relatedTree = PointerUtils.getInstance().getSynonymTree(sense, depth);
        }
        return relatedTree;
    }

    private static Dictionary getWordNetDictionary() {
        if (wordNetdictionary == null) {
            wordNetdictionary = Dictionary.getInstance();
        }
        return wordNetdictionary;
    }

}
