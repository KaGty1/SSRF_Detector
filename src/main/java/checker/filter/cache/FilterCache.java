package checker.filter.cache;

import cn.hutool.cache.Cache;
import cn.hutool.cache.impl.CacheObj;
import lombok.Getter;
import lombok.Setter;

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class FilterCache<K, V> {
    private final Cache<K, V> cache;
    
    @Getter
    @Setter
    private static String path = System.getProperty("user.home") + "/AppData/Roaming/BurpSuite/AutoSSRF/cache";
    private List<CacheObj<K, V>> cacheObjList = null;

    private void createFile(File file) throws IOException {
        File parentFile = file.getParentFile();
        if (!parentFile.exists()) {
            if (!parentFile.mkdirs()) {
                throw new IOException("创建文件失败: " + file.getAbsolutePath());
            }
        }

        boolean newFile = file.createNewFile();
        if (!newFile) {
            throw new IOException("创建文件失败: " + file.getAbsolutePath());
        }
    }

    private void buildCacheFromPath(Cache<K, V> cache, String path) throws IOException, ClassNotFoundException {
        File cacheFile = new File(path);
        if (!cacheFile.exists()) {
            return;
        }
        try (FileInputStream fis = new FileInputStream(cacheFile)) {
            try (ObjectInputStream ois = new ObjectInputStream(fis)) {
                Object cacheObject = ois.readObject();
                List<CacheObj<K, V>> cacheObjList = (List<CacheObj<K, V>>) cacheObject;
                for (CacheObj<K, V> cacheObj : cacheObjList) {
                    cache.put(cacheObj.getKey(), cacheObj.getValue());
                }
            }
        }
    }

    public FilterCache(Cache<K, V> cache) throws IOException, ClassNotFoundException {
        this.cache = cache;
        buildCacheFromPath(cache, path);
    }

    public FilterCache(Cache<K, V> cache, String path) throws IOException, ClassNotFoundException {
        this.cache = cache;
        FilterCache.path = path;
        buildCacheFromPath(cache, path);
    }

    
    public boolean contains(K key) {
        return cache.containsKey(key);
    }

    
    public void put(K key, V value) {
        cache.put(key, value);
    }
    
    public void clear() {
        cache.clear();
    }

    public void store() throws IOException {
        store(path);
    }

    public void store(String path) throws IOException {
        File cacheFile = new File(path);
        if (cacheFile.exists()) {
            delete(path);
        }
        createFile(cacheFile);
        try (FileOutputStream fos = new FileOutputStream(cacheFile)) {
            try (ObjectOutputStream oos = new ObjectOutputStream(fos)) {
                oos.writeObject(getCacheObjList());
            }
        }
    }

    public void delete() {
        delete(path);
    }

    public void delete(String path) throws RuntimeException {
        File file = new File(path);
        boolean delete = file.delete();
        if (!delete) {
            throw new RuntimeException("删除文件失败: " + file.getAbsolutePath());
        }
    }

    public List<CacheObj<K, V>> getCacheObjList() {
        if (cacheObjList != null && !cacheObjList.isEmpty()) {
            return cacheObjList;
        }
        List<CacheObj<K, V>> cacheObjs = new ArrayList<>();
        Iterator<CacheObj<K, V>> cacheObjIterator = cache.cacheObjIterator();
        while (cacheObjIterator.hasNext()) {
            CacheObj<K, V> cacheObj = cacheObjIterator.next();
            cacheObjs.add(cacheObj);
        }
        cacheObjList = cacheObjs;
        return cacheObjs;
    }

    public Integer getCacheCount() {
        return cache.size();
    }

}
