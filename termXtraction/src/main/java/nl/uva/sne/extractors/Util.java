/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.sne.extractors;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import nl.uva.sne.commons.ValueComparator;
import org.unix4j.Unix4j;
import org.unix4j.unix.grep.GrepOptionSets;

/**
 *
 * @author S. Koulouzis
 */
public class Util {

    public static Map<String, Double> getScore(Map<String, Double> termDictionaray, String scoreType, List<File> docs) throws IOException, InterruptedException {
        ValueComparator bvc = new ValueComparator(termDictionaray);
        Map<String, Double> sorted_map = new TreeMap(bvc);
        sorted_map.putAll(termDictionaray);

        switch (scoreType) {
            case "tf":
                return sorted_map;

            case "tf_ratio":
//                TF(t) = (Number of times term t appears in a document) / (Total number of terms in the document).
//                FileUtils.grep(f, pattern, false);
                return sorted_map;

            case "idf":
                Map<String, Double> newTermDictionaray = new HashMap<>();
                for (String term : sorted_map.keySet()) {
                    int numOfDocsWithTerm = 0;
                    GrepOptionSets go = new GrepOptionSets();
                    for (File f : docs) {
                        try (InputStream fis = new FileInputStream(f)) {
                            String result = Unix4j.from(fis).grep(go.i.count, term.replaceAll("_", " ")).toStringResult();
                            Integer lineCount = Integer.valueOf(result);
                            if (lineCount > 0) {
                                numOfDocsWithTerm++;
                            }
                        }
                    }
                    double idf = 0;
                    if (numOfDocsWithTerm > 0) {
                        idf = Math.log((double) docs.size() / (double) numOfDocsWithTerm);
                    }
                    newTermDictionaray.put(term, idf);
                    Thread.sleep(1);
                }

                bvc = new ValueComparator(newTermDictionaray);
                sorted_map = new TreeMap(bvc);
                sorted_map.putAll(newTermDictionaray);
                return sorted_map;

            default:
                return sorted_map;
        }
    }
}
