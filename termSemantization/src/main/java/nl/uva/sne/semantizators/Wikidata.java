/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.sne.semantizators;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.didion.jwnl.JWNLException;
import nl.uva.sne.commons.SemanticUtils;
import nl.uva.sne.commons.Term;
import nl.uva.sne.commons.TermFactory;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;

/**
 *
 * @author S. Koulouzis
 */
public class Wikidata implements Semantizatior {

    private Integer limit;

    private DB db;
    private String cachePath;
//    private static Map<String, String> extractsCache;
    private static Map<String, Set<String>> termCache;
    private static Map<String, Set<String>> titlesCache;
    private Double minimumSimilarity;

    private static final String[] EXCLUDED_CAT = new String[]{
        "articles needing",
        "articles lacking",
        "articles with",
        "articles containing",
        "articles to",
        "article disambiguation",
        "articles incorporating",
        "articles covered",
        "articles created",
        "articles that",
        "cs1 ",
        "disambiguation pages",
        "set index articles",
        "copied and pasted articles",
        "cleanup tagged articles",
        "pages needing",
        "pages lacking",
        "pages with",
        "pages using",
        "disambiguation pages",
        "use dmy dates",
        "use mdy dates",
        "all stub articles",
        "orphaned articles",
        "wikipedia introduction",
        "wikipedia articles",
        "wikipedia external",
        "wikipedia indefinitely",
        "wikipedia spam",
        "on wikidata"
    };
    private String page = "https://www.wikidata.org/w/api.php";

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
                if (term.length() >= 1) {
                    count++;
                    if (count > limit) {
                        break;
                    }
                    Term tt = getTerm(term, allTermsDictionary, minimumSimilarity);
                    if (tt != null) {
                        terms.add(tt);
                    }
                }
            }
        } catch (Exception ex) {
            Logger.getLogger(Wikidata.class.getName()).log(Level.WARNING, null, ex);
            return terms;
        } finally {
            saveCache();
        }
        return terms;
    }

    @Override
    public void configure(Properties properties) {
        limit = Integer.valueOf(properties.getProperty("num.of.terms", "5"));
        cachePath = properties.getProperty("cache.path");
        minimumSimilarity = Double.valueOf(properties.getProperty("minimum.similarity", "0,3"));
    }

    @Override
    public Term getTerm(String term, String allTermsDictionary, double confidence) throws IOException, ParseException, JWNLException {
        Set<Term> possibleTerms = getTermNodeByLemma(term);
//        if (possibleTerms != null & possibleTerms.size() > 1) {
        Term dis = SemanticUtils.disambiguate(term, possibleTerms, allTermsDictionary, confidence, true);
//        } else if (possibleTerms.size() == 1) {
//            return possibleTerms.iterator().next();
//        }
//        return null;
        if (dis == null) {
            Logger.getLogger(Wikidata.class.getName()).log(Level.INFO, "Couldn''''t figure out what ''{0}'' means", term);
        } else {
            Logger.getLogger(Wikidata.class.getName()).log(Level.INFO, "Term: {0}. Confidence: {1}", new Object[]{dis, dis.getConfidence()});
        }
        return dis;
    }

    private Set<Term> getTermNodeByLemma(String lemma) throws MalformedURLException, IOException, ParseException {
        if (db == null || db.isClosed()) {
            loadCache();
        }

        Set<String> termsStr = termCache.get(lemma);
        if (termsStr != null) {
            return TermFactory.create(termsStr);
        }
        Set<Term> terms = null;
        String query = lemma.replaceAll("_", " ");
        query = URLEncoder.encode(query, "UTF-8");
        int i = 0;
        URL url = new URL(page + "?action=wbsearchentities&format=json&language=en&continue=" + i + "&limit=50&search=" + query);
        System.err.println(url);
        String jsonString = IOUtils.toString(url);
        terms = getCandidateTerms(jsonString, lemma);

        if (db.isClosed()) {
            loadCache();
        }
        termCache.put(lemma, TermFactory.terms2Json(terms));
        db.commit();
        return terms;
    }

    private List<String> getBroaderID(String id) throws MalformedURLException, IOException, ParseException {

        return getNumProperty(id, "P31");

    }

    private Set<Term> getCandidateTerms(String jsonString, String originalTerm) throws ParseException, IOException {
        Set<Term> terms = new HashSet<>();
        JSONObject jsonObj = (JSONObject) JSONValue.parseWithException(jsonString);
        JSONArray search = (JSONArray) jsonObj.get("search");
        for (Object obj : search) {
            JSONObject jObj = (JSONObject) obj;
            String label = (String) jObj.get("label");
            if (label != null && !label.toLowerCase().contains("(disambiguation)")) {

                label = label.replaceAll("%(?![0-9a-fA-F]{2})", "%25");
                label = label.replaceAll("\\+", "%2B");
                label = java.net.URLDecoder.decode(label, "UTF-8");
                label = label.replaceAll("_", " ").toLowerCase();
                originalTerm = java.net.URLDecoder.decode(originalTerm, "UTF-8");
                originalTerm = originalTerm.replaceAll("_", " ");
                int dist;
                if (!label.startsWith("(") && label.contains("(")) {
                    int index = label.indexOf("(") - 1;
                    String sub = label.substring(0, index);
                    dist = edu.stanford.nlp.util.StringUtils.editDistance(originalTerm, sub);
                } else {
                    dist = edu.stanford.nlp.util.StringUtils.editDistance(originalTerm, label);
                }
                if (label.contains(originalTerm) && dist <= 7) {
                    Term t = new Term(label);

                    JSONArray aliases = (JSONArray) jObj.get("aliases");
                    if (aliases != null) {
                        List<String> altLables = new ArrayList<>();
                        for (Object aObj : aliases) {
                            String alt = (String) aObj;
                            altLables.add(alt);
                        }
                        t.setAlternativeLables(altLables);
                    }

                    String description = (String) jObj.get("description");
                    String id = (String) jObj.get("id");
                    List<String> glosses = new ArrayList<>();
                    glosses.add(description);
                    t.setGlosses(glosses);
                    t.setUID(id);

                    List<String> broaderID = getBroaderID(id);
                    t.setBroaderUIDS(broaderID);

                    List<String> cat = getCategories(id);
                    t.setCategories(cat);

                    terms.add(t);
                }

            }

        }
        return terms;
    }

    private void loadCache() throws MalformedURLException {
        String fName = FilenameUtils.getName(cachePath);
        String newName = new URL(page).getHost() + "." + fName;
        cachePath = cachePath.replaceAll(fName, newName);

        File cacheDBFile = new File(cachePath);
        db = DBMaker.newFileDB(cacheDBFile).make();
//        extractsCache = db.getHashMap("extractsCacheDB");

//        if (extractsCache == null) {
//            extractsCache = db.createHashMap("extractsCacheDB").keySerializer(Serializer.STRING).valueSerializer(Serializer.STRING).make();
//        }
        titlesCache = db.get("titlesCacheDB");
        if (titlesCache == null) {
            titlesCache = db.createHashMap("titlesCacheDB").keySerializer(Serializer.STRING).valueSerializer(Serializer.BASIC).make();
        }
        termCache = db.get("termCacheDB");
        if (termCache == null) {
            termCache = db.createHashMap("termCacheDB").keySerializer(Serializer.STRING).valueSerializer(Serializer.BASIC).make();
        }
    }

    private void saveCache() throws FileNotFoundException, IOException {
        Logger.getLogger(Wikidata.class.getName()).log(Level.FINE, "Saving cache");
        if (db != null) {
            if (!db.isClosed()) {
                db.commit();
                db.close();
            }
        }
    }

    private boolean shouldAddCategory(String cat) {
        for (String s : EXCLUDED_CAT) {
            if (cat.toLowerCase().contains(s)) {
                return false;
            }
        }
        return true;
    }

    private List<String> getNumProperty(String id, String prop) throws MalformedURLException, IOException, ParseException {
        URL url = new URL(page + "?action=wbgetclaims&format=json&props=&property=" + prop + "&entity=" + id);
        System.err.println(url);
        String jsonString = IOUtils.toString(url);
        JSONObject jsonObj = (JSONObject) JSONValue.parseWithException(jsonString);

        JSONObject claims = (JSONObject) jsonObj.get("claims");

        JSONArray Jprop = (JSONArray) claims.get(prop);
        List<String> ids = new ArrayList<>();
        if (Jprop != null) {
            for (Object obj : Jprop) {
                JSONObject jobj = (JSONObject) obj;

                JSONObject mainsnak = (JSONObject) jobj.get("mainsnak");
//            System.err.println(mainsnak);
                JSONObject datavalue = (JSONObject) mainsnak.get("datavalue");
//            System.err.println(datavalue);
                JSONObject value = (JSONObject) datavalue.get("value");
//            System.err.println(value);
                java.lang.Long numericID = (java.lang.Long) value.get("numeric-id");
//                System.err.println(id + " -> Q" + numericID);
                ids.add("Q" + numericID);
            }
        }

        return ids;
    }

    private List<String> getCategories(String id) throws IOException, MalformedURLException, ParseException {
        List<String> ids = getNumProperty(id, "P910");
        List<String> lables = new ArrayList();
        if (ids != null) {
            for (String s : ids) {
                String l = getLabel(s);
                lables.add(l);
            }
        }

        return lables;
    }

    private String getLabel(String id) throws MalformedURLException, IOException, ParseException {

        URL url = new URL(page + "?action=wbgetentities&format=json&props=labels&languages=en&ids=" + id);
        System.err.println(url);
        String jsonString = IOUtils.toString(url);
        JSONObject jsonObj = (JSONObject) JSONValue.parseWithException(jsonString);

        JSONObject entities = (JSONObject) jsonObj.get("entities");
//        System.err.println(entities);
        JSONObject jID = (JSONObject) entities.get(id);

        JSONObject labels = (JSONObject) jID.get("labels");
//        System.err.println(labels);
        JSONObject en = (JSONObject) labels.get("en");
//        System.err.println(en);
        if (en != null) {
            String value = (String) en.get("value");

            return value.substring("Category:".length()).toLowerCase();
        }
        return null;
    }

}
