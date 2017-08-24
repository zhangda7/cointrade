package com.spare.cointrade.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by dada on 2017/8/25.
 */
public class ApplicationContextHolder {

    public static Map<String, Object> beanMap = new HashMap<>();

    public static <T> T getBean(Class<T> clazz) {
        return (T) beanMap.get(clazz.getSimpleName().toUpperCase());
    }

    public static void putBean(String key, Object obj) {
        beanMap.put(key, obj);
    }

}
