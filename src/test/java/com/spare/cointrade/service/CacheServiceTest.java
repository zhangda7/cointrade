package com.spare.cointrade.service;

import org.junit.Assert;
import org.junit.Test;

/**
 * Created by dada on 2017/9/16.
 */
public class CacheServiceTest {

    @Test
    public void testCache() {
        CacheService.INSTANCE.getServiceChargeCache().put("huobi", 1.0);
        Assert.assertEquals(CacheService.INSTANCE.getServiceChargeCache().get("huobi"), 1.0,0);
        CacheService.INSTANCE.getServiceChargeCache().put("huobi", 12.1);

        CacheService.INSTANCE.close();
    }

    @Test
    public void testDisk() {
        Assert.assertEquals(CacheService.INSTANCE.getServiceChargeCache().get("huobi"), 12.1,0);
    }

    @Test
    public void clearDisk() {
        CacheService.INSTANCE.getServiceChargeCache().remove("huobi");
    }

}
