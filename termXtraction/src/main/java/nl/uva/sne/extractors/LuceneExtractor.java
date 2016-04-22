package nl.uva.sne.extractors;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.didion.jwnl.JWNLException;
import net.didion.jwnl.data.POS;
import nl.uva.sne.commons.SemanticUtils;
import org.apache.commons.io.FilenameUtils;
import nl.uva.sne.commons.ValueComparator;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author S. Koulouzis
 */
public class LuceneExtractor implements TermExtractor {

    private boolean removeExclusiveTerms;
    private int maxNgrams;

    @Override
    public void configure(Properties prop) {
        removeExclusiveTerms = Boolean.valueOf(prop.getProperty("remove.exclusive.terms", "false"));
        maxNgrams = Integer.valueOf(prop.getProperty("max.ngrams", "4"));
    }

    @Override
    public Map<String, Double> termXtraction(String inDir) throws IOException, FileNotFoundException, MalformedURLException {
        File dir = new File(inDir);
        Map<String, Double> termDictionaray = new HashMap();
        int count = 0;
        for (File f : dir.listFiles()) {
            count++;
            Logger.getLogger(LuceneExtractor.class.getName()).log(Level.INFO, "{0}: {1} of {2}", new Object[]{f.getName(), count, dir.list().length});
            if (FilenameUtils.getExtension(f.getName()).endsWith("txt")) {
                try {
                    StringBuilder sb = new StringBuilder();
                    try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                        for (String text; (text = br.readLine()) != null;) {
                            sb.append(text.toLowerCase()).append(" ");
                        }
                    }
                    List<String> tokens = SemanticUtils.tokenize(sb.toString());
                    for (String t : tokens) {
                        t = SemanticUtils.lemmatize(t);
                        POS[] pos = SemanticUtils.getPOS(t);
                        if (pos.length == 1 && pos[0].equals(POS.ADJECTIVE)) {
                            continue;
                        }
                        Double tf;
                        if (termDictionaray.containsKey(t)) {
                            tf = termDictionaray.get(t);
                            tf++;
                        } else {
                            tf = 1.0;
                        }
                        termDictionaray.put(t, tf);
                    }
                    List<String> ngrams = SemanticUtils.getNGrams(sb.toString(), maxNgrams);
                    for (String t : ngrams) {
                        Double tf;
                        if (termDictionaray.containsKey(t)) {
                            tf = termDictionaray.get(t);
                            tf++;
                        } else {
                            tf = 1.0;
                        }
                        termDictionaray.put(t, tf);
                    }

                } catch (JWNLException ex) {
                    Logger.getLogger(LuceneExtractor.class.getName()).log(Level.SEVERE, null, ex);
                } catch (Exception ex) {
                    Logger.getLogger(LuceneExtractor.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        if (removeExclusiveTerms) {
            return removeExclusiveTerms(termDictionaray);
        }
        return termDictionaray;
    }

    private Map<String, Double> removeExclusiveTerms(Map<String, Double> keywordsDictionaray) {
        ValueComparator bvc = new ValueComparator(keywordsDictionaray);
        Map<String, Double> sorted_map = new TreeMap(bvc);
        sorted_map.putAll(keywordsDictionaray);

        //remove terms that only apear with others. e.g. if we only 
        //have 'machine learning' there is no point to keep 'machine' or 'learning'
        List<String> toRemove = new ArrayList<>();
        Integer singleTermRank = 0;
        for (String key1 : sorted_map.keySet()) {
            singleTermRank++;
            Integer multiTermRank = 0;
            for (String key2 : sorted_map.keySet()) {
                multiTermRank++;
                if (!key1.contains("_") && key2.contains("_") && key2.split("_")[0].equals(key1)) {
                    int diff = multiTermRank - singleTermRank;
                    if (diff <= 5 && diff > 0) {
                        if (!toRemove.contains(key1)) {
                            toRemove.add(key1);
                        }
                    }
                    break;
                }
            }
        }
        for (String k : toRemove) {
            keywordsDictionaray.remove(k);
        }
        return keywordsDictionaray;
    }
}
