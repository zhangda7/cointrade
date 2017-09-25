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
}
