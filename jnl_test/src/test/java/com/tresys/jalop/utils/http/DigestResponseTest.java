package com.tresys.jalop.utils.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

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
public class DigestResponseTest {

    private static Server server;

    private static String jjnlDirPath = "";
    private static String inputDirStr = "";
    private static String outputDirStr = "";
    private static File resourcesDirectory;

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @BeforeClass
    public static void startWebServiceServer() throws Exception {
        TestResources.configureLogging(Level.DEBUG);

        resourcesDirectory = new File("src/test/resources/unit_test");
        jjnlDirPath = resourcesDirectory.getAbsolutePath() + "/../../../../..";
        inputDirStr = jjnlDirPath + "/input";
        outputDirStr = jjnlDirPath + "/jnl_test/output";

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
        TestResources.cleanAllDirectories(inputDirStr, outputDirStr);
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

            assertNotNull(jalIdHeader);
            assertEquals(jalId, jalIdHeader.getValue());
            assertNotNull(messageHeader);
            assertEquals(HttpUtils.MSG_SESSION_FAILURE, messageHeader.getValue());

            final Header sessionIdHeader = response.getFirstHeader(HttpUtils.HDRS_SESSION_ID);
            assertNull(sessionIdHeader);
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

            assertNotNull(jalIdHeader);
            assertEquals(jalId, jalIdHeader.getValue());
            final Header sessionIdHeader = response.getFirstHeader(HttpUtils.HDRS_SESSION_ID);
            assertNull(sessionIdHeader);

            assertNotNull(messageHeader);
            assertEquals(HttpUtils.MSG_SESSION_FAILURE, messageHeader.getValue());
        }
    }

    @Test
    public void testMissingJalIdInDigestResponseMessage() throws ClientProtocolException, IOException {
        System.out.println("----testMissingJalIdInDigestResponseMessage---");
        System.out.println("DR1.018.004 - record-failure:  digest-response");
        System.out.println("DR1.018.005 - record-failure:  JAL-Id");
        System.out.println("DR1.018.006 - record-failure:  JAL-Error-Message");
        System.out.println("DR1.018.006.001 - record-failure:  JAL-Error-Message:  Error Reasons");
        System.out.println("DR1.018.006.001.009 - record-failure:  JAL-Error-Message:  Error Reasons - JAL-Invalid-JAL-Id");

        String publisherId = UUID.randomUUID().toString();
        for (RecordType recType : RecordType.values())
        {
            if (recType.equals(RecordType.Unset))
            {
                continue;
            }

            System.out.println("Testing record type of " + recType.toString());

            //Valid initialize
            String sessionId = TestResources.sendValidInitialize(recType, true, publisherId);

            //Valid JAL record post
            String jalId = TestResources.sendValidJalRecord(recType, sessionId);

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

            assertNotNull(jalIdHeader);
            assertEquals("", jalIdHeader.getValue());
            assertNotNull(messageHeader);
            assertEquals(HttpUtils.MSG_RECORD_FAILURE, messageHeader.getValue());
        }

        System.out.println("----testMissingJalIdInDigestResponseMessage success----\n");
    }

    @Test
    public void testEmptyJalIdInDigestResponseMessage() throws ClientProtocolException, IOException {

        System.out.println("----testEmptyJalIdInDigestResponseMessage---");
        System.out.println("DR1.018.004 - record-failure:  digest-response");
        System.out.println("DR1.018.005 - record-failure:  JAL-Id");
        System.out.println("DR1.018.006 - record-failure:  JAL-Error-Message");
        System.out.println("DR1.018.006.001 - record-failure:  JAL-Error-Message:  Error Reasons");
        System.out.println("DR1.018.006.001.009 - record-failure:  JAL-Error-Message:  Error Reasons - JAL-Invalid-JAL-Id");

        String publisherId = UUID.randomUUID().toString();
        for (RecordType recType : RecordType.values())
        {
            if (recType.equals(RecordType.Unset))
            {
                continue;
            }

            System.out.println("Testing record type of " + recType.toString());

            //Valid initialize
            String sessionId = TestResources.sendValidInitialize(recType, true, publisherId);

            //Valid JAL record post
            String jalId = TestResources.sendValidJalRecord(recType, sessionId);

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
            assertEquals(HttpUtils.MSG_RECORD_FAILURE, messageHeader.getValue());
        }
        System.out.println("----testEmptyJalIdInDigestResponseMessage success----\n");
    }

    @Test
    public void testMissingDigestStatusInDigestResponseMessage() throws ClientProtocolException, IOException {
        System.out.println("----testMissingDigestStatusInDigestResponseMessage---");
        System.out.println("DR1.018.004 - record-failure:  digest-response");
        System.out.println("DR1.018.005 - record-failure:  JAL-Id");
        System.out.println("DR1.018.006 - record-failure:  JAL-Error-Message");
        System.out.println("DR1.018.006.001 - record-failure:  JAL-Error-Message:  Error Reasons");
        System.out.println("DR1.018.006.001.012 - record-failure:  JAL-Error-Message:  Error Reasons - JAL-Invalid-Digest-Status");

        String publisherId = UUID.randomUUID().toString();
        for (RecordType recType : RecordType.values())
        {
            if (recType.equals(RecordType.Unset))
            {
                continue;
            }

            System.out.println("Testing record type of " + recType.toString());

            //Valid initialize
            String sessionId = TestResources.sendValidInitialize(recType, true, publisherId);

            //Valid JAL record post
            String jalId = TestResources.sendValidJalRecord(recType, sessionId);

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
            assertEquals(HttpUtils.MSG_RECORD_FAILURE, messageHeader.getValue());
        }
        System.out.println("----testMissingDigestStatusInDigestResponseMessage success----\n");
    }

    @Test
    public void testEmptyDigestStatusInDigestResponseMessage() throws ClientProtocolException, IOException {
        System.out.println("----testEmptyDigestStatusInDigestResponseMessage---");
        System.out.println("DR1.018.004 - record-failure:  digest-response");
        System.out.println("DR1.018.005 - record-failure:  JAL-Id");
        System.out.println("DR1.018.006 - record-failure:  JAL-Error-Message");
        System.out.println("DR1.018.006.001 - record-failure:  JAL-Error-Message:  Error Reasons");
        System.out.println("DR1.018.006.001.012 - record-failure:  JAL-Error-Message:  Error Reasons - JAL-Invalid-Digest-Status");

        String publisherId = UUID.randomUUID().toString();
        for (RecordType recType : RecordType.values())
        {
            if (recType.equals(RecordType.Unset))
            {
                continue;
            }
            System.out.println("Testing record type of " + recType.toString());

            //Valid initialize
            String sessionId = TestResources.sendValidInitialize(recType, true, publisherId);

            //Valid JAL record post
            String jalId = TestResources.sendValidJalRecord(recType, sessionId);

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
            assertEquals(HttpUtils.MSG_RECORD_FAILURE, messageHeader.getValue());
        }
        System.out.println("----testEmptyDigestStatusInDigestResponseMessage success----\n");
    }

    @Test
    public void testNotValidDigestStatusInDigestResponseMessage() throws ClientProtocolException, IOException {
        System.out.println("----testNotValidDigestStatusInDigestResponseMessage---");
        System.out.println("DR1.018.004 - record-failure:  digest-response");
        System.out.println("DR1.018.005 - record-failure:  JAL-Id");
        System.out.println("DR1.018.006 - record-failure:  JAL-Error-Message");
        System.out.println("DR1.018.006.001 - record-failure:  JAL-Error-Message:  Error Reasons");
        System.out.println("DR1.018.006.001.012 - record-failure:  JAL-Error-Message:  Error Reasons - JAL-Invalid-Digest-Status");

        String publisherId = UUID.randomUUID().toString();
        for (RecordType recType : RecordType.values())
        {
            if (recType.equals(RecordType.Unset))
            {
                continue;
            }
            System.out.println("Testing record type of " + recType.toString());

            //Valid initialize
            String sessionId = TestResources.sendValidInitialize(recType, true, publisherId);

            //Valid JAL record post
            String jalId = TestResources.sendValidJalRecord(recType, sessionId);

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
            assertEquals(HttpUtils.MSG_RECORD_FAILURE, messageHeader.getValue());
        }
        System.out.println("----testNotValidDigestStatusInDigestResponseMessage success----\n");
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

            final Header sessionHeader = response.getFirstHeader(HttpUtils.HDRS_SESSION_ID);
            assertNotNull(sessionHeader);
            assertEquals(sessionId, sessionHeader.getValue());

            assertNotNull(messageHeader);
            assertEquals(HttpUtils.MSG_SESSION_FAILURE, messageHeader.getValue());
        }
    }

    @Test
    public void testValidConfirmedDigestResponseMessage() throws ClientProtocolException, IOException
    {
        System.out.println("----testValidConfirmedDigestResponseMessage---");
        System.out.println("DR1.020 - digest-response");
        System.out.println("DR1.020.001.001 - digest-response:  Digest Comparison - JAL-Session-Id");
        System.out.println("DR1.020.001.002 - digest-response:  Digest Comparison - JAL-Id");
        System.out.println("DR1.020.001.003 - digest-response:  Digest Comparison - JAL-Digest-Status");
        System.out.println("DR1.020.001.003.001 - digest-response:  Digest Comparison - JAL-Digest-Status:  confirmed");
        System.out.println("DR1.022 - sync");
        System.out.println("DR1.022.001 - sync:  log-record");
        System.out.println("DR1.022.002 - sync:  audit-record");
        System.out.println("DR1.022.003 - sync:  journal-record");
        System.out.println("DR1.022.004 - sync:  digest-response");
        System.out.println("DR1.022.005 - sync:  JAL-Id");

        String publisherId = UUID.randomUUID().toString();

        //Ensures output dir is clean
        TestResources.cleanOutputDirectory(outputDirStr);

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

            //Verify record exists
            String autoNumberDir = TestResources.getAutoNumberDirectoryName(1);
            String recordDirStr = outputDirStr +  "/" + publisherId + "/" + recType.toString().toLowerCase() + "/" + autoNumberDir;
            File recordDir = new File(recordDirStr);
            assertTrue(recordDir.exists());
            assertTrue(recordDir.list().length > 0);

            //Verifies the confirmed location is empty
            String confirmDirStr = outputDirStr +  "/" + recType.toString().toLowerCase() + "/" + autoNumberDir;
            File confirmDir = new File(confirmDirStr);
            assertTrue(!confirmDir.exists());

            //Sends digest response
            final HttpPost httpPost = new HttpPost("http://localhost:" + TestResources.HTTP_PORT + "/" + recType.toString().toLowerCase());
            httpPost.setHeader(HttpUtils.HDRS_CONTENT_TYPE, HttpUtils.DEFAULT_CONTENT_TYPE);
            httpPost.setHeader(HttpUtils.HDRS_MESSAGE, HttpUtils.MSG_DIGEST_RESP);
            httpPost.setHeader(HttpUtils.HDRS_SESSION_ID, sessionId);
            httpPost.setHeader(HttpUtils.HDRS_NONCE, jalId);
            httpPost.setHeader(HttpUtils.HDRS_DIGEST_STATUS, "confirmed");

            HttpClient client = HttpClientBuilder.create().build();
            final HttpResponse response = client.execute(httpPost);

            //Verify record dir has been deleted and confirm record dir exists after confirm status
            assertTrue(confirmDir.exists());
            assertTrue(confirmDir.list().length > 0);
            assertTrue(!recordDir.exists());

            final Header jalIdHeader = response.getFirstHeader(HttpUtils.HDRS_NONCE);
            final Header messageHeader = response.getFirstHeader(HttpUtils.HDRS_MESSAGE);

            assertNotNull(jalIdHeader);
            assertEquals(jalId, jalIdHeader.getValue());

            assertNotNull(messageHeader);
            assertEquals(HttpUtils.MSG_SYNC, messageHeader.getValue());

            final Header errorHeader = response.getFirstHeader(HttpUtils.HDRS_ERROR_MESSAGE);
            assertNull(errorHeader);
        }

        System.out.println("----testValidConfirmedDigestResponseMessage success---");
    }

    @Test
    public void testValidConfirmedDigestResponseMessageCaseInsensitive() throws ClientProtocolException, IOException
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
            httpPost.setHeader("CoNTent-TyPE", "aPPlication/ocTET-strEAm");
            httpPost.setHeader("JAL-MeSSage", "dIGest-rESponse");
            httpPost.setHeader("JAL-SeSSion-ID", sessionId);
            httpPost.setHeader("Jal-ID", jalId);
            httpPost.setHeader("JAL-DiGESt-StaTUs", "conFIRmed");

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
    public void testSyncFailureDigestResponseMessage() throws ClientProtocolException, IOException
    {
        System.out.println("----testSyncFailureDigestResponseMessage---");
        System.out.println("DR1.020 - digest-response");
        System.out.println("DR1.020.001.001 - digest-response:  Digest Comparison - JAL-Session-Id");
        System.out.println("DR1.020.001.002 - digest-response:  Digest Comparison - JAL-Id");
        System.out.println("DR1.020.001.003 - digest-response:  Digest Comparison - JAL-Digest-Status");
        System.out.println("DR1.021 - sync-failure");
        System.out.println("DR1.021.001 - sync-failure:  log-record");
        System.out.println("DR1.021.002 - sync-failure:  audit-record");
        System.out.println("DR1.021.003 - sync-failure:  journal-record");
        System.out.println("DR1.021.004 - sync-failure:  digest-response");
        System.out.println("DR1.021.005 - sync-failure:  JAL-Id");
        System.out.println("DR1.021.006 - sync-failure:  JAL-Error-Message");
        System.out.println("DR1.021.006.001 - sync-failure:  JAL-Error-Message:  Error Reasons");
        System.out.println("DR1.021.006.001.001 - sync-failure:  JAL-Error-Message:  Error Reasons - JAL-Sync-Failure");

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

            //Force delete of record on the subscriber to force sync failure
            TestResources.cleanAllDirectories(inputDirStr, outputDirStr);

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
            assertEquals(HttpUtils.MSG_SYNC_FAILURE, messageHeader.getValue());

            final Header errorHeader = response.getFirstHeader(HttpUtils.HDRS_ERROR_MESSAGE);
            assertNotNull(errorHeader);
            assertEquals(HttpUtils.HDRS_SYNC_FAILURE, errorHeader.getValue());
        }

        System.out.println("----testSyncFailureDigestResponseMessage success---");
    }

    @Test
    public void testMissingPayloadFileDigestResponseMessage() throws ClientProtocolException, IOException
    {
        System.out.println("----testMissingPayloadFileDigestResponseMessage---");
        System.out.println("DR1.020 - digest-response");
        System.out.println("DR1.020.001.001 - digest-response:  Digest Comparison - JAL-Session-Id");
        System.out.println("DR1.020.001.002 - digest-response:  Digest Comparison - JAL-Id");
        System.out.println("DR1.020.001.003 - digest-response:  Digest Comparison - JAL-Digest-Status");
        System.out.println("DR1.021 - sync-failure");
        System.out.println("DR1.021.001 - sync-failure:  log-record");
        System.out.println("DR1.021.002 - sync-failure:  audit-record");
        System.out.println("DR1.021.003 - sync-failure:  journal-record");
        System.out.println("DR1.021.004 - sync-failure:  digest-response");
        System.out.println("DR1.021.005 - sync-failure:  JAL-Id");
        System.out.println("DR1.021.006 - sync-failure:  JAL-Error-Message");
        System.out.println("DR1.021.006.001 - sync-failure:  JAL-Error-Message:  Error Reasons");
        System.out.println("DR1.021.006.001.001 - sync-failure:  JAL-Error-Message:  Error Reasons - JAL-Sync-Failure");

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

            //Force delete of payload file in temporary storage area
            String autoNumberDir = TestResources.getAutoNumberDirectoryName(1);
            String payloadFileStr = outputDirStr +  "/" + publisherId + "/" + recType.toString().toLowerCase() + "/" + autoNumberDir + "/" + TestResources.PAYLOAD_FILENAME;
            File payloadFile = new File(payloadFileStr);
            payloadFile.delete();

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
            assertEquals(HttpUtils.MSG_SYNC_FAILURE, messageHeader.getValue());

            final Header errorHeader = response.getFirstHeader(HttpUtils.HDRS_ERROR_MESSAGE);
            assertNotNull(errorHeader);
            assertEquals(HttpUtils.HDRS_SYNC_FAILURE, errorHeader.getValue());
        }

        System.out.println("----testMissingPayloadFileDigestResponseMessage success---");
    }

    @Test
    public void testMissingAppMetadataFileDigestResponseMessage() throws ClientProtocolException, IOException
    {
        System.out.println("----testMissingAppMetadataFileDigestResponseMessage---");
        System.out.println("DR1.020 - digest-response");
        System.out.println("DR1.020.001.001 - digest-response:  Digest Comparison - JAL-Session-Id");
        System.out.println("DR1.020.001.002 - digest-response:  Digest Comparison - JAL-Id");
        System.out.println("DR1.020.001.003 - digest-response:  Digest Comparison - JAL-Digest-Status");
        System.out.println("DR1.021 - sync-failure");
        System.out.println("DR1.021.001 - sync-failure:  log-record");
        System.out.println("DR1.021.002 - sync-failure:  audit-record");
        System.out.println("DR1.021.003 - sync-failure:  journal-record");
        System.out.println("DR1.021.004 - sync-failure:  digest-response");
        System.out.println("DR1.021.005 - sync-failure:  JAL-Id");
        System.out.println("DR1.021.006 - sync-failure:  JAL-Error-Message");
        System.out.println("DR1.021.006.001 - sync-failure:  JAL-Error-Message:  Error Reasons");
        System.out.println("DR1.021.006.001.001 - sync-failure:  JAL-Error-Message:  Error Reasons - JAL-Sync-Failure");

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

            //Force delete of app metadata file in temporary storage area
            String autoNumberDir = TestResources.getAutoNumberDirectoryName(1);
            String appMetaFileStr = outputDirStr +  "/" + publisherId + "/" + recType.toString().toLowerCase() + "/" + autoNumberDir + "/" + TestResources.APP_META_FILENAME;
            File appMetaFile = new File(appMetaFileStr);
            appMetaFile.delete();

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
            assertEquals(HttpUtils.MSG_SYNC_FAILURE, messageHeader.getValue());

            final Header errorHeader = response.getFirstHeader(HttpUtils.HDRS_ERROR_MESSAGE);
            assertNotNull(errorHeader);
            assertEquals(HttpUtils.HDRS_SYNC_FAILURE, errorHeader.getValue());
        }

        System.out.println("----testMissingAppMetadataFileDigestResponseMessage success---");
    }

    @Test
    public void testMissingSysMetadataFileDigestResponseMessage() throws ClientProtocolException, IOException
    {
        System.out.println("----testMissingSysMetadataFileDigestResponseMessage---");
        System.out.println("DR1.020 - digest-response");
        System.out.println("DR1.020.001.001 - digest-response:  Digest Comparison - JAL-Session-Id");
        System.out.println("DR1.020.001.002 - digest-response:  Digest Comparison - JAL-Id");
        System.out.println("DR1.020.001.003 - digest-response:  Digest Comparison - JAL-Digest-Status");
        System.out.println("DR1.021 - sync-failure");
        System.out.println("DR1.021.001 - sync-failure:  log-record");
        System.out.println("DR1.021.002 - sync-failure:  audit-record");
        System.out.println("DR1.021.003 - sync-failure:  journal-record");
        System.out.println("DR1.021.004 - sync-failure:  digest-response");
        System.out.println("DR1.021.005 - sync-failure:  JAL-Id");
        System.out.println("DR1.021.006 - sync-failure:  JAL-Error-Message");
        System.out.println("DR1.021.006.001 - sync-failure:  JAL-Error-Message:  Error Reasons");
        System.out.println("DR1.021.006.001.001 - sync-failure:  JAL-Error-Message:  Error Reasons - JAL-Sync-Failure");

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

            //Force delete of sys metadata file in temporary storage area
            String autoNumberDir = TestResources.getAutoNumberDirectoryName(1);
            String sysMetaFileStr = outputDirStr +  "/" + publisherId + "/" + recType.toString().toLowerCase() + "/" + autoNumberDir + "/" + TestResources.SYS_META_FILENAME;
            File sysMetaFile = new File(sysMetaFileStr);
            sysMetaFile.delete();

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
            assertEquals(HttpUtils.MSG_SYNC_FAILURE, messageHeader.getValue());

            final Header errorHeader = response.getFirstHeader(HttpUtils.HDRS_ERROR_MESSAGE);
            assertNotNull(errorHeader);
            assertEquals(HttpUtils.HDRS_SYNC_FAILURE, errorHeader.getValue());
        }

        System.out.println("----testMissingSysMetadataFileDigestResponseMessage success---");
    }

    @Test
    public void testValidInvalidDigestResponseMessage() throws ClientProtocolException, IOException
    {
        System.out.println("----testValidInvalidDigestResponseMessage---");
        System.out.println("DR1.020 - digest-response");
        System.out.println("DR1.020.001.001 - digest-response:  Digest Comparison - JAL-Session-Id");
        System.out.println("DR1.020.001.002 - digest-response:  Digest Comparison - JAL-Id");
        System.out.println("DR1.020.001.003 - digest-response:  Digest Comparison - JAL-Digest-Status");
        System.out.println("DR1.020.001.003.002 - digest-response:  Digest Comparison - JAL-Digest-Status:  invalid");
        System.out.println("DR1.018.004 - record-failure:  digest-response");
        System.out.println("DR1.018.005 - record-failure:  JAL-Id");
        System.out.println("DR1.018.006 - record-failure:  JAL-Error-Message");
        System.out.println("DR1.018.006.001 - record-failure:  JAL-Error-Message:  Error Reasons");
        System.out.println("DR1.018.006.001.011 - record-failure:  JAL-Error-Message:  Error Reasons - JAL-Invalid-Digest");
        String publisherId = UUID.randomUUID().toString();

        //Ensures output dir is clean
        TestResources.cleanOutputDirectory(outputDirStr);

        for (RecordType recType : RecordType.values())
        {
            if (recType.equals(RecordType.Unset))
            {
                continue;
            }

            System.out.println("Testing record type of " + recType.toString());

            //Valid initialize
            String sessionId = TestResources.sendValidInitialize(recType, true, publisherId);

            //Valid JAL record post
            String jalId = TestResources.sendValidJalRecord(recType, sessionId);

            //Verify record exists
            String autoNumberDir = TestResources.getAutoNumberDirectoryName(1);
            String recordDirStr = outputDirStr +  "/" + publisherId + "/" + recType.toString().toLowerCase() + "/" + autoNumberDir;
            File recordDir = new File(recordDirStr);
            assertTrue(recordDir.exists());

            //Verifies the confirmed location is empty
            String confirmDirStr = outputDirStr +  "/" + recType.toString().toLowerCase() + "/" + autoNumberDir;
            File confirmDir = new File(confirmDirStr);
            assertTrue(!confirmDir.exists());

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

            //Ensure record was deleted and not moved to confirmed directory
            assertTrue(!recordDir.exists());
            assertTrue(!confirmDir.exists());

            assertNotNull(jalIdHeader);
            assertEquals(jalId, jalIdHeader.getValue());

            assertNotNull(messageHeader);
            assertEquals(HttpUtils.MSG_RECORD_FAILURE, messageHeader.getValue());

            final Header errorHeader = response.getFirstHeader(HttpUtils.HDRS_ERROR_MESSAGE);
            assertNotNull(errorHeader);
            assertEquals(HttpUtils.HDRS_INVALID_DIGEST, errorHeader.getValue());
        }

        System.out.println("----testValidInvalidDigestResponseMessage success----\n");
    }
}
