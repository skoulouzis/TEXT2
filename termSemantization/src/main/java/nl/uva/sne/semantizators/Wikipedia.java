/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.sne.semantizators;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import net.didion.jwnl.JWNLException;
import nl.uva.sne.commons.Term;
import nl.uva.sne.commons.TermFactory;
import org.apache.commons.io.IOUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;

/**
 *
 * @author S. Koulouzis
 */
public class Wikipedia implements Semantizator {

    private Integer limit;

    @Override
    public List<Term> semnatizeTerms(String allTermsDictionary, String filterredDictionary) throws IOException, ParseException {
        List<Term> terms = new ArrayList<>();
        File dictionary = new File(filterredDictionary);
        int count = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(dictionary))) {
            for (String line; (line = br.readLine()) != null;) {
                String[] parts = line.split(",");
                String term = parts[0];
//                Integer score = Integer.valueOf(parts[1]);
                if (term.length() > 1) {
                    count++;
                    if (count > limit) {
                        break;
                    }
                    Term tt = getTerm(term, allTermsDictionary);
                    if (tt != null) {
                        terms.add(tt);
                    }
                }
            }
        } catch (Exception ex) {
            return terms;
        }
        return terms;
    }

    @Override
    public void configure(Properties properties) {
        limit = Integer.valueOf(properties.getProperty("num.of.terms", "5"));

    }

    private Term getTerm(String term, String allTermsDictionary) throws IOException, ParseException, JWNLException {
        List<Term> possibleTerms = getTermNodeByLemma(term);
        if (possibleTerms != null & possibleTerms.size() > 1) {
            return disambiguate(term, possibleTerms, allTermsDictionary);
        } else if (possibleTerms.size() == 1) {
            return possibleTerms.get(0);
        }
        return null;
    }

    private List<Term> getTermNodeByLemma(String lemma) throws MalformedURLException, IOException, ParseException {
        String query = lemma.replaceAll("_", " ");
        query = URLEncoder.encode(query, "UTF-8");
//sroffset=10
        URL url = new URL("https://en.wikipedia.org/w/api.php?action=query&format=json&list=search&srlimit=500&srsearch=" + query);

        String jsonString = IOUtils.toString(url);
        List<String> titlesList = getTitles(jsonString);
        StringBuilder titles = new StringBuilder();
        for (int i = 0; i < titlesList.size(); i++) {
            String t = titlesList.get(i);
            t = URLEncoder.encode(t, "UTF-8");
            titles.append(t).append("|");
            if (i % 20 == 0 && i > 0) {
                titles.deleteCharAt(titles.length() - 1);
                titles.setLength(titles.length() - 1);
                url = new URL("https://en.wikipedia.org/w/api.php?format=json&action=query&prop=extracts&exlimit=max&explaintext&exintro&titles=" + titles.toString());
                System.err.println(url);
                jsonString = IOUtils.toString(url);
                List<Term> ids = getCandidateTerms(jsonString);
                titles = new StringBuilder();
            }
        }

        return null;
    }

    private Term disambiguate(String term, List<Term> possibleTerms, String allTermsDictionary) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private List<String> getTitles(String jsonString) throws ParseException {
        List<String> titles = new ArrayList<>();
        JSONObject jsonObj = (JSONObject) JSONValue.parseWithException(jsonString);
        JSONObject query = (JSONObject) jsonObj.get("query");
        JSONArray search = (JSONArray) query.get("search");
        if (search != null) {
            for (Object o : search) {
                JSONObject res = (JSONObject) o;
                String title = (String) res.get("title");
                titles.add(title);
            }
        }
        return titles;
    }

    private List<Term> getCandidateTerms(String jsonString) throws ParseException {
        List<Term> ids = new ArrayList<>();
        JSONObject jsonObj = (JSONObject) JSONValue.parseWithException(jsonString);
        JSONObject query = (JSONObject) jsonObj.get("query");
        JSONObject pages = (JSONObject) query.get("pages");
        Set<String> keys = pages.keySet();
        for (String key : keys) {
            JSONObject page = (JSONObject) pages.get(key);
            TermFactory.create(page);
        }
//        if (pages != null) {
//            for (Object o : pages) {
//                JSONObject res = (JSONObject) o;
//                String pageid = (String) res.get("pageid");
//                System.err.println(pageid);
//                ids.add(pageid);
//            }
//        }

        return ids;
    }

}
