/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.sne.extractors;

import java.io.IOException;
import java.util.Map;

/**
 *
 * @author S. Koulouzis
 */
public interface SortTerms {

    public Map<String, Double> sort(Map<String, Double> termDictionaray, String docs) throws IOException, InterruptedException;
}
