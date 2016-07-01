/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.sne.disambiguators;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.didion.jwnl.JWNLException;
import nl.uva.sne.commons.SemanticUtils;
import nl.uva.sne.commons.Term;
import nl.uva.sne.commons.TermFactory;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
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
public class WikipediaOnline extends Wikipedia {

    private DB db;
//    private static Map<String, String> extractsCache;
    private static Map<String, Set<String>> termCache;
//    private static Map<String, Set<String>> titlesCache;

    private File cacheDBFile;
    private Object prevTitles = "";

    @Override
    public void configure(Properties properties) {
        try {
            super.configure(properties);

            String fName = FilenameUtils.getName(getCachePath());
            String newName = new URL(page).getHost() + "." + fName;
            newName = getCachePath().replaceAll(fName, newName);
            cacheDBFile = new File(newName);
        } catch (MalformedURLException ex) {
            Logger.getLogger(WikipediaOnline.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public Term getTerm(String term) throws IOException, ParseException, JWNLException, MalformedURLException, UnsupportedEncodingException {
        Set<Term> possibleTerms = null;
        try {
            possibleTerms = getTermNodeByLemma(term);
        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(WikipediaOnline.class.getName()).log(Level.SEVERE, null, ex);
        }
//        if (possibleTerms != null & possibleTerms.size() > 1) {
        Term dis = SemanticUtils.disambiguate(term, possibleTerms, getAllTermsDictionaryPath(), getMinimumSimilarity(), true);
//        } else if (possibleTerms.size() == 1) {
//            return possibleTerms.iterator().next();
//        }
//        return null;
        if (dis == null) {
            Logger.getLogger(WikipediaOnline.class.getName()).log(Level.INFO, "Couldn''''t figure out what ''{0}'' means", term);
        } else {
            Logger.getLogger(BabelNet.class.getName()).log(Level.INFO, "Term: {0}. Confidence: {1} URL: {2}", new Object[]{dis, dis.getConfidence(), dis.getUrl()});
        }
        return dis;
    }

    @Override
    protected Set<Term> getTermNodeByLemma(String lemma) throws MalformedURLException, IOException, ParseException, UnsupportedEncodingException, JWNLException, InterruptedException, ExecutionException {
        
        Set<String> termsStr = getFromTermCache(lemma);
        if (termsStr != null) {
            return TermFactory.create(termsStr);
        }

        Set<Term> terms = new HashSet<>();

        URL url;
        String jsonString;

        Set<String> titlesList = getTitles(lemma);

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
                jsonString = null;
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
        putInTermCache(lemma, TermFactory.terms2Json(terms));
        return terms;
    }

    private Set<Term> getCandidateTerms(String jsonString, String originalTerm) throws ParseException, IOException, MalformedURLException, InterruptedException, ExecutionException {
        Set<Term> terms = new HashSet<>();
        Set<Term> termsToReturn = new HashSet<>();
        JSONObject jsonObj = (JSONObject) JSONValue.parseWithException(jsonString);
        JSONObject query = (JSONObject) jsonObj.get("query");
        JSONObject pages = (JSONObject) query.get("pages");
        Set<String> keys = pages.keySet();
        for (String key : keys) {
            JSONObject jsonpage = (JSONObject) pages.get(key);
            Term t = TermFactory.create(jsonpage, originalTerm);
            if (t != null) {
                terms.add(t);
            }
        }
        if (terms.size() > 0) {
            Map<String, List<String>> cats = getCategories(terms);
            for (Term t : terms) {
                boolean add = true;
                List<String> cat = cats.get(t.getUID());
                t.setCategories(cat);
                for (String g : t.getGlosses()) {
                    if (g != null && g.contains("may refer to:")) {
                        Set<Term> referToTerms = getReferToTerms(g, originalTerm);
                        if (referToTerms != null) {
                            for (Term rt : referToTerms) {
                                String url = "https://en.wikipedia.org/?curid=" + rt.getUID();
                                rt.setUrl(url);
                                termsToReturn.add(rt);
                            }
                        }
                        add = false;
                        break;
                    }
                }
                if (add) {
                    String url = "https://en.wikipedia.org/?curid=" + t.getUID();
                    t.setUrl(url);
                    termsToReturn.add(t);
                }
            }
        }
        return termsToReturn;
    }

    private Map<String, List<String>> getCategories(Set<Term> terms) throws MalformedURLException, InterruptedException, ExecutionException {
        int maxT = 2;
        ExecutorService pool = new ThreadPoolExecutor(maxT, maxT,
                5000L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(maxT, true), new ThreadPoolExecutor.CallerRunsPolicy());

        Map<String, List<String>> cats = new HashMap<>();
        Set<Future<Map<String, List<String>>>> set = new HashSet<>();
        for (Term t : terms) {
            URL url = new URL(page + "?action=query&format=json&prop=categories&pageids=" + t.getUID());
            System.err.println(url);
            WikiRequestor req = new WikiRequestor(url, t.getUID(), 0);
            Future<Map<String, List<String>>> future = pool.submit(req);
            set.add(future);
        }
        pool.shutdown();

        for (Future<Map<String, List<String>>> future : set) {
            while (!future.isDone()) {
//                Logger.getLogger(WikipediaOnline.class.getName()).log(Level.INFO, "Task is not completed yet....");
                Thread.currentThread().sleep(10);
            }
            Map<String, List<String>> c = future.get();
            if (c != null) {
                cats.putAll(c);
            }
        }

        return cats;
    }

    private Set<Term> getReferToTerms(String g, String lemma) throws IOException, ParseException, MalformedURLException, InterruptedException, ExecutionException {
        String titles = getReferToTitles(g);
        if (titles.length() > 0 && !titles.equals(prevTitles)) {
            URL url = new URL(page + "?format=json&redirects&action=query&prop=extracts&exlimit=max&explaintext&exintro&titles=" + titles);
            System.err.println(url);
            String jsonString = IOUtils.toString(url);
            prevTitles = titles;
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
                if (t.length() > 0) {
                    titles.append(t).append("|");
                }
            }
        }
        if (titles.length() > 1) {
            titles.deleteCharAt(titles.length() - 1);
            titles.setLength(titles.length());
        }
        return titles.toString();
    }

    private void putInTermCache(String lemma, Set<String> terms2Json) throws InterruptedException, IOException {
        File lock = waitForDB(cacheDBFile);
        lock.createNewFile();
        loadTermCache();
        termCache.put(lemma, terms2Json);
        db.commit();
        db.close();
        lock.delete();
    }

    private void loadTermCache() {
        if (db == null || db.isClosed()) {
            db = DBMaker.newFileDB(cacheDBFile).make();
        }
        termCache = db.getHashMap("termCacheDB");
        if (termCache == null) {
            termCache = db.createHashMap("termCacheDB").keySerializer(Serializer.STRING).valueSerializer(Serializer.STRING).make();
        }
    }

    private Set<String> getFromTermCache(String lemma) throws InterruptedException, IOException {
        File lock = waitForDB(cacheDBFile);
        lock.createNewFile();
        loadTermCache();
        Set<String> termsStr = termCache.get(lemma);
        db.close();
        lock.delete();
        return termsStr;
    }
}
