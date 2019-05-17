package com.tresys.jalop.jnl.impl.http;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

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

import com.tresys.jalop.jnl.DigestStatus;
import com.tresys.jalop.jnl.Mode;
import com.tresys.jalop.jnl.RecordInfo;
import com.tresys.jalop.jnl.RecordType;
import com.tresys.jalop.jnl.Role;
import com.tresys.jalop.jnl.Session;
import com.tresys.jalop.jnl.SubscribeRequest;
import com.tresys.jalop.jnl.Subscriber;
import com.tresys.jalop.jnl.SubscriberSession;
import com.tresys.jalop.jnl.exceptions.JNLException;
import com.tresys.jalop.jnl.impl.subscriber.SubscriberHttpSessionImpl;
import com.tresys.jalop.jnl.impl.subscriber.SubscriberImpl;

@SuppressWarnings("serial")
public class JNLSubscriber implements Subscriber, JNLTestInterface
{
    /** Logger for this class */
    private static final Logger logger = Logger.getLogger(JNLSubscriber.class);

    /**
     * From Sessions to associated {@link SubscriberImpl}
     */
    private final Map<Session, SubscriberImpl> sessMap = new HashMap<Session, SubscriberImpl>();

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
     * Configuration for this instance of JNLTest.
     */
    private final HttpSubscriberConfig config;

    /**
     * Create a JNLTest object based on the specified configuration.
     *
     * @param config
     *            A {@link Config}
     */
    public JNLSubscriber(final HttpSubscriberConfig config) {

        if (config == null) {
            throw new IllegalArgumentException("'HttpSubscriberConfig' is required.");
        }
        this.config = config;
    }

    public void start() throws Exception{

        HttpUtils.requestCount.set(0);

        if (this.config.getRole() == Role.Subscriber)
        {
            //Sets the subscriber reference
            HttpUtils.setSubscriber(this);

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
            SslContextFactory sslContextFactory = new SslContextFactory();

            if (config.getTlsConfiguration().equals(HttpUtils.MSG_ON))
            {
                String keystorePath = config.getKeystorePath();
                File keystoreFile = new File(keystorePath);
                if (!keystoreFile.exists())
                {
                    throw new FileNotFoundException(keystoreFile.getAbsolutePath());
                }

                // SSL Context Factory for HTTPS
                // SSL requires a certificate so we configure a factory for ssl contents
                // with information pointing to what keystore the ssl connection needs
                // to know about. Much more configuration is available the ssl context,
                // including things like choosing the particular certificate out of a
                // keystore to be used.
                sslContextFactory.setKeyStorePath(keystoreFile.getAbsolutePath());
                sslContextFactory.setKeyStorePassword(config.getKeystorePassword());
                sslContextFactory.setKeyManagerPassword(config.getKeystorePassword());

                //Exclude all weak ciphers
                sslContextFactory.setExcludeCipherSuites("^.*_(MD5|SHA|SHA1)$");
                // Exclude ciphers that don't support forward secrecy
                sslContextFactory.addExcludeCipherSuites("^TLS_RSA_.*$");
                // Exclude SSL ciphers (that are accidentally present due to Include patterns)
                sslContextFactory.addExcludeCipherSuites("^SSL_.*$");
                // Exclude NULL ciphers (that are accidentally present due to Include patterns)
                sslContextFactory.addExcludeCipherSuites("^.*_NULL_.*$");
                // Exclude anon ciphers (that are accidentally present due to Include patterns)
                sslContextFactory.addExcludeCipherSuites("^.*_anon_.*$");

                //Exclude CBC ciphers
                sslContextFactory.addExcludeCipherSuites("^.*_CBC_.*$");

                sslContextFactory.addExcludeCipherSuites("TLS_DHE_RSA_WITH_AES_128_GCM_SHA256");


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
                HttpConfiguration https_config = new HttpConfiguration();
                https_config.setRequestHeaderSize(HttpUtils.MAX_HEADER_SIZE);
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
                https.setPort(config.getPort());
                https.setIdleTimeout(500000);
                https.setHost(config.getAddress());

                // Here you see the server having multiple connectors registered with
                // it, now requests can flow into the server from both http and https
                // urls to their respective ports and be processed accordingly by jetty.
                // A simple handler is also registered with the server so the example
                // has something to pass requests off to.

                // Set the connectors
                server.setConnectors(new Connector[] { https });
            }

            else if (config.getTlsConfiguration().equals(HttpUtils.MSG_OFF))
            {
                // HTTP connector
                // The first server connector we create is the one for http, passing in
                // the http configuration we configured above so it can get things like
                // the output buffer size, etc. We also set the port (8080) and
                // configure an idle timeout.
                HttpConfiguration http_config = new HttpConfiguration();
                http_config.setRequestHeaderSize(HttpUtils.MAX_HEADER_SIZE);

                ServerConnector http = new ServerConnector(server,
                        new HttpConnectionFactory(http_config));
                http.setPort(config.getPort());
                http.setIdleTimeout(500000);
                http.setHost(config.getAddress());

                // Here you see the server having multiple connectors registered with
                // it, now requests can flow into the server from both http and https
                // urls to their respective ports and be processed accordingly by jetty.
                // A simple handler is also registered with the server so the example
                // has something to pass requests off to.

                // Set the connectors
                server.setConnectors(new Connector[] { http });
            }

            else
            {
                throw new JNLException("Cannot subscribe without configuring TLS on/off");
            }

            // Passing in the class for the Servlet allows jetty to instantiate an
            // instance of that Servlet and mount it on a given context path.

            // IMPORTANT:
            // This is a raw Servlet, not a Servlet that has been configured
            // through a web.xml @WebServlet annotation, or anything similar.

            //Sets allowed configure digest values from config
            HttpUtils.setAllowedConfigureDigests(config.getAllowedConfigureDigests());

            //Separate endpoints/servlets for audit,journal,log
            //Only sets up endpoints as allowed in the configuration file.
            Set<RecordType>recordTypeSet = config.getRecordTypes();
            if (recordTypeSet.contains(RecordType.Unset))
            {
                throw new JNLException("Cannot subscribe with a RecordType of 'Unset'");
            }

            if (recordTypeSet.contains(RecordType.Log))
            {
                logger.info("Log endpoint is active.");
                handler.addServlet(JNLLogServlet.class, HttpUtils.LOG_ENDPOINT);
            }

            if (recordTypeSet.contains(RecordType.Audit))
            {
                logger.info("Audit endpoint is active.");
                handler.addServlet(JNLAuditServlet.class, HttpUtils.AUDIT_ENDPOINT);
            }

            if (recordTypeSet.contains(RecordType.Journal))
            {
                logger.info("Journal endpoint is active.");
                handler.addServlet(JNLJournalServlet.class, HttpUtils.JOURNAL_ENDPOINT);
            }

            // Start things up!
            server.start();

            if (config.getTlsConfiguration().equals(HttpUtils.MSG_ON)) {
                //Display supported protocols and ciphers.
                SSLEngine engine = sslContextFactory.newSSLEngine();
                String enabledProtocols[] = engine.getEnabledProtocols();
                String enabledCiphers[] = engine.getEnabledCipherSuites();

                logger.info("JALoP Jetty Server only supports the following protocols: " + Arrays.toString(enabledProtocols));
                logger.info("JALoP Jetty Server only supports the following ciphers: " + Arrays.toString(enabledCiphers));
            }

            // The use of server.join() the will make the current thread join and
            // wait until the server is done executing.
            // See
            // http://docs.oracle.com/javase/7/docs/api/java/lang/Thread.html#join()
            server.join();
        } else {
            logger.error("Invalid configuration, only subscriber mode is supported");
        }
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

    @Override
    public SubscribeRequest getSubscribeRequest(final SubscriberSession sess) {
        // TODO: All the code here to manage the maps should really be happening in the
        // connection handler callbacks, but the library isn't generating those events
        // quite yet.
        SubscriberImpl sub;
        synchronized (this.sessMap) {
            sub = this.sessMap.get(sess);
            if (sub == null) {
                sub = new SubscriberImpl(sess.getRecordType(), config.getOutputPath(), null, this, sess.getPublisherId());
                this.sessMap.put(sess, sub);
            }
        }

        return sub.getSubscribeRequest(sess);
    }

    public Session getSessionBySessionId(String sessionId)
    {
        Session foundSession = null;
        synchronized (this.sessMap) {

            for (Session currSession : this.sessMap.keySet())
            {
                if (((SubscriberHttpSessionImpl)currSession).getSessionId().equals(sessionId))
                {
                    foundSession = currSession;
                    break;
                }
            }
        }

        return foundSession;
    }


    public void prepareForNewSession()
    {
        synchronized (this.sessMap) {

            if (this.sessMap.size() == config.getMaxSessionLimit())
            {
                String oldestSessionId = null;
                LocalDateTime oldestLastTouchedTimestamp = null;
                for (Map.Entry<Session, SubscriberImpl> entry : sessMap.entrySet())
                {
                    if (oldestSessionId == null && oldestLastTouchedTimestamp == null)
                    {
                        SubscriberHttpSessionImpl session = ((SubscriberHttpSessionImpl)entry.getKey());
                        oldestSessionId = session.getSessionId();
                        oldestLastTouchedTimestamp = session.getLastTouchedTimestamp();
                        continue;
                    }

                    SubscriberHttpSessionImpl session = ((SubscriberHttpSessionImpl)entry.getKey());

                    if (oldestLastTouchedTimestamp.compareTo(session.getLastTouchedTimestamp()) > 0)
                    {
                        oldestSessionId = session.getSessionId();
                        oldestLastTouchedTimestamp = session.getLastTouchedTimestamp();
                    }
                }

                removeSession(oldestSessionId);

            }
        }
    }

    public void removeSession(String sessionId)
    {
        synchronized (this.sessMap) {

            Session removeKey = null;

            for (Session currSession : this.sessMap.keySet())
            {
                SubscriberHttpSessionImpl session = ((SubscriberHttpSessionImpl)currSession);
                if (session.getSessionId().equals(sessionId))
                {
                    removeKey = currSession;
                    break;
                }
            }

            this.sessMap.remove(removeKey);
        }
    }

    @Override
    public boolean notifySysMetadata(final SubscriberSession sess, final RecordInfo recordInfo, final InputStream sysMetaData) {
        return this.sessMap.get(sess).notifySysMetadata(sess, recordInfo, sysMetaData);
    }

    @Override
    public boolean notifyAppMetadata(final SubscriberSession sess, final RecordInfo recordInfo, final InputStream appMetaData) {
        return this.sessMap.get(sess).notifyAppMetadata(sess, recordInfo, appMetaData);
    }

    @Override
    public boolean notifyPayload(final SubscriberSession sess, final RecordInfo recordInfo, final InputStream payload) {
        return this.sessMap.get(sess).notifyPayload(sess, recordInfo, payload);
    }

    @Override
    public boolean notifyDigest(final SubscriberSession sess, final RecordInfo recordInfo, final byte[] digest) {
        return this.sessMap.get(sess).notifyDigest(sess, recordInfo, digest);
    }

    @Override
    public boolean notifyDigestResponse(final SubscriberSession sess, final String nonce, final DigestStatus status) {
        return this.sessMap.get(sess).notifyDigestResponse(sess, nonce, status);
    }

    @Override
    public boolean notifyJournalMissing(final SubscriberSession sess, final String nonce) {
        return this.sessMap.get(sess).notifyJournalMissing(sess, nonce);
    }

    @Override
    public Mode getMode() {
        return config.getMode();
    }

    public HttpSubscriberConfig getConfig()
    {
        return config;
    }
}