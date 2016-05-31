/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.sne.commons;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.didion.jwnl.JWNL;
import net.didion.jwnl.JWNLException;
import net.didion.jwnl.data.IndexWord;
import net.didion.jwnl.data.IndexWordSet;
import net.didion.jwnl.data.POS;
import net.didion.jwnl.dictionary.Dictionary;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.hy.ArmenianAnalyzer;
import org.apache.lucene.analysis.shingle.ShingleFilter;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.util.Version;
import org.json.simple.parser.ParseException;

/**
 *
 * @author S. Koulouzis
 */
public class SemanticUtils {

    static Set<String> stopwords = new HashSet();
    public static String stopwordsFile = System.getProperty("user.home")
            + File.separator + "workspace" + File.separator + "TEXT2"
            + File.separator + "etc" + File.separator + "sropwords";

    static Map<String, Map<String, Double>> termDocCache;

    static Set<String> nonLematizedWords = new HashSet();
    private static Dictionary wordNetdictionary;

    private static File nonLematizedWordsFile = new File(
            System.getProperty("user.home")
            + File.separator + "workspace" + File.separator + "TEXT2"
            + File.separator + "termXtraction" + File.separator + "etc" + File.separator + "nonLematizedWords");
    private static Map<String, String> stemCache = new HashMap<>();

    static {
        try {
            JWNL.initialize(new FileInputStream(System.getProperty("user.home")
                    + File.separator + "workspace" + File.separator + "TEXT2"
                    + File.separator + "etc" + File.separator + "file_properties.xml"));
        } catch (JWNLException | FileNotFoundException ex) {
            Logger.getLogger(SemanticUtils.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static double tfIdf(List<String> doc, List<List<String>> docs, String term) {
        return tf(doc, term) * idf(docs, term);
    }

    private static double tf(List<String> doc, String term) {
        double result = 0;
        for (String word : doc) {
            if (term.equalsIgnoreCase(word)) {
                result++;
            }
        }
        return result / (double) doc.size();
    }

    private static double idf(List<List<String>> docs, String term) {
        double n = 0;
        for (List<String> doc : docs) {
            for (String word : doc) {
                if (term.equalsIgnoreCase(word)) {
                    n++;
                    break;
                }
            }
        }
        if (n <= 0) {
            n = 1;
        }
        return Math.log(docs.size() / n);
    }

    //Code From org.apache.commons.text.similarity. 
    /**
     * Calculates the cosine similarity for two given vectors.
     *
     * @param leftVector left vector
     * @param rightVector right vector
     * @return cosine similarity between the two vectors
     */
    public static Double cosineSimilarity(Map<String, Double> leftVector, Map<String, Double> rightVector) {
        if (leftVector == null || rightVector == null) {
            throw new IllegalArgumentException("Vectors must not be null");
        }

        Set<String> intersection = getIntersection(leftVector, rightVector);

//        System.err.println(leftVector);
//        System.err.println(rightVector);
        double dotProduct = dot(leftVector, rightVector, intersection);
        double d1 = 0.0d;
        for (Double value : leftVector.values()) {
            d1 += Math.pow(value, 2);
        }
        double d2 = 0.0d;
        for (Double value : rightVector.values()) {
            d2 += Math.pow(value, 2);
        }
        double cosineSimilarity;
        if (d1 <= 0.0 || d2 <= 0.0) {
            cosineSimilarity = 0.0;
        } else {
            double a = Math.sqrt(d1) * Math.sqrt(d2);
            cosineSimilarity = (dotProduct / a);
        }
        return cosineSimilarity;
    }

    /**
     * Returns a set with strings common to the two given maps.
     *
     * @param leftVector left vector map
     * @param rightVector right vector map
     * @return common strings
     */
    private static Set<String> getIntersection(Map<String, Double> leftVector,
            Map<String, Double> rightVector) {

//        ValueComparator bvc = new ValueComparator(leftVector);
//        TreeMap<String, Double> Lsorted_map = new TreeMap(bvc);
//        Lsorted_map.putAll(leftVector);
//
//        bvc = new ValueComparator(rightVector);
//        TreeMap<String, Double> Rsorted_map = new TreeMap(bvc);
//        Rsorted_map.putAll(rightVector);
//
//        SortedSet<String> Lkeys = new TreeSet<>(leftVector.keySet());
//        SortedSet<String> Rkeys = new TreeSet<>(rightVector.keySet());
        Set<String> intersection = new HashSet<>(leftVector.keySet());
        intersection.retainAll(rightVector.keySet());
        return intersection;
    }

    /**
     * Computes the dot product of two vectors. It ignores remaining elements.
     * It means that if a vector is longer than other, then a smaller part of it
     * will be used to compute the dot product.
     *
     * @param leftVector left vector
     * @param rightVector right vector
     * @param intersection common elements
     * @return the dot product
     */
    private static double dot(Map<String, Double> leftVector, Map<String, Double> rightVector,
            Set<String> intersection) {
        Double dotProduct = 0.0;
        for (String key : intersection) {
            dotProduct += leftVector.get(key) * rightVector.get(key);
        }
        return dotProduct;
    }

    public static Set<String> getDocument(Term term) throws IOException, JWNLException, MalformedURLException, ParseException {

        Set<String> doc = new HashSet<>();

        List<String> g = term.getGlosses();
        if (g != null) {
            for (String s : g) {
                if (s != null) {
                    doc.addAll(tokenize(s, true));
                }
            }
        }
        List<String> al = term.getAlternativeLables();
        if (al != null) {
            for (String s : al) {
                if (s != null) {
                    doc.addAll(tokenize(s, true));
                }
            }
        }
        List<String> cat = term.getCategories();
        if (cat != null) {
            for (String s : cat) {
                if (s != null) {
                    doc.addAll(tokenize(s, true));
                }
            }
        }
        return doc;
    }

    public static List<String> getNGrams(String text, int maxNGrams) throws IOException {
        List<String> words = new ArrayList<>();

        Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_42, CharArraySet.EMPTY_SET);
        TokenStream tokenStream = analyzer.tokenStream("content", new StringReader(text));
        StopFilter stopFilter = new StopFilter(Version.LUCENE_42, tokenStream, getStopWords());
        stopFilter.setEnablePositionIncrements(false);
//        SnowballFilter snowballFilter = new SnowballFilter(stopFilter, "English");

        try (ShingleFilter sf = new ShingleFilter(stopFilter, 2, maxNGrams)) {
            sf.setOutputUnigrams(false);
            CharTermAttribute charTermAttribute = sf.addAttribute(CharTermAttribute.class);
            sf.reset();
            while (sf.incrementToken()) {
                String word = charTermAttribute.toString();
                words.add(word.replaceAll(" ", "_"));
            }
            sf.end();
        }
        return words;
    }

    public static TokenStream tokenStemStream(String fieldName, Reader reader) throws IOException {
        TokenStream stream = new WhitespaceTokenizer(Version.LUCENE_42, reader);
        stream = new StopFilter(Version.LUCENE_42, stream, getStopWords());
        stream = new PorterStemFilter(stream);
        return stream;
    }

    public static TokenStream tokenStream(String fieldName, Reader reader) throws IOException {
        Analyzer analyzer = new ArmenianAnalyzer(Version.LUCENE_42, getStopWords());
        TokenStream stream = analyzer.tokenStream("field", reader);
        return stream;
    }

    public static List<String> tokenize(String text, boolean stem) throws IOException, JWNLException {
        text = text.replaceAll("’", "'");

        text = text.replaceAll("_", " ");
        text = text.replaceAll("[0-9]", "");
        text = text.replaceAll("[\\p{Punct}&&[^'-]]+", " ");

        text = text.replaceAll("(?:'(?:[tdsm]|[vr]e|ll))+\\b", "");
        text = text.toLowerCase();

        TokenStream tokenStream;
        if (stem) {
            tokenStream = tokenStemStream("field", new StringReader(text));
        } else {
            tokenStream = tokenStream("field", new StringReader(text));
        }

        ArrayList<String> words = new ArrayList<>();
        try {
            CharTermAttribute term = tokenStream.addAttribute(CharTermAttribute.class);
            tokenStream.reset();
            while (tokenStream.incrementToken()) {
                words.add(term.toString());
            }
            tokenStream.end();
        } finally {
            tokenStream.close();
        }
        return words;
    }

    private static CharArraySet getStopWords() throws IOException {
        if (stopwords.isEmpty()) {
            readStopWords();
        }
        return new CharArraySet(Version.LUCENE_42, stopwords, true);
    }

    private static void readStopWords() throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(stopwordsFile))) {
            for (String line; (line = br.readLine()) != null;) {
                stopwords.add(line);
            }
        }
    }

    public static Map<String, Double> getTermsInDoc(Map<String, Double> termDictionaray, File f) throws IOException, JWNLException {

        if (termDocCache == null) {
            termDocCache = new HashMap<>();
        }
        Map<String, Double> termsInDoc = termDocCache.get(f.getAbsolutePath());
        if (termsInDoc != null) {
            return termsInDoc;
        }
        termsInDoc = new HashMap<>();
        List<String> tokens = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            for (String text; (text = br.readLine()) != null;) {
                tokens.addAll(tokenize(text, true));
            }
        }

        for (String term : termDictionaray.keySet()) {
            for (String t : tokens) {
                if (term.equals(t)) {
                    Double tf = termsInDoc.get(t);
                    if (tf != null) {
                        tf++;
                    } else {
                        tf = 1.0;
                    }
                    termsInDoc.put(t, tf);
                }
            }
        }
        termDocCache.put(f.getAbsolutePath(), termsInDoc);
        return termsInDoc;
    }

    public static String lemmatize(String word) throws JWNLException, FileNotFoundException, MalformedURLException, IOException, ParseException, Exception {
        if (nonLemetize(word) || word.contains("_")) {
            return word;
        }

        wordNetdictionary = getWordNetDictionary();
        IndexWordSet set = wordNetdictionary.lookupAllIndexWords(word);
        for (IndexWord iw : set.getIndexWordArray()) {
            return iw.getLemma();
        }
//        word = lmmtizeFromOnlineWordNet(word, language);
//        word = lemmatizeFromBabelNet(word, language);

        return word;
    }

    public static boolean nonLemetize(String word) throws FileNotFoundException, IOException {
        if (nonLematizedWords.isEmpty() || nonLematizedWords == null) {
            loadNonLematizeWords();
        }
        return nonLematizedWords.contains(word);
    }

    private static Dictionary getWordNetDictionary() {
        if (wordNetdictionary == null) {
            wordNetdictionary = Dictionary.getInstance();
        }
        return wordNetdictionary;
    }

    private static void loadNonLematizeWords() throws FileNotFoundException, IOException {
        if (nonLematizedWordsFile.exists() && nonLematizedWordsFile.length() > 1) {
            try (BufferedReader br = new BufferedReader(new FileReader(nonLematizedWordsFile))) {
                String line;
                while ((line = br.readLine()) != null) {
                    nonLematizedWords.add(line);
                }
            }
        }
    }

    public static POS[] getPOS(String s) throws JWNLException {
        // Look up all IndexWords (an IndexWord can only be one POS)
        wordNetdictionary = getWordNetDictionary();
        IndexWordSet set = wordNetdictionary.lookupAllIndexWords(s);
        // Turn it into an array of IndexWords
        IndexWord[] words = set.getIndexWordArray();
        // Make the array of POS
        POS[] pos = new POS[words.length];
        for (int i = 0; i < words.length; i++) {
            pos[i] = words[i].getPOS();
        }
        return pos;
    }

    public static Set<Term> tf_idf_Disambiguation(Set<Term> possibleTerms, Set<String> nGrams, String lemma, double confidence, boolean matchTitle) throws IOException, JWNLException, ParseException {
        List<List<String>> allDocs = new ArrayList<>();
        Map<String, List<String>> docs = new HashMap<>();
        for (Term tv : possibleTerms) {
            Set<String> doc = SemanticUtils.getDocument(tv);
            allDocs.add(new ArrayList<>(doc));
            docs.put(tv.getUID(), new ArrayList<>(doc));
        }

        Set<String> contextDoc = new HashSet<>();
        for (String s : nGrams) {
            if (s.contains("_")) {
                String[] parts = s.split("_");
                for (String token : parts) {
                    if (token.length() >= 1 && !token.contains(lemma)) {
                        contextDoc.add(token);
                    }
                }
            } else if (s.length() >= 1 && !s.contains(lemma)) {
                contextDoc.add(s);
            }
        }
        docs.put("context", new ArrayList<>(contextDoc));

        Map<String, Map<String, Double>> featureVectors = new HashMap<>();
        for (String k : docs.keySet()) {
            List<String> doc = docs.get(k);
            Map<String, Double> featureVector = new TreeMap<>();
            for (String term : doc) {
                if (!featureVector.containsKey(term)) {
                    double tfidf = tfIdf(doc, allDocs, term);
//  double tf = SemanticUtils.tf(doc, allDocs, term);
                    featureVector.put(term, tfidf);
                }
            }
            featureVectors.put(k, featureVector);
        }

        Map<String, Double> contextVector = featureVectors.remove("context");

        Map<String, Double> scoreMap = new HashMap<>();
        for (String key : featureVectors.keySet()) {
            if (key.equals("20268")) {
                System.err.println("");
            }
            Double similarity = cosineSimilarity(contextVector, featureVectors.get(key));

            for (Term t : possibleTerms) {
                if (t.getUID().equals(key)) {
                    String stemTitle = stem(t.getLemma().toLowerCase());
                    String stemLema = stem(lemma);

                    if (!t.getLemma().toLowerCase().startsWith("(") && t.getLemma().toLowerCase().contains("(") && t.getLemma().toLowerCase().contains(")")) {
                        int index1 = t.getLemma().toLowerCase().indexOf("(") + 1;
                        int index2 = t.getLemma().toLowerCase().indexOf(")");
                        String sub = t.getLemma().toLowerCase().substring(index1, index2);

                        List<String> subTokens = tokenize(sub, true);
                        List<String> nTokens = new ArrayList<>();
                        for (String s : nGrams) {
                            if (s.contains("_")) {
                                String[] parts = s.split("_");
                                for (String token : parts) {
                                    nTokens.addAll(tokenize(token, true));
                                }
                            } else {
                                nTokens.addAll(tokenize(s, true));
                            }
                        }

                        Set<String> intersection = new HashSet<>(nTokens);
                        intersection.retainAll(subTokens);
                        if (intersection.isEmpty()) {
                            similarity -= 0.1;
                        }
                    }

//                    String shorter;
//                    String longer;
//                    if (stemTitle.length() > stemLema.length()) {
//                        longer = stemTitle;
//                        shorter = stemLema;
//                    } else {
//                        longer = stemLema;
//                        shorter = stemTitle;
//                    }
//                    if (longer.contains(shorter)) {
//                        similarity += 0.02;
//                    }
                    int dist = edu.stanford.nlp.util.StringUtils.editDistance(stemTitle, stemLema);
                    similarity = similarity - (dist * 0.001);
                    double logSim = Math.log(similarity - (dist * 0.001));
                    System.err.println(similarity + " " + logSim);
                    t.setConfidence(similarity);
                }
            }
            scoreMap.put(key, similarity);
        }
        if (scoreMap.isEmpty()) {
            return null;
        }

        ValueComparator bvc = new ValueComparator(scoreMap);
        TreeMap<String, Double> sorted_map = new TreeMap(bvc);
        sorted_map.putAll(scoreMap);

        Iterator<String> it = sorted_map.keySet().iterator();
        String winner = it.next();

        Double s1 = scoreMap.get(winner);
        if (s1 < confidence) {
            return null;
        }

        Set<Term> terms = new HashSet<>();
        for (Term t : possibleTerms) {
            if (t.getUID().equals(winner)) {
                terms.add(t);
            }
        }
        if (!terms.isEmpty()) {
            return terms;
        } else {
            Logger.getLogger(SemanticUtils.class.getName()).log(Level.INFO, "No winner");
            return null;
        }
    }

    public static Term disambiguate(String term, Set<Term> possibleTerms, String allTermsDictionary, double confidence, boolean matchTitle) throws IOException, JWNLException, ParseException {
        Set<String> ngarms = FileUtils.getNGramsFromTermDictionary(term, allTermsDictionary);
        possibleTerms = SemanticUtils.tf_idf_Disambiguation(possibleTerms, ngarms, term, confidence, matchTitle);
        Term dis = null;
        if (possibleTerms != null && possibleTerms.size() == 1) {
            dis = possibleTerms.iterator().next();
        }
//        if (dis != null) {
//            Logger.getLogger(SemanticUtils.class.getName()).log(Level.INFO, "Term: {0}. Confidence: {1}", new Object[]{dis, dis.getConfidence()});
//        } else {
//            Logger.getLogger(SemanticUtils.class.getName()).log(Level.INFO, "Couldn''''t figure out what ''{0}'' means", term);
//        }
        return dis;
    }

    public static String stem(String string) throws IOException, JWNLException {
        if (string.length() < 1) {
            return string;
        }
        String res = stemCache.get(string);
        if (res != null) {
            return res;
        }

        List<String> stems = SemanticUtils.tokenize(string, true);
        StringBuilder stem = new StringBuilder();
        for (String s : stems) {
            stem.append(s).append(" ");
        }
        if (stem.length() > 1) {
            stem.deleteCharAt(stem.length() - 1);
            stem.setLength(stem.length());
        }

        stemCache.put(string, stem.toString());
        return stem.toString();
    }

}
