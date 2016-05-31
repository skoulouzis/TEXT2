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
public class Wikipedia implements Semantizatior {

    private Integer limit;

    private DB db;
//    private static Map<String, String> extractsCache;
    private static Map<String, Set<String>> termCache;
//    private static Map<String, Set<String>> titlesCache;
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
        "on wikidata",
        "vague or ambiguous time from",
        "stubs"
    };
    private String page = "https://en.wikipedia.org/w/api.php";
    private File cacheDBFile;

    @Override
    public void configure(Properties properties) {
        try {
            limit = Integer.valueOf(properties.getProperty("num.of.terms", "5"));
            String cachePath = properties.getProperty("cache.path");
            minimumSimilarity = Double.valueOf(properties.getProperty("minimum.similarity", "0,3"));

            String fName = FilenameUtils.getName(cachePath);
            String newName = new URL(page).getHost() + "." + fName;
            newName = cachePath.replaceAll(fName, newName);
            cacheDBFile = new File(newName);
        } catch (MalformedURLException ex) {
            Logger.getLogger(Wikipedia.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

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
            Logger.getLogger(Wikipedia.class.getName()).log(Level.WARNING, null, ex);
            return terms;
        } finally {
            saveCache();
        }
        return terms;
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
            Logger.getLogger(Wikipedia.class.getName()).log(Level.INFO, "Couldn''''t figure out what ''{0}'' means", term);
        } else {
            Logger.getLogger(BabelNet.class.getName()).log(Level.INFO, "Term: {0}. Confidence: {1} URL: {2}", new Object[]{dis, dis.getConfidence(), dis.getUrl()});
        }
        return dis;
    }

    private Set<Term> getTermNodeByLemma(String lemma) throws MalformedURLException, IOException, ParseException, UnsupportedEncodingException, JWNLException {
        if (db == null || db.isClosed()) {
            loadCache();
        }

        Set<String> termsStr = termCache.get(lemma);
        if (termsStr != null) {
            return TermFactory.create(termsStr);
        }

        Set<Term> terms = new HashSet<>();
//        Set<String> titlesList = titlesCache.get(lemma);
        URL url;
        String jsonString;

        String query = lemma.replaceAll("_", " ");
        query = URLEncoder.encode(query, "UTF-8");
//sroffset=10
        url = new URL(page + "?action=query&format=json&redirects&list=search&srlimit=500&srsearch=" + query);
        System.err.println(url);
        jsonString = IOUtils.toString(url);
        Set<String> titlesList = getTitles(jsonString, lemma);

        StringBuilder titles = new StringBuilder();

        Iterator<String> it = titlesList.iterator();
        int i = 0;
        while (it.hasNext()) {
            String t = it.next();
            t = URLEncoder.encode(t, "UTF-8");
            titles.append(t).append("|");
            if ((i % 20 == 0 && i > 0) || i >= titlesList.size() - 1) {
                titles.deleteCharAt(titles.length() - 1);
                titles.setLength(titles.length());
                jsonString = null; //extractsCache.get(titles.toString());
                if (jsonString == null) {
                    url = new URL(page + "?format=json&redirects&action=query&prop=extracts&exlimit=max&explaintext&exintro&titles=" + titles.toString());
                    System.err.println(url);
                    jsonString = IOUtils.toString(url);
                    titles = new StringBuilder();
                }
                terms.addAll(getCandidateTerms(jsonString, lemma));
            }
            i++;
        }
        if (db.isClosed()) {
            loadCache();
        }
        termCache.put(lemma, TermFactory.terms2Json(terms));
        db.commit();
        return terms;
    }

    private Set<String> getTitles(String jsonString, String lemma) throws ParseException, UnsupportedEncodingException, IOException, JWNLException {
        Set<String> titles = new TreeSet<>();
        JSONObject jsonObj = (JSONObject) JSONValue.parseWithException(jsonString);
        JSONObject query = (JSONObject) jsonObj.get("query");
        JSONArray search = (JSONArray) query.get("search");
        if (search != null) {
            for (Object o : search) {
                JSONObject res = (JSONObject) o;
                String title = (String) res.get("title");
//                System.err.println(title);
                if (title != null && !title.toLowerCase().contains("(disambiguation)")) {
//                if (title != null) {
                    title = title.replaceAll("%(?![0-9a-fA-F]{2})", "%25");
                    title = title.replaceAll("\\+", "%2B");
                    title = java.net.URLDecoder.decode(title, "UTF-8");
                    title = title.replaceAll("_", " ").toLowerCase();
                    lemma = java.net.URLDecoder.decode(lemma, "UTF-8");
                    lemma = lemma.replaceAll("_", " ");

                    String stemTitle = SemanticUtils.stem(title);
                    String stemLema = SemanticUtils.stem(lemma);
                    String shorter, longer;
                    if (stemTitle.length() > stemLema.length()) {
                        shorter = stemLema;
                        longer = stemTitle;
                    } else {
                        shorter = stemTitle;
                        longer = stemLema;
                    }

                    int dist = edu.stanford.nlp.util.StringUtils.editDistance(stemLema, stemTitle);
                    if (longer.contains(shorter) && dist <= 10) {
                        titles.add(title);
                    }
                }

            }
        }
        titles.add(lemma);
        return titles;
    }

    private Set<Term> getCandidateTerms(String jsonString, String originalTerm) throws ParseException, IOException {
        Set<Term> terms = new HashSet<>();
        JSONObject jsonObj = (JSONObject) JSONValue.parseWithException(jsonString);
        JSONObject query = (JSONObject) jsonObj.get("query");
        JSONObject pages = (JSONObject) query.get("pages");
        Set<String> keys = pages.keySet();

        for (String key : keys) {
            JSONObject jsonpage = (JSONObject) pages.get(key);
            Term t = TermFactory.create(jsonpage, originalTerm);
            boolean add = true;
            if (t != null) {
                List<String> cat = getCategories(t.getUID());
                t.setCategories(cat);
                for (String g : t.getGlosses()) {
                    if (g != null && g.contains("may refer to:")) {
                        Set<Term> referToTerms = getReferToTerms(g, originalTerm);
                        if (referToTerms != null) {
                            for (Term rt : referToTerms) {
                                String url = "https://en.wikipedia.org/?curid=" + rt.getUID();
                                rt.setUrl(url);
                                terms.add(rt);
                            }
                        }

                        add = false;
                        break;
                    }
                }
                String url = "https://en.wikipedia.org/?curid=" + t.getUID();
                t.setUrl(url);
                if (add) {
                    terms.add(t);
                }
            }
        }
        return terms;
    }

    private List<String> getCategories(String uid) throws MalformedURLException, IOException, ParseException {
        URL url = new URL(page + "?action=query&format=json&prop=categories&pageids=" + uid);
        System.err.println(url);
        List<String> categoriesList = new ArrayList<>();
        String jsonString = IOUtils.toString(url);
        JSONObject jsonObj = (JSONObject) JSONValue.parseWithException(jsonString);
        JSONObject query = (JSONObject) jsonObj.get("query");
        JSONObject pages = (JSONObject) query.get("pages");
        Set<String> keys = pages.keySet();
        for (String key : keys) {
            JSONObject p = (JSONObject) pages.get(key);
            JSONArray categories = (JSONArray) p.get("categories");
            if (categories != null) {
                for (Object obj : categories) {
                    JSONObject jObj = (JSONObject) obj;
                    String cat = (String) jObj.get("title");
                    if (shouldAddCategory(cat)) {
//                    System.err.println(cat.substring("Category:".length()).toLowerCase());
                        categoriesList.add(cat.substring("Category:".length()).toLowerCase().replaceAll(" ", "_"));
                    }
                }
            }

        }
        return categoriesList;
    }

    private void loadCache() throws MalformedURLException {
        db = DBMaker.newFileDB(cacheDBFile).make();
        termCache = db.get("termCacheDB");
        if (termCache == null) {
            termCache = db.createHashMap("termCacheDB").keySerializer(Serializer.STRING).valueSerializer(Serializer.BASIC).make();
        }
    }

    private void saveCache() throws FileNotFoundException, IOException {
        Logger.getLogger(Wikipedia.class.getName()).log(Level.FINE, "Saving cache");
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

    private Set<Term> getReferToTerms(String g, String lemma) throws IOException, ParseException {
        String titles = getReferToTitles(g);
        if (titles.length() > 0) {
            URL url = new URL(page + "?format=json&redirects&action=query&prop=extracts&exlimit=max&explaintext&exintro&titles=" + titles);
            System.err.println(url);
            String jsonString = IOUtils.toString(url);
            return getCandidateTerms(jsonString, lemma);
        }

        return null;
    }

    private String getReferToTitles(String g) throws UnsupportedEncodingException {
        String[] titlesArray = g.split("\n");
        StringBuilder titles = new StringBuilder();
        for (String t : titlesArray) {
            if (!t.toLowerCase().contains("may refer to:")) {
                t = URLEncoder.encode(t.split(",")[0], "UTF-8");
                titles.append(t).append("|");
            }
        }
        if (titles.length() > 1) {
            titles.deleteCharAt(titles.length() - 1);
            titles.setLength(titles.length());
        }
        return titles.toString();
    }

}
