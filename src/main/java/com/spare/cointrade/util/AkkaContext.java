package com.spare.cointrade.util;

import akka.actor.ActorSystem;

/**
 * Created by dada on 2017/8/20.
 */
public class AkkaContext {

    private static final ActorSystem system = ActorSystem.create("rootSystem");

    public static ActorSystem getSystem() {
        return system;
    }

    public static final String AKKA_SYSTEM_PREFIX = "akka://rootSystem/user/";

    /**
     * 获取akka的actor权限定名
     * @param simpleName 比如tradeJudge
     * @return akka://rootSystem/user/tradeJudge
     */
    public static String getFullActorName(String simpleName) {
        return AKKA_SYSTEM_PREFIX + simpleName;
    }
}
