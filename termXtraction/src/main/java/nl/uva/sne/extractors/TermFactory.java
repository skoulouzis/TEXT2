/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.sne.extractors;

import java.io.IOException;
import org.json.simple.parser.ParseException;

/**
 *
 * @author S. Koulouzis
 */
class TermFactory {

    public static Term create(String jsonFile) throws IOException, ParseException {

        Term term = new Term(FileUtils.getLemma(jsonFile));
        term.setUID(FileUtils.getUID(jsonFile));
        term.setAlternativeLables(FileUtils.getAltLables(jsonFile));
        term.setBroaderUIDS(FileUtils.getBroaderUIDS(jsonFile));
        term.setCategories(FileUtils.getCategories(jsonFile));
        term.setForeignKey(FileUtils.getForeignKey(jsonFile));
        term.setGlosses(FileUtils.getGlosses(jsonFile));
        term.setIsFromDictionary(FileUtils.IsFromDictionary(jsonFile));
        return term;
    }

}
