package com.tresys.jalop.utils.jnltest;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.json.simple.parser.ParseException;

import com.tresys.jalop.jnl.impl.http.JNLSubscriber;
import com.tresys.jalop.utils.jnltest.Config.ConfigurationException;
import com.tresys.jalop.utils.jnltest.Config.HttpConfig;

@SuppressWarnings("serial")
public class JNLTestSubscriber
{
    /** Logger for this class */
    private static final Logger logger = Logger.getLogger(JNLTestSubscriber.class);

    /**
     * ConnectionHandler implementation
     */

    public static void main( String[] args ) throws Exception
    {
        if (args.length != 1) {
            System.err.println("Must specify exactly one argument that is "
                    + " the configuration file to use");
            System.exit(1);
        }
        HttpConfig config;
        try {
            config = HttpConfig.parse(args[0]);
        } catch (final IOException e) {
            System.err.println("Caught IO exception: " + e.getMessage());
            System.exit(1);
            throw new RuntimeException("Failed to call exit()");
        } catch (final ParseException e) {
            System.err.print(e.toString());
            System.exit(1);
            throw new RuntimeException("Failed to call exit()");
        } catch (final ConfigurationException e) {
            System.err.println("Exception processing the config file: "
                    + e.getMessage());
            System.exit(1);
            throw new RuntimeException("Failed to call exit()");
        }
        final JNLSubscriber jt = new JNLSubscriber(config.getHttpSubscriberConfig());
        System.out.println("Starting Connections");

        jt.start();
    }
}

