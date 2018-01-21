package com.spare.cointrade.telnet.server;

import com.spare.cointrade.telnet.Util;
import com.spare.cointrade.telnet.command.CDHandler;
import com.spare.cointrade.telnet.command.CommandHandler;
import com.spare.cointrade.telnet.command.CommandHandlerFactory;
import com.spare.cointrade.telnet.command.ExitHandler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 
 * Client Worker handling request of a client.
 * 
 * @author a.khettar
 * 
 */
public class ClientWorker implements Runnable {

    private final Socket socket;
    private String workingDir;
    private final Logger logger = Logger.getLogger(ClientWorker.class.getName());

    /**
     * @param socket
     */
    public ClientWorker(final Socket socket, String homeDir) {
        this.socket = socket;
        this.workingDir = homeDir;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run() {

        try {
            final BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            final PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            // display welcome screen
            out.println(Util.buildWelcomeScreen());

            boolean cancel = false;
            CommandHandlerFactory fac = CommandHandlerFactory.getInstance();
            while (!cancel) {

                final String command = reader.readLine();
                if (command == null) {

                    continue;
                }

                //handle the command
                final CommandHandler handler = fac.getHandler(command, workingDir);
                String response = handler.handle();

                // setting the working directory
                if (handler instanceof CDHandler) {

                    workingDir = (response.contains("No such file or directory") || response
                            .contains("You must supply directory name")) ? workingDir : response;
                    logger.info("Working directory set to: " + workingDir);
                }
                out.println(response);

                // command issuing an exit.
                if (handler instanceof ExitHandler) {
                    cancel = true;
                }

            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Failed to close the socket", e);

            }
        }
    }

}
