/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.sne.classifiers;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import org.json.simple.parser.ParseException;

/**
 *
 * @author S. Koulouzis
 */
public interface Classifier {

    public void configure(Properties properties);

    public Map<String, String> cluster(String inDir) throws IOException, ParseException;
}
