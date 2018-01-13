package com.spare.cointrade.util;

import com.spare.cointrade.model.ListingDepth;

import java.util.Iterator;
import java.util.Map;

public class ListingDepthUtil {

    /**
     * 获取depthinfo第几级的信息
     * @param listingDepth
     * @param level 第几层级的消息，最大到5
     * @return
     */
    public static ListingDepth.DepthInfo getLevelDepthInfo(ListingDepth listingDepth, int level) {
        Iterator<Map.Entry<Double, ListingDepth.DepthInfo>> iterator = listingDepth.getDepthInfoMap().entrySet().iterator();
        if(iterator == null) {
            return null;
        }
        int index = 0;
        while (iterator.hasNext()) {
            if(index == level) {
                return iterator.next().getValue();
            }
            index++;
            if(index > 5) {
                return iterator.next().getValue();
            }
        }
        return null;
    }

}
