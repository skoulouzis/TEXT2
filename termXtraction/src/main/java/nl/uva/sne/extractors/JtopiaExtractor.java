/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.sne.extractors;

import com.sree.textbytes.jtopia.Configuration;
import com.sree.textbytes.jtopia.TermDocument;
import com.sree.textbytes.jtopia.TermsExtractor;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FilenameUtils;

/**
 *
 * @author S. Koulouzis
 */
public class JtopiaExtractor implements TermExtractor {

    @Override
    public void configure(Properties prop) {
        for (Object k : prop.keySet()) {
            Logger.getLogger(JtopiaExtractor.class.getName()).log(Level.INFO, k + " : " + prop.getProperty((String) k));
        }
        String taggerType = prop.getProperty("tagger.type", "stanford");

        String modelPath = prop.getProperty("model.path",
                System.getProperty("user.home") + File.separator + "workspace"
                + File.separator + "TEXT2" + File.separator + "etc" + File.separator + "model");
        if (modelPath.endsWith("/")) {
            modelPath = modelPath.substring(0, modelPath.length() - 1);
        }
        switch (taggerType) {
            case "stanford":
                Configuration.setModelFileLocation(modelPath + File.separator
                        + "stanford" + File.separator + "english-left3words-distsim.tagger");
                Configuration.setTaggerType("stanford");
                break;
            case "openNLP":
                Configuration.setModelFileLocation(modelPath + File.separator
                        + "openNLP" + File.separator + "en-pos-maxent.bin");
                Configuration.setTaggerType("openNLP");
                break;
            case "default":
                Configuration.setModelFileLocation(modelPath + File.separator
                        + "default" + File.separator + "english-lexicon.txt");
                Configuration.setTaggerType("default");
                break;
        }
        Integer singleStrength = Integer.valueOf(prop.getProperty("single.strength", "3"));
        Configuration.setSingleStrength(singleStrength);

        Integer noLimitStrength = Integer.valueOf(prop.getProperty("no.limit.strength", "2"));
        Configuration.setNoLimitStrength(noLimitStrength);
    }

    @Override
    public Map<String, Double> termXtraction(String inDir) throws IOException {
        File dir = new File(inDir);
        TermsExtractor termExtractor = new TermsExtractor();
        TermDocument topiaDoc = new TermDocument();
        HashMap<String, Double> keywordsDictionaray = new HashMap();
        int count = 0;
        if (dir.isDirectory()) {
            for (File f : dir.listFiles()) {
                if (FilenameUtils.getExtension(f.getName()).endsWith("txt")) {
                    count++;
                    Logger.getLogger(JtopiaExtractor.class.getName()).log(Level.INFO, "{0}: {1} of {2}", new Object[]{f.getName(), count, dir.list().length});
                    keywordsDictionaray.putAll(extractFromFile(f, termExtractor, topiaDoc));
                }
            }
        } else if (dir.isFile()) {
            keywordsDictionaray.putAll(extractFromFile(dir, termExtractor, topiaDoc));
        }
        return keywordsDictionaray;
    }

    private Map<String, Double> extractFromFile(File f, TermsExtractor termExtractor, TermDocument topiaDoc) throws IOException {
        HashMap<String, Double> keywordsDictionaray = null;
        if (FilenameUtils.getExtension(f.getName()).endsWith("txt")) {
            keywordsDictionaray = new HashMap();
            try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                StringBuilder stringBuffer = new StringBuilder();
                for (String text; (text = br.readLine()) != null;) {
                    text = text.replaceAll("-", "");
                    text = text.replaceAll("((mailto\\:|(news|(ht|f)tp(s?))\\://){1}\\S+)", "");
                    text = text.replaceAll("[^a-zA-Z\\s]", "");
//        text = text.replaceAll("(\\d+,\\d+)|\\d+", "");
                    text = text.replaceAll("  ", " ");
                    text = text.toLowerCase();
                    stringBuffer.append(text).append("\n");
                }

                topiaDoc = termExtractor.extractTerms(stringBuffer.toString());
                Set<String> terms = topiaDoc.getFinalFilteredTerms().keySet();
                for (String t : terms) {
                    String text = t.replaceAll(" ", "_");
                    Double tf;
                    if (keywordsDictionaray.containsKey(text.toLowerCase())) {
                        tf = keywordsDictionaray.get(text.toLowerCase());
                        tf++;
                    } else {
                        tf = 1.0;
                    }
                    keywordsDictionaray.put(text.toLowerCase(), tf);
                }
            }
        }
        return keywordsDictionaray;
    }
}
