package com.spare.cointrade.telnet.server;

/**
 * Created with IntelliJ IDEA.
 * User: a.khettar
 * Date: 20/02/2013
 * Time: 18:30
 * To change this template use File | Settings | File Templates.
 */
public class ServerLauncher {

    /**
     * Main method.
     * 
     * @param args
     */
    public static void main(String[] args) {

        // launch the server
        new TelnetServer(args.length == 0 ? null : args[0]).run();
    }
}
