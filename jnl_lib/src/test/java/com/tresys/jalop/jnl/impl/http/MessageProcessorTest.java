package com.tresys.jalop.jnl.impl.http;

import static org.junit.Assert.assertEquals;
import java.io.IOException;
import javax.xml.crypto.dsig.DigestMethod;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests for common utility class.
 */
public class MessageProcessorTest {

    private static Server server;
    private static int HTTP_PORT = 8080;

    private static String JOURNAL_ENDPOINT = "/journal";
    private static String AUDIT_ENDPOINT = "/audit";
    private static String LOG_ENDPOINT = "/log";


    /**
     * Sets up the server for the web service.
     *
     * @throws Exception
     */
    @BeforeClass
    public static void startWebServiceServer() throws Exception {
        server = new Server(HTTP_PORT);

        ServletHandler handler = new ServletHandler();
        server.setHandler(handler);

        handler.addServletWithMapping(JNLJournalServlet.class, JOURNAL_ENDPOINT);
        handler.addServletWithMapping(JNLAuditServlet.class, AUDIT_ENDPOINT);
        handler.addServletWithMapping(JNLLogServlet.class, LOG_ENDPOINT);

        server.start();
    }

    /**
     * Stops the web service server.
     *
     * @throws Exception
     */
    @AfterClass
    public static void stopWebServiceServer() throws Exception {
        server.stop();
    }

    @Test
    public void testInitializeJournalReturnsInitializeAck() throws ClientProtocolException, IOException {

        final HttpPost httpPost = new HttpPost("http://localhost:" + HTTP_PORT + JOURNAL_ENDPOINT);
        httpPost.setHeader(MessageProcessor.HDRS_CONTENT_TYPE, MessageProcessor.DEFAULT_CONTENT_TYPE);
        httpPost.setHeader(MessageProcessor.HDRS_MESSAGE, MessageProcessor.MSG_INIT);
        httpPost.setHeader(MessageProcessor.HDRS_MODE, MessageProcessor.MSG_PUBLISH_LIVE);
        httpPost.setHeader(MessageProcessor.HDRS_ACCEPT_DIGEST, DigestMethod.SHA256);
        httpPost.setHeader(MessageProcessor.HDRS_ACCEPT_XML_COMPRESSION, MessageProcessor.SUPPORTED_XML_COMPRESSIONS[0]);
        httpPost.setHeader(MessageProcessor.HDRS_DATA_CLASS, MessageProcessor.JOURNAL);
        httpPost.setHeader(MessageProcessor.HDRS_VERSION, MessageProcessor.SUPPORTED_VERSION);

        HttpClient client = HttpClientBuilder.create().build();

        final HttpResponse response = client.execute(httpPost);
        final String responseMessage = response.getFirstHeader(MessageProcessor.HDRS_MESSAGE).getValue();
        final int responseStatus = response.getStatusLine().getStatusCode();
        assertEquals(responseStatus, 200);
        assertEquals(responseMessage, "initialize-ack");
    }

    @Test
    public void testInitializeAuditReturnsInitializeAck() throws ClientProtocolException, IOException {

        final HttpPost httpPost = new HttpPost("http://localhost:" + HTTP_PORT + AUDIT_ENDPOINT);
        httpPost.setHeader(MessageProcessor.HDRS_CONTENT_TYPE, MessageProcessor.DEFAULT_CONTENT_TYPE);
        httpPost.setHeader(MessageProcessor.HDRS_MESSAGE, MessageProcessor.MSG_INIT);
        httpPost.setHeader(MessageProcessor.HDRS_MODE, MessageProcessor.MSG_PUBLISH_LIVE);
        httpPost.setHeader(MessageProcessor.HDRS_ACCEPT_DIGEST, DigestMethod.SHA256);
        httpPost.setHeader(MessageProcessor.HDRS_ACCEPT_XML_COMPRESSION, MessageProcessor.SUPPORTED_XML_COMPRESSIONS[0]);
        httpPost.setHeader(MessageProcessor.HDRS_DATA_CLASS, MessageProcessor.AUDIT);
        httpPost.setHeader(MessageProcessor.HDRS_VERSION, MessageProcessor.SUPPORTED_VERSION);

        HttpClient client = HttpClientBuilder.create().build();

        final HttpResponse response = client.execute(httpPost);
        final String responseMessage = response.getFirstHeader(MessageProcessor.HDRS_MESSAGE).getValue();
        final int responseStatus = response.getStatusLine().getStatusCode();
        assertEquals(responseStatus, 200);
        assertEquals(responseMessage, "initialize-ack");
    }

    @Test
    public void testInitializeLogReturnsInitializeAck() throws ClientProtocolException, IOException {

        final HttpPost httpPost = new HttpPost("http://localhost:" + HTTP_PORT + LOG_ENDPOINT);
        httpPost.setHeader(MessageProcessor.HDRS_CONTENT_TYPE, MessageProcessor.DEFAULT_CONTENT_TYPE);
        httpPost.setHeader(MessageProcessor.HDRS_MESSAGE, MessageProcessor.MSG_INIT);
        httpPost.setHeader(MessageProcessor.HDRS_MODE, MessageProcessor.MSG_PUBLISH_LIVE);
        httpPost.setHeader(MessageProcessor.HDRS_ACCEPT_DIGEST, DigestMethod.SHA256);
        httpPost.setHeader(MessageProcessor.HDRS_ACCEPT_XML_COMPRESSION, MessageProcessor.SUPPORTED_XML_COMPRESSIONS[0]);
        httpPost.setHeader(MessageProcessor.HDRS_DATA_CLASS, MessageProcessor.LOG);
        httpPost.setHeader(MessageProcessor.HDRS_VERSION, MessageProcessor.SUPPORTED_VERSION);

        HttpClient client = HttpClientBuilder.create().build();

        final HttpResponse response = client.execute(httpPost);
        final String responseMessage = response.getFirstHeader(MessageProcessor.HDRS_MESSAGE).getValue();
        final int responseStatus = response.getStatusLine().getStatusCode();
        assertEquals(responseStatus, 200);
        assertEquals(responseMessage, "initialize-ack");
    }

    @Test
    public void testInvalidModeReturnsInitializeNack() throws ClientProtocolException, IOException {

        final HttpPost httpPost = new HttpPost("http://localhost:" + HTTP_PORT + JOURNAL_ENDPOINT);
        httpPost.setHeader(MessageProcessor.HDRS_CONTENT_TYPE, MessageProcessor.DEFAULT_CONTENT_TYPE);
        httpPost.setHeader(MessageProcessor.HDRS_MESSAGE, MessageProcessor.MSG_INIT);
        httpPost.setHeader(MessageProcessor.HDRS_MODE, "invalid");
        httpPost.setHeader(MessageProcessor.HDRS_ACCEPT_DIGEST, DigestMethod.SHA256);
        httpPost.setHeader(MessageProcessor.HDRS_ACCEPT_XML_COMPRESSION, MessageProcessor.SUPPORTED_XML_COMPRESSIONS[0]);
        httpPost.setHeader(MessageProcessor.HDRS_DATA_CLASS, MessageProcessor.JOURNAL);
        httpPost.setHeader(MessageProcessor.HDRS_VERSION, MessageProcessor.SUPPORTED_VERSION);

        HttpClient client = HttpClientBuilder.create().build();

        final HttpResponse response = client.execute(httpPost);
        final String responseMessage = response.getFirstHeader(MessageProcessor.HDRS_MESSAGE).getValue();
        final int responseStatus = response.getStatusLine().getStatusCode();
        assertEquals(responseStatus, 200);
        assertEquals(responseMessage, "initialize-nack");
    }

    @Test
    public void testInvalidDigestReturnsInitializeNack() throws ClientProtocolException, IOException {

        final HttpPost httpPost = new HttpPost("http://localhost:" + HTTP_PORT + JOURNAL_ENDPOINT);
        httpPost.setHeader(MessageProcessor.HDRS_CONTENT_TYPE, MessageProcessor.DEFAULT_CONTENT_TYPE);
        httpPost.setHeader(MessageProcessor.HDRS_MESSAGE, MessageProcessor.MSG_INIT);
        httpPost.setHeader(MessageProcessor.HDRS_MODE, MessageProcessor.MSG_PUBLISH_LIVE);
        httpPost.setHeader(MessageProcessor.HDRS_ACCEPT_DIGEST, "invalid");
        httpPost.setHeader(MessageProcessor.HDRS_ACCEPT_XML_COMPRESSION, MessageProcessor.SUPPORTED_XML_COMPRESSIONS[0]);
        httpPost.setHeader(MessageProcessor.HDRS_DATA_CLASS, MessageProcessor.JOURNAL);
        httpPost.setHeader(MessageProcessor.HDRS_VERSION, MessageProcessor.SUPPORTED_VERSION);

        HttpClient client = HttpClientBuilder.create().build();

        final HttpResponse response = client.execute(httpPost);
        final String responseMessage = response.getFirstHeader(MessageProcessor.HDRS_MESSAGE).getValue();
        final int responseStatus = response.getStatusLine().getStatusCode();
        assertEquals(responseStatus, 200);
        assertEquals(responseMessage, "initialize-nack");
    }

    @Test
    public void testInvalidXmlCompressionReturnsInitializeNack() throws ClientProtocolException, IOException {

        final HttpPost httpPost = new HttpPost("http://localhost:" + HTTP_PORT + JOURNAL_ENDPOINT);
        httpPost.setHeader(MessageProcessor.HDRS_CONTENT_TYPE, MessageProcessor.DEFAULT_CONTENT_TYPE);
        httpPost.setHeader(MessageProcessor.HDRS_MESSAGE, MessageProcessor.MSG_INIT);
        httpPost.setHeader(MessageProcessor.HDRS_MODE, MessageProcessor.MSG_PUBLISH_LIVE);
        httpPost.setHeader(MessageProcessor.HDRS_ACCEPT_DIGEST, DigestMethod.SHA256);
        httpPost.setHeader(MessageProcessor.HDRS_ACCEPT_XML_COMPRESSION, "invalid");
        httpPost.setHeader(MessageProcessor.HDRS_DATA_CLASS, MessageProcessor.JOURNAL);
        httpPost.setHeader(MessageProcessor.HDRS_VERSION, MessageProcessor.SUPPORTED_VERSION);

        HttpClient client = HttpClientBuilder.create().build();

        final HttpResponse response = client.execute(httpPost);
        final String responseMessage = response.getFirstHeader(MessageProcessor.HDRS_MESSAGE).getValue();
        final int responseStatus = response.getStatusLine().getStatusCode();
        assertEquals(responseStatus, 200);
        assertEquals(responseMessage, "initialize-nack");
    }

    @Test
    public void testInvalidDataClassReturnsInitializeNack() throws ClientProtocolException, IOException {

        final HttpPost httpPost = new HttpPost("http://localhost:" + HTTP_PORT + JOURNAL_ENDPOINT);
        httpPost.setHeader(MessageProcessor.HDRS_CONTENT_TYPE, MessageProcessor.DEFAULT_CONTENT_TYPE);
        httpPost.setHeader(MessageProcessor.HDRS_MESSAGE, MessageProcessor.MSG_INIT);
        httpPost.setHeader(MessageProcessor.HDRS_MODE, MessageProcessor.MSG_PUBLISH_LIVE);
        httpPost.setHeader(MessageProcessor.HDRS_ACCEPT_DIGEST, DigestMethod.SHA256);
        httpPost.setHeader(MessageProcessor.HDRS_ACCEPT_XML_COMPRESSION, MessageProcessor.SUPPORTED_XML_COMPRESSIONS[0]);
        httpPost.setHeader(MessageProcessor.HDRS_DATA_CLASS, "invalid");
        httpPost.setHeader(MessageProcessor.HDRS_VERSION, MessageProcessor.SUPPORTED_VERSION);

        HttpClient client = HttpClientBuilder.create().build();

        final HttpResponse response = client.execute(httpPost);
        final String responseMessage = response.getFirstHeader(MessageProcessor.HDRS_MESSAGE).getValue();
        final int responseStatus = response.getStatusLine().getStatusCode();
        assertEquals(responseStatus, 200);
        assertEquals(responseMessage, "initialize-nack");
    }

    @Test
    public void testInvalidVersionReturnsInitializeNack() throws ClientProtocolException, IOException {

        final HttpPost httpPost = new HttpPost("http://localhost:" + HTTP_PORT + JOURNAL_ENDPOINT);
        httpPost.setHeader(MessageProcessor.HDRS_CONTENT_TYPE, MessageProcessor.DEFAULT_CONTENT_TYPE);
        httpPost.setHeader(MessageProcessor.HDRS_MESSAGE, MessageProcessor.MSG_INIT);
        httpPost.setHeader(MessageProcessor.HDRS_MODE, MessageProcessor.MSG_PUBLISH_LIVE);
        httpPost.setHeader(MessageProcessor.HDRS_ACCEPT_DIGEST, DigestMethod.SHA256);
        httpPost.setHeader(MessageProcessor.HDRS_ACCEPT_XML_COMPRESSION, MessageProcessor.SUPPORTED_XML_COMPRESSIONS[0]);
        httpPost.setHeader(MessageProcessor.HDRS_DATA_CLASS, MessageProcessor.JOURNAL);
        httpPost.setHeader(MessageProcessor.HDRS_VERSION,"invalid");

        HttpClient client = HttpClientBuilder.create().build();

        final HttpResponse response = client.execute(httpPost);
        final String responseMessage = response.getFirstHeader(MessageProcessor.HDRS_MESSAGE).getValue();
        final int responseStatus = response.getStatusLine().getStatusCode();
        assertEquals(responseStatus, 200);
        assertEquals(responseMessage, "initialize-nack");
    }
}