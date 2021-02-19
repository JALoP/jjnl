package com.tresys.jalop.utils.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

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

import com.tresys.jalop.jnl.Mode;
import com.tresys.jalop.jnl.RecordType;
import com.tresys.jalop.jnl.impl.http.HttpUtils;
import com.tresys.jalop.utils.jnltest.JNLSubscriber;

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

    //global variables set from command line config parameters to configure the stress unit tests
    private static long numRecords = 0;
    private static int maxThreadCount = 10;
    private static ArrayList<RecordType> recordTypes;
    private static boolean performDigest;

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    /**
     * Sets up the server for the web service.
     *
     * @throws Exception
     */
    @BeforeClass
    public static void startWebServiceServer() throws Exception {

        //Clears out input and output directories
        TestResources.cleanAllDirectories(inputDirStr, outputDirStr);

        //gets jjnl dir path
        resourcesDirectory = new File("src/test/resources/unit_test");
        jjnlDirPath = resourcesDirectory.getAbsolutePath() + "/../../../../..";
        inputDirStr = jjnlDirPath + "/input";
        jalopTestDataDir = resourcesDirectory.getAbsolutePath(); 
        outputDirStr = jjnlDirPath + "/jnl_test/output";
        jalopTestDataRepoDir = jjnlDirPath + "/../jalop-test-data";

        TestResources.configureLogging(Level.INFO);

        //Ensures input dir exists
        File inputDir = new File(inputDirStr);
        if (!inputDir.exists())
        {
            inputDir.mkdir();
        }


        //Retrieves the optional unit test parameters to configure the stress tests
        String numRecordsStr = System.getProperty("numRecords");

        if (numRecordsStr != null)
        {
            try
            {
                numRecords = Long.parseLong(numRecordsStr);
            }
            catch(NumberFormatException e)
            {
                System.out.println("Error parsing numRecords parameter, defaulting to value of " + numRecords);
                //Just accept the default value if parse fails
            }
        }

        String recordTypesStr = System.getProperty("recordTypes");
        recordTypes = TestResources.getRecordTypes(recordTypesStr);

        String performDigestStr = System.getProperty("performDigest");
        if (performDigestStr != null && performDigestStr.trim().equalsIgnoreCase("no"))
        {
            performDigest = false;
        }
        else
        {
            performDigest = true;
        }

        //Configures logging
        String logLevel = System.getProperty("logLevel");
        if (logLevel != null)
        {
            if (logLevel.trim().equalsIgnoreCase("info"))
            {
                TestResources.configureLogging(Level.INFO);
            }
            else if (logLevel.trim().equalsIgnoreCase("warn"))
            {
                TestResources.configureLogging(Level.WARN);
            }
            else if (logLevel.trim().equalsIgnoreCase("off"))
            {
                TestResources.configureLogging(Level.OFF);
            }
            else if (logLevel.trim().equalsIgnoreCase("debug"))
            {
                TestResources.configureLogging(Level.DEBUG);
            }
            else if (logLevel.trim().equalsIgnoreCase("error"))
            {
                TestResources.configureLogging(Level.ERROR);
            }
        }

        //Allows for configurable max number of threads in thread pool
        String maxThreadCountStr = System.getProperty("maxThreadCount");
        if (maxThreadCountStr != null)
        {
            try
            {
                maxThreadCount = Integer.parseInt(maxThreadCountStr);
            }
            catch(NumberFormatException e)
            {
                System.out.println("Error parsing maxThreadCount parameter, defaulting to value of " + maxThreadCount);
                //Just accept the default value if parse fails
            }
        }

        //End config parameter parsing section

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

    public class JalRecordTask implements Runnable
    {
        private RecordType currRecType;
        private File currInputDir;
        private String currExpectedDigest;
        private String currSessionId;
        private JalRecordLength currRecordLen;
        private boolean hasError;

        public boolean getHasError()
        {
            return this.hasError;
        }

        public JalRecordTask(RecordType recType, File inputDir, String expectedDigest, String sessionId, JalRecordLength recordLen)
        {
            this.currRecType = recType;
            this.currInputDir = inputDir;
            this.currExpectedDigest = expectedDigest;
            this.currSessionId = sessionId;
            this.currRecordLen = recordLen;
            this.hasError = false;
        }

        @Override
        public void run()
        {

            try
            {
                String jalId = sendJalRecord(currRecType, currInputDir, currSessionId, currExpectedDigest, currRecordLen);

                confirmRecord(jalId, currRecType, currSessionId);
            }
            catch (IOException e)
            {
                e.printStackTrace();
                //flag error
                assertTrue(false);
                hasError = true;
            }
            catch (AssertionError ae)
            {
                hasError = true;
                throw(ae);
            }
        }
    }

    private void confirmRecord(String jalId, RecordType recType, String sessionId) throws ClientProtocolException, IOException
    {
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

    private void sendJalRecordsWithThreadPool(RecordType recType, String publisherId, String expectedDigest, boolean performDigest, ThreadPoolExecutor executor, ArrayList<JalRecordTask> jalRecordTaskList) throws ClientProtocolException, IOException
    {
        //Performs initialize for the record channel
        String sessionId = TestResources.sendValidInitialize(recType, performDigest, publisherId);

        //Loops through all the generated records and sends each record on its own thread via thread pool
        File inputDir = new File(inputDirStr + "/" + recType.toString().toLowerCase());
        File[] directoryListing = inputDir.listFiles();
        if (directoryListing != null) {
            final ArrayList<AssertionError> assertErr = new ArrayList<AssertionError>();
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

                JalRecordTask jalRecordTask = new JalRecordTask(recType, currDir, expectedDigest, sessionId, recordLen);
                jalRecordTaskList.add(jalRecordTask);
                executor.execute(jalRecordTask);
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
            final ArrayList<AssertionError> assertErr = new ArrayList<AssertionError>();
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
                        catch (AssertionError ae)
                        {
                            if (assertErr.size() == 0)
                            {
                                assertErr.add(ae);
                            }
                            throw(ae);
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

            //Ensures no assert errors occurred in any threads
            assertEquals(0, assertErr.size());
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

    private String sendJalRecord(RecordType recType, File currDir, String sessionId, String expectedDigest, JalRecordLength recordLen) throws ClientProtocolException, IOException
    {
        HttpPost httpPost = new HttpPost("http://localhost:" + TestResources.HTTP_PORT + "/" + recType.toString().toLowerCase());

        String jalId = UUID.randomUUID().toString();
        HashMap<String, String> headers = TestResources.getJalRecordHeaders(sessionId, jalId, Long.toString(recordLen.sysMetadataLen), Long.toString(recordLen.appMetadataLen), Long.toString(recordLen.payloadLen), recType);

        for (Map.Entry<String, String> entry : headers.entrySet())
        {
            httpPost.setHeader(entry.getKey(), entry.getValue());
        }

        String jalRecordPath = currDir.getAbsolutePath() + "/" + JAL_RECORD_FILE;
        HttpEntity entity = EntityBuilder.create().setFile(new File(jalRecordPath)).build();

        httpPost.setEntity(entity);

        HttpClient client = HttpClientBuilder.create().build();

        final HttpResponse response = client.execute(httpPost);
        final Header responseMessageHeader = response.getFirstHeader(HttpUtils.HDRS_MESSAGE);
        final Header responseDigestHeader = response.getFirstHeader(HttpUtils.HDRS_DIGEST_VALUE);
        final Header errorMessage = response.getFirstHeader(HttpUtils.HDRS_ERROR_MESSAGE);
        final int responseStatus = response.getStatusLine().getStatusCode();
        assertEquals(200, responseStatus);
        assertEquals(null, errorMessage);

        //Validate digest is correct for test file sent.
        if (expectedDigest != null && expectedDigest != "")
        {
            assertNotNull(responseDigestHeader);
            assertEquals(expectedDigest, responseDigestHeader.getValue());
            assertNotNull(responseMessageHeader);
            assertEquals(HttpUtils.MSG_DIGEST_CHALLENGE, responseMessageHeader.getValue());
        }
        else
        {
            assertNotNull(responseMessageHeader);
            assertEquals(HttpUtils.MSG_SYNC, responseMessageHeader.getValue());
        }

        return jalId;
    }

    @Test
    public void testProcessLiveJALRecordsRequirementTest() throws ClientProtocolException, IOException {

        System.out.println("----testProcessLiveJALRecordsRequirementTest---");
        System.out.println("DR1.017 - Transfer Records");
        System.out.println("DR1.017.001 - Transfer Records:  JAL-Id");
        System.out.println("DR1.017.002 - Transfer Records:  live");
        System.out.println("DR1.017.006 - Transfer Records:  log-record");
        System.out.println("DR1.017.006.001 - Transfer Records:  log-record - JAL-Session-Id");
        System.out.println("DR1.017.006.002 - Transfer Records:  log-record - JAL-Id");
        System.out.println("DR1.017.006.004 - Transfer Records:  log-record - JAL-System-Metadata-Length");
        System.out.println("DR1.017.006.004.001 - Transfer Records:  log-record - JAL-System-Metadata-Length:  In Bytes");
        System.out.println("DR1.017.006.005 - Transfer Records:  log-record - JAL-Application-Metadata-Length");
        System.out.println("DR1.017.006.005.001 - Transfer Records:  log-record - JAL-Application-Metadata-Length:  In Bytes");
        System.out.println("DR1.017.006.006 - Transfer Records:  log-record - JAL-Log-Length");
        System.out.println("DR1.017.006.006.001 - Transfer Records:  log-record - JAL-Log-Length:  In Bytes");
        System.out.println("DR1.017.006.007 - Transfer Records:  log-record - Log Record");
        System.out.println("DR1.017.006.007.001 - Transfer Records:  log-record - Log Record:  JAL-Log-Length");
        System.out.println("DR1.017.006.007.002 - Transfer Records:  log-record - Log Record:  JAL-System-Metadata-Length");
        System.out.println("DR1.017.006.007.003 - Transfer Records:  log-record - Log Record:  JAL-Application-Metadata-Length");
        System.out.println("DR1.017.007 - Transfer Records:  audit-record");
        System.out.println("DR1.017.007.001 - Transfer Records:  audit-record - JAL-Session-Id");
        System.out.println("DR1.017.007.002 - Transfer Records:  audit-record - JAL-Id");
        System.out.println("DR1.017.007.005 - Transfer Records:  audit-record - JAL-System-Metadata-Length");
        System.out.println("DR1.017.007.005.001 - Transfer Records:  audit-record - JAL-System-Metadata-Length:  In Bytes");
        System.out.println("DR1.017.007.006 - Transfer Records:  audit-record - JAL-Application-Metadata-Length");
        System.out.println("DR1.017.007.006.001 - Transfer Records:  audit-record - JAL-Application-Metadata-Length:  In Bytes");
        System.out.println("DR1.017.007.007 - Transfer Records:  audit-record - JAL-Audit-Length");
        System.out.println("DR1.017.007.007.001 - Transfer Records:  audit-record - JAL-Audit-Length:  In Bytes");
        System.out.println("DR1.017.007.008 - Transfer Records:  audit-record - Audit Record");
        System.out.println("DR1.017.007.008.002 - Transfer Records:  audit-record - Audit Record:  JAL-Audit-Length");
        System.out.println("DR1.017.007.008.003 - Transfer Records:  audit-record - Audit Record:  JAL-System-Metadata-Length");
        System.out.println("DR1.017.007.008.004 - Transfer Records:  audit-record - Audit Record:  JAL-Application-Metadata-Length");
        System.out.println("DR1.017.008 - Transfer Records:  journal-record");
        System.out.println("DR1.017.008.001 - Transfer Records:  journal-record - JAL-Session-Id");
        System.out.println("DR1.017.008.002 - Transfer Records:  journal-record - JAL-Id");
        System.out.println("DR1.017.008.004 - Transfer Records:  journal-record - JAL-System-Metadata-Length");
        System.out.println("DR1.017.008.004.001 - Transfer Records:  journal-record - JAL-System-Metadata-Length:  In Bytes");
        System.out.println("DR1.017.008.005 - Transfer Records:  journal-record - JAL-Application-Metadata-Length");
        System.out.println("DR1.017.008.005.001 - Transfer Records:  journal-record - JAL-Application-Metadata-Length:  In Bytes");
        System.out.println("DR1.017.008.006 - Transfer Records:  journal-record - JAL-Journal-Length");
        System.out.println("DR1.017.008.006.001 - Transfer Records:  journal-record - JAL-Journal-Length:  In Bytes");
        System.out.println("DR1.017.008.007 - Transfer Records:  journal-record - Journal Record");
        System.out.println("DR1.017.008.007.001 - Transfer Records:  journal-record - Journal Record:  JAL-Journal-Length");
        System.out.println("DR1.017.008.007.002 - Transfer Records:  journal-record - Journal Record:  JAL-System-Metadata-Length");
        System.out.println("DR1.017.008.007.003 - Transfer Records:  journal-record - Journal Record:  JAL-Application-Metadata-Length");

        String publisherId = UUID.randomUUID().toString();
        for (RecordType recType : RecordType.values())
        {
            if (recType.equals(RecordType.Unset))
            {
                continue;
            }

            System.out.println("Testing record type of " + recType.toString() + " with mode of live");
            String sessionId = TestResources.sendValidInitialize(recType, true, publisherId, HttpUtils.MSG_LIVE);

            HttpPost httpPost = new HttpPost("http://localhost:" + TestResources.HTTP_PORT + "/" + recType.toString().toLowerCase());

            String jalId = UUID.randomUUID().toString();
            HashMap<String, String> headers = TestResources.getJalRecordHeaders(sessionId, jalId, "3083", "1125", "19", recType);

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

        System.out.println("----testProcessLiveJALRecordsRequirementTest success----\n");
    }

    @Test
    public void testProcessArchiveJALRecordsRequirementTest() throws ClientProtocolException, IOException {

        System.out.println("----testProcessArchiveJALRecordsRequirementTest---");
        System.out.println("DR1.017 - Transfer Records");
        System.out.println("DR1.017.001 - Transfer Records:  JAL-Id");
        System.out.println("DR1.017.003 - Transfer Records:  archive");
        System.out.println("DR1.017.006 - Transfer Records:  log-record");
        System.out.println("DR1.017.006.001 - Transfer Records:  log-record - JAL-Session-Id");
        System.out.println("DR1.017.006.002 - Transfer Records:  log-record - JAL-Id");
        System.out.println("DR1.017.006.004 - Transfer Records:  log-record - JAL-System-Metadata-Length");
        System.out.println("DR1.017.006.004.001 - Transfer Records:  log-record - JAL-System-Metadata-Length:  In Bytes");
        System.out.println("DR1.017.006.005 - Transfer Records:  log-record - JAL-Application-Metadata-Length");
        System.out.println("DR1.017.006.005.001 - Transfer Records:  log-record - JAL-Application-Metadata-Length:  In Bytes");
        System.out.println("DR1.017.006.006 - Transfer Records:  log-record - JAL-Log-Length");
        System.out.println("DR1.017.006.006.001 - Transfer Records:  log-record - JAL-Log-Length:  In Bytes");
        System.out.println("DR1.017.006.007 - Transfer Records:  log-record - Log Record");
        System.out.println("DR1.017.006.007.001 - Transfer Records:  log-record - Log Record:  JAL-Log-Length");
        System.out.println("DR1.017.006.007.002 - Transfer Records:  log-record - Log Record:  JAL-System-Metadata-Length");
        System.out.println("DR1.017.006.007.003 - Transfer Records:  log-record - Log Record:  JAL-Application-Metadata-Length");
        System.out.println("DR1.017.007 - Transfer Records:  audit-record");
        System.out.println("DR1.017.007.001 - Transfer Records:  audit-record - JAL-Session-Id");
        System.out.println("DR1.017.007.002 - Transfer Records:  audit-record - JAL-Id");
        System.out.println("DR1.017.007.005 - Transfer Records:  audit-record - JAL-System-Metadata-Length");
        System.out.println("DR1.017.007.005.001 - Transfer Records:  audit-record - JAL-System-Metadata-Length:  In Bytes");
        System.out.println("DR1.017.007.006 - Transfer Records:  audit-record - JAL-Application-Metadata-Length");
        System.out.println("DR1.017.007.006.001 - Transfer Records:  audit-record - JAL-Application-Metadata-Length:  In Bytes");
        System.out.println("DR1.017.007.007 - Transfer Records:  audit-record - JAL-Audit-Length");
        System.out.println("DR1.017.007.007.001 - Transfer Records:  audit-record - JAL-Audit-Length:  In Bytes");
        System.out.println("DR1.017.007.008 - Transfer Records:  audit-record - Audit Record");
        System.out.println("DR1.017.007.008.002 - Transfer Records:  audit-record - Audit Record:  JAL-Audit-Length");
        System.out.println("DR1.017.007.008.003 - Transfer Records:  audit-record - Audit Record:  JAL-System-Metadata-Length");
        System.out.println("DR1.017.007.008.004 - Transfer Records:  audit-record - Audit Record:  JAL-Application-Metadata-Length");
        System.out.println("DR1.017.008 - Transfer Records:  journal-record");
        System.out.println("DR1.017.008.001 - Transfer Records:  journal-record - JAL-Session-Id");
        System.out.println("DR1.017.008.002 - Transfer Records:  journal-record - JAL-Id");
        System.out.println("DR1.017.008.004 - Transfer Records:  journal-record - JAL-System-Metadata-Length");
        System.out.println("DR1.017.008.004.001 - Transfer Records:  journal-record - JAL-System-Metadata-Length:  In Bytes");
        System.out.println("DR1.017.008.005 - Transfer Records:  journal-record - JAL-Application-Metadata-Length");
        System.out.println("DR1.017.008.005.001 - Transfer Records:  journal-record - JAL-Application-Metadata-Length:  In Bytes");
        System.out.println("DR1.017.008.006 - Transfer Records:  journal-record - JAL-Journal-Length");
        System.out.println("DR1.017.008.006.001 - Transfer Records:  journal-record - JAL-Journal-Length:  In Bytes");
        System.out.println("DR1.017.008.007 - Transfer Records:  journal-record - Journal Record");
        System.out.println("DR1.017.008.007.001 - Transfer Records:  journal-record - Journal Record:  JAL-Journal-Length");
        System.out.println("DR1.017.008.007.002 - Transfer Records:  journal-record - Journal Record:  JAL-System-Metadata-Length");
        System.out.println("DR1.017.008.007.003 - Transfer Records:  journal-record - Journal Record:  JAL-Application-Metadata-Length");

        //Set archive mode on the subscriber
        JNLSubscriber subscriber = (JNLSubscriber)HttpUtils.getSubscriber();
        subscriber.getConfig().setMode(Mode.Archive);

        try
        {
            String publisherId = UUID.randomUUID().toString();
            for (RecordType recType : RecordType.values())
            {
                if (recType.equals(RecordType.Unset))
                {
                    continue;
                }


                System.out.println("Testing record type of " + recType.toString() + " with mode of archive");
                String sessionId = TestResources.sendValidInitialize(recType, true, publisherId, HttpUtils.MSG_ARCHIVE);

                //send 3 archive records
                for (int i=0; i < 3; i++)
                {
                    HttpPost httpPost = new HttpPost("http://localhost:" + TestResources.HTTP_PORT + "/" + recType.toString().toLowerCase());

                    String jalId = UUID.randomUUID().toString();
                    HashMap<String, String> headers = TestResources.getJalRecordHeaders(sessionId, jalId, "3083", "1125", "19", recType);

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

        }
        finally
        {
            //Reset subscriber back to live mode
            subscriber.getConfig().setMode(Mode.Live);
        }

        System.out.println("----testProcessArchiveJALRecordsRequirementTest success----\n");
    }

    @Test
    public void testProcessJALRecordsCaseInsensitiveTest() throws ClientProtocolException, IOException {

        JNLSubscriber subscriber = (JNLSubscriber)HttpUtils.getSubscriber();
        String [] modes = new String[] {"lIVe"};
        String publisherId = UUID.randomUUID().toString();
        for (RecordType recType : RecordType.values())
        {
            if (recType.equals(RecordType.Unset))
            {
                continue;
            }

            for (String currMode : modes)
            {
                subscriber.getConfig().setMode(HttpUtils.getMode(currMode));
                System.out.println("Testing record type of " + recType.toString() + " with mode of " + currMode);
                String sessionId = TestResources.sendValidInitialize(recType, true, publisherId, currMode);

                HttpPost httpPost = new HttpPost("http://localhost:" + TestResources.HTTP_PORT + "/" + recType.toString().toLowerCase());

                String jalId = UUID.randomUUID().toString();
                HashMap<String, String> headers = TestResources.getJalRecordHeaders(sessionId, jalId, "3083", "1125", "19", recType);

                for (Map.Entry<String, String> entry : headers.entrySet())
                {
                    httpPost.setHeader(entry.getKey().toLowerCase(), entry.getValue().toLowerCase());
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

        subscriber.getConfig().setMode(Mode.Live);
    }

    @Test
    public void testProcessJALRecordMessageInvalidSessionId() throws ClientProtocolException, IOException {

        System.out.println("----testProcessJALRecordMessageInvalidSessionId---");
        System.out.println("DR1.017.006.001 - Transfer Records:  log-record - JAL-Session-Id");
        System.out.println("DR1.017.007.001 - Transfer Records:  audit-record - JAL-Session-Id");
        System.out.println("DR1.017.008.001 - Transfer Records:  journal-record - JAL-Session-Id");

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
                HashMap<String, String> headers = TestResources.getJalRecordHeaders(testValue, "jalId", "0", "0", "0", recType);

                for (Map.Entry<String, String> entry : headers.entrySet())
                {
                    httpPost.setHeader(entry.getKey(), entry.getValue());
                }

                HttpClient client = HttpClientBuilder.create().build();

                final HttpResponse response = client.execute(httpPost);
                final Header messageHeader = response.getFirstHeader(HttpUtils.HDRS_MESSAGE);
                final Header digestHeader = response.getFirstHeader(HttpUtils.HDRS_DIGEST_VALUE);
                final Header jalIdHeader = response.getFirstHeader(HttpUtils.HDRS_NONCE);
                final Header errorHeader = response.getFirstHeader(HttpUtils.HDRS_ERROR_MESSAGE);
                final int responseStatus = response.getStatusLine().getStatusCode();
                assertEquals(200, responseStatus);

                assertNotNull(messageHeader);
                assertEquals(HttpUtils.MSG_SESSION_FAILURE, messageHeader.getValue());
                assertNotNull(errorHeader);
                assertEquals(HttpUtils.HDRS_UNSUPPORTED_SESSION_ID, errorHeader.getValue());
                assertNotNull(jalIdHeader);
                assertEquals("jalId", jalIdHeader.getValue());

                final Header sessionHeader = response.getFirstHeader(HttpUtils.HDRS_SESSION_ID);

                if (testValue == null || testValue.equals(""))
                {
                    assertNull(sessionHeader);
                }
                else
                {
                    assertNotNull(sessionHeader);
                    assertEquals(testValue, sessionHeader.getValue());
                }
                assertNull(digestHeader);
            }
        }

        System.out.println("----testProcessJALRecordMessageInvalidSessionId success----\n");
    }

    @Test
    public void testProcessJALRecordMessageInvalidSystemMetadataLen() throws ClientProtocolException, IOException {

        System.out.println("----testProcessJALRecordMessageInvalidSystemMetadataLen---");
        System.out.println("DR1.018.001 - record-failure:  log-record");
        System.out.println("DR1.018.002 - record-failure:  audit-record");
        System.out.println("DR1.018.003 - record-failure:  journal-record");
        System.out.println("DR1.018.005 - record-failure:  JAL-Id");
        System.out.println("DR1.018.006 - record-failure:  JAL-Error-Message");
        System.out.println("DR1.018.006.001 - record-failure:  JAL-Error-Message:  Error Reasons");
        System.out.println("DR1.018.006.001.001 - record-failure:  JAL-Error-Message:  Error Reasons - JAL-Invalid-System-Metadata-Length");

        String [] testValues = new String[] {null, "", "junk", "-1", "0"};
        String publisherId = UUID.randomUUID().toString();
        for (RecordType recType : RecordType.values())
        {
            if (recType.equals(RecordType.Unset))
            {
                continue;
            }

            System.out.println("Testing record type of " + recType.toString());

            String sessionId = TestResources.sendValidInitialize(recType, true, publisherId);

            HttpPost httpPost = new HttpPost("http://localhost:" + TestResources.HTTP_PORT + "/" + recType.toString().toLowerCase());
            for (String testValue : testValues)
            {
                HashMap<String, String> headers = TestResources.getJalRecordHeaders(sessionId, "jalId", testValue, "0", "0", recType);

                for (Map.Entry<String, String> entry : headers.entrySet())
                {
                    httpPost.setHeader(entry.getKey(), entry.getValue());
                }

                HttpClient client = HttpClientBuilder.create().build();

                final HttpResponse response = client.execute(httpPost);
                final Header messageHeader = response.getFirstHeader(HttpUtils.HDRS_MESSAGE);
                final Header errorHeader = response.getFirstHeader(HttpUtils.HDRS_ERROR_MESSAGE);
                final Header digestHeader = response.getFirstHeader(HttpUtils.HDRS_DIGEST_VALUE);
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

        System.out.println("----testProcessJALRecordMessageInvalidSystemMetadataLen success----\n");
    }

    @Test
    public void testProcessJALRecordMessageInvalidAppMetadataLen() throws ClientProtocolException, IOException {
        System.out.println("----testProcessJALRecordMessageInvalidAppMetadataLen---");
        System.out.println("DR1.018.001 - record-failure:  log-record");
        System.out.println("DR1.018.002 - record-failure:  audit-record");
        System.out.println("DR1.018.003 - record-failure:  journal-record");
        System.out.println("DR1.018.005 - record-failure:  JAL-Id");
        System.out.println("DR1.018.006 - record-failure:  JAL-Error-Message");
        System.out.println("DR1.018.006.001 - record-failure:  JAL-Error-Message:  Error Reasons");
        System.out.println("DR1.018.006.001.002 - record-failure:  JAL-Error-Message:  Error Reasons - JAL-Invalid-Application-Metadata-Length");

        String [] testValues = new String[] {null, "", "junk", "-1"};
        String publisherId = UUID.randomUUID().toString();
        for (RecordType recType : RecordType.values())
        {
            if (recType.equals(RecordType.Unset))
            {
                continue;
            }

            System.out.println("Testing record type of " + recType.toString());

            String sessionId = TestResources.sendValidInitialize(recType, true, publisherId);

            HttpPost httpPost = new HttpPost("http://localhost:" + TestResources.HTTP_PORT + "/" + recType.toString().toLowerCase());
            for (String testValue : testValues)
            {
                HashMap<String, String> headers = TestResources.getJalRecordHeaders(sessionId, "jalId", "1", testValue, "0", recType);

                for (Map.Entry<String, String> entry : headers.entrySet())
                {
                    httpPost.setHeader(entry.getKey(), entry.getValue());
                }

                HttpClient client = HttpClientBuilder.create().build();

                final HttpResponse response = client.execute(httpPost);
                final Header messageHeader = response.getFirstHeader(HttpUtils.HDRS_MESSAGE);
                final Header errorHeader = response.getFirstHeader(HttpUtils.HDRS_ERROR_MESSAGE);
                final Header digestHeader = response.getFirstHeader(HttpUtils.HDRS_DIGEST_VALUE);
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
        System.out.println("----testProcessJALRecordMessageInvalidAppMetadataLen success----\n");
    }

    @Test
    public void testProcessJALRecordMessageInvalidPayLoadLen() throws ClientProtocolException, IOException {

        System.out.println("----testProcessJALRecordMessageInvalidPayLoadLen---");
        System.out.println("DR1.018.001 - record-failure:  log-record");
        System.out.println("DR1.018.002 - record-failure:  audit-record");
        System.out.println("DR1.018.003 - record-failure:  journal-record");
        System.out.println("DR1.018.005 - record-failure:  JAL-Id");
        System.out.println("DR1.018.006 - record-failure:  JAL-Error-Message");
        System.out.println("DR1.018.006.001 - record-failure:  JAL-Error-Message:  Error Reasons");
        System.out.println("DR1.018.006.001.003 - record-failure:  JAL-Error-Message:  Error Reasons - JAL-Invalid-Journal-Length");
        System.out.println("DR1.018.006.001.004 - record-failure:  JAL-Error-Message:  Error Reasons - JAL-Invalid-Audit-Length");
        System.out.println("DR1.018.006.001.005 - record-failure:  JAL-Error-Message:  Error Reasons - JAL-Invalid-Log-Length");

        String [] testValues = new String[] {null, "", "junk", "-1"};
        String publisherId = UUID.randomUUID().toString();
        for (RecordType recType : RecordType.values())
        {
            if (recType.equals(RecordType.Unset))
            {
                continue;
            }

            System.out.println("Testing record type of " + recType.toString());

            String sessionId = TestResources.sendValidInitialize(recType, true, publisherId);

            HttpPost httpPost = new HttpPost("http://localhost:" + TestResources.HTTP_PORT + "/" + recType.toString().toLowerCase());
            for (String testValue : testValues)
            {
                HashMap<String, String> headers = TestResources.getJalRecordHeaders(sessionId, "jalId", "1", "0", testValue, recType);

                for (Map.Entry<String, String> entry : headers.entrySet())
                {
                    httpPost.setHeader(entry.getKey(), entry.getValue());
                }

                HttpClient client = HttpClientBuilder.create().build();

                final HttpResponse response = client.execute(httpPost);
                final Header messageHeader = response.getFirstHeader(HttpUtils.HDRS_MESSAGE);
                final Header errorHeader = response.getFirstHeader(HttpUtils.HDRS_ERROR_MESSAGE);
                final Header digestHeader = response.getFirstHeader(HttpUtils.HDRS_DIGEST_VALUE);
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

        System.out.println("----testProcessJALRecordMessageInvalidPayLoadLen success----\n");
    }

    @Test
    public void testProcessJALRecordMessageInvalidJalMessageAudit() throws ClientProtocolException, IOException {

        System.out.println("----testProcessJALRecordMessageInvalidJalMessageAudit---");
        System.out.println("DR1.018.002 - record-failure:  audit-record");
        System.out.println("DR1.018.005 - record-failure:  JAL-Id");
        System.out.println("DR1.018.006 - record-failure:  JAL-Error-Message");
        System.out.println("DR1.018.006.001 - record-failure:  JAL-Error-Message:  Error Reasons");
        System.out.println("DR1.018.006.001.004 - record-failure:  JAL-Error-Message:  Error Reasons - JAL-Invalid-Audit-Length");

        HttpPost httpPost = new HttpPost("http://localhost:" + TestResources.HTTP_PORT + HttpUtils.AUDIT_ENDPOINT);

        String publisherId = UUID.randomUUID().toString();
        String sessionId = TestResources.sendValidInitialize(RecordType.Audit, true, publisherId);

        HashMap<String, String> headers = TestResources.getJalRecordHeaders(sessionId, "jalId", "1", "0", "1", HttpUtils.HDRS_JOURNAL_LEN, HttpUtils.MSG_AUDIT, HttpUtils.ENC_XML);

        for (Map.Entry<String, String> entry : headers.entrySet())
        {
            httpPost.setHeader(entry.getKey(), entry.getValue());
        }

        HttpClient client = HttpClientBuilder.create().build();

        final HttpResponse response = client.execute(httpPost);
        final Header messageHeader = response.getFirstHeader(HttpUtils.HDRS_MESSAGE);
        final Header errorHeader = response.getFirstHeader(HttpUtils.HDRS_ERROR_MESSAGE);
        final Header digestHeader = response.getFirstHeader(HttpUtils.HDRS_DIGEST_VALUE);
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

        System.out.println("----testProcessJALRecordMessageInvalidJalMessageAudit success----\n");
    }

    @Test
    public void testProcessJALRecordMessageInvalidJalMessageJournal() throws ClientProtocolException, IOException {

        System.out.println("----testProcessJALRecordMessageInvalidJalMessageJournal---");
        System.out.println("DR1.018.003 - record-failure:  journal-record");
        System.out.println("DR1.018.005 - record-failure:  JAL-Id");
        System.out.println("DR1.018.006 - record-failure:  JAL-Error-Message");
        System.out.println("DR1.018.006.001 - record-failure:  JAL-Error-Message:  Error Reasons");
        System.out.println("DR1.018.006.001.003 - record-failure:  JAL-Error-Message:  Error Reasons - JAL-Invalid-Journal-Length");

        HttpPost httpPost = new HttpPost("http://localhost:" + TestResources.HTTP_PORT + HttpUtils.JOURNAL_ENDPOINT);

        String publisherId = UUID.randomUUID().toString();
        String sessionId = TestResources.sendValidInitialize(RecordType.Journal, true, publisherId);

        HashMap<String, String> headers = TestResources.getJalRecordHeaders(sessionId, "jalId", "1", "0", "1", HttpUtils.HDRS_AUDIT_LEN, HttpUtils.MSG_JOURNAL, HttpUtils.ENC_XML);

        for (Map.Entry<String, String> entry : headers.entrySet())
        {
            httpPost.setHeader(entry.getKey(), entry.getValue());
        }

        HttpClient client = HttpClientBuilder.create().build();

        final HttpResponse response = client.execute(httpPost);
        final Header messageHeader = response.getFirstHeader(HttpUtils.HDRS_MESSAGE);
        final Header errorHeader = response.getFirstHeader(HttpUtils.HDRS_ERROR_MESSAGE);
        final Header digestHeader = response.getFirstHeader(HttpUtils.HDRS_DIGEST_VALUE);
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

        System.out.println("----testProcessJALRecordMessageInvalidJalMessageJournal success----\n");
    }

    @Test
    public void testProcessJALRecordMessageInvalidJalMessageLog() throws ClientProtocolException, IOException {

        System.out.println("----testProcessJALRecordMessageInvalidJalMessageLog---");
        System.out.println("DR1.018.001 - record-failure:  log-record");
        System.out.println("DR1.018.005 - record-failure:  JAL-Id");
        System.out.println("DR1.018.006 - record-failure:  JAL-Error-Message");
        System.out.println("DR1.018.006.001 - record-failure:  JAL-Error-Message:  Error Reasons");
        System.out.println("DR1.018.006.001.005 - record-failure:  JAL-Error-Message:  Error Reasons - JAL-Invalid-Log-Length");

        HttpPost httpPost = new HttpPost("http://localhost:" + TestResources.HTTP_PORT + HttpUtils.LOG_ENDPOINT);

        String publisherId = UUID.randomUUID().toString();
        String sessionId = TestResources.sendValidInitialize(RecordType.Log, true, publisherId);

        HashMap<String, String> headers = TestResources.getJalRecordHeaders(sessionId, "jalId", "1", "0", "1", HttpUtils.HDRS_JOURNAL_LEN, HttpUtils.MSG_LOG, HttpUtils.ENC_XML);

        for (Map.Entry<String, String> entry : headers.entrySet())
        {
            httpPost.setHeader(entry.getKey(), entry.getValue());
        }

        HttpClient client = HttpClientBuilder.create().build();

        final HttpResponse response = client.execute(httpPost);
        final Header messageHeader = response.getFirstHeader(HttpUtils.HDRS_MESSAGE);
        final Header errorHeader = response.getFirstHeader(HttpUtils.HDRS_ERROR_MESSAGE);
        final Header digestHeader = response.getFirstHeader(HttpUtils.HDRS_DIGEST_VALUE);
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

        System.out.println("----testProcessJALRecordMessageInvalidJalMessageLog success----\n");
    }

    @Test
    public void testProcessJALRecordMessageUnsupportedRecordTypeAudit() throws ClientProtocolException, IOException {

        System.out.println("----testProcessJALRecordMessageUnsupportedRecordTypeAudit---");
        System.out.println("DR1.018.002 - record-failure:  audit-record");
        System.out.println("DR1.018.005 - record-failure:  JAL-Id");
        System.out.println("DR1.018.006 - record-failure:  JAL-Error-Message");
        System.out.println("DR1.018.006.001 - record-failure:  JAL-Error-Message:  Error Reasons");
        System.out.println("DR1.018.006.001.008 - record-failure:  JAL-Error-Message:  Error Reasons - JAL-Unsupported-Record-Type");

        HttpPost httpPost = new HttpPost("http://localhost:" + TestResources.HTTP_PORT + HttpUtils.AUDIT_ENDPOINT);

        String publisherId = UUID.randomUUID().toString();
        String sessionId = TestResources.sendValidInitialize(RecordType.Audit, true, publisherId);

        HashMap<String, String> headers = TestResources.getJalRecordHeaders(sessionId, "jalId", "1", "0", "1", HttpUtils.HDRS_AUDIT_LEN, HttpUtils.MSG_JOURNAL, HttpUtils.ENC_XML);

        for (Map.Entry<String, String> entry : headers.entrySet())
        {
            httpPost.setHeader(entry.getKey(), entry.getValue());
        }

        HttpClient client = HttpClientBuilder.create().build();

        final HttpResponse response = client.execute(httpPost);
        final Header messageHeader = response.getFirstHeader(HttpUtils.HDRS_MESSAGE);
        final Header errorHeader = response.getFirstHeader(HttpUtils.HDRS_ERROR_MESSAGE);
        final Header digestHeader = response.getFirstHeader(HttpUtils.HDRS_DIGEST_VALUE);
        final Header jalIdHeader = response.getFirstHeader(HttpUtils.HDRS_NONCE);
        final int responseStatus = response.getStatusLine().getStatusCode();
        assertEquals(200, responseStatus);

        assertNotNull(messageHeader);
        assertEquals(HttpUtils.MSG_RECORD_FAILURE, messageHeader.getValue());
        assertNotNull(errorHeader);
        assertEquals(HttpUtils.HDRS_UNSUPPORTED_RECORD_TYPE, errorHeader.getValue());
        assertNotNull(jalIdHeader);
        assertEquals("jalId", jalIdHeader.getValue());
        assertNull(digestHeader);

        System.out.println("----testProcessJALRecordMessageUnsupportedRecordTypeAudit success----\n");
    }

    @Test
    public void testProcessJALRecordMessageUnsupportedRecordTypeJournal() throws ClientProtocolException, IOException {

        System.out.println("----testProcessJALRecordMessageUnsupportedRecordTypeJournal---");
        System.out.println("DR1.018.003 - record-failure:  journal-record");
        System.out.println("DR1.018.005 - record-failure:  JAL-Id");
        System.out.println("DR1.018.006 - record-failure:  JAL-Error-Message");
        System.out.println("DR1.018.006.001 - record-failure:  JAL-Error-Message:  Error Reasons");
        System.out.println("DR1.018.006.001.008 - record-failure:  JAL-Error-Message:  Error Reasons - JAL-Unsupported-Record-Type");

        HttpPost httpPost = new HttpPost("http://localhost:" + TestResources.HTTP_PORT + HttpUtils.JOURNAL_ENDPOINT);

        String publisherId = UUID.randomUUID().toString();
        String sessionId = TestResources.sendValidInitialize(RecordType.Journal, true, publisherId);
        HashMap<String, String> headers = TestResources.getJalRecordHeaders(sessionId, "jalId", "1", "0", "1", HttpUtils.HDRS_JOURNAL_LEN, HttpUtils.MSG_AUDIT, HttpUtils.ENC_XML);

        for (Map.Entry<String, String> entry : headers.entrySet())
        {
            httpPost.setHeader(entry.getKey(), entry.getValue());
        }

        HttpClient client = HttpClientBuilder.create().build();

        final HttpResponse response = client.execute(httpPost);
        final Header messageHeader = response.getFirstHeader(HttpUtils.HDRS_MESSAGE);
        final Header errorHeader = response.getFirstHeader(HttpUtils.HDRS_ERROR_MESSAGE);
        final Header digestHeader = response.getFirstHeader(HttpUtils.HDRS_DIGEST_VALUE);
        final Header jalIdHeader = response.getFirstHeader(HttpUtils.HDRS_NONCE);
        final int responseStatus = response.getStatusLine().getStatusCode();
        assertEquals(200, responseStatus);

        assertNotNull(messageHeader);
        assertEquals(HttpUtils.MSG_RECORD_FAILURE, messageHeader.getValue());
        assertNotNull(errorHeader);
        assertEquals(HttpUtils.HDRS_UNSUPPORTED_RECORD_TYPE, errorHeader.getValue());
        assertNotNull(jalIdHeader);
        assertEquals("jalId", jalIdHeader.getValue());
        assertNull(digestHeader);

        System.out.println("----testProcessJALRecordMessageUnsupportedRecordTypeJournal success----\n");
    }

    @Test
    public void testProcessJALRecordMessageUnsupportedRecordTypeLog() throws ClientProtocolException, IOException {
        System.out.println("----testProcessJALRecordMessageUnsupportedRecordTypeLog---");
        System.out.println("DR1.018.001 - record-failure:  log-record");
        System.out.println("DR1.018.005 - record-failure:  JAL-Id");
        System.out.println("DR1.018.006 - record-failure:  JAL-Error-Message");
        System.out.println("DR1.018.006.001 - record-failure:  JAL-Error-Message:  Error Reasons");
        System.out.println("DR1.018.006.001.008 - record-failure:  JAL-Error-Message:  Error Reasons - JAL-Unsupported-Record-Type");

        HttpPost httpPost = new HttpPost("http://localhost:" + TestResources.HTTP_PORT + HttpUtils.LOG_ENDPOINT);

        String publisherId = UUID.randomUUID().toString();
        String sessionId = TestResources.sendValidInitialize(RecordType.Log, true, publisherId);

        HashMap<String, String> headers = TestResources.getJalRecordHeaders(sessionId, "jalId", "1", "0", "1", HttpUtils.HDRS_LOG_LEN, HttpUtils.MSG_AUDIT, HttpUtils.ENC_XML);

        for (Map.Entry<String, String> entry : headers.entrySet())
        {
            httpPost.setHeader(entry.getKey(), entry.getValue());
        }

        HttpClient client = HttpClientBuilder.create().build();

        final HttpResponse response = client.execute(httpPost);
        final Header messageHeader = response.getFirstHeader(HttpUtils.HDRS_MESSAGE);
        final Header errorHeader = response.getFirstHeader(HttpUtils.HDRS_ERROR_MESSAGE);
        final Header digestHeader = response.getFirstHeader(HttpUtils.HDRS_DIGEST_VALUE);
        final Header jalIdHeader = response.getFirstHeader(HttpUtils.HDRS_NONCE);
        final int responseStatus = response.getStatusLine().getStatusCode();
        assertEquals(200, responseStatus);

        assertNotNull(messageHeader);
        assertEquals(HttpUtils.MSG_RECORD_FAILURE, messageHeader.getValue());
        assertNotNull(errorHeader);
        assertEquals(HttpUtils.HDRS_UNSUPPORTED_RECORD_TYPE, errorHeader.getValue());
        assertNotNull(jalIdHeader);
        assertEquals("jalId", jalIdHeader.getValue());
        assertNull(digestHeader);

        System.out.println("----testProcessJALRecordMessageUnsupportedRecordTypeLog success----\n");
    }

    @Test
    public void testProcessJALRecordMessageInvalidJalId() throws ClientProtocolException, IOException {

        System.out.println("----testProcessJALRecordMessageInvalidJalId---");
        System.out.println("DR1.018.001 - record-failure:  log-record");
        System.out.println("DR1.018.002 - record-failure:  audit-record");
        System.out.println("DR1.018.003 - record-failure:  journal-record");
        System.out.println("DR1.018.005 - record-failure:  JAL-Id");
        System.out.println("DR1.018.006 - record-failure:  JAL-Error-Message");
        System.out.println("DR1.018.006.001 - record-failure:  JAL-Error-Message:  Error Reasons");
        System.out.println("DR1.018.006.001.009 - record-failure:  JAL-Error-Message:  Error Reasons - JAL-Invalid-JAL-Id");

        String [] testValues = new String[] {null, ""};

        String publisherId = UUID.randomUUID().toString();
        for (RecordType recType : RecordType.values())
        {
            if (recType.equals(RecordType.Unset))
            {
                continue;
            }

            System.out.println("Testing record type of " + recType.toString());

            String sessionId = TestResources.sendValidInitialize(recType, true, publisherId);

            HttpPost httpPost = new HttpPost("http://localhost:" + TestResources.HTTP_PORT + "/" + recType.toString().toLowerCase());
            for (String testValue : testValues)
            {
                HashMap<String, String> headers = TestResources.getJalRecordHeaders(sessionId, testValue, "1", "0", "1", recType);

                for (Map.Entry<String, String> entry : headers.entrySet())
                {
                    httpPost.setHeader(entry.getKey(), entry.getValue());
                }

                HttpClient client = HttpClientBuilder.create().build();

                final HttpResponse response = client.execute(httpPost);
                final Header messageHeader = response.getFirstHeader(HttpUtils.HDRS_MESSAGE);
                final Header errorHeader = response.getFirstHeader(HttpUtils.HDRS_ERROR_MESSAGE);
                final Header digestHeader = response.getFirstHeader(HttpUtils.HDRS_DIGEST_VALUE);
                final Header jalIdHeader = response.getFirstHeader(HttpUtils.HDRS_NONCE);
                final int responseStatus = response.getStatusLine().getStatusCode();
                assertEquals(200, responseStatus);

                assertNotNull(messageHeader);
                assertEquals(HttpUtils.MSG_RECORD_FAILURE, messageHeader.getValue());
                assertNotNull(errorHeader);
                assertEquals(HttpUtils.HDRS_INVALID_JAL_ID, errorHeader.getValue());

                assertNotNull(jalIdHeader);
                assertEquals("", jalIdHeader.getValue());
                assertNull(digestHeader);
            }
        }

        System.out.println("----testProcessJALRecordMessageInvalidJalId success----\n");
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

            HashMap<String, String> headers = TestResources.getJalRecordHeaders(TestResources.SESSION_ID, "jalId", "1", "0", "1", recType);

            for (Map.Entry<String, String> entry : headers.entrySet())
            {
                httpPost.setHeader(entry.getKey(), entry.getValue());
            }

            HttpClient client = HttpClientBuilder.create().build();

            final HttpResponse response = client.execute(httpPost);
            final Header messageHeader = response.getFirstHeader(HttpUtils.HDRS_MESSAGE);
            final Header errorHeader = response.getFirstHeader(HttpUtils.HDRS_ERROR_MESSAGE);
            final Header digestHeader = response.getFirstHeader(HttpUtils.HDRS_DIGEST_VALUE);
            final Header jalIdHeader = response.getFirstHeader(HttpUtils.HDRS_NONCE);
            final int responseStatus = response.getStatusLine().getStatusCode();
            assertEquals(200, responseStatus);

            assertNotNull(messageHeader);
            assertEquals(HttpUtils.MSG_SESSION_FAILURE, messageHeader.getValue());
            assertNotNull(errorHeader);
            assertEquals(HttpUtils.HDRS_UNSUPPORTED_SESSION_ID, errorHeader.getValue());
            assertNotNull(jalIdHeader);
            assertEquals("jalId", jalIdHeader.getValue());

            final Header sessionHeader = response.getFirstHeader(HttpUtils.HDRS_SESSION_ID);
            assertNotNull(sessionHeader);
            assertEquals(TestResources.SESSION_ID, sessionHeader.getValue());
            assertNull(digestHeader);
        }
    }

    @Test
    public void testProcessJALRecordMessageEmptyPayloadRecord() throws ClientProtocolException, IOException {

        System.out.println("----testProcessJALRecordMessageEmptyPayloadRecord---");
        System.out.println("DR1.018.001 - record-failure:  log-record");
        System.out.println("DR1.018.002 - record-failure:  audit-record");
        System.out.println("DR1.018.003 - record-failure:  journal-record");
        System.out.println("DR1.018.005 - record-failure:  JAL-Id");
        System.out.println("DR1.018.006 - record-failure:  JAL-Error-Message");
        System.out.println("DR1.018.006.001 - record-failure:  JAL-Error-Message:  Error Reasons");
        System.out.println("DR1.018.006.001.010 - record-failure:  JAL-Error-Message:  Error Reasons - JAL-Record-Failure");

        String publisherId = UUID.randomUUID().toString();
        for (RecordType recType : RecordType.values())
        {
            if (recType.equals(RecordType.Unset))
            {
                continue;
            }

            System.out.println("Testing record type of " + recType.toString());

            String sessionId = TestResources.sendValidInitialize(recType, true, publisherId);

            HttpPost httpPost = new HttpPost("http://localhost:" + TestResources.HTTP_PORT + "/" + recType.toString().toLowerCase());

            HashMap<String, String> headers = TestResources.getJalRecordHeaders(sessionId, "jalId", "1", "0", "1", recType);

            for (Map.Entry<String, String> entry : headers.entrySet())
            {
                httpPost.setHeader(entry.getKey(), entry.getValue());
            }

            HttpClient client = HttpClientBuilder.create().build();

            final HttpResponse response = client.execute(httpPost);
            final Header messageHeader = response.getFirstHeader(HttpUtils.HDRS_MESSAGE);
            final Header errorHeader = response.getFirstHeader(HttpUtils.HDRS_ERROR_MESSAGE);
            final Header digestHeader = response.getFirstHeader(HttpUtils.HDRS_DIGEST_VALUE);
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

        System.out.println("----testProcessJALRecordMessageEmptyPayloadRecord success----\n");
    }

    @Test
    public void testProcessJALRecordMessageValidRecordDigestChallenge() throws ClientProtocolException, IOException {
        System.out.println("----testProcessJALRecordMessageValidRecordDigestChallenge---");
        System.out.println("DR1.019 - digest-challenge");
        System.out.println("DR1.019.001 - digest-challenge:  log-record");
        System.out.println("DR1.019.002 - digest-challenge:  audit-record");
        System.out.println("DR1.019.003 - digest-challenge:  journal-record");
        System.out.println("DR1.019.004 - digest-challenge:  JAL-Id");
        System.out.println("DR1.019.005 - digest-challenge:  JAL-Digest-Value");
        System.out.println("DR1.019.005.001 - digest-challenge:  JAL-Digest-Value - Calculated");
        System.out.println("DR1.019.005.001.001 - digest-challenge:  JAL-Digest-Value - Calculated:  Over");
        System.out.println("DR1.019.005.001.003 - digest-challenge:  JAL-Digest-Value - Calculated:  Algorithm");
        String publisherId = UUID.randomUUID().toString();
        for (RecordType recType : RecordType.values())
        {
            if (recType.equals(RecordType.Unset))
            {
                continue;
            }

            System.out.println("Testing record type of " + recType.toString());

            String sessionId = TestResources.sendValidInitialize(recType, true, publisherId);

            HttpPost httpPost = new HttpPost("http://localhost:" + TestResources.HTTP_PORT + "/" + recType.toString().toLowerCase());

            String jalId = UUID.randomUUID().toString();
            HashMap<String, String> headers = TestResources.getJalRecordHeaders(sessionId, jalId, "3083", "1125", "19", recType);

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

        System.out.println("----testProcessJALRecordMessageValidRecordDigestChallenge success----\n");
    }

    @Test
    public void testProcessJALRecordMessageWithDigestOff() throws ClientProtocolException, IOException {
        String publisherId = UUID.randomUUID().toString();
        for (RecordType recType : RecordType.values())
        {
            if (recType.equals(RecordType.Unset))
            {
                continue;
            }

            System.out.println("Testing record type of " + recType.toString());

            String sessionId = TestResources.sendValidInitialize(recType, false, publisherId);
            HttpPost httpPost = new HttpPost("http://localhost:" + TestResources.HTTP_PORT + "/" + recType.toString().toLowerCase());

            //Send 3 records of each
            for (int i = 0; i < 3; i++)
            {
                String jalId = UUID.randomUUID().toString();
                HashMap<String, String> headers = TestResources.getJalRecordHeaders(sessionId, jalId, "3083", "1125", "19", recType);

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

                //Validate digest is empty since digest is off and that sync message was sent instead
                assertNull(digestHeader);
                assertEquals(HttpUtils.MSG_SYNC, responseMessage);
                assertEquals(jalId, jalIdHeader.getValue());
            }
        }
    }

    @Test
    public void testProcessJALRecordMessageUnsupportedAuditFormat() throws ClientProtocolException, IOException {
        System.out.println("----testProcessJALRecordMessageUnsupportedAuditFormat---");
        System.out.println("DR1.018.001 - record-failure:  log-record");
        System.out.println("DR1.018.002 - record-failure:  audit-record");
        System.out.println("DR1.018.003 - record-failure:  journal-record");
        System.out.println("DR1.018.005 - record-failure:  JAL-Id");
        System.out.println("DR1.018.006 - record-failure:  JAL-Error-Message");
        System.out.println("DR1.018.006.001 - record-failure:  JAL-Error-Message:  Error Reasons");
        System.out.println("DR1.018.006.001.013 - record-failure:  JAL-Error-Message:  Error Reasons - JAL-Unsupported-Audit-Format");

        String publisherId = UUID.randomUUID().toString();
        String [] auditFormats = new String[] {"", null, "invalidformat"};
        for (RecordType recType : RecordType.values())
        {
            if (recType.equals(RecordType.Unset))
            {
                continue;
            }

            System.out.println("Testing record type of " + recType.toString());


                String sessionId = TestResources.sendValidInitialize(recType, true, publisherId);

                for (String auditFormat : auditFormats)
                {
                HttpPost httpPost = new HttpPost("http://localhost:" + TestResources.HTTP_PORT + "/" + recType.toString().toLowerCase());

                String jalId = UUID.randomUUID().toString();
                String jalLengthHeader = "JAL-" + recType.toString() + "-Length";
                String jalMessage = recType.toString().toLowerCase() +  "-record";
                HashMap<String, String> headers = TestResources.getJalRecordHeaders(sessionId, jalId, "3083", "1125", "19", jalLengthHeader, jalMessage, auditFormat);

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

                //Only audit-record message should fail if JAL-Audit-Format is unsupported
                if (!recType.equals(RecordType.Audit))
                {
                    //Check for success
                    assertEquals(null, errorMessage);
                    assertNotNull(digestHeader);

                    //Validate digest is correct for test file sent.
                    assertEquals("bbd801ce4dc24520c028025c05b44c5532b240824d2d7ce25644b73b667b6c7a", digestHeader.getValue());
                    assertEquals(HttpUtils.MSG_DIGEST_CHALLENGE, responseMessage);
                    assertEquals(jalId, jalIdHeader.getValue());
                }
                else //audit-record message, should fail with JAL-Unsupported-Audit-Format
                {
                    //Check for failure
                    assertNotNull(responseMessage);
                    assertEquals(HttpUtils.MSG_RECORD_FAILURE, responseMessage);
                    assertNotNull(errorMessage);
                    assertEquals(HttpUtils.HDRS_UNSUPPORTED_AUDIT_FORMAT, errorMessage.getValue());
                    assertNotNull(jalIdHeader);
                    assertEquals(jalId, jalIdHeader.getValue());
                    assertNull(digestHeader);
                }
            }
        }

        System.out.println("----testProcessJALRecordMessageUnsupportedAuditFormat success----\n");
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

            String jalId = UUID.randomUUID().toString();
            HashMap<String, String> headers = TestResources.getJalRecordHeaders(sessionId, jalId, "3083", "1125", "19", recType);

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
            final Header messageHeader = response.getFirstHeader(HttpUtils.HDRS_MESSAGE);
            final Header digestHeader = response.getFirstHeader(HttpUtils.HDRS_DIGEST_VALUE);
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
        System.out.println("----testProcessJALRecordMessageValidRecordInvalidSysMetadataLen---");
        System.out.println("DR1.018.001 - record-failure:  log-record");
        System.out.println("DR1.018.002 - record-failure:  audit-record");
        System.out.println("DR1.018.003 - record-failure:  journal-record");
        System.out.println("DR1.018.005 - record-failure:  JAL-Id");
        System.out.println("DR1.018.006 - record-failure:  JAL-Error-Message");
        System.out.println("DR1.018.006.001 - record-failure:  JAL-Error-Message:  Error Reasons");
        System.out.println("DR1.018.006.001.010 - record-failure:  JAL-Error-Message:  Error Reasons - JAL-Record-Failure");

        String publisherId = UUID.randomUUID().toString();
        for (RecordType recType : RecordType.values())
        {
            if (recType.equals(RecordType.Unset))
            {
                continue;
            }

            System.out.println("Testing record type of " + recType.toString());

            String sessionId = TestResources.sendValidInitialize(recType, true, publisherId);

            HttpPost httpPost = new HttpPost("http://localhost:" + TestResources.HTTP_PORT + "/" + recType.toString().toLowerCase());

            String jalId = UUID.randomUUID().toString();
            HashMap<String, String> headers = TestResources.getJalRecordHeaders(sessionId, jalId, "3082", "1125", "19", recType);

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
            final Header messageHeader = response.getFirstHeader(HttpUtils.HDRS_MESSAGE);
            final Header errorHeader = response.getFirstHeader(HttpUtils.HDRS_ERROR_MESSAGE);
            final Header digestHeader = response.getFirstHeader(HttpUtils.HDRS_DIGEST_VALUE);
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

        System.out.println("----testProcessJALRecordMessageValidRecordInvalidSysMetadataLen success----\n");
    }

    @Test
    public void testProcessJALRecordMessageValidRecordInvalidAppMetadataLen() throws ClientProtocolException, IOException {
        System.out.println("----testProcessJALRecordMessageValidRecordInvalidAppMetadataLen---");
        System.out.println("DR1.018.001 - record-failure:  log-record");
        System.out.println("DR1.018.002 - record-failure:  audit-record");
        System.out.println("DR1.018.003 - record-failure:  journal-record");
        System.out.println("DR1.018.005 - record-failure:  JAL-Id");
        System.out.println("DR1.018.006 - record-failure:  JAL-Error-Message");
        System.out.println("DR1.018.006.001 - record-failure:  JAL-Error-Message:  Error Reasons");
        System.out.println("DR1.018.006.001.010 - record-failure:  JAL-Error-Message:  Error Reasons - JAL-Record-Failure");

        String publisherId = UUID.randomUUID().toString();
        for (RecordType recType : RecordType.values())
        {
            if (recType.equals(RecordType.Unset))
            {
                continue;
            }

            System.out.println("Testing record type of " + recType.toString());

            String sessionId = TestResources.sendValidInitialize(recType, true, publisherId);

            HttpPost httpPost = new HttpPost("http://localhost:" + TestResources.HTTP_PORT + "/" + recType.toString().toLowerCase());

            String jalId = UUID.randomUUID().toString();
            HashMap<String, String> headers = TestResources.getJalRecordHeaders(sessionId, jalId, "3083", "1126", "19", recType);

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
            final Header messageHeader = response.getFirstHeader(HttpUtils.HDRS_MESSAGE);
            final Header errorHeader = response.getFirstHeader(HttpUtils.HDRS_ERROR_MESSAGE);
            final Header digestHeader = response.getFirstHeader(HttpUtils.HDRS_DIGEST_VALUE);
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

        System.out.println("----testProcessJALRecordMessageValidRecordInvalidAppMetadataLen success----\n");
    }

    @Test
    public void testProcessJALRecordMessageValidRecordInvalidPayloadLen() throws ClientProtocolException, IOException {
        System.out.println("----testProcessJALRecordMessageValidRecordInvalidPayloadLen---");
        System.out.println("DR1.018.001 - record-failure:  log-record");
        System.out.println("DR1.018.002 - record-failure:  audit-record");
        System.out.println("DR1.018.003 - record-failure:  journal-record");
        System.out.println("DR1.018.005 - record-failure:  JAL-Id");
        System.out.println("DR1.018.006 - record-failure:  JAL-Error-Message");
        System.out.println("DR1.018.006.001 - record-failure:  JAL-Error-Message:  Error Reasons");
        System.out.println("DR1.018.006.001.010 - record-failure:  JAL-Error-Message:  Error Reasons - JAL-Record-Failure");

        String publisherId = UUID.randomUUID().toString();
        for (RecordType recType : RecordType.values())
        {
            if (recType.equals(RecordType.Unset))
            {
                continue;
            }

            System.out.println("Testing record type of " + recType.toString());

            String sessionId = TestResources.sendValidInitialize(recType, true, publisherId);

            HttpPost httpPost = new HttpPost("http://localhost:" + TestResources.HTTP_PORT + "/" + recType.toString().toLowerCase());

            String jalId = UUID.randomUUID().toString();
            HashMap<String, String> headers = TestResources.getJalRecordHeaders(sessionId, jalId, "3083", "1125", "18", recType);

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
            final Header messageHeader = response.getFirstHeader(HttpUtils.HDRS_MESSAGE);
            final Header errorHeader = response.getFirstHeader(HttpUtils.HDRS_ERROR_MESSAGE);
            final Header digestHeader = response.getFirstHeader(HttpUtils.HDRS_DIGEST_VALUE);
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

        System.out.println("----testProcessJALRecordMessageValidRecordInvalidPayloadLen success----\n");
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
                sendJalRecords(recType, publisherId, "36eed06644011545939ba64028d4d4e354e9fa132d9096ce64aeefa4a90eadbc", true);
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

                sendJalRecords(recType, publisherId, "ccb26d3b28eb8f7dc5fd521aa237903c5475faed7da7dd54b87d3925fb043018", true);
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
        System.out.println("----testEmptyJALRecord---");
        System.out.println("DR1.018.001 - record-failure:  log-record");
        System.out.println("DR1.018.002 - record-failure:  audit-record");
        System.out.println("DR1.018.003 - record-failure:  journal-record");
        System.out.println("DR1.018.005 - record-failure:  JAL-Id");
        System.out.println("DR1.018.006 - record-failure:  JAL-Error-Message");
        System.out.println("DR1.018.006.001 - record-failure:  JAL-Error-Message:  Error Reasons");
        System.out.println("DR1.018.006.001.001 - record-failure:  JAL-Error-Message:  Error Reasons - JAL-Invalid-System-Metadata-Length");
        String publisherId = UUID.randomUUID().toString();

        for (RecordType recType : RecordType.values())
        {
            if (recType.equals(RecordType.Unset))
            {
                continue;
            }
            System.out.println("Testing record type of " + recType.toString());
            String sessionId = TestResources.sendValidInitialize(recType, true, publisherId);

            HttpPost httpPost = new HttpPost("http://localhost:" + TestResources.HTTP_PORT + "/" + recType.toString().toLowerCase());

            String jalId = UUID.randomUUID().toString();
            HashMap<String, String> headers = TestResources.getJalRecordHeaders(sessionId, jalId, "0","0","0", recType);

            for (Map.Entry<String, String> entry : headers.entrySet())
            {
                httpPost.setHeader(entry.getKey(), entry.getValue());
            }

            HttpClient client = HttpClientBuilder.create().build();

            final HttpResponse response = client.execute(httpPost);
            final Header messageHeader = response.getFirstHeader(HttpUtils.HDRS_MESSAGE);
            final Header errorHeader = response.getFirstHeader(HttpUtils.HDRS_ERROR_MESSAGE);
            final Header digestHeader = response.getFirstHeader(HttpUtils.HDRS_DIGEST_VALUE);
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

        System.out.println("----testEmptyJALRecord success----\n");
    }

    @Test
    public void testEmptySysMetadataInJALRecord() throws ClientProtocolException, IOException {
        System.out.println("----testEmptySysMetadataInJALRecord---");
        System.out.println("DR1.018.001 - record-failure:  log-record");
        System.out.println("DR1.018.002 - record-failure:  audit-record");
        System.out.println("DR1.018.003 - record-failure:  journal-record");
        System.out.println("DR1.018.005 - record-failure:  JAL-Id");
        System.out.println("DR1.018.006 - record-failure:  JAL-Error-Message");
        System.out.println("DR1.018.006.001 - record-failure:  JAL-Error-Message:  Error Reasons");
        System.out.println("DR1.018.006.001.001 - record-failure:  JAL-Error-Message:  Error Reasons - JAL-Invalid-System-Metadata-Length");
        String publisherId = UUID.randomUUID().toString();

        for (RecordType recType : RecordType.values())
        {
            if (recType.equals(RecordType.Unset))
            {
                continue;
            }

            System.out.println("Testing record type of " + recType.toString());
            String sessionId = TestResources.sendValidInitialize(recType, true, publisherId);

            HttpPost httpPost = new HttpPost("http://localhost:" + TestResources.HTTP_PORT + "/" + recType.toString().toLowerCase());

            String jalId = UUID.randomUUID().toString();
            HashMap<String, String> headers = TestResources.getJalRecordHeaders(sessionId, jalId, "0","1125","19", recType);

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
            final Header digestHeader = response.getFirstHeader(HttpUtils.HDRS_DIGEST_VALUE);
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

        System.out.println("----testEmptySysMetadataInJALRecord success----\n");
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

            String jalId = UUID.randomUUID().toString();
            HashMap<String, String> headers = TestResources.getJalRecordHeaders(sessionId, jalId, "3083","0","19", recType);

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
            final Header digestHeader = response.getFirstHeader(HttpUtils.HDRS_DIGEST_VALUE);
            final Header jalIdHeader = response.getFirstHeader(HttpUtils.HDRS_NONCE);
            final int responseStatus = response.getStatusLine().getStatusCode();
            assertEquals(200, responseStatus);

            assertNull(errorHeader);
            assertNotNull(digestHeader);

            //Validate digest is correct for test file sent.
            assertEquals("111fc8cbbf9a1ea8010b44a348e73ee4e962a90d200b9439f28fa62edf84175e", digestHeader.getValue());
            assertNotNull(messageHeader);
            assertEquals(HttpUtils.MSG_DIGEST_CHALLENGE, messageHeader.getValue());
            assertEquals(jalId, jalIdHeader.getValue());
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
            System.out.println("Testing record type of " + recType.toString());
            String sessionId = TestResources.sendValidInitialize(recType, true, publisherId);

            HttpPost httpPost = new HttpPost("http://localhost:" + TestResources.HTTP_PORT + "/" + recType.toString().toLowerCase());

            String jalId = UUID.randomUUID().toString();
            HashMap<String, String> headers = TestResources.getJalRecordHeaders(sessionId, jalId, "3083","1125","0", recType);

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
            final Header digestHeader = response.getFirstHeader(HttpUtils.HDRS_DIGEST_VALUE);
            final Header jalIdHeader = response.getFirstHeader(HttpUtils.HDRS_NONCE);
            final int responseStatus = response.getStatusLine().getStatusCode();
            assertEquals(200, responseStatus);

            //Only Journal and Log records can have empty payloads.
            if(recType.equals(RecordType.Journal))
            {
                assertNull(errorHeader);
                assertNotNull(messageHeader);
                assertNotNull(digestHeader);

                //Validate digest is correct for test file sent.
                assertEquals("f09a91f9d22625e91bf936493f460c7a1f8ae395c0c1fb252420caede3034bfc", digestHeader.getValue());
                assertEquals(HttpUtils.MSG_DIGEST_CHALLENGE, messageHeader.getValue());
                assertEquals(jalId, jalIdHeader.getValue());
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

                //Validate digest is correct for test file sent.
                assertEquals("f09a91f9d22625e91bf936493f460c7a1f8ae395c0c1fb252420caede3034bfc", digestHeader.getValue());
                assertEquals(HttpUtils.MSG_DIGEST_CHALLENGE, messageHeader.getValue());
                assertEquals(jalId, jalIdHeader.getValue());
            }
        }

        cleanOutputDirectoryByPublisherId(publisherId);
    }

    @Test
    public void testMissingSysMetadataInJALRecord() throws ClientProtocolException, IOException {
        System.out.println("----testMissingSysMetadataInJALRecord---");
        System.out.println("DR1.018.001 - record-failure:  log-record");
        System.out.println("DR1.018.002 - record-failure:  audit-record");
        System.out.println("DR1.018.003 - record-failure:  journal-record");
        System.out.println("DR1.018.005 - record-failure:  JAL-Id");
        System.out.println("DR1.018.006 - record-failure:  JAL-Error-Message");
        System.out.println("DR1.018.006.001 - record-failure:  JAL-Error-Message:  Error Reasons");
        System.out.println("DR1.018.006.001.001 - record-failure:  JAL-Error-Message:  Error Reasons - JAL-Invalid-System-Metadata-Length");

        String publisherId = UUID.randomUUID().toString();

        for (RecordType recType : RecordType.values())
        {
            if (recType.equals(RecordType.Unset))
            {
                continue;
            }
            System.out.println("Testing record type of " + recType.toString());
            String sessionId = TestResources.sendValidInitialize(recType, true, publisherId);

            HttpPost httpPost = new HttpPost("http://localhost:" + TestResources.HTTP_PORT + "/" + recType.toString().toLowerCase());

            String jalId = UUID.randomUUID().toString();
            HashMap<String, String> headers = TestResources.getJalRecordHeaders(sessionId, jalId, "0","1125","19", recType);

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
            final Header digestHeader = response.getFirstHeader(HttpUtils.HDRS_DIGEST_VALUE);
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

        System.out.println("----testMissingSysMetadataInJALRecord success----\n");
    }

    @Test
    public void testMissingAppMetadataInJALRecord() throws ClientProtocolException, IOException {
        System.out.println("----testMissingAppMetadataInJALRecord---");
        System.out.println("DR1.018.001 - record-failure:  log-record");
        System.out.println("DR1.018.002 - record-failure:  audit-record");
        System.out.println("DR1.018.003 - record-failure:  journal-record");
        System.out.println("DR1.018.005 - record-failure:  JAL-Id");
        System.out.println("DR1.018.006 - record-failure:  JAL-Error-Message");
        System.out.println("DR1.018.006.001 - record-failure:  JAL-Error-Message:  Error Reasons");
        System.out.println("DR1.018.006.001.010 - record-failure:  JAL-Error-Message:  Error Reasons - JAL-Record-Failure");

        String publisherId = UUID.randomUUID().toString();

        for (RecordType recType : RecordType.values())
        {
            if (recType.equals(RecordType.Unset))
            {
                continue;
            }
            System.out.println("Testing record type of " + recType.toString());
            String sessionId = TestResources.sendValidInitialize(recType, true, publisherId);

            HttpPost httpPost = new HttpPost("http://localhost:" + TestResources.HTTP_PORT + "/" + recType.toString().toLowerCase());

            String jalId = UUID.randomUUID().toString();
            HashMap<String, String> headers = TestResources.getJalRecordHeaders(sessionId, jalId, "3083","0","19", recType);

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
            final Header digestHeader = response.getFirstHeader(HttpUtils.HDRS_DIGEST_VALUE);
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
        System.out.println("----testMissingAppMetadataInJALRecord success----\n");
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

            String jalId = UUID.randomUUID().toString();
            HashMap<String, String> headers = TestResources.getJalRecordHeaders(sessionId, jalId, "3083","1125","0", recType);

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
            final Header digestHeader = response.getFirstHeader(HttpUtils.HDRS_DIGEST_VALUE);
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
    public void testProcessJALRecordMessageInvalidLogRecord() throws ClientProtocolException, IOException {
        System.out.println("----testProcessJALRecordMessageInvalidLogRecord---");
        System.out.println("DR1.018.001 - record-failure:  log-record");
        System.out.println("DR1.018.002 - record-failure:  audit-record");
        System.out.println("DR1.017.008 - Transfer Records:  journal-record");
        System.out.println("DR1.018.005 - record-failure:  JAL-Id");
        System.out.println("DR1.018.006 - record-failure:  JAL-Error-Message");
        System.out.println("DR1.018.006.001 - record-failure:  JAL-Error-Message:  Error Reasons");
        System.out.println("DR1.018.006.001.014 - record-failure:  JAL-Error-Message:  Error Reasons - JAL-Invalid-Log-Record");
        System.out.println("DR1.018.006.001.004 - record-failure:  JAL-Error-Message:  Error Reasons - JAL-Invalid-Audit-Length");

        String publisherId = UUID.randomUUID().toString();
        String [] auditFormats = new String[] {"", null, "invalidformat"};
        for (RecordType recType : RecordType.values())
        {
            if (recType.equals(RecordType.Unset))
            {
                continue;
            }

            System.out.println("Testing record type of " + recType.toString());


                String sessionId = TestResources.sendValidInitialize(recType, true, publisherId);

                for (String auditFormat : auditFormats)
                {
                HttpPost httpPost = new HttpPost("http://localhost:" + TestResources.HTTP_PORT + "/" + recType.toString().toLowerCase());

                String jalId = UUID.randomUUID().toString();
                String jalLengthHeader = "JAL-" + recType.toString() + "-Length";
                String jalMessage = recType.toString().toLowerCase() +  "-record";
                HashMap<String, String> headers = TestResources.getJalRecordHeaders(sessionId, jalId, "3083", "0", "0", jalLengthHeader, jalMessage, auditFormat);

                for (Map.Entry<String, String> entry : headers.entrySet())
                {
                    httpPost.setHeader(entry.getKey(), entry.getValue());
                }

                //Adds jal record to post
                File resourcesDirectory = new File("src/test/resources/unit_test");

                String jalRecord1Path = resourcesDirectory.getAbsolutePath() + "/empty_app_metadata_and_payload.txt";
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

                //Only Log record should fail with JAL-Invalid-Log-Record error
                if (recType.equals(RecordType.Log))
                {
                    //Check for failure
                    assertNotNull(responseMessage);
                    assertEquals(HttpUtils.MSG_RECORD_FAILURE, responseMessage);
                    assertNotNull(errorMessage);
                    assertEquals(HttpUtils.HDRS_INVALID_LOG_RECORD, errorMessage.getValue());
                    assertNotNull(jalIdHeader);
                    assertEquals(jalId, jalIdHeader.getValue());
                    assertNull(digestHeader);
                }
                else if (recType.equals(RecordType.Journal))  //Journal should pass
                {
                    //Check for success
                    assertEquals(null, errorMessage);
                    assertNotNull(digestHeader);

                    //Validate digest is correct for test file sent.
                    assertEquals("951c2b91e958c6df4eb4d154902d980b16b906c26ba199ada8519081452a88e7", digestHeader.getValue());
                    assertEquals(HttpUtils.MSG_DIGEST_CHALLENGE, responseMessage);
                    assertEquals(jalId, jalIdHeader.getValue());
                }
                else //audit-record message, should fail with JAL-Invalid-Audit-Length
                {
                    //Check for failure
                    assertNotNull(responseMessage);
                    assertEquals(HttpUtils.MSG_RECORD_FAILURE, responseMessage);
                    assertNotNull(errorMessage);
                    assertEquals(HttpUtils.HDRS_INVALID_AUDIT_LEN, errorMessage.getValue());
                    assertNotNull(jalIdHeader);
                    assertEquals(jalId, jalIdHeader.getValue());
                    assertNull(digestHeader);
                }
            }
        }

        System.out.println("----testProcessJALRecordMessageInvalidLogRecord success----\n");
    }

    @Test
    public void test100EachRecTypeSingleThread() throws ClientProtocolException, IOException {

        String publisherId = UUID.randomUUID().toString();

        for (RecordType recType : RecordType.values())
        {
            try
            {
                if (recType.equals(RecordType.Unset))
                {
                    continue;
                }
                boolean result = generateRecords(recType, 100, SYS_METADATA_GOOD, APP_METADATA_GOOD, PAYLOAD_GOOD_SMALL );
                assertTrue(result);

                sendJalRecords(recType, publisherId, "8dc8c3f7917b992cc4aafe5e70bea854ec6ee82034ada9ab3591f2f3a6510e1b", true);
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
    public void test100EachRecTypeConcurrentPerRecType() throws ClientProtocolException, IOException {

        String publisherId = UUID.randomUUID().toString();
        ArrayList<Thread> arrThreads = new ArrayList<Thread>();

        for (RecordType recType : RecordType.values())
        {
            if (recType.equals(RecordType.Unset))
            {
                continue;
            }

            boolean result = generateRecords(recType, 100, SYS_METADATA_GOOD, APP_METADATA_GOOD, PAYLOAD_GOOD_SMALL);
            assertTrue(result);
        }

        final ArrayList<AssertionError> assertErr = new ArrayList<AssertionError>();
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
                        sendJalRecords(currRecType, currPublisherId, "8dc8c3f7917b992cc4aafe5e70bea854ec6ee82034ada9ab3591f2f3a6510e1b", true);
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                        //flag error
                        assertTrue(false);
                    }
                    catch (AssertionError ae)
                    {
                        if (assertErr.size() == 0)
                        {
                            assertErr.add(ae);
                        }
                        throw(ae);
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

        //Ensures no assert errors occurred in any threads
        assertEquals(0, assertErr.size());

        //If you want to see the files in the output directory on the subscriber side, comment this line out so files remain after unit test execution
        cleanOutputDirectoryByPublisherId(publisherId);
    }


    //This test sends each record in a separate thread to test full concurrent record posts.  1000 records sent, 1000 threads all concurrent.
    @Test
    public void test100EachRecTypeConcurrentRecordPost() throws ClientProtocolException, IOException {

        String publisherId = UUID.randomUUID().toString();

        for (RecordType recType : RecordType.values())
        {
            if (recType.equals(RecordType.Unset))
            {
                continue;
            }

            int recordCount = 100;  //Increase this number to increase the total number of record and threads created to stress test concurrent posting.  This creates 1 thread per record being submitted
            boolean result = generateRecords(recType, recordCount, SYS_METADATA_GOOD, APP_METADATA_GOOD, PAYLOAD_GOOD_SMALL);
            assertTrue(result);
        }

        for (RecordType recType : RecordType.values())
        {
            if (recType.equals(RecordType.Unset))
            {
                continue;
            }

            sendJalRecordsConcurrent(recType, publisherId, "8dc8c3f7917b992cc4aafe5e70bea854ec6ee82034ada9ab3591f2f3a6510e1b", true);

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


    @Test
    public void testStressTestLargeFilesConcurrent() throws ClientProtocolException, IOException {

        //This test allows for sending a specific number of 100 mb records to the specified channels
        //Only run stress test if number of records is set via config setting on command line.
        //During a normal build -DnumRecords parameter is not set, and this test is not executed.

        /*To manually execute this test run the following at the command line in the <jalop git root>/jjnl/jnl_parent directory:
             /usr/local/apache-maven-3.6.0/bin/mvn test -Dtest=com.tresys.jalop.utils.http.JalRecordTest#testStressTestLargeFilesConcurrent -DfailIfNoTests=false -DnumRecords=5 -DrecordTypes=audit,log,journal -DperformDigest=yes -DlogLevel=off -DmaxThreadCount=25
         *
         * Set -DnumRecords to the number of records you want to send. If empty, then default value is set to 0 and this test is not executed
         * Set -DrecordTypes to a comma separate list of the record types you want to send these number of records each to.
         * Set -DperformDigest to "yes" to enable digesting, "no" for no digesting.  Default is "yes" if not specified
         * Set -DlogLevel to one of the following values: (off, info, warn, debug, error), default is "info"
         * Set -DmaxThreadCount to specify the maximum number of threads in the threadpool processing the jal record posts, default is 10
         *
         * Example of payload size calculation: -DnumRecords=5 and -DrecordTypes=audit,journal = 5 x 2 record types x 100mb = 1 gig of payload data sent.
         */

        if (numRecords <= 0 )
        {
            return;
        }

        String digestValue = "on";
        if (!performDigest)
        {
            digestValue = "off";
        }

        long totalsize = numRecords * 100 * recordTypes.size();
        long totalRecords = numRecords * recordTypes.size();

        String recordTypeStr = String.join(",", TestResources.getRecordTypeNames(recordTypes));

        System.out.println("\n**********************************************************");
        System.out.println("Performing testStressTestLargeFilesConcurrent unit test with following parameters using 100MB payload per record:");
        System.out.println("Digesting is " + digestValue);
        System.out.println("maxThreadCount is " + maxThreadCount);
        System.out.println("Sending " + Long.toString(numRecords) + " records each on " + recordTypeStr + " channel(s) for a total of " + totalRecords + " records sent and total payload transfer size of " + totalsize  + " MB");
        System.out.println("**********************************************************\n");

        String publisherId = UUID.randomUUID().toString();

        for (RecordType recType : recordTypes)
        {
            if (recType.equals(RecordType.Unset))
            {
                continue;
            }

            //To stress test more, increase the record count param, example if set to 10, then 10 x 100MB = 1 GB per channel.
            boolean result = generateRecords(recType, numRecords, SYS_METADATA_GOOD, APP_METADATA_GOOD, PAYLOAD_100MB);
            assertTrue(result);
        }

        final long startTime = System.currentTimeMillis();
        ArrayList<JalRecordTask> jalRecordTaskList = new ArrayList<JalRecordTask>();

        //Sets up thread pool
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(maxThreadCount);
        for (RecordType recType : RecordType.values())
        {

            if (recType.equals(RecordType.Unset))
            {
                continue;
            }

            String expectedDigest = "";
            if (performDigest)
            {
                expectedDigest = "4b5880a00d4adbe43cddc06c500db6b85951debf7437d070c9044a399cf16bc9";
            }
            sendJalRecordsWithThreadPool(recType, publisherId, expectedDigest, true, executor, jalRecordTaskList);
        }

        //Shuts down threadpool and waits for all threads to finish
        executor.shutdown();
        try {
            if (!executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException ex) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        final long elapsedTimeMillis = System.currentTimeMillis() - startTime;
        String time = TestResources.formatElapsedTime(elapsedTimeMillis);

        System.out.println("\n**********************************************************");
        System.out.println(totalRecords + " total records with " + totalsize + " MB sent in " + time);
        System.out.println("**********************************************************\n");

        //Clean all input dirs
        for (RecordType recType : RecordType.values())
        {
            if (recType.equals(RecordType.Unset))
            {
                continue;
            }
            TestResources.cleanInputDirectory(recType, inputDirStr);
        }

        //If you want to see the files in the output directory on the subscriber side, comment this line out so files remain after unit test execution
        TestResources.cleanOutputDirectory(outputDirStr);

        //Verifies that no errors occurred during each job post in separate threads
        for (JalRecordTask jalRecordTask : jalRecordTaskList)
        {
            assertFalse(jalRecordTask.getHasError());
        }
    }

    @Test
    public void testStressTestSmallFilesConcurrent() throws ClientProtocolException, IOException {

        //This test allows for sending a specific number of 1kb records to the specified channels
        //Only run stress test if number of records is set via config setting on command line.
        //During a normal build -DnumRecords parameter is not set, and this test is not executed.

        /*To manually execute this test run the following at the command line in the <jalop git root>/jjnl/jnl_parent directory:
             /usr/local/apache-maven-3.6.0/bin/mvn test -Dtest=com.tresys.jalop.utils.http.JalRecordTest#testStressTestSmallFilesConcurrent -DfailIfNoTests=false -DnumRecords=500 -DrecordTypes=audit,log,journal -DperformDigest=yes -DlogLevel=off
         *
         * Set -DnumRecords to the number of records you want to send. If empty, then default value is set to 0 and this test is not executed
         * Set -DrecordTypes to a comma separate list of the record types you want to send these number of records each to, if left empty defaults to audit,journal,log
         * Set -DperformDigest to "yes" to enable digesting, "no" for no digesting.  Default is "yes" if not specified
         * Set -DlogLevel to one of the following values: (off, info, warn, debug, error), default is info
         * Set -DmaxThreadCount to specify the maximum number of threads in the threadpool processing the jal record posts, default is 10
         *
         * Example of payload size calculation: -DnumRecords=500 and -DrecordTypes=audit,journal = 500 x 2 record types x 1kb = 1 MB of payload data sent.
         */

        if (numRecords <= 0 )
        {
            return;
        }

        long totalsize = numRecords * 1 * recordTypes.size();
        long totalRecords = numRecords * recordTypes.size();

        String recordTypeStr = String.join(",", TestResources.getRecordTypeNames(recordTypes));

        String digestValue = "on";
        if (!performDigest)
        {
            digestValue = "off";
        }

        System.out.println("\n**********************************************************");
        System.out.println("Performing testStressTestSmallFilesConcurrent unit test with following parameters using 1KB payload per record:");
        System.out.println("Digesting is " + digestValue);
        System.out.println("maxThreadCount is " + maxThreadCount);
        System.out.println("Sending " + Long.toString(numRecords) + " records each on " + recordTypeStr + " channel(s) for a total of " + totalRecords + " records sent and total payload transfer size of " + totalsize  + " KB");
        System.out.println("**********************************************************\n");

        String publisherId = UUID.randomUUID().toString();
        ArrayList<Thread> arrThreads = new ArrayList<Thread>();

        for (RecordType recType : recordTypes)
        {
            if (recType.equals(RecordType.Unset))
            {
                continue;
            }

            //To stress test more, increase the record count param below, currently set to 10, so 10 x 1kb = 1 MB per channel.
            boolean result = generateRecords(recType, numRecords, SYS_METADATA_GOOD, APP_METADATA_GOOD, PAYLOAD_GOOD_SMALL);
            assertTrue(result);
        }

        final long startTime = System.currentTimeMillis();
        ArrayList<JalRecordTask> jalRecordTaskList = new ArrayList<JalRecordTask>();

        //Sets up thread pool
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(maxThreadCount);
        for (RecordType recType : RecordType.values())
        {
            if (recType.equals(RecordType.Unset))
            {
                continue;
            }

            String expectedDigest = "";
            if (performDigest)
            {
                expectedDigest = "8dc8c3f7917b992cc4aafe5e70bea854ec6ee82034ada9ab3591f2f3a6510e1b";
            }
            sendJalRecordsWithThreadPool(recType, publisherId, expectedDigest, true, executor, jalRecordTaskList);
        }

        //Shuts down threadpool and waits for all threads to finish
        executor.shutdown();
        try {
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException ex) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        final long elapsedTimeMillis = System.currentTimeMillis() - startTime;
        String time = TestResources.formatElapsedTime(elapsedTimeMillis);

        System.out.println("\n**********************************************************");
        System.out.println(totalRecords + " total records with " + totalsize + " KB sent in " + time);
        System.out.println("**********************************************************\n");

        //Clean all input dirs
        for (RecordType recType : RecordType.values())
        {
            if (recType.equals(RecordType.Unset))
            {
                continue;
            }
            TestResources.cleanInputDirectory(recType, inputDirStr);
        }

        //If you want to see the files in the output directory on the subscriber side, comment this line out so files remain after unit test execution
        TestResources.cleanOutputDirectory(outputDirStr);

        //Verifies that no errors occurred during each job post in separate threads
        for (JalRecordTask jalRecordTask : jalRecordTaskList)
        {
            assertFalse(jalRecordTask.getHasError());
        }
    }
}
