package com.tresys.jalop.jnl.impl.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

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

import com.tresys.jalop.jnl.DigestStatus;
import com.tresys.jalop.jnl.RecordType;

/**
 * Tests for common utility class.
 */
public class DigestResponseTest {

    private static Server server;

    private static String jjnlDirPath = "";
    private static String jalopTestDataDir = "";
    private static String inputDirStr = "";
    private static String outputDirStr = "";
    private static String jalopTestDataRepoDir = "";
    private static File resourcesDirectory;

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @BeforeClass
    public static void startWebServiceServer() throws Exception {
        TestResources.configureLogging(Level.DEBUG);

        resourcesDirectory = new File("src/test/resources");
        jjnlDirPath = resourcesDirectory.getAbsolutePath() + "/../../../..";
        inputDirStr = jjnlDirPath + "/input";
        outputDirStr = jjnlDirPath + "/jnl_lib/output";

        //Clears out input and output directories
        TestResources.cleanAllDirectories(inputDirStr, outputDirStr);

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

        //Clears out input and output directories
    //    TestResources.cleanAllDirectories(inputDirStr, outputDirStr);
    }

    @Test
    public void testMissingSessionIdInDigestResponseMessage() throws ClientProtocolException, IOException {

        for (RecordType recType : RecordType.values())
        {
            if (recType.equals(RecordType.Unset))
            {
                continue;
            }

            String jalId = UUID.randomUUID().toString();
            final HttpPost httpPost = new HttpPost("http://localhost:" + TestResources.HTTP_PORT + "/" + recType.toString().toLowerCase());
            httpPost.setHeader(HttpUtils.HDRS_CONTENT_TYPE, HttpUtils.DEFAULT_CONTENT_TYPE);
            httpPost.setHeader(HttpUtils.HDRS_MESSAGE, HttpUtils.MSG_DIGEST_RESP);
            httpPost.setHeader(HttpUtils.HDRS_NONCE, jalId);
            httpPost.setHeader(HttpUtils.HDRS_DIGEST_STATUS, DigestStatus.Confirmed.toString().toLowerCase());

            HttpClient client = HttpClientBuilder.create().build();

            final HttpResponse response = client.execute(httpPost);

            final Header errorHeader = response.getFirstHeader(HttpUtils.HDRS_ERROR_MESSAGE);
            assertNotNull(errorHeader);
            assertEquals(HttpUtils.HDRS_UNSUPPORTED_SESSION_ID, errorHeader.getValue());

            final Header jalIdHeader = response.getFirstHeader(HttpUtils.HDRS_NONCE);
            final Header messageHeader = response.getFirstHeader(HttpUtils.HDRS_MESSAGE);

            assertNull(jalIdHeader);
            assertNotNull(messageHeader);
            assertEquals(HttpUtils.MSG_SESSION_FAILURE, messageHeader.getValue());
        }
    }

    @Test
    public void testEmptySessionIdInDigestResponseMessage() throws ClientProtocolException, IOException {

        for (RecordType recType : RecordType.values())
        {
            if (recType.equals(RecordType.Unset))
            {
                continue;
            }

            String jalId = UUID.randomUUID().toString();
            final HttpPost httpPost = new HttpPost("http://localhost:" + TestResources.HTTP_PORT + "/" + recType.toString().toLowerCase());
            httpPost.setHeader(HttpUtils.HDRS_CONTENT_TYPE, HttpUtils.DEFAULT_CONTENT_TYPE);
            httpPost.setHeader(HttpUtils.HDRS_MESSAGE, HttpUtils.MSG_DIGEST_RESP);
            httpPost.setHeader(HttpUtils.HDRS_SESSION_ID, "");
            httpPost.setHeader(HttpUtils.HDRS_NONCE, jalId);
            httpPost.setHeader(HttpUtils.HDRS_DIGEST_STATUS, DigestStatus.Confirmed.toString().toLowerCase());

            HttpClient client = HttpClientBuilder.create().build();

            final HttpResponse response = client.execute(httpPost);

            final Header errorHeader = response.getFirstHeader(HttpUtils.HDRS_ERROR_MESSAGE);
            assertNotNull(errorHeader);
            assertEquals(HttpUtils.HDRS_UNSUPPORTED_SESSION_ID, errorHeader.getValue());

            final Header jalIdHeader = response.getFirstHeader(HttpUtils.HDRS_NONCE);
            final Header messageHeader = response.getFirstHeader(HttpUtils.HDRS_MESSAGE);

            assertNull(jalIdHeader);

            assertNotNull(messageHeader);
            assertEquals(HttpUtils.MSG_SESSION_FAILURE, messageHeader.getValue());
        }
    }

    @Test
    public void testMissingJalIdInDigestResponseMessage() throws ClientProtocolException, IOException {

        for (RecordType recType : RecordType.values())
        {
            if (recType.equals(RecordType.Unset))
            {
                continue;
            }

            String sessionId = UUID.randomUUID().toString();
            final HttpPost httpPost = new HttpPost("http://localhost:" + TestResources.HTTP_PORT + "/" + recType.toString().toLowerCase());
            httpPost.setHeader(HttpUtils.HDRS_CONTENT_TYPE, HttpUtils.DEFAULT_CONTENT_TYPE);
            httpPost.setHeader(HttpUtils.HDRS_MESSAGE, HttpUtils.MSG_DIGEST_RESP);
            httpPost.setHeader(HttpUtils.HDRS_SESSION_ID, sessionId);
            httpPost.setHeader(HttpUtils.HDRS_DIGEST_STATUS, DigestStatus.Confirmed.toString().toLowerCase());

            HttpClient client = HttpClientBuilder.create().build();

            final HttpResponse response = client.execute(httpPost);

            final Header errorHeader = response.getFirstHeader(HttpUtils.HDRS_ERROR_MESSAGE);
            assertNotNull(errorHeader);
            assertEquals(HttpUtils.HDRS_INVALID_JAL_ID, errorHeader.getValue());

            final Header jalIdHeader = response.getFirstHeader(HttpUtils.HDRS_NONCE);
            final Header messageHeader = response.getFirstHeader(HttpUtils.HDRS_MESSAGE);

            assertNull(jalIdHeader);
            assertNotNull(messageHeader);
            assertEquals(HttpUtils.MSG_SYNC_FAILURE, messageHeader.getValue());
        }
    }

    @Test
    public void testEmptyJalIdInDigestResponseMessage() throws ClientProtocolException, IOException {

        for (RecordType recType : RecordType.values())
        {
            if (recType.equals(RecordType.Unset))
            {
                continue;
            }

            String sessionId = UUID.randomUUID().toString();
            final HttpPost httpPost = new HttpPost("http://localhost:" + TestResources.HTTP_PORT + "/" + recType.toString().toLowerCase());
            httpPost.setHeader(HttpUtils.HDRS_CONTENT_TYPE, HttpUtils.DEFAULT_CONTENT_TYPE);
            httpPost.setHeader(HttpUtils.HDRS_MESSAGE, HttpUtils.MSG_DIGEST_RESP);
            httpPost.setHeader(HttpUtils.HDRS_SESSION_ID, sessionId);
            httpPost.setHeader(HttpUtils.HDRS_NONCE, "");
            httpPost.setHeader(HttpUtils.HDRS_DIGEST_STATUS, DigestStatus.Confirmed.toString().toLowerCase());

            HttpClient client = HttpClientBuilder.create().build();

            final HttpResponse response = client.execute(httpPost);

            final Header errorHeader = response.getFirstHeader(HttpUtils.HDRS_ERROR_MESSAGE);
            assertNotNull(errorHeader);
            assertEquals(HttpUtils.HDRS_INVALID_JAL_ID, errorHeader.getValue());

            final Header jalIdHeader = response.getFirstHeader(HttpUtils.HDRS_NONCE);
            final Header messageHeader = response.getFirstHeader(HttpUtils.HDRS_MESSAGE);

            assertNotNull(jalIdHeader);
            assertEquals("", jalIdHeader.getValue());
            assertNotNull(messageHeader);
            assertEquals(HttpUtils.MSG_SYNC_FAILURE, messageHeader.getValue());
        }
    }

    @Test
    public void testMissingDigestStatusInDigestResponseMessage() throws ClientProtocolException, IOException {

        for (RecordType recType : RecordType.values())
        {
            if (recType.equals(RecordType.Unset))
            {
                continue;
            }

            String sessionId = UUID.randomUUID().toString();
            String jalId = UUID.randomUUID().toString();
            final HttpPost httpPost = new HttpPost("http://localhost:" + TestResources.HTTP_PORT + "/" + recType.toString().toLowerCase());
            httpPost.setHeader(HttpUtils.HDRS_CONTENT_TYPE, HttpUtils.DEFAULT_CONTENT_TYPE);
            httpPost.setHeader(HttpUtils.HDRS_MESSAGE, HttpUtils.MSG_DIGEST_RESP);
            httpPost.setHeader(HttpUtils.HDRS_SESSION_ID, sessionId);
            httpPost.setHeader(HttpUtils.HDRS_NONCE, jalId);

            HttpClient client = HttpClientBuilder.create().build();

            final HttpResponse response = client.execute(httpPost);

            final Header errorHeader = response.getFirstHeader(HttpUtils.HDRS_ERROR_MESSAGE);
            assertNotNull(errorHeader);
            assertEquals(HttpUtils.HDRS_INVALID_DIGEST_STATUS, errorHeader.getValue());

            final Header jalIdHeader = response.getFirstHeader(HttpUtils.HDRS_NONCE);
            final Header messageHeader = response.getFirstHeader(HttpUtils.HDRS_MESSAGE);

            assertNotNull(jalIdHeader);
            assertEquals(jalId, jalIdHeader.getValue());
            assertNotNull(messageHeader);
            assertEquals(HttpUtils.MSG_SYNC_FAILURE, messageHeader.getValue());
        }
    }

    @Test
    public void testEmptyDigestStatusInDigestResponseMessage() throws ClientProtocolException, IOException {

        for (RecordType recType : RecordType.values())
        {
            if (recType.equals(RecordType.Unset))
            {
                continue;
            }

            String sessionId = UUID.randomUUID().toString();
            String jalId = UUID.randomUUID().toString();
            final HttpPost httpPost = new HttpPost("http://localhost:" + TestResources.HTTP_PORT + "/" + recType.toString().toLowerCase());
            httpPost.setHeader(HttpUtils.HDRS_CONTENT_TYPE, HttpUtils.DEFAULT_CONTENT_TYPE);
            httpPost.setHeader(HttpUtils.HDRS_MESSAGE, HttpUtils.MSG_DIGEST_RESP);
            httpPost.setHeader(HttpUtils.HDRS_SESSION_ID, sessionId);
            httpPost.setHeader(HttpUtils.HDRS_NONCE, jalId);
            httpPost.setHeader(HttpUtils.HDRS_DIGEST_STATUS, "");

            HttpClient client = HttpClientBuilder.create().build();

            final HttpResponse response = client.execute(httpPost);

            final Header errorHeader = response.getFirstHeader(HttpUtils.HDRS_ERROR_MESSAGE);
            assertNotNull(errorHeader);
            assertEquals(HttpUtils.HDRS_INVALID_DIGEST_STATUS, errorHeader.getValue());

            final Header jalIdHeader = response.getFirstHeader(HttpUtils.HDRS_NONCE);
            final Header messageHeader = response.getFirstHeader(HttpUtils.HDRS_MESSAGE);

            assertNotNull(jalIdHeader);
            assertEquals(jalId, jalIdHeader.getValue());
            assertNotNull(messageHeader);
            assertEquals(HttpUtils.MSG_SYNC_FAILURE, messageHeader.getValue());
        }
    }

    @Test
    public void testNotValidDigestStatusInDigestResponseMessage() throws ClientProtocolException, IOException {

        for (RecordType recType : RecordType.values())
        {
            if (recType.equals(RecordType.Unset))
            {
                continue;
            }

            String sessionId = UUID.randomUUID().toString();
            String jalId = UUID.randomUUID().toString();
            final HttpPost httpPost = new HttpPost("http://localhost:" + TestResources.HTTP_PORT + "/" + recType.toString().toLowerCase());
            httpPost.setHeader(HttpUtils.HDRS_CONTENT_TYPE, HttpUtils.DEFAULT_CONTENT_TYPE);
            httpPost.setHeader(HttpUtils.HDRS_MESSAGE, HttpUtils.MSG_DIGEST_RESP);
            httpPost.setHeader(HttpUtils.HDRS_SESSION_ID, sessionId);
            httpPost.setHeader(HttpUtils.HDRS_NONCE, jalId);
            httpPost.setHeader(HttpUtils.HDRS_DIGEST_STATUS, "notvalid");

            HttpClient client = HttpClientBuilder.create().build();

            final HttpResponse response = client.execute(httpPost);

            final Header errorHeader = response.getFirstHeader(HttpUtils.HDRS_ERROR_MESSAGE);
            assertNotNull(errorHeader);
            assertEquals(HttpUtils.HDRS_INVALID_DIGEST_STATUS, errorHeader.getValue());

            final Header jalIdHeader = response.getFirstHeader(HttpUtils.HDRS_NONCE);
            final Header messageHeader = response.getFirstHeader(HttpUtils.HDRS_MESSAGE);

            assertNotNull(jalIdHeader);
            assertEquals(jalId, jalIdHeader.getValue());
            assertNotNull(messageHeader);
            assertEquals(HttpUtils.MSG_SYNC_FAILURE, messageHeader.getValue());
        }
    }

    @Test
    public void testSessionDoesNotExistInDigestResponseMessage() throws ClientProtocolException, IOException {

        for (RecordType recType : RecordType.values())
        {
            if (recType.equals(RecordType.Unset))
            {
                continue;
            }

            String sessionId = UUID.randomUUID().toString();
            String jalId = UUID.randomUUID().toString();
            final HttpPost httpPost = new HttpPost("http://localhost:" + TestResources.HTTP_PORT + "/" + recType.toString().toLowerCase());
            httpPost.setHeader(HttpUtils.HDRS_CONTENT_TYPE, HttpUtils.DEFAULT_CONTENT_TYPE);
            httpPost.setHeader(HttpUtils.HDRS_MESSAGE, HttpUtils.MSG_DIGEST_RESP);
            httpPost.setHeader(HttpUtils.HDRS_SESSION_ID, sessionId);
            httpPost.setHeader(HttpUtils.HDRS_NONCE, jalId);
            httpPost.setHeader(HttpUtils.HDRS_DIGEST_STATUS, "confirmed");

            HttpClient client = HttpClientBuilder.create().build();

            final HttpResponse response = client.execute(httpPost);

            final Header errorHeader = response.getFirstHeader(HttpUtils.HDRS_ERROR_MESSAGE);
            assertNotNull(errorHeader);
            assertEquals(HttpUtils.HDRS_UNSUPPORTED_SESSION_ID, errorHeader.getValue());

            final Header jalIdHeader = response.getFirstHeader(HttpUtils.HDRS_NONCE);
            final Header messageHeader = response.getFirstHeader(HttpUtils.HDRS_MESSAGE);

            assertNotNull(jalIdHeader);
            assertEquals(jalId, jalIdHeader.getValue());

            //TODO - once session management is implemented, this may need changed to session-failed depending on how this check is implemented.
            //This is due to the check for if session exists is happening in the processDigestResponse message method currently.
            assertNotNull(messageHeader);
            assertEquals(HttpUtils.MSG_SYNC_FAILURE, messageHeader.getValue());
        }
    }

    @Test
    public void testValidConfirmedDigestResponseMessage() throws ClientProtocolException, IOException
    {
        String publisherId = UUID.randomUUID().toString();
        for (RecordType recType : RecordType.values())
        {
            if (recType.equals(RecordType.Unset))
            {
                continue;
            }

            //Valid initialize
            String sessionId = TestResources.sendValidInitialize(recType, true, publisherId);

            //Valid JAL record post
            String jalId = TestResources.sendValidJalRecord(recType, sessionId);

            //Sends digest response
            final HttpPost httpPost = new HttpPost("http://localhost:" + TestResources.HTTP_PORT + "/" + recType.toString().toLowerCase());
            httpPost.setHeader(HttpUtils.HDRS_CONTENT_TYPE, HttpUtils.DEFAULT_CONTENT_TYPE);
            httpPost.setHeader(HttpUtils.HDRS_MESSAGE, HttpUtils.MSG_DIGEST_RESP);
            httpPost.setHeader(HttpUtils.HDRS_SESSION_ID, sessionId);
            httpPost.setHeader(HttpUtils.HDRS_NONCE, jalId);
            httpPost.setHeader(HttpUtils.HDRS_DIGEST_STATUS, "confirmed");

            HttpClient client = HttpClientBuilder.create().build();
            final HttpResponse response = client.execute(httpPost);

            final Header jalIdHeader = response.getFirstHeader(HttpUtils.HDRS_NONCE);
            final Header messageHeader = response.getFirstHeader(HttpUtils.HDRS_MESSAGE);

            assertNotNull(jalIdHeader);
            assertEquals(jalId, jalIdHeader.getValue());

            assertNotNull(messageHeader);
            assertEquals(HttpUtils.MSG_SYNC, messageHeader.getValue());

            final Header errorHeader = response.getFirstHeader(HttpUtils.HDRS_ERROR_MESSAGE);
            assertNull(errorHeader);
        }
    }

    @Test
    public void testValidInvalidDigestResponseMessage() throws ClientProtocolException, IOException
    {
        String publisherId = UUID.randomUUID().toString();
        for (RecordType recType : RecordType.values())
        {
            if (recType.equals(RecordType.Unset))
            {
                continue;
            }

            //Valid initialize
            String sessionId = TestResources.sendValidInitialize(recType, true, publisherId);

            //Valid JAL record post
            String jalId = TestResources.sendValidJalRecord(recType, sessionId);

            //Sends digest response
            final HttpPost httpPost = new HttpPost("http://localhost:" + TestResources.HTTP_PORT + "/" + recType.toString().toLowerCase());
            httpPost.setHeader(HttpUtils.HDRS_CONTENT_TYPE, HttpUtils.DEFAULT_CONTENT_TYPE);
            httpPost.setHeader(HttpUtils.HDRS_MESSAGE, HttpUtils.MSG_DIGEST_RESP);
            httpPost.setHeader(HttpUtils.HDRS_SESSION_ID, sessionId);
            httpPost.setHeader(HttpUtils.HDRS_NONCE, jalId);
            httpPost.setHeader(HttpUtils.HDRS_DIGEST_STATUS, "invalid");

            HttpClient client = HttpClientBuilder.create().build();
            final HttpResponse response = client.execute(httpPost);

            final Header jalIdHeader = response.getFirstHeader(HttpUtils.HDRS_NONCE);
            final Header messageHeader = response.getFirstHeader(HttpUtils.HDRS_MESSAGE);

            assertNotNull(jalIdHeader);
            assertEquals(jalId, jalIdHeader.getValue());

            assertNotNull(messageHeader);
            assertEquals(HttpUtils.MSG_SYNC, messageHeader.getValue());

            final Header errorHeader = response.getFirstHeader(HttpUtils.HDRS_ERROR_MESSAGE);
            assertNull(errorHeader);
        }
    }
}
