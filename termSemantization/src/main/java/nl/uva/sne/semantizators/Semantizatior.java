/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.sne.semantizators;

import java.io.IOException;
import java.util.List;
import java.util.Properties;
import net.didion.jwnl.JWNLException;
import nl.uva.sne.commons.Term;
import org.json.simple.parser.ParseException;

/**
 *
 * @author S. Koulouzis
 */
public interface Semantizatior {

    public List<Term> semnatizeTerms(String allTermsDictionary, String filterredDictionary) throws IOException, ParseException;

    public void configure(Properties properties);

    public Term getTerm(String term, String allTermsDictionaryPath, double minimumSimilarity) throws IOException, ParseException, JWNLException;
}
