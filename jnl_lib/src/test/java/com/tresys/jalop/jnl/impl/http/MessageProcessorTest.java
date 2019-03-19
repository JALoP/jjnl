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

        handler.addServletWithMapping(JNLJournalServlet.class, HttpUtils.JOURNAL_ENDPOINT);
        handler.addServletWithMapping(JNLAuditServlet.class, HttpUtils.AUDIT_ENDPOINT);
        handler.addServletWithMapping(JNLLogServlet.class, HttpUtils.LOG_ENDPOINT);

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

        final HttpPost httpPost = new HttpPost("http://localhost:" + HTTP_PORT + HttpUtils.JOURNAL_ENDPOINT);
        httpPost.setHeader(HttpUtils.HDRS_CONTENT_TYPE, HttpUtils.DEFAULT_CONTENT_TYPE);
        httpPost.setHeader(HttpUtils.HDRS_MESSAGE, HttpUtils.MSG_INIT);
        httpPost.setHeader(HttpUtils.HDRS_MODE, HttpUtils.MSG_PUBLISH_LIVE);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_DIGEST, DigestMethod.SHA256);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_XML_COMPRESSION, HttpUtils.SUPPORTED_XML_COMPRESSIONS[0]);
        httpPost.setHeader(HttpUtils.HDRS_DATA_CLASS, HttpUtils.JOURNAL);
        httpPost.setHeader(HttpUtils.HDRS_VERSION, HttpUtils.SUPPORTED_VERSIONS[0]);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_CONFIGURE_DIGEST_CHALLENGE, HttpUtils.MSG_CONFIGURE_DIGEST_ON);

        HttpClient client = HttpClientBuilder.create().build();

        final HttpResponse response = client.execute(httpPost);
        final String responseMessage = response.getFirstHeader(HttpUtils.HDRS_MESSAGE).getValue();
        final int responseStatus = response.getStatusLine().getStatusCode();
        assertEquals(responseStatus, 200);
        assertEquals(responseMessage, "initialize-ack");
    }

    @Test
    public void testInitializeAuditReturnsInitializeAck() throws ClientProtocolException, IOException {

        final HttpPost httpPost = new HttpPost("http://localhost:" + HTTP_PORT + HttpUtils.AUDIT_ENDPOINT);
        httpPost.setHeader(HttpUtils.HDRS_CONTENT_TYPE, HttpUtils.DEFAULT_CONTENT_TYPE);
        httpPost.setHeader(HttpUtils.HDRS_MESSAGE, HttpUtils.MSG_INIT);
        httpPost.setHeader(HttpUtils.HDRS_MODE, HttpUtils.MSG_PUBLISH_LIVE);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_DIGEST, DigestMethod.SHA256);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_XML_COMPRESSION, HttpUtils.SUPPORTED_XML_COMPRESSIONS[0]);
        httpPost.setHeader(HttpUtils.HDRS_DATA_CLASS, HttpUtils.AUDIT);
        httpPost.setHeader(HttpUtils.HDRS_VERSION, HttpUtils.SUPPORTED_VERSIONS[0]);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_CONFIGURE_DIGEST_CHALLENGE, HttpUtils.MSG_CONFIGURE_DIGEST_ON);

        HttpClient client = HttpClientBuilder.create().build();

        final HttpResponse response = client.execute(httpPost);
        final String responseMessage = response.getFirstHeader(HttpUtils.HDRS_MESSAGE).getValue();
        final int responseStatus = response.getStatusLine().getStatusCode();
        assertEquals(responseStatus, 200);
        assertEquals(responseMessage, "initialize-ack");
    }

    @Test
    public void testInitializeLogReturnsInitializeAck() throws ClientProtocolException, IOException {

        final HttpPost httpPost = new HttpPost("http://localhost:" + HTTP_PORT + HttpUtils.LOG_ENDPOINT);
        httpPost.setHeader(HttpUtils.HDRS_CONTENT_TYPE, HttpUtils.DEFAULT_CONTENT_TYPE);
        httpPost.setHeader(HttpUtils.HDRS_MESSAGE, HttpUtils.MSG_INIT);
        httpPost.setHeader(HttpUtils.HDRS_MODE, HttpUtils.MSG_PUBLISH_LIVE);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_DIGEST, DigestMethod.SHA256);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_XML_COMPRESSION, HttpUtils.SUPPORTED_XML_COMPRESSIONS[0]);
        httpPost.setHeader(HttpUtils.HDRS_DATA_CLASS, HttpUtils.LOG);
        httpPost.setHeader(HttpUtils.HDRS_VERSION, HttpUtils.SUPPORTED_VERSIONS[0]);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_CONFIGURE_DIGEST_CHALLENGE, HttpUtils.MSG_CONFIGURE_DIGEST_ON);


        HttpClient client = HttpClientBuilder.create().build();

        final HttpResponse response = client.execute(httpPost);
        final String responseMessage = response.getFirstHeader(HttpUtils.HDRS_MESSAGE).getValue();
        final int responseStatus = response.getStatusLine().getStatusCode();
        assertEquals(responseStatus, 200);
        assertEquals(responseMessage, "initialize-ack");
    }

    @Test
    public void testNullDigestReturnsInitializeAck() throws ClientProtocolException, IOException {

        final HttpPost httpPost = new HttpPost("http://localhost:" + HTTP_PORT + HttpUtils.JOURNAL_ENDPOINT);
        httpPost.setHeader(HttpUtils.HDRS_CONTENT_TYPE, HttpUtils.DEFAULT_CONTENT_TYPE);
        httpPost.setHeader(HttpUtils.HDRS_MESSAGE, HttpUtils.MSG_INIT);
        httpPost.setHeader(HttpUtils.HDRS_MODE, HttpUtils.MSG_PUBLISH_LIVE);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_XML_COMPRESSION, HttpUtils.SUPPORTED_XML_COMPRESSIONS[0]);
        httpPost.setHeader(HttpUtils.HDRS_DATA_CLASS, HttpUtils.JOURNAL);
        httpPost.setHeader(HttpUtils.HDRS_VERSION, HttpUtils.SUPPORTED_VERSIONS[0]);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_CONFIGURE_DIGEST_CHALLENGE, HttpUtils.MSG_CONFIGURE_DIGEST_ON);

        HttpClient client = HttpClientBuilder.create().build();

        final HttpResponse response = client.execute(httpPost);
        final String responseMessage = response.getFirstHeader(HttpUtils.HDRS_MESSAGE).getValue();
        final int responseStatus = response.getStatusLine().getStatusCode();
        assertEquals(responseStatus, 200);
        assertEquals(responseMessage, "initialize-ack");
    }

    @Test
    public void testEmptyDigestReturnsInitializeAck() throws ClientProtocolException, IOException {

        final HttpPost httpPost = new HttpPost("http://localhost:" + HTTP_PORT + HttpUtils.JOURNAL_ENDPOINT);
        httpPost.setHeader(HttpUtils.HDRS_CONTENT_TYPE, HttpUtils.DEFAULT_CONTENT_TYPE);
        httpPost.setHeader(HttpUtils.HDRS_MESSAGE, HttpUtils.MSG_INIT);
        httpPost.setHeader(HttpUtils.HDRS_MODE, HttpUtils.MSG_PUBLISH_LIVE);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_DIGEST, "");
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_XML_COMPRESSION, HttpUtils.SUPPORTED_XML_COMPRESSIONS[0]);
        httpPost.setHeader(HttpUtils.HDRS_DATA_CLASS, HttpUtils.JOURNAL);
        httpPost.setHeader(HttpUtils.HDRS_VERSION, HttpUtils.SUPPORTED_VERSIONS[0]);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_CONFIGURE_DIGEST_CHALLENGE, HttpUtils.MSG_CONFIGURE_DIGEST_ON);

        HttpClient client = HttpClientBuilder.create().build();

        final HttpResponse response = client.execute(httpPost);
        final String responseMessage = response.getFirstHeader(HttpUtils.HDRS_MESSAGE).getValue();
        final int responseStatus = response.getStatusLine().getStatusCode();
        assertEquals(responseStatus, 200);
        assertEquals(responseMessage, "initialize-ack");
    }

    @Test
    public void testNullXmlCompressionReturnsInitializeAck() throws ClientProtocolException, IOException {

        final HttpPost httpPost = new HttpPost("http://localhost:" + HTTP_PORT + HttpUtils.JOURNAL_ENDPOINT);
        httpPost.setHeader(HttpUtils.HDRS_CONTENT_TYPE, HttpUtils.DEFAULT_CONTENT_TYPE);
        httpPost.setHeader(HttpUtils.HDRS_MESSAGE, HttpUtils.MSG_INIT);
        httpPost.setHeader(HttpUtils.HDRS_MODE, HttpUtils.MSG_PUBLISH_LIVE);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_DIGEST, DigestMethod.SHA256);
        httpPost.setHeader(HttpUtils.HDRS_DATA_CLASS, HttpUtils.JOURNAL);
        httpPost.setHeader(HttpUtils.HDRS_VERSION, HttpUtils.SUPPORTED_VERSIONS[0]);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_CONFIGURE_DIGEST_CHALLENGE, HttpUtils.MSG_CONFIGURE_DIGEST_ON);

        HttpClient client = HttpClientBuilder.create().build();

        final HttpResponse response = client.execute(httpPost);
        final String responseMessage = response.getFirstHeader(HttpUtils.HDRS_MESSAGE).getValue();
        final int responseStatus = response.getStatusLine().getStatusCode();
        assertEquals(responseStatus, 200);
        assertEquals(responseMessage, "initialize-ack");
    }

    @Test
    public void testEmptyXmlCompressionReturnsInitializeAck() throws ClientProtocolException, IOException {

        final HttpPost httpPost = new HttpPost("http://localhost:" + HTTP_PORT + HttpUtils.JOURNAL_ENDPOINT);
        httpPost.setHeader(HttpUtils.HDRS_CONTENT_TYPE, HttpUtils.DEFAULT_CONTENT_TYPE);
        httpPost.setHeader(HttpUtils.HDRS_MESSAGE, HttpUtils.MSG_INIT);
        httpPost.setHeader(HttpUtils.HDRS_MODE, HttpUtils.MSG_PUBLISH_LIVE);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_DIGEST, DigestMethod.SHA256);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_XML_COMPRESSION, "");
        httpPost.setHeader(HttpUtils.HDRS_DATA_CLASS, HttpUtils.JOURNAL);
        httpPost.setHeader(HttpUtils.HDRS_VERSION, HttpUtils.SUPPORTED_VERSIONS[0]);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_CONFIGURE_DIGEST_CHALLENGE, HttpUtils.MSG_CONFIGURE_DIGEST_ON);

        HttpClient client = HttpClientBuilder.create().build();

        final HttpResponse response = client.execute(httpPost);
        final String responseMessage = response.getFirstHeader(HttpUtils.HDRS_MESSAGE).getValue();
        final int responseStatus = response.getStatusLine().getStatusCode();
        assertEquals(responseStatus, 200);
        assertEquals(responseMessage, "initialize-ack");
    }

    @Test
    public void testNullConfigureDigestChallengeReturnsInitializeAck() throws ClientProtocolException, IOException {

        final HttpPost httpPost = new HttpPost("http://localhost:" + HTTP_PORT + HttpUtils.JOURNAL_ENDPOINT);
        httpPost.setHeader(HttpUtils.HDRS_CONTENT_TYPE, HttpUtils.DEFAULT_CONTENT_TYPE);
        httpPost.setHeader(HttpUtils.HDRS_MESSAGE, HttpUtils.MSG_INIT);
        httpPost.setHeader(HttpUtils.HDRS_MODE, HttpUtils.MSG_PUBLISH_LIVE);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_DIGEST, DigestMethod.SHA256);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_XML_COMPRESSION, HttpUtils.SUPPORTED_XML_COMPRESSIONS[0]);
        httpPost.setHeader(HttpUtils.HDRS_DATA_CLASS, HttpUtils.JOURNAL);
        httpPost.setHeader(HttpUtils.HDRS_VERSION, HttpUtils.SUPPORTED_VERSIONS[0]);

        HttpClient client = HttpClientBuilder.create().build();

        final HttpResponse response = client.execute(httpPost);
        final String responseMessage = response.getFirstHeader(HttpUtils.HDRS_MESSAGE).getValue();
        final int responseStatus = response.getStatusLine().getStatusCode();
        assertEquals(responseStatus, 200);
        assertEquals(responseMessage, "initialize-ack");
    }

    @Test
    public void testEmptyConfigureDigestChallengeReturnsInitializeAck() throws ClientProtocolException, IOException {

        final HttpPost httpPost = new HttpPost("http://localhost:" + HTTP_PORT + HttpUtils.JOURNAL_ENDPOINT);
        httpPost.setHeader(HttpUtils.HDRS_CONTENT_TYPE, HttpUtils.DEFAULT_CONTENT_TYPE);
        httpPost.setHeader(HttpUtils.HDRS_MESSAGE, HttpUtils.MSG_INIT);
        httpPost.setHeader(HttpUtils.HDRS_MODE, HttpUtils.MSG_PUBLISH_LIVE);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_DIGEST, DigestMethod.SHA256);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_XML_COMPRESSION, HttpUtils.SUPPORTED_XML_COMPRESSIONS[0]);
        httpPost.setHeader(HttpUtils.HDRS_DATA_CLASS, HttpUtils.JOURNAL);
        httpPost.setHeader(HttpUtils.HDRS_VERSION, HttpUtils.SUPPORTED_VERSIONS[0]);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_CONFIGURE_DIGEST_CHALLENGE, "");

        HttpClient client = HttpClientBuilder.create().build();

        final HttpResponse response = client.execute(httpPost);
        final String responseMessage = response.getFirstHeader(HttpUtils.HDRS_MESSAGE).getValue();
        final int responseStatus = response.getStatusLine().getStatusCode();
        assertEquals(responseStatus, 200);
        assertEquals(responseMessage, "initialize-ack");
    }

    @Test
    public void testInitializeWithListsReturnsInitializeAck() throws ClientProtocolException, IOException {

        final HttpPost httpPost = new HttpPost("http://localhost:" + HTTP_PORT + HttpUtils.JOURNAL_ENDPOINT);
        httpPost.setHeader(HttpUtils.HDRS_CONTENT_TYPE, HttpUtils.DEFAULT_CONTENT_TYPE);
        httpPost.setHeader(HttpUtils.HDRS_MESSAGE, HttpUtils.MSG_INIT);
        httpPost.setHeader(HttpUtils.HDRS_MODE, HttpUtils.MSG_PUBLISH_LIVE);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_DIGEST, DigestMethod.SHA256);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_XML_COMPRESSION, String.join(",", HttpUtils.SUPPORTED_XML_COMPRESSIONS[0]));
        httpPost.setHeader(HttpUtils.HDRS_DATA_CLASS, HttpUtils.JOURNAL);
        httpPost.setHeader(HttpUtils.HDRS_VERSION, HttpUtils.SUPPORTED_VERSIONS[0]);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_CONFIGURE_DIGEST_CHALLENGE, String.join(",", HttpUtils.MSG_CONFIGURE_DIGEST_ON));

        HttpClient client = HttpClientBuilder.create().build();

        final HttpResponse response = client.execute(httpPost);
        final String responseMessage = response.getFirstHeader(HttpUtils.HDRS_MESSAGE).getValue();
        final int responseStatus = response.getStatusLine().getStatusCode();
        assertEquals(responseStatus, 200);
        assertEquals(responseMessage, "initialize-ack");
    }

    @Test
    public void testInvalidModeReturnsInitializeNack() throws ClientProtocolException, IOException {

        final HttpPost httpPost = new HttpPost("http://localhost:" + HTTP_PORT + HttpUtils.JOURNAL_ENDPOINT);
        httpPost.setHeader(HttpUtils.HDRS_CONTENT_TYPE, HttpUtils.DEFAULT_CONTENT_TYPE);
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
        assertEquals(responseStatus, 200);
        assertEquals(responseMessage, "initialize-nack");
    }

    @Test
    public void testInvalidDigestReturnsInitializeNack() throws ClientProtocolException, IOException {

        final HttpPost httpPost = new HttpPost("http://localhost:" + HTTP_PORT + HttpUtils.JOURNAL_ENDPOINT);
        httpPost.setHeader(HttpUtils.HDRS_CONTENT_TYPE, HttpUtils.DEFAULT_CONTENT_TYPE);
        httpPost.setHeader(HttpUtils.HDRS_MESSAGE, HttpUtils.MSG_INIT);
        httpPost.setHeader(HttpUtils.HDRS_MODE, HttpUtils.MSG_PUBLISH_LIVE);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_DIGEST, "invalid");
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_XML_COMPRESSION, HttpUtils.SUPPORTED_XML_COMPRESSIONS[0]);
        httpPost.setHeader(HttpUtils.HDRS_DATA_CLASS, HttpUtils.JOURNAL);
        httpPost.setHeader(HttpUtils.HDRS_VERSION, HttpUtils.SUPPORTED_VERSIONS[0]);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_CONFIGURE_DIGEST_CHALLENGE, HttpUtils.MSG_CONFIGURE_DIGEST_ON);

        HttpClient client = HttpClientBuilder.create().build();

        final HttpResponse response = client.execute(httpPost);
        final String responseMessage = response.getFirstHeader(HttpUtils.HDRS_MESSAGE).getValue();
        final int responseStatus = response.getStatusLine().getStatusCode();
        assertEquals(responseStatus, 200);
        assertEquals(responseMessage, "initialize-nack");
    }

    @Test
    public void testInvalidXmlCompressionReturnsInitializeNack() throws ClientProtocolException, IOException {

        final HttpPost httpPost = new HttpPost("http://localhost:" + HTTP_PORT + HttpUtils.JOURNAL_ENDPOINT);
        httpPost.setHeader(HttpUtils.HDRS_CONTENT_TYPE, HttpUtils.DEFAULT_CONTENT_TYPE);
        httpPost.setHeader(HttpUtils.HDRS_MESSAGE, HttpUtils.MSG_INIT);
        httpPost.setHeader(HttpUtils.HDRS_MODE, HttpUtils.MSG_PUBLISH_LIVE);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_DIGEST, DigestMethod.SHA256);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_XML_COMPRESSION, "invalid");
        httpPost.setHeader(HttpUtils.HDRS_DATA_CLASS, HttpUtils.JOURNAL);
        httpPost.setHeader(HttpUtils.HDRS_VERSION, HttpUtils.SUPPORTED_VERSIONS[0]);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_CONFIGURE_DIGEST_CHALLENGE, HttpUtils.MSG_CONFIGURE_DIGEST_ON);

        HttpClient client = HttpClientBuilder.create().build();

        final HttpResponse response = client.execute(httpPost);
        final String responseMessage = response.getFirstHeader(HttpUtils.HDRS_MESSAGE).getValue();
        final int responseStatus = response.getStatusLine().getStatusCode();
        assertEquals(responseStatus, 200);
        assertEquals(responseMessage, "initialize-nack");
    }

    @Test
    public void testInvalidDataClassReturnsInitializeNack() throws ClientProtocolException, IOException {

        final HttpPost httpPost = new HttpPost("http://localhost:" + HTTP_PORT + HttpUtils.JOURNAL_ENDPOINT);
        httpPost.setHeader(HttpUtils.HDRS_CONTENT_TYPE, HttpUtils.DEFAULT_CONTENT_TYPE);
        httpPost.setHeader(HttpUtils.HDRS_MESSAGE, HttpUtils.MSG_INIT);
        httpPost.setHeader(HttpUtils.HDRS_MODE, HttpUtils.MSG_PUBLISH_LIVE);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_DIGEST, DigestMethod.SHA256);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_XML_COMPRESSION, HttpUtils.SUPPORTED_XML_COMPRESSIONS[0]);
        httpPost.setHeader(HttpUtils.HDRS_DATA_CLASS, "invalid");
        httpPost.setHeader(HttpUtils.HDRS_VERSION, HttpUtils.SUPPORTED_VERSIONS[0]);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_CONFIGURE_DIGEST_CHALLENGE, HttpUtils.MSG_CONFIGURE_DIGEST_ON);

        HttpClient client = HttpClientBuilder.create().build();

        final HttpResponse response = client.execute(httpPost);
        final String responseMessage = response.getFirstHeader(HttpUtils.HDRS_MESSAGE).getValue();
        final int responseStatus = response.getStatusLine().getStatusCode();
        assertEquals(responseStatus, 200);
        assertEquals(responseMessage, "initialize-nack");
    }

    @Test
    public void testInvalidVersionReturnsInitializeNack() throws ClientProtocolException, IOException {

        final HttpPost httpPost = new HttpPost("http://localhost:" + HTTP_PORT + HttpUtils.JOURNAL_ENDPOINT);
        httpPost.setHeader(HttpUtils.HDRS_CONTENT_TYPE, HttpUtils.DEFAULT_CONTENT_TYPE);
        httpPost.setHeader(HttpUtils.HDRS_MESSAGE, HttpUtils.MSG_INIT);
        httpPost.setHeader(HttpUtils.HDRS_MODE, HttpUtils.MSG_PUBLISH_LIVE);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_DIGEST, DigestMethod.SHA256);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_XML_COMPRESSION, HttpUtils.SUPPORTED_XML_COMPRESSIONS[0]);
        httpPost.setHeader(HttpUtils.HDRS_DATA_CLASS, HttpUtils.JOURNAL);
        httpPost.setHeader(HttpUtils.HDRS_VERSION,"invalid");
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_CONFIGURE_DIGEST_CHALLENGE, HttpUtils.MSG_CONFIGURE_DIGEST_ON);

        HttpClient client = HttpClientBuilder.create().build();

        final HttpResponse response = client.execute(httpPost);
        final String responseMessage = response.getFirstHeader(HttpUtils.HDRS_MESSAGE).getValue();
        final int responseStatus = response.getStatusLine().getStatusCode();
        assertEquals(responseStatus, 200);
        assertEquals(responseMessage, "initialize-nack");
    }

    @Test
    public void testInvalidConfigureDigestChallengeReturnsInitializeNack() throws ClientProtocolException, IOException {

        final HttpPost httpPost = new HttpPost("http://localhost:" + HTTP_PORT + HttpUtils.JOURNAL_ENDPOINT);
        httpPost.setHeader(HttpUtils.HDRS_CONTENT_TYPE, HttpUtils.DEFAULT_CONTENT_TYPE);
        httpPost.setHeader(HttpUtils.HDRS_MESSAGE, HttpUtils.MSG_INIT);
        httpPost.setHeader(HttpUtils.HDRS_MODE, HttpUtils.MSG_PUBLISH_LIVE);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_DIGEST, DigestMethod.SHA256);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_XML_COMPRESSION, HttpUtils.SUPPORTED_XML_COMPRESSIONS[0]);
        httpPost.setHeader(HttpUtils.HDRS_DATA_CLASS, HttpUtils.JOURNAL);
        httpPost.setHeader(HttpUtils.HDRS_VERSION, HttpUtils.SUPPORTED_VERSIONS[0]);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_CONFIGURE_DIGEST_CHALLENGE, "invalid");

        HttpClient client = HttpClientBuilder.create().build();

        final HttpResponse response = client.execute(httpPost);
        final String responseMessage = response.getFirstHeader(HttpUtils.HDRS_MESSAGE).getValue();
        final int responseStatus = response.getStatusLine().getStatusCode();
        assertEquals(responseStatus, 200);
        assertEquals(responseMessage, "initialize-nack");
    }
}