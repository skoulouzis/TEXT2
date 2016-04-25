/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.sne.commons;

import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;

/**
 *
 * @author S. Koulouzis
 */
public class TermFactory {

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

    public static Term create(String synet, String language, String lemma, String theID) throws ParseException, UnsupportedEncodingException {
        Term node = null;
        language = language.toLowerCase();
        JSONObject jSynet = (JSONObject) JSONValue.parseWithException(synet);

        JSONArray categoriesArray = (JSONArray) jSynet.get("categories");
        List<String> categories = null;
        if (categoriesArray != null) {

            categories = new ArrayList<>();
            for (Object o : categoriesArray) {
                JSONObject cat = (JSONObject) o;
                String lang = (String) cat.get("language");
                if (lang.toLowerCase().equals(language)) {
                    String category = ((String) cat.get("category")).toLowerCase();
                    categories.add(category);
                }
            }
        }

        JSONArray glossesArray = (JSONArray) jSynet.get("glosses");

        List<String> glosses = null;
        if (glossesArray != null) {
            glosses = new ArrayList<>();

            for (Object o : glossesArray) {
                JSONObject gloss = (JSONObject) o;
                String lang = (String) gloss.get("language");
                if (lang.toLowerCase().equals(language)) {
                    String g = ((String) gloss.get("gloss")).toLowerCase();
                    glosses.add(g);
                }
            }
        }

        JSONArray senses = (JSONArray) jSynet.get("senses");
        if (senses != null) {
            for (Object o2 : senses) {
                JSONObject jo2 = (JSONObject) o2;
                JSONObject synsetID = (JSONObject) jo2.get("synsetID");
                String babelNetID = (String) synsetID.get("id");

                String lang = (String) jo2.get("language");

                String lemma1, lemma2;
                if (lang.toLowerCase().equals(language)) {
                    List<String> altLables = new ArrayList<>();
                    String jlemma = (String) jo2.get("lemma");
                    jlemma = jlemma.toLowerCase().replaceAll("(\\d+,\\d+)|\\d+", "");
                    altLables.add(jlemma);
                    if (theID != null && babelNetID.equals(theID)) {
                        node = new Term(jlemma);
                        node.setUID(babelNetID);
                        node.setCategories(categories);
                        node.setAlternativeLables(altLables);
                        node.setGlosses(glosses);
                        return node;
                    }
                    lemma = java.net.URLDecoder.decode(lemma, "UTF-8");
                    lemma = lemma.replaceAll(" ", "_");
                    int dist;
                    if (!jlemma.startsWith("(") && jlemma.contains("(")) {
                        int index = jlemma.indexOf("(") - 1;
                        String sub = jlemma.substring(0, index);
                        dist = edu.stanford.nlp.util.StringUtils.editDistance(lemma, sub);
                    } else {
                        dist = edu.stanford.nlp.util.StringUtils.editDistance(lemma, jlemma);
                    }
//                    dist = edu.stanford.nlp.util.StringUtils.editDistance(lemma, jlemma);
                    if (dist <= 0) {
                        node = new Term(jlemma);
                        node.setUID(babelNetID);
                        node.setCategories(categories);
                        node.setAlternativeLables(altLables);
                        node.setGlosses(glosses);
                        return node;
                    }
                    if (jlemma.contains(lemma) && jlemma.contains("_")) {
                        String[] parts = jlemma.split("_");
                        for (String p : parts) {
                            if (lemma.contains(p)) {
                                jlemma = p;
                                break;
                            }
                            if (p.contains(lemma)) {
                                jlemma = lemma;
                                break;
                            }
                        }
                    }
                    dist = edu.stanford.nlp.util.StringUtils.editDistance(lemma, jlemma);
                    if (lemma.length() < jlemma.length()) {
                        lemma1 = lemma;
                        lemma2 = jlemma;
                    } else {
                        lemma2 = lemma;
                        lemma1 = jlemma;
                    }
                    if (dist <= 3 && lemma2.contains(lemma1)) {
                        node = new Term(jlemma);
                        node.setUID(babelNetID);
                        node.setCategories(categories);
                        node.setAlternativeLables(altLables);
                        node.setGlosses(glosses);
                        return node;
                    }
                }
            }
        }

        return null;
    }

    public static Term create(FileReader fr) throws IOException, ParseException {
        String jsonStr = FileUtils.readFile(fr);
        Term term = new Term(FileUtils.getLemma(jsonStr));
        term.setUID(FileUtils.getUID(jsonStr));
        term.setAlternativeLables(FileUtils.getAltLables(jsonStr));
        term.setBroaderUIDS(FileUtils.getBroaderUIDS(jsonStr));
        term.setCategories(FileUtils.getCategories(jsonStr));
        term.setForeignKey(FileUtils.getForeignKey(jsonStr));
        term.setGlosses(FileUtils.getGlosses(jsonStr));
        term.setIsFromDictionary(FileUtils.IsFromDictionary(jsonStr));
        return term;

    }

}
