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
import net.didion.jwnl.JWNLException;
import static nl.uva.sne.commons.SemanticUtils.stem;
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
    public static Term create(String synet, String language, String lemma, String theID, String url) throws ParseException, UnsupportedEncodingException, IOException, JWNLException {
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
                        node = new Term(jlemma, url);
                        node.setUID(babelNetID);
                        node.setCategories(categories);
                        node.setAlternativeLables(altLables);
                        node.setGlosses(glosses);
                        node.setOriginalTerm(lemma);
                        return node;
                    }
                    lemma = java.net.URLDecoder.decode(lemma, "UTF-8");
                    lemma = lemma.replaceAll(" ", "_");

                    String stemjlemma = stem(jlemma);
                    String stemLema = stem(lemma);
                    int dist = edu.stanford.nlp.util.StringUtils.editDistance(stemLema, stemjlemma);

                    if (dist <= 0) {
                        node = new Term(jlemma, url);
                        node.setUID(babelNetID);
                        node.setCategories(categories);
                        node.setAlternativeLables(altLables);
                        node.setGlosses(glosses);
                        node.setOriginalTerm(lemma);
                        return node;
                    }
//                    if (jlemma.contains(lemma) && jlemma.contains("_")) {
//                        String[] parts = jlemma.split("_");
//                        for (String p : parts) {
//                            if (lemma.contains(p)) {
//                                jlemma = p;
//                                break;
//                            }
//                            if (p.contains(lemma)) {
//                                jlemma = lemma;
//                                break;
//                            }
//                        }
//                    }
                    dist = edu.stanford.nlp.util.StringUtils.editDistance(lemma, jlemma);
                    String shorter, longer;
                    if (stemjlemma.length() > stemLema.length()) {
                        shorter = stemLema;
                        longer = stemjlemma;
                    } else {
                        shorter = stemjlemma;
                        longer = stemLema;
                    }

                    if (dist <= 4 && longer.contains(shorter)) {
                        node = new Term(jlemma, url);
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
        Term term = new Term(FileUtils.getLemma(jsonStr), FileUtils.getURL(jsonStr));
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
        Term term = new Term(FileUtils.getLemma(jsonStr), FileUtils.getURL(jsonStr));
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
        obj.put("url", t.getUrl());
        return obj;
    }

    public static Term create(JSONObject page, String originalTerm) {
        String title = (String) page.get("title");
        Long pageid = (Long) page.get("pageid");
        String extract = (String) page.get("extract");
        String url = null;
        Term t = null;
        if (extract != null) {
            t = new Term(title.toLowerCase(), url);
            List<String> glosses = new ArrayList<>();
            glosses.add(extract.toLowerCase());
            t.setGlosses(glosses);
            t.setUID(String.valueOf(pageid));
            url = "https://en.wikipedia.org/?curid=" + t.getUID();
            t.setUrl(url);
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
            Term t = create(s);
            te.add(t);
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

    public static Term merge(Term tv1, Term tv2) throws Exception {
        if (!tv1.getUID().equals(tv2.getUID())) {
            throw new Exception("Can't merge differant terms. UID must be the same");
        }
        Set<String> altLables = new HashSet<>();
        List<String> a1 = tv1.getAlternativeLables();
        if (a1 != null) {
            altLables.addAll(a1);
        }
        List<String> a2 = tv2.getAlternativeLables();
        if (a2 != null) {
            altLables.addAll(a2);
        }

        Set<Term> broader = new HashSet<>();
        List<Term> b1 = tv1.getBroader();
        if (b1 != null) {
            broader.addAll(b1);
        }
        List<Term> b2 = tv2.getBroader();
        if (b2 != null) {
            broader.addAll(b2);
        }

//        List<Term> broads1 = tv1.getBroader();
//        List<Term> broads2 = tv2.getBroader();
//        List<Term> broader = new ArrayList();
//        for (Term b : broads1) {
//            if (!broader.contains(b)) {
//                broader.add(b);
//            }
//        }
//        for (Term b : broads2) {
//            if (!broader.contains(b)) {
//                broader.add(b);
//            }
//        }
        Set<Term> narrower = new HashSet<>();
        List<Term> n1 = tv1.getNarrower();
        if (n1 != null) {
            narrower.addAll(n1);
        }
        List<Term> n2 = tv2.getNarrower();
        if (n2 != null) {
            narrower.addAll(n2);
        }

//                List<Term> narrower = new ArrayList();
//        List<Term> narrs1 = tv1.getNarrower();
//         List<Term> narrs2 = tv2.getNarrower();
//        for (Term n : narrs1) {
//            if (!narrower.contains(n)) {
//                narrower.add(n);
//            }
//        }
//        for (Term n : narrs2) {
//            if (!broader.contains(n)) {
//                narrower.add(n);
//            }
//        }
        Set<String> broaderUIDS = new HashSet<>();
        List<String> buid1 = tv1.getBroaderUIDS();
        if (buid1 != null) {
            broaderUIDS.addAll(buid1);
        }
        List<String> buid2 = tv2.getBroaderUIDS();
        if (buid2 != null) {
            broaderUIDS.addAll(buid2);
        }

        Set<String> categories = new HashSet<>();
        List<String> c1 = tv1.getCategories();
        if (c1 != null) {
            categories.addAll(c1);
        }

        List<String> c2 = tv2.getCategories();
        if (c2 != null) {
            categories.addAll(c2);
        }

        String foreignKey;
        if (tv1.getForeignKey() != null) {
            foreignKey = tv1.getForeignKey();
        } else {
            foreignKey = tv2.getForeignKey();
        }

        Set<String> glosses = new HashSet<>();
        List<String> g1 = tv1.getGlosses();
        if (g1 != null) {
            glosses.addAll(g1);
        }
        List<String> g2 = tv2.getGlosses();
        if (g1 != null) {
            glosses.addAll(g2);
        }

        boolean idFromDic = false;
        if (tv1.getIsFromDictionary() && tv2.getIsFromDictionary()) {
            idFromDic = true;
        }
        if (!tv1.getLemma().equals(tv2.getLemma())) {
            altLables.add(tv2.getLemma());
        }
        String lemma = tv1.getLemma();

        Set<String> narrowerUIDS = new HashSet<>();
        Set<String> nuid1 = tv1.getNarrowerUIDS();
        if (nuid1 != null) {
            narrowerUIDS.addAll(nuid1);
        }
        Set<String> nuid2 = tv2.getNarrowerUIDS();
        if (nuid2 != null) {
            narrowerUIDS.addAll(nuid2);
        }

        Set<Term> synonyms = new HashSet<>();
        List<Term> s1 = tv1.getSynonyms();
        if (s1 != null) {
            synonyms.addAll(s1);
        }

        List<Term> s2 = tv2.getSynonyms();
        if (s2 != null) {
            synonyms.addAll(s2);
        }

        Term mtv = new Term(lemma, "");
        mtv.setUID(tv1.getUID());
        mtv.setAlternativeLables(new ArrayList<>(altLables));
        mtv.setBroader(new ArrayList<>(broader));
        mtv.setBroaderUIDS(new ArrayList<>(broaderUIDS));
        mtv.setCategories(new ArrayList<>(categories));
        mtv.setForeignKey(foreignKey);
        mtv.setGlosses(new ArrayList<>(glosses));
        mtv.setIsFromDictionary(idFromDic);
        mtv.setNarrowerUIDS(narrowerUIDS);
        mtv.setNarrower(new ArrayList<>(narrower));
        mtv.setSynonyms(new ArrayList<>(synonyms));

        return mtv;
    }
}
