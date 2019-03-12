package com.tresys.jalop.utils.jnltest;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.SSLEngine;

import org.apache.log4j.Logger;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.json.simple.parser.ParseException;

import com.tresys.jalop.jnl.ConnectionHandler;
import com.tresys.jalop.jnl.DigestStatus;
import com.tresys.jalop.jnl.Mode;
import com.tresys.jalop.jnl.RecordInfo;
import com.tresys.jalop.jnl.RecordType;
import com.tresys.jalop.jnl.Role;
import com.tresys.jalop.jnl.Session;
import com.tresys.jalop.jnl.SubscribeRequest;
import com.tresys.jalop.jnl.Subscriber;
import com.tresys.jalop.jnl.SubscriberSession;
import com.tresys.jalop.jnl.impl.http.HttpUtils;
import com.tresys.jalop.jnl.impl.http.JNLAuditServlet;
import com.tresys.jalop.jnl.impl.http.JNLJournalServlet;
import com.tresys.jalop.jnl.impl.http.JNLLogServlet;
import com.tresys.jalop.jnl.impl.http.JNLTestInterface;
import com.tresys.jalop.utils.jnltest.Config.Config;
import com.tresys.jalop.utils.jnltest.Config.ConfigurationException;


    @SuppressWarnings("serial")
    public class JNLSubscriber implements Subscriber, JNLTestInterface
    {
        /** Logger for this class */
        private static final Logger logger = Logger.getLogger(JNLTest.class);

        /**
         * Configuration for this instance of JNLTest.
         */
        private final Config config;
        /**
         * From Sessions to associated {@link SubscriberImpl}
         */
        private final Map<Session, Map<RecordType, SubscriberImpl>> sessMap = new HashMap<Session, Map<RecordType,SubscriberImpl>>();

        /**
         * ConnectionHandler implementation
         */
        private ConnectionHandler connectionHandler;
        /**
         * Counter to keep track of the last used nonce for log records
         */
        private long latestLogNONCE;
        /**
         * Counter to keep track of the last used nonce for audit records
         */
        private long latestAuditNONCE;
        /**
         * Counter to keep track of the last used nonce for journal records
         */
        private long latestJournalNONCE;
        /**
         * Create a JNLTest object based on the specified configuration.
         *
         * @param config
         *            A {@link Config}
         */
        public JNLSubscriber(final Config config) {
            this.config = config;
        }
        public JNLSubscriber() {
            this.config = null;
        }
        /**
         * @return the latestLogNONCE
         */
        @Override
        public long getLatestLogNONCE() {
            return latestLogNONCE;
        }

        /**
         * @param latestLogNONCE the latestLogNONCE to set
         */
        @Override
        public void setLatestLogNONCE(final long latestLogNONCE) {
            this.latestLogNONCE = latestLogNONCE;
        }

        /**
         * @return the latestAuditNONCE
         */
        @Override
        public long getLatestAuditNONCE() {
            return latestAuditNONCE;
        }

        /**
         * @param latestAuditNONCE the latestAuditNONCE to set
         */
        @Override
        public void setLatestAuditNONCE(final long latestAuditNONCE) {
            this.latestAuditNONCE = latestAuditNONCE;
        }

        /**
         * @return the latestJournalNONCE
         */
        @Override
        public long getLatestJournalNONCE() {
            return latestJournalNONCE;
        }

        /**
         * @param latestJournalNONCE the latestJournalNONCE to set
         */
        @Override
        public void setLatestJournalNONCE(final long latestJournalNONCE) {
            this.latestJournalNONCE = latestJournalNONCE;
        }
        public static void main( String[] args ) throws Exception
        {
            if (args.length != 1) {
                System.err.println("Must specify exactly one argument that is "
                        + " the configuration file to use");
                System.exit(1);
            }
            Config config;
            try {
                config = Config.parse(args[0]);
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
            final JNLSubscriber jt = new JNLSubscriber(config);
            System.out.println("Started Connections");


            jt.start();

        }


        private void start() throws Exception{

            HttpUtils.requestCount.set(0);
            if (this.config.getRole() == Role.Subscriber)
            {
                // Create a basic jetty server object that will listen on port 8080.
                // Note that if you set this to port 0 then a randomly available port
                // will be assigned that you can either look in the logs for the port,
                // or programmatically obtain it for use in test cases.
                Server server = new Server();

                // The ServletHandler is a dead simple way to create a context handler
                // that is backed by an instance of a Servlet.
                // This handler then needs to be registered with the Server object.
                ServletContextHandler handler =  new ServletContextHandler(server, "/");
                server.setHandler(handler);

                String keystorePath = System.getProperty("user.home") + "/jalop/jjnl/keystore/keystore.jks";
                File keystoreFile = new File(keystorePath);
                if (!keystoreFile.exists())
                {
                    throw new FileNotFoundException(keystoreFile.getAbsolutePath());
                }

                // HTTP Configuration
                // HttpConfiguration is a collection of configuration information
                // appropriate for http and https. The default scheme for http is
                // <code>http</code> of course, as the default for secured http is
                // <code>https</code> but we show setting the scheme to show it can be
                // done. The port for secured communication is also set here.
                HttpConfiguration http_config = new HttpConfiguration();
                http_config.setSecureScheme("https");
                http_config.setSecurePort(8444);
             //   http_config.setOutputBufferSize(32768);

                // HTTP connector
                // The first server connector we create is the one for http, passing in
                // the http configuration we configured above so it can get things like
                // the output buffer size, etc. We also set the port (8080) and
                // configure an idle timeout.
                ServerConnector http = new ServerConnector(server,
                        new HttpConnectionFactory(http_config));
                http.setPort(8080);
                http.setIdleTimeout(30000);

                // SSL Context Factory for HTTPS
                // SSL requires a certificate so we configure a factory for ssl contents
                // with information pointing to what keystore the ssl connection needs
                // to know about. Much more configuration is available the ssl context,
                // including things like choosing the particular certificate out of a
                // keystore to be used.
                SslContextFactory sslContextFactory = new SslContextFactory();
                sslContextFactory.setKeyStorePath(keystoreFile.getAbsolutePath());
                sslContextFactory.setKeyStorePassword("changeit");
                sslContextFactory.setKeyManagerPassword("changeit");

                //Disable all protocols except for tls 1.2
                String[] excludedProtocols = new String[]{"SSLv2Hello", "SSLv3", "TLSv1", "TLSv1.1"};
                sslContextFactory.setExcludeProtocols(excludedProtocols);

                //This forces client certificate to be required
                sslContextFactory.setNeedClientAuth(true);

                // HTTPS Configuration
                // A new HttpConfiguration object is needed for the next connector and
                // you can pass the old one as an argument to effectively clone the
                // contents. On this HttpConfiguration object we add a
                // SecureRequestCustomizer which is how a new connector is able to
                // resolve the https connection before handing control over to the Jetty
                // Server.
                HttpConfiguration https_config = new HttpConfiguration(http_config);
                SecureRequestCustomizer src = new SecureRequestCustomizer();
                src.setStsMaxAge(2000);
                src.setStsIncludeSubDomains(true);
                https_config.addCustomizer(src);

                // HTTPS connector
                // We create a second ServerConnector, passing in the http configuration
                // we just made along with the previously created ssl context factory.
                // Next we set the port and a longer idle timeout.
                ServerConnector https = new ServerConnector(server,
                    new SslConnectionFactory(sslContextFactory,HttpVersion.HTTP_1_1.asString()),
                        new HttpConnectionFactory(https_config));
                https.setPort(8444);
                https.setIdleTimeout(500000);

                // Here you see the server having multiple connectors registered with
                // it, now requests can flow into the server from both http and https
                // urls to their respective ports and be processed accordingly by jetty.
                // A simple handler is also registered with the server so the example
                // has something to pass requests off to.

                // Set the connectors
                server.setConnectors(new Connector[] { http, https });

                // Passing in the class for the Servlet allows jetty to instantiate an
                // instance of that Servlet and mount it on a given context path.

                // IMPORTANT:
                // This is a raw Servlet, not a Servlet that has been configured
                // through a web.xml @WebServlet annotation, or anything similar.

                //Separate endpoints/servlets for audit,journal,log
                handler.addServlet(JNLLogServlet.class, "/log");
                handler.addServlet(JNLAuditServlet.class, "/audit");
                handler.addServlet(JNLJournalServlet.class, "/journal");

                // Start things up!
                server.start();

                //Display supported protocols and ciphers.
                SSLEngine engine = sslContextFactory.newSSLEngine();
                String enabledProtocols[] = engine.getEnabledProtocols();
                String enabledCiphers[] = engine.getEnabledCipherSuites();

                System.out.println("JALoP Jetty Server only supports the following protocols: " + Arrays.toString(enabledProtocols));
                System.out.println("JALoP Jetty Server only supports the following ciphers: " + Arrays.toString(enabledCiphers));

                // The use of server.join() the will make the current thread join and
                // wait until the server is done executing.
                // See
                // http://docs.oracle.com/javase/7/docs/api/java/lang/Thread.html#join()
                server.join();
            } else {
                System.out.println("Invalid configuration, only subscriber mode is supported");
            }
        }

        @Override
        public SubscribeRequest getSubscribeRequest(final SubscriberSession sess) {
            // TODO: All the code here to manage the maps should really be happening in the
            // connection handler callbacks, but the library isn't generating those events
            // quite yet.
            Map<RecordType, SubscriberImpl> map;
            synchronized (this.sessMap) {
                map = this.sessMap.get(sess);
                if (map == null) {
                    map = new HashMap<RecordType, SubscriberImpl>();
                    this.sessMap.put(sess, map);
                }
            }
            SubscriberImpl sub;
            synchronized(map) {
                sub = map.get(sess.getRecordType());
                if (sub == null) {
                    sub = new SubscriberImpl(sess.getRecordType(), this.config.getOutputPath(), sess.getAddress(), this);
                    map.put(sess.getRecordType(), sub);
                }
            }
            return sub.getSubscribeRequest(sess);
        }

        @Override
        public boolean notifySysMetadata(final SubscriberSession sess, final RecordInfo recordInfo, final InputStream sysMetaData) {
            return this.sessMap.get(sess).get(sess.getRecordType()).notifySysMetadata(sess, recordInfo, sysMetaData);
        }

        @Override
        public boolean notifyAppMetadata(final SubscriberSession sess, final RecordInfo recordInfo, final InputStream appMetaData) {
            return this.sessMap.get(sess).get(sess.getRecordType()).notifyAppMetadata(sess, recordInfo, appMetaData);
        }

        @Override
        public boolean notifyPayload(final SubscriberSession sess, final RecordInfo recordInfo, final InputStream payload) {
            return this.sessMap.get(sess).get(sess.getRecordType()).notifyPayload(sess, recordInfo, payload);
        }

        @Override
        public boolean notifyDigest(final SubscriberSession sess, final RecordInfo recordInfo, final byte[] digest) {
            return this.sessMap.get(sess).get(sess.getRecordType()).notifyDigest(sess, recordInfo, digest);
        }

        @Override
        public boolean notifyDigestResponse(final SubscriberSession sess, final String nonce, final DigestStatus status) {
            return this.sessMap.get(sess).get(sess.getRecordType()).notifyDigestResponse(sess, nonce, status);
        }

        @Override
        public Mode getMode() {
            return this.config.getMode();
        }

    }

