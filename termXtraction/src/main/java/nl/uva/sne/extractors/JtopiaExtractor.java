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
import org.apache.commons.io.FilenameUtils;

/**
 *
 * @author S. Koulouzis
 */
public class JtopiaExtractor implements TermExtractor {

    @Override
    public void configure(Properties prop) {

        String taggerType = prop.getProperty("tagger.type", "stanford");
        switch (taggerType) {
            case "stanford":
                Configuration.setModelFileLocation(System.getProperty("user.home")
                        + File.separator + "workspace" + File.separator + "termXtraction"
                        + File.separator + "etc" + File.separator
                        + "model/stanford/english-left3words-distsim.tagger");
                Configuration.setTaggerType("stanford");
                break;
            case "openNLP":
                Configuration.setModelFileLocation(System.getProperty("user.home")
                        + File.separator + "workspace" + File.separator + "termXtraction"
                        + File.separator + "etc" + File.separator
                        + "model/openNLP/en-pos-maxent.bin");
                Configuration.setTaggerType("openNLP");
                break;
            case "default":
                Configuration.setModelFileLocation(System.getProperty("user.home")
                        + File.separator + "workspace" + File.separator + "termXtraction"
                        + File.separator + "etc" + File.separator
                        + "model/default/english-lexicon.txt");
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
               
        for (File f : dir.listFiles()) {
            if (FilenameUtils.getExtension(f.getName()).endsWith("txt")) {
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
        }
        return keywordsDictionaray;
    }
}
