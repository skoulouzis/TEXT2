/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.sne.extractors;

import com.sree.textbytes.jtopia.TermDocument;
import com.sree.textbytes.jtopia.TermsExtractor;
import edu.ehu.galan.rake.RakeAlgorithm;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author S. Koulouzis
 *
 */
public class RAKE implements TermExtractor {

    @Override
    public void configure(Properties prop) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Map<String, Double> termXtraction(String inDir) throws IOException {
        File dir = new File(inDir);

        HashMap<String, Double> keywordsDictionaray = new HashMap();
        int count = 0;
        if (dir.isDirectory()) {
            for (File f : dir.listFiles()) {
                count++;
                Logger.getLogger(JtopiaExtractor.class.getName()).log(Level.INFO, "{0}: {1} of {2}", new Object[]{f.getName(), count, dir.list().length});
                keywordsDictionaray.putAll(extractFromFile(f));
            }
        } else if (dir.isFile()) {
            keywordsDictionaray.putAll(extractFromFile(dir));
        }
        return keywordsDictionaray;
    }

    private Map<? extends String, ? extends Double> extractFromFile(File f) {
//            Document doc=new Document(full_path,name);
//    doc.setSentenceList(sentences);
//    doc.setTokenList(tokenized_sentences); 
edu.ehu.galan.rake.RakeAlgorithm ex = new edu.ehu.galan.rake.RakeAlgorithm();
        
        ex.loadStopWordsList("resources/lite/stopWordLists/RakeStopLists/SmartStopListEn");
        ex.loadPunctStopWord("resources/lite/stopWordLists/RakeStopLists/RakePunctDefaultStopList");
    
//    parser.readSource("testCorpus/textAstronomy");
    edu.ehu.galan.rake.model.Document doc = new edu.ehu.galan.rake.model.Document("full_path", "name");
    
//    ex.init(doc, pPropsDir);
//    ex.runAlgorithm();
//    doc.getTermList();
return null;
    }

}
