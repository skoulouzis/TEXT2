/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.sne.disambiguators;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Properties;
import net.didion.jwnl.JWNLException;
import nl.uva.sne.commons.Term;
import org.apache.commons.io.FilenameUtils;
import org.json.simple.parser.ParseException;

/**
 *
 * @author S. Koulouzis
 */
class DisambiguatorImpl implements Disambiguator, Runnable {

    private Integer limit;
    private Double minimumSimilarity;
    private Integer lineOffset;

    @Override
    public List<Term> disambiguateTerms(String allTermsDictionary, String filterredDictionary) throws IOException, ParseException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void configure(Properties properties) {
        StringBuilder props = new StringBuilder();
        String numOfTerms = System.getProperty("num.of.terms");

        if (numOfTerms == null) {
            limit = Integer.valueOf(properties.getProperty("num.of.terms", "5"));
        } else {
            limit = Integer.valueOf(numOfTerms);
        }
        props.append("num.of.terms: ").append(limit).append(" ");

        String offset = System.getProperty("offset.terms");

        if (offset == null) {
            lineOffset = Integer.valueOf(properties.getProperty("offset.terms", "1"));
        } else {
            lineOffset = Integer.valueOf(offset);
        }
        minimumSimilarity = Double.valueOf(properties.getProperty("minimum.similarity", "0,3"));
        props.append("minimum.similarity: ").append(minimumSimilarity).append(" ");
    }

    @Override
    public Term getTerm(String term, String allTermsDictionaryPath, double minimumSimilarity) throws IOException, ParseException, JWNLException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void run() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
