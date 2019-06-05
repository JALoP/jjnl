package com.tresys.jalop.utils.http;

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
import com.tresys.jalop.jnl.impl.http.HttpUtils;

/**
 * Tests for common utility class.
 */
public class CloseSessionTest {

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
        TestResources.configureLogging(Level.INFO);

        resourcesDirectory = new File("src/test/resources/unit_test");
        jjnlDirPath = resourcesDirectory.getAbsolutePath() + "/../../../../..";
        outputDirStr = jjnlDirPath + "/jnl_test/output";

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
    public void testCloseSessionMessage() throws ClientProtocolException, IOException, InterruptedException {

        String publisherId = UUID.randomUUID().toString();

        for (RecordType recType : RecordType.values())
        {
            if (recType.equals(RecordType.Unset))
            {
                continue;
            }
            //Initialize to get a session
            String sessionId = TestResources.sendValidInitialize(recType, true, publisherId, HttpUtils.MSG_LIVE);

            //Send record successfully
            String jalId = TestResources.sendValidJalRecord(recType, sessionId);

            //Close session
            HttpPost httpPost = new HttpPost("http://localhost:" + TestResources.HTTP_PORT + "/" + recType.toString().toLowerCase());
            httpPost.setHeader(HttpUtils.HDRS_CONTENT_TYPE, HttpUtils.DEFAULT_CONTENT_TYPE);
            httpPost.setHeader(HttpUtils.HDRS_MESSAGE, HttpUtils.MSG_CLOSE_SESSION);
            httpPost.setHeader(HttpUtils.HDRS_SESSION_ID, sessionId);

            HttpClient client = HttpClientBuilder.create().build();

            HttpResponse response = client.execute(httpPost);

            Header errorHeader = response.getFirstHeader(HttpUtils.HDRS_ERROR_MESSAGE);
            assertNull(errorHeader);

            Header messageHeader = response.getFirstHeader(HttpUtils.HDRS_MESSAGE);
            assertNotNull(messageHeader);
            assertEquals(HttpUtils.MSG_CLOSE_SESSION_RESPONSE, messageHeader.getValue());

            //Attempt another message, should fail since session doesn't exist anymore.
            httpPost = new HttpPost("http://localhost:" + TestResources.HTTP_PORT + "/" + recType.toString().toLowerCase());
            httpPost.setHeader(HttpUtils.HDRS_CONTENT_TYPE, HttpUtils.DEFAULT_CONTENT_TYPE);
            httpPost.setHeader(HttpUtils.HDRS_MESSAGE, HttpUtils.MSG_DIGEST_RESP);
            httpPost.setHeader(HttpUtils.HDRS_SESSION_ID, sessionId);
            httpPost.setHeader(HttpUtils.HDRS_DIGEST_STATUS, DigestStatus.Confirmed.toString().toLowerCase());

            client = HttpClientBuilder.create().build();

            response = client.execute(httpPost);

            errorHeader = response.getFirstHeader(HttpUtils.HDRS_ERROR_MESSAGE);
            assertNotNull(errorHeader);
            assertEquals(HttpUtils.HDRS_UNSUPPORTED_SESSION_ID, errorHeader.getValue());

            messageHeader = response.getFirstHeader(HttpUtils.HDRS_MESSAGE);
            assertNotNull(messageHeader);
            assertEquals(HttpUtils.MSG_SESSION_FAILURE, messageHeader.getValue());
        }

        TestResources.cleanOutputDirectory(outputDirStr);
    }

    @Test
    public void testCloseSessionMessageInvalidSessionId() throws ClientProtocolException, IOException, InterruptedException {

        for (RecordType recType : RecordType.values())
        {
            if (recType.equals(RecordType.Unset))
            {
                continue;
            }

            String sessionId = UUID.randomUUID().toString();

            //Close session
            final HttpPost httpPost = new HttpPost("http://localhost:" + TestResources.HTTP_PORT + "/" + recType.toString().toLowerCase());
            httpPost.setHeader(HttpUtils.HDRS_CONTENT_TYPE, HttpUtils.DEFAULT_CONTENT_TYPE);
            httpPost.setHeader(HttpUtils.HDRS_MESSAGE, HttpUtils.MSG_CLOSE_SESSION);
            httpPost.setHeader(HttpUtils.HDRS_SESSION_ID, sessionId);

            final HttpClient client = HttpClientBuilder.create().build();

            final HttpResponse response = client.execute(httpPost);

            final Header errorHeader = response.getFirstHeader(HttpUtils.HDRS_ERROR_MESSAGE);
            assertNotNull(errorHeader);
            assertEquals(HttpUtils.HDRS_UNSUPPORTED_SESSION_ID, errorHeader.getValue());

            final Header messageHeader = response.getFirstHeader(HttpUtils.HDRS_MESSAGE);
            assertNotNull(messageHeader);
            assertEquals(HttpUtils.MSG_SESSION_FAILURE, messageHeader.getValue());
        }
    }
}

