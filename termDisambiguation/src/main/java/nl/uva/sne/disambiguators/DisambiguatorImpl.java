/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.sne.disambiguators;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.didion.jwnl.JWNLException;
import nl.uva.sne.commons.Term;
import org.json.simple.parser.ParseException;

/**
 *
 * @author S. Koulouzis
 */
public class DisambiguatorImpl implements Disambiguator, Callable {

    private Integer limit;
    private Double minimumSimilarity;
    private Integer lineOffset;
    private String cachePath;
    private String allTermsDictionaryPath;
    private String termToProcess;

    @Override
    public List<Term> disambiguateTerms(String filterredDictionary) throws IOException, ParseException, FileNotFoundException {
//        Logger.getLogger(DisambiguatorImpl.class.getName()).log(Level.INFO, "filterredDictionary: " + filterredDictionary);
        List<Term> terms = new ArrayList<>();
        File dictionary = new File(filterredDictionary);
        int count = 0;
        int lineCount = 1;
        try (BufferedReader br = new BufferedReader(new FileReader(dictionary))) {
            for (String line; (line = br.readLine()) != null;) {
//                Logger.getLogger(DisambiguatorImpl.class.getName()).log(Level.INFO, "line: " + line);
                if (lineCount >= getLineOffset()) {
//                    Logger.getLogger(DisambiguatorImpl.class.getName()).log(Level.INFO, "Processing: " + line);
                    String[] parts = line.split(",");
                    String term = parts[0];
//                Integer score = Integer.valueOf(parts[1]);
                    if (term.length() >= 1) {
                        count++;
                        if (count > getLimit()) {
                            break;
                        }
                        Term tt = getTerm(term);
                        if (tt != null) {
                            terms.add(tt);
                        }
                    }
                }
                lineCount++;
            }
        } catch (Exception ex) {
            Logger.getLogger(DisambiguatorImpl.class.getName()).log(Level.WARNING, null, ex);
            return terms;
        } finally {
//            try {
//                saveCache();
//            } catch (InterruptedException ex) {
//                Logger.getLogger(DisambiguatorImpl.class.getName()).log(Level.SEVERE, null, ex);
//            }
        }
        return terms;
    }

    @Override
    public void configure(Properties properties) {
        StringBuilder props = new StringBuilder();
        String numOfTerms = System.getProperty("num.of.terms");

        if (numOfTerms == null) {
            limit = Integer.valueOf(properties.getProperty("num.of.terms", "5"));
        } else {
            limit = Integer.valueOf(numOfTerms);
        }
        props.append("num.of.terms: ").append(limit).append(" ");

        String offset = System.getProperty("offset.terms");

        if (offset == null) {
            lineOffset = Integer.valueOf(properties.getProperty("offset.terms", "1"));
        } else {
            lineOffset = Integer.valueOf(offset);
        }
        props.append("offset.terms: ").append(lineOffset).append(" ");

        minimumSimilarity = Double.valueOf(properties.getProperty("minimum.similarity", "0,3"));
        props.append("minimum.similarity: ").append(minimumSimilarity).append(" ");

        this.cachePath = (properties.getProperty("cache.path"));
        props.append("cache.path: ").append(cachePath).append(" ");
        allTermsDictionaryPath = properties.getProperty("all.terms.dictionary.path");
        props.append("all.terms.dictionary.path: ").append(allTermsDictionaryPath).append(" ");
        Logger.getLogger(DisambiguatorImpl.class.getName()).log(Level.INFO, "Properties: " + props.toString());
        props = null;
    }

    @Override
    public Term getTerm(String term) throws IOException, ParseException, JWNLException {
        return null;
    }

    /**
     * @return the cachePath
     */
    public String getCachePath() {
        return cachePath;
    }

    /**
     * @return the limit
     */
    public Integer getLimit() {
        return limit;
    }

    /**
     * @return the minimumSimilarity
     */
    public Double getMinimumSimilarity() {
        return minimumSimilarity;
    }

    /**
     * @return the lineOffset
     */
    public Integer getLineOffset() {
        return lineOffset;
    }

    /**
     * @return the allTermsDictionaryPath
     */
    public String getAllTermsDictionaryPath() {
        return allTermsDictionaryPath;
    }

    /**
     * @return the termToProcess
     */
    public String getTermToProcess() {
        return termToProcess;
    }

    /**
     * @param termToProcess the termToProcess to set
     */
    public void setTermToProcess(String termToProcess) {
        this.termToProcess = termToProcess;
    }

    @Override
    public Term call() throws Exception {
        return getTerm(getTermToProcess());
    }

    protected File waitForDB(File cacheDBFile) throws InterruptedException {
        File lock = new File(cacheDBFile.getAbsolutePath() + ".lock");
        int count = 0;
        long sleepTime = 5;
        int max = 4;
        int min = 2;
        while (lock.exists()) {
            Random random = new Random();
            sleepTime = sleepTime * random.nextInt(max - min + 1) + min;
            count++;
            if (count >= 40) {
                lock.delete();
                break;
            }
            Logger.getLogger(Wikipedia.class.getName()).log(Level.INFO, "DB {0} locked. Sleeping: {1} {2}", new Object[]{lock.getAbsolutePath(), sleepTime, count});
            Thread.currentThread().sleep(sleepTime);
        }
        return lock;
    }

}
