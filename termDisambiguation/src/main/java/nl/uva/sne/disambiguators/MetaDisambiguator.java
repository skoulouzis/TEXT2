/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.sne.disambiguators;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.didion.jwnl.JWNLException;
import nl.uva.sne.commons.Term;
import org.json.simple.parser.ParseException;

/**
 *
 * @author S. Koulouzis
 */
public class MetaDisambiguator extends DisambiguatorImpl {

    List<DisambiguatorImpl> disambiguators = new ArrayList<>();
    private boolean sequentially;

    @Override
    public void configure(Properties properties) {
        super.configure(properties);

        String semantizatiorClassNames = properties.getProperty("disambiguators", "nl.uva.sne.disambiguators.BabelNet,nl.uva.sne.disambiguators.Wikipedia");

        String[] classes = semantizatiorClassNames.split(",");

        for (String className : classes) {
            try {
                Class c = Class.forName(className);
                Object obj = c.newInstance();
                DisambiguatorImpl disambiguator = (DisambiguatorImpl) obj;
                disambiguator.configure(properties);

                disambiguators.add(disambiguator);
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException ex) {
                Logger.getLogger(MetaDisambiguator.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        sequentially = Boolean.valueOf(properties.getProperty("execute.sequentially", "false"));
    }

    @Override
    public Term getTerm(String term) throws IOException, ParseException, JWNLException {
//        long start = System.currentTimeMillis();
        try {
            if (sequentially) {
                return getTermSequentially(term);
            } else {
                return getTermConcurrently(term);
            }
//            long end = System.currentTimeMillis();
//            Logger.getLogger(MetaDisambiguator.class.getName()).log(Level.INFO, "-----------Elapsed: " + (end - start));
//            return d;
        } catch (IOException | ParseException | JWNLException | InterruptedException | ExecutionException ex) {
            if (ex instanceof IOException && ex.getMessage().contains("Your key is not valid or the daily requests limit has been reached")) {
                Logger.getLogger(MetaDisambiguator.class.getName()).log(Level.WARNING, null, ex);
            } else {
                Logger.getLogger(MetaDisambiguator.class.getName()).log(Level.SEVERE, null, ex);
            }

        }

        return null;
    }

    private Term getWinner(Set<Term> possibleTerms, double minimumSimilarity) {
        double highScore = minimumSimilarity;
        String id = null;
        for (Term t : possibleTerms) {
//            System.err.println(t + " " + t.getConfidence());
            if (t.getConfidence() >= highScore) {
                highScore = t.getConfidence();
                id = t.getUID();
            }
        }
        if (id != null) {
            for (Term t : possibleTerms) {
                if (id.equals(t.getUID())) {
                    return t;
                }
            }
        }
        return null;
    }

    private Term getTermSequentially(String term) throws IOException, ParseException, JWNLException {
        Set<Term> possibleTerms = new HashSet();
        for (Disambiguator s : disambiguators) {
            Term t = s.getTerm(term);
            if (t != null) {
                possibleTerms.add(t);
            }
        }
        Term dis = getWinner(possibleTerms, getMinimumSimilarity());
//        Term dis = SemanticUtils.disambiguate(term, possibleTerms, allTermsDictionaryPath, minimumSimilarity, true);
        if (dis == null) {
            Logger.getLogger(MetaDisambiguator.class.getName()).log(Level.INFO, "Couldn''''t figure out what ''{0}'' means", term);
        } else {
            Logger.getLogger(MetaDisambiguator.class.getName()).log(Level.INFO, "Term: {0}. Confidence: {1} URL: {3} Glosses: {2}  ", new Object[]{dis, dis.getConfidence(), dis.getGlosses(), dis.getUrl()});
        }
        return dis;
    }

    private Term getTermConcurrently(String term) throws InterruptedException, ExecutionException {
        Set<Term> possibleTerms = new HashSet();

        ExecutorService pool = Executors.newFixedThreadPool(disambiguators.size());
        Set<Future<Term>> set = new HashSet<>();
        for (int i = 0; i < disambiguators.size(); i++) {
            DisambiguatorImpl s = disambiguators.get(i);
            s.setTermToProcess(term);
            Future<Term> future = pool.submit(s);
            set.add(future);
        }

        for (Future<Term> future : set) {
            while (!future.isDone()) {
                Logger.getLogger(MetaDisambiguator.class.getName()).log(Level.INFO, "Task is not completed yet....");
                Thread.currentThread().sleep(50);
            }
            Term t = future.get();
            if (t != null) {
                possibleTerms.add(t);
            }
        }
        pool.shutdown();

        Term dis = getWinner(possibleTerms, getMinimumSimilarity());
//        Term dis = SemanticUtils.disambiguate(term, possibleTerms, allTermsDictionaryPath, minimumSimilarity, true);
        if (dis == null) {
            Logger.getLogger(MetaDisambiguator.class.getName()).log(Level.INFO, "Couldn''''t figure out what ''{0}'' means", term);
        } else {
            Logger.getLogger(MetaDisambiguator.class.getName()).log(Level.INFO, "Term: {0}. Confidence: {1} URL: {3} Glosses: {2}  ", new Object[]{dis, dis.getConfidence(), dis.getGlosses(), dis.getUrl()});
        }

        return dis;

    }
}
