/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.sne.disambiguators;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.didion.jwnl.JWNLException;
import nl.uva.sne.commons.SemanticUtils;
import nl.uva.sne.commons.Term;
import org.apache.commons.io.IOUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;

/**
 *
 * @author S. Koulouzis
 */
class Wikipedia extends DisambiguatorImpl {

    protected final String page = "https://en.wikipedia.org/w/api.php";

    public static final String[] EXCLUDED_CAT = new String[]{
        "articles needing",
        "articles lacking",
        "articles with",
        "articles containing",
        "articles to",
        "article disambiguation",
        "articles incorporating",
        "articles_including",
        "articles covered",
        "articles created",
        "articles that",
        "cs1 ",
        "disambiguation pages",
        "set index articles",
        "copied and pasted articles",
        "cleanup tagged articles",
        "pages needing",
        "pages lacking",
        "pages with",
        "pages using",
        "disambiguation pages",
        "use dmy dates",
        "use mdy dates",
        "all stub articles",
        "orphaned articles",
        "wikipedia introduction",
        "wikipedia articles",
        "wikipedia external",
        "wikipedia indefinitely",
        "wikipedia spam",
        "on wikidata",
        "vague or ambiguous time from",
        "stubs"
    };

    @Override
    public Term getTerm(String term) throws IOException, ParseException, JWNLException, MalformedURLException, UnsupportedEncodingException {
        Set<Term> possibleTerms = null;
        try {
            possibleTerms = getTermNodeByLemma(term);
        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(WikipediaOnline.class.getName()).log(Level.SEVERE, null, ex);
        }
//        if (possibleTerms != null & possibleTerms.size() > 1) {
        Term dis = SemanticUtils.disambiguate(term, possibleTerms, getAllTermsDictionaryPath(), getMinimumSimilarity(), true);
//        } else if (possibleTerms.size() == 1) {
//            return possibleTerms.iterator().next();
//        }
//        return null;
        if (dis == null) {
            Logger.getLogger(WikipediaOnline.class.getName()).log(Level.INFO, "Couldn''''t figure out what ''{0}'' means", term);
        } else {
            Logger.getLogger(BabelNet.class.getName()).log(Level.INFO, "Term: {0}. Confidence: {1} URL: {2}", new Object[]{dis, dis.getConfidence(), dis.getUrl()});
        }
        return dis;
    }

    protected Set<Term> getTermNodeByLemma(String lemma) throws MalformedURLException, IOException, ParseException, UnsupportedEncodingException, JWNLException, InterruptedException, ExecutionException {
        return null;
    }

    protected Set<String> getTitles(String lemma) throws ParseException, UnsupportedEncodingException, IOException, JWNLException {
        String URLquery = lemma.replaceAll("_", " ");
        URLquery = URLEncoder.encode(URLquery, "UTF-8");
        //sroffset=10
        URL url = new URL(page + "?action=query&format=json&redirects&list=search&srlimit=500&srsearch=" + URLquery);
        System.err.println(url);
        String jsonString = IOUtils.toString(url);

        Set<String> titles = new TreeSet<>();
        JSONObject jsonObj = (JSONObject) JSONValue.parseWithException(jsonString);
        JSONObject query = (JSONObject) jsonObj.get("query");
        JSONArray search = (JSONArray) query.get("search");
        if (search != null) {
            for (Object o : search) {
                JSONObject res = (JSONObject) o;
                String title = (String) res.get("title");
//                System.err.println(title);
                if (title != null && !title.toLowerCase().contains("(disambiguation)")) {
//                if (title != null) {
                    title = title.replaceAll("%(?![0-9a-fA-F]{2})", "%25");
                    title = title.replaceAll("\\+", "%2B");
                    title = java.net.URLDecoder.decode(title, "UTF-8");
                    title = title.replaceAll("_", " ").toLowerCase();
                    lemma = java.net.URLDecoder.decode(lemma, "UTF-8");
                    lemma = lemma.replaceAll("_", " ");

//                    String stemTitle = SemanticUtils.stem(title);
//                    String stemLema = SemanticUtils.stem(lemma);
//                    String shorter, longer;
//                    if (stemTitle.length() > stemLema.length()) {
//                        shorter = stemLema;
//                        longer = stemTitle;
//                    } else {
//                        shorter = stemTitle;
//                        longer = stemLema;
//                    }
//                    int dist = edu.stanford.nlp.util.StringUtils.editDistance(stemLema, stemTitle);
//                    if (longer.contains(shorter) && dist <= 10) {
                    titles.add(title);
//                    }
                }

            }
        }
        titles.add(lemma);
        return titles;
    }
}
