package com.spare.cointrade.telnet.command;

import java.util.logging.Logger;

/**
 * 
 * Server status handler
 * 
 * @author a.khettar
 * 
 */
public class StatusHandler implements CommandHandler {

    private final String command;
    private final Logger logger = Logger.getLogger(StatusHandler.class.getName());

    public StatusHandler(final String command) {
        this.command = command;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.akhettar.telnet.command.CommandHandler#handle()
     */
    @Override
    public String handle() {

        logger.info("running the following command: " + command);
        return "Server running";

    }

}
