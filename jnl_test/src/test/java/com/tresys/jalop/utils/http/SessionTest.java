package com.tresys.jalop.utils.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.EntityBuilder;
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
import com.tresys.jalop.jnl.impl.http.HttpUtils;

/**
 * Tests for common utility class.
 */
public class SessionTest {

    private static Server server;

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    /**
     * Sets up the server for the web service.
     *
     * @throws Exception
     */
    @BeforeClass
    public static void startWebServiceServer() throws Exception {
        TestResources.configureLogging(Level.INFO);

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
    }


    @Test
    public void testMaxConcurrentSessionEcxeededTest() throws ClientProtocolException, IOException, InterruptedException {

        String publisherId = UUID.randomUUID().toString();

        String sessionId = TestResources.sendValidInitialize(RecordType.Journal, true, publisherId, HttpUtils.MSG_LIVE);

        //Exceed the maximum session limit, and kick out our sessionId above
        String concurrentSession1 = TestResources.sendValidInitialize(RecordType.Journal, true, publisherId, HttpUtils.MSG_LIVE);
        String concurrentSession2 = TestResources.sendValidInitialize(RecordType.Journal, true, publisherId, HttpUtils.MSG_LIVE);
        String concurrentSession3 = TestResources.sendValidInitialize(RecordType.Journal, true, publisherId, HttpUtils.MSG_LIVE);
        String concurrentSession4 = TestResources.sendValidInitialize(RecordType.Journal, true, publisherId, HttpUtils.MSG_LIVE);
        String concurrentSession5 = TestResources.sendValidInitialize(RecordType.Journal, true, publisherId, HttpUtils.MSG_LIVE);

        HttpPost httpPost = new HttpPost("http://localhost:" + TestResources.HTTP_PORT + "/" + RecordType.Journal.toString().toLowerCase());

        String jalId = UUID.randomUUID().toString();
        HashMap<String, String> headers = TestResources.getJalRecordHeaders(sessionId, jalId, "3083", "1125", "19", RecordType.Journal);

        for (Map.Entry<String, String> entry : headers.entrySet())
        {
            httpPost.setHeader(entry.getKey(), entry.getValue());
        }

        //Adds jal record to post
        File resourcesDirectory = new File("src/test/resources/unit_test");

        String jalRecord1Path = resourcesDirectory.getAbsolutePath() + "/jal_record1.txt";
        HttpEntity entity = EntityBuilder.create().setFile(new File(jalRecord1Path)).build();

        httpPost.setEntity(entity);

        HttpClient client = HttpClientBuilder.create().build();

        final HttpResponse response = client.execute(httpPost);

        final String responseMessage = response.getFirstHeader(HttpUtils.HDRS_MESSAGE).getValue();
        final Header digestHeader = response.getFirstHeader(HttpUtils.HDRS_DIGEST_VALUE);
        final Header jalIdHeader = response.getFirstHeader(HttpUtils.HDRS_NONCE);
        final Header errorMessage = response.getFirstHeader(HttpUtils.HDRS_ERROR_MESSAGE);
        final int responseStatus = response.getStatusLine().getStatusCode();
        assertEquals(200, responseStatus);
        assertNotNull(errorMessage);
        assertNull(digestHeader);
        assertNotNull(jalIdHeader);
        assertEquals(jalId, jalIdHeader.getValue());

        final Header sessionHeader = response.getFirstHeader(HttpUtils.HDRS_SESSION_ID);
        assertNotNull(sessionHeader);
        assertEquals(sessionId, sessionHeader.getValue());
        assertEquals(HttpUtils.MSG_SESSION_FAILURE, responseMessage);
    }

    @Test
    public void testMaxConcurrentSessionTest() throws ClientProtocolException, IOException, InterruptedException {

	    String publisherId = UUID.randomUUID().toString();

        System.out.println("Testing record type of " + RecordType.Journal.toString() + " with mode of live");
        String sessionId = TestResources.sendValidInitialize(RecordType.Journal, true, publisherId, HttpUtils.MSG_LIVE);

        //Reach the maximum session limit, but don't exceed it
        String concurrentSession1 = TestResources.sendValidInitialize(RecordType.Journal, true, publisherId, HttpUtils.MSG_LIVE);
        String concurrentSession2 = TestResources.sendValidInitialize(RecordType.Journal, true, publisherId, HttpUtils.MSG_LIVE);
        String concurrentSession3 = TestResources.sendValidInitialize(RecordType.Journal, true, publisherId, HttpUtils.MSG_LIVE);
        String concurrentSession4 = TestResources.sendValidInitialize(RecordType.Journal, true, publisherId, HttpUtils.MSG_LIVE);

        HttpPost httpPost = new HttpPost("http://localhost:" + TestResources.HTTP_PORT + "/" + RecordType.Journal.toString().toLowerCase());

        String jalId = UUID.randomUUID().toString();
        HashMap<String, String> headers = TestResources.getJalRecordHeaders(sessionId, jalId, "3083", "1125", "19", RecordType.Journal);

        for (Map.Entry<String, String> entry : headers.entrySet())
        {
            httpPost.setHeader(entry.getKey(), entry.getValue());
        }

        //Adds jal record to post
        File resourcesDirectory = new File("src/test/resources/unit_test");

        String jalRecord1Path = resourcesDirectory.getAbsolutePath() + "/jal_record1.txt";
        HttpEntity entity = EntityBuilder.create().setFile(new File(jalRecord1Path)).build();

        httpPost.setEntity(entity);

        HttpClient client = HttpClientBuilder.create().build();

        final HttpResponse response = client.execute(httpPost);
        final String responseMessage = response.getFirstHeader(HttpUtils.HDRS_MESSAGE).getValue();
        final Header digestHeader = response.getFirstHeader(HttpUtils.HDRS_DIGEST_VALUE);
        final Header jalIdHeader = response.getFirstHeader(HttpUtils.HDRS_NONCE);
        final Header errorMessage = response.getFirstHeader(HttpUtils.HDRS_ERROR_MESSAGE);
        final int responseStatus = response.getStatusLine().getStatusCode();
        assertEquals(200, responseStatus);
        assertEquals(null, errorMessage);
        assertNotNull(digestHeader);

        //Validate digest is correct for test file sent.
        assertEquals("bbd801ce4dc24520c028025c05b44c5532b240824d2d7ce25644b73b667b6c7a", digestHeader.getValue());
        assertEquals(HttpUtils.MSG_DIGEST_CHALLENGE, responseMessage);
        assertEquals(jalId, jalIdHeader.getValue());
    }
}
