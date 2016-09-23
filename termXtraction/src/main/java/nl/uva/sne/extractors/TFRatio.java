/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.sne.extractors;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author S. Koulouzis
 */
public class TFRatio implements SortTerms {

    @Override
    public Map<String, Double> sort(Map<String, Double> termDictionaray, String dirPath) throws IOException, InterruptedException {
        TF tfSort = new TF();
        Map<String, Double> tfMap = tfSort.sort(termDictionaray, dirPath);
        Map<String, Double> newTermDictionaray = new HashMap<>();

        for (String term : tfMap.keySet()) {
            Double tf = tfMap.get(term);
            int totalNumberOfTerms = tfMap.size();
            double tf_ratio = (double) tf / (double) totalNumberOfTerms;
            newTermDictionaray.put(term, tf_ratio);
        }
        return newTermDictionaray;
    }

}
