package com.tresys.jalop.jnl.impl.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import javax.xml.crypto.dsig.DigestMethod;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.eclipse.jetty.server.Server;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.tresys.jalop.jnl.RecordType;

/**
 * Tests for common utility class.
 */
public class InitializeTest {

    private static Server server;
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

        TestResources.disableHttpClientLogging();

        //gets jjnl dir path
        resourcesDirectory = new File("src/test/resources");
        jjnlDirPath = resourcesDirectory.getAbsolutePath() + "/../../../..";
        outputDirStr = jjnlDirPath + "/jnl_lib/output";

        server = TestResources.getWebServer();

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

        TestResources.cleanOutputDirectory(outputDirStr);
    }

    @Test
    public void testInitializeJournalReturnsInitializeAck() throws ClientProtocolException, IOException {

        UUID publisherUUID = UUID.randomUUID();
        final String publisherId = publisherUUID.toString();
        final HttpPost httpPost = new HttpPost("http://localhost:" + TestResources.HTTP_PORT + HttpUtils.JOURNAL_ENDPOINT);
        httpPost.setHeader(HttpUtils.HDRS_CONTENT_TYPE, HttpUtils.DEFAULT_CONTENT_TYPE);
        httpPost.setHeader(HttpUtils.HDRS_PUBLISHER_ID, publisherId);
        httpPost.setHeader(HttpUtils.HDRS_MESSAGE, HttpUtils.MSG_INIT);
        httpPost.setHeader(HttpUtils.HDRS_MODE, HttpUtils.MSG_LIVE);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_DIGEST, DigestMethod.SHA256);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_XML_COMPRESSION, HttpUtils.SUPPORTED_XML_COMPRESSIONS[0]);
        httpPost.setHeader(HttpUtils.HDRS_DATA_CLASS, HttpUtils.JOURNAL);
        httpPost.setHeader(HttpUtils.HDRS_VERSION, HttpUtils.SUPPORTED_VERSIONS[0]);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_CONFIGURE_DIGEST_CHALLENGE, HttpUtils.MSG_CONFIGURE_DIGEST_ON);
        httpPost.setHeader(HttpUtils.HDRS_CLIENT_CERTIFICATE, TestResources.getCertForHeader(resourcesDirectory.getAbsolutePath()));

        HttpClient client = HttpClientBuilder.create().build();

        final HttpResponse response = client.execute(httpPost);
        final String responseMessage = response.getFirstHeader(HttpUtils.HDRS_MESSAGE).getValue();
        final int responseStatus = response.getStatusLine().getStatusCode();
        assertEquals(200, responseStatus);
        assertEquals("initialize-ack", responseMessage);

        final Header errorHeader = response.getFirstHeader(HttpUtils.HDRS_ERROR_MESSAGE);
        assertNull(errorHeader);

        final Header sessionHeader = response.getFirstHeader(HttpUtils.HDRS_SESSION_ID);
        assertNotNull(sessionHeader);
    }

    @Test
    public void testInitializeAuditReturnsInitializeAck() throws ClientProtocolException, IOException {

        UUID publisherUUID = UUID.randomUUID();
        final String publisherId = publisherUUID.toString();
        final HttpPost httpPost = new HttpPost("http://localhost:" + TestResources.HTTP_PORT + HttpUtils.AUDIT_ENDPOINT);
        httpPost.setHeader(HttpUtils.HDRS_CONTENT_TYPE, HttpUtils.DEFAULT_CONTENT_TYPE);
        httpPost.setHeader(HttpUtils.HDRS_PUBLISHER_ID, publisherId);
        httpPost.setHeader(HttpUtils.HDRS_MESSAGE, HttpUtils.MSG_INIT);
        httpPost.setHeader(HttpUtils.HDRS_MODE, HttpUtils.MSG_LIVE);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_DIGEST, DigestMethod.SHA256);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_XML_COMPRESSION, HttpUtils.SUPPORTED_XML_COMPRESSIONS[0]);
        httpPost.setHeader(HttpUtils.HDRS_DATA_CLASS, HttpUtils.AUDIT);
        httpPost.setHeader(HttpUtils.HDRS_VERSION, HttpUtils.SUPPORTED_VERSIONS[0]);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_CONFIGURE_DIGEST_CHALLENGE, HttpUtils.MSG_CONFIGURE_DIGEST_ON);
        httpPost.setHeader(HttpUtils.HDRS_CLIENT_CERTIFICATE, TestResources.getCertForHeader(resourcesDirectory.getAbsolutePath()));

        HttpClient client = HttpClientBuilder.create().build();

        final HttpResponse response = client.execute(httpPost);
        final String responseMessage = response.getFirstHeader(HttpUtils.HDRS_MESSAGE).getValue();
        final int responseStatus = response.getStatusLine().getStatusCode();
        assertEquals(200, responseStatus);
        assertEquals("initialize-ack", responseMessage);

        final Header errorHeader = response.getFirstHeader(HttpUtils.HDRS_ERROR_MESSAGE);
        assertNull(errorHeader);

        final Header sessionHeader = response.getFirstHeader(HttpUtils.HDRS_SESSION_ID);
        assertNotNull(sessionHeader);
    }

    @Test
    public void testInitializeLogReturnsInitializeAck() throws ClientProtocolException, IOException {

        UUID publisherUUID = UUID.randomUUID();
        final String publisherId = publisherUUID.toString();
        final HttpPost httpPost = new HttpPost("http://localhost:" + TestResources.HTTP_PORT + HttpUtils.LOG_ENDPOINT);
        httpPost.setHeader(HttpUtils.HDRS_CONTENT_TYPE, HttpUtils.DEFAULT_CONTENT_TYPE);
        httpPost.setHeader(HttpUtils.HDRS_PUBLISHER_ID, publisherId);
        httpPost.setHeader(HttpUtils.HDRS_MESSAGE, HttpUtils.MSG_INIT);
        httpPost.setHeader(HttpUtils.HDRS_MODE, HttpUtils.MSG_LIVE);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_DIGEST, DigestMethod.SHA256);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_XML_COMPRESSION, HttpUtils.SUPPORTED_XML_COMPRESSIONS[0]);
        httpPost.setHeader(HttpUtils.HDRS_DATA_CLASS, HttpUtils.LOG);
        httpPost.setHeader(HttpUtils.HDRS_VERSION, HttpUtils.SUPPORTED_VERSIONS[0]);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_CONFIGURE_DIGEST_CHALLENGE, HttpUtils.MSG_CONFIGURE_DIGEST_ON);
        httpPost.setHeader(HttpUtils.HDRS_CLIENT_CERTIFICATE, TestResources.getCertForHeader(resourcesDirectory.getAbsolutePath()));

        HttpClient client = HttpClientBuilder.create().build();

        final HttpResponse response = client.execute(httpPost);
        final String responseMessage = response.getFirstHeader(HttpUtils.HDRS_MESSAGE).getValue();
        final int responseStatus = response.getStatusLine().getStatusCode();
        assertEquals(200, responseStatus);
        assertEquals("initialize-ack", responseMessage);

        final Header errorHeader = response.getFirstHeader(HttpUtils.HDRS_ERROR_MESSAGE);
        assertNull(errorHeader);

        final Header sessionHeader = response.getFirstHeader(HttpUtils.HDRS_SESSION_ID);
        assertNotNull(sessionHeader);
    }

    @Test
    public void testNullDigestReturnsInitializeAck() throws ClientProtocolException, IOException {

        UUID publisherUUID = UUID.randomUUID();
        final String publisherId = publisherUUID.toString();
        final HttpPost httpPost = new HttpPost("http://localhost:" + TestResources.HTTP_PORT + HttpUtils.JOURNAL_ENDPOINT);
        httpPost.setHeader(HttpUtils.HDRS_CONTENT_TYPE, HttpUtils.DEFAULT_CONTENT_TYPE);
        httpPost.setHeader(HttpUtils.HDRS_PUBLISHER_ID, publisherId);
        httpPost.setHeader(HttpUtils.HDRS_MESSAGE, HttpUtils.MSG_INIT);
        httpPost.setHeader(HttpUtils.HDRS_MODE, HttpUtils.MSG_LIVE);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_XML_COMPRESSION, HttpUtils.SUPPORTED_XML_COMPRESSIONS[0]);
        httpPost.setHeader(HttpUtils.HDRS_DATA_CLASS, HttpUtils.JOURNAL);
        httpPost.setHeader(HttpUtils.HDRS_VERSION, HttpUtils.SUPPORTED_VERSIONS[0]);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_CONFIGURE_DIGEST_CHALLENGE, HttpUtils.MSG_CONFIGURE_DIGEST_ON);
        httpPost.setHeader(HttpUtils.HDRS_CLIENT_CERTIFICATE, TestResources.getCertForHeader(resourcesDirectory.getAbsolutePath()));

        HttpClient client = HttpClientBuilder.create().build();

        final HttpResponse response = client.execute(httpPost);
        final String responseMessage = response.getFirstHeader(HttpUtils.HDRS_MESSAGE).getValue();
        final int responseStatus = response.getStatusLine().getStatusCode();
        assertEquals(200, responseStatus);
        assertEquals("initialize-ack", responseMessage);

        final Header errorHeader = response.getFirstHeader(HttpUtils.HDRS_ERROR_MESSAGE);
        assertNull(errorHeader);

        final Header sessionHeader = response.getFirstHeader(HttpUtils.HDRS_SESSION_ID);
        assertNotNull(sessionHeader);
    }

    @Test
    public void testEmptyDigestReturnsInitializeAck() throws ClientProtocolException, IOException {

        UUID publisherUUID = UUID.randomUUID();
        final String publisherId = publisherUUID.toString();
        final HttpPost httpPost = new HttpPost("http://localhost:" + TestResources.HTTP_PORT + HttpUtils.JOURNAL_ENDPOINT);
        httpPost.setHeader(HttpUtils.HDRS_CONTENT_TYPE, HttpUtils.DEFAULT_CONTENT_TYPE);
        httpPost.setHeader(HttpUtils.HDRS_PUBLISHER_ID, publisherId);
        httpPost.setHeader(HttpUtils.HDRS_MESSAGE, HttpUtils.MSG_INIT);
        httpPost.setHeader(HttpUtils.HDRS_MODE, HttpUtils.MSG_LIVE);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_DIGEST, "");
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_XML_COMPRESSION, HttpUtils.SUPPORTED_XML_COMPRESSIONS[0]);
        httpPost.setHeader(HttpUtils.HDRS_DATA_CLASS, HttpUtils.JOURNAL);
        httpPost.setHeader(HttpUtils.HDRS_VERSION, HttpUtils.SUPPORTED_VERSIONS[0]);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_CONFIGURE_DIGEST_CHALLENGE, HttpUtils.MSG_CONFIGURE_DIGEST_ON);
        httpPost.setHeader(HttpUtils.HDRS_CLIENT_CERTIFICATE, TestResources.getCertForHeader(resourcesDirectory.getAbsolutePath()));

        HttpClient client = HttpClientBuilder.create().build();

        final HttpResponse response = client.execute(httpPost);
        final String responseMessage = response.getFirstHeader(HttpUtils.HDRS_MESSAGE).getValue();
        final int responseStatus = response.getStatusLine().getStatusCode();
        assertEquals(200, responseStatus);
        assertEquals("initialize-ack", responseMessage);

        final Header errorHeader = response.getFirstHeader(HttpUtils.HDRS_ERROR_MESSAGE);
        assertNull(errorHeader);

        final Header sessionHeader = response.getFirstHeader(HttpUtils.HDRS_SESSION_ID);
        assertNotNull(sessionHeader);
    }

    @Test
    public void testNullXmlCompressionReturnsInitializeAck() throws ClientProtocolException, IOException {

        UUID publisherUUID = UUID.randomUUID();
        final String publisherId = publisherUUID.toString();
        final HttpPost httpPost = new HttpPost("http://localhost:" + TestResources.HTTP_PORT + HttpUtils.JOURNAL_ENDPOINT);
        httpPost.setHeader(HttpUtils.HDRS_CONTENT_TYPE, HttpUtils.DEFAULT_CONTENT_TYPE);
        httpPost.setHeader(HttpUtils.HDRS_PUBLISHER_ID, publisherId);
        httpPost.setHeader(HttpUtils.HDRS_MESSAGE, HttpUtils.MSG_INIT);
        httpPost.setHeader(HttpUtils.HDRS_MODE, HttpUtils.MSG_LIVE);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_DIGEST, DigestMethod.SHA256);
        httpPost.setHeader(HttpUtils.HDRS_DATA_CLASS, HttpUtils.JOURNAL);
        httpPost.setHeader(HttpUtils.HDRS_VERSION, HttpUtils.SUPPORTED_VERSIONS[0]);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_CONFIGURE_DIGEST_CHALLENGE, HttpUtils.MSG_CONFIGURE_DIGEST_ON);
        httpPost.setHeader(HttpUtils.HDRS_CLIENT_CERTIFICATE, TestResources.getCertForHeader(resourcesDirectory.getAbsolutePath()));

        HttpClient client = HttpClientBuilder.create().build();

        final HttpResponse response = client.execute(httpPost);
        final String responseMessage = response.getFirstHeader(HttpUtils.HDRS_MESSAGE).getValue();
        final int responseStatus = response.getStatusLine().getStatusCode();
        assertEquals(200, responseStatus);
        assertEquals("initialize-ack", responseMessage);

        final Header errorHeader = response.getFirstHeader(HttpUtils.HDRS_ERROR_MESSAGE);
        assertNull(errorHeader);

        final Header sessionHeader = response.getFirstHeader(HttpUtils.HDRS_SESSION_ID);
        assertNotNull(sessionHeader);
    }

    @Test
    public void testEmptyXmlCompressionReturnsInitializeAck() throws ClientProtocolException, IOException {

        UUID publisherUUID = UUID.randomUUID();
        final String publisherId = publisherUUID.toString();
        final HttpPost httpPost = new HttpPost("http://localhost:" + TestResources.HTTP_PORT + HttpUtils.JOURNAL_ENDPOINT);
        httpPost.setHeader(HttpUtils.HDRS_CONTENT_TYPE, HttpUtils.DEFAULT_CONTENT_TYPE);
        httpPost.setHeader(HttpUtils.HDRS_PUBLISHER_ID, publisherId);
        httpPost.setHeader(HttpUtils.HDRS_MESSAGE, HttpUtils.MSG_INIT);
        httpPost.setHeader(HttpUtils.HDRS_MODE, HttpUtils.MSG_LIVE);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_DIGEST, DigestMethod.SHA256);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_XML_COMPRESSION, "");
        httpPost.setHeader(HttpUtils.HDRS_DATA_CLASS, HttpUtils.JOURNAL);
        httpPost.setHeader(HttpUtils.HDRS_VERSION, HttpUtils.SUPPORTED_VERSIONS[0]);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_CONFIGURE_DIGEST_CHALLENGE, HttpUtils.MSG_CONFIGURE_DIGEST_ON);
        httpPost.setHeader(HttpUtils.HDRS_CLIENT_CERTIFICATE, TestResources.getCertForHeader(resourcesDirectory.getAbsolutePath()));

        HttpClient client = HttpClientBuilder.create().build();

        final HttpResponse response = client.execute(httpPost);
        final String responseMessage = response.getFirstHeader(HttpUtils.HDRS_MESSAGE).getValue();
        final int responseStatus = response.getStatusLine().getStatusCode();
        assertEquals(200, responseStatus);
        assertEquals("initialize-ack", responseMessage);

        final Header errorHeader = response.getFirstHeader(HttpUtils.HDRS_ERROR_MESSAGE);
        assertNull(errorHeader);

        final Header sessionHeader = response.getFirstHeader(HttpUtils.HDRS_SESSION_ID);
        assertNotNull(sessionHeader);
    }

    @Test
    public void testNullConfigureDigestChallengeReturnsInitializeAck() throws ClientProtocolException, IOException {

        UUID publisherUUID = UUID.randomUUID();
        final String publisherId = publisherUUID.toString();
        final HttpPost httpPost = new HttpPost("http://localhost:" + TestResources.HTTP_PORT + HttpUtils.JOURNAL_ENDPOINT);
        httpPost.setHeader(HttpUtils.HDRS_CONTENT_TYPE, HttpUtils.DEFAULT_CONTENT_TYPE);
        httpPost.setHeader(HttpUtils.HDRS_PUBLISHER_ID, publisherId);
        httpPost.setHeader(HttpUtils.HDRS_MESSAGE, HttpUtils.MSG_INIT);
        httpPost.setHeader(HttpUtils.HDRS_MODE, HttpUtils.MSG_LIVE);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_DIGEST, DigestMethod.SHA256);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_XML_COMPRESSION, HttpUtils.SUPPORTED_XML_COMPRESSIONS[0]);
        httpPost.setHeader(HttpUtils.HDRS_DATA_CLASS, HttpUtils.JOURNAL);
        httpPost.setHeader(HttpUtils.HDRS_VERSION, HttpUtils.SUPPORTED_VERSIONS[0]);
        httpPost.setHeader(HttpUtils.HDRS_CLIENT_CERTIFICATE, TestResources.getCertForHeader(resourcesDirectory.getAbsolutePath()));

        HttpClient client = HttpClientBuilder.create().build();

        final HttpResponse response = client.execute(httpPost);
        final String responseMessage = response.getFirstHeader(HttpUtils.HDRS_MESSAGE).getValue();
        final int responseStatus = response.getStatusLine().getStatusCode();
        assertEquals(200, responseStatus);
        assertEquals("initialize-ack", responseMessage);

        final Header errorHeader = response.getFirstHeader(HttpUtils.HDRS_ERROR_MESSAGE);
        assertNull(errorHeader);

        final Header sessionHeader = response.getFirstHeader(HttpUtils.HDRS_SESSION_ID);
        assertNotNull(sessionHeader);
    }

    @Test
    public void testEmptyConfigureDigestChallengeReturnsInitializeAck() throws ClientProtocolException, IOException {

        UUID publisherUUID = UUID.randomUUID();
        final String publisherId = publisherUUID.toString();
        final HttpPost httpPost = new HttpPost("http://localhost:" + TestResources.HTTP_PORT + HttpUtils.JOURNAL_ENDPOINT);
        httpPost.setHeader(HttpUtils.HDRS_CONTENT_TYPE, HttpUtils.DEFAULT_CONTENT_TYPE);
        httpPost.setHeader(HttpUtils.HDRS_PUBLISHER_ID, publisherId);
        httpPost.setHeader(HttpUtils.HDRS_MESSAGE, HttpUtils.MSG_INIT);
        httpPost.setHeader(HttpUtils.HDRS_MODE, HttpUtils.MSG_LIVE);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_DIGEST, DigestMethod.SHA256);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_XML_COMPRESSION, HttpUtils.SUPPORTED_XML_COMPRESSIONS[0]);
        httpPost.setHeader(HttpUtils.HDRS_DATA_CLASS, HttpUtils.JOURNAL);
        httpPost.setHeader(HttpUtils.HDRS_VERSION, HttpUtils.SUPPORTED_VERSIONS[0]);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_CONFIGURE_DIGEST_CHALLENGE, "");
        httpPost.setHeader(HttpUtils.HDRS_CLIENT_CERTIFICATE, TestResources.getCertForHeader(resourcesDirectory.getAbsolutePath()));

        HttpClient client = HttpClientBuilder.create().build();

        final HttpResponse response = client.execute(httpPost);
        final String responseMessage = response.getFirstHeader(HttpUtils.HDRS_MESSAGE).getValue();
        final int responseStatus = response.getStatusLine().getStatusCode();
        assertEquals(200, responseStatus);
        assertEquals("initialize-ack", responseMessage);

        final Header errorHeader = response.getFirstHeader(HttpUtils.HDRS_ERROR_MESSAGE);
        assertNull(errorHeader);

        final Header sessionHeader = response.getFirstHeader(HttpUtils.HDRS_SESSION_ID);
        assertNotNull(sessionHeader);
    }

    @Test
    public void testInitializeWithListsReturnsInitializeAck() throws ClientProtocolException, IOException {

        UUID publisherUUID = UUID.randomUUID();
        final String publisherId = publisherUUID.toString();
        final HttpPost httpPost = new HttpPost("http://localhost:" + TestResources.HTTP_PORT + HttpUtils.JOURNAL_ENDPOINT);
        httpPost.setHeader(HttpUtils.HDRS_CONTENT_TYPE, HttpUtils.DEFAULT_CONTENT_TYPE);
        httpPost.setHeader(HttpUtils.HDRS_PUBLISHER_ID, publisherId);
        httpPost.setHeader(HttpUtils.HDRS_MESSAGE, HttpUtils.MSG_INIT);
        httpPost.setHeader(HttpUtils.HDRS_MODE, HttpUtils.MSG_LIVE);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_DIGEST, DigestMethod.SHA256);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_XML_COMPRESSION, String.join(",", HttpUtils.SUPPORTED_XML_COMPRESSIONS[0]));
        httpPost.setHeader(HttpUtils.HDRS_DATA_CLASS, HttpUtils.JOURNAL);
        httpPost.setHeader(HttpUtils.HDRS_VERSION, HttpUtils.SUPPORTED_VERSIONS[0]);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_CONFIGURE_DIGEST_CHALLENGE, String.join(",", HttpUtils.MSG_CONFIGURE_DIGEST_ON));
        httpPost.setHeader(HttpUtils.HDRS_CLIENT_CERTIFICATE, TestResources.getCertForHeader(resourcesDirectory.getAbsolutePath()));

        HttpClient client = HttpClientBuilder.create().build();

        final HttpResponse response = client.execute(httpPost);
        final String responseMessage = response.getFirstHeader(HttpUtils.HDRS_MESSAGE).getValue();
        final int responseStatus = response.getStatusLine().getStatusCode();
        assertEquals(200, responseStatus);
        assertEquals("initialize-ack", responseMessage);

        final Header errorHeader = response.getFirstHeader(HttpUtils.HDRS_ERROR_MESSAGE);
        assertNull(errorHeader);

        final Header sessionHeader = response.getFirstHeader(HttpUtils.HDRS_SESSION_ID);
        assertNotNull(sessionHeader);
    }

    @Test
    public void testInvalidPublisherIdReturnsInitializeNack() throws ClientProtocolException, IOException {

        final HttpPost httpPost = new HttpPost("http://localhost:" + TestResources.HTTP_PORT + HttpUtils.JOURNAL_ENDPOINT);
        httpPost.setHeader(HttpUtils.HDRS_CONTENT_TYPE, HttpUtils.DEFAULT_CONTENT_TYPE);
        httpPost.setHeader(HttpUtils.HDRS_PUBLISHER_ID, "");
        httpPost.setHeader(HttpUtils.HDRS_MESSAGE, HttpUtils.MSG_INIT);
        httpPost.setHeader(HttpUtils.HDRS_MODE, HttpUtils.MSG_LIVE);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_DIGEST, DigestMethod.SHA256);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_XML_COMPRESSION, String.join(",", HttpUtils.SUPPORTED_XML_COMPRESSIONS[0]));
        httpPost.setHeader(HttpUtils.HDRS_DATA_CLASS, HttpUtils.JOURNAL);
        httpPost.setHeader(HttpUtils.HDRS_VERSION, HttpUtils.SUPPORTED_VERSIONS[0]);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_CONFIGURE_DIGEST_CHALLENGE, String.join(",", HttpUtils.MSG_CONFIGURE_DIGEST_ON));
        httpPost.setHeader(HttpUtils.HDRS_CLIENT_CERTIFICATE, TestResources.getCertForHeader(resourcesDirectory.getAbsolutePath()));

        HttpClient client = HttpClientBuilder.create().build();

        final HttpResponse response = client.execute(httpPost);
        final String responseMessage = response.getFirstHeader(HttpUtils.HDRS_MESSAGE).getValue();
        final int responseStatus = response.getStatusLine().getStatusCode();
        assertEquals(200, responseStatus);
        assertEquals("initialize-nack", responseMessage);

        final Header errorHeader = response.getFirstHeader(HttpUtils.HDRS_ERROR_MESSAGE);
        assertNotNull(errorHeader);
        assertEquals(HttpUtils.HDRS_UNSUPPORTED_PUBLISHER_ID, errorHeader.getValue());

        final Header sessionHeader = response.getFirstHeader(HttpUtils.HDRS_SESSION_ID);
        assertNull(sessionHeader);
    }

    @Test
    public void testMissingCertReturnsInitializeNack() throws ClientProtocolException, IOException {
        UUID publisherUUID = UUID.randomUUID();
        final String publisherId = publisherUUID.toString();
        final HttpPost httpPost = new HttpPost("http://localhost:" + TestResources.HTTP_PORT + HttpUtils.JOURNAL_ENDPOINT);
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
        assertEquals("initialize-nack", responseMessage);

        final Header errorHeader = response.getFirstHeader(HttpUtils.HDRS_ERROR_MESSAGE);
        assertNotNull(errorHeader);
        assertEquals(HttpUtils.HDRS_INVALID_USER_CERT, errorHeader.getValue());

        final Header sessionHeader = response.getFirstHeader(HttpUtils.HDRS_SESSION_ID);
        assertNull(sessionHeader);
    }

    @Test
    public void testDuplicatePublisherWithDifferentCertReturnsInitializeNack() throws ClientProtocolException, IOException {

        UUID publisherUUID = UUID.randomUUID();
        final String publisherId = publisherUUID.toString();
        final HttpPost httpPost = new HttpPost("http://localhost:" + TestResources.HTTP_PORT + HttpUtils.AUDIT_ENDPOINT);
        httpPost.setHeader(HttpUtils.HDRS_CONTENT_TYPE, HttpUtils.DEFAULT_CONTENT_TYPE);
        httpPost.setHeader(HttpUtils.HDRS_PUBLISHER_ID, publisherId);
        httpPost.setHeader(HttpUtils.HDRS_MESSAGE, HttpUtils.MSG_INIT);
        httpPost.setHeader(HttpUtils.HDRS_MODE, HttpUtils.MSG_LIVE);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_DIGEST, DigestMethod.SHA256);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_XML_COMPRESSION, HttpUtils.SUPPORTED_XML_COMPRESSIONS[0]);
        httpPost.setHeader(HttpUtils.HDRS_DATA_CLASS, HttpUtils.AUDIT);
        httpPost.setHeader(HttpUtils.HDRS_VERSION, HttpUtils.SUPPORTED_VERSIONS[0]);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_CONFIGURE_DIGEST_CHALLENGE, HttpUtils.MSG_CONFIGURE_DIGEST_ON);
        httpPost.setHeader(HttpUtils.HDRS_CLIENT_CERTIFICATE, TestResources.getCertForHeader(resourcesDirectory.getAbsolutePath()));

        HttpClient client = HttpClientBuilder.create().build();

        final HttpResponse response = client.execute(httpPost);
        final String responseMessage = response.getFirstHeader(HttpUtils.HDRS_MESSAGE).getValue();
        final int responseStatus = response.getStatusLine().getStatusCode();
        assertEquals(200, responseStatus);
        assertEquals("initialize-ack", responseMessage);

        final Header errorHeader = response.getFirstHeader(HttpUtils.HDRS_ERROR_MESSAGE);
        assertNull(errorHeader);
        final Header sessionHeader = response.getFirstHeader(HttpUtils.HDRS_SESSION_ID);
        assertNotNull(sessionHeader);

        httpPost.setHeader(HttpUtils.HDRS_CLIENT_CERTIFICATE, TestResources.getCertForHeader(TestResources.DIFFERENT_CERT_FILENAME, resourcesDirectory.getAbsolutePath()));
        final HttpResponse nextResponse = client.execute(httpPost);
        final String nextResponseMessage = nextResponse.getFirstHeader(HttpUtils.HDRS_MESSAGE).getValue();
        final int nextResponseStatus = nextResponse.getStatusLine().getStatusCode();
        assertEquals(200, nextResponseStatus);
        assertEquals("initialize-nack", nextResponseMessage);

        final Header errorHeader2 = nextResponse.getFirstHeader(HttpUtils.HDRS_ERROR_MESSAGE);
        assertNotNull(errorHeader2);
        assertEquals(HttpUtils.HDRS_SESSION_ALREADY_EXISTS, errorHeader2.getValue());

        final Header sessionHeader2 = nextResponse.getFirstHeader(HttpUtils.HDRS_SESSION_ID);
        assertNull(sessionHeader2);
    }

    @Test
    public void testDuplicatePublisherWithSameCertReturnsInitializeAck() throws ClientProtocolException, IOException {

        UUID publisherUUID = UUID.randomUUID();
        final String publisherId = publisherUUID.toString();
        final HttpPost httpPost = new HttpPost("http://localhost:" + TestResources.HTTP_PORT + HttpUtils.AUDIT_ENDPOINT);
        httpPost.setHeader(HttpUtils.HDRS_CONTENT_TYPE, HttpUtils.DEFAULT_CONTENT_TYPE);
        httpPost.setHeader(HttpUtils.HDRS_PUBLISHER_ID, publisherId);
        httpPost.setHeader(HttpUtils.HDRS_MESSAGE, HttpUtils.MSG_INIT);
        httpPost.setHeader(HttpUtils.HDRS_MODE, HttpUtils.MSG_LIVE);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_DIGEST, DigestMethod.SHA256);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_XML_COMPRESSION, HttpUtils.SUPPORTED_XML_COMPRESSIONS[0]);
        httpPost.setHeader(HttpUtils.HDRS_DATA_CLASS, HttpUtils.AUDIT);
        httpPost.setHeader(HttpUtils.HDRS_VERSION, HttpUtils.SUPPORTED_VERSIONS[0]);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_CONFIGURE_DIGEST_CHALLENGE, HttpUtils.MSG_CONFIGURE_DIGEST_ON);
        httpPost.setHeader(HttpUtils.HDRS_CLIENT_CERTIFICATE, TestResources.getCertForHeader(resourcesDirectory.getAbsolutePath()));

        HttpClient client = HttpClientBuilder.create().build();

        final HttpResponse response = client.execute(httpPost);
        final String responseMessage = response.getFirstHeader(HttpUtils.HDRS_MESSAGE).getValue();
        final String firstSessionId = response.getFirstHeader(HttpUtils.HDRS_SESSION_ID).getValue();
        final int responseStatus = response.getStatusLine().getStatusCode();
        assertEquals(200, responseStatus);
        assertEquals("initialize-ack", responseMessage);

        final Header errorHeader = response.getFirstHeader(HttpUtils.HDRS_ERROR_MESSAGE);
        assertNull(errorHeader);

        final HttpResponse nextResponse = client.execute(httpPost);
        final String nextResponseMessage = nextResponse.getFirstHeader(HttpUtils.HDRS_MESSAGE).getValue();
        final String secondSessionId = nextResponse.getFirstHeader(HttpUtils.HDRS_SESSION_ID).getValue();
        final int nextResponseStatus = nextResponse.getStatusLine().getStatusCode();
        assertEquals(200, nextResponseStatus);
        assertEquals("initialize-ack", nextResponseMessage);
        assertFalse(firstSessionId.equals(secondSessionId));

        final Header errorHeader2 = nextResponse.getFirstHeader(HttpUtils.HDRS_ERROR_MESSAGE);
        assertNull(errorHeader2);
    }

    @Test
    public void testInvalidModeReturnsInitializeNack() throws ClientProtocolException, IOException {

        UUID publisherUUID = UUID.randomUUID();
        final String publisherId = publisherUUID.toString();
        final HttpPost httpPost = new HttpPost("http://localhost:" + TestResources.HTTP_PORT + HttpUtils.JOURNAL_ENDPOINT);
        httpPost.setHeader(HttpUtils.HDRS_CONTENT_TYPE, HttpUtils.DEFAULT_CONTENT_TYPE);
        httpPost.setHeader(HttpUtils.HDRS_PUBLISHER_ID, publisherId);
        httpPost.setHeader(HttpUtils.HDRS_MESSAGE, HttpUtils.MSG_INIT);
        httpPost.setHeader(HttpUtils.HDRS_MODE, "invalid");
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_DIGEST, DigestMethod.SHA256);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_XML_COMPRESSION, HttpUtils.SUPPORTED_XML_COMPRESSIONS[0]);
        httpPost.setHeader(HttpUtils.HDRS_DATA_CLASS, HttpUtils.JOURNAL);
        httpPost.setHeader(HttpUtils.HDRS_VERSION, HttpUtils.SUPPORTED_VERSIONS[0]);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_CONFIGURE_DIGEST_CHALLENGE, HttpUtils.MSG_CONFIGURE_DIGEST_ON);
        httpPost.setHeader(HttpUtils.HDRS_CLIENT_CERTIFICATE, TestResources.getCertForHeader(resourcesDirectory.getAbsolutePath()));

        HttpClient client = HttpClientBuilder.create().build();

        final HttpResponse response = client.execute(httpPost);
        final String responseMessage = response.getFirstHeader(HttpUtils.HDRS_MESSAGE).getValue();
        final int responseStatus = response.getStatusLine().getStatusCode();
        assertEquals(200, responseStatus);
        assertEquals("initialize-nack", responseMessage);

        final Header errorHeader = response.getFirstHeader(HttpUtils.HDRS_ERROR_MESSAGE);
        assertNotNull(errorHeader);
        assertEquals(HttpUtils.HDRS_UNSUPPORTED_MODE, errorHeader.getValue());

        final Header sessionHeader = response.getFirstHeader(HttpUtils.HDRS_SESSION_ID);
        assertNull(sessionHeader);
    }

    @Test
    public void testInvalidDigestReturnsInitializeNack() throws ClientProtocolException, IOException {

        UUID publisherUUID = UUID.randomUUID();
        final String publisherId = publisherUUID.toString();
        final HttpPost httpPost = new HttpPost("http://localhost:" + TestResources.HTTP_PORT + HttpUtils.JOURNAL_ENDPOINT);
        httpPost.setHeader(HttpUtils.HDRS_CONTENT_TYPE, HttpUtils.DEFAULT_CONTENT_TYPE);
        httpPost.setHeader(HttpUtils.HDRS_PUBLISHER_ID, publisherId);
        httpPost.setHeader(HttpUtils.HDRS_MESSAGE, HttpUtils.MSG_INIT);
        httpPost.setHeader(HttpUtils.HDRS_MODE, HttpUtils.MSG_LIVE);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_DIGEST, "invalid");
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_XML_COMPRESSION, HttpUtils.SUPPORTED_XML_COMPRESSIONS[0]);
        httpPost.setHeader(HttpUtils.HDRS_DATA_CLASS, HttpUtils.JOURNAL);
        httpPost.setHeader(HttpUtils.HDRS_VERSION, HttpUtils.SUPPORTED_VERSIONS[0]);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_CONFIGURE_DIGEST_CHALLENGE, HttpUtils.MSG_CONFIGURE_DIGEST_ON);
        httpPost.setHeader(HttpUtils.HDRS_CLIENT_CERTIFICATE, TestResources.getCertForHeader(resourcesDirectory.getAbsolutePath()));

        HttpClient client = HttpClientBuilder.create().build();

        final HttpResponse response = client.execute(httpPost);
        final String responseMessage = response.getFirstHeader(HttpUtils.HDRS_MESSAGE).getValue();
        final int responseStatus = response.getStatusLine().getStatusCode();
        assertEquals(200, responseStatus);
        assertEquals("initialize-nack", responseMessage);

        final Header errorHeader = response.getFirstHeader(HttpUtils.HDRS_ERROR_MESSAGE);
        assertNotNull(errorHeader);
        assertEquals(HttpUtils.HDRS_UNSUPPORTED_DIGEST, errorHeader.getValue());

        final Header sessionHeader = response.getFirstHeader(HttpUtils.HDRS_SESSION_ID);
        assertNull(sessionHeader);
    }

    @Test
    public void testInvalidXmlCompressionReturnsInitializeNack() throws ClientProtocolException, IOException {

        UUID publisherUUID = UUID.randomUUID();
        final String publisherId = publisherUUID.toString();
        final HttpPost httpPost = new HttpPost("http://localhost:" + TestResources.HTTP_PORT + HttpUtils.JOURNAL_ENDPOINT);
        httpPost.setHeader(HttpUtils.HDRS_CONTENT_TYPE, HttpUtils.DEFAULT_CONTENT_TYPE);
        httpPost.setHeader(HttpUtils.HDRS_PUBLISHER_ID, publisherId);
        httpPost.setHeader(HttpUtils.HDRS_MESSAGE, HttpUtils.MSG_INIT);
        httpPost.setHeader(HttpUtils.HDRS_MODE, HttpUtils.MSG_LIVE);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_DIGEST, DigestMethod.SHA256);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_XML_COMPRESSION, "invalid");
        httpPost.setHeader(HttpUtils.HDRS_DATA_CLASS, HttpUtils.JOURNAL);
        httpPost.setHeader(HttpUtils.HDRS_VERSION, HttpUtils.SUPPORTED_VERSIONS[0]);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_CONFIGURE_DIGEST_CHALLENGE, HttpUtils.MSG_CONFIGURE_DIGEST_ON);
        httpPost.setHeader(HttpUtils.HDRS_CLIENT_CERTIFICATE, TestResources.getCertForHeader(resourcesDirectory.getAbsolutePath()));

        HttpClient client = HttpClientBuilder.create().build();

        final HttpResponse response = client.execute(httpPost);
        final String responseMessage = response.getFirstHeader(HttpUtils.HDRS_MESSAGE).getValue();
        final int responseStatus = response.getStatusLine().getStatusCode();
        assertEquals(200, responseStatus);
        assertEquals("initialize-nack", responseMessage);

        final Header errorHeader = response.getFirstHeader(HttpUtils.HDRS_ERROR_MESSAGE);
        assertNotNull(errorHeader);
        assertEquals(HttpUtils.HDRS_UNSUPPORTED_XML_COMPRESSION, errorHeader.getValue());

        final Header sessionHeader = response.getFirstHeader(HttpUtils.HDRS_SESSION_ID);
        assertNull(sessionHeader);
    }

    @Test
    public void testInvalidDataClassReturnsInitializeNack() throws ClientProtocolException, IOException {

        UUID publisherUUID = UUID.randomUUID();
        final String publisherId = publisherUUID.toString();
        final HttpPost httpPost = new HttpPost("http://localhost:" + TestResources.HTTP_PORT + HttpUtils.JOURNAL_ENDPOINT);
        httpPost.setHeader(HttpUtils.HDRS_CONTENT_TYPE, HttpUtils.DEFAULT_CONTENT_TYPE);
        httpPost.setHeader(HttpUtils.HDRS_PUBLISHER_ID, publisherId);
        httpPost.setHeader(HttpUtils.HDRS_MESSAGE, HttpUtils.MSG_INIT);
        httpPost.setHeader(HttpUtils.HDRS_MODE, HttpUtils.MSG_LIVE);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_DIGEST, DigestMethod.SHA256);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_XML_COMPRESSION, HttpUtils.SUPPORTED_XML_COMPRESSIONS[0]);
        httpPost.setHeader(HttpUtils.HDRS_DATA_CLASS, "invalid");
        httpPost.setHeader(HttpUtils.HDRS_VERSION, HttpUtils.SUPPORTED_VERSIONS[0]);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_CONFIGURE_DIGEST_CHALLENGE, HttpUtils.MSG_CONFIGURE_DIGEST_ON);
        httpPost.setHeader(HttpUtils.HDRS_CLIENT_CERTIFICATE, TestResources.getCertForHeader(resourcesDirectory.getAbsolutePath()));

        HttpClient client = HttpClientBuilder.create().build();

        final HttpResponse response = client.execute(httpPost);
        final String responseMessage = response.getFirstHeader(HttpUtils.HDRS_MESSAGE).getValue();
        final int responseStatus = response.getStatusLine().getStatusCode();
        assertEquals(200, responseStatus);
        assertEquals("initialize-nack", responseMessage);

        final Header errorHeader = response.getFirstHeader(HttpUtils.HDRS_ERROR_MESSAGE);
        assertNotNull(errorHeader);
        assertEquals(HttpUtils.HDRS_UNSUPPORTED_DATACLASS, errorHeader.getValue());

        final Header sessionHeader = response.getFirstHeader(HttpUtils.HDRS_SESSION_ID);
        assertNull(sessionHeader);
    }

    @Test
    public void testInvalidVersionReturnsInitializeNack() throws ClientProtocolException, IOException {

        UUID publisherUUID = UUID.randomUUID();
        final String publisherId = publisherUUID.toString();
        final HttpPost httpPost = new HttpPost("http://localhost:" + TestResources.HTTP_PORT + HttpUtils.JOURNAL_ENDPOINT);
        httpPost.setHeader(HttpUtils.HDRS_CONTENT_TYPE, HttpUtils.DEFAULT_CONTENT_TYPE);
        httpPost.setHeader(HttpUtils.HDRS_PUBLISHER_ID, publisherId);
        httpPost.setHeader(HttpUtils.HDRS_MESSAGE, HttpUtils.MSG_INIT);
        httpPost.setHeader(HttpUtils.HDRS_MODE, HttpUtils.MSG_LIVE);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_DIGEST, DigestMethod.SHA256);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_XML_COMPRESSION, HttpUtils.SUPPORTED_XML_COMPRESSIONS[0]);
        httpPost.setHeader(HttpUtils.HDRS_DATA_CLASS, HttpUtils.JOURNAL);
        httpPost.setHeader(HttpUtils.HDRS_VERSION,"invalid");
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_CONFIGURE_DIGEST_CHALLENGE, HttpUtils.MSG_CONFIGURE_DIGEST_ON);
        httpPost.setHeader(HttpUtils.HDRS_CLIENT_CERTIFICATE, TestResources.getCertForHeader(resourcesDirectory.getAbsolutePath()));

        HttpClient client = HttpClientBuilder.create().build();

        final HttpResponse response = client.execute(httpPost);
        final String responseMessage = response.getFirstHeader(HttpUtils.HDRS_MESSAGE).getValue();
        final int responseStatus = response.getStatusLine().getStatusCode();
        assertEquals(200, responseStatus);
        assertEquals("initialize-nack", responseMessage);

        final Header errorHeader = response.getFirstHeader(HttpUtils.HDRS_ERROR_MESSAGE);
        assertNotNull(errorHeader);
        assertEquals(HttpUtils.HDRS_UNSUPPORTED_VERSION, errorHeader.getValue());

        final Header sessionHeader = response.getFirstHeader(HttpUtils.HDRS_SESSION_ID);
        assertNull(sessionHeader);
    }

    @Test
    public void testInvalidConfigureDigestChallengeReturnsInitializeNack() throws ClientProtocolException, IOException {

        UUID publisherUUID = UUID.randomUUID();
        final String publisherId = publisherUUID.toString();
        final HttpPost httpPost = new HttpPost("http://localhost:" + TestResources.HTTP_PORT + HttpUtils.JOURNAL_ENDPOINT);
        httpPost.setHeader(HttpUtils.HDRS_CONTENT_TYPE, HttpUtils.DEFAULT_CONTENT_TYPE);
        httpPost.setHeader(HttpUtils.HDRS_PUBLISHER_ID, publisherId);
        httpPost.setHeader(HttpUtils.HDRS_MESSAGE, HttpUtils.MSG_INIT);
        httpPost.setHeader(HttpUtils.HDRS_MODE, HttpUtils.MSG_LIVE);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_DIGEST, DigestMethod.SHA256);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_XML_COMPRESSION, HttpUtils.SUPPORTED_XML_COMPRESSIONS[0]);
        httpPost.setHeader(HttpUtils.HDRS_DATA_CLASS, HttpUtils.JOURNAL);
        httpPost.setHeader(HttpUtils.HDRS_VERSION, HttpUtils.SUPPORTED_VERSIONS[0]);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_CONFIGURE_DIGEST_CHALLENGE, "invalid");
        httpPost.setHeader(HttpUtils.HDRS_CLIENT_CERTIFICATE, TestResources.getCertForHeader(resourcesDirectory.getAbsolutePath()));

        HttpClient client = HttpClientBuilder.create().build();

        final HttpResponse response = client.execute(httpPost);
        final String responseMessage = response.getFirstHeader(HttpUtils.HDRS_MESSAGE).getValue();
        final int responseStatus = response.getStatusLine().getStatusCode();
        assertEquals(200, responseStatus);
        assertEquals("initialize-nack", responseMessage);

        final Header errorHeader = response.getFirstHeader(HttpUtils.HDRS_ERROR_MESSAGE);
        assertNotNull(errorHeader);
        assertEquals(HttpUtils.HDRS_UNSUPPORTED_CONFIGURE_DIGEST_CHALLENGE, errorHeader.getValue());

        final Header sessionHeader = response.getFirstHeader(HttpUtils.HDRS_SESSION_ID);
        assertNull(sessionHeader);
    }

    @Test
    /**
     * Testing that when we send a JOURNAL_MISSING message to the Journal end point that it returns an HTTP status of OK (200)
     * and a message of JOURNAL_MISSING_RESPONSE
     * @throws ClientProtocolException
     * @throws IOException
     */
    public void testProcessJournalMissingMessageToJournalEndPoint() throws ClientProtocolException, IOException
    {
        UUID publisherUUID = UUID.randomUUID();
        final String publisherId = publisherUUID.toString();
        String sessionID = TestResources.sendValidInitialize(RecordType.Journal, false, publisherId, resourcesDirectory.getAbsolutePath());

        final HttpPost httpPost = new HttpPost("http://localhost:" + TestResources.HTTP_PORT + HttpUtils.JOURNAL_ENDPOINT);
        httpPost.setHeader(HttpUtils.HDRS_SESSION_ID, sessionID);
        httpPost.setHeader(HttpUtils.HDRS_MESSAGE, HttpUtils.MSG_JOURNAL_MISSING);
        httpPost.setHeader(HttpUtils.HDRS_NONCE, "1000"); //sets the jalID

        HttpClient client = HttpClientBuilder.create().build();

        final HttpResponse response = client.execute(httpPost);
        final String responseMessage = response.getFirstHeader(HttpUtils.HDRS_MESSAGE).getValue();
        final int responseStatus = response.getStatusLine().getStatusCode();
        assertEquals(200, responseStatus);
        assertEquals(HttpUtils.MSG_JOURNAL_MISSING_RESPONSE, responseMessage);
    }

    @Test
    /**
     * Testing that if we send a JOURNAL_MISSING message to an audit endpoint that we get back an HTTP status of Bad Request (400)
     * @throws ClientProtocolException
     * @throws IOException
     */
    public void testProcessJournalMissingMessageToAuditEndPoint() throws ClientProtocolException, IOException
    {
        UUID publisherUUID = UUID.randomUUID();
        final String publisherId = publisherUUID.toString();
        String sessionID = TestResources.sendValidInitialize(RecordType.Audit, false, publisherId, resourcesDirectory.getAbsolutePath());

        final HttpPost httpPost = new HttpPost("http://localhost:" + TestResources.HTTP_PORT + HttpUtils.AUDIT_ENDPOINT);
        httpPost.setHeader(HttpUtils.HDRS_SESSION_ID, sessionID);
        httpPost.setHeader(HttpUtils.HDRS_MESSAGE, HttpUtils.MSG_JOURNAL_MISSING);
        httpPost.setHeader(HttpUtils.HDRS_NONCE, "1000"); //sets the jalID

        HttpClient client = HttpClientBuilder.create().build();

        final HttpResponse response = client.execute(httpPost);
        final int responseStatus = response.getStatusLine().getStatusCode();
        assertNull(response.getFirstHeader(HttpUtils.HDRS_MESSAGE));
        assertNotNull(HttpUtils.HDRS_ERROR_MESSAGE);
        assertEquals(200, responseStatus);
    }

    @Test
    /**
     * Testing that if we send a JOURNAL_MISSING message to an log endpoint that we get back an HTTP status of Bad Request (400)
     * @throws ClientProtocolException
     * @throws IOException
     */
    public void testProcessJournalMissingMessageToLogEndPoint() throws ClientProtocolException, IOException
    {
        UUID publisherUUID = UUID.randomUUID();
        final String publisherId = publisherUUID.toString();
        String sessionID = TestResources.sendValidInitialize(RecordType.Log, false, publisherId, resourcesDirectory.getAbsolutePath());

        final HttpPost httpPost = new HttpPost("http://localhost:" + TestResources.HTTP_PORT + HttpUtils.LOG_ENDPOINT);
        httpPost.setHeader(HttpUtils.HDRS_SESSION_ID, sessionID);
        httpPost.setHeader(HttpUtils.HDRS_MESSAGE, HttpUtils.MSG_JOURNAL_MISSING);
        httpPost.setHeader(HttpUtils.HDRS_NONCE, "1000"); //sets the jalID

        HttpClient client = HttpClientBuilder.create().build();

        final HttpResponse response = client.execute(httpPost);
        final int responseStatus = response.getStatusLine().getStatusCode();
        assertNull(response.getFirstHeader(HttpUtils.HDRS_MESSAGE));
        assertEquals(200, responseStatus);
    }
}