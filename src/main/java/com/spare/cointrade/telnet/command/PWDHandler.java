package com.spare.cointrade.telnet.command;

import java.util.logging.Logger;

/**
 * The pwd handler. Just return the current working directory.
 * 
 * @author a.khettar
 * 
 */
public class PWDHandler implements CommandHandler {

    private final String command;
    private final Logger logger = Logger.getLogger(PWDHandler.class.getName());
    private final String workingDir;

    public PWDHandler(final String command, String workingDir) {
        this.command = command;
        this.workingDir = workingDir;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.akhettar.telnet.command.CommandHandler#handle()
     */
    @Override
    public String handle() {

        logger.info("running the follwoing commnad:" + command);
        return workingDir;

    }

}
