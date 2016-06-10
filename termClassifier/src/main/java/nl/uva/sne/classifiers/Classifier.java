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

    public void trainModel(String trainDataDir,String outDir) throws IOException, ParseException;

    public Map<String, String> classify(String model, String dataDir) throws IOException, ParseException;
       
    public void saveClusterFile(String model, String dataDir, String filePath) throws IOException, ParseException;
}
