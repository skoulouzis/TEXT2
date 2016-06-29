/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.sne.disambiguators;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import static nl.uva.sne.disambiguators.WikipediaOnline.EXCLUDED_CAT;
import org.apache.commons.io.IOUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;

/**
 *
 * @author S. Koulouzis
 */
public class WikiRequestor implements Callable {

    private final URL url;
    private final int type;
    private final String termUID;

    public WikiRequestor(URL url, String termUID, int type) {
        this.url = url;
        this.type = type;
        this.termUID = termUID;
    }

    @Override
    public Map<String, List<String>> call() throws Exception {
//        long start = System.currentTimeMillis();
        Map<String, List<String>> map = null;
        switch (type) {
            case 0:
                map = getWikiCategory();
                break;
            case 1:
                map = getWikidataNumProperty();
                break;
            case 2:
                return getWikidataLables();

        }
//        System.err.println(this.hashCode() + " Elapsed: " + (System.currentTimeMillis() - start));
        return map;
    }

    private boolean shouldAddCategory(String cat) {
        for (String s : EXCLUDED_CAT) {
            if (cat.toLowerCase().contains(s)) {
                return false;
            }
        }
        return true;
    }

    private Map<String, List<String>> getWikiCategory() throws IOException, ParseException {
        List<String> categoriesList = new ArrayList<>();
        String jsonString = IOUtils.toString(url);
        JSONObject jsonObj = (JSONObject) JSONValue.parseWithException(jsonString);
        JSONObject query = (JSONObject) jsonObj.get("query");
        JSONObject pages = (JSONObject) query.get("pages");
        Set<String> keys = pages.keySet();
        Map<String, List<String>> map = new HashMap<>();
        for (String key : keys) {
            JSONObject p = (JSONObject) pages.get(key);
            JSONArray categories = (JSONArray) p.get("categories");
            if (categories != null) {
                for (Object obj : categories) {
                    JSONObject jObj = (JSONObject) obj;
                    String cat = (String) jObj.get("title");
                    if (shouldAddCategory(cat)) {
//                    System.err.println(cat.substring("Category:".length()).toLowerCase());
                        categoriesList.add(cat.substring("Category:".length()).toLowerCase().replaceAll(" ", "_"));
                    }
                }
                map.put(termUID, categoriesList);
            }

        }
        return map;

    }

    private Map<String, List<String>> getWikidataNumProperty() throws IOException, ParseException {
        String jsonString = IOUtils.toString(url);
        Map<String, List<String>> map = new HashMap<>();
        JSONObject jsonObj = (JSONObject) JSONValue.parseWithException(jsonString);

        JSONObject claims = (JSONObject) jsonObj.get("claims");
//        "?action=wbgetclaims&format=json&props=&property=" + prop + "&entity="
        String prop = getPropertyName();

        JSONArray Jprop = (JSONArray) claims.get(prop);
        List<String> ids = new ArrayList<>();
        if (Jprop != null) {
            for (Object obj : Jprop) {
                JSONObject jobj = (JSONObject) obj;

                JSONObject mainsnak = (JSONObject) jobj.get("mainsnak");
//                System.err.println(mainsnak);
                JSONObject datavalue = (JSONObject) mainsnak.get("datavalue");
//                System.err.println(datavalue);
                if (datavalue != null) {
                    JSONObject value = (JSONObject) datavalue.get("value");
//            System.err.println(value);
                    java.lang.Long numericID = (java.lang.Long) value.get("numeric-id");
//                System.err.println(id + " -> Q" + numericID);
                    ids.add("Q" + numericID);
                }
            }
        }
        map.put(termUID, ids);
        return map;
    }

    private Map<String, List<String>> getWikidataLables() throws IOException, ParseException {
        String jsonString = IOUtils.toString(url);
        Map<String, List<String>> map = new HashMap<>();
        JSONObject jsonObj = (JSONObject) JSONValue.parseWithException(jsonString);

        JSONObject entities = (JSONObject) jsonObj.get("entities");
//        System.err.println(entities);
        String catID = this.url.toString().split("ids=")[1];
        JSONObject jID = (JSONObject) entities.get(catID);

        JSONObject labels = (JSONObject) jID.get("labels");
//        System.err.println(labels);
        JSONObject en = (JSONObject) labels.get("en");
//        System.err.println(en);
        if (en != null) {
            String value = (String) en.get("value");
            List<String> v = new ArrayList<>();
            v.add(value.substring("Category:".length()).toLowerCase().replaceAll(" ", "_"));
            map.put(termUID, v);
            return map;
        }
        return null;

    }

    private String getPropertyName() {
        String prop = url.toString().split("property=")[1];
        prop = prop.substring(0, prop.indexOf("&"));
        return prop;
    }

}
