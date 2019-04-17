package com.tresys.jalop.jnl.impl.http;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.xml.crypto.dsig.DigestMethod;

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletHandler;

import com.tresys.jalop.jnl.RecordType;

public class TestResources {
    public static int HTTP_PORT = 8080;
    public static String SESSION_ID = "fe8a54d7-dd7c-4c50-a7e7-f948a140c556";
    public static String CERT_FINGERPRINT = "0a7be6d47ac528d8ce0e1c59b4d052be88f5ddc4601caec1f1dcda8a09766723";
    public static String CERT_FILENAME = "cert.pem";
    public static String DIFFERENT_CERT_FILENAME = "different_cert.pem";

    private static String certHeaderStr = null;

    //Disables all http client logging
    private static void disableHttpClientLogging()
    {
        //Following logger config settings get rid of the httpclient debug logging in the console.
        //Only comment out these lines if you wish to see the http client debug statements
        java.util.logging.Logger.getLogger("org.apache.http.wire").setLevel(java.util.logging.Level.FINEST);
        java.util.logging.Logger.getLogger("org.apache.http.headers").setLevel(java.util.logging.Level.FINEST);
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.SimpleLog");
        System.setProperty("org.apache.commons.logging.simplelog.showdatetime", "true");
        System.setProperty("org.apache.commons.logging.simplelog.log.httpclient.wire", "ERROR");
        System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http", "ERROR");
        System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http.headers", "ERROR");
    }

    public static void configureLogging(Level logLevel)
    {
        //This method disables the debug logging for the http client in the console, only comment out this method if you wish to see this output.
        //Unless there is some need to view this data, this should be called all the time before any tests using httpclient to prevent cluttered console output.
        disableHttpClientLogging();

        LogManager.getRootLogger().setLevel(logLevel);
    }


    public static Server getWebServer()
    {
        Server server = new Server(HTTP_PORT);

        ServletHandler handler = new ServletHandler();
        HttpConfiguration http_config = new HttpConfiguration();
        http_config.setRequestHeaderSize(HttpUtils.MAX_HEADER_SIZE);

        ServerConnector http = new ServerConnector(server,
                new HttpConnectionFactory(http_config));
        http.setPort(HTTP_PORT);
        server.setConnectors(new Connector[] { http, });
        server.setHandler(handler);

        handler.addServletWithMapping(JNLJournalServlet.class, HttpUtils.JOURNAL_ENDPOINT);
        handler.addServletWithMapping(JNLAuditServlet.class, HttpUtils.AUDIT_ENDPOINT);
        handler.addServletWithMapping(JNLLogServlet.class, HttpUtils.LOG_ENDPOINT);

        //Sets up the subscriber
        HttpSubscriberConfig config = new HttpSubscriberConfig();

        List<String> allowedConfigureDigests = new ArrayList<String>();
        allowedConfigureDigests.add(HttpUtils.MSG_CONFIGURE_DIGEST_ON);
        allowedConfigureDigests.add(HttpUtils.MSG_CONFIGURE_DIGEST_OFF);
        config.setAllowedConfigureDigests(allowedConfigureDigests);

        HttpUtils.setAllowedConfigureDigests(allowedConfigureDigests);
        config.setOutputPath(new File("./output"));

        JNLSubscriber subscriber = new JNLSubscriber(config);
        HttpUtils.setSubscriber(subscriber);

        return server;
    }

    public static String getCertForHeader(String filename, String testDir)
    {
        String content = null;
        String filePath = testDir + "/" + filename;
        try
        {
            content = new String ( Files.readAllBytes( Paths.get(filePath) ) );
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return null;
        }

        if (content != null)
        {
            content = content.replace("\n", "");
        }
        return content;
    }

    public static String getCertForHeader(String testDir)
    {
       if (certHeaderStr == null)
       {
           certHeaderStr = getCertForHeader(CERT_FILENAME, testDir);
       }

       return certHeaderStr;
    }

    public static String getCertFingerprintForHeader(String filename, String testDir)
    {
        String content = null;
        String fingerprint = null;
        String filePath = testDir + "/" + filename;
        try
        {
            content = new String ( Files.readAllBytes( Paths.get(filePath) ) );
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return null;
        }

        if (content != null)
        {
            fingerprint = HttpUtils.getCertFingerprintFromHeader(content);
        }
        return fingerprint;
    }

    public static void cleanOutputDirectory(String outputDirStr) throws IOException
    {
        if (outputDirStr != null && outputDirStr.contains("output"))
        {
            File outputDir = new File(outputDirStr);
            FileUtils.deleteDirectory(outputDir);
        }
    }

    public static HashMap<String, String> getJalRecordHeaders(String sessionId, String jalId, String systemMetadataLen, String appMetadataLen, String payloadLength, String jalLengthHeader, String jalMessage)
    {
        HashMap<String, String> headers = new HashMap<String, String>();

        headers.put(HttpUtils.HDRS_CONTENT_TYPE, HttpUtils.DEFAULT_CONTENT_TYPE);

        if (sessionId != null)
        {
            headers.put(HttpUtils.HDRS_SESSION_ID, sessionId);
        }

        if (jalId != null)
        {
            headers.put(HttpUtils.HDRS_NONCE, jalId);
        }

        if (systemMetadataLen != null)
        {
            headers.put(HttpUtils.HDRS_SYS_META_LEN, systemMetadataLen);
        }

        if (appMetadataLen != null)
        {
            headers.put(HttpUtils.HDRS_APP_META_LEN, appMetadataLen);
        }

        if (payloadLength != null && jalLengthHeader != null)
        {
            headers.put(jalLengthHeader, payloadLength);
        }

        if (jalMessage != null)
        {
            headers.put(HttpUtils.HDRS_MESSAGE, jalMessage);
        }

        return headers;
    }

    /**
     * This will send a valid initialize message and get back a session id.
     * @param recType
     * @param performDigest
     * @param publisherId
     * @return a valid sessionID
     * @throws ClientProtocolException
     * @throws IOException
     */
    public static String sendValidInitialize(RecordType recType, boolean performDigest, String publisherId, String testDir) throws ClientProtocolException, IOException
    {
        final HttpPost httpPost = new HttpPost("http://localhost:" + HTTP_PORT + "/" + recType.toString().toLowerCase());
        httpPost.setHeader(HttpUtils.HDRS_CONTENT_TYPE, HttpUtils.DEFAULT_CONTENT_TYPE);
        httpPost.setHeader(HttpUtils.HDRS_PUBLISHER_ID, publisherId);
        httpPost.setHeader(HttpUtils.HDRS_MESSAGE, HttpUtils.MSG_INIT);
        httpPost.setHeader(HttpUtils.HDRS_MODE, HttpUtils.MSG_LIVE);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_DIGEST, DigestMethod.SHA256);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_XML_COMPRESSION, HttpUtils.SUPPORTED_XML_COMPRESSIONS[0]);
        httpPost.setHeader(HttpUtils.HDRS_DATA_CLASS, recType.toString().toLowerCase());
        httpPost.setHeader(HttpUtils.HDRS_VERSION, HttpUtils.SUPPORTED_VERSIONS[0]);
        httpPost.setHeader(HttpUtils.HDRS_CLIENT_CERTIFICATE, getCertForHeader(CERT_FILENAME, testDir));

        if (performDigest == true)
        {
            httpPost.setHeader(HttpUtils.HDRS_ACCEPT_CONFIGURE_DIGEST_CHALLENGE, HttpUtils.MSG_CONFIGURE_DIGEST_ON);
        }
        else
        {
            httpPost.setHeader(HttpUtils.HDRS_ACCEPT_CONFIGURE_DIGEST_CHALLENGE, HttpUtils.MSG_CONFIGURE_DIGEST_OFF);
        }

        HttpClient client = HttpClientBuilder.create().build();

        final HttpResponse response = client.execute(httpPost);
        final String sessionId = response.getFirstHeader(HttpUtils.HDRS_SESSION_ID).getValue();

        return sessionId;
    }
}
