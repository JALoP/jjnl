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

import com.tresys.jalop.jnl.RecordType;

/**
 * Tests for common utility class.
 */
public class JournalMissingTest {

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
        TestResources.cleanAllDirectories(inputDirStr, outputDirStr);
    }

    @Test
    public void testMissingSessionIdInJournalMissingMessage() throws ClientProtocolException, IOException {

        for (RecordType recType : RecordType.values())
        {
            if (recType.equals(RecordType.Unset))
            {
                continue;
            }

            String jalId = UUID.randomUUID().toString();
            final HttpPost httpPost = new HttpPost("http://localhost:" + TestResources.HTTP_PORT + "/" + recType.toString().toLowerCase());
            httpPost.setHeader(HttpUtils.HDRS_CONTENT_TYPE, HttpUtils.DEFAULT_CONTENT_TYPE);
            httpPost.setHeader(HttpUtils.HDRS_MESSAGE, HttpUtils.MSG_JOURNAL_MISSING);
            httpPost.setHeader(HttpUtils.HDRS_NONCE, jalId);

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
    public void testEmptySessionIdInJournalMissingMessage() throws ClientProtocolException, IOException {

        for (RecordType recType : RecordType.values())
        {
            if (recType.equals(RecordType.Unset))
            {
                continue;
            }

            String jalId = UUID.randomUUID().toString();
            final HttpPost httpPost = new HttpPost("http://localhost:" + TestResources.HTTP_PORT + "/" + recType.toString().toLowerCase());
            httpPost.setHeader(HttpUtils.HDRS_CONTENT_TYPE, HttpUtils.DEFAULT_CONTENT_TYPE);
            httpPost.setHeader(HttpUtils.HDRS_MESSAGE, HttpUtils.MSG_JOURNAL_MISSING);
            httpPost.setHeader(HttpUtils.HDRS_SESSION_ID, "");
            httpPost.setHeader(HttpUtils.HDRS_NONCE, jalId);

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
    public void testMissingJalIdInJournalMissingMessage() throws ClientProtocolException, IOException {
        System.out.println("----testMissingJalIdInJournalMissingMessage---");

        //TODO needs updated for requirements
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
            httpPost.setHeader(HttpUtils.HDRS_MESSAGE, HttpUtils.MSG_JOURNAL_MISSING);
            httpPost.setHeader(HttpUtils.HDRS_SESSION_ID, sessionId);

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

        System.out.println("----testMissingJalIdInJournalMissingMessage success----\n");
    }

    @Test
    public void testEmptyJalIdInJournalMissingMessage() throws ClientProtocolException, IOException {

        System.out.println("----testEmptyJalIdInJournalMissingMessage---");

        //TODO needs updated for requirements
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
            httpPost.setHeader(HttpUtils.HDRS_MESSAGE, HttpUtils.MSG_JOURNAL_MISSING);
            httpPost.setHeader(HttpUtils.HDRS_SESSION_ID, sessionId);
            httpPost.setHeader(HttpUtils.HDRS_NONCE, "");

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
        System.out.println("----testEmptyJalIdInJournalMissingMessage success----\n");
    }

    @Test
    public void testUnsupportedRecordTypeInJournalMissingMessage() throws ClientProtocolException, IOException {

        System.out.println("----testUnsupportedRecordTypeInJournalMissingMessage---");

        //TODO needs updated for requirements
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
            httpPost.setHeader(HttpUtils.HDRS_MESSAGE, HttpUtils.MSG_JOURNAL_MISSING);
            httpPost.setHeader(HttpUtils.HDRS_SESSION_ID, sessionId);
            httpPost.setHeader(HttpUtils.HDRS_NONCE, jalId);

            HttpClient client = HttpClientBuilder.create().build();

            final HttpResponse response = client.execute(httpPost);

            //Unsupported record type error is thrown if not journal
            if (!recType.equals(RecordType.Journal))
            {
                final Header errorHeader = response.getFirstHeader(HttpUtils.HDRS_ERROR_MESSAGE);
                assertNotNull(errorHeader);
                assertEquals(HttpUtils.HDRS_UNSUPPORTED_RECORD_TYPE, errorHeader.getValue());

                final Header jalIdHeader = response.getFirstHeader(HttpUtils.HDRS_NONCE);
                final Header messageHeader = response.getFirstHeader(HttpUtils.HDRS_MESSAGE);

                assertNotNull(jalIdHeader);
                assertEquals(jalId, jalIdHeader.getValue());
                assertNotNull(messageHeader);
                assertEquals(HttpUtils.MSG_RECORD_FAILURE, messageHeader.getValue());
            }
            else
            {
                //If journal, this should get past the unsupported record type error successfully pass with no errors
                final Header errorHeader = response.getFirstHeader(HttpUtils.HDRS_ERROR_MESSAGE);
                assertNull(errorHeader);

                final Header jalIdHeader = response.getFirstHeader(HttpUtils.HDRS_NONCE);
                final Header messageHeader = response.getFirstHeader(HttpUtils.HDRS_MESSAGE);

                assertNotNull(jalIdHeader);
                assertEquals(jalId, jalIdHeader.getValue());
                assertNotNull(messageHeader);
                assertEquals(HttpUtils.MSG_JOURNAL_MISSING_RESPONSE, messageHeader.getValue());
            }
        }
        System.out.println("----testUnsupportedRecordTypeInJournalMissingMessage success----\n");
    }

    @Test
    public void testJournaMissingFailureInJournalMissingMessage() throws ClientProtocolException, IOException
    {
        String publisherId = UUID.randomUUID().toString();

        //Valid initialize
        String sessionId = TestResources.sendValidInitialize(RecordType.Journal, true, publisherId);

        //Valid JAL record post
        String jalId = TestResources.sendValidJalRecord(RecordType.Journal, sessionId);

        //Force delete of record directory to trigger a journal missing failure
        TestResources.cleanOutputDirectory(outputDirStr);

        final HttpPost httpPost = new HttpPost("http://localhost:" + TestResources.HTTP_PORT + "/" + RecordType.Journal.toString().toLowerCase());
        httpPost.setHeader(HttpUtils.HDRS_CONTENT_TYPE, HttpUtils.DEFAULT_CONTENT_TYPE);
        httpPost.setHeader(HttpUtils.HDRS_MESSAGE, HttpUtils.MSG_JOURNAL_MISSING);
        httpPost.setHeader(HttpUtils.HDRS_SESSION_ID, sessionId);
        httpPost.setHeader(HttpUtils.HDRS_NONCE, jalId);

        HttpClient client = HttpClientBuilder.create().build();
        final HttpResponse response = client.execute(httpPost);

        final Header errorHeader = response.getFirstHeader(HttpUtils.HDRS_ERROR_MESSAGE);
        assertNotNull(errorHeader);
        assertEquals(HttpUtils.HDRS_JOURNAL_MISSING_FAILURE, errorHeader.getValue());

        final Header jalIdHeader = response.getFirstHeader(HttpUtils.HDRS_NONCE);
        final Header messageHeader = response.getFirstHeader(HttpUtils.HDRS_MESSAGE);

        assertNotNull(jalIdHeader);
        assertEquals(jalId, jalIdHeader.getValue());
        assertNotNull(messageHeader);
        assertEquals(HttpUtils.MSG_RECORD_FAILURE, messageHeader.getValue());
    }

    @Test
    public void testValidJournaMissingMessage() throws ClientProtocolException, IOException
    {
        String publisherId = UUID.randomUUID().toString();

        //Valid initialize
        String sessionId = TestResources.sendValidInitialize(RecordType.Journal, true, publisherId);

        //Valid JAL record post
        String jalId = TestResources.sendValidJalRecord(RecordType.Journal, sessionId);

        final HttpPost httpPost = new HttpPost("http://localhost:" + TestResources.HTTP_PORT + "/" + RecordType.Journal.toString().toLowerCase());
        httpPost.setHeader(HttpUtils.HDRS_CONTENT_TYPE, HttpUtils.DEFAULT_CONTENT_TYPE);
        httpPost.setHeader(HttpUtils.HDRS_MESSAGE, HttpUtils.MSG_JOURNAL_MISSING);
        httpPost.setHeader(HttpUtils.HDRS_SESSION_ID, sessionId);
        httpPost.setHeader(HttpUtils.HDRS_NONCE, jalId);

        HttpClient client = HttpClientBuilder.create().build();
        final HttpResponse response = client.execute(httpPost);

        final Header jalIdHeader = response.getFirstHeader(HttpUtils.HDRS_NONCE);
        final Header messageHeader = response.getFirstHeader(HttpUtils.HDRS_MESSAGE);

        assertNotNull(jalIdHeader);
        assertEquals(jalId, jalIdHeader.getValue());

        assertNotNull(messageHeader);
        assertEquals(HttpUtils.MSG_JOURNAL_MISSING_RESPONSE, messageHeader.getValue());

        final Header errorHeader = response.getFirstHeader(HttpUtils.HDRS_ERROR_MESSAGE);
        assertNull(errorHeader);
    }
}

