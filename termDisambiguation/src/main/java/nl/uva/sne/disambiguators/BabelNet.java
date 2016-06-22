/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.sne.disambiguators;

import edu.stanford.nlp.util.Pair;
import java.util.Properties;

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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.didion.jwnl.JWNLException;
import nl.uva.sne.commons.FileUtils;
import nl.uva.sne.commons.SemanticUtils;
import nl.uva.sne.commons.Term;
import nl.uva.sne.commons.TermFactory;
import nl.uva.sne.commons.ValueComparator;
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
public class BabelNet implements Disambiguator {

    private String keysStr;
    private DB db;

    private static Map<String, String> synsetCache;
    private static Map<String, List<String>> wordIDCache;
    private static Map<String, String> disambiguateCache;
    private static Map<String, String> edgesCache;

    private int limit;
    private String key;
    private String[] keys;
    private int keyIndex = 0;
    private Double minimumSimilarity;
    private File cacheDBFile;
    private Integer lineOffset;

    @Override
    public List<Term> disambiguateTerms(String allTermsDictionary, String filterredDictionary) throws IOException, ParseException {
        List<Term> terms = new ArrayList<>();
        File dictionary = new File(filterredDictionary);
        int count = 0;
        int lineCount = 1;
        try (BufferedReader br = new BufferedReader(new FileReader(dictionary))) {
            for (String line; (line = br.readLine()) != null;) {
                if (lineCount >= lineOffset) {
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
                lineCount++;
            }
        } catch (Exception ex) {
            Logger.getLogger(SemanticUtils.class.getName()).log(Level.WARNING, null, ex);
            return terms;
        }
        return terms;
    }

    @Override
    public Term getTerm(String term, String allTermsDictionary, double minimumSimilarity) throws IOException, ParseException, JWNLException, UnsupportedEncodingException, FileNotFoundException {
        Set<Term> possibleTerms;
        try {
            possibleTerms = getTermNodeByLemma(term);

//        if (possibleTerms != null & possibleTerms.size() > 1) {
            Term dis = disambiguate(term, possibleTerms, allTermsDictionary, minimumSimilarity);
//        } else if (possibleTerms.size() == 1) {
//            return possibleTerms.iterator().next();
//        }
//        return null;
            if (dis == null) {
                Logger.getLogger(BabelNet.class.getName()).log(Level.INFO, "Couldn''''t figure out what ''{0}'' means", term);
            } else {
                Logger.getLogger(BabelNet.class.getName()).log(Level.INFO, "Term: {0}. Confidence: {1} URL: {2}", new Object[]{dis, dis.getConfidence(), dis.getUrl()});
            }
            return dis;
        } catch (InterruptedException ex) {
            Logger.getLogger(BabelNet.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    private Set<Term> getTermNodeByLemma(String term) throws IOException, ParseException, UnsupportedEncodingException, JWNLException, FileNotFoundException, InterruptedException {
        String language = "EN";
        List<String> ids = getcandidateWordIDs(language, term, key);
        Set<Term> nodes = new HashSet<>();
        if (ids != null) {
            for (String id : ids) {
                String synet = getBabelnetSynset(id, language, key);
                String url = null;
                Term node = TermFactory.create(synet, language, term, null, url);
                if (node != null) {
                    try {
                        url = "http://babelnet.org/synset?word=" + URLEncoder.encode(node.getUID(), "UTF-8");
                        node.setUrl(url);
                        List<Term> h = getHypernyms(language, node, key);
                        if (h != null && !h.isEmpty()) {
                            node.setBroader(h);
                            for (Term t : h) {
                                node.addBroaderUID(t.getUID());
                            }
                        }
                    } catch (Exception ex) {
                        Logger.getLogger(SemanticUtils.class.getName()).log(Level.WARNING, null, ex);
                    }
                    nodes.add(node);
                }
            }
        }
        return nodes;
    }

    private String getBabelnetSynset(String id, String lan, String key) throws IOException, FileNotFoundException, InterruptedException {
        if (db == null || db.isClosed()) {
            loadCache();
        }
        if (id == null || id.length() < 1) {
            return null;
        }
        String json = synsetCache.get(id);
        if (json != null && json.equals("NON-EXISTING")) {
            return null;
        }
        if (json == null) {
            URL url = new URL("http://babelnet.io/v2/getSynset?id=" + id + "&filterLangs=" + lan + "&langs=" + lan + "&key=" + key);
            json = IOUtils.toString(url);
            handleKeyLimitException(json);
            if (db.isClosed()) {
                loadCache();
            }

            if (json != null) {
                synsetCache.put(id, json);
                db.commit();
            } else {
                synsetCache.put(id, "NON-EXISTING");
                db.commit();
            }
        }

        return json;
    }

    @Override
    public void configure(Properties properties) {
        keysStr = properties.getProperty("bablenet.key");
        keys = keysStr.split(",");
        key = keys[keyIndex];
        String cachePath = properties.getProperty("cache.path");
        String fName = FilenameUtils.getName(cachePath);
        String newName = this.getClass().getSimpleName() + "." + fName;
        cachePath = cachePath.replaceAll(fName, newName);
        cacheDBFile = new File(cachePath);

        limit = Integer.valueOf(properties.getProperty("num.of.terms", "5"));
        lineOffset = Integer.valueOf(properties.getProperty("offset.terms", "0"));
        minimumSimilarity = Double.valueOf(properties.getProperty("minimum.similarity", "0,3"));
    }

    private List<String> getcandidateWordIDs(String language, String word, String key) throws IOException, ParseException, FileNotFoundException, InterruptedException {
        if (db == null || db.isClosed()) {
            loadCache();
        }
        List<String> ids = wordIDCache.get(word);
        if (ids != null && ids.size() == 1 && ids.get(0).equals("NON-EXISTING")) {
            return null;
        }
        language = language.toUpperCase();
        if (ids == null || ids.isEmpty()) {
            ids = new ArrayList<>();
            URL url = new URL("http://babelnet.io/v2/getSynsetIds?word=" + word + "&langs=" + language + "&key=" + key);
            String genreJson = IOUtils.toString(url);

            handleKeyLimitException(genreJson);

            Object obj = JSONValue.parseWithException(genreJson);
            if (obj instanceof JSONArray) {
                JSONArray jsonArray = (JSONArray) obj;
                for (Object o : jsonArray) {
                    JSONObject jo = (JSONObject) o;
                    if (jo != null) {
                        String id = (String) jo.get("id");
                        if (id != null) {
                            ids.add(id);
                        }
                    }
                }
            } else if (obj instanceof JSONObject) {
                JSONObject jsonObj = (JSONObject) obj;
                String id = (String) jsonObj.get("id");
                if (id != null) {
                    ids.add(id);
                }
            }
            if (db.isClosed()) {
                loadCache();
            }

            if (ids.isEmpty()) {
                ids.add("NON-EXISTING");
                wordIDCache.put(word, ids);
                commitDB();
                return null;
            }
            wordIDCache.put(word, ids);
            commitDB();
        }
        return ids;
    }

    private void loadCache() throws FileNotFoundException, IOException, InterruptedException {
        File lock = new File(cacheDBFile.getAbsolutePath() + ".lock");
        int count = 0;
        long sleepTime = 5;
        while (lock.exists()) {
            sleepTime = sleepTime * 2;
            count++;
            if (count >= 10) {
                break;
            }
            Logger.getLogger(SemanticUtils.class.getName()).log(Level.INFO, "DB locked. Sleeping: " + sleepTime + " " + count);
            Thread.sleep(sleepTime);
        }

        lock.createNewFile();
        db = DBMaker.newFileDB(cacheDBFile).make();
        synsetCache = db.getHashMap("synsetCacheDB");
        if (synsetCache == null) {
            synsetCache = db.createHashMap("synsetCacheDB").keySerializer(Serializer.STRING).valueSerializer(Serializer.STRING).make();
        }
        wordIDCache = db.get("wordIDCacheDB");
        if (wordIDCache == null) {
            wordIDCache = db.createHashMap("wordIDCacheDB").keySerializer(Serializer.STRING).valueSerializer(Serializer.BASIC).make();
        }

        disambiguateCache = db.get("disambiguateCacheDB");
        if (disambiguateCache == null) {
            disambiguateCache = db.createHashMap("").keySerializer(Serializer.STRING).valueSerializer(Serializer.STRING).make();
        }

        edgesCache = db.getHashMap("edgesCacheDB");
        if (edgesCache == null) {
            edgesCache = db.createHashMap("edgesCacheDB").keySerializer(Serializer.STRING).valueSerializer(Serializer.STRING).make();
        }
        db.commit();
        lock.delete();

    }

    private void handleKeyLimitException(String genreJson) throws IOException, FileNotFoundException, InterruptedException {
        if (genreJson.contains("Your key is not valid or the daily requests limit has been reached")) {
            saveCache();
            keyIndex++;
            if (keyIndex > keys.length - 1) {
                keyIndex = 0;
            }
            key = keys[keyIndex];
            Logger.getLogger(BabelNet.class.getName()).log(Level.FINE, "Switch to: {0}", keyIndex);
            throw new IOException(genreJson);
        }
    }

    private void saveCache() throws FileNotFoundException, IOException, InterruptedException {
        Logger.getLogger(BabelNet.class.getName()).log(Level.FINE, "Saving cache");
        if (db != null) {
            if (!db.isClosed()) {
                commitDB();
                db.close();
            }
        }
    }

    private List<Term> getHypernyms(String language, Term t, String key) throws MalformedURLException, IOException, ParseException, Exception {
        Map<String, Double> hypenymMap = getEdgeIDs(language, t.getUID(), "HYPERNYM", key);
        List<Term> hypernyms = new ArrayList<>();

        ValueComparator bvc = new ValueComparator(hypenymMap);
        Map<String, Double> sorted_map = new TreeMap(bvc);
        sorted_map.putAll(hypenymMap);
        int maxNumOfHyper = 5;

        for (String uid : sorted_map.keySet()) {
            if (maxNumOfHyper <= 0) {
                break;
            }

            String synetHyper = getBabelnetSynset(uid, language, key);
            String url = "http://babelnet.org/synset?word=" + URLEncoder.encode(uid, "UTF-8");
            Term hypernym = TermFactory.create(synetHyper, language, null, uid, url);
            if (hypernym != null) {
                hypernyms.add(hypernym);
            }

            maxNumOfHyper--;
        }
//        hypenymMap = getEdgeIDs(language, t.getUID(), "MERONYM", key);
        return hypernyms;
    }

    private Map<String, Double> getEdgeIDs(String language, String id, String relation, String key) throws MalformedURLException, IOException, ParseException, Exception {
        if (db == null || db.isClosed()) {
            loadCache();
        }
        String genreJson = edgesCache.get(id);
        if (genreJson == null) {
            URL url = new URL("http://babelnet.io/v2/getEdges?id=" + id + "&key=" + key);
            genreJson = IOUtils.toString(url);
            handleKeyLimitException(genreJson);
            if (genreJson != null) {
                edgesCache.put(id, genreJson);
            }
            if (genreJson == null) {
                edgesCache.put(id, "NON-EXISTING");
            }
            commitDB();
        }
        Object obj = JSONValue.parseWithException(genreJson);
        JSONArray edgeArray = (JSONArray) obj;
        Map<String, Double> map = new HashMap<>();
        for (Object o : edgeArray) {
            JSONObject pointer = (JSONObject) ((JSONObject) o).get("pointer");
            String relationGroup = (String) pointer.get("relationGroup");
            String target = (String) ((JSONObject) o).get("target");
            Double normalizedWeight = (Double) ((JSONObject) o).get("normalizedWeight");
            Double weight = (Double) ((JSONObject) o).get("weight");
            if (relationGroup.equals(relation)) {
                map.put(target, ((normalizedWeight + weight) / 2.0));
            }
        }
        return map;
    }

    private Term disambiguate(String term, Set<Term> possibleTerms, String termDictionaryFile, double minimumSimilarity) throws IOException, JWNLException, ParseException {
        Term dis = SemanticUtils.disambiguate(term, possibleTerms, termDictionaryFile, minimumSimilarity, true);
        if (dis != null) {
            return dis;
        } else {
            Set<String> ngarms = FileUtils.getNGramsFromTermDictionary(term, termDictionaryFile);
            possibleTerms = babelNetDisambiguation("EN", term, ngarms);
            if (possibleTerms != null && possibleTerms.size() == 1) {
                dis = possibleTerms.iterator().next();
            }
        }
        return dis;
    }

    private Set<Term> babelNetDisambiguation(String language, String lemma, Set<String> ngarms) {
        if (ngarms.isEmpty()) {
            return null;
        }
        if (ngarms.size() == 1 && ngarms.iterator().next().length() <= 1) {
            return null;
        }

        HashMap<String, Double> idsMap = new HashMap<>();
        Map<String, Term> termMap = new HashMap<>();
        Set<Term> terms = new HashSet<>();
        int count = 0;
        int breaklimit = 1000;
        int oneElementlimit = 65;
        int difflimit = 60;
        Double persent;
        for (String n : ngarms) {
            if (n.length() <= 1) {
                continue;
            }
            count++;
            if (idsMap.size() == 1 && count > oneElementlimit) {
//                Double score = idsMap.values().iterator().next();
//                if (score >= 10) {
                break;
//                }
            }

            if ((count % 2) == 0 && idsMap.size() >= 2 && count > difflimit) {
                ValueComparator bvc = new ValueComparator(idsMap);
                TreeMap<String, Double> sorted_map = new TreeMap(bvc);
                sorted_map.putAll(idsMap);
                Iterator<String> iter = sorted_map.keySet().iterator();
                Double first = idsMap.get(iter.next());
                Double second = idsMap.get(iter.next());

                persent = first / (first + second);
                if (persent > 0.65) {
                    break;
                }
            }
            if (count > breaklimit) {
                break;
            }

            String clearNg = n.replaceAll("_", " ");
            if (clearNg == null) {
                continue;
            }
            if (clearNg.startsWith(" ")) {
                clearNg = clearNg.replaceFirst(" ", "");
            }
            if (clearNg.endsWith(" ")) {
                clearNg = clearNg.substring(0, clearNg.length() - 1);
            }

            Pair<Term, Double> termPair = null;
            try {
                termPair = babelNetDisambiguation(language, lemma, clearNg);
            } catch (Exception ex) {
                if (ex.getMessage() != null && ex.getMessage().contains("Your key is not valid")) {
                    try {
                        termPair = babelNetDisambiguation(language, lemma, clearNg);
                    } catch (Exception ex1) {
//                        Logger.getLogger(BabelNet.class.getName()).log(Level.WARNING, ex1, null);
                    }
                } else {
                    Logger.getLogger(SemanticUtils.class.getName()).log(Level.WARNING, null, ex);
                }
            }
            if (termPair != null) {
                termMap.put(termPair.first.getUID(), termPair.first);
                Double score;
                if (idsMap.containsKey(termPair.first.getUID())) {
                    score = idsMap.get(termPair.first.getUID());
//                    score++;
                    score += termPair.second;
                } else {
//                    score = 1.0;
                    score = termPair.second;
                }
                idsMap.put(termPair.first.getUID(), score);
            }
        }
        if (!idsMap.isEmpty()) {
            ValueComparator bvc = new ValueComparator(idsMap);
            TreeMap<String, Double> sorted_map = new TreeMap(bvc);
            sorted_map.putAll(idsMap);
            count = 0;
            Double firstScore = idsMap.get(sorted_map.firstKey());
            terms.add(termMap.get(sorted_map.firstKey()));
            idsMap.remove(sorted_map.firstKey());
            for (String tvID : sorted_map.keySet()) {
                if (count >= 1) {
                    Double secondScore = idsMap.get(tvID);
                    persent = secondScore / (firstScore + secondScore);
                    if (persent > 0.2) {
                        terms.add(termMap.get(tvID));
                    }
                    if (count >= 2) {
                        break;
                    }
                }
                count++;
            }
            return terms;
        }
        return null;
    }

    private Pair<Term, Double> babelNetDisambiguation(String language, String lemma, String sentence) throws IOException, ParseException, Exception {
        if (lemma == null || lemma.length() < 1) {
            return null;
        }
        if (db == null || db.isClosed()) {
            loadCache();
        }
        String query = lemma + " " + sentence.replaceAll("_", " ");

        query = URLEncoder.encode(query, "UTF-8");
        String genreJson = null;
        try {
            genreJson = disambiguateCache.get(sentence);
        } catch (Exception ex) {
            System.err.println("");
        }
        if (genreJson != null && genreJson.equals("NON-EXISTING")) {
            return null;
        }
        if (genreJson == null) {
            URL url = new URL("http://babelfy.io/v1/disambiguate?text=" + query + "&lang=" + language + "&key=" + key);
            genreJson = IOUtils.toString(url);
            handleKeyLimitException(genreJson);
            if (db.isClosed()) {
                loadCache();
            }
            if (!genreJson.isEmpty() || genreJson.length() < 1) {
                disambiguateCache.put(sentence, genreJson);
            } else {
                disambiguateCache.put(sentence, "NON-EXISTING");
            }
            commitDB();
        }
        Object obj = JSONValue.parseWithException(genreJson);
//        Term term = null;
        if (obj instanceof JSONArray) {
            JSONArray ja = (JSONArray) obj;
            for (Object o : ja) {
                JSONObject jo = (JSONObject) o;
                String id = (String) jo.get("babelSynsetID");
                Double score = (Double) jo.get("score");
                Double globalScore = (Double) jo.get("globalScore");
                Double coherenceScore = (Double) jo.get("coherenceScore");
                double someScore = (score + globalScore + coherenceScore) / 3.0;
                String synet = getBabelnetSynset(id, language, keysStr);
                String url = "http://babelnet.org/synset?word=" + URLEncoder.encode(id, "UTF-8");
                Term t = TermFactory.create(synet, language, lemma, null, url);
                if (t != null) {
                    List<Term> h = getHypernyms(language, t, keysStr);
                    t.setBroader(h);
                    return new Pair<>(t, someScore);
                }
            }
        }
        return null;
    }

    private void commitDB() throws InterruptedException, IOException {
        File lock = new File(cacheDBFile.getAbsolutePath() + ".lock");
        int count = 0;
        long sleepTime = 5;
        while (lock.exists()) {
            sleepTime = sleepTime * 2;
            count++;
            if (count >= 10) {
                break;
            }
            Logger.getLogger(SemanticUtils.class.getName()).log(Level.INFO, "DB locked. Sleeping: " + sleepTime + " " + count);
            Thread.sleep(sleepTime);
        }
        lock.createNewFile();
        db.commit();
        lock.delete();
    }

}
