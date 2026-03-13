package com.itau.EL9.custom.liberty.components;
 
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
 
public class ItauTAICacheManager {
 
    static CacheManager cacheManager;
    static Ehcache cache;
    static Configuration config;
 
    public static String getPrivateKeyFromCache(String keyId) {
 
        ItauTAICacheKey key = new ItauTAICacheKey(keyId);
 
        Element elemento = cache.get(key);
 
        if (elemento == null)
            return null;
 
        return elemento.getObjectValue().toString();
    }
 
    public static void addPrivateKeyOnCache(String keyId, String value) {
        Element e = new Element(new ItauTAICacheKey(keyId), value);
   
        cache.put(e);
    }
   
    public static void removeKey(String keyId){
        cache.remove(new ItauTAICacheKey(keyId));
    }
 
    public static void initializeCache() {
        config = new Configuration();
 
        cacheManager = CacheManager.create(config);
        cache = new Cache(ItauTAICacheKey.class.getName(), Integer.MAX_VALUE, false, false, 30000, 3000);
        //TODO: Checar o tempo de cache pq a chave n�o muda tanto
        cacheManager.addCache(cache);
    }
 
    public static void disposeCache() {
        cache.dispose();
        cacheManager.shutdown();
    }
}
 
