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
import java.util.Set;
import java.util.TreeMap;
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

    public static Set<String> getNGramsFromTermDictionary(String lemma, String keywordsDictionarayFile) throws IOException {
        if (nGramsMap == null) {
            nGramsMap = new HashMap<>();
        }
        Set<String> nGrams = nGramsMap.get(lemma);
        if (nGrams != null) {
            return nGrams;
        }

        Pattern pattern = Pattern.compile(lemma);
        nGrams = grep(new File(keywordsDictionarayFile), pattern, true);

        nGramsMap.put(lemma, nGrams);

        return nGrams;
    }

    public static Map<String, Double> csv2Map(String file) throws IOException {
        Map<String, Double> map = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.length() > 2) {
                    String[] parts = line.split(",");
                    if (parts.length > 1) {
                        map.put(parts[0], Double.valueOf(parts[1]));
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
                out.print(key + "," + value + "\n");
            }
        }
    }

}
