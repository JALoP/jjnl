package com.tresys.jalop.jnl.impl.http;

import static org.junit.Assert.assertEquals;
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
import org.apache.log4j.Level;
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

        TestResources.configureLogging(Level.OFF);

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
    public void testValidInitializeRequirmentTest() throws ClientProtocolException, IOException {
        System.out.println("----testValidInitializeRequirmentTest - Testing following initialize requirements----");
        System.out.println("DR1.012 - initialize");
        System.out.println("DR1.012.001 - initialize:  Initiated By");
        System.out.println("DR1.012.002 - initialize:  JAL-Version");
        System.out.println("DR1.012.002.001 - initialize:  JAL-Version Values");
        System.out.println("DR1.012.003 - initialize:  JAL-Accept-XML-Compression");
        System.out.println("DR1.012.003.001 - initialize:  JAL-Accept-XML-Compression Required Values");
        System.out.println("DR1.012.003.002 - initialize:  JAL-Accept-XML-Compression Optional Values");
        System.out.println("DR1.012.004 - initialize:  JAL-Accept-Digest");
        System.out.println("DR1.012.004.001 - initialize:  JAL-Accept-Digest Required Values");
        System.out.println("DR1.012.005 - initialize:  JAL-Data-Class");
        System.out.println("DR1.012.005.001 - initialize:  JAL-Data-Class Valid Values");
        System.out.println("DR1.012.006 - initialize:  JAL-Mode");
        System.out.println("DR1.012.006.001 - initialize:  JAL-Mode Valid Values");
        System.out.println("DR1.012.007 - initialize:  JAL-Configure-Digest-Challenge");
        System.out.println("DR1.012.007.001 - initialize:  JAL-Configure-Digest-Challenge Required Values");
        System.out.println("DR1.012.007.002 - initialize:  JAL-Configure-Digest-Challenge Optional Values");
        System.out.println("DR1.014 - initialize-ack");
        System.out.println("DR1.014.001 - initialize-ack:  Communication Accepted");
        System.out.println("DR1.014.001.001 - initialize-ack:  Communication Accepted - JAL-XML-Compression");
        System.out.println("DR1.014.001.002 - initialize-ack:  Communication Accepted - JAL-Digest");
        System.out.println("DR1.014.001.003 - initialize-ack:  Communication Accepted - JAL-Configure-Digest-Challenge");


        UUID publisherUUID = UUID.randomUUID();
        final String publisherId = publisherUUID.toString();
        String [] configureDigests = new String[] {"on", "off"};
        String [] modes = new String[] {"live", "archival"};

        for (RecordType recType : RecordType.values())
        {
            if (recType.equals(RecordType.Unset))
            {
                continue;
            }

            for (String xmlCompression : HttpUtils.SUPPORTED_XML_COMPRESSIONS)
            {
                for (String configureDigest : configureDigests)
                {
                    for (String currMode : modes)
                    {
                        final HttpPost httpPost = new HttpPost("http://localhost:" + TestResources.HTTP_PORT + "/" + recType.toString().toLowerCase());
                        httpPost.setHeader(HttpUtils.HDRS_CONTENT_TYPE, HttpUtils.DEFAULT_CONTENT_TYPE);
                        httpPost.setHeader(HttpUtils.HDRS_PUBLISHER_ID, publisherId);
                        httpPost.setHeader(HttpUtils.HDRS_MESSAGE, HttpUtils.MSG_INIT);
                        httpPost.setHeader(HttpUtils.HDRS_MODE, currMode);
                        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_DIGEST, DigestMethod.SHA256);
                        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_XML_COMPRESSION, xmlCompression);
                        httpPost.setHeader(HttpUtils.HDRS_DATA_CLASS, recType.toString().toLowerCase());
                        httpPost.setHeader(HttpUtils.HDRS_VERSION, "2.0.0.0");
                        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_CONFIGURE_DIGEST_CHALLENGE, configureDigest);


                        HttpClient client = HttpClientBuilder.create().build();

                        final HttpResponse response = client.execute(httpPost);
                        final String responseMessage = response.getFirstHeader(HttpUtils.HDRS_MESSAGE).getValue();
                        final int responseStatus = response.getStatusLine().getStatusCode();
                        final Header errorHeader = response.getFirstHeader(HttpUtils.HDRS_ERROR_MESSAGE);
                        final Header sessionHeader = response.getFirstHeader(HttpUtils.HDRS_SESSION_ID);

                        if (200 != responseStatus || !"initialize-ack".equals(responseMessage) || errorHeader != null || sessionHeader == null)
                        {
                            System.out.println("Failure parameters:");
                            System.out.println("Record Type: " + recType.toString());
                            System.out.println("Mode: " + currMode);
                            System.out.println("Configure Digest " + configureDigest);
                            System.out.println("XML Compression " + xmlCompression);

                            assertEquals(200, responseStatus);
                            assertEquals("initialize-ack", responseMessage);
                            assertNull(errorHeader);
                            assertNotNull(sessionHeader);
                        }

                        final Header digestHeader = response.getFirstHeader(HttpUtils.HDRS_DIGEST);
                        final Header configureDigestHeader = response.getFirstHeader(HttpUtils.HDRS_CONFIGURE_DIGEST_CHALLENGE);
                        final Header xmlCompressionHeader = response.getFirstHeader(HttpUtils.HDRS_XML_COMPRESSION);

                        assertNotNull(digestHeader);
                        assertEquals(DigestMethod.SHA256, digestHeader.getValue());
                        assertNotNull(configureDigestHeader);
                        assertEquals(configureDigest, configureDigestHeader.getValue());
                        assertNotNull(xmlCompressionHeader);
                        assertEquals(xmlCompression, xmlCompressionHeader.getValue());
                    }
                }
            }
        }
        System.out.println("----testValidInitializeRequirmentTest success----\n");
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

        System.out.println("----testNullDigestReturnsInitializeAck----");
        System.out.println("DR1.012.004.003 - initialize:  JAL-Accept-Digest Value Order - Missing JAL-Accept-Digest returns sha256");
        System.out.println("DR1.014.001.002 - initialize-ack:  Communication Accepted - JAL-Digest");

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


        HttpClient client = HttpClientBuilder.create().build();

        final HttpResponse response = client.execute(httpPost);
        final String responseMessage = response.getFirstHeader(HttpUtils.HDRS_MESSAGE).getValue();
        final int responseStatus = response.getStatusLine().getStatusCode();
        assertEquals(200, responseStatus);
        assertEquals("initialize-ack", responseMessage);

        final Header digest = response.getFirstHeader(HttpUtils.HDRS_DIGEST);
        assertNotNull(digest);
        assertEquals(DigestMethod.SHA256, digest.getValue());

        final Header errorHeader = response.getFirstHeader(HttpUtils.HDRS_ERROR_MESSAGE);
        assertNull(errorHeader);

        final Header sessionHeader = response.getFirstHeader(HttpUtils.HDRS_SESSION_ID);
        assertNotNull(sessionHeader);

        System.out.println("----testNullDigestReturnsInitializeAck success----\n");
    }

    @Test
    public void testEmptyDigestReturnsInitializeAck() throws ClientProtocolException, IOException {

        System.out.println("----testEmptyDigestReturnsInitializeAck----");
        System.out.println("DR1.012.004.003 - initialize:  JAL-Accept-Digest Value Order - Empty JAL-Accept-Digest returns sha256");
        System.out.println("DR1.014.001.002 - initialize-ack:  Communication Accepted - JAL-Digest");

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


        HttpClient client = HttpClientBuilder.create().build();

        final HttpResponse response = client.execute(httpPost);
        final String responseMessage = response.getFirstHeader(HttpUtils.HDRS_MESSAGE).getValue();
        final int responseStatus = response.getStatusLine().getStatusCode();
        assertEquals(200, responseStatus);
        assertEquals("initialize-ack", responseMessage);

        final Header digest = response.getFirstHeader(HttpUtils.HDRS_DIGEST);
        assertNotNull(digest);
        assertEquals(DigestMethod.SHA256, digest.getValue());

        final Header errorHeader = response.getFirstHeader(HttpUtils.HDRS_ERROR_MESSAGE);
        assertNull(errorHeader);

        final Header sessionHeader = response.getFirstHeader(HttpUtils.HDRS_SESSION_ID);
        assertNotNull(sessionHeader);

        System.out.println("----testEmptyDigestReturnsInitializeAck success----\n");
    }

    @Test
    public void testNullXmlCompressionReturnsInitializeAck() throws ClientProtocolException, IOException {

        System.out.println("----testNullXmlCompressionReturnsInitializeAck----");
        System.out.println("DR1.012.003.003 - initialize:  JAL-Accept-XML-Compression Value Order - Missing JAL-Accept-XML-Compression returns none");
        System.out.println("DR1.014.001.001 - initialize-ack:  Communication Accepted - JAL-XML-Compression");

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


        HttpClient client = HttpClientBuilder.create().build();

        final HttpResponse response = client.execute(httpPost);
        final String responseMessage = response.getFirstHeader(HttpUtils.HDRS_MESSAGE).getValue();
        final int responseStatus = response.getStatusLine().getStatusCode();
        assertEquals(200, responseStatus);
        assertEquals("initialize-ack", responseMessage);

        final Header xmlCompression = response.getFirstHeader(HttpUtils.HDRS_XML_COMPRESSION);
        assertNotNull(xmlCompression);
        assertEquals("none", xmlCompression.getValue());

        final Header errorHeader = response.getFirstHeader(HttpUtils.HDRS_ERROR_MESSAGE);
        assertNull(errorHeader);

        final Header sessionHeader = response.getFirstHeader(HttpUtils.HDRS_SESSION_ID);
        assertNotNull(sessionHeader);

        System.out.println("----testNullXmlCompressionReturnsInitializeAck success----\n");
    }

    @Test
    public void testEmptyXmlCompressionReturnsInitializeAck() throws ClientProtocolException, IOException {
        System.out.println("----testEmptyXmlCompressionReturnsInitializeAck----");
        System.out.println("DR1.012.003.003 - initialize:  JAL-Accept-XML-Compression Value Order - Empty JAL-Accept-XML-Compression returns none");
        System.out.println("DR1.014.001.001 - initialize-ack:  Communication Accepted - JAL-XML-Compression");

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


        HttpClient client = HttpClientBuilder.create().build();

        final HttpResponse response = client.execute(httpPost);
        final String responseMessage = response.getFirstHeader(HttpUtils.HDRS_MESSAGE).getValue();
        final int responseStatus = response.getStatusLine().getStatusCode();
        assertEquals(200, responseStatus);
        assertEquals("initialize-ack", responseMessage);

        final Header errorHeader = response.getFirstHeader(HttpUtils.HDRS_ERROR_MESSAGE);
        assertNull(errorHeader);

        final Header xmlCompression = response.getFirstHeader(HttpUtils.HDRS_XML_COMPRESSION);
        assertNotNull(xmlCompression);
        assertEquals("none", xmlCompression.getValue());

        final Header sessionHeader = response.getFirstHeader(HttpUtils.HDRS_SESSION_ID);
        assertNotNull(sessionHeader);
        System.out.println("----testEmptyXmlCompressionReturnsInitializeAck success----\n");
    }

    @Test
    public void testNullConfigureDigestChallengeReturnsInitializeAck() throws ClientProtocolException, IOException {

        System.out.println("----testNullConfigureDigestChallengeReturnsInitializeAck---");
        System.out.println("DR1.012.007.003 - initialize:  JAL-Configure-Digest-Challenge Value Order - Missing JAL-Configure-Digest-Challenge returns on");
        System.out.println("DR1.014.001.003 - initialize-ack:  Communication Accepted - JAL-Configure-Digest-Challenge");

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


        HttpClient client = HttpClientBuilder.create().build();

        final HttpResponse response = client.execute(httpPost);
        final String responseMessage = response.getFirstHeader(HttpUtils.HDRS_MESSAGE).getValue();
        final int responseStatus = response.getStatusLine().getStatusCode();
        assertEquals(200, responseStatus);
        assertEquals("initialize-ack", responseMessage);

        final Header errorHeader = response.getFirstHeader(HttpUtils.HDRS_ERROR_MESSAGE);
        assertNull(errorHeader);

        final Header configureDigest = response.getFirstHeader(HttpUtils.HDRS_CONFIGURE_DIGEST_CHALLENGE);
        assertNotNull(configureDigest);
        assertEquals(HttpUtils.MSG_CONFIGURE_DIGEST_ON, configureDigest.getValue());

        final Header sessionHeader = response.getFirstHeader(HttpUtils.HDRS_SESSION_ID);
        assertNotNull(sessionHeader);

        System.out.println("----testNullConfigureDigestChallengeReturnsInitializeAck success---\n");
    }

    @Test
    public void testEmptyConfigureDigestChallengeReturnsInitializeAck() throws ClientProtocolException, IOException {

        System.out.println("----testEmptyConfigureDigestChallengeReturnsInitializeAck---");
        System.out.println("DR1.012.007.003 - initialize:  JAL-Configure-Digest-Challenge Value Order - Empty JAL-Configure-Digest-Challenge returns on");
        System.out.println("DR1.014.001.003 - initialize-ack:  Communication Accepted - JAL-Configure-Digest-Challenge");

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


        HttpClient client = HttpClientBuilder.create().build();

        final HttpResponse response = client.execute(httpPost);
        final String responseMessage = response.getFirstHeader(HttpUtils.HDRS_MESSAGE).getValue();
        final int responseStatus = response.getStatusLine().getStatusCode();
        assertEquals(200, responseStatus);
        assertEquals("initialize-ack", responseMessage);

        final Header configureDigest = response.getFirstHeader(HttpUtils.HDRS_CONFIGURE_DIGEST_CHALLENGE);
        assertNotNull(configureDigest);
        assertEquals(HttpUtils.MSG_CONFIGURE_DIGEST_ON, configureDigest.getValue());

        final Header errorHeader = response.getFirstHeader(HttpUtils.HDRS_ERROR_MESSAGE);
        assertNull(errorHeader);

        final Header sessionHeader = response.getFirstHeader(HttpUtils.HDRS_SESSION_ID);
        assertNotNull(sessionHeader);

        System.out.println("----testEmptyConfigureDigestChallengeReturnsInitializeAck success---\n");
    }

    @Test
    public void testConfigureDigestList() throws ClientProtocolException, IOException {
        System.out.println("----testConfigureDigestList----");
        System.out.println("DR1.012.007.003 - initialize:  JAL-Configure-Digest-Challenge Value Order - Accepts first valid JAL-Configure-Digest-Challenge value");

        UUID publisherUUID = UUID.randomUUID();
        final String publisherId = publisherUUID.toString();
        final HttpPost httpPost = new HttpPost("http://localhost:" + TestResources.HTTP_PORT + HttpUtils.JOURNAL_ENDPOINT);
        httpPost.setHeader(HttpUtils.HDRS_CONTENT_TYPE, HttpUtils.DEFAULT_CONTENT_TYPE);
        httpPost.setHeader(HttpUtils.HDRS_PUBLISHER_ID, publisherId);
        httpPost.setHeader(HttpUtils.HDRS_MESSAGE, HttpUtils.MSG_INIT);
        httpPost.setHeader(HttpUtils.HDRS_MODE, HttpUtils.MSG_LIVE);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_DIGEST, DigestMethod.SHA256);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_XML_COMPRESSION, "none");
        httpPost.setHeader(HttpUtils.HDRS_DATA_CLASS, HttpUtils.JOURNAL);
        httpPost.setHeader(HttpUtils.HDRS_VERSION, HttpUtils.SUPPORTED_VERSIONS[0]);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_CONFIGURE_DIGEST_CHALLENGE, "invalid, invalid2, off, on");


        HttpClient client = HttpClientBuilder.create().build();

        final HttpResponse response = client.execute(httpPost);
        final String responseMessage = response.getFirstHeader(HttpUtils.HDRS_MESSAGE).getValue();
        final int responseStatus = response.getStatusLine().getStatusCode();
        assertEquals(200, responseStatus);
        assertEquals("initialize-ack", responseMessage);

        final Header configureDigest = response.getFirstHeader(HttpUtils.HDRS_CONFIGURE_DIGEST_CHALLENGE);
        assertNotNull(configureDigest);
        assertEquals("off", configureDigest.getValue());

        final Header errorHeader = response.getFirstHeader(HttpUtils.HDRS_ERROR_MESSAGE);
        assertNull(errorHeader);

        final Header sessionHeader = response.getFirstHeader(HttpUtils.HDRS_SESSION_ID);
        assertNotNull(sessionHeader);

        System.out.println("----testConfigureDigestList success----\n");
    }

    @Test
    public void testXmlCompressionOrderList() throws ClientProtocolException, IOException {
        System.out.println("----testXmlCompressionOrderList----");
        System.out.println("DR1.012.003.003 - initialize:  JAL-Accept-XML-Compression Value Order - Accepts first valid JAL-Accept-XML-Compression value");

        UUID publisherUUID = UUID.randomUUID();
        final String publisherId = publisherUUID.toString();
        final HttpPost httpPost = new HttpPost("http://localhost:" + TestResources.HTTP_PORT + HttpUtils.JOURNAL_ENDPOINT);
        httpPost.setHeader(HttpUtils.HDRS_CONTENT_TYPE, HttpUtils.DEFAULT_CONTENT_TYPE);
        httpPost.setHeader(HttpUtils.HDRS_PUBLISHER_ID, publisherId);
        httpPost.setHeader(HttpUtils.HDRS_MESSAGE, HttpUtils.MSG_INIT);
        httpPost.setHeader(HttpUtils.HDRS_MODE, HttpUtils.MSG_LIVE);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_DIGEST, DigestMethod.SHA256);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_XML_COMPRESSION, "notvalid,deflate,none,exi-1.0");
        httpPost.setHeader(HttpUtils.HDRS_DATA_CLASS, HttpUtils.JOURNAL);
        httpPost.setHeader(HttpUtils.HDRS_VERSION, HttpUtils.SUPPORTED_VERSIONS[0]);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_CONFIGURE_DIGEST_CHALLENGE, String.join(",", HttpUtils.MSG_CONFIGURE_DIGEST_ON));


        HttpClient client = HttpClientBuilder.create().build();

        final HttpResponse response = client.execute(httpPost);
        final String responseMessage = response.getFirstHeader(HttpUtils.HDRS_MESSAGE).getValue();
        final int responseStatus = response.getStatusLine().getStatusCode();
        assertEquals(200, responseStatus);
        assertEquals("initialize-ack", responseMessage);

        final Header xmlCompression = response.getFirstHeader(HttpUtils.HDRS_XML_COMPRESSION);
        assertNotNull(xmlCompression);
        assertEquals("deflate", xmlCompression.getValue());

        final Header errorHeader = response.getFirstHeader(HttpUtils.HDRS_ERROR_MESSAGE);
        assertNull(errorHeader);

        final Header sessionHeader = response.getFirstHeader(HttpUtils.HDRS_SESSION_ID);
        assertNotNull(sessionHeader);
        System.out.println("----testXmlCompressionOrderList success----\n");
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
    public void testInvalidModeReturnsInitializeNack() throws ClientProtocolException, IOException {

        System.out.println("----testInvalidModeReturnsInitializeNack----");
        System.out.println("DR1.013.001.001.006 - initialize-nack:  Communication Declined - Error Reasons:  JAL-Unsupported-Mode");

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

        System.out.println("----testInvalidModeReturnsInitializeNack success----\n");
    }

    @Test
    public void testInvalidDigestReturnsInitializeNack() throws ClientProtocolException, IOException {

        System.out.println("----testInvalidDigestReturnsInitializeNack----");
        System.out.println("DR1.013.001.001.003 - initialize-nack:  Communication Declined - Error Reasons:  JAL-Unsupported-Digest");

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

        System.out.println("----testInvalidDigestReturnsInitializeNack success----\n");
    }

    @Test
    public void testInvalidXmlCompressionReturnsInitializeNack() throws ClientProtocolException, IOException {

        System.out.println("----testInvalidXmlCompressionReturnsInitializeNack----");
        System.out.println("DR1.013.001.001.002 - initialize-nack:  Communication Declined - Error Reasons:  JAL-Unsupported-XML-Compression");

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

        System.out.println("----testInvalidXmlCompressionReturnsInitializeNack success----\n");
    }

    @Test
    public void testInvalidDataClassReturnsInitializeNack() throws ClientProtocolException, IOException {
        System.out.println("----testInvalidDataClassReturnsInitializeNack----");
        System.out.println("DR1.013.001.001.005 - initialize-nack:  Communication Declined - Error Reasons:  JAL-Unsupported-Data-Class");

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

        System.out.println("----testInvalidDataClassReturnsInitializeNack success----\n");
    }

    @Test
    public void testInvalidVersionReturnsInitializeNack() throws ClientProtocolException, IOException {

        System.out.println("----testInvalidVersionReturnsInitializeNack----");
        System.out.println("DR1.013.001.001.001 - initialize-nack:  Communication Declined - Error Reasons:  JAL-Unsupported-Version");
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

        System.out.println("----testInvalidVersionReturnsInitializeNack success----\n");
    }

    @Test
    public void testInvalidConfigureDigestChallengeReturnsInitializeNack() throws ClientProtocolException, IOException {

        System.out.println("----testInvalidConfigureDigestChallengeReturnsInitializeNack----");
        System.out.println("DR1.013.001.001.004 - initialize-nack:  Communication Declined - Error Reasons:  JAL-Unsupported-Configure-Digest-Challenge");

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

        System.out.println("----testInvalidConfigureDigestChallengeReturnsInitializeNack success----\n");
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