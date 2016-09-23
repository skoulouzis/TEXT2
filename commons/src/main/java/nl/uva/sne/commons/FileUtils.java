/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.sne.commons;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.CharBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 *
 * @author S. Koulouzis
 */
public class FileUtils {

    private static final Charset CHARSET = Charset.forName("ISO-8859-15");
    private static final CharsetDecoder DECODER = CHARSET.newDecoder();
    private static final Pattern LINE_PATTERN
            = Pattern.compile(".*\r?\n");
    private static Map<String, Set<String>> nGramsMap;
    private static JSONParser parser;

    protected static List<String> getGlosses(String jsonString) throws IOException, org.json.simple.parser.ParseException {
        return getList(jsonString, "glosses");
    }

    protected static List<String> getGlosses(FileReader fr) throws IOException, org.json.simple.parser.ParseException {
        return getList(fr, "glosses");
    }

    protected static String getUID(String jsonString) throws IOException, org.json.simple.parser.ParseException {
        return getString(jsonString, "uid");
    }

    static String getUID(FileReader fr) throws IOException, ParseException {
        return getString(fr, "uid");
    }

    protected static String getLemma(String jsonString) throws IOException, ParseException {
        return getString(jsonString, "lemma");
    }

    static String getLemma(FileReader fr) throws IOException, ParseException {
        return getString(fr, "lemma");
    }

    private static String getString(FileReader fr, String field) throws IOException, ParseException {
        if (parser == null) {
            parser = new JSONParser();
        }
        Object obj = parser.parse(fr);
        JSONObject jsonObject = (JSONObject) obj;
        return (String) jsonObject.get(field);
    }

    protected static String getString(String jsonString, String field) throws IOException, org.json.simple.parser.ParseException {
        if (parser == null) {
            parser = new JSONParser();
        }
        Object obj = parser.parse(jsonString);
        JSONObject jsonObject = (JSONObject) obj;
        return (String) jsonObject.get(field);
    }

    protected static Boolean getBoolean(String jsonStr, String field) throws IOException, org.json.simple.parser.ParseException {
        if (parser == null) {
            parser = new JSONParser();
        }
        Object obj = parser.parse(jsonStr);
        JSONObject jsonObject = (JSONObject) obj;
        return (Boolean) jsonObject.get(field);
    }

    private static double getDouble(String jsonStr, String field) throws ParseException {
        if (parser == null) {
            parser = new JSONParser();
        }
        Object obj = parser.parse(jsonStr);
        JSONObject jsonObject = (JSONObject) obj;
        return (Double) jsonObject.get(field);
    }

    private static boolean getBoolean(FileReader fr, String field) throws IOException, ParseException {
        if (parser == null) {
            parser = new JSONParser();
        }
        Object obj = parser.parse(fr);
        JSONObject jsonObject = (JSONObject) obj;
        return (Boolean) jsonObject.get(field);
    }

    static List<String> getAltLables(String jsonString) throws IOException, ParseException {
        return getList(jsonString, "alternativeLables");
    }

    static List<String> getAltLables(FileReader fr) throws IOException, ParseException {
        return getList(fr, "alternativeLables");
    }

    private static List<String> getList(String jsonString, String field) throws IOException, ParseException {
        if (parser == null) {
            parser = new JSONParser();
        }
        Object obj;
        obj = parser.parse(jsonString);
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

    private static List<String> getList(FileReader fr, String field) throws IOException, ParseException {
        if (parser == null) {
            parser = new JSONParser();
        }
        Object obj;
        obj = parser.parse(fr);
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

    static List<String> getBroaderUIDS(String jsonString) throws IOException, ParseException {
        return getList(jsonString, "broaderUIDS");
    }

    static List<String> getBroaderUIDS(FileReader fr) throws IOException, ParseException {
        return getList(fr, "broaderUIDS");
    }

    static List<String> getCategories(String jsonString) throws IOException, ParseException {
        return getList(jsonString, "categories");
    }

    static List<String> getCategories(FileReader fr) throws IOException, ParseException {
        return getList(fr, "categories");
    }

    static String getForeignKey(String jsonString) throws IOException, ParseException {
        return getString(jsonString, "foreignKey");
    }

    static String getForeignKey(FileReader fr) throws IOException, ParseException {
        return getString(fr, "foreignKey");
    }

    static String getURL(String jsonStr) throws IOException, ParseException {
        return getString(jsonStr, "url");
    }

    static boolean IsFromDictionary(String jsonString) throws IOException, ParseException {
        return getBoolean(jsonString, "isFromDictionary");
    }

    static boolean IsFromDictionary(FileReader fr) throws IOException, ParseException {
        return getBoolean(fr, "isFromDictionary");
    }

    static String getOriginalTerm(String jsonStr) throws IOException, ParseException {
        return getString(jsonStr, "originalTerm");
    }

    static double getConfidence(String jsonStr) throws ParseException {
        return getDouble(jsonStr, "confidence");
    }

    public static Set<String> grep(File f, Pattern pattern, boolean removePattern) throws IOException {

        // Open the file and then get a channel from the stream
        FileInputStream fis = new FileInputStream(f);
        FileChannel fc = fis.getChannel();

        // Get the file's size and then map it into memory
        int sz = (int) fc.size();
        MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, sz);

        // Decode the file into a char buffer
        CharBuffer cb = DECODER.decode(bb);

        // Perform the search
        Set<String> ngrams = grep(cb, pattern, removePattern);

        // Close the channel and the stream
        fc.close();
        return ngrams;
    }

    // Use the linePattern to break the given CharBuffer into lines, applying
    // the input pattern to each line to see if we have a match
    //
    private static Set<String> grep(CharBuffer cb, Pattern pattern, boolean removePattern) {
        Set<String> nGrams = new HashSet<>();
        Matcher lm = LINE_PATTERN.matcher(cb);	// Line matcher
        Matcher pm = null;			// Pattern matcher
        while (lm.find()) {
            CharSequence cs = lm.group(); 	// The current line
            if (pm == null) {
                pm = pattern.matcher(cs);
            } else {
                pm.reset(cs);
            }
            if (pm.find()) {
                String match = String.valueOf(cs);
                match = match.split(",")[0];
                if (removePattern) {
                    match = match.replaceAll(pattern.toString(), "");
                }
                if (match.length() > 1) {
                    nGrams.add(match);
                }

            }
            if (lm.end() == cb.limit()) {
                break;
            }
        }
        return nGrams;
    }

//    public static Set<String> getNGramsFromTermDictionary(String lemma, String keywordsDictionarayFile) throws IOException {
//        if (nGramsMap == null) {
//            nGramsMap = new HashMap<>();
//        }
//        Set<String> nGrams = nGramsMap.get(lemma);
//        if (nGrams != null) {
//            Logger.getLogger(FileUtils.class.getName()).log(Level.INFO, "Loaded {0} N-grams for: {1}", new Object[]{nGramsMap.size(), lemma});
//            return nGrams;
//        }
//
//        Pattern pattern = Pattern.compile(lemma);
//        nGrams = grep(new File(keywordsDictionarayFile), pattern, true);
//
//        nGramsMap.put(lemma, nGrams);
//        Logger.getLogger(FileUtils.class.getName()).log(Level.INFO, "Loaded {0} N-grams for: {1}", new Object[]{nGramsMap.size(), lemma});
//        return nGrams;
//    }
    public static Set<String> getNGramsFromTermDictionary(String lemma, String keywordsDictionarayFile) throws FileNotFoundException, IOException {
        if (nGramsMap == null) {
            nGramsMap = new HashMap<>();
        }
        Set<String> nGrams = nGramsMap.get(lemma);
        if (nGrams != null) {
            return nGrams;
        }
        nGrams = new HashSet<>();
        try (BufferedReader br = new BufferedReader(new FileReader(keywordsDictionarayFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                String keyword = line.split(",")[0];
                if (keyword.contains("_") && keyword.contains(lemma)) {
                    String[] parts = keyword.split(lemma);
                    for (String p : parts) {
                        if (p.length() >= 1) {
                            String[] defs = p.split("_");
                            for (String d : defs) {
                                if (d.length() >= 1) {
                                    nGrams.add(d);
                                }
                            }
                        }
                    }
                }
            }
        }
        nGramsMap.put(lemma, nGrams);
        Logger.getLogger(FileUtils.class.getName()).log(Level.INFO, "Loaded {0} N-grams for: {1}", new Object[]{nGrams.size(), lemma});
        return nGrams;
    }

    public static Map<String, Double> csv2Map(String file) throws IOException {
        Map<String, Double> map = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.length() > 1) {
                    if (line.contains(",")) {
                        String[] parts = line.split(",");
                        if (parts.length > 1) {
                            map.put(parts[0], Double.valueOf(parts[1]));
                        }
                    } else {
                        map.put(line, 0.0);
                    }

                }
            }
        }
        return map;
    }

    public static Map<String, Double> mergeDictionaries(Map<String, Double> correctTF, Map<String, Double> otherMap) {
        Map<String, Double> map = new HashMap<>();
        for (String t : otherMap.keySet()) {
            Double tf = correctTF.get(t);
            if (tf == null) {
                tf = otherMap.get(t);
            }
            map.put(t, tf);
        }
        return map;
    }

    public static void writeDictionary2File(Map<String, Double> keywordsDictionaray, String outkeywordsDictionarayFile) throws FileNotFoundException {
        ValueComparator bvc = new ValueComparator(keywordsDictionaray);
        Map<String, Double> sorted_map = new TreeMap(bvc);
        sorted_map.putAll(keywordsDictionaray);

        try (PrintWriter out = new PrintWriter(outkeywordsDictionarayFile)) {
            for (String key : sorted_map.keySet()) {
                Double value = keywordsDictionaray.get(key);
                key = key.toLowerCase().trim().replaceAll(" ", "_");
                if (key.endsWith("_")) {
                    key = key.substring(0, key.lastIndexOf("_"));
                }

                out.print(key + "," + value + "\n");
            }
        }
    }

    public static Properties getProperties(String propertiesPath) throws IOException {
        Logger.getLogger(FileUtils.class.getName()).log(Level.INFO, "Reading properties from: {0}", propertiesPath);
        InputStream in = null;
        try {
            if (new File(propertiesPath).exists() && new File(propertiesPath).isFile()) {
                in = new FileInputStream(propertiesPath);
            } else {
                ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
                in = classLoader.getResourceAsStream(propertiesPath);
            }
            Properties properties = new Properties();
            properties.load(in);
            return properties;
        } catch (IOException ex) {
            Logger.getLogger(FileUtils.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            if (in != null) {
                in.close();
            }
        }
        return null;
    }

    public static String readFile(FileReader fr) throws IOException {
        try (BufferedReader reader = new BufferedReader(fr)) {
            String line;
            StringBuilder stringBuilder = new StringBuilder();
            String ls = System.getProperty("line.separator");

            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
                stringBuilder.append(ls);
            }
            return stringBuilder.toString();
        }
    }

}
