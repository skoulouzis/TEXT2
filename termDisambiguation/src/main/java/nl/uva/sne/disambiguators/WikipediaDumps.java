/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.sne.disambiguators;

import edu.jhu.nlp.wikipedia.PageCallbackHandler;
import edu.jhu.nlp.wikipedia.WikiPage;
import edu.jhu.nlp.wikipedia.WikiXMLParser;
import edu.jhu.nlp.wikipedia.WikiXMLParserFactory;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.didion.jwnl.JWNLException;
import nl.uva.sne.commons.Term;
import org.apache.commons.io.FilenameUtils;
import org.json.simple.parser.ParseException;

/**
 *
 * @author S. Koulouzis
 */
public class WikipediaDumps extends Wikipedia {

    private String dumpLocation;
    private Map<String, Set<String>> titleCache = new HashMap<>();

    @Override
    public void configure(Properties properties) {
        super.configure(properties);
        dumpLocation = System.getProperty("wiki.dump.file");
        if (dumpLocation == null) {
            dumpLocation = properties.getProperty("wiki.dump.file",
                    System.getProperty("user.home") + File.separator + "Downloads"
                    + File.separator + "simplewiki-latest-pages-articles.xml.bz2");
        }
    }

    @Override
    protected Set<Term> getTermNodeByLemma(String lemma) throws MalformedURLException, IOException, ParseException, UnsupportedEncodingException, JWNLException, InterruptedException, ExecutionException {
        Set<String> titlesList = titleCache.get(lemma);
        if (titlesList == null) {
            titlesList = getTitles(lemma);
            titleCache.put(lemma, titlesList);
        }

        WikiXMLParser wxsp = WikiXMLParserFactory.getSAXParser(dumpLocation);

        try {
            
            wxsp.setPageCallback(new MyPageCallbackHandler(titlesList));

            wxsp.parse();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static class MyPageCallbackHandler implements PageCallbackHandler {

        private final Set<String> titlesList;

        private MyPageCallbackHandler(Set<String> titlesList) {
            this.titlesList = titlesList;
        }

        @Override
        public void process(WikiPage wp) {
            for (String title : titlesList) {
                System.err.println(title);
                if (title.equals(wp.getTitle())) {
                    System.out.println(wp.getTitle());
                    System.out.println(wp.getText());
                }
            }

        }
    }

}
