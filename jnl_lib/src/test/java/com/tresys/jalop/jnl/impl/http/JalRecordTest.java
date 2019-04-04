package com.tresys.jalop.jnl.impl.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
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

    private static final String APP_METADATA_FILE = "app_metadata.xml";
    private static final String PAYLOAD_FILE = "payload";
    private static final String SYS_METADATA_FILE = "sys_metadata.xml";
    private static final String JAL_RECORD_FILE = "jal_record.bin";
    private static final String BREAK_STR = "BREAK";

    private static String jjnlDirPath = "";
    private static String jalopTestDataDir = "";
    private static String inputDirStr = "";
    private static String outputDirStr = "";

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
        File resourcesDirectory = new File("src/test/resources");
        jjnlDirPath = resourcesDirectory.getAbsolutePath() + "/../../../..";
        inputDirStr = jjnlDirPath + "/input";
        jalopTestDataDir = jjnlDirPath + "/../jalop/test-input";
        outputDirStr = jjnlDirPath + "/jnl_lib/output";

        server = MessageProcessorTest.getWebServer();
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
            File sysMetadataFile = new File(recordDir + "/" + SYS_METADATA_FILE);
            File appMetadataFile = new File(recordDir + "/" + APP_METADATA_FILE);
            File payloadFile = new File (recordDir + "/" + PAYLOAD_FILE);

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

    private void cleanOutputDirectory(String publisherId) throws IOException
    {
        if (outputDirStr != null && outputDirStr.contains("output"))
        {
            File outputDir = new File(outputDirStr + "/" + publisherId);
            FileUtils.deleteDirectory(outputDir);
        }
    }

    private boolean generateRecords(RecordType recType, long numRecords)
    {
        File inputDir = new File(inputDirStr);

        try
        {
            if (inputDirStr != null && inputDirStr.contains("input"))
            {
                FileUtils.deleteDirectory(inputDir);
                inputDir.mkdir();

            }
            String[] cmd = {
                    "python",
                    jjnlDirPath + "/generate_records.py",
                    recType.toString().toLowerCase(), inputDirStr, Long.toString(numRecords), jalopTestDataDir + "/system-metadata.xml", jalopTestDataDir + "/good_app_meta_input.xml",
                    jalopTestDataDir + "/good_audit_input.xml"
            };
            Runtime.getRuntime().exec(cmd);
        }
        catch (IOException ioe)
        {
            ioe.printStackTrace();
            return false;
        }

        return true;
    }

    @Test
    public void testProcessJALRecordMessage1000recStressTest() throws ClientProtocolException, IOException {

        String publisherId = UUID.randomUUID().toString();
        try
        {
            RecordType recType = RecordType.Journal;
            boolean result = generateRecords(recType, 1000);
            assertTrue(result);

            String sessionId = MessageProcessorTest.sendValidInitialize(recType, true, publisherId);
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
                    assertEquals("0b601ac35ea3cd61984ad46f4b9726f4380fdf07dc69540f6ef8b594b5a013c0", responseMessage);
                }
            }
        }
        finally
        {
            cleanOutputDirectory(publisherId);
        }
    }
}
