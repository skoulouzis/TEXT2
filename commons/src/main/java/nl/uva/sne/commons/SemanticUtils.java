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
import java.io.StringReader;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
        return result / doc.size();
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

    public static Set<String> getDocument(Term term) throws IOException, JWNLException {

        Set<String> doc = new HashSet<>();

        List<String> g = term.getGlosses();
        if (g != null) {
            for (String s : g) {
                doc.addAll(tokenize(s));
            }
        }
        List<String> al = term.getAlternativeLables();
        if (al != null) {
            for (String s : al) {
                doc.addAll(tokenize(s));
            }
        }
        List<String> cat = term.getCategories();
        if (cat != null) {
            for (String s : cat) {
                doc.addAll(tokenize(s));
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

    public static List<String> tokenize(String text) throws IOException, JWNLException {
        text = text.replaceAll("[^a-zA-Z\\s]", "");
        text = text.replaceAll("-+", "-0");
        text = text.replaceAll("’", "'");
        text = text.replaceAll("[\\p{Punct}&&[^'-]]+", " ");
        text = text.replaceAll("(?:'(?:[tdsm]|[vr]e|ll))+\\b", "");
        text = text.toLowerCase();

        ArrayList<String> words = new ArrayList<>();
        Analyzer analyzer = new ArmenianAnalyzer(Version.LUCENE_42, getStopWords());
        StringBuilder sb = new StringBuilder();
        try (TokenStream tokenStream = analyzer.tokenStream("field", new StringReader(text))) {
            CharTermAttribute term = tokenStream.addAttribute(CharTermAttribute.class);
            tokenStream.reset();
            while (tokenStream.incrementToken()) {
                words.add(term.toString());
                sb.append(term.toString()).append(" ");
            }
            tokenStream.end();
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
                tokens.addAll(tokenize(text));
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

}
