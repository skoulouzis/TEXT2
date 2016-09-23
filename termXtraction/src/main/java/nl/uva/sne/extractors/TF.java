/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.sne.extractors;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.unix4j.Unix4j;
import org.unix4j.unix.grep.GrepOptionSets;

/**
 *
 * @author S. Koulouzis
 */
public class TF implements SortTerms {

    @Override
    public Map<String, Double> sort(Map<String, Double> termDictionaray, String dirPath) throws IOException, InterruptedException {
        Map<String, Double> newTermDictionaray = new HashMap<>();
        File dir = new File(dirPath);
        File[] docs = dir.listFiles();

        for (String term : termDictionaray.keySet()) {
            double tf = 0;
            for (File f : docs) {
                int count = 0;
                if (FilenameUtils.getExtension(f.getName()).endsWith("txt")) {
                    count++;
                    Logger.getLogger(TF.class.getName()).log(Level.INFO, "{0}: {1} of {2}", new Object[]{f.getName(), count, docs.length});
                    String line;
                    StringBuilder sb = new StringBuilder();
                    try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                        while ((line = br.readLine()) != null) {
                            sb.append(line);
                        }
                    }
                    tf += StringUtils.countMatches(sb.toString(), term);
                }
            }

            newTermDictionaray.put(term, tf);
        }
        return newTermDictionaray;
    }

}
