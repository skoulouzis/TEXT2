/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.sne.commons;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;

/**
 *
 * @author S. Koulouzis
 */
public class CacheUtil {

    private static DB db;
    public static Map<String, String> synsetCache;
    public static Map<String, List<String>> wordIDCache;
    public static Map<String, String> disambiguateCache;
    public static Map<String, String> edgesCache;

    public static void removeElement(String key, String cachePath) throws IOException {
        if (db == null || db.isClosed()) {
            loadCache(cachePath);
        }
        Logger.getLogger(CacheUtil.class.getName()).log(Level.INFO, "Removing: {0}", key);
        synsetCache.remove(key);
        wordIDCache.remove(key);
        disambiguateCache.remove(key);
        edgesCache.remove(key);
    }

    public static void loadCache(String cachePath) throws FileNotFoundException, IOException {

        File cacheDBFile = new File(cachePath);
        db = DBMaker.newFileDB(cacheDBFile).make();
        synsetCache = db.getHashMap("synsetCacheDB");
        if (synsetCache == null) {
            synsetCache = db.createHashMap("synsetCacheDB").keySerializer(Serializer.STRING).valueSerializer(Serializer.STRING).make();
        }
        wordIDCache = db.get("wordIDCacheDB");
        if (wordIDCache == null) {
            wordIDCache = db.createHashMap("wordIDCacheDB").keySerializer(Serializer.STRING).valueSerializer(Serializer.BASIC).make();
        }

        disambiguateCache = db.get("disambiguateCacheDB");
        if (disambiguateCache == null) {
            disambiguateCache = db.createHashMap("").keySerializer(Serializer.STRING).valueSerializer(Serializer.STRING).make();
        }

        edgesCache = db.getHashMap("edgesCacheDB");
        if (edgesCache == null) {
            edgesCache = db.createHashMap("edgesCacheDB").keySerializer(Serializer.STRING).valueSerializer(Serializer.STRING).make();
        }

        db.commit();
    }

    private void deleteNonExisting() {

        Map<String, String> map = synsetCache;
        Collection<String> keys = map.keySet();
        for (String k : keys) {
            if (map.get(k).equals("NON-EXISTING")) {
                map.remove(k);
            }
        }
        map = disambiguateCache;
        keys = map.keySet();
        for (String k : keys) {
            if (map.get(k).equals("NON-EXISTING")) {
                map.remove(k);
            }
        }

        map = edgesCache;
        keys = map.keySet();
        for (String k : keys) {
            if (map.get(k).equals("NON-EXISTING")) {
                map.remove(k);
            }
        }
        keys = wordIDCache.keySet();
        for (String k : keys) {
            List<String> val = wordIDCache.get(k);
            if (val != null && val.size() == 1 && val.get(0).equals("NON-EXISTING")) {
                wordIDCache.remove(k);
            }
        }
    }
}
