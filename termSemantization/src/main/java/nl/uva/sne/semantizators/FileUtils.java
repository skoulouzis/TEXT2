/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.sne.semantizators;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.CharBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    private static Set<String> grep(File f, Pattern pattern, boolean removePattern) throws IOException {

        // Open the file and then get a channel from the stream
        FileInputStream fis = new FileInputStream(f);
        FileChannel fc = fis.getChannel();

        // Get the file's size and then map it into memory
        int sz = (int) fc.size();
        MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, sz);

        // Decode the file into a char buffer
        CharBuffer cb = DECODER.decode(bb);

        // Perform the search
        Set<String> ngrams = grep(f, cb, pattern, removePattern);

        // Close the channel and the stream
        fc.close();
        return ngrams;
    }

    // Use the linePattern to break the given CharBuffer into lines, applying
    // the input pattern to each line to see if we have a match
    //
    private static Set<String> grep(File f, CharBuffer cb, Pattern pattern, boolean removePattern) {
        Set<String> nGrams = new HashSet<>();
        Matcher lm = LINE_PATTERN.matcher(cb);	// Line matcher
        Matcher pm = null;			// Pattern matcher
//        int lines = 0;
        while (lm.find()) {
//            lines++;
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

    protected static Set<String> getNGramsFromTermDictionary(String lemma, String keywordsDictionarayFile) throws IOException {
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
}
