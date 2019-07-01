package com.tresys.jalop.utils.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.xml.crypto.dsig.DigestMethod;

import org.apache.commons.io.FileUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.EntityBuilder;
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

import com.tresys.jalop.jnl.Mode;
import com.tresys.jalop.jnl.RecordType;
import com.tresys.jalop.jnl.impl.http.HttpSubscriberConfig;
import com.tresys.jalop.jnl.impl.http.HttpUtils;
import com.tresys.jalop.jnl.impl.http.JNLAuditServlet;
import com.tresys.jalop.jnl.impl.http.JNLJournalServlet;
import com.tresys.jalop.jnl.impl.http.JNLLogServlet;
import com.tresys.jalop.utils.jnltest.JNLSubscriber;

public class TestResources {
    public static int HTTP_PORT = 8080;
    public static String SESSION_ID = "fe8a54d7-dd7c-4c50-a7e7-f948a140c556";
    /** The filename for the system meta-data document. */
    public static final String SYS_META_FILENAME = "sys_metadata.xml";

    /** The filename for the application meta-data document. */
    public static final String APP_META_FILENAME = "app_metadata.xml";

    /** The filename for the payload. */
    public static final String PAYLOAD_FILENAME = "payload";

    private static final String NONCE_FORMAT_STRING = "0000000000";
    static final DecimalFormat NONCE_FORMATER =
            new DecimalFormat(NONCE_FORMAT_STRING);

    public static String getAutoNumberDirectoryName(int count)
    {
        return NONCE_FORMATER.format(count);
    }

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

    public static ArrayList<RecordType> getRecordTypes(String recordTypeString)
    {
        ArrayList<RecordType> recordTypes = new ArrayList<RecordType>();
        if (recordTypeString != null)
        {
            String [] splitValues = recordTypeString.split(",");

            for (String value : splitValues)
            {
                RecordType recType = HttpUtils.getRecordType(value.trim());

                if (recType != RecordType.Unset)
                {
                    recordTypes.add(recType);
                }
            }
        }
        else
        {
            //Add all record types by default if none passed in.
            for (RecordType recType : RecordType.values())
            {
                if (recType != RecordType.Unset)
                {
                    recordTypes.add(recType);
                }
            }
        }

        return recordTypes;
    }

    public static ArrayList<String> getRecordTypeNames(ArrayList<RecordType> recordTypes)
    {
        ArrayList<String> recordTypeNames = new ArrayList<String>();

        for (RecordType recType : recordTypes)
        {
            recordTypeNames.add(recType.toString());
        }

        return recordTypeNames;
    }

    public static String formatElapsedTime(final long elapsedTimeMillis)
    {
        long millis = elapsedTimeMillis % 1000;
        long second = (elapsedTimeMillis / 1000) % 60;
        long minute = (elapsedTimeMillis / (1000 * 60)) % 60;
        long hour = (elapsedTimeMillis / (1000 * 60 * 60)) % 24;

        String time = String.format("%02d:%02d:%02d.%d hh:mm:ss:SSS", hour, minute, second, millis);

        return time;
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
        config.setMode(Mode.Live);

        List<String> allowedConfigureDigests = new ArrayList<String>();
        allowedConfigureDigests.add(HttpUtils.MSG_ON);
        allowedConfigureDigests.add(HttpUtils.MSG_OFF);
        config.setAllowedConfigureDigests(allowedConfigureDigests);

        HttpUtils.setAllowedConfigureDigests(allowedConfigureDigests);
        config.setOutputPath(new File("./output"));
        config.setMaxSessionLimit(5);

        JNLSubscriber subscriber = new JNLSubscriber(config);
        HttpUtils.setSubscriber(subscriber);

        return server;
    }

    public static void cleanAllDirectories(String inputDirStr, String outputDirStr) throws IOException
    {
        for (RecordType recType : RecordType.values())
        {

            if (recType.equals(RecordType.Unset))
            {
                continue;
            }
            cleanInputDirectory(recType, inputDirStr);
        }

        //Clears out input and output directories
        cleanOutputDirectory(outputDirStr);
    }

    public static boolean cleanInputDirectory(RecordType recType, String inputDirStr)
    {
        File inputDir = new File(inputDirStr + "/" + recType.toString().toLowerCase());

        try
        {
            if (inputDirStr != null && inputDirStr.contains("input"))
            {
                FileUtils.deleteDirectory(inputDir);
            }
        }
        catch (IOException ioe)
        {
            ioe.printStackTrace();
            return false;
        }

        return true;
    }

    public static void cleanOutputDirectory(String outputDirStr) throws IOException
    {
        if (outputDirStr != null && outputDirStr.contains("output"))
        {
            File outputDir = new File(outputDirStr);
            FileUtils.deleteDirectory(outputDir);
        }
    }

    public static String sendValidJalRecord(RecordType recType, String sessionId, String jalId) throws ClientProtocolException, IOException
    {
        return sendValidJalRecord(recType, sessionId, jalId, "19", "jal_record1.txt", "bbd801ce4dc24520c028025c05b44c5532b240824d2d7ce25644b73b667b6c7a");
    }

    public static String sendValidJalRecord(RecordType recType, String sessionId, String jalId, String payloadSize, String filename, String expectedDigest) throws ClientProtocolException, IOException
    {
        File resourcesDirectory = new File("src/test/resources/unit_test");

        HttpPost httpPost = new HttpPost("http://localhost:" + TestResources.HTTP_PORT + "/" + recType.toString().toLowerCase());

        HashMap<String, String> headers = TestResources.getJalRecordHeaders(sessionId, jalId, "3083", "1125", payloadSize, recType);

        for (Map.Entry<String, String> entry : headers.entrySet())
        {
            httpPost.setHeader(entry.getKey(), entry.getValue());
        }

        //Adds jal record to post
        String jalRecord1Path = resourcesDirectory.getAbsolutePath() + "/" + filename;
        HttpEntity entity = EntityBuilder.create().setFile(new File(jalRecord1Path)).build();

        httpPost.setEntity(entity);

        HttpClient client = HttpClientBuilder.create().build();

        final HttpResponse response = client.execute(httpPost);
        final Header digestHeader = response.getFirstHeader(HttpUtils.HDRS_DIGEST_VALUE);
        final Header errorMessage = response.getFirstHeader(HttpUtils.HDRS_ERROR_MESSAGE);
        final int responseStatus = response.getStatusLine().getStatusCode();
        assertEquals(200, responseStatus);
        assertEquals(null, errorMessage);

        //Validate digest is correct for test file sent.
        assertNotNull(digestHeader);
        assertEquals(expectedDigest, digestHeader.getValue());

        return jalId;
    }

    public static String sendValidJalRecord(RecordType recType, String sessionId) throws ClientProtocolException, IOException
    {
        String jalId = UUID.randomUUID().toString();

        return sendValidJalRecord(recType, sessionId, jalId);
    }

    public static HashMap<String, String> getJalRecordHeaders(String sessionId, String jalId, String systemMetadataLen, String appMetadataLen, String payloadLength, RecordType recType)
    {
        String jalLengthHeader = "JAL-" + recType.toString() + "-Length";
        String jalMessage = recType.toString().toLowerCase() +  "-record";

        String auditFormat = null;
        if (recType.equals(RecordType.Audit))
        {
            auditFormat = HttpUtils.ENC_XML;
        }

        return getJalRecordHeaders(sessionId, jalId, systemMetadataLen, appMetadataLen, payloadLength, jalLengthHeader, jalMessage, auditFormat);
    }

    public static HashMap<String, String> getJalRecordHeaders(String sessionId, String jalId, String systemMetadataLen, String appMetadataLen, String payloadLength, String jalLengthHeader, String jalMessage, String auditFormat)
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

        if (auditFormat != null)
        {
            headers.put(HttpUtils.HDRS_AUDIT_FORMAT, auditFormat);
        }

        if (jalMessage != null)
        {
            headers.put(HttpUtils.HDRS_MESSAGE, jalMessage);
        }

        return headers;
    }

    public static String sendValidInitialize(RecordType recType, boolean performDigest, String publisherId) throws ClientProtocolException, IOException
    {
        return sendValidInitialize(recType, performDigest, publisherId, HttpUtils.MSG_LIVE);
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
    public static String sendValidInitialize(RecordType recType, boolean performDigest, String publisherId, String mode) throws ClientProtocolException, IOException
    {
        final HttpPost httpPost = new HttpPost("http://localhost:" + HTTP_PORT + "/" + recType.toString().toLowerCase());
        httpPost.setHeader(HttpUtils.HDRS_CONTENT_TYPE, HttpUtils.DEFAULT_CONTENT_TYPE);
        httpPost.setHeader(HttpUtils.HDRS_PUBLISHER_ID, publisherId);
        httpPost.setHeader(HttpUtils.HDRS_MESSAGE, HttpUtils.MSG_INIT);
        httpPost.setHeader(HttpUtils.HDRS_MODE, mode);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_DIGEST, DigestMethod.SHA256);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_XML_COMPRESSION, HttpUtils.SUPPORTED_XML_COMPRESSIONS[0]);
        httpPost.setHeader(HttpUtils.HDRS_RECORD_TYPE, recType.toString().toLowerCase());
        httpPost.setHeader(HttpUtils.HDRS_VERSION, HttpUtils.SUPPORTED_VERSIONS[0]);

        if (performDigest == true)
        {
            httpPost.setHeader(HttpUtils.HDRS_ACCEPT_CONFIGURE_DIGEST_CHALLENGE, HttpUtils.MSG_ON);
        }
        else
        {
            httpPost.setHeader(HttpUtils.HDRS_ACCEPT_CONFIGURE_DIGEST_CHALLENGE, HttpUtils.MSG_OFF);
        }

        HttpClient client = HttpClientBuilder.create().build();

        final HttpResponse response = client.execute(httpPost);
        final String sessionId = response.getFirstHeader(HttpUtils.HDRS_SESSION_ID).getValue();

        return sessionId;
    }
}
