package com.spare.cointrade.telnet.command;

import java.io.File;
import java.util.logging.Logger;

/**
 * cd command handler
 * 
 * @author a.khettar
 * 
 */
public class CDHandler implements CommandHandler {

    private final String command;
    private final Logger logger = Logger.getLogger(StatusHandler.class.getName());
    private final String workingDir;

    public CDHandler(final String command, String workingDir) {
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

        logger.info("Running the follwing command: " + command);

        if (command.split(" ").length == 1) {
            return "You must supply directory name: cd [directory name]";
        }
        String dirpath = command.split(" ", 2)[1];
        StringBuilder newpath = new StringBuilder();

        // navigating to an abs
        if (dirpath.startsWith("/") || dirpath.matches("(C|c):.*")) {
            return checkDirExist(dirpath);
        } else {
            newpath.append(workingDir);
            newpath.append(File.separator);
            newpath.append(dirpath);
        }
        return checkDirExist(newpath.toString());

    }

    /**
     * Checks if the working dir exist otherwise returns the approviate
     * message.
     * 
     * @param newpath
     * @return
     */
    private String checkDirExist(String newpath) {

        File file = new File(newpath.toString().endsWith(File.separator) ? newpath.toString() : newpath.toString()
                + File.separator);

        return file.isDirectory() ? newpath : "cd: " + newpath + ": No such file or directory";
    }

}
