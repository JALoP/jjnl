package com.tresys.jalop.jnl.impl.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
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
    private static int HTTP_PORT = 8080;
    private static String SESSION_ID = "fe8a54d7-dd7c-4c50-a7e7-f948a140c556";

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
    private static final String PAYLOAD_NO_INPUT = "no_input";
    private static final String PAYLOAD_GOOD_SMALL = "good_audit_input.xml";


    //sys metadata test files
    private static final String SYS_METADATA_GOOD = "system-metadata.xml";
    private static final String SYS_METADATA_MALFORMED = "system-metadata-malformed.xml";
    private static final String SYS_METADATA_CDATA = "system-metadata-cdata.xml";

    //app metadata test files
    private static final String APP_METADATA_GOOD = "good_app_meta_input.xml";

    private static String jjnlDirPath = "";
    private static String jalopTestDataDir = "";
    private static String inputDirStr = "";
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

        //gets jjnl dir path
        resourcesDirectory = new File("src/test/resources");
        jjnlDirPath = resourcesDirectory.getAbsolutePath() + "/../../../..";
        inputDirStr = jjnlDirPath + "/input";
        jalopTestDataDir = jjnlDirPath + "/../jalop/test-input";
        outputDirStr = jjnlDirPath + "/jnl_lib/output";

        //Clears out input and output directories
        cleanAllDirectories();

        server = MessageProcessorTest.getWebServer();
        server.start();
    }

    private static void cleanAllDirectories() throws IOException
    {
        for (RecordType recType : RecordType.values())
        {

            if (recType.equals(RecordType.Unset))
            {
                continue;
            }
            cleanInputDirectory(recType);
        }

        //Clears out input and output directories
        cleanOutputDirectory("");
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
        cleanAllDirectories();
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

    private static void cleanOutputDirectory(String publisherId) throws IOException
    {
        if (outputDirStr != null && outputDirStr.contains("output"))
        {
            File outputDir = new File(outputDirStr + "/" + publisherId);
            FileUtils.deleteDirectory(outputDir);
        }
    }

    private boolean generateRecords(RecordType recType, long numRecords, String sysFilename, String appFilename, String payloadFilename)
    {
        cleanInputDirectory(recType);
        try
        {
            String[] cmd = {
                    "python",
                    jjnlDirPath + "/generate_records.py",
                    recType.toString().toLowerCase(), inputDirStr, Long.toString(numRecords), jalopTestDataDir + "/" + sysFilename, jalopTestDataDir + "/" + appFilename,
                    jalopTestDataDir + "/" + payloadFilename
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

        return true;
    }

    private static boolean cleanInputDirectory(RecordType recType)
    {
        File inputDir = new File(inputDirStr + "/" + recType.toString().toLowerCase());

        try
        {
            if (inputDirStr != null && inputDirStr.contains("input"))
            {
                FileUtils.deleteDirectory(inputDir);
            }
        }
        catch (IOException ioe)
        {
            ioe.printStackTrace();
            return false;
        }

        return true;
    }

    private void sendJalRecords(RecordType recType, String publisherId, String expectedDigest, boolean performDigest) throws ClientProtocolException, IOException
    {
        String sessionId = MessageProcessorTest.sendValidInitialize(recType, performDigest, publisherId);
        File inputDir = new File(inputDirStr + "/" + recType.toString().toLowerCase());
        File[] directoryListing = inputDir.listFiles();
        if (directoryListing != null) {
            for (File currDir : directoryListing) {
                if (!currDir.isDirectory())
                {
                    continue;
                }

                HttpPost httpPost = new HttpPost("http://localhost:" + HTTP_PORT + "/" + recType.toString().toLowerCase());

                String payLoadLengthHeader = "JAL-" + recType.toString() + "-Length";
                String jalMessage = recType.toString().toLowerCase() +  "-record";

                JalRecordLength recordLen = convertToJalRecord(currDir.getAbsolutePath());
                assertEquals(true, recordLen != null);

                HashMap<String, String> headers = MessageProcessorTest.getJalRecordHeaders(sessionId, UUID.randomUUID().toString(), Long.toString(recordLen.sysMetadataLen), Long.toString(recordLen.appMetadataLen), Long.toString(recordLen.payloadLen), payLoadLengthHeader, jalMessage);

                for (Map.Entry<String, String> entry : headers.entrySet())
                {
                    httpPost.setHeader(entry.getKey(), entry.getValue());
                }

                String jalRecordPath = currDir.getAbsolutePath() + "/" + JAL_RECORD_FILE;
                HttpEntity entity = EntityBuilder.create().setFile(new File(jalRecordPath)).build();

                httpPost.setEntity(entity);

                HttpClient client = HttpClientBuilder.create().build();

                final HttpResponse response = client.execute(httpPost);
                final String responseMessage = response.getFirstHeader(HttpUtils.HDRS_DIGEST).getValue();
                final Header errorMessage = response.getFirstHeader(HttpUtils.HDRS_ERROR_MESSAGE);
                final int responseStatus = response.getStatusLine().getStatusCode();
                assertEquals(200, responseStatus);
                assertEquals(null, errorMessage);

                //Validate digest is correct for test file sent.
                assertEquals(expectedDigest, responseMessage);
            }
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
                cleanOutputDirectory(publisherId);

                try
                {
                    //Need sleep to clean up input dir correctly
                    Thread.sleep(1000);
                }
                catch(Exception e)
                {
                }
                cleanInputDirectory(recType);
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
                cleanOutputDirectory(publisherId);

                try
                {
                    //Need sleep to clean up input dir correctly
                    Thread.sleep(500);
                }
                catch(Exception e)
                {
                }
                cleanInputDirectory(recType);
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
                cleanOutputDirectory(publisherId);

                try
                {
                    //Need sleep to clean up input dir correctly
                    Thread.sleep(1000);
                }
                catch(Exception e)
                {
                }
                cleanInputDirectory(recType);
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
            String sessionId = MessageProcessorTest.sendValidInitialize(recType, true, publisherId);

            HttpPost httpPost = new HttpPost("http://localhost:" + HTTP_PORT + "/" + recType.toString().toLowerCase());

            String payLoadLengthHeader = "JAL-" + recType.toString() + "-Length";
            String jalMessage = recType.toString().toLowerCase() +  "-record";


            HashMap<String, String> headers = MessageProcessorTest.getJalRecordHeaders(sessionId, UUID.randomUUID().toString(), "0","0","0", payLoadLengthHeader, jalMessage);

            for (Map.Entry<String, String> entry : headers.entrySet())
            {
                httpPost.setHeader(entry.getKey(), entry.getValue());
            }

            HttpClient client = HttpClientBuilder.create().build();

            final HttpResponse response = client.execute(httpPost);
            final Header responseMessage = response.getFirstHeader(HttpUtils.HDRS_DIGEST);
            final String errorMessage = response.getFirstHeader(HttpUtils.HDRS_ERROR_MESSAGE).getValue();
            final int responseStatus = response.getStatusLine().getStatusCode();
            assertEquals(200, responseStatus);
            assertEquals(HttpUtils.HDRS_INVALID_SYS_META_LEN, errorMessage);

            //Validate digest is correct for test file sent.
            assertEquals(null, responseMessage);
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
            String sessionId = MessageProcessorTest.sendValidInitialize(recType, true, publisherId);

            HttpPost httpPost = new HttpPost("http://localhost:" + HTTP_PORT + "/" + recType.toString().toLowerCase());

            String payLoadLengthHeader = "JAL-" + recType.toString() + "-Length";
            String jalMessage = recType.toString().toLowerCase() +  "-record";


            HashMap<String, String> headers = MessageProcessorTest.getJalRecordHeaders(sessionId, UUID.randomUUID().toString(), "0","1125","19", payLoadLengthHeader, jalMessage);

            for (Map.Entry<String, String> entry : headers.entrySet())
            {
                httpPost.setHeader(entry.getKey(), entry.getValue());
            }

            String jalRecordPath = resourcesDirectory.getAbsolutePath() + "/empty_sys_metadata.txt";
            HttpEntity entity = EntityBuilder.create().setFile(new File(jalRecordPath)).build();

            httpPost.setEntity(entity);

            HttpClient client = HttpClientBuilder.create().build();

            final HttpResponse response = client.execute(httpPost);
            final Header responseMessage = response.getFirstHeader(HttpUtils.HDRS_DIGEST);
            final String errorMessage = response.getFirstHeader(HttpUtils.HDRS_ERROR_MESSAGE).getValue();
            final int responseStatus = response.getStatusLine().getStatusCode();
            assertEquals(200, responseStatus);
            assertEquals(HttpUtils.HDRS_INVALID_SYS_META_LEN, errorMessage);

            //Validate digest is correct for test file sent.
            assertEquals(null, responseMessage);
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
            String sessionId = MessageProcessorTest.sendValidInitialize(recType, true, publisherId);

            HttpPost httpPost = new HttpPost("http://localhost:" + HTTP_PORT + "/" + recType.toString().toLowerCase());

            String payLoadLengthHeader = "JAL-" + recType.toString() + "-Length";
            String jalMessage = recType.toString().toLowerCase() +  "-record";


            HashMap<String, String> headers = MessageProcessorTest.getJalRecordHeaders(sessionId, UUID.randomUUID().toString(), "3083","0","19", payLoadLengthHeader, jalMessage);

            for (Map.Entry<String, String> entry : headers.entrySet())
            {
                httpPost.setHeader(entry.getKey(), entry.getValue());
            }

            String jalRecordPath = resourcesDirectory.getAbsolutePath() + "/empty_app_metadata.txt";
            HttpEntity entity = EntityBuilder.create().setFile(new File(jalRecordPath)).build();

            httpPost.setEntity(entity);

            HttpClient client = HttpClientBuilder.create().build();

            final HttpResponse response = client.execute(httpPost);
            final String responseMessage = response.getFirstHeader(HttpUtils.HDRS_DIGEST).getValue();
            final Header errorMessage = response.getFirstHeader(HttpUtils.HDRS_ERROR_MESSAGE);
            final int responseStatus = response.getStatusLine().getStatusCode();
            assertEquals(200, responseStatus);
            assertEquals(null, errorMessage);

            //Validate digest is correct for test file sent.
            assertEquals("111fc8cbbf9a1ea8010b44a348e73ee4e962a90d200b9439f28fa62edf84175e", responseMessage);
        }

        cleanOutputDirectory(publisherId);
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
            String sessionId = MessageProcessorTest.sendValidInitialize(recType, true, publisherId);

            HttpPost httpPost = new HttpPost("http://localhost:" + HTTP_PORT + "/" + recType.toString().toLowerCase());

            String payLoadLengthHeader = "JAL-" + recType.toString() + "-Length";
            String jalMessage = recType.toString().toLowerCase() +  "-record";


            HashMap<String, String> headers = MessageProcessorTest.getJalRecordHeaders(sessionId, UUID.randomUUID().toString(), "3083","1125","0", payLoadLengthHeader, jalMessage);

            for (Map.Entry<String, String> entry : headers.entrySet())
            {
                httpPost.setHeader(entry.getKey(), entry.getValue());
            }

            String jalRecordPath = resourcesDirectory.getAbsolutePath() + "/empty_payload.txt";
            HttpEntity entity = EntityBuilder.create().setFile(new File(jalRecordPath)).build();

            httpPost.setEntity(entity);

            HttpClient client = HttpClientBuilder.create().build();

            final HttpResponse response = client.execute(httpPost);
            final Header responseMessage = response.getFirstHeader(HttpUtils.HDRS_DIGEST);
            final Header errorMessage = response.getFirstHeader(HttpUtils.HDRS_ERROR_MESSAGE);
            final int responseStatus = response.getStatusLine().getStatusCode();
            assertEquals(200, responseStatus);

            //Only Journal and Log records can have empty payloads.
            if(recType.equals(RecordType.Journal))
            {
                assertEquals(null, errorMessage);

                //Validate digest is correct for test file sent.
                assertEquals("f09a91f9d22625e91bf936493f460c7a1f8ae395c0c1fb252420caede3034bfc", responseMessage.getValue());
            }
            else if (recType.equals(RecordType.Audit))
            {
                assertEquals(HttpUtils.HDRS_INVALID_AUDIT_LEN, errorMessage.getValue());
                assertEquals(null, responseMessage);
            }
            else if (recType.equals(RecordType.Log))
            {
                assertEquals(null, errorMessage);

                //Validate digest is correct for test file sent.
                assertEquals("f09a91f9d22625e91bf936493f460c7a1f8ae395c0c1fb252420caede3034bfc", responseMessage.getValue());
            }
        }

        cleanOutputDirectory(publisherId);
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
            String sessionId = MessageProcessorTest.sendValidInitialize(recType, true, publisherId);

            HttpPost httpPost = new HttpPost("http://localhost:" + HTTP_PORT + "/" + recType.toString().toLowerCase());

            String payLoadLengthHeader = "JAL-" + recType.toString() + "-Length";
            String jalMessage = recType.toString().toLowerCase() +  "-record";


            HashMap<String, String> headers = MessageProcessorTest.getJalRecordHeaders(sessionId, UUID.randomUUID().toString(), "0","1125","19", payLoadLengthHeader, jalMessage);

            for (Map.Entry<String, String> entry : headers.entrySet())
            {
                httpPost.setHeader(entry.getKey(), entry.getValue());
            }

            String jalRecordPath = resourcesDirectory.getAbsolutePath() + "/missing_sys_metadata.txt";
            HttpEntity entity = EntityBuilder.create().setFile(new File(jalRecordPath)).build();

            httpPost.setEntity(entity);

            HttpClient client = HttpClientBuilder.create().build();

            final HttpResponse response = client.execute(httpPost);
            final Header responseMessage = response.getFirstHeader(HttpUtils.HDRS_DIGEST);
            final String errorMessage = response.getFirstHeader(HttpUtils.HDRS_ERROR_MESSAGE).getValue();
            final int responseStatus = response.getStatusLine().getStatusCode();
            assertEquals(200, responseStatus);
            assertEquals(HttpUtils.HDRS_INVALID_SYS_META_LEN, errorMessage);

            //Validate digest is correct for test file sent.
            assertEquals(null, responseMessage);
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
            String sessionId = MessageProcessorTest.sendValidInitialize(recType, true, publisherId);

            HttpPost httpPost = new HttpPost("http://localhost:" + HTTP_PORT + "/" + recType.toString().toLowerCase());

            String payLoadLengthHeader = "JAL-" + recType.toString() + "-Length";
            String jalMessage = recType.toString().toLowerCase() +  "-record";


            HashMap<String, String> headers = MessageProcessorTest.getJalRecordHeaders(sessionId, UUID.randomUUID().toString(), "3083","0","19", payLoadLengthHeader, jalMessage);

            for (Map.Entry<String, String> entry : headers.entrySet())
            {
                httpPost.setHeader(entry.getKey(), entry.getValue());
            }

            String jalRecordPath = resourcesDirectory.getAbsolutePath() + "/missing_app_metadata.txt";
            HttpEntity entity = EntityBuilder.create().setFile(new File(jalRecordPath)).build();

            httpPost.setEntity(entity);

            HttpClient client = HttpClientBuilder.create().build();

            final HttpResponse response = client.execute(httpPost);
            final Header responseMessage = response.getFirstHeader(HttpUtils.HDRS_DIGEST);
            final String errorMessage = response.getFirstHeader(HttpUtils.HDRS_ERROR_MESSAGE).getValue();
            final int responseStatus = response.getStatusLine().getStatusCode();
            assertEquals(200, responseStatus);
            assertEquals(HttpUtils.HDRS_RECORD_FAILED, errorMessage);

            //Validate digest is correct for test file sent.
            assertEquals(null, responseMessage);
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
            String sessionId = MessageProcessorTest.sendValidInitialize(recType, true, publisherId);

            HttpPost httpPost = new HttpPost("http://localhost:" + HTTP_PORT + "/" + recType.toString().toLowerCase());

            String payLoadLengthHeader = "JAL-" + recType.toString() + "-Length";
            String jalMessage = recType.toString().toLowerCase() +  "-record";


            HashMap<String, String> headers = MessageProcessorTest.getJalRecordHeaders(sessionId, UUID.randomUUID().toString(), "3083","1125","0", payLoadLengthHeader, jalMessage);

            for (Map.Entry<String, String> entry : headers.entrySet())
            {
                httpPost.setHeader(entry.getKey(), entry.getValue());
            }

            String jalRecordPath = resourcesDirectory.getAbsolutePath() + "/missing_payload.txt";
            HttpEntity entity = EntityBuilder.create().setFile(new File(jalRecordPath)).build();

            httpPost.setEntity(entity);

            HttpClient client = HttpClientBuilder.create().build();

            final HttpResponse response = client.execute(httpPost);
            final Header responseMessage = response.getFirstHeader(HttpUtils.HDRS_DIGEST);
            final Header errorMessage = response.getFirstHeader(HttpUtils.HDRS_ERROR_MESSAGE);
            final int responseStatus = response.getStatusLine().getStatusCode();
            assertEquals(200, responseStatus);

            //Only Journal and Log records can have empty payloads.
            if(recType.equals(RecordType.Journal))
            {
                assertEquals(HttpUtils.HDRS_RECORD_FAILED, errorMessage.getValue());
                assertEquals(null, responseMessage);
            }
            else if (recType.equals(RecordType.Audit))
            {
                assertEquals(HttpUtils.HDRS_INVALID_AUDIT_LEN, errorMessage.getValue());
                assertEquals(null, responseMessage);
            }
            else if (recType.equals(RecordType.Log))
            {
                assertEquals(HttpUtils.HDRS_RECORD_FAILED, errorMessage.getValue());
                assertEquals(null, responseMessage);
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
                cleanOutputDirectory(publisherId);

                try
                {
                    //Need sleep to clean up input dir correctly
                    Thread.sleep(1000);
                }
                catch(Exception e)
                {
                }
                cleanInputDirectory(recType);
            }
        }
    }

    @Test
    public void test1000EachRecTypeConcurrent() throws ClientProtocolException, IOException {

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
        cleanOutputDirectory(publisherId);
    }

    //NOTE comment in this test to stress test the system, this test takes over 10 minutes to run as it sends 3 GB of JAL records over audit,log,journal channels
    /*@Test
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
        cleanOutputDirectory(publisherId);
    } */
}
