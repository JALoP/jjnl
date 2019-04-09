package com.tresys.jalop.jnl.impl.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
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
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.tresys.jalop.jnl.RecordType;

/**
 * Tests for common utility class.
 */
public class MessageProcessorTest {

    private static Server server;
    private static int HTTP_PORT = 8080;
    private static String SESSION_ID = "fe8a54d7-dd7c-4c50-a7e7-f948a140c556";

    private static String jjnlDirPath = "";
    private static String outputDirStr = "";
    private static File resourcesDirectory;

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    /**
     * Sets up the server for the web service.
     *
     * @throws Exception
     */
    @BeforeClass
    public static void startWebServiceServer() throws Exception {

        HttpUtilsTest.disableHttpClientLogging();

        //gets jjnl dir path
        resourcesDirectory = new File("src/test/resources");
        jjnlDirPath = resourcesDirectory.getAbsolutePath() + "/../../../..";
        outputDirStr = jjnlDirPath + "/jnl_lib/output";

        server = getWebServer();

        server.start();
    }

    public static Server getWebServer()
    {
        Server server = new Server(HTTP_PORT);

        ServletHandler handler = new ServletHandler();
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

    private static void cleanOutputDirectory() throws IOException
    {
        if (outputDirStr != null && outputDirStr.contains("output"))
        {
            File outputDir = new File(outputDirStr);
            FileUtils.deleteDirectory(outputDir);
        }
    }

    /**
     * Stops the web service server.
     *
     * @throws Exception
     */
    @AfterClass
    public static void stopWebServiceServer() throws Exception {
        server.stop();

        cleanOutputDirectory();
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

    public static String sendValidInitialize(RecordType recType, boolean performDigest, String publisherId) throws ClientProtocolException, IOException
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

    @Test
    public void testInitializeJournalReturnsInitializeAck() throws ClientProtocolException, IOException {

        UUID publisherUUID = UUID.randomUUID();
        final String publisherId = publisherUUID.toString();
        final HttpPost httpPost = new HttpPost("http://localhost:" + HTTP_PORT + HttpUtils.JOURNAL_ENDPOINT);
        httpPost.setHeader(HttpUtils.HDRS_CONTENT_TYPE, HttpUtils.DEFAULT_CONTENT_TYPE);
        httpPost.setHeader(HttpUtils.HDRS_PUBLISHER_ID, publisherId);
        httpPost.setHeader(HttpUtils.HDRS_MESSAGE, HttpUtils.MSG_INIT);
        httpPost.setHeader(HttpUtils.HDRS_MODE, HttpUtils.MSG_LIVE);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_DIGEST, DigestMethod.SHA256);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_XML_COMPRESSION, HttpUtils.SUPPORTED_XML_COMPRESSIONS[0]);
        httpPost.setHeader(HttpUtils.HDRS_DATA_CLASS, HttpUtils.JOURNAL);
        httpPost.setHeader(HttpUtils.HDRS_VERSION, HttpUtils.SUPPORTED_VERSIONS[0]);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_CONFIGURE_DIGEST_CHALLENGE, HttpUtils.MSG_CONFIGURE_DIGEST_ON);

        HttpClient client = HttpClientBuilder.create().build();

        final HttpResponse response = client.execute(httpPost);
        final String responseMessage = response.getFirstHeader(HttpUtils.HDRS_MESSAGE).getValue();
        final int responseStatus = response.getStatusLine().getStatusCode();
        assertEquals(200, responseStatus);
        assertEquals("initialize-ack", responseMessage);
    }

    @Test
    public void testInitializeAuditReturnsInitializeAck() throws ClientProtocolException, IOException {

        UUID publisherUUID = UUID.randomUUID();
        final String publisherId = publisherUUID.toString();
        final HttpPost httpPost = new HttpPost("http://localhost:" + HTTP_PORT + HttpUtils.AUDIT_ENDPOINT);
        httpPost.setHeader(HttpUtils.HDRS_CONTENT_TYPE, HttpUtils.DEFAULT_CONTENT_TYPE);
        httpPost.setHeader(HttpUtils.HDRS_PUBLISHER_ID, publisherId);
        httpPost.setHeader(HttpUtils.HDRS_MESSAGE, HttpUtils.MSG_INIT);
        httpPost.setHeader(HttpUtils.HDRS_MODE, HttpUtils.MSG_LIVE);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_DIGEST, DigestMethod.SHA256);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_XML_COMPRESSION, HttpUtils.SUPPORTED_XML_COMPRESSIONS[0]);
        httpPost.setHeader(HttpUtils.HDRS_DATA_CLASS, HttpUtils.AUDIT);
        httpPost.setHeader(HttpUtils.HDRS_VERSION, HttpUtils.SUPPORTED_VERSIONS[0]);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_CONFIGURE_DIGEST_CHALLENGE, HttpUtils.MSG_CONFIGURE_DIGEST_ON);

        HttpClient client = HttpClientBuilder.create().build();

        final HttpResponse response = client.execute(httpPost);
        final String responseMessage = response.getFirstHeader(HttpUtils.HDRS_MESSAGE).getValue();
        final int responseStatus = response.getStatusLine().getStatusCode();
        assertEquals(200, responseStatus);
        assertEquals("initialize-ack", responseMessage);
    }

    @Test
    public void testInitializeLogReturnsInitializeAck() throws ClientProtocolException, IOException {

        UUID publisherUUID = UUID.randomUUID();
        final String publisherId = publisherUUID.toString();
        final HttpPost httpPost = new HttpPost("http://localhost:" + HTTP_PORT + HttpUtils.LOG_ENDPOINT);
        httpPost.setHeader(HttpUtils.HDRS_CONTENT_TYPE, HttpUtils.DEFAULT_CONTENT_TYPE);
        httpPost.setHeader(HttpUtils.HDRS_PUBLISHER_ID, publisherId);
        httpPost.setHeader(HttpUtils.HDRS_MESSAGE, HttpUtils.MSG_INIT);
        httpPost.setHeader(HttpUtils.HDRS_MODE, HttpUtils.MSG_LIVE);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_DIGEST, DigestMethod.SHA256);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_XML_COMPRESSION, HttpUtils.SUPPORTED_XML_COMPRESSIONS[0]);
        httpPost.setHeader(HttpUtils.HDRS_DATA_CLASS, HttpUtils.LOG);
        httpPost.setHeader(HttpUtils.HDRS_VERSION, HttpUtils.SUPPORTED_VERSIONS[0]);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_CONFIGURE_DIGEST_CHALLENGE, HttpUtils.MSG_CONFIGURE_DIGEST_ON);


        HttpClient client = HttpClientBuilder.create().build();

        final HttpResponse response = client.execute(httpPost);
        final String responseMessage = response.getFirstHeader(HttpUtils.HDRS_MESSAGE).getValue();
        final int responseStatus = response.getStatusLine().getStatusCode();
        assertEquals(200, responseStatus);
        assertEquals("initialize-ack", responseMessage);
    }

    @Test
    public void testNullDigestReturnsInitializeAck() throws ClientProtocolException, IOException {

        UUID publisherUUID = UUID.randomUUID();
        final String publisherId = publisherUUID.toString();
        final HttpPost httpPost = new HttpPost("http://localhost:" + HTTP_PORT + HttpUtils.JOURNAL_ENDPOINT);
        httpPost.setHeader(HttpUtils.HDRS_CONTENT_TYPE, HttpUtils.DEFAULT_CONTENT_TYPE);
        httpPost.setHeader(HttpUtils.HDRS_PUBLISHER_ID, publisherId);
        httpPost.setHeader(HttpUtils.HDRS_MESSAGE, HttpUtils.MSG_INIT);
        httpPost.setHeader(HttpUtils.HDRS_MODE, HttpUtils.MSG_LIVE);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_XML_COMPRESSION, HttpUtils.SUPPORTED_XML_COMPRESSIONS[0]);
        httpPost.setHeader(HttpUtils.HDRS_DATA_CLASS, HttpUtils.JOURNAL);
        httpPost.setHeader(HttpUtils.HDRS_VERSION, HttpUtils.SUPPORTED_VERSIONS[0]);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_CONFIGURE_DIGEST_CHALLENGE, HttpUtils.MSG_CONFIGURE_DIGEST_ON);

        HttpClient client = HttpClientBuilder.create().build();

        final HttpResponse response = client.execute(httpPost);
        final String responseMessage = response.getFirstHeader(HttpUtils.HDRS_MESSAGE).getValue();
        final int responseStatus = response.getStatusLine().getStatusCode();
        assertEquals(200, responseStatus);
        assertEquals("initialize-ack", responseMessage);
    }

    @Test
    public void testEmptyDigestReturnsInitializeAck() throws ClientProtocolException, IOException {

        UUID publisherUUID = UUID.randomUUID();
        final String publisherId = publisherUUID.toString();
        final HttpPost httpPost = new HttpPost("http://localhost:" + HTTP_PORT + HttpUtils.JOURNAL_ENDPOINT);
        httpPost.setHeader(HttpUtils.HDRS_CONTENT_TYPE, HttpUtils.DEFAULT_CONTENT_TYPE);
        httpPost.setHeader(HttpUtils.HDRS_PUBLISHER_ID, publisherId);
        httpPost.setHeader(HttpUtils.HDRS_MESSAGE, HttpUtils.MSG_INIT);
        httpPost.setHeader(HttpUtils.HDRS_MODE, HttpUtils.MSG_LIVE);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_DIGEST, "");
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_XML_COMPRESSION, HttpUtils.SUPPORTED_XML_COMPRESSIONS[0]);
        httpPost.setHeader(HttpUtils.HDRS_DATA_CLASS, HttpUtils.JOURNAL);
        httpPost.setHeader(HttpUtils.HDRS_VERSION, HttpUtils.SUPPORTED_VERSIONS[0]);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_CONFIGURE_DIGEST_CHALLENGE, HttpUtils.MSG_CONFIGURE_DIGEST_ON);

        HttpClient client = HttpClientBuilder.create().build();

        final HttpResponse response = client.execute(httpPost);
        final String responseMessage = response.getFirstHeader(HttpUtils.HDRS_MESSAGE).getValue();
        final int responseStatus = response.getStatusLine().getStatusCode();
        assertEquals(200, responseStatus);
        assertEquals("initialize-ack", responseMessage);
    }

    @Test
    public void testNullXmlCompressionReturnsInitializeAck() throws ClientProtocolException, IOException {

        UUID publisherUUID = UUID.randomUUID();
        final String publisherId = publisherUUID.toString();
        final HttpPost httpPost = new HttpPost("http://localhost:" + HTTP_PORT + HttpUtils.JOURNAL_ENDPOINT);
        httpPost.setHeader(HttpUtils.HDRS_CONTENT_TYPE, HttpUtils.DEFAULT_CONTENT_TYPE);
        httpPost.setHeader(HttpUtils.HDRS_PUBLISHER_ID, publisherId);
        httpPost.setHeader(HttpUtils.HDRS_MESSAGE, HttpUtils.MSG_INIT);
        httpPost.setHeader(HttpUtils.HDRS_MODE, HttpUtils.MSG_LIVE);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_DIGEST, DigestMethod.SHA256);
        httpPost.setHeader(HttpUtils.HDRS_DATA_CLASS, HttpUtils.JOURNAL);
        httpPost.setHeader(HttpUtils.HDRS_VERSION, HttpUtils.SUPPORTED_VERSIONS[0]);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_CONFIGURE_DIGEST_CHALLENGE, HttpUtils.MSG_CONFIGURE_DIGEST_ON);

        HttpClient client = HttpClientBuilder.create().build();

        final HttpResponse response = client.execute(httpPost);
        final String responseMessage = response.getFirstHeader(HttpUtils.HDRS_MESSAGE).getValue();
        final int responseStatus = response.getStatusLine().getStatusCode();
        assertEquals(200, responseStatus);
        assertEquals("initialize-ack", responseMessage);
    }

    @Test
    public void testEmptyXmlCompressionReturnsInitializeAck() throws ClientProtocolException, IOException {

        UUID publisherUUID = UUID.randomUUID();
        final String publisherId = publisherUUID.toString();
        final HttpPost httpPost = new HttpPost("http://localhost:" + HTTP_PORT + HttpUtils.JOURNAL_ENDPOINT);
        httpPost.setHeader(HttpUtils.HDRS_CONTENT_TYPE, HttpUtils.DEFAULT_CONTENT_TYPE);
        httpPost.setHeader(HttpUtils.HDRS_PUBLISHER_ID, publisherId);
        httpPost.setHeader(HttpUtils.HDRS_MESSAGE, HttpUtils.MSG_INIT);
        httpPost.setHeader(HttpUtils.HDRS_MODE, HttpUtils.MSG_LIVE);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_DIGEST, DigestMethod.SHA256);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_XML_COMPRESSION, "");
        httpPost.setHeader(HttpUtils.HDRS_DATA_CLASS, HttpUtils.JOURNAL);
        httpPost.setHeader(HttpUtils.HDRS_VERSION, HttpUtils.SUPPORTED_VERSIONS[0]);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_CONFIGURE_DIGEST_CHALLENGE, HttpUtils.MSG_CONFIGURE_DIGEST_ON);

        HttpClient client = HttpClientBuilder.create().build();

        final HttpResponse response = client.execute(httpPost);
        final String responseMessage = response.getFirstHeader(HttpUtils.HDRS_MESSAGE).getValue();
        final int responseStatus = response.getStatusLine().getStatusCode();
        assertEquals(200, responseStatus);
        assertEquals("initialize-ack", responseMessage);
    }

    @Test
    public void testNullConfigureDigestChallengeReturnsInitializeAck() throws ClientProtocolException, IOException {

        UUID publisherUUID = UUID.randomUUID();
        final String publisherId = publisherUUID.toString();
        final HttpPost httpPost = new HttpPost("http://localhost:" + HTTP_PORT + HttpUtils.JOURNAL_ENDPOINT);
        httpPost.setHeader(HttpUtils.HDRS_CONTENT_TYPE, HttpUtils.DEFAULT_CONTENT_TYPE);
        httpPost.setHeader(HttpUtils.HDRS_PUBLISHER_ID, publisherId);
        httpPost.setHeader(HttpUtils.HDRS_MESSAGE, HttpUtils.MSG_INIT);
        httpPost.setHeader(HttpUtils.HDRS_MODE, HttpUtils.MSG_LIVE);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_DIGEST, DigestMethod.SHA256);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_XML_COMPRESSION, HttpUtils.SUPPORTED_XML_COMPRESSIONS[0]);
        httpPost.setHeader(HttpUtils.HDRS_DATA_CLASS, HttpUtils.JOURNAL);
        httpPost.setHeader(HttpUtils.HDRS_VERSION, HttpUtils.SUPPORTED_VERSIONS[0]);

        HttpClient client = HttpClientBuilder.create().build();

        final HttpResponse response = client.execute(httpPost);
        final String responseMessage = response.getFirstHeader(HttpUtils.HDRS_MESSAGE).getValue();
        final int responseStatus = response.getStatusLine().getStatusCode();
        assertEquals(200, responseStatus);
        assertEquals("initialize-ack", responseMessage);
    }

    @Test
    public void testEmptyConfigureDigestChallengeReturnsInitializeAck() throws ClientProtocolException, IOException {

        UUID publisherUUID = UUID.randomUUID();
        final String publisherId = publisherUUID.toString();
        final HttpPost httpPost = new HttpPost("http://localhost:" + HTTP_PORT + HttpUtils.JOURNAL_ENDPOINT);
        httpPost.setHeader(HttpUtils.HDRS_CONTENT_TYPE, HttpUtils.DEFAULT_CONTENT_TYPE);
        httpPost.setHeader(HttpUtils.HDRS_PUBLISHER_ID, publisherId);
        httpPost.setHeader(HttpUtils.HDRS_MESSAGE, HttpUtils.MSG_INIT);
        httpPost.setHeader(HttpUtils.HDRS_MODE, HttpUtils.MSG_LIVE);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_DIGEST, DigestMethod.SHA256);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_XML_COMPRESSION, HttpUtils.SUPPORTED_XML_COMPRESSIONS[0]);
        httpPost.setHeader(HttpUtils.HDRS_DATA_CLASS, HttpUtils.JOURNAL);
        httpPost.setHeader(HttpUtils.HDRS_VERSION, HttpUtils.SUPPORTED_VERSIONS[0]);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_CONFIGURE_DIGEST_CHALLENGE, "");

        HttpClient client = HttpClientBuilder.create().build();

        final HttpResponse response = client.execute(httpPost);
        final String responseMessage = response.getFirstHeader(HttpUtils.HDRS_MESSAGE).getValue();
        final int responseStatus = response.getStatusLine().getStatusCode();
        assertEquals(200, responseStatus);
        assertEquals("initialize-ack", responseMessage);
    }

    @Test
    public void testInitializeWithListsReturnsInitializeAck() throws ClientProtocolException, IOException {

        UUID publisherUUID = UUID.randomUUID();
        final String publisherId = publisherUUID.toString();
        final HttpPost httpPost = new HttpPost("http://localhost:" + HTTP_PORT + HttpUtils.JOURNAL_ENDPOINT);
        httpPost.setHeader(HttpUtils.HDRS_CONTENT_TYPE, HttpUtils.DEFAULT_CONTENT_TYPE);
        httpPost.setHeader(HttpUtils.HDRS_PUBLISHER_ID, publisherId);
        httpPost.setHeader(HttpUtils.HDRS_MESSAGE, HttpUtils.MSG_INIT);
        httpPost.setHeader(HttpUtils.HDRS_MODE, HttpUtils.MSG_LIVE);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_DIGEST, DigestMethod.SHA256);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_XML_COMPRESSION, String.join(",", HttpUtils.SUPPORTED_XML_COMPRESSIONS[0]));
        httpPost.setHeader(HttpUtils.HDRS_DATA_CLASS, HttpUtils.JOURNAL);
        httpPost.setHeader(HttpUtils.HDRS_VERSION, HttpUtils.SUPPORTED_VERSIONS[0]);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_CONFIGURE_DIGEST_CHALLENGE, String.join(",", HttpUtils.MSG_CONFIGURE_DIGEST_ON));

        HttpClient client = HttpClientBuilder.create().build();

        final HttpResponse response = client.execute(httpPost);
        final String responseMessage = response.getFirstHeader(HttpUtils.HDRS_MESSAGE).getValue();
        final int responseStatus = response.getStatusLine().getStatusCode();
        assertEquals(200, responseStatus);
        assertEquals("initialize-ack", responseMessage);
    }

    @Test
    public void testInvalidPublisherIdReturnsInitializeNack() throws ClientProtocolException, IOException {

        final HttpPost httpPost = new HttpPost("http://localhost:" + HTTP_PORT + HttpUtils.JOURNAL_ENDPOINT);
        httpPost.setHeader(HttpUtils.HDRS_CONTENT_TYPE, HttpUtils.DEFAULT_CONTENT_TYPE);
        httpPost.setHeader(HttpUtils.HDRS_PUBLISHER_ID, "");
        httpPost.setHeader(HttpUtils.HDRS_MESSAGE, HttpUtils.MSG_INIT);
        httpPost.setHeader(HttpUtils.HDRS_MODE, HttpUtils.MSG_LIVE);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_DIGEST, DigestMethod.SHA256);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_XML_COMPRESSION, String.join(",", HttpUtils.SUPPORTED_XML_COMPRESSIONS[0]));
        httpPost.setHeader(HttpUtils.HDRS_DATA_CLASS, HttpUtils.JOURNAL);
        httpPost.setHeader(HttpUtils.HDRS_VERSION, HttpUtils.SUPPORTED_VERSIONS[0]);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_CONFIGURE_DIGEST_CHALLENGE, String.join(",", HttpUtils.MSG_CONFIGURE_DIGEST_ON));

        HttpClient client = HttpClientBuilder.create().build();

        final HttpResponse response = client.execute(httpPost);
        final String responseMessage = response.getFirstHeader(HttpUtils.HDRS_MESSAGE).getValue();
        final int responseStatus = response.getStatusLine().getStatusCode();
        assertEquals(200, responseStatus);
        assertEquals("initialize-nack", responseMessage);
    }

    @Test
    public void testDuplicatePublisherReturnsInitializeNack() throws ClientProtocolException, IOException {

        UUID publisherUUID = UUID.randomUUID();
        final String publisherId = publisherUUID.toString();
        final HttpPost httpPost = new HttpPost("http://localhost:" + HTTP_PORT + HttpUtils.AUDIT_ENDPOINT);
        httpPost.setHeader(HttpUtils.HDRS_CONTENT_TYPE, HttpUtils.DEFAULT_CONTENT_TYPE);
        httpPost.setHeader(HttpUtils.HDRS_PUBLISHER_ID, publisherId);
        httpPost.setHeader(HttpUtils.HDRS_MESSAGE, HttpUtils.MSG_INIT);
        httpPost.setHeader(HttpUtils.HDRS_MODE, HttpUtils.MSG_LIVE);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_DIGEST, DigestMethod.SHA256);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_XML_COMPRESSION, HttpUtils.SUPPORTED_XML_COMPRESSIONS[0]);
        httpPost.setHeader(HttpUtils.HDRS_DATA_CLASS, HttpUtils.AUDIT);
        httpPost.setHeader(HttpUtils.HDRS_VERSION, HttpUtils.SUPPORTED_VERSIONS[0]);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_CONFIGURE_DIGEST_CHALLENGE, HttpUtils.MSG_CONFIGURE_DIGEST_ON);

        HttpClient client = HttpClientBuilder.create().build();

        final HttpResponse response = client.execute(httpPost);
        final String responseMessage = response.getFirstHeader(HttpUtils.HDRS_MESSAGE).getValue();
        final int responseStatus = response.getStatusLine().getStatusCode();
        assertEquals(200, responseStatus);
        assertEquals("initialize-ack", responseMessage);

        final HttpResponse nextResponse = client.execute(httpPost);
        final String nextResponseMessage = nextResponse.getFirstHeader(HttpUtils.HDRS_MESSAGE).getValue();
        final int nextResponseStatus = nextResponse.getStatusLine().getStatusCode();
        assertEquals(200, nextResponseStatus);
        assertEquals("initialize-nack", nextResponseMessage);
    }

    @Test
    public void testInvalidModeReturnsInitializeNack() throws ClientProtocolException, IOException {

        UUID publisherUUID = UUID.randomUUID();
        final String publisherId = publisherUUID.toString();
        final HttpPost httpPost = new HttpPost("http://localhost:" + HTTP_PORT + HttpUtils.JOURNAL_ENDPOINT);
        httpPost.setHeader(HttpUtils.HDRS_CONTENT_TYPE, HttpUtils.DEFAULT_CONTENT_TYPE);
        httpPost.setHeader(HttpUtils.HDRS_PUBLISHER_ID, publisherId);
        httpPost.setHeader(HttpUtils.HDRS_MESSAGE, HttpUtils.MSG_INIT);
        httpPost.setHeader(HttpUtils.HDRS_MODE, "invalid");
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_DIGEST, DigestMethod.SHA256);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_XML_COMPRESSION, HttpUtils.SUPPORTED_XML_COMPRESSIONS[0]);
        httpPost.setHeader(HttpUtils.HDRS_DATA_CLASS, HttpUtils.JOURNAL);
        httpPost.setHeader(HttpUtils.HDRS_VERSION, HttpUtils.SUPPORTED_VERSIONS[0]);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_CONFIGURE_DIGEST_CHALLENGE, HttpUtils.MSG_CONFIGURE_DIGEST_ON);

        HttpClient client = HttpClientBuilder.create().build();

        final HttpResponse response = client.execute(httpPost);
        final String responseMessage = response.getFirstHeader(HttpUtils.HDRS_MESSAGE).getValue();
        final int responseStatus = response.getStatusLine().getStatusCode();
        assertEquals(200, responseStatus);
        assertEquals("initialize-nack", responseMessage);
    }

    @Test
    public void testInvalidDigestReturnsInitializeNack() throws ClientProtocolException, IOException {

        UUID publisherUUID = UUID.randomUUID();
        final String publisherId = publisherUUID.toString();
        final HttpPost httpPost = new HttpPost("http://localhost:" + HTTP_PORT + HttpUtils.JOURNAL_ENDPOINT);
        httpPost.setHeader(HttpUtils.HDRS_CONTENT_TYPE, HttpUtils.DEFAULT_CONTENT_TYPE);
        httpPost.setHeader(HttpUtils.HDRS_PUBLISHER_ID, publisherId);
        httpPost.setHeader(HttpUtils.HDRS_MESSAGE, HttpUtils.MSG_INIT);
        httpPost.setHeader(HttpUtils.HDRS_MODE, HttpUtils.MSG_LIVE);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_DIGEST, "invalid");
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_XML_COMPRESSION, HttpUtils.SUPPORTED_XML_COMPRESSIONS[0]);
        httpPost.setHeader(HttpUtils.HDRS_DATA_CLASS, HttpUtils.JOURNAL);
        httpPost.setHeader(HttpUtils.HDRS_VERSION, HttpUtils.SUPPORTED_VERSIONS[0]);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_CONFIGURE_DIGEST_CHALLENGE, HttpUtils.MSG_CONFIGURE_DIGEST_ON);

        HttpClient client = HttpClientBuilder.create().build();

        final HttpResponse response = client.execute(httpPost);
        final String responseMessage = response.getFirstHeader(HttpUtils.HDRS_MESSAGE).getValue();
        final int responseStatus = response.getStatusLine().getStatusCode();
        assertEquals(200, responseStatus);
        assertEquals("initialize-nack", responseMessage);
    }

    @Test
    public void testInvalidXmlCompressionReturnsInitializeNack() throws ClientProtocolException, IOException {

        UUID publisherUUID = UUID.randomUUID();
        final String publisherId = publisherUUID.toString();
        final HttpPost httpPost = new HttpPost("http://localhost:" + HTTP_PORT + HttpUtils.JOURNAL_ENDPOINT);
        httpPost.setHeader(HttpUtils.HDRS_CONTENT_TYPE, HttpUtils.DEFAULT_CONTENT_TYPE);
        httpPost.setHeader(HttpUtils.HDRS_PUBLISHER_ID, publisherId);
        httpPost.setHeader(HttpUtils.HDRS_MESSAGE, HttpUtils.MSG_INIT);
        httpPost.setHeader(HttpUtils.HDRS_MODE, HttpUtils.MSG_LIVE);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_DIGEST, DigestMethod.SHA256);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_XML_COMPRESSION, "invalid");
        httpPost.setHeader(HttpUtils.HDRS_DATA_CLASS, HttpUtils.JOURNAL);
        httpPost.setHeader(HttpUtils.HDRS_VERSION, HttpUtils.SUPPORTED_VERSIONS[0]);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_CONFIGURE_DIGEST_CHALLENGE, HttpUtils.MSG_CONFIGURE_DIGEST_ON);

        HttpClient client = HttpClientBuilder.create().build();

        final HttpResponse response = client.execute(httpPost);
        final String responseMessage = response.getFirstHeader(HttpUtils.HDRS_MESSAGE).getValue();
        final int responseStatus = response.getStatusLine().getStatusCode();
        assertEquals(200, responseStatus);
        assertEquals("initialize-nack", responseMessage);
    }

    @Test
    public void testInvalidDataClassReturnsInitializeNack() throws ClientProtocolException, IOException {

        UUID publisherUUID = UUID.randomUUID();
        final String publisherId = publisherUUID.toString();
        final HttpPost httpPost = new HttpPost("http://localhost:" + HTTP_PORT + HttpUtils.JOURNAL_ENDPOINT);
        httpPost.setHeader(HttpUtils.HDRS_CONTENT_TYPE, HttpUtils.DEFAULT_CONTENT_TYPE);
        httpPost.setHeader(HttpUtils.HDRS_PUBLISHER_ID, publisherId);
        httpPost.setHeader(HttpUtils.HDRS_MESSAGE, HttpUtils.MSG_INIT);
        httpPost.setHeader(HttpUtils.HDRS_MODE, HttpUtils.MSG_LIVE);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_DIGEST, DigestMethod.SHA256);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_XML_COMPRESSION, HttpUtils.SUPPORTED_XML_COMPRESSIONS[0]);
        httpPost.setHeader(HttpUtils.HDRS_DATA_CLASS, "invalid");
        httpPost.setHeader(HttpUtils.HDRS_VERSION, HttpUtils.SUPPORTED_VERSIONS[0]);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_CONFIGURE_DIGEST_CHALLENGE, HttpUtils.MSG_CONFIGURE_DIGEST_ON);

        HttpClient client = HttpClientBuilder.create().build();

        final HttpResponse response = client.execute(httpPost);
        final String responseMessage = response.getFirstHeader(HttpUtils.HDRS_MESSAGE).getValue();
        final int responseStatus = response.getStatusLine().getStatusCode();
        assertEquals(200, responseStatus);
        assertEquals("initialize-nack", responseMessage);
    }

    @Test
    public void testInvalidVersionReturnsInitializeNack() throws ClientProtocolException, IOException {

        UUID publisherUUID = UUID.randomUUID();
        final String publisherId = publisherUUID.toString();
        final HttpPost httpPost = new HttpPost("http://localhost:" + HTTP_PORT + HttpUtils.JOURNAL_ENDPOINT);
        httpPost.setHeader(HttpUtils.HDRS_CONTENT_TYPE, HttpUtils.DEFAULT_CONTENT_TYPE);
        httpPost.setHeader(HttpUtils.HDRS_PUBLISHER_ID, publisherId);
        httpPost.setHeader(HttpUtils.HDRS_MESSAGE, HttpUtils.MSG_INIT);
        httpPost.setHeader(HttpUtils.HDRS_MODE, HttpUtils.MSG_LIVE);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_DIGEST, DigestMethod.SHA256);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_XML_COMPRESSION, HttpUtils.SUPPORTED_XML_COMPRESSIONS[0]);
        httpPost.setHeader(HttpUtils.HDRS_DATA_CLASS, HttpUtils.JOURNAL);
        httpPost.setHeader(HttpUtils.HDRS_VERSION,"invalid");
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_CONFIGURE_DIGEST_CHALLENGE, HttpUtils.MSG_CONFIGURE_DIGEST_ON);

        HttpClient client = HttpClientBuilder.create().build();

        final HttpResponse response = client.execute(httpPost);
        final String responseMessage = response.getFirstHeader(HttpUtils.HDRS_MESSAGE).getValue();
        final int responseStatus = response.getStatusLine().getStatusCode();
        assertEquals(200, responseStatus);
        assertEquals("initialize-nack", responseMessage);
    }

    @Test
    public void testInvalidConfigureDigestChallengeReturnsInitializeNack() throws ClientProtocolException, IOException {

        UUID publisherUUID = UUID.randomUUID();
        final String publisherId = publisherUUID.toString();
        final HttpPost httpPost = new HttpPost("http://localhost:" + HTTP_PORT + HttpUtils.JOURNAL_ENDPOINT);
        httpPost.setHeader(HttpUtils.HDRS_CONTENT_TYPE, HttpUtils.DEFAULT_CONTENT_TYPE);
        httpPost.setHeader(HttpUtils.HDRS_PUBLISHER_ID, publisherId);
        httpPost.setHeader(HttpUtils.HDRS_MESSAGE, HttpUtils.MSG_INIT);
        httpPost.setHeader(HttpUtils.HDRS_MODE, HttpUtils.MSG_LIVE);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_DIGEST, DigestMethod.SHA256);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_XML_COMPRESSION, HttpUtils.SUPPORTED_XML_COMPRESSIONS[0]);
        httpPost.setHeader(HttpUtils.HDRS_DATA_CLASS, HttpUtils.JOURNAL);
        httpPost.setHeader(HttpUtils.HDRS_VERSION, HttpUtils.SUPPORTED_VERSIONS[0]);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_CONFIGURE_DIGEST_CHALLENGE, "invalid");

        HttpClient client = HttpClientBuilder.create().build();

        final HttpResponse response = client.execute(httpPost);
        final String responseMessage = response.getFirstHeader(HttpUtils.HDRS_MESSAGE).getValue();
        final int responseStatus = response.getStatusLine().getStatusCode();
        assertEquals(200, responseStatus);
        assertEquals("initialize-nack", responseMessage);
    }

    @Test
    public void testProcessInitializeMessageNullRequestHeaders() throws IOException
    {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("requestHeaders is required");
        HashMap<String, String> successResponseHeaders = new HashMap<String, String>();
        List<String> errorMessages = new ArrayList<String>();
        boolean result = MessageProcessor.processInitializeMessage(null, RecordType.Audit, successResponseHeaders, errorMessages);
        assertEquals(false, result);
    }

    @Test
    public void testProcessInitializeMessageNullSuccessResponseHeaders() throws IOException
    {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("successResponseHeaders is required");
        HashMap<String, String> requestHeaders = new HashMap<String, String>();
        List<String> errorMessages = new ArrayList<String>();
        boolean result = MessageProcessor.processInitializeMessage(requestHeaders, RecordType.Audit, null, errorMessages);
        assertEquals(false, result);
    }

    @Test
    public void testProcessInitializeMessageNullErrorMessages() throws IOException
    {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("errorMessages is required");
        HashMap<String, String> successResponseHeaders = new HashMap<String, String>();
        HashMap<String, String> requestHeaders = new HashMap<String, String>();
        boolean result = MessageProcessor.processInitializeMessage(requestHeaders, RecordType.Audit, successResponseHeaders, null);
        assertEquals(false, result);
    }

    @Test
    public void testProcessInitializeMessageNullSupportedRecType() throws IOException
    {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("supportedRecType is required");
        HashMap<String, String> successResponseHeaders = new HashMap<String, String>();
        HashMap<String, String> requestHeaders = new HashMap<String, String>();
        List<String> errorMessages = new ArrayList<String>();
        boolean result = MessageProcessor.processInitializeMessage(requestHeaders, null, successResponseHeaders, errorMessages);
        assertEquals(false, result);
    }

    @Test
    public void testProcessJALRecordMessageNullDigestResultParam()
    {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("digestResult is required");
        byte[] test = new byte[10];
        boolean result = MessageProcessor.processJALRecordMessage(new HashMap<String, String>(), new ByteArrayInputStream(test), RecordType.Audit, null, new ArrayList<String>());
        assertEquals(false, result);
    }

    @Test
    public void testProcessJALRecordMessageNullErrorMessagesParam()
    {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("errorMessages is required");
        byte[] test = new byte[10];
        boolean result = MessageProcessor.processJALRecordMessage(new HashMap<String, String>(), new ByteArrayInputStream(test), RecordType.Audit, new DigestResult(), null);
        assertEquals(false, result);
    }

    @Test
    public void testProcessJALRecordMessageNullRequestHeadersParam()
    {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("requestHeaders is required");
        byte[] test = new byte[10];
        boolean result = MessageProcessor.processJALRecordMessage(null, new ByteArrayInputStream(test), RecordType.Audit, new DigestResult(), new ArrayList<String>());
        assertEquals(false, result);
    }

    @Test
    public void testProcessJALRecordMessageNullRequestInputStreamParam()
    {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("requestInputStream is required");
        boolean result = MessageProcessor.processJALRecordMessage(new HashMap<String, String>(), null, RecordType.Audit, new DigestResult(), new ArrayList<String>());
        assertEquals(false, result);
    }

    @Test
    public void testProcessJALRecordMessageNullSupportedRecordTypeParam()
    {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("supportedRecType is required");
        byte[] test = new byte[10];
        boolean result = MessageProcessor.processJALRecordMessage(new HashMap<String, String>(), new ByteArrayInputStream(test), null, new DigestResult(), new ArrayList<String>());
        assertEquals(false, result);
    }

    @Test
    public void testProcessJALRecordMessageInvalidSessionId() throws ClientProtocolException, IOException {
        String [] testValues = new String[] {null, "", "junk"};

        for (RecordType recType : RecordType.values())
        {
            if (recType.equals(RecordType.Unset))
            {
                continue;
            }

            HttpPost httpPost = new HttpPost("http://localhost:" + HTTP_PORT + "/" + recType.toString().toLowerCase());
            for (String testValue : testValues)
            {
                String payLoadLengthHeader = "JAL-" + recType.toString() + "-Length";
                String jalMessage = recType.toString().toLowerCase() +  "-record";

                HashMap<String, String> headers = getJalRecordHeaders(testValue, "jalId", "0", "0", "0", payLoadLengthHeader, jalMessage);

                for (Map.Entry<String, String> entry : headers.entrySet())
                {
                    httpPost.setHeader(entry.getKey(), entry.getValue());
                }

                HttpClient client = HttpClientBuilder.create().build();

                final HttpResponse response = client.execute(httpPost);
                final String responseMessage = response.getFirstHeader(HttpUtils.HDRS_ERROR_MESSAGE).getValue();
                final int responseStatus = response.getStatusLine().getStatusCode();
                assertEquals(200, responseStatus);
                assertEquals(HttpUtils.HDRS_UNSUPPORTED_SESSION_ID, responseMessage);
            }
        }
    }

    @Test
    public void testProcessJALRecordMessageInvalidSystemMetadataLen() throws ClientProtocolException, IOException {
        String [] testValues = new String[] {null, "", "junk", "-1", "0"};

        for (RecordType recType : RecordType.values())
        {
            if (recType.equals(RecordType.Unset))
            {
                continue;
            }

            HttpPost httpPost = new HttpPost("http://localhost:" + HTTP_PORT + "/" + recType.toString().toLowerCase());
            for (String testValue : testValues)
            {

                String payLoadLengthHeader = "JAL-" + recType.toString() + "-Length";
                String jalMessage = recType.toString().toLowerCase() +  "-record";

                HashMap<String, String> headers = getJalRecordHeaders(SESSION_ID, "jalId", testValue, "0", "0", payLoadLengthHeader, jalMessage);

                for (Map.Entry<String, String> entry : headers.entrySet())
                {
                    httpPost.setHeader(entry.getKey(), entry.getValue());
                }

                HttpClient client = HttpClientBuilder.create().build();

                final HttpResponse response = client.execute(httpPost);
                final String responseMessage = response.getFirstHeader(HttpUtils.HDRS_ERROR_MESSAGE).getValue();
                final int responseStatus = response.getStatusLine().getStatusCode();
                assertEquals(200, responseStatus);
                assertEquals(HttpUtils.HDRS_INVALID_SYS_META_LEN, responseMessage);
            }
        }
    }

    @Test
    public void testProcessJALRecordMessageInvalidAppMetadataLen() throws ClientProtocolException, IOException {
        String [] testValues = new String[] {null, "", "junk", "-1"};

        for (RecordType recType : RecordType.values())
        {
            if (recType.equals(RecordType.Unset))
            {
                continue;
            }

            HttpPost httpPost = new HttpPost("http://localhost:" + HTTP_PORT + "/" + recType.toString().toLowerCase());
            for (String testValue : testValues)
            {

                String payLoadLengthHeader = "JAL-" + recType.toString() + "-Length";
                String jalMessage = recType.toString().toLowerCase() +  "-record";

                HashMap<String, String> headers = getJalRecordHeaders(SESSION_ID, "jalId", "1", testValue, "0", payLoadLengthHeader, jalMessage);

                for (Map.Entry<String, String> entry : headers.entrySet())
                {
                    httpPost.setHeader(entry.getKey(), entry.getValue());
                }

                HttpClient client = HttpClientBuilder.create().build();

                final HttpResponse response = client.execute(httpPost);
                final String responseMessage = response.getFirstHeader(HttpUtils.HDRS_ERROR_MESSAGE).getValue();
                final int responseStatus = response.getStatusLine().getStatusCode();
                assertEquals(200, responseStatus);
                assertEquals(HttpUtils.HDRS_INVALID_APP_META_LEN, responseMessage);
            }
        }
    }

    @Test
    public void testProcessJALRecordMessageInvalidPayLoadLen() throws ClientProtocolException, IOException {
        String [] testValues = new String[] {null, "", "junk", "-1"};

        for (RecordType recType : RecordType.values())
        {
            if (recType.equals(RecordType.Unset))
            {
                continue;
            }

            HttpPost httpPost = new HttpPost("http://localhost:" + HTTP_PORT + "/" + recType.toString().toLowerCase());
            for (String testValue : testValues)
            {

                String payLoadLengthHeader = "JAL-" + recType.toString() + "-Length";
                String jalMessage = recType.toString().toLowerCase() +  "-record";

                HashMap<String, String> headers = getJalRecordHeaders(SESSION_ID, "jalId", "1", "0", testValue, payLoadLengthHeader, jalMessage);

                for (Map.Entry<String, String> entry : headers.entrySet())
                {
                    httpPost.setHeader(entry.getKey(), entry.getValue());
                }

                HttpClient client = HttpClientBuilder.create().build();

                final HttpResponse response = client.execute(httpPost);
                final String responseMessage = response.getFirstHeader(HttpUtils.HDRS_ERROR_MESSAGE).getValue();
                final int responseStatus = response.getStatusLine().getStatusCode();
                assertEquals(200, responseStatus);
                assertEquals("JAL-Invalid-" + recType.toString() + "-Length", responseMessage);
            }
        }
    }

    @Test
    public void testProcessJALRecordMessageInvalidJalMessageAudit() throws ClientProtocolException, IOException {

        HttpPost httpPost = new HttpPost("http://localhost:" + HTTP_PORT + HttpUtils.AUDIT_ENDPOINT);

        HashMap<String, String> headers = getJalRecordHeaders(SESSION_ID, "jalId", "1", "0", "1", HttpUtils.HDRS_JOURNAL_LEN, HttpUtils.MSG_AUDIT);

        for (Map.Entry<String, String> entry : headers.entrySet())
        {
            httpPost.setHeader(entry.getKey(), entry.getValue());
        }

        HttpClient client = HttpClientBuilder.create().build();

        final HttpResponse response = client.execute(httpPost);
        final String responseMessage = response.getFirstHeader(HttpUtils.HDRS_ERROR_MESSAGE).getValue();
        final int responseStatus = response.getStatusLine().getStatusCode();
        assertEquals(200, responseStatus);
        assertEquals(HttpUtils.HDRS_INVALID_AUDIT_LEN, responseMessage);
    }

    @Test
    public void testProcessJALRecordMessageInvalidJalMessageJournal() throws ClientProtocolException, IOException {

        HttpPost httpPost = new HttpPost("http://localhost:" + HTTP_PORT + HttpUtils.JOURNAL_ENDPOINT);

        HashMap<String, String> headers = getJalRecordHeaders(SESSION_ID, "jalId", "1", "0", "1", HttpUtils.HDRS_AUDIT_LEN, HttpUtils.MSG_JOURNAL);

        for (Map.Entry<String, String> entry : headers.entrySet())
        {
            httpPost.setHeader(entry.getKey(), entry.getValue());
        }

        HttpClient client = HttpClientBuilder.create().build();

        final HttpResponse response = client.execute(httpPost);
        final String responseMessage = response.getFirstHeader(HttpUtils.HDRS_ERROR_MESSAGE).getValue();
        final int responseStatus = response.getStatusLine().getStatusCode();
        assertEquals(200, responseStatus);
        assertEquals(HttpUtils.HDRS_INVALID_JOURNAL_LEN, responseMessage);
    }

    @Test
    public void testProcessJALRecordMessageInvalidJalMessageLog() throws ClientProtocolException, IOException {

        HttpPost httpPost = new HttpPost("http://localhost:" + HTTP_PORT + HttpUtils.LOG_ENDPOINT);

        HashMap<String, String> headers = getJalRecordHeaders(SESSION_ID, "jalId", "1", "0", "1", HttpUtils.HDRS_JOURNAL_LEN, HttpUtils.MSG_LOG);

        for (Map.Entry<String, String> entry : headers.entrySet())
        {
            httpPost.setHeader(entry.getKey(), entry.getValue());
        }

        HttpClient client = HttpClientBuilder.create().build();

        final HttpResponse response = client.execute(httpPost);
        final String responseMessage = response.getFirstHeader(HttpUtils.HDRS_ERROR_MESSAGE).getValue();
        final int responseStatus = response.getStatusLine().getStatusCode();
        assertEquals(200, responseStatus);
        assertEquals(HttpUtils.HDRS_INVALID_LOG_LEN, responseMessage);
    }

    @Test
    public void testProcessJALRecordMessageUnsupportedDataClassAudit() throws ClientProtocolException, IOException {

        HttpPost httpPost = new HttpPost("http://localhost:" + HTTP_PORT + HttpUtils.AUDIT_ENDPOINT);

        HashMap<String, String> headers = getJalRecordHeaders(SESSION_ID, "jalId", "1", "0", "1", HttpUtils.HDRS_AUDIT_LEN, HttpUtils.MSG_JOURNAL);

        for (Map.Entry<String, String> entry : headers.entrySet())
        {
            httpPost.setHeader(entry.getKey(), entry.getValue());
        }

        HttpClient client = HttpClientBuilder.create().build();

        final HttpResponse response = client.execute(httpPost);
        final String responseMessage = response.getFirstHeader(HttpUtils.HDRS_ERROR_MESSAGE).getValue();
        final int responseStatus = response.getStatusLine().getStatusCode();
        assertEquals(200, responseStatus);
        assertEquals(HttpUtils.HDRS_UNSUPPORTED_DATACLASS, responseMessage);
    }

    @Test
    public void testProcessJALRecordMessageUnsupportedDataClassJournal() throws ClientProtocolException, IOException {

        HttpPost httpPost = new HttpPost("http://localhost:" + HTTP_PORT + HttpUtils.JOURNAL_ENDPOINT);

        HashMap<String, String> headers = getJalRecordHeaders(SESSION_ID, "jalId", "1", "0", "1", HttpUtils.HDRS_JOURNAL_LEN, HttpUtils.MSG_AUDIT);

        for (Map.Entry<String, String> entry : headers.entrySet())
        {
            httpPost.setHeader(entry.getKey(), entry.getValue());
        }

        HttpClient client = HttpClientBuilder.create().build();

        final HttpResponse response = client.execute(httpPost);
        final String responseMessage = response.getFirstHeader(HttpUtils.HDRS_ERROR_MESSAGE).getValue();
        final int responseStatus = response.getStatusLine().getStatusCode();
        assertEquals(200, responseStatus);
        assertEquals(HttpUtils.HDRS_UNSUPPORTED_DATACLASS, responseMessage);
    }

    @Test
    public void testProcessJALRecordMessageUnsupportedDataClassLog() throws ClientProtocolException, IOException {

        HttpPost httpPost = new HttpPost("http://localhost:" + HTTP_PORT + HttpUtils.LOG_ENDPOINT);

        HashMap<String, String> headers = getJalRecordHeaders(SESSION_ID, "jalId", "1", "0", "1", HttpUtils.HDRS_LOG_LEN, HttpUtils.MSG_AUDIT);

        for (Map.Entry<String, String> entry : headers.entrySet())
        {
            httpPost.setHeader(entry.getKey(), entry.getValue());
        }

        HttpClient client = HttpClientBuilder.create().build();

        final HttpResponse response = client.execute(httpPost);
        final String responseMessage = response.getFirstHeader(HttpUtils.HDRS_ERROR_MESSAGE).getValue();
        final int responseStatus = response.getStatusLine().getStatusCode();
        assertEquals(200, responseStatus);
        assertEquals(HttpUtils.HDRS_UNSUPPORTED_DATACLASS, responseMessage);
    }

    @Test
    public void testProcessJALRecordMessageInvalidJalId() throws ClientProtocolException, IOException {
        String [] testValues = new String[] {null, ""};

        for (RecordType recType : RecordType.values())
        {
            if (recType.equals(RecordType.Unset))
            {
                continue;
            }

            HttpPost httpPost = new HttpPost("http://localhost:" + HTTP_PORT + "/" + recType.toString().toLowerCase());
            for (String testValue : testValues)
            {

                String payLoadLengthHeader = "JAL-" + recType.toString() + "-Length";
                String jalMessage = recType.toString().toLowerCase() +  "-record";

                HashMap<String, String> headers = getJalRecordHeaders(SESSION_ID, testValue, "1", "0", "1", payLoadLengthHeader, jalMessage);

                for (Map.Entry<String, String> entry : headers.entrySet())
                {
                    httpPost.setHeader(entry.getKey(), entry.getValue());
                }

                HttpClient client = HttpClientBuilder.create().build();

                final HttpResponse response = client.execute(httpPost);
                final String responseMessage = response.getFirstHeader(HttpUtils.HDRS_ERROR_MESSAGE).getValue();
                final int responseStatus = response.getStatusLine().getStatusCode();
                assertEquals(200, responseStatus);
                assertEquals(HttpUtils.HDRS_INVALID_JAL_ID, responseMessage);
            }
        }
    }

    @Test
    public void testProcessJALRecordMessageUnsupportedSessionId() throws ClientProtocolException, IOException {
        for (RecordType recType : RecordType.values())
        {
            if (recType.equals(RecordType.Unset))
            {
                continue;
            }

            HttpPost httpPost = new HttpPost("http://localhost:" + HTTP_PORT + "/" + recType.toString().toLowerCase());

            String payLoadLengthHeader = "JAL-" + recType.toString() + "-Length";
            String jalMessage = recType.toString().toLowerCase() +  "-record";

            HashMap<String, String> headers = getJalRecordHeaders(SESSION_ID, "jalId", "1", "0", "1", payLoadLengthHeader, jalMessage);

            for (Map.Entry<String, String> entry : headers.entrySet())
            {
                httpPost.setHeader(entry.getKey(), entry.getValue());
            }

            HttpClient client = HttpClientBuilder.create().build();

            final HttpResponse response = client.execute(httpPost);
            final String responseMessage = response.getFirstHeader(HttpUtils.HDRS_ERROR_MESSAGE).getValue();
            final int responseStatus = response.getStatusLine().getStatusCode();
            assertEquals(200, responseStatus);

            //Session does not exist
            assertEquals(HttpUtils.HDRS_UNSUPPORTED_SESSION_ID, responseMessage);
        }
    }

    @Test
    public void testProcessJALRecordMessageEmptyPayloadRecord() throws ClientProtocolException, IOException {
        String publisherId = UUID.randomUUID().toString();
        for (RecordType recType : RecordType.values())
        {
            if (recType.equals(RecordType.Unset))
            {
                continue;
            }

            String sessionId = sendValidInitialize(recType, true, publisherId);

            HttpPost httpPost = new HttpPost("http://localhost:" + HTTP_PORT + "/" + recType.toString().toLowerCase());

            String payLoadLengthHeader = "JAL-" + recType.toString() + "-Length";
            String jalMessage = recType.toString().toLowerCase() +  "-record";

            HashMap<String, String> headers = getJalRecordHeaders(sessionId, "jalId", "1", "0", "1", payLoadLengthHeader, jalMessage);

            for (Map.Entry<String, String> entry : headers.entrySet())
            {
                httpPost.setHeader(entry.getKey(), entry.getValue());
            }

            HttpClient client = HttpClientBuilder.create().build();

            final HttpResponse response = client.execute(httpPost);
            final String responseMessage = response.getFirstHeader(HttpUtils.HDRS_ERROR_MESSAGE).getValue();
            final int responseStatus = response.getStatusLine().getStatusCode();
            assertEquals(200, responseStatus);

            //Session does not exist
            assertEquals(HttpUtils.HDRS_RECORD_FAILED, responseMessage);
        }
    }

    @Test
    public void testProcessJALRecordMessageValidRecord() throws ClientProtocolException, IOException {
        String publisherId = UUID.randomUUID().toString();
        for (RecordType recType : RecordType.values())
        {
            if (recType.equals(RecordType.Unset))
            {
                continue;
            }

            String sessionId = sendValidInitialize(recType, true, publisherId);

            HttpPost httpPost = new HttpPost("http://localhost:" + HTTP_PORT + "/" + recType.toString().toLowerCase());

            String payLoadLengthHeader = "JAL-" + recType.toString() + "-Length";
            String jalMessage = recType.toString().toLowerCase() +  "-record";

            HashMap<String, String> headers = getJalRecordHeaders(sessionId, UUID.randomUUID().toString(), "3083", "1125", "19", payLoadLengthHeader, jalMessage);

            for (Map.Entry<String, String> entry : headers.entrySet())
            {
                httpPost.setHeader(entry.getKey(), entry.getValue());
            }

            //Adds jal record to post
            File resourcesDirectory = new File("src/test/resources");

            String jalRecord1Path = resourcesDirectory.getAbsolutePath() + "/jal_record1.txt";
            HttpEntity entity = EntityBuilder.create().setFile(new File(jalRecord1Path)).build();

            httpPost.setEntity(entity);

            HttpClient client = HttpClientBuilder.create().build();

            final HttpResponse response = client.execute(httpPost);
            final String responseMessage = response.getFirstHeader(HttpUtils.HDRS_DIGEST).getValue();
            final Header errorMessage = response.getFirstHeader(HttpUtils.HDRS_ERROR_MESSAGE);
            final int responseStatus = response.getStatusLine().getStatusCode();
            assertEquals(200, responseStatus);
            assertEquals(null, errorMessage);

            //Validate digest is correct for test file sent.
            assertEquals("bbd801ce4dc24520c028025c05b44c5532b240824d2d7ce25644b73b667b6c7a", responseMessage);
        }
    }

    @Test
    public void testProcessJALRecordMessageValidRecordDigestOff() throws ClientProtocolException, IOException {
        String publisherId = UUID.randomUUID().toString();
        for (RecordType recType : RecordType.values())
        {
            if (recType.equals(RecordType.Unset))
            {
                continue;
            }

            String sessionId = sendValidInitialize(recType, false, publisherId);

            HttpPost httpPost = new HttpPost("http://localhost:" + HTTP_PORT + "/" + recType.toString().toLowerCase());

            String payLoadLengthHeader = "JAL-" + recType.toString() + "-Length";
            String jalMessage = recType.toString().toLowerCase() +  "-record";

            HashMap<String, String> headers = getJalRecordHeaders(sessionId, UUID.randomUUID().toString(), "3083", "1125", "19", payLoadLengthHeader, jalMessage);

            for (Map.Entry<String, String> entry : headers.entrySet())
            {
                httpPost.setHeader(entry.getKey(), entry.getValue());
            }

            //Adds jal record to post
            File resourcesDirectory = new File("src/test/resources");

            String jalRecord1Path = resourcesDirectory.getAbsolutePath() + "/jal_record1.txt";
            HttpEntity entity = EntityBuilder.create().setFile(new File(jalRecord1Path)).build();

            httpPost.setEntity(entity);

            HttpClient client = HttpClientBuilder.create().build();

            final HttpResponse response = client.execute(httpPost);
            final String responseMessage = response.getFirstHeader(HttpUtils.HDRS_DIGEST).getValue();
            final Header errorMessage = response.getFirstHeader(HttpUtils.HDRS_ERROR_MESSAGE);
            final int responseStatus = response.getStatusLine().getStatusCode();
            assertEquals(200, responseStatus);
            assertEquals(null, errorMessage);

            //Validate that no digest was sent since digest was configured to be off.
            assertEquals("", responseMessage);
        }
    }

    @Test
    public void testProcessJALRecordMessageValidRecordInvalidSysMetadataLen() throws ClientProtocolException, IOException {
        String publisherId = UUID.randomUUID().toString();
        for (RecordType recType : RecordType.values())
        {
            if (recType.equals(RecordType.Unset))
            {
                continue;
            }

            String sessionId = sendValidInitialize(recType, true, publisherId);

            HttpPost httpPost = new HttpPost("http://localhost:" + HTTP_PORT + "/" + recType.toString().toLowerCase());

            String payLoadLengthHeader = "JAL-" + recType.toString() + "-Length";
            String jalMessage = recType.toString().toLowerCase() +  "-record";

            HashMap<String, String> headers = getJalRecordHeaders(sessionId, UUID.randomUUID().toString(), "3082", "1125", "19", payLoadLengthHeader, jalMessage);

            for (Map.Entry<String, String> entry : headers.entrySet())
            {
                httpPost.setHeader(entry.getKey(), entry.getValue());
            }

            //Adds jal record to post
            File resourcesDirectory = new File("src/test/resources");

            String jalRecord1Path = resourcesDirectory.getAbsolutePath() + "/jal_record1.txt";
            HttpEntity entity = EntityBuilder.create().setFile(new File(jalRecord1Path)).build();

            httpPost.setEntity(entity);

            HttpClient client = HttpClientBuilder.create().build();

            final HttpResponse response = client.execute(httpPost);
            final String responseMessage = response.getFirstHeader(HttpUtils.HDRS_ERROR_MESSAGE).getValue();
            final int responseStatus = response.getStatusLine().getStatusCode();
            assertEquals(200, responseStatus);

            //Session does not exist
            assertEquals(HttpUtils.HDRS_RECORD_FAILED, responseMessage);
        }
    }

    @Test
    public void testProcessJALRecordMessageValidRecordInvalidAppMetadataLen() throws ClientProtocolException, IOException {
        String publisherId = UUID.randomUUID().toString();
        for (RecordType recType : RecordType.values())
        {
            if (recType.equals(RecordType.Unset))
            {
                continue;
            }

            String sessionId = sendValidInitialize(recType, true, publisherId);

            HttpPost httpPost = new HttpPost("http://localhost:" + HTTP_PORT + "/" + recType.toString().toLowerCase());

            String payLoadLengthHeader = "JAL-" + recType.toString() + "-Length";
            String jalMessage = recType.toString().toLowerCase() +  "-record";

            HashMap<String, String> headers = getJalRecordHeaders(sessionId, UUID.randomUUID().toString(), "3083", "1126", "19", payLoadLengthHeader, jalMessage);

            for (Map.Entry<String, String> entry : headers.entrySet())
            {
                httpPost.setHeader(entry.getKey(), entry.getValue());
            }

            //Adds jal record to post
            File resourcesDirectory = new File("src/test/resources");

            String jalRecord1Path = resourcesDirectory.getAbsolutePath() + "/jal_record1.txt";
            HttpEntity entity = EntityBuilder.create().setFile(new File(jalRecord1Path)).build();

            httpPost.setEntity(entity);

            HttpClient client = HttpClientBuilder.create().build();

            final HttpResponse response = client.execute(httpPost);
            final String responseMessage = response.getFirstHeader(HttpUtils.HDRS_ERROR_MESSAGE).getValue();
            final int responseStatus = response.getStatusLine().getStatusCode();
            assertEquals(200, responseStatus);

            //Session does not exist
            assertEquals(HttpUtils.HDRS_RECORD_FAILED, responseMessage);
        }
    }

    @Test
    public void testProcessJALRecordMessageValidRecordInvalidPayloadLen() throws ClientProtocolException, IOException {
        String publisherId = UUID.randomUUID().toString();
        for (RecordType recType : RecordType.values())
        {
            if (recType.equals(RecordType.Unset))
            {
                continue;
            }

            String sessionId = sendValidInitialize(recType, true, publisherId);

            HttpPost httpPost = new HttpPost("http://localhost:" + HTTP_PORT + "/" + recType.toString().toLowerCase());

            String payLoadLengthHeader = "JAL-" + recType.toString() + "-Length";
            String jalMessage = recType.toString().toLowerCase() +  "-record";

            HashMap<String, String> headers = getJalRecordHeaders(sessionId, UUID.randomUUID().toString(), "3083", "1125", "18", payLoadLengthHeader, jalMessage);

            for (Map.Entry<String, String> entry : headers.entrySet())
            {
                httpPost.setHeader(entry.getKey(), entry.getValue());
            }

            //Adds jal record to post
            File resourcesDirectory = new File("src/test/resources");

            String jalRecord1Path = resourcesDirectory.getAbsolutePath() + "/jal_record1.txt";
            HttpEntity entity = EntityBuilder.create().setFile(new File(jalRecord1Path)).build();

            httpPost.setEntity(entity);

            HttpClient client = HttpClientBuilder.create().build();

            final HttpResponse response = client.execute(httpPost);
            final String responseMessage = response.getFirstHeader(HttpUtils.HDRS_ERROR_MESSAGE).getValue();
            final int responseStatus = response.getStatusLine().getStatusCode();
            assertEquals(200, responseStatus);

            //Session does not exist
            assertEquals(HttpUtils.HDRS_RECORD_FAILED, responseMessage);
        }
    }

    @Test
    public void testCreateJournalResumeMessageEmptyNounce()
    {
        HashMap<String, String> successHeaders = new HashMap<String, String>();
        List<String> errorHeaders = new ArrayList<String>();
        boolean result = MessageProcessor.createJournalResumeMessage(null, 0, successHeaders, errorHeaders);
        assertEquals(null, successHeaders.get(HttpUtils.HDRS_NONCE));
        assertEquals("0", successHeaders.get(HttpUtils.HDRS_JOURNAL_OFFSET));
        assertEquals(0, errorHeaders.size());
        assertTrue(result);

    }

    @Test
    public void testCreateJournalResumeMessageInvalidJalOffset()
    {
        HashMap<String, String> headers = new HashMap<String, String>();
        List<String> errorHeaders = new ArrayList<String>();
        boolean result = MessageProcessor.createJournalResumeMessage("test", -1, headers, errorHeaders);
        assertEquals(false, result);
        assertEquals(0, headers.size());
        assertEquals(true, errorHeaders.contains(HttpUtils.HDRS_INVALID_JOURNAL_OFFSET));
    }
}