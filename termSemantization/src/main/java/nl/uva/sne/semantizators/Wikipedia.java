/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.sne.semantizators;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import net.didion.jwnl.JWNLException;
import nl.uva.sne.commons.Term;
import org.json.simple.parser.ParseException;

/**
 *
 * @author S. Koulouzis
 */
public class Wikipedia implements Semantizator {

    private Integer limit;

    @Override
    public List<Term> semnatizeTerms(String allTermsDictionary, String filterredDictionary) throws IOException, ParseException {
        List<Term> terms = new ArrayList<>();
        File dictionary = new File(filterredDictionary);
        int count = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(dictionary))) {
            for (String line; (line = br.readLine()) != null;) {
                String[] parts = line.split(",");
                String term = parts[0];
//                Integer score = Integer.valueOf(parts[1]);
                if (term.length() > 1) {
                    count++;
                    if (count > limit) {
                        break;
                    }
                    Term tt = getTerm(term, allTermsDictionary);
                    if (tt != null) {
                        terms.add(tt);
                    }
                }
            }
        } catch (Exception ex) {
            return terms;
        }
        return terms;
    }

    @Override
    public void configure(Properties properties) {
        limit = Integer.valueOf(properties.getProperty("num.of.terms", "5"));

    }

    private Term getTerm(String term, String allTermsDictionary) throws IOException, ParseException, JWNLException {
        List<Term> possibleTerms = getTermNodeByLemma(term);
        if (possibleTerms != null & possibleTerms.size() > 1) {
            return disambiguate(term, possibleTerms, allTermsDictionary);
        } else if (possibleTerms.size() == 1) {
            return possibleTerms.get(0);
        }
        return null;
    }

    private List<Term> getTermNodeByLemma(String term) {
        String url = "https://en.wikipedia.org/w/api.php?action=query&list=search&format=json&srsearch=" + term;
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private Term disambiguate(String term, List<Term> possibleTerms, String allTermsDictionary) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
