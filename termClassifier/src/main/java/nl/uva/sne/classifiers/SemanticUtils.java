/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.sne.classifiers;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.hy.ArmenianAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.util.Version;

/**
 *
 * @author S. Koulouzis
 */
public class SemanticUtils {

    static Set<String> stopwords = new HashSet();
    public static String stopwordsFile;

    protected static double tfIdf(List<String> doc, List<List<String>> docs, String term) {
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
    protected static Double cosineSimilarity(Map<String, Double> leftVector, Map<String, Double> rightVector) {
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
    
    

    public static Set<String> getDocument(Term term) throws IOException {

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

    private static List<String> tokenize(String text) throws IOException {

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

}
