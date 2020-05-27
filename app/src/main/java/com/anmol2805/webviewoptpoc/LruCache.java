package com.anmol2805.webviewoptpoc;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;

public class LruCache {
    private Map<String, String> cache;
    private long capacity = 10000 * 1024;
    private long cacheSize;
    private Context context;

    public LruCache(LinkedHashMap<String, String> cache, long cacheSize, Context context) {
        this.cache = cache;
        this.cacheSize = cacheSize;
        this.context = context;
    }

    public boolean hasFile(String key) {
        return cache.containsKey(key);
    }

    public File get(String key) {
        String fileName = cache.get(key);
        cache.remove(key);
        cache.put(key, fileName);
        return new File(context.getFilesDir(), fileName);
    }

    public boolean put(String key, File value) {
        Log.d("incoming file", key);
        while (cacheSize + value.length() > capacity) {
            try {
                Map.Entry<String, String> firstKey = cache.entrySet().iterator().next();
                File file = new File(context.getFilesDir(),firstKey.getValue());
                if (file.exists()) {
                    cacheSize -= file.length();
                    boolean fileDeleted = file.delete();
                }
                Log.d("deleting file", firstKey.getKey());
                cache.remove(firstKey.getKey());
            }catch (NoSuchElementException e){
                e.printStackTrace();
                break;
            }
        }

        cache.put(key, value.getName());
        cacheSize += value.length();

        return true;
    }

    public Map<String, String> getCache() {
        return cache;
    }

    public long getCacheSize() {
        return cacheSize;
    }


}
