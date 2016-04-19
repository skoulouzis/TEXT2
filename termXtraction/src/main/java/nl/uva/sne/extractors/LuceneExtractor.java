package nl.uva.sne.extractors;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import nl.uva.sne.commons.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.hy.ArmenianAnalyzer;
import org.apache.lucene.analysis.shingle.ShingleFilter;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.util.CharArraySet;
import nl.uva.sne.commons.ValueComparator;
import org.apache.lucene.util.Version;
import org.unix4j.Unix4j;
import org.unix4j.unix.grep.GrepOption;
import org.unix4j.unix.grep.GrepOptionSets;
import org.unix4j.unix.grep.GrepOptions;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author S. Koulouzis
 */
public class LuceneExtractor implements TermExtractor {

    static Set<String> stopwords = new HashSet();

    private String stopwordsFile;
    private int maxNGrams;
    private String scoreType;
    private boolean removeExclusiveTerms;
    private List<File> docs = new ArrayList<>();
    private File dir;

    @Override
    public void configure(Properties prop) {
        stopwordsFile = prop.getProperty("stop.words", System.getProperty("user.home")
                + File.separator + "workspace" + File.separator + "termXtraction"
                + File.separator + "etc" + File.separator + "sropwords");

        maxNGrams = Integer.valueOf(prop.getProperty("max.ngrams", "4"));

        scoreType = prop.getProperty("score.type", "tf").toLowerCase();

        removeExclusiveTerms = Boolean.valueOf(prop.getProperty("remove.exclusive.terms", "false"));
    }

    @Override
    public Map<String, Double> termXtraction(String inDir) throws IOException {
        dir = new File(inDir);
        Map<String, Double> termDictionaray = new HashMap();
        for (File f : dir.listFiles()) {
            if (FilenameUtils.getExtension(f.getName()).endsWith("txt")) {
                docs.add(f);
                try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                    for (String text; (text = br.readLine()) != null;) {
                        List<String> tokens = tokenize(text);
                        for (String t : tokens) {
                            Double tf;
                            if (termDictionaray.containsKey(t)) {
                                tf = termDictionaray.get(t);
                                tf++;
                            } else {
                                tf = 1.0;
                            }
                            termDictionaray.put(t, tf);
                        }
                    }
                }
            }
        }
        if (removeExclusiveTerms) {
            return removeExclusiveTerms(termDictionaray);
        }

        try {
            return Util.getScore(termDictionaray, scoreType, docs);
        } catch (InterruptedException ex) {
            Logger.getLogger(LuceneExtractor.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    private CharArraySet getStopWords() throws IOException {
        if (stopwords.isEmpty()) {
            readStopWords();
        }
        return new CharArraySet(Version.LUCENE_42, stopwords, true);
    }

    private void readStopWords() throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(stopwordsFile))) {
            for (String line; (line = br.readLine()) != null;) {
                stopwords.add(line);
            }
        }
    }

    private Map<String, Double> removeExclusiveTerms(Map<String, Double> keywordsDictionaray) {
        ValueComparator bvc = new ValueComparator(keywordsDictionaray);
        Map<String, Double> sorted_map = new TreeMap(bvc);
        sorted_map.putAll(keywordsDictionaray);

        //remove terms that only apear with others. e.g. if we only 
        //have 'machine learning' there is no point to keep 'machine' or 'learning'
        List<String> toRemove = new ArrayList<>();
        Integer singleTermRank = 0;
        for (String key1 : sorted_map.keySet()) {
            singleTermRank++;
            Integer multiTermRank = 0;
            for (String key2 : sorted_map.keySet()) {
                multiTermRank++;
                if (!key1.contains("_") && key2.contains("_") && key2.split("_")[0].equals(key1)) {
                    int diff = multiTermRank - singleTermRank;
                    if (diff <= 5 && diff > 0) {
                        if (!toRemove.contains(key1)) {
                            toRemove.add(key1);
                        }
                    }
                    break;
                }
            }
        }
        for (String k : toRemove) {
            keywordsDictionaray.remove(k);
        }
        bvc = new ValueComparator(keywordsDictionaray);
        sorted_map = new TreeMap(bvc);
        sorted_map.putAll(keywordsDictionaray);
        return sorted_map;
    }

    private List<String> tokenize(String text) throws IOException {

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

        StandardTokenizer source = new StandardTokenizer(Version.LUCENE_42, new StringReader(sb.toString()));
        TokenStream tokenStream = new StandardFilter(Version.LUCENE_42, source);
        try (ShingleFilter sf = new ShingleFilter(tokenStream, 2, maxNGrams)) {
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

}
