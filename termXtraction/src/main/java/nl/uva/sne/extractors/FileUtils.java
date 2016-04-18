/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.sne.extractors;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 *
 * @author S. Koulouzis
 */
public class FileUtils {

    protected static List<String> getGlosses(String jsonFile) throws IOException, org.json.simple.parser.ParseException {
        return getList(jsonFile, "glosses");
    }

    protected static String getUID(String path) throws IOException, org.json.simple.parser.ParseException {
        return getString(path, "uid");
    }

    protected static String getLemma(String jsonFile) throws IOException, ParseException {
        return getString(jsonFile, "lemma");
    }

    protected static String getString(String path, String field) throws IOException, org.json.simple.parser.ParseException {
        JSONParser parser = new JSONParser();
        Object obj = parser.parse(new FileReader(path));
        JSONObject jsonObject = (JSONObject) obj;
        return (String) jsonObject.get(field);
    }

    protected static Boolean getBoolean(String path, String field) throws IOException, org.json.simple.parser.ParseException {
        JSONParser parser = new JSONParser();
        Object obj = parser.parse(new FileReader(path));
        JSONObject jsonObject = (JSONObject) obj;
        return (Boolean) jsonObject.get(field);
    }

    static List<String> getAltLables(String jsonFile) throws IOException, ParseException {
        return getList(jsonFile, "alternativeLables");
    }

    private static List<String> getList(String jsonFile, String field) throws IOException, ParseException {
        JSONParser parser = new JSONParser();
        Object obj = parser.parse(new FileReader(jsonFile));
        JSONObject jsonObject = (JSONObject) obj;
        org.json.simple.JSONArray ja = (org.json.simple.JSONArray) jsonObject.get(field);
        if (ja == null) {
            return null;
        }
        List<String> list = new ArrayList<>();
        for (Object elem : ja) {
            String s = (String) elem;
            list.add(s);
        }
        return list;
    }

    static List<String> getBroaderUIDS(String jsonFile) throws IOException, ParseException {
        return getList(jsonFile, "broaderUIDS");
    }

    static List<String> getCategories(String jsonFile) throws IOException, ParseException {
        return getList(jsonFile, "categories");
    }

    static String getForeignKey(String jsonFile) throws IOException, ParseException {
        return getString(jsonFile, "foreignKey");
    }

    static boolean IsFromDictionary(String jsonFile) throws IOException, ParseException {
        return getBoolean(jsonFile, "isFromDictionary");
    }

}
