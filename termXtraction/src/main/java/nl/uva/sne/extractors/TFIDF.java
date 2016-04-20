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
public class TFIDF implements SortTerms {

    @Override
    public Map<String, Double> sort(Map<String, Double> termDictionaray, String docs) throws IOException, InterruptedException {
        Map<String, Double> newTermDictionaray = new HashMap<>();
        TFRatio tfSort = new TFRatio();
        Map<String, Double> tfMap = tfSort.sort(termDictionaray, docs);
        IDFSort idfSort = new IDFSort();
        Map<String, Double> idfMap = idfSort.sort(termDictionaray, docs);
        for (String t : tfMap.keySet()) {
            Double tf = tfMap.get(t);
            Double idf = idfMap.get(t);
            newTermDictionaray.put(t, (tf * idf));
        }
        return newTermDictionaray;
    }

}
