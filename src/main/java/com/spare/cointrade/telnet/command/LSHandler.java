package com.spare.cointrade.telnet.command;

import java.io.File;
import java.util.logging.Logger;

/**
 * ls command handler
 * 
 * @author a.khettar
 * 
 */
public class LSHandler implements CommandHandler {

    private final String command;
    private final Logger logger = Logger.getLogger(StatusHandler.class.getName());
    private final String workingDir;

    public LSHandler(final String command, String workingDir) {
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
        String cr = System.getProperty("os.name").matches("(W|w)indows.*") ? "\r\n" : "\n";
        StringBuilder builder = new StringBuilder();
        builder.append(cr);
        for (String file : new File(workingDir).list()) {
            builder.append(new File(workingDir + File.separator + file).isDirectory() ? "d - " + file : "f - " + file);
            builder.append(cr);
        }
        return builder.toString();

    }
}
