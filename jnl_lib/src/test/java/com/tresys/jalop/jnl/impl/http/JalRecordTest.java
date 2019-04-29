package com.tresys.jalop.jnl.impl.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
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

/**
 * Tests for common utility class.
 */
public class JalRecordTest {

    private static Server server;

    private static final String SOURCE_APP_METADATA_FILE = "app_metadata.xml";
    private static final String SOURCE_PAYLOAD_FILE = "payload";
    private static final String SOURCE_SYS_METADATA_FILE = "sys_metadata.xml";
    private static final String JAL_RECORD_FILE = "jal_record.bin";
    private static final String BREAK_STR = "BREAK";

    //Test file names from jalop/test-input

    //Payload test files
    private static final String PAYLOAD_100MB = "100MB_audit_input.xml";
    private static final String PAYLOAD_BAD_INPUT = "bad_input.xml";
    private static final String PAYLOAD_BIG = "big_payload.txt";
    private static final String PAYLOAD_GOOD_SMALL = "good_audit_input.xml";


    //sys metadata test files
    private static final String SYS_METADATA_GOOD = "system-metadata.xml";
    private static final String SYS_METADATA_MALFORMED = "system-metadata-malformed.xml";

    //app metadata test files
    private static final String APP_METADATA_GOOD = "good_app_meta_input.xml";

    private static String jjnlDirPath = "";
    private static String jalopTestDataDir = "";
    private static String inputDirStr = "";
    private static String outputDirStr = "";
    private static String jalopTestDataRepoDir = "";
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

        //gets jjnl dir path
        resourcesDirectory = new File("src/test/resources");
        jjnlDirPath = resourcesDirectory.getAbsolutePath() + "/../../../..";
        inputDirStr = jjnlDirPath + "/input";
        jalopTestDataDir = jjnlDirPath + "/../jalop/test-input";
        outputDirStr = jjnlDirPath + "/jnl_lib/output";
        jalopTestDataRepoDir = jjnlDirPath + "/../jalop-test-data";

        TestResources.configureLogging(Level.INFO);

        //Ensures input dir exists
        File inputDir = new File(inputDirStr);
        if (!inputDir.exists())
        {
            inputDir.mkdir();
        }

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

    private class JalRecordLength
    {
        public long appMetadataLen;
        public long sysMetadataLen;
        public long payloadLen;
    }

    private JalRecordLength convertToJalRecord(String recordDir)
    {
        FileOutputStream outStream = null;
        FileInputStream sysMetadataInputStream = null;
        FileInputStream appMetadataInputStream = null;
        FileInputStream payloadInputStream = null;
        JalRecordLength jalRecordLen = new JalRecordLength();

        try
        {
            File sysMetadataFile = new File(recordDir + "/" + SOURCE_SYS_METADATA_FILE);
            File appMetadataFile = new File(recordDir + "/" + SOURCE_APP_METADATA_FILE);
            File payloadFile = new File (recordDir + "/" + SOURCE_PAYLOAD_FILE);

            File jalRecordFile = new File(recordDir + "/" + JAL_RECORD_FILE);
            outStream = new FileOutputStream(jalRecordFile);
            sysMetadataInputStream = new FileInputStream(sysMetadataFile);
            appMetadataInputStream = new FileInputStream(appMetadataFile);
            payloadInputStream = new FileInputStream(payloadFile);

            byte[] buffer = new byte[1024];

            //Reads sys metadata
            int length;
            while ((length = sysMetadataInputStream.read(buffer)) > 0){

                jalRecordLen.sysMetadataLen += length;
                outStream.write(buffer, 0, length);
            }

            //Adds BREAK
            outStream.write(BREAK_STR.getBytes());

            //Reads app metadata
            while ((length = appMetadataInputStream.read(buffer)) > 0){

                jalRecordLen.appMetadataLen += length;
                outStream.write(buffer, 0, length);
            }

            //Adds BREAK
            outStream.write(BREAK_STR.getBytes());

            //Reads payload
            while ((length = payloadInputStream.read(buffer)) > 0){

                jalRecordLen.payloadLen += length;
                outStream.write(buffer, 0, length);
            }

            //Adds BREAK
            outStream.write(BREAK_STR.getBytes());

            //Closing the input/output file streams
            sysMetadataInputStream.close();
            appMetadataInputStream.close();
            payloadInputStream.close();
            outStream.close();

            return jalRecordLen;
        }
        catch (IOException ioe)
        {
            ioe.printStackTrace();
            return null;
        }
    }

    private static void cleanOutputDirectoryByPublisherId(String publisherId) throws IOException
    {
        if (outputDirStr != null && outputDirStr.contains("output"))
        {
            File outputDir = new File(outputDirStr + "/" + publisherId);
            FileUtils.deleteDirectory(outputDir);
        }
    }

    private boolean generateRecords(RecordType recType, long numRecords, String sysFilename, String appFilename, String payloadFilename)
    {
        TestResources.cleanInputDirectory(recType, inputDirStr);
        String testDataPath = "";

        //Special case for 100MB file, which is located in jalop-test-data-repo
        if (payloadFilename == PAYLOAD_100MB)
        {
            testDataPath = jalopTestDataRepoDir + "/input/audit";
        }
        else
        {
            testDataPath = jalopTestDataDir;
        }
        try
        {
            String[] cmd = {
                    "python",
                    jjnlDirPath + "/generate_records.py",
                    recType.toString().toLowerCase(), inputDirStr, Long.toString(numRecords), jalopTestDataDir + "/" + sysFilename, jalopTestDataDir + "/" + appFilename,
                    testDataPath + "/" + payloadFilename
            };
            Process p = Runtime.getRuntime().exec(cmd);
            p.waitFor();
        }
        catch (IOException ioe)
        {
            ioe.printStackTrace();
            return false;
        }
        catch (InterruptedException ie)
        {
            ie.printStackTrace();
            return false;
        }

        //Checks to ensure the number of records specified were actually generated
        File inputDir = new File(inputDirStr + "/" + recType.toString().toLowerCase());
        File[] files = inputDir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.isDirectory();
            }
        });

        assertEquals(false, files == null);
        assertEquals(numRecords, files.length);

        return true;
    }

    private void sendJalRecords(RecordType recType, String publisherId, String expectedDigest, boolean performDigest) throws ClientProtocolException, IOException
    {
        String sessionId = TestResources.sendValidInitialize(recType, performDigest, publisherId);
        File inputDir = new File(inputDirStr + "/" + recType.toString().toLowerCase());
        File[] directoryListing = inputDir.listFiles();
        if (directoryListing != null) {
            for (File currDir : directoryListing) {
                if (!currDir.isDirectory())
                {
                    continue;
                }
                JalRecordLength recordLen = convertToJalRecord(currDir.getAbsolutePath());
                assertEquals(true, recordLen != null);

                sendJalRecord(recType, currDir, sessionId, expectedDigest, recordLen);
            }
        }
    }

    private void sendJalRecordsConcurrent(RecordType recType, String publisherId, String expectedDigest, boolean performDigest) throws ClientProtocolException, IOException
    {

        ArrayList<Thread> arrThreads = new ArrayList<Thread>();

        String sessionId = TestResources.sendValidInitialize(recType, performDigest, publisherId);
        File inputDir = new File(inputDirStr + "/" + recType.toString().toLowerCase());
        File[] directoryListing = inputDir.listFiles();
        if (directoryListing != null) {
            for (File currDir : directoryListing) {
                if (!currDir.isDirectory())
                {
                    continue;
                }

                JalRecordLength recordLen = convertToJalRecord(currDir.getAbsolutePath());
                assertEquals(true, recordLen != null);

                final RecordType currRecType = recType;
                final File currInputDir = currDir;
                final String currExpectedDigest = expectedDigest;
                final String currSessionId = sessionId;
                final JalRecordLength currRecordLen = recordLen;

                Thread t1 = new Thread(new Runnable() {
                    @Override
                    public void run(){


                        try
                        {
                            sendJalRecord(currRecType, currInputDir, currSessionId, currExpectedDigest, currRecordLen);
                        }
                        catch (IOException e)
                        {
                            e.printStackTrace();
                            //flag error
                            assertTrue(false);
                        }
                    }
                });
                t1.start();
                arrThreads.add(t1);

            }

            //Wait until all threads are done executing
            for (int i = 0; i < arrThreads.size(); i++)
            {
                try
                {
                    arrThreads.get(i).join();
                }
                catch (InterruptedException ie)
                {
                    ie.printStackTrace();
                    //flag error
                    assertTrue(false);
                }
            }
        }

        try
        {
            //Need sleep to clean up input dir correctly
            Thread.sleep(1000);
        }
        catch(Exception e)
        {
            e.printStackTrace();
            //flag error
            assertTrue(false);
        }
      //  TestResources.cleanInputDirectory(recType, inputDirStr);
    }

    private void sendJalRecord(RecordType recType, File currDir, String sessionId, String expectedDigest, JalRecordLength recordLen) throws ClientProtocolException, IOException
    {
        HttpPost httpPost = new HttpPost("http://localhost:" + TestResources.HTTP_PORT + "/" + recType.toString().toLowerCase());

        String payLoadLengthHeader = "JAL-" + recType.toString() + "-Length";
        String jalMessage = recType.toString().toLowerCase() +  "-record";

        HashMap<String, String> headers = TestResources.getJalRecordHeaders(sessionId, UUID.randomUUID().toString(), Long.toString(recordLen.sysMetadataLen), Long.toString(recordLen.appMetadataLen), Long.toString(recordLen.payloadLen), payLoadLengthHeader, jalMessage);

        for (Map.Entry<String, String> entry : headers.entrySet())
        {
            httpPost.setHeader(entry.getKey(), entry.getValue());
        }

        String jalRecordPath = currDir.getAbsolutePath() + "/" + JAL_RECORD_FILE;
        HttpEntity entity = EntityBuilder.create().setFile(new File(jalRecordPath)).build();

        httpPost.setEntity(entity);

        HttpClient client = HttpClientBuilder.create().build();

        final HttpResponse response = client.execute(httpPost);
        final String responseMessage = response.getFirstHeader(HttpUtils.HDRS_MESSAGE).getValue();
        final String responseDigest = response.getFirstHeader(HttpUtils.HDRS_DIGEST).getValue();
        final Header errorMessage = response.getFirstHeader(HttpUtils.HDRS_ERROR_MESSAGE);
        final int responseStatus = response.getStatusLine().getStatusCode();
        assertEquals(200, responseStatus);
        assertEquals(null, errorMessage);

        //Validate digest is correct for test file sent.
        assertEquals(expectedDigest, responseDigest);
        assertEquals(HttpUtils.MSG_DIGEST_CHALLENGE, responseMessage);
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

            HttpPost httpPost = new HttpPost("http://localhost:" + TestResources.HTTP_PORT + "/" + recType.toString().toLowerCase());
            for (String testValue : testValues)
            {
                String payLoadLengthHeader = "JAL-" + recType.toString() + "-Length";
                String jalMessage = recType.toString().toLowerCase() +  "-record";

                HashMap<String, String> headers = TestResources.getJalRecordHeaders(testValue, "jalId", "0", "0", "0", payLoadLengthHeader, jalMessage);

                for (Map.Entry<String, String> entry : headers.entrySet())
                {
                    httpPost.setHeader(entry.getKey(), entry.getValue());
                }

                HttpClient client = HttpClientBuilder.create().build();

                final HttpResponse response = client.execute(httpPost);
                final Header messageHeader = response.getFirstHeader(HttpUtils.HDRS_MESSAGE);
                final Header digestHeader = response.getFirstHeader(HttpUtils.HDRS_DIGEST);
                final Header jalIdHeader = response.getFirstHeader(HttpUtils.HDRS_NONCE);
                final Header errorHeader = response.getFirstHeader(HttpUtils.HDRS_ERROR_MESSAGE);
                final int responseStatus = response.getStatusLine().getStatusCode();
                assertEquals(200, responseStatus);

                assertNotNull(messageHeader);
                assertEquals(HttpUtils.MSG_SESSION_FAILURE, messageHeader.getValue());
                assertNotNull(errorHeader);
                assertEquals(HttpUtils.HDRS_UNSUPPORTED_SESSION_ID, errorHeader.getValue());
                assertNull(jalIdHeader);
                assertNull(digestHeader);
            }
        }
    }

    @Test
    public void testProcessJALRecordMessageInvalidSystemMetadataLen() throws ClientProtocolException, IOException {
        String [] testValues = new String[] {null, "", "junk", "-1", "0"};
        String publisherId = UUID.randomUUID().toString();
        for (RecordType recType : RecordType.values())
        {
            if (recType.equals(RecordType.Unset))
            {
                continue;
            }

            String sessionId = TestResources.sendValidInitialize(recType, true, publisherId);

            HttpPost httpPost = new HttpPost("http://localhost:" + TestResources.HTTP_PORT + "/" + recType.toString().toLowerCase());
            for (String testValue : testValues)
            {

                String payLoadLengthHeader = "JAL-" + recType.toString() + "-Length";
                String jalMessage = recType.toString().toLowerCase() +  "-record";

                HashMap<String, String> headers = TestResources.getJalRecordHeaders(sessionId, "jalId", testValue, "0", "0", payLoadLengthHeader, jalMessage);

                for (Map.Entry<String, String> entry : headers.entrySet())
                {
                    httpPost.setHeader(entry.getKey(), entry.getValue());
                }

                HttpClient client = HttpClientBuilder.create().build();

                final HttpResponse response = client.execute(httpPost);
                final Header messageHeader = response.getFirstHeader(HttpUtils.HDRS_MESSAGE);
                final Header errorHeader = response.getFirstHeader(HttpUtils.HDRS_ERROR_MESSAGE);
                final Header digestHeader = response.getFirstHeader(HttpUtils.HDRS_DIGEST);
                final Header jalIdHeader = response.getFirstHeader(HttpUtils.HDRS_NONCE);
                final int responseStatus = response.getStatusLine().getStatusCode();
                assertEquals(200, responseStatus);

                assertNotNull(messageHeader);
                assertEquals(HttpUtils.MSG_RECORD_FAILURE, messageHeader.getValue());
                assertNotNull(errorHeader);
                assertEquals(HttpUtils.HDRS_INVALID_SYS_META_LEN, errorHeader.getValue());
                assertNotNull(jalIdHeader);
                assertEquals("jalId", jalIdHeader.getValue());
                assertNull(digestHeader);
            }
        }
    }

    @Test
    public void testProcessJALRecordMessageInvalidAppMetadataLen() throws ClientProtocolException, IOException {
        String [] testValues = new String[] {null, "", "junk", "-1"};
        String publisherId = UUID.randomUUID().toString();
        for (RecordType recType : RecordType.values())
        {
            if (recType.equals(RecordType.Unset))
            {
                continue;
            }

            String sessionId = TestResources.sendValidInitialize(recType, true, publisherId);

            HttpPost httpPost = new HttpPost("http://localhost:" + TestResources.HTTP_PORT + "/" + recType.toString().toLowerCase());
            for (String testValue : testValues)
            {

                String payLoadLengthHeader = "JAL-" + recType.toString() + "-Length";
                String jalMessage = recType.toString().toLowerCase() +  "-record";

                HashMap<String, String> headers = TestResources.getJalRecordHeaders(sessionId, "jalId", "1", testValue, "0", payLoadLengthHeader, jalMessage);

                for (Map.Entry<String, String> entry : headers.entrySet())
                {
                    httpPost.setHeader(entry.getKey(), entry.getValue());
                }

                HttpClient client = HttpClientBuilder.create().build();

                final HttpResponse response = client.execute(httpPost);
                final Header messageHeader = response.getFirstHeader(HttpUtils.HDRS_MESSAGE);
                final Header errorHeader = response.getFirstHeader(HttpUtils.HDRS_ERROR_MESSAGE);
                final Header digestHeader = response.getFirstHeader(HttpUtils.HDRS_DIGEST);
                final Header jalIdHeader = response.getFirstHeader(HttpUtils.HDRS_NONCE);
                final int responseStatus = response.getStatusLine().getStatusCode();
                assertEquals(200, responseStatus);

                assertNotNull(messageHeader);
                assertEquals(HttpUtils.MSG_RECORD_FAILURE, messageHeader.getValue());
                assertNotNull(errorHeader);
                assertEquals(HttpUtils.HDRS_INVALID_APP_META_LEN, errorHeader.getValue());
                assertNotNull(jalIdHeader);
                assertEquals("jalId", jalIdHeader.getValue());
                assertNull(digestHeader);
            }
        }
    }

    @Test
    public void testProcessJALRecordMessageInvalidPayLoadLen() throws ClientProtocolException, IOException {
        String [] testValues = new String[] {null, "", "junk", "-1"};
        String publisherId = UUID.randomUUID().toString();
        for (RecordType recType : RecordType.values())
        {
            if (recType.equals(RecordType.Unset))
            {
                continue;
            }

            String sessionId = TestResources.sendValidInitialize(recType, true, publisherId);

            HttpPost httpPost = new HttpPost("http://localhost:" + TestResources.HTTP_PORT + "/" + recType.toString().toLowerCase());
            for (String testValue : testValues)
            {

                String payLoadLengthHeader = "JAL-" + recType.toString() + "-Length";
                String jalMessage = recType.toString().toLowerCase() +  "-record";

                HashMap<String, String> headers = TestResources.getJalRecordHeaders(sessionId, "jalId", "1", "0", testValue, payLoadLengthHeader, jalMessage);

                for (Map.Entry<String, String> entry : headers.entrySet())
                {
                    httpPost.setHeader(entry.getKey(), entry.getValue());
                }

                HttpClient client = HttpClientBuilder.create().build();

                final HttpResponse response = client.execute(httpPost);
                final Header messageHeader = response.getFirstHeader(HttpUtils.HDRS_MESSAGE);
                final Header errorHeader = response.getFirstHeader(HttpUtils.HDRS_ERROR_MESSAGE);
                final Header digestHeader = response.getFirstHeader(HttpUtils.HDRS_DIGEST);
                final Header jalIdHeader = response.getFirstHeader(HttpUtils.HDRS_NONCE);
                final int responseStatus = response.getStatusLine().getStatusCode();
                assertEquals(200, responseStatus);

                assertNotNull(messageHeader);
                assertEquals(HttpUtils.MSG_RECORD_FAILURE, messageHeader.getValue());
                assertNotNull(errorHeader);
                assertEquals("JAL-Invalid-" + recType.toString() + "-Length", errorHeader.getValue());
                assertNotNull(jalIdHeader);
                assertEquals("jalId", jalIdHeader.getValue());
                assertNull(digestHeader);
            }
        }
    }

    @Test
    public void testProcessJALRecordMessageInvalidJalMessageAudit() throws ClientProtocolException, IOException {

        HttpPost httpPost = new HttpPost("http://localhost:" + TestResources.HTTP_PORT + HttpUtils.AUDIT_ENDPOINT);

        String publisherId = UUID.randomUUID().toString();
        String sessionId = TestResources.sendValidInitialize(RecordType.Audit, true, publisherId);

        HashMap<String, String> headers = TestResources.getJalRecordHeaders(sessionId, "jalId", "1", "0", "1", HttpUtils.HDRS_JOURNAL_LEN, HttpUtils.MSG_AUDIT);

        for (Map.Entry<String, String> entry : headers.entrySet())
        {
            httpPost.setHeader(entry.getKey(), entry.getValue());
        }

        HttpClient client = HttpClientBuilder.create().build();

        final HttpResponse response = client.execute(httpPost);
        final Header messageHeader = response.getFirstHeader(HttpUtils.HDRS_MESSAGE);
        final Header errorHeader = response.getFirstHeader(HttpUtils.HDRS_ERROR_MESSAGE);
        final Header digestHeader = response.getFirstHeader(HttpUtils.HDRS_DIGEST);
        final Header jalIdHeader = response.getFirstHeader(HttpUtils.HDRS_NONCE);
        final int responseStatus = response.getStatusLine().getStatusCode();
        assertEquals(200, responseStatus);

        assertNotNull(messageHeader);
        assertEquals(HttpUtils.MSG_RECORD_FAILURE, messageHeader.getValue());
        assertNotNull(errorHeader);
        assertEquals(HttpUtils.HDRS_INVALID_AUDIT_LEN, errorHeader.getValue());
        assertNotNull(jalIdHeader);
        assertEquals("jalId", jalIdHeader.getValue());
        assertNull(digestHeader);
    }

    @Test
    public void testProcessJALRecordMessageInvalidJalMessageJournal() throws ClientProtocolException, IOException {

        HttpPost httpPost = new HttpPost("http://localhost:" + TestResources.HTTP_PORT + HttpUtils.JOURNAL_ENDPOINT);

        String publisherId = UUID.randomUUID().toString();
        String sessionId = TestResources.sendValidInitialize(RecordType.Journal, true, publisherId);

        HashMap<String, String> headers = TestResources.getJalRecordHeaders(sessionId, "jalId", "1", "0", "1", HttpUtils.HDRS_AUDIT_LEN, HttpUtils.MSG_JOURNAL);

        for (Map.Entry<String, String> entry : headers.entrySet())
        {
            httpPost.setHeader(entry.getKey(), entry.getValue());
        }

        HttpClient client = HttpClientBuilder.create().build();

        final HttpResponse response = client.execute(httpPost);
        final Header messageHeader = response.getFirstHeader(HttpUtils.HDRS_MESSAGE);
        final Header errorHeader = response.getFirstHeader(HttpUtils.HDRS_ERROR_MESSAGE);
        final Header digestHeader = response.getFirstHeader(HttpUtils.HDRS_DIGEST);
        final Header jalIdHeader = response.getFirstHeader(HttpUtils.HDRS_NONCE);
        final int responseStatus = response.getStatusLine().getStatusCode();
        assertEquals(200, responseStatus);

        assertNotNull(messageHeader);
        assertEquals(HttpUtils.MSG_RECORD_FAILURE, messageHeader.getValue());
        assertNotNull(errorHeader);
        assertEquals(HttpUtils.HDRS_INVALID_JOURNAL_LEN, errorHeader.getValue());
        assertNotNull(jalIdHeader);
        assertEquals("jalId", jalIdHeader.getValue());
        assertNull(digestHeader);
    }

    @Test
    public void testProcessJALRecordMessageInvalidJalMessageLog() throws ClientProtocolException, IOException {

        HttpPost httpPost = new HttpPost("http://localhost:" + TestResources.HTTP_PORT + HttpUtils.LOG_ENDPOINT);

        String publisherId = UUID.randomUUID().toString();
        String sessionId = TestResources.sendValidInitialize(RecordType.Log, true, publisherId);

        HashMap<String, String> headers = TestResources.getJalRecordHeaders(sessionId, "jalId", "1", "0", "1", HttpUtils.HDRS_JOURNAL_LEN, HttpUtils.MSG_LOG);

        for (Map.Entry<String, String> entry : headers.entrySet())
        {
            httpPost.setHeader(entry.getKey(), entry.getValue());
        }

        HttpClient client = HttpClientBuilder.create().build();

        final HttpResponse response = client.execute(httpPost);
        final Header messageHeader = response.getFirstHeader(HttpUtils.HDRS_MESSAGE);
        final Header errorHeader = response.getFirstHeader(HttpUtils.HDRS_ERROR_MESSAGE);
        final Header digestHeader = response.getFirstHeader(HttpUtils.HDRS_DIGEST);
        final Header jalIdHeader = response.getFirstHeader(HttpUtils.HDRS_NONCE);
        final int responseStatus = response.getStatusLine().getStatusCode();
        assertEquals(200, responseStatus);

        assertNotNull(messageHeader);
        assertEquals(HttpUtils.MSG_RECORD_FAILURE, messageHeader.getValue());
        assertNotNull(errorHeader);
        assertEquals(HttpUtils.HDRS_INVALID_LOG_LEN, errorHeader.getValue());
        assertNotNull(jalIdHeader);
        assertEquals("jalId", jalIdHeader.getValue());
        assertNull(digestHeader);
    }

    @Test
    public void testProcessJALRecordMessageUnsupportedDataClassAudit() throws ClientProtocolException, IOException {

        HttpPost httpPost = new HttpPost("http://localhost:" + TestResources.HTTP_PORT + HttpUtils.AUDIT_ENDPOINT);

        String publisherId = UUID.randomUUID().toString();
        String sessionId = TestResources.sendValidInitialize(RecordType.Audit, true, publisherId);

        HashMap<String, String> headers = TestResources.getJalRecordHeaders(sessionId, "jalId", "1", "0", "1", HttpUtils.HDRS_AUDIT_LEN, HttpUtils.MSG_JOURNAL);

        for (Map.Entry<String, String> entry : headers.entrySet())
        {
            httpPost.setHeader(entry.getKey(), entry.getValue());
        }

        HttpClient client = HttpClientBuilder.create().build();

        final HttpResponse response = client.execute(httpPost);
        final Header messageHeader = response.getFirstHeader(HttpUtils.HDRS_MESSAGE);
        final Header errorHeader = response.getFirstHeader(HttpUtils.HDRS_ERROR_MESSAGE);
        final Header digestHeader = response.getFirstHeader(HttpUtils.HDRS_DIGEST);
        final Header jalIdHeader = response.getFirstHeader(HttpUtils.HDRS_NONCE);
        final int responseStatus = response.getStatusLine().getStatusCode();
        assertEquals(200, responseStatus);

        assertNotNull(messageHeader);
        assertEquals(HttpUtils.MSG_RECORD_FAILURE, messageHeader.getValue());
        assertNotNull(errorHeader);
        assertEquals(HttpUtils.HDRS_UNSUPPORTED_DATACLASS, errorHeader.getValue());
        assertNotNull(jalIdHeader);
        assertEquals("jalId", jalIdHeader.getValue());
        assertNull(digestHeader);
    }

    @Test
    public void testProcessJALRecordMessageUnsupportedDataClassJournal() throws ClientProtocolException, IOException {

        HttpPost httpPost = new HttpPost("http://localhost:" + TestResources.HTTP_PORT + HttpUtils.JOURNAL_ENDPOINT);

        String publisherId = UUID.randomUUID().toString();
        String sessionId = TestResources.sendValidInitialize(RecordType.Journal, true, publisherId);
        HashMap<String, String> headers = TestResources.getJalRecordHeaders(sessionId, "jalId", "1", "0", "1", HttpUtils.HDRS_JOURNAL_LEN, HttpUtils.MSG_AUDIT);

        for (Map.Entry<String, String> entry : headers.entrySet())
        {
            httpPost.setHeader(entry.getKey(), entry.getValue());
        }

        HttpClient client = HttpClientBuilder.create().build();

        final HttpResponse response = client.execute(httpPost);
        final Header messageHeader = response.getFirstHeader(HttpUtils.HDRS_MESSAGE);
        final Header errorHeader = response.getFirstHeader(HttpUtils.HDRS_ERROR_MESSAGE);
        final Header digestHeader = response.getFirstHeader(HttpUtils.HDRS_DIGEST);
        final Header jalIdHeader = response.getFirstHeader(HttpUtils.HDRS_NONCE);
        final int responseStatus = response.getStatusLine().getStatusCode();
        assertEquals(200, responseStatus);

        assertNotNull(messageHeader);
        assertEquals(HttpUtils.MSG_RECORD_FAILURE, messageHeader.getValue());
        assertNotNull(errorHeader);
        assertEquals(HttpUtils.HDRS_UNSUPPORTED_DATACLASS, errorHeader.getValue());
        assertNotNull(jalIdHeader);
        assertEquals("jalId", jalIdHeader.getValue());
        assertNull(digestHeader);
    }

    @Test
    public void testProcessJALRecordMessageUnsupportedDataClassLog() throws ClientProtocolException, IOException {

        HttpPost httpPost = new HttpPost("http://localhost:" + TestResources.HTTP_PORT + HttpUtils.LOG_ENDPOINT);

        String publisherId = UUID.randomUUID().toString();
        String sessionId = TestResources.sendValidInitialize(RecordType.Log, true, publisherId);

        HashMap<String, String> headers = TestResources.getJalRecordHeaders(sessionId, "jalId", "1", "0", "1", HttpUtils.HDRS_LOG_LEN, HttpUtils.MSG_AUDIT);

        for (Map.Entry<String, String> entry : headers.entrySet())
        {
            httpPost.setHeader(entry.getKey(), entry.getValue());
        }

        HttpClient client = HttpClientBuilder.create().build();

        final HttpResponse response = client.execute(httpPost);
        final Header messageHeader = response.getFirstHeader(HttpUtils.HDRS_MESSAGE);
        final Header errorHeader = response.getFirstHeader(HttpUtils.HDRS_ERROR_MESSAGE);
        final Header digestHeader = response.getFirstHeader(HttpUtils.HDRS_DIGEST);
        final Header jalIdHeader = response.getFirstHeader(HttpUtils.HDRS_NONCE);
        final int responseStatus = response.getStatusLine().getStatusCode();
        assertEquals(200, responseStatus);

        assertNotNull(messageHeader);
        assertEquals(HttpUtils.MSG_RECORD_FAILURE, messageHeader.getValue());
        assertNotNull(errorHeader);
        assertEquals(HttpUtils.HDRS_UNSUPPORTED_DATACLASS, errorHeader.getValue());
        assertNotNull(jalIdHeader);
        assertEquals("jalId", jalIdHeader.getValue());
        assertNull(digestHeader);
    }

    @Test
    public void testProcessJALRecordMessageInvalidJalId() throws ClientProtocolException, IOException {
        String [] testValues = new String[] {null, ""};

        String publisherId = UUID.randomUUID().toString();
        for (RecordType recType : RecordType.values())
        {
            if (recType.equals(RecordType.Unset))
            {
                continue;
            }

            String sessionId = TestResources.sendValidInitialize(recType, true, publisherId);

            HttpPost httpPost = new HttpPost("http://localhost:" + TestResources.HTTP_PORT + "/" + recType.toString().toLowerCase());
            for (String testValue : testValues)
            {

                String payLoadLengthHeader = "JAL-" + recType.toString() + "-Length";
                String jalMessage = recType.toString().toLowerCase() +  "-record";

                HashMap<String, String> headers = TestResources.getJalRecordHeaders(sessionId, testValue, "1", "0", "1", payLoadLengthHeader, jalMessage);

                for (Map.Entry<String, String> entry : headers.entrySet())
                {
                    httpPost.setHeader(entry.getKey(), entry.getValue());
                }

                HttpClient client = HttpClientBuilder.create().build();

                final HttpResponse response = client.execute(httpPost);
                final Header messageHeader = response.getFirstHeader(HttpUtils.HDRS_MESSAGE);
                final Header errorHeader = response.getFirstHeader(HttpUtils.HDRS_ERROR_MESSAGE);
                final Header digestHeader = response.getFirstHeader(HttpUtils.HDRS_DIGEST);
                final Header jalIdHeader = response.getFirstHeader(HttpUtils.HDRS_NONCE);
                final int responseStatus = response.getStatusLine().getStatusCode();
                assertEquals(200, responseStatus);

                assertNotNull(messageHeader);
                assertEquals(HttpUtils.MSG_RECORD_FAILURE, messageHeader.getValue());
                assertNotNull(errorHeader);
                assertEquals(HttpUtils.HDRS_INVALID_JAL_ID, errorHeader.getValue());

                assertNull(jalIdHeader);
                assertNull(digestHeader);
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

            HttpPost httpPost = new HttpPost("http://localhost:" + TestResources.HTTP_PORT + "/" + recType.toString().toLowerCase());

            String payLoadLengthHeader = "JAL-" + recType.toString() + "-Length";
            String jalMessage = recType.toString().toLowerCase() +  "-record";

            HashMap<String, String> headers = TestResources.getJalRecordHeaders(TestResources.SESSION_ID, "jalId", "1", "0", "1", payLoadLengthHeader, jalMessage);

            for (Map.Entry<String, String> entry : headers.entrySet())
            {
                httpPost.setHeader(entry.getKey(), entry.getValue());
            }

            HttpClient client = HttpClientBuilder.create().build();

            final HttpResponse response = client.execute(httpPost);
            final Header messageHeader = response.getFirstHeader(HttpUtils.HDRS_MESSAGE);
            final Header errorHeader = response.getFirstHeader(HttpUtils.HDRS_ERROR_MESSAGE);
            final Header digestHeader = response.getFirstHeader(HttpUtils.HDRS_DIGEST);
            final Header jalIdHeader = response.getFirstHeader(HttpUtils.HDRS_NONCE);
            final int responseStatus = response.getStatusLine().getStatusCode();
            assertEquals(200, responseStatus);

            assertNotNull(messageHeader);
            assertEquals(HttpUtils.MSG_RECORD_FAILURE, messageHeader.getValue());
            assertNotNull(errorHeader);
            assertEquals(HttpUtils.HDRS_UNSUPPORTED_SESSION_ID, errorHeader.getValue());
            assertNull(jalIdHeader);
            assertNull(digestHeader);
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

            String sessionId = TestResources.sendValidInitialize(recType, true, publisherId);

            HttpPost httpPost = new HttpPost("http://localhost:" + TestResources.HTTP_PORT + "/" + recType.toString().toLowerCase());

            String payLoadLengthHeader = "JAL-" + recType.toString() + "-Length";
            String jalMessage = recType.toString().toLowerCase() +  "-record";

            HashMap<String, String> headers = TestResources.getJalRecordHeaders(sessionId, "jalId", "1", "0", "1", payLoadLengthHeader, jalMessage);

            for (Map.Entry<String, String> entry : headers.entrySet())
            {
                httpPost.setHeader(entry.getKey(), entry.getValue());
            }

            HttpClient client = HttpClientBuilder.create().build();

            final HttpResponse response = client.execute(httpPost);
            final Header messageHeader = response.getFirstHeader(HttpUtils.HDRS_MESSAGE);
            final Header errorHeader = response.getFirstHeader(HttpUtils.HDRS_ERROR_MESSAGE);
            final Header digestHeader = response.getFirstHeader(HttpUtils.HDRS_DIGEST);
            final Header jalIdHeader = response.getFirstHeader(HttpUtils.HDRS_NONCE);
            final int responseStatus = response.getStatusLine().getStatusCode();
            assertEquals(200, responseStatus);

            //Check for failure
            assertNotNull(messageHeader);
            assertEquals(HttpUtils.MSG_RECORD_FAILURE, messageHeader.getValue());
            assertNotNull(errorHeader);
            assertEquals(HttpUtils.HDRS_RECORD_FAILURE, errorHeader.getValue());
            assertNotNull(jalIdHeader);
            assertEquals("jalId", jalIdHeader.getValue());
            assertNull(digestHeader);
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

            String sessionId = TestResources.sendValidInitialize(recType, true, publisherId);

            HttpPost httpPost = new HttpPost("http://localhost:" + TestResources.HTTP_PORT + "/" + recType.toString().toLowerCase());

            String payLoadLengthHeader = "JAL-" + recType.toString() + "-Length";
            String jalMessage = recType.toString().toLowerCase() +  "-record";

            HashMap<String, String> headers = TestResources.getJalRecordHeaders(sessionId, UUID.randomUUID().toString(), "3083", "1125", "19", payLoadLengthHeader, jalMessage);

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
            final String responseMessage = response.getFirstHeader(HttpUtils.HDRS_MESSAGE).getValue();
            final Header digestHeader = response.getFirstHeader(HttpUtils.HDRS_DIGEST);
            final Header jalIdHeader = response.getFirstHeader(HttpUtils.HDRS_NONCE);
            final Header errorMessage = response.getFirstHeader(HttpUtils.HDRS_ERROR_MESSAGE);
            final int responseStatus = response.getStatusLine().getStatusCode();
            assertEquals(200, responseStatus);
            assertEquals(null, errorMessage);
            assertNotNull(digestHeader);

            //Validate digest is correct for test file sent.
            assertEquals("bbd801ce4dc24520c028025c05b44c5532b240824d2d7ce25644b73b667b6c7a", digestHeader.getValue());
            assertEquals(HttpUtils.MSG_DIGEST_CHALLENGE, responseMessage);
            assertNull(jalIdHeader);
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

            String sessionId = TestResources.sendValidInitialize(recType, false, publisherId);

            HttpPost httpPost = new HttpPost("http://localhost:" + TestResources.HTTP_PORT + "/" + recType.toString().toLowerCase());

            String payLoadLengthHeader = "JAL-" + recType.toString() + "-Length";
            String jalMessage = recType.toString().toLowerCase() +  "-record";

            String jalId = UUID.randomUUID().toString();
            HashMap<String, String> headers = TestResources.getJalRecordHeaders(sessionId, jalId, "3083", "1125", "19", payLoadLengthHeader, jalMessage);

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
            final Header messageHeader = response.getFirstHeader(HttpUtils.HDRS_MESSAGE);
            final Header digestHeader = response.getFirstHeader(HttpUtils.HDRS_DIGEST);
            final Header jalIdHeader = response.getFirstHeader(HttpUtils.HDRS_NONCE);
            final Header errorHeader = response.getFirstHeader(HttpUtils.HDRS_ERROR_MESSAGE);
            final int responseStatus = response.getStatusLine().getStatusCode();
            assertEquals(200, responseStatus);

            assertNull(errorHeader);
            assertNotNull(messageHeader);

            //Verify that sync message was sent
            assertEquals(HttpUtils.MSG_SYNC, messageHeader.getValue());

            //Validate that no digest was sent since digest was configured to be off.
            assertNull(digestHeader);
            assertNotNull(jalIdHeader);
            assertEquals(jalId, jalIdHeader.getValue());
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

            String sessionId = TestResources.sendValidInitialize(recType, true, publisherId);

            HttpPost httpPost = new HttpPost("http://localhost:" + TestResources.HTTP_PORT + "/" + recType.toString().toLowerCase());

            String payLoadLengthHeader = "JAL-" + recType.toString() + "-Length";
            String jalMessage = recType.toString().toLowerCase() +  "-record";

            String jalId = UUID.randomUUID().toString();
            HashMap<String, String> headers = TestResources.getJalRecordHeaders(sessionId, jalId, "3082", "1125", "19", payLoadLengthHeader, jalMessage);

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
            final Header messageHeader = response.getFirstHeader(HttpUtils.HDRS_MESSAGE);
            final Header errorHeader = response.getFirstHeader(HttpUtils.HDRS_ERROR_MESSAGE);
            final Header digestHeader = response.getFirstHeader(HttpUtils.HDRS_DIGEST);
            final Header jalIdHeader = response.getFirstHeader(HttpUtils.HDRS_NONCE);
            final int responseStatus = response.getStatusLine().getStatusCode();
            assertEquals(200, responseStatus);

            //Check for failure
            assertNotNull(messageHeader);
            assertEquals(HttpUtils.MSG_RECORD_FAILURE, messageHeader.getValue());
            assertNotNull(errorHeader);
            assertEquals(HttpUtils.HDRS_RECORD_FAILURE, errorHeader.getValue());
            assertNotNull(jalIdHeader);
            assertEquals(jalId, jalIdHeader.getValue());
            assertNull(digestHeader);
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

            String sessionId = TestResources.sendValidInitialize(recType, true, publisherId);

            HttpPost httpPost = new HttpPost("http://localhost:" + TestResources.HTTP_PORT + "/" + recType.toString().toLowerCase());

            String payLoadLengthHeader = "JAL-" + recType.toString() + "-Length";
            String jalMessage = recType.toString().toLowerCase() +  "-record";

            String jalId = UUID.randomUUID().toString();
            HashMap<String, String> headers = TestResources.getJalRecordHeaders(sessionId, jalId, "3083", "1126", "19", payLoadLengthHeader, jalMessage);

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
            final Header messageHeader = response.getFirstHeader(HttpUtils.HDRS_MESSAGE);
            final Header errorHeader = response.getFirstHeader(HttpUtils.HDRS_ERROR_MESSAGE);
            final Header digestHeader = response.getFirstHeader(HttpUtils.HDRS_DIGEST);
            final Header jalIdHeader = response.getFirstHeader(HttpUtils.HDRS_NONCE);
            final int responseStatus = response.getStatusLine().getStatusCode();
            assertEquals(200, responseStatus);

            assertNotNull(messageHeader);
            assertEquals(HttpUtils.MSG_RECORD_FAILURE, messageHeader.getValue());
            assertNotNull(errorHeader);
            assertEquals(HttpUtils.HDRS_RECORD_FAILURE, errorHeader.getValue());
            assertNotNull(jalIdHeader);
            assertEquals(jalId, jalIdHeader.getValue());
            assertNull(digestHeader);
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

            String sessionId = TestResources.sendValidInitialize(recType, true, publisherId);

            HttpPost httpPost = new HttpPost("http://localhost:" + TestResources.HTTP_PORT + "/" + recType.toString().toLowerCase());

            String payLoadLengthHeader = "JAL-" + recType.toString() + "-Length";
            String jalMessage = recType.toString().toLowerCase() +  "-record";

            String jalId = UUID.randomUUID().toString();
            HashMap<String, String> headers = TestResources.getJalRecordHeaders(sessionId, jalId, "3083", "1125", "18", payLoadLengthHeader, jalMessage);

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
            final Header messageHeader = response.getFirstHeader(HttpUtils.HDRS_MESSAGE);
            final Header errorHeader = response.getFirstHeader(HttpUtils.HDRS_ERROR_MESSAGE);
            final Header digestHeader = response.getFirstHeader(HttpUtils.HDRS_DIGEST);
            final Header jalIdHeader = response.getFirstHeader(HttpUtils.HDRS_NONCE);
            final int responseStatus = response.getStatusLine().getStatusCode();
            assertEquals(200, responseStatus);

            assertNotNull(messageHeader);
            assertEquals(HttpUtils.MSG_RECORD_FAILURE, messageHeader.getValue());
            assertNotNull(errorHeader);
            assertEquals(HttpUtils.HDRS_RECORD_FAILURE, errorHeader.getValue());
            assertNotNull(jalIdHeader);
            assertEquals(jalId, jalIdHeader.getValue());
            assertNull(digestHeader);
        }
    }

    @Test
    public void testMalformedSysMetdataInJALRecord() throws ClientProtocolException, IOException {
        String publisherId = UUID.randomUUID().toString();

        for (RecordType recType : RecordType.values())
        {
            try
            {
                if (recType.equals(RecordType.Unset))
                {
                    continue;
                }
                boolean result = generateRecords(recType, 1, SYS_METADATA_MALFORMED, APP_METADATA_GOOD, PAYLOAD_GOOD_SMALL );
                assertTrue(result);

                //Currently it appears no validation is performed on the subscriber on if the system metadata is malformed
                //TODO determine if this is a requirement, doesn't appear that the old code performed any validation.
                sendJalRecords(recType, publisherId, "98d22311ac6e009c84073d1fbf0aa60aca9e18104a923b2dfec4d28dbcf18c9f", true);
            }
            finally
            {
                //If you want to see the files in the output directory on the subscriber side, comment this line out so files remain after unit test execution
                cleanOutputDirectoryByPublisherId(publisherId);

                try
                {
                    //Need sleep to clean up input dir correctly
                    Thread.sleep(1000);
                }
                catch(Exception e)
                {
                }
                TestResources.cleanInputDirectory(recType, inputDirStr);
            }
        }
    }

    @Test
    public void testBadInputInJALRecord() throws ClientProtocolException, IOException {
        String publisherId = UUID.randomUUID().toString();

        for (RecordType recType : RecordType.values())
        {
            try
            {
                if (recType.equals(RecordType.Unset))
                {
                    continue;
                }

                //This is generating using the bad_input.xml test case under jalop/test-input as the payload, doesn't appear the existing old code did any validation.
                boolean result = generateRecords(recType, 1, SYS_METADATA_MALFORMED, APP_METADATA_GOOD, PAYLOAD_BAD_INPUT );
                assertTrue(result);

                sendJalRecords(recType, publisherId, "fafe52beef2cb201521f6a465db0d4c67be61a9694ccc60223ecfc7f579b319f", true);
            }
            finally
            {
                //If you want to see the files in the output directory on the subscriber side, comment this line out so files remain after unit test execution
                cleanOutputDirectoryByPublisherId(publisherId);

                try
                {
                    //Need sleep to clean up input dir correctly
                    Thread.sleep(500);
                }
                catch(Exception e)
                {
                }
                TestResources.cleanInputDirectory(recType, inputDirStr);
            }
        }
    }

    @Test
    public void testBigPayloadInJALRecord() throws ClientProtocolException, IOException {
        String publisherId = UUID.randomUUID().toString();

        for (RecordType recType : RecordType.values())
        {
            try
            {
                if (recType.equals(RecordType.Unset))
                {
                    continue;
                }

                //This is generating using the bad_input.xml test case under jalop/test-input as the payload, doesn't appear the existing old code did any validation.
                boolean result = generateRecords(recType, 10, SYS_METADATA_MALFORMED, APP_METADATA_GOOD, PAYLOAD_BIG );
                assertTrue(result);

                sendJalRecords(recType, publisherId, "e8627e7c21cf831454365077ae73793125a0d1fc1874f4b05580b46dc3d1b0ca", true);
            }
            finally
            {
                //If you want to see the files in the output directory on the subscriber side, comment this line out so files remain after unit test execution
                cleanOutputDirectoryByPublisherId(publisherId);

                try
                {
                    //Need sleep to clean up input dir correctly
                    Thread.sleep(1000);
                }
                catch(Exception e)
                {
                }
                TestResources.cleanInputDirectory(recType, inputDirStr);
            }
        }
    }

    @Test
    public void testEmptyJALRecord() throws ClientProtocolException, IOException {
        String publisherId = UUID.randomUUID().toString();

        for (RecordType recType : RecordType.values())
        {
            if (recType.equals(RecordType.Unset))
            {
                continue;
            }
            String sessionId = TestResources.sendValidInitialize(recType, true, publisherId);

            HttpPost httpPost = new HttpPost("http://localhost:" + TestResources.HTTP_PORT + "/" + recType.toString().toLowerCase());

            String payLoadLengthHeader = "JAL-" + recType.toString() + "-Length";
            String jalMessage = recType.toString().toLowerCase() +  "-record";

            String jalId = UUID.randomUUID().toString();
            HashMap<String, String> headers = TestResources.getJalRecordHeaders(sessionId, jalId, "0","0","0", payLoadLengthHeader, jalMessage);

            for (Map.Entry<String, String> entry : headers.entrySet())
            {
                httpPost.setHeader(entry.getKey(), entry.getValue());
            }

            HttpClient client = HttpClientBuilder.create().build();

            final HttpResponse response = client.execute(httpPost);
            final Header messageHeader = response.getFirstHeader(HttpUtils.HDRS_MESSAGE);
            final Header errorHeader = response.getFirstHeader(HttpUtils.HDRS_ERROR_MESSAGE);
            final Header digestHeader = response.getFirstHeader(HttpUtils.HDRS_DIGEST);
            final Header jalIdHeader = response.getFirstHeader(HttpUtils.HDRS_NONCE);
            final int responseStatus = response.getStatusLine().getStatusCode();
            assertEquals(200, responseStatus);

            assertNotNull(messageHeader);
            assertEquals(HttpUtils.MSG_RECORD_FAILURE, messageHeader.getValue());
            assertNotNull(errorHeader);
            assertEquals(HttpUtils.HDRS_INVALID_SYS_META_LEN, errorHeader.getValue());;

            assertNotNull(jalIdHeader);
            assertEquals(jalId, jalIdHeader.getValue());
            assertNull(digestHeader);
        }
    }

    @Test
    public void testEmptySysMetadataInJALRecord() throws ClientProtocolException, IOException {
        String publisherId = UUID.randomUUID().toString();

        for (RecordType recType : RecordType.values())
        {
            if (recType.equals(RecordType.Unset))
            {
                continue;
            }
            String sessionId = TestResources.sendValidInitialize(recType, true, publisherId);

            HttpPost httpPost = new HttpPost("http://localhost:" + TestResources.HTTP_PORT + "/" + recType.toString().toLowerCase());

            String payLoadLengthHeader = "JAL-" + recType.toString() + "-Length";
            String jalMessage = recType.toString().toLowerCase() +  "-record";

            String jalId = UUID.randomUUID().toString();
            HashMap<String, String> headers = TestResources.getJalRecordHeaders(sessionId, jalId, "0","1125","19", payLoadLengthHeader, jalMessage);

            for (Map.Entry<String, String> entry : headers.entrySet())
            {
                httpPost.setHeader(entry.getKey(), entry.getValue());
            }

            String jalRecordPath = resourcesDirectory.getAbsolutePath() + "/empty_sys_metadata.txt";
            HttpEntity entity = EntityBuilder.create().setFile(new File(jalRecordPath)).build();

            httpPost.setEntity(entity);

            HttpClient client = HttpClientBuilder.create().build();

            final HttpResponse response = client.execute(httpPost);
            final Header messageHeader = response.getFirstHeader(HttpUtils.HDRS_MESSAGE);
            final Header errorHeader = response.getFirstHeader(HttpUtils.HDRS_ERROR_MESSAGE);
            final Header digestHeader = response.getFirstHeader(HttpUtils.HDRS_DIGEST);
            final Header jalIdHeader = response.getFirstHeader(HttpUtils.HDRS_NONCE);
            final int responseStatus = response.getStatusLine().getStatusCode();
            assertEquals(200, responseStatus);

            assertNotNull(messageHeader);
            assertEquals(HttpUtils.MSG_RECORD_FAILURE, messageHeader.getValue());
            assertNotNull(errorHeader);
            assertEquals(HttpUtils.HDRS_INVALID_SYS_META_LEN, errorHeader.getValue());

            assertNotNull(jalIdHeader);
            assertEquals(jalId, jalIdHeader.getValue());
            assertNull(digestHeader);
        }
    }

    @Test
    public void testEmptyAppMetadataInJALRecord() throws ClientProtocolException, IOException {
        String publisherId = UUID.randomUUID().toString();

        for (RecordType recType : RecordType.values())
        {
            if (recType.equals(RecordType.Unset))
            {
                continue;
            }
            String sessionId = TestResources.sendValidInitialize(recType, true, publisherId);

            HttpPost httpPost = new HttpPost("http://localhost:" + TestResources.HTTP_PORT + "/" + recType.toString().toLowerCase());

            String payLoadLengthHeader = "JAL-" + recType.toString() + "-Length";
            String jalMessage = recType.toString().toLowerCase() +  "-record";

            String jalId = UUID.randomUUID().toString();
            HashMap<String, String> headers = TestResources.getJalRecordHeaders(sessionId, jalId, "3083","0","19", payLoadLengthHeader, jalMessage);

            for (Map.Entry<String, String> entry : headers.entrySet())
            {
                httpPost.setHeader(entry.getKey(), entry.getValue());
            }

            String jalRecordPath = resourcesDirectory.getAbsolutePath() + "/empty_app_metadata.txt";
            HttpEntity entity = EntityBuilder.create().setFile(new File(jalRecordPath)).build();

            httpPost.setEntity(entity);

            HttpClient client = HttpClientBuilder.create().build();

            final HttpResponse response = client.execute(httpPost);
            final Header messageHeader = response.getFirstHeader(HttpUtils.HDRS_MESSAGE);
            final Header errorHeader = response.getFirstHeader(HttpUtils.HDRS_ERROR_MESSAGE);
            final Header digestHeader = response.getFirstHeader(HttpUtils.HDRS_DIGEST);
            final Header jalIdHeader = response.getFirstHeader(HttpUtils.HDRS_NONCE);
            final int responseStatus = response.getStatusLine().getStatusCode();
            assertEquals(200, responseStatus);

            assertNull(errorHeader);
            assertNotNull(digestHeader);

            //Validate digest is correct for test file sent.
            assertEquals("111fc8cbbf9a1ea8010b44a348e73ee4e962a90d200b9439f28fa62edf84175e", digestHeader.getValue());
            assertNotNull(messageHeader);
            assertEquals(HttpUtils.MSG_DIGEST_CHALLENGE, messageHeader.getValue());
            assertNull(jalIdHeader);
        }

        cleanOutputDirectoryByPublisherId(publisherId);
    }

    @Test
    public void testEmptyPayloadInJALRecord() throws ClientProtocolException, IOException {
        String publisherId = UUID.randomUUID().toString();

        for (RecordType recType : RecordType.values())
        {
            if (recType.equals(RecordType.Unset))
            {
                continue;
            }
            String sessionId = TestResources.sendValidInitialize(recType, true, publisherId);

            HttpPost httpPost = new HttpPost("http://localhost:" + TestResources.HTTP_PORT + "/" + recType.toString().toLowerCase());

            String payLoadLengthHeader = "JAL-" + recType.toString() + "-Length";
            String jalMessage = recType.toString().toLowerCase() +  "-record";

            String jalId = UUID.randomUUID().toString();
            HashMap<String, String> headers = TestResources.getJalRecordHeaders(sessionId, jalId, "3083","1125","0", payLoadLengthHeader, jalMessage);

            for (Map.Entry<String, String> entry : headers.entrySet())
            {
                httpPost.setHeader(entry.getKey(), entry.getValue());
            }

            String jalRecordPath = resourcesDirectory.getAbsolutePath() + "/empty_payload.txt";
            HttpEntity entity = EntityBuilder.create().setFile(new File(jalRecordPath)).build();

            httpPost.setEntity(entity);

            HttpClient client = HttpClientBuilder.create().build();

            final HttpResponse response = client.execute(httpPost);
            final Header messageHeader = response.getFirstHeader(HttpUtils.HDRS_MESSAGE);
            final Header errorHeader = response.getFirstHeader(HttpUtils.HDRS_ERROR_MESSAGE);
            final Header digestHeader = response.getFirstHeader(HttpUtils.HDRS_DIGEST);
            final Header jalIdHeader = response.getFirstHeader(HttpUtils.HDRS_NONCE);
            final int responseStatus = response.getStatusLine().getStatusCode();
            assertEquals(200, responseStatus);

            //Only Journal and Log records can have empty payloads.
            if(recType.equals(RecordType.Journal))
            {
                assertNull(errorHeader);
                assertNotNull(messageHeader);
                assertNotNull(digestHeader);
                assertNull(jalIdHeader);

                //Validate digest is correct for test file sent.
                assertEquals("f09a91f9d22625e91bf936493f460c7a1f8ae395c0c1fb252420caede3034bfc", digestHeader.getValue());
                assertEquals(HttpUtils.MSG_DIGEST_CHALLENGE, messageHeader.getValue());
            }
            else if (recType.equals(RecordType.Audit))
            {
                assertNotNull(errorHeader);
                assertEquals(HttpUtils.HDRS_INVALID_AUDIT_LEN, errorHeader.getValue());
                assertNotNull(messageHeader);
                assertEquals(HttpUtils.MSG_RECORD_FAILURE, messageHeader.getValue());
                assertNotNull(jalIdHeader);
                assertEquals(jalId, jalIdHeader.getValue());
                assertNull(digestHeader);
            }
            else if (recType.equals(RecordType.Log))
            {
                assertNull(errorHeader);
                assertNotNull(messageHeader);
                assertNotNull(digestHeader);
                assertNull(jalIdHeader);

                //Validate digest is correct for test file sent.
                assertEquals("f09a91f9d22625e91bf936493f460c7a1f8ae395c0c1fb252420caede3034bfc", digestHeader.getValue());
                assertEquals(HttpUtils.MSG_DIGEST_CHALLENGE, messageHeader.getValue());
            }
        }

        cleanOutputDirectoryByPublisherId(publisherId);
    }

    @Test
    public void testMissingSysMetadataInJALRecord() throws ClientProtocolException, IOException {
        String publisherId = UUID.randomUUID().toString();

        for (RecordType recType : RecordType.values())
        {
            if (recType.equals(RecordType.Unset))
            {
                continue;
            }
            String sessionId = TestResources.sendValidInitialize(recType, true, publisherId);

            HttpPost httpPost = new HttpPost("http://localhost:" + TestResources.HTTP_PORT + "/" + recType.toString().toLowerCase());

            String payLoadLengthHeader = "JAL-" + recType.toString() + "-Length";
            String jalMessage = recType.toString().toLowerCase() +  "-record";

            String jalId = UUID.randomUUID().toString();
            HashMap<String, String> headers = TestResources.getJalRecordHeaders(sessionId, jalId, "0","1125","19", payLoadLengthHeader, jalMessage);

            for (Map.Entry<String, String> entry : headers.entrySet())
            {
                httpPost.setHeader(entry.getKey(), entry.getValue());
            }

            String jalRecordPath = resourcesDirectory.getAbsolutePath() + "/missing_sys_metadata.txt";
            HttpEntity entity = EntityBuilder.create().setFile(new File(jalRecordPath)).build();

            httpPost.setEntity(entity);

            HttpClient client = HttpClientBuilder.create().build();

            final HttpResponse response = client.execute(httpPost);
            final Header messageHeader = response.getFirstHeader(HttpUtils.HDRS_MESSAGE);
            final Header errorHeader = response.getFirstHeader(HttpUtils.HDRS_ERROR_MESSAGE);
            final Header digestHeader = response.getFirstHeader(HttpUtils.HDRS_DIGEST);
            final Header jalIdHeader = response.getFirstHeader(HttpUtils.HDRS_NONCE);
            final int responseStatus = response.getStatusLine().getStatusCode();
            assertEquals(200, responseStatus);

            assertNotNull(messageHeader);
            assertEquals(HttpUtils.MSG_RECORD_FAILURE, messageHeader.getValue());
            assertNotNull(errorHeader);
            assertEquals(HttpUtils.HDRS_INVALID_SYS_META_LEN, errorHeader.getValue());

            assertNotNull(jalIdHeader);
            assertEquals(jalId, jalIdHeader.getValue());
            assertNull(digestHeader);
        }
    }

    @Test
    public void testMissingAppMetadataInJALRecord() throws ClientProtocolException, IOException {
        String publisherId = UUID.randomUUID().toString();

        for (RecordType recType : RecordType.values())
        {
            if (recType.equals(RecordType.Unset))
            {
                continue;
            }
            String sessionId = TestResources.sendValidInitialize(recType, true, publisherId);

            HttpPost httpPost = new HttpPost("http://localhost:" + TestResources.HTTP_PORT + "/" + recType.toString().toLowerCase());

            String payLoadLengthHeader = "JAL-" + recType.toString() + "-Length";
            String jalMessage = recType.toString().toLowerCase() +  "-record";

            String jalId = UUID.randomUUID().toString();
            HashMap<String, String> headers = TestResources.getJalRecordHeaders(sessionId, jalId, "3083","0","19", payLoadLengthHeader, jalMessage);

            for (Map.Entry<String, String> entry : headers.entrySet())
            {
                httpPost.setHeader(entry.getKey(), entry.getValue());
            }

            String jalRecordPath = resourcesDirectory.getAbsolutePath() + "/missing_app_metadata.txt";
            HttpEntity entity = EntityBuilder.create().setFile(new File(jalRecordPath)).build();

            httpPost.setEntity(entity);

            HttpClient client = HttpClientBuilder.create().build();

            final HttpResponse response = client.execute(httpPost);
            final Header messageHeader = response.getFirstHeader(HttpUtils.HDRS_MESSAGE);
            final Header errorHeader = response.getFirstHeader(HttpUtils.HDRS_ERROR_MESSAGE);
            final Header digestHeader = response.getFirstHeader(HttpUtils.HDRS_DIGEST);
            final Header jalIdHeader = response.getFirstHeader(HttpUtils.HDRS_NONCE);
            final int responseStatus = response.getStatusLine().getStatusCode();
            assertEquals(200, responseStatus);

            assertNotNull(messageHeader);
            assertEquals(HttpUtils.MSG_RECORD_FAILURE, messageHeader.getValue());
            assertNotNull(errorHeader);
            assertEquals(HttpUtils.HDRS_RECORD_FAILURE, errorHeader.getValue());

            assertNotNull(jalIdHeader);
            assertEquals(jalId, jalIdHeader.getValue());
            assertNull(digestHeader);
        }
    }

    @Test
    public void testMissingPayloadInJALRecord() throws ClientProtocolException, IOException {
        String publisherId = UUID.randomUUID().toString();

        for (RecordType recType : RecordType.values())
        {
            if (recType.equals(RecordType.Unset))
            {
                continue;
            }
            String sessionId = TestResources.sendValidInitialize(recType, true, publisherId);

            HttpPost httpPost = new HttpPost("http://localhost:" + TestResources.HTTP_PORT + "/" + recType.toString().toLowerCase());

            String payLoadLengthHeader = "JAL-" + recType.toString() + "-Length";
            String jalMessage = recType.toString().toLowerCase() +  "-record";

            String jalId = UUID.randomUUID().toString();
            HashMap<String, String> headers = TestResources.getJalRecordHeaders(sessionId, jalId, "3083","1125","0", payLoadLengthHeader, jalMessage);

            for (Map.Entry<String, String> entry : headers.entrySet())
            {
                httpPost.setHeader(entry.getKey(), entry.getValue());
            }

            String jalRecordPath = resourcesDirectory.getAbsolutePath() + "/missing_payload.txt";
            HttpEntity entity = EntityBuilder.create().setFile(new File(jalRecordPath)).build();

            httpPost.setEntity(entity);

            HttpClient client = HttpClientBuilder.create().build();

            final HttpResponse response = client.execute(httpPost);
            final Header messageHeader = response.getFirstHeader(HttpUtils.HDRS_MESSAGE);
            final Header errorHeader = response.getFirstHeader(HttpUtils.HDRS_ERROR_MESSAGE);
            final Header digestHeader = response.getFirstHeader(HttpUtils.HDRS_DIGEST);
            final Header jalIdHeader = response.getFirstHeader(HttpUtils.HDRS_NONCE);                    ;
            final int responseStatus = response.getStatusLine().getStatusCode();
            assertEquals(200, responseStatus);

            assertNotNull(messageHeader);
            assertEquals(HttpUtils.MSG_RECORD_FAILURE, messageHeader.getValue());
            assertNotNull(errorHeader);
            assertNull(digestHeader);
            assertNotNull(jalIdHeader);
            assertEquals(jalId, jalIdHeader.getValue());

            //Only Journal and Log records can have empty payloads.
            if(recType.equals(RecordType.Journal))
            {
                assertEquals(HttpUtils.HDRS_RECORD_FAILURE, errorHeader.getValue());
            }
            else if (recType.equals(RecordType.Audit))
            {
                assertEquals(HttpUtils.HDRS_INVALID_AUDIT_LEN, errorHeader.getValue());
            }
            else if (recType.equals(RecordType.Log))
            {
                assertEquals(HttpUtils.HDRS_RECORD_FAILURE, errorHeader.getValue());
            }
        }
    }

    @Test
    public void test1000EachRecTypeSingleThread() throws ClientProtocolException, IOException {

        String publisherId = UUID.randomUUID().toString();

        for (RecordType recType : RecordType.values())
        {
            try
            {
                if (recType.equals(RecordType.Unset))
                {
                    continue;
                }
                boolean result = generateRecords(recType, 1000, SYS_METADATA_GOOD, APP_METADATA_GOOD, PAYLOAD_GOOD_SMALL );
                assertTrue(result);

                sendJalRecords(recType, publisherId, "0b601ac35ea3cd61984ad46f4b9726f4380fdf07dc69540f6ef8b594b5a013c0", true);
            }
            finally
            {
                //If you want to see the files in the output directory on the subscriber side, comment this line out so files remain after unit test execution
                cleanOutputDirectoryByPublisherId(publisherId);

                try
                {
                    //Need sleep to clean up input dir correctly
                    Thread.sleep(1000);
                }
                catch(Exception e)
                {
                }
                TestResources.cleanInputDirectory(recType, inputDirStr);
            }
        }
    }

    //This test uses 3 separate threads to send records concurrent per record type.  Each record of the same type are sent sequentially however, but concurrent across the 3 types
    @Test
    public void test1000EachRecTypeConcurrentPerRecType() throws ClientProtocolException, IOException {

        String publisherId = UUID.randomUUID().toString();
        ArrayList<Thread> arrThreads = new ArrayList<Thread>();

        for (RecordType recType : RecordType.values())
        {
            if (recType.equals(RecordType.Unset))
            {
                continue;
            }
            boolean result = generateRecords(recType, 1000, SYS_METADATA_GOOD, APP_METADATA_GOOD, PAYLOAD_GOOD_SMALL);
            assertTrue(result);
        }

        for (RecordType recType : RecordType.values())
        {

            if (recType.equals(RecordType.Unset))
            {
                continue;
            }

            final RecordType currRecType = recType;
            final String currPublisherId = publisherId;

            Thread t1 = new Thread(new Runnable() {
                @Override
                public void run(){


                    try
                    {
                        sendJalRecords(currRecType, currPublisherId, "0b601ac35ea3cd61984ad46f4b9726f4380fdf07dc69540f6ef8b594b5a013c0", true);
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                        //flag error
                        assertTrue(false);
                    }
                    finally
                    {
                        try
                        {
                            //Need sleep to clean up input dir correctly
                            Thread.sleep(1000);
                        }
                        catch(Exception e)
                        {
                            e.printStackTrace();
                            //flag error
                            assertTrue(false);
                        }
                        TestResources.cleanInputDirectory(currRecType, inputDirStr);
                    }
                }
            });
            t1.start();
            arrThreads.add(t1);

        }

        //Wait until all threads are done executing
        for (int i = 0; i < arrThreads.size(); i++)
        {
            try
            {
                arrThreads.get(i).join();
            }
            catch (InterruptedException ie)
            {
                ie.printStackTrace();
                //flag error
                assertTrue(false);
            }
        }

        //If you want to see the files in the output directory on the subscriber side, comment this line out so files remain after unit test execution
        cleanOutputDirectoryByPublisherId(publisherId);
    }


    //This test sends each record in a separate thread to test full concurrent record posts.  1000 records sent, 1000 threads all concurrent.
    @Test
    public void test1000EachRecTypeConcurrentPerEachRecord() throws ClientProtocolException, IOException {

        String publisherId = UUID.randomUUID().toString();
        ArrayList<Thread> arrThreads = new ArrayList<Thread>();

        for (RecordType recType : RecordType.values())
        {
            if (recType.equals(RecordType.Unset))
            {
                continue;
            }
            boolean result = generateRecords(recType, 1000, SYS_METADATA_GOOD, APP_METADATA_GOOD, PAYLOAD_GOOD_SMALL);
            assertTrue(result);
        }

        for (RecordType recType : RecordType.values())
        {
            if (recType.equals(RecordType.Unset))
            {
                continue;
            }

            sendJalRecordsConcurrent(recType, publisherId, "0b601ac35ea3cd61984ad46f4b9726f4380fdf07dc69540f6ef8b594b5a013c0", true);

            try
            {
                //Need sleep to clean up input dir correctly
                Thread.sleep(1000);
            }
            catch(Exception e)
            {
                e.printStackTrace();
                //flag error
                assertTrue(false);
            }
            TestResources.cleanInputDirectory(recType, inputDirStr);
        }

        //If you want to see the files in the output directory on the subscriber side, comment this line out so files remain after unit test execution
        cleanOutputDirectoryByPublisherId(publisherId);
    }

    //NOTE comment in this test to stress test the system, this test takes over 10 minutes to run as it sends 3 GB of JAL records over audit,log,journal channels
 /*   @Test
    public void test1gigEachRecTypeConcurrent() throws ClientProtocolException, IOException {

        String publisherId = UUID.randomUUID().toString();
        ArrayList<Thread> arrThreads = new ArrayList<Thread>();

        for (RecordType recType : RecordType.values())
        {
            if (recType.equals(RecordType.Unset))
            {
                continue;
            }

            //To stress test more, increase the record count param below, currently set to 10, so 10 x 100MB = 1 GB per channel.
            boolean result = generateRecords(recType, 10, SYS_METADATA_GOOD, APP_METADATA_GOOD, PAYLOAD_100MB);
            assertTrue(result);
        }

        for (RecordType recType : RecordType.values())
        {

            if (recType.equals(RecordType.Unset))
            {
                continue;
            }

            final RecordType currRecType = recType;
            final String currPublisherId = publisherId;

            Thread t1 = new Thread(new Runnable() {
                @Override
                public void run(){


                    try
                    {
                        sendJalRecords(currRecType, currPublisherId, "0ef7425362bb001b7b7a408387185319c4ae7eecba914ea7c2034fae0cd34faf", true);
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                        //flag error
                        assertTrue(false);
                    }
                    finally
                    {
                        try
                        {
                            //Need sleep to clean up input dir correctly
                            Thread.sleep(1000);
                        }
                        catch(Exception e)
                        {
                            e.printStackTrace();
                            //flag error
                            assertTrue(false);
                        }
                        cleanInputDirectory(currRecType);
                    }
                }
            });
            t1.start();
            arrThreads.add(t1);

        }

        //Wait until all threads are done executing
        for (int i = 0; i < arrThreads.size(); i++)
        {
            try
            {
                arrThreads.get(i).join();
            }
            catch (InterruptedException ie)
            {
                ie.printStackTrace();
                //flag error
                assertTrue(false);
            }
        }

        //If you want to see the files in the output directory on the subscriber side, comment this line out so files remain after unit test execution
        cleanOutputDirectoryByPublisherId(publisherId);
    } */
}
