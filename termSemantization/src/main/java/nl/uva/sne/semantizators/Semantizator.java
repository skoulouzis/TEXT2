/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.sne.semantizators;

import java.io.IOException;
import java.util.List;
import java.util.Properties;
import org.json.simple.parser.ParseException;

/**
 *
 * @author S. Koulouzis
 */
public interface Semantizator {

    public List<Term> semnatizeTerms(String termDictionaryFile) throws IOException, ParseException;

    public void configure(Properties properties);

}
