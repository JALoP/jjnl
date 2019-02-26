package com.tresys.jalop.utils.jnltest;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.net.ssl.SSLEngine;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.json.simple.parser.ParseException;

import com.tresys.jalop.jnl.ConnectAck;
import com.tresys.jalop.jnl.ConnectNack;
import com.tresys.jalop.jnl.Connection;
import com.tresys.jalop.jnl.ConnectionHandler;
import com.tresys.jalop.jnl.ConnectionRequest;
import com.tresys.jalop.jnl.DigestStatus;
import com.tresys.jalop.jnl.Mode;
import com.tresys.jalop.jnl.RecordInfo;
import com.tresys.jalop.jnl.RecordType;
import com.tresys.jalop.jnl.Role;
import com.tresys.jalop.jnl.Session;
import com.tresys.jalop.jnl.SubscribeRequest;
import com.tresys.jalop.jnl.Subscriber;
import com.tresys.jalop.jnl.SubscriberSession;
import com.tresys.jalop.utils.jnltest.Config.Config;
import com.tresys.jalop.utils.jnltest.Config.ConfigurationException;


    @SuppressWarnings("serial")
    public class JNLSubscriber extends HttpServlet implements Subscriber, ConnectionHandler, JNLTestInterface
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
                ServletHandler handler = new ServletHandler();
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
                handler.addServletWithMapping(JNLSubscriber.class, "/*");

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
        protected void doGet( HttpServletRequest request,
                              HttpServletResponse response ) throws ServletException,
                                                            IOException
        {
            response.setContentType("text/html");
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().println("<h1>Jalop Jetty Server</h1>");
        }

        public HashMap<String, String> parseHttpHeaders(HttpServletRequest request)
        {
            HashMap<String, String> currHeaders = new HashMap<String, String>();
            Enumeration<String> headerNames = request.getHeaderNames();

            while (headerNames.hasMoreElements()) {

                String headerName = headerNames.nextElement();
                String headerValue = request.getHeader(headerName);
                System.out.println("Header Name: " + headerName + " Header Value: " + headerValue);

                currHeaders.put(headerName, headerValue);
            }

            return currHeaders;
        }

        @Override
        protected void doPost(HttpServletRequest request,
                HttpServletResponse response)
                        throws ServletException, IOException {

            HashMap<String, String> currHeaders = parseHttpHeaders(request);

            //Handles messages

            //Init message
            String messageType = request.getHeader(HttpUtils.HDRS_MESSAGE);
            if (messageType.equals(HttpUtils.MSG_INIT))
            {
                System.out.println(HttpUtils.MSG_INIT + " message received.");

                HashMap <String,String> successResponseHeaders = new HashMap<String,String>();
                HashMap <String,String> errorResponseHeaders = new HashMap<String,String>();

                //Validates mode, must be publish live or publish archive, sets any error in response.
                String modeStr = request.getHeader(HttpUtils.HDRS_MODE);
                if (!HttpUtils.validateMode(modeStr, errorResponseHeaders))
                {
                    System.out.println("Initialize message failed due to invalid mode value of: " + modeStr);
                    HttpUtils.setInitializeNackResponse(errorResponseHeaders, response);
                    return;
                }

                //Validates supported digest
                String digestStr = request.getHeader(HttpUtils.HDRS_ACCEPT_DIGEST);
                if (!HttpUtils.validateDigests(digestStr, successResponseHeaders, errorResponseHeaders))
                {
                    System.out.println("Initialize message failed due to none of the following digests supported: " + digestStr);
                    HttpUtils.setInitializeNackResponse(errorResponseHeaders, response);
                    return;
                }

                //Validates supported xml compression
                String xmlCompressionsStr = request.getHeader(HttpUtils.HDRS_ACCEPT_XML_COMPRESSION);
                if (!HttpUtils.validateXmlCompression(xmlCompressionsStr, successResponseHeaders, errorResponseHeaders))
                {
                    System.out.println("Initialize message failed due to none of the following xml compressions supported: " + xmlCompressionsStr);
                    HttpUtils.setInitializeNackResponse(errorResponseHeaders, response);
                    return;
                }

                //Validates data class
                String dataClassStr = request.getHeader(HttpUtils.HDRS_DATA_CLASS);
                if (!HttpUtils.validateDataClass(dataClassStr))
                {
                    System.out.println("Initialize message failed due to none of the following data classes: " + dataClassStr);
                    HttpUtils.setInitializeNackResponse(errorResponseHeaders, response);
                    return;
                }

                //Validates version
                String versionStr = request.getHeader(HttpUtils.HDRS_VERSION);
                if (!HttpUtils.validateVersion(versionStr, errorResponseHeaders))
                {
                    System.out.println("Initialize message failed due to unsupported version: " + versionStr);
                    HttpUtils.setInitializeNackResponse(errorResponseHeaders, response);
                    return;
                }

                //If no errors, return initialize-ack with supported digest/encoding
                System.out.println("Initialize message is valid, sending intialize-ack");
                HttpUtils.setInitializeAckResponse(successResponseHeaders, response);
            }
            else
            {
                System.out.println("Invalid message received: " + messageType + " , returning server error");
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                return;
            }

            ///TODO - Handle the body of the http post, needed for record binary transfer, demo file transfer currently
            readBinaryDataFromRequest(request);

            writeBinaryDataToResponse(response);
        }

        public void readBinaryDataFromRequest(HttpServletRequest request) throws IOException
        {
            //Handle the binary data that was posted from the client in the request
            byte[] readRequestBuffer = new byte[524288]; //new byte[10240];
            File fileUpload = new File("/tmp/upload.bin");
            try (
                    InputStream input = request.getInputStream(); //Reading the binary post data in the request
                    OutputStream output = new FileOutputStream(fileUpload);
                    )
            {
                for (int length = 0; (length = input.read(readRequestBuffer)) > 0;)
                {
                    output.write(readRequestBuffer, 0, length);
                }
            }
        }

        public void writeBinaryDataToResponse(HttpServletResponse response) throws IOException
        {
            //Handle sending the binary data response to the client.   Currently this is just sending the file back to the
            //client, but would be changed to send the jalop binary response instead.
            byte[] writeResponseBuffer = new byte[524288]; //new byte[10240];
            File fileUpload = new File("/tmp/upload.bin");
            try (
                    InputStream input = new FileInputStream(fileUpload);
                    OutputStream output = response.getOutputStream(); //Just need to write to this output stream to send response back to client
                    )
            {
                for (int length = 0; (length = input.read(writeResponseBuffer)) > 0;)
                {
                    output.write(writeResponseBuffer, 0, length);
                }
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

        @Override
        public Set<ConnectError> handleConnectionRequest(final boolean rejecting,
                final ConnectionRequest connRequest) {
            return this.connectionHandler.handleConnectionRequest(rejecting, connRequest);
        }

        @Override
        public void sessionClosed(final Session sess) {
            this.connectionHandler.sessionClosed(sess);
        }

        @Override
        public void connectionClosed(final Connection conn) {
            this.connectionHandler.connectionClosed(conn);
        }

        @Override
        public void connectAck(final Session sess, final ConnectAck ack) {
            this.connectionHandler.connectAck(sess, ack);
        }

        @Override
        public void connectNack(final Session sess, final ConnectNack nack) {
            this.connectionHandler.connectNack(sess, nack);
        }
    }

