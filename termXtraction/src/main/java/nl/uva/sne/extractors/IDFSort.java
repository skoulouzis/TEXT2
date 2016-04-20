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
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FilenameUtils;
import org.unix4j.Unix4j;
import org.unix4j.unix.grep.GrepOptionSets;

/**
 *
 * @author S. Koulouzis
 */
public class IDFSort implements SortTerms {

    @Override
    public Map<String, Double> sort(Map<String, Double> termDictionaray, String dirPath) throws IOException, InterruptedException {
        Map<String, Double> newTermDictionaray = new HashMap<>();
        File dir = new File(dirPath);
        File[] docs = dir.listFiles();
        GrepOptionSets go = new GrepOptionSets();
        for (String term : termDictionaray.keySet()) {
            int numOfDocsWithTerm = 0;
            for (File f : docs) {
                int count = 0;
                if (FilenameUtils.getExtension(f.getName()).endsWith("txt")) {
                    count++;
                    Logger.getLogger(IDFSort.class.getName()).log(Level.INFO, "{0}: {1} of {2}", new Object[]{f.getName(), count, docs.length});
                    try (InputStream fis = new FileInputStream(f)) {
                        String result = Unix4j.from(fis).grep(go.i.count, term.replaceAll("_", " ")).toStringResult();
                        Integer lineCount = Integer.valueOf(result);
                        if (lineCount > 0) {
                            numOfDocsWithTerm++;
                        }
                    }
                }

            }
            double idf = 0;
            if (numOfDocsWithTerm > 0) {
                idf = Math.log((double) docs.length / (double) numOfDocsWithTerm);
            }
            newTermDictionaray.put(term, idf);
        }
        return newTermDictionaray;
    }
}
