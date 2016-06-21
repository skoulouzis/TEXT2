/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.sne.disambiguators;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.didion.jwnl.JWNLException;
import nl.uva.sne.commons.Term;
import org.json.simple.parser.ParseException;
import org.mapdb.DB;

/**
 *
 * @author S. Koulouzis
 */
public class MetaDisambiguator implements Disambiguator {

    List<Disambiguator> disambiguators = new ArrayList<>();
    private Integer limit;
    private DB db;
    private double minimumSimilarity;
    private String cachePath;
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
//            terms = removeIrrelevant(terms);
        } catch (Exception ex) {
            Logger.getLogger(MetaDisambiguator.class.getName()).log(Level.WARNING, null, ex);
            return terms;
        } finally {
//            saveCache();
        }
        return terms;
    }

    @Override
    public void configure(Properties properties) {
        StringBuffer props = new StringBuffer();
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
        cachePath = properties.getProperty("cache.path");
        props.append("cache.path: ").append(cachePath).append(" ");
        minimumSimilarity = Double.valueOf(properties.getProperty("minimum.similarity", "0,3"));
        props.append("minimum.similarity: ").append(minimumSimilarity).append(" ");
        String semantizatiorClassNames = properties.getProperty("disambiguators", "nl.uva.sne.disambiguators.BabelNet,nl.uva.sne.disambiguators.Wikipedia");
        props.append("disambiguators: ").append(semantizatiorClassNames).append(" ");
        Logger.getLogger(MetaDisambiguator.class.getName()).log(Level.INFO, props.toString());

        String[] classes = semantizatiorClassNames.split(",");

        for (String className : classes) {
            try {
                Class c = Class.forName(className);
                Object obj = c.newInstance();
                Disambiguator disambiguator = (Disambiguator) obj;
                disambiguator.configure(properties);
                disambiguators.add(disambiguator);
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException ex) {
                Logger.getLogger(MetaDisambiguator.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

    }

    @Override
    public Term getTerm(String term, String allTermsDictionaryPath, double minimumSimilarity) throws IOException, ParseException, JWNLException {
        Set<Term> possibleTerms = new HashSet();
        for (Disambiguator s : disambiguators) {
            Term t = s.getTerm(term, allTermsDictionaryPath, minimumSimilarity);
            if (t != null) {
                possibleTerms.add(t);
            }
        }
        Term dis = getWinner(possibleTerms, minimumSimilarity);
//        Term dis = SemanticUtils.disambiguate(term, possibleTerms, allTermsDictionaryPath, minimumSimilarity, true);
        if (dis == null) {
            Logger.getLogger(MetaDisambiguator.class.getName()).log(Level.INFO, "Couldn''''t figure out what ''{0}'' means", term);
        } else {
            Logger.getLogger(MetaDisambiguator.class.getName()).log(Level.INFO, "Term: {0}. Confidence: {1} URL: {3} Glosses: {2}  ", new Object[]{dis, dis.getConfidence(), dis.getGlosses(), dis.getUrl()});
        }

//        if (possibleTerms.size() > 1) {
        return dis;
//        } else if (possibleTerms.size() == 1) {
//            return possibleTerms.iterator().next();
//        }
    }

//    private void saveCache() throws FileNotFoundException, IOException {
//        Logger.getLogger(MetaDisambiguator.class.getName()).log(Level.FINE, "Saving cache");
//
//        File cacheDBFile = new File(cachePath);
//        if (cacheDBFile.isFile() && cacheDBFile.exists()) {
//            db = DBMaker.newFileDB(cacheDBFile).make();
//            if (db != null) {
//                if (!db.isClosed()) {
//                    db.commit();
//                    db.close();
//                }
//            }
//        } else if (!cacheDBFile.exists()) {
//            File[] files = cacheDBFile.getParentFile().listFiles();
//            for (File f : files) {
//                db = DBMaker.newFileDB(f).make();
//                if (db != null) {
//                    if (!db.isClosed()) {
//                        db.commit();
//                        db.close();
//                    }
//                }
//            }
//        }
//
//    }
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

//    private List<Term> removeIrrelevant(List<Term> terms) throws IOException, JWNLException, MalformedURLException, ParseException {
//        return terms;
//    }
}
