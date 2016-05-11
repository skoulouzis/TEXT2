/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.sne.commons;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;

/**
 *
 * @author S. Koulouzis
 */
public class TermFactory {

//    public static Term create(String jsonFile) throws IOException, ParseException {
//
//        Term term = new Term(FileUtils.getLemma(jsonFile));
//        term.setUID(FileUtils.getUID(jsonFile));
//        term.setAlternativeLables(FileUtils.getAltLables(jsonFile));
//        term.setBroaderUIDS(FileUtils.getBroaderUIDS(jsonFile));
//        term.setCategories(FileUtils.getCategories(jsonFile));
//        term.setForeignKey(FileUtils.getForeignKey(jsonFile));
//        term.setGlosses(FileUtils.getGlosses(jsonFile));
//        term.setIsFromDictionary(FileUtils.IsFromDictionary(jsonFile));
//        return term;
//    }
    public static Term create(String synet, String language, String lemma, String theID) throws ParseException, UnsupportedEncodingException {
        Term node;
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
                        node.setOriginalTerm(lemma);
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
                        node.setOriginalTerm(lemma);
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
                        node.setOriginalTerm(lemma);
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
        term.setOriginalTerm(FileUtils.getOriginalTerm(jsonStr));
        term.setConfidence(FileUtils.getConfidence(jsonStr));
        return term;
    }

//    public static Term create(String jsonFile) throws IOException, ParseException {
//        Term term = new Term(FileUtils.getLemma(jsonFile));
//        term.setUID(FileUtils.getUID(jsonFile));
//        term.setAlternativeLables(FileUtils.getAltLables(jsonFile));
//        term.setBroaderUIDS(FileUtils.getBroaderUIDS(jsonFile));
//        term.setCategories(FileUtils.getCategories(jsonFile));
//        term.setForeignKey(FileUtils.getForeignKey(jsonFile));
//        term.setGlosses(FileUtils.getGlosses(jsonFile));
//        term.setIsFromDictionary(FileUtils.IsFromDictionary(jsonFile));
//        term.setOriginalTerm(FileUtils.getOriginalTerm(jsonFile));
//        term.setConfidence(FileUtils.getConfidence(jsonFile));
//        return term;
//    }
    public static Term create(String jsonStr) throws IOException, ParseException {
        Term term = new Term(FileUtils.getLemma(jsonStr));
        term.setUID(FileUtils.getUID(jsonStr));
        term.setAlternativeLables(FileUtils.getAltLables(jsonStr));
        term.setBroaderUIDS(FileUtils.getBroaderUIDS(jsonStr));
        term.setCategories(FileUtils.getCategories(jsonStr));
        term.setForeignKey(FileUtils.getForeignKey(jsonStr));
        term.setGlosses(FileUtils.getGlosses(jsonStr));
        term.setIsFromDictionary(FileUtils.IsFromDictionary(jsonStr));
        term.setOriginalTerm(FileUtils.getOriginalTerm(jsonStr));
        term.setConfidence(FileUtils.getConfidence(jsonStr));
        return term;
    }

    public static Set<JSONObject> term2Json(List<Term> terms) {
        Set<JSONObject> objs = new HashSet<>();
        for (Term t : terms) {
            objs.add(term2Json(t));
        }
        return objs;
    }

    public static JSONObject term2Json(Term t) {
        JSONObject obj = new JSONObject();
        obj.put("uid", t.getUID());
        obj.put("lemma", t.getLemma());
        obj.put("alternativeLables", t.getAlternativeLables());
        obj.put("broaderUIDS", t.getBroaderUIDS());
        obj.put("categories", t.getCategories());
        obj.put("foreignKey", t.getForeignKey());
        obj.put("glosses", t.getGlosses());
        obj.put("isFromDictionary", t.getIsFromDictionary());
//                    obj.put("narrower", t.getNarrower());
        obj.put("narrowerUIDS", t.getNarrowerUIDS());
//  obj.put("narrowerUIDS", t.getSynonyms());
        obj.put("confidence", t.getConfidence());
        obj.put("originalTerm", t.getOriginalTerm());
        return obj;
    }

    public static Term create(JSONObject page, String originalTerm) {
        String title = (String) page.get("title");
        Long pageid = (Long) page.get("pageid");
        String extract = (String) page.get("extract");
        Term t = null;
        if (extract != null) {
            t = new Term(title.toLowerCase());
            List<String> glosses = new ArrayList<>();
            glosses.add(extract.toLowerCase());
            t.setGlosses(glosses);
            t.setUID(String.valueOf(pageid));
            t.setOriginalTerm(originalTerm);
        }
        return t;
    }

    public static Term create(File file) throws IOException, ParseException {
        try (FileReader fr = new FileReader(file)) {
            String jsonStr = FileUtils.readFile(fr);
            return create(jsonStr);
        }
    }

    public static Set<Term> create(Set<String> terms) throws IOException, ParseException {
        Set<Term> te = new HashSet<>();
        for (String s : terms) {
            te.add(create(s));
        }
        return te;
    }

    public static Set<String> terms2Json(Set<Term> terms) throws IOException, ParseException {
        Set<String> te = new HashSet<>();
        for (Term t : terms) {
            te.add(term2Json(t).toJSONString());
//            te.add(create(s));
        }
        return te;
    }

}
