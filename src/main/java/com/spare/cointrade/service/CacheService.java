package com.spare.cointrade.service;

import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.PersistentCacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.units.EntryUnit;
import org.ehcache.config.units.MemoryUnit;

import java.io.File;

/**
 * Created by dada on 2017/9/16.
 */
public class CacheService {

    private CacheService() {
        init();
    }

    private static final String SERVICE_CHARGE_CACHE = "servicecharge";

    private PersistentCacheManager cacheManager;

    public static CacheService INSTANCE = new CacheService();

    private void init() {
        cacheManager = CacheManagerBuilder.newCacheManagerBuilder()
                .with(CacheManagerBuilder.persistence(new File("/home/admin/cache/" + SERVICE_CHARGE_CACHE)))
                .withCache(SERVICE_CHARGE_CACHE,
                        CacheConfigurationBuilder.newCacheConfigurationBuilder(String.class, Double.class,
                                ResourcePoolsBuilder.newResourcePoolsBuilder()
                                        .heap(10, EntryUnit.ENTRIES)
                                        .offheap(1, MemoryUnit.MB)
                                        .disk(20, MemoryUnit.MB, true)
                        ))
//                        CacheConfigurationBuilder.newCacheConfigurationBuilder(String.class, Double.class, ResourcePoolsBuilder.heap(10)))
                .build();
        cacheManager.init();
//        Cache<Long, String> myCache = cacheManager.createCache("myCache",
//                CacheConfigurationBuilder.newCacheConfigurationBuilder(Long.class, String.class, ResourcePoolsBuilder.heap(10)));
//
//        myCache.put(1L, "da one!");
//        String value = myCache.get(1L);
//
//        cacheManager.removeCache("preConfigured");
//
//        cacheManager.close();
    }

    public Cache<String, Double> getServiceChargeCache() {
        return cacheManager.getCache(SERVICE_CHARGE_CACHE, String.class, Double.class);
    }

    public void close() {
        cacheManager.close();
    }

}
