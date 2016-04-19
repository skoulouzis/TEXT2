/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.sne.extractors;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;

/**
 *
 * @author S. Koulouzis
 */
public interface TermExtractor {
    
    public void configure(Properties prop);
    
    public Map<String,Double> termXtraction(String inDir) throws IOException;
    
}
