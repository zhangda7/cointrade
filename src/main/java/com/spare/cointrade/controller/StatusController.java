package com.spare.cointrade.controller;

import akka.actor.ActorRef;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by dada on 2017/7/16.
 */
@RestController
public class StatusController {

    @RequestMapping("/tradestatus")
    public String tradeStatus() {
        return "Hello world";
    }

}
