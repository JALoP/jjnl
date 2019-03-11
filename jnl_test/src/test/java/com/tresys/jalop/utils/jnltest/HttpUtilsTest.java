package com.tresys.jalop.utils.jnltest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.crypto.dsig.DigestMethod;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.tresys.jalop.jnl.exceptions.MissingMimeHeaderException;
import com.tresys.jalop.jnl.exceptions.UnexpectedMimeValueException;

/**
 * Tests for common utility class.
 */
public class HttpUtilsTest {

    private static Server server;
    private static int HTTP_PORT = 8080;

    private static String JOURNAL_ENDPOINT = "/journal";
    private static String AUDIT_ENDPOINT = "/audit";
    private static String LOG_ENDPOINT = "/log";


    /**
     * Sets up the server for the web service.
     *
     * @throws Exception
     */
    @BeforeClass
    public static void startWebServiceServer() throws Exception {
        server = new Server(HTTP_PORT);

        ServletHandler handler = new ServletHandler();
        server.setHandler(handler);

        handler.addServletWithMapping(JNLJournalServlet.class, JOURNAL_ENDPOINT);
        handler.addServletWithMapping(JNLAuditServlet.class, AUDIT_ENDPOINT);
        handler.addServletWithMapping(JNLLogServlet.class, LOG_ENDPOINT);

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
    public void testInitializeJournalReturnsInitializeAck() throws ClientProtocolException, IOException {

        final HttpPost httpPost = new HttpPost("http://localhost:" + HTTP_PORT + JOURNAL_ENDPOINT);
        httpPost.setHeader(HttpUtils.HDRS_CONTENT_TYPE, HttpUtils.DEFAULT_CONTENT_TYPE);
        httpPost.setHeader(HttpUtils.HDRS_MESSAGE, HttpUtils.MSG_INIT);
        httpPost.setHeader(HttpUtils.HDRS_MODE, HttpUtils.MSG_PUBLISH_LIVE);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_DIGEST, DigestMethod.SHA256);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_XML_COMPRESSION, HttpUtils.SUPPORTED_XML_COMPRESSIONS[0]);
        httpPost.setHeader(HttpUtils.HDRS_DATA_CLASS, HttpUtils.JOURNAL);
        httpPost.setHeader(HttpUtils.HDRS_VERSION, HttpUtils.SUPPORTED_VERSION);

        HttpClient client = HttpClientBuilder.create().build();

        final HttpResponse response = client.execute(httpPost);
        final String responseMessage = response.getFirstHeader(HttpUtils.HDRS_MESSAGE).getValue();
        final int responseStatus = response.getStatusLine().getStatusCode();
        assertEquals(responseStatus, 200);
        assertEquals(responseMessage, "initialize-ack");
    }

    @Test
    public void testInitializeAuditReturnsInitializeAck() throws ClientProtocolException, IOException {

        final HttpPost httpPost = new HttpPost("http://localhost:" + HTTP_PORT + AUDIT_ENDPOINT);
        httpPost.setHeader(HttpUtils.HDRS_CONTENT_TYPE, HttpUtils.DEFAULT_CONTENT_TYPE);
        httpPost.setHeader(HttpUtils.HDRS_MESSAGE, HttpUtils.MSG_INIT);
        httpPost.setHeader(HttpUtils.HDRS_MODE, HttpUtils.MSG_PUBLISH_LIVE);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_DIGEST, DigestMethod.SHA256);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_XML_COMPRESSION, HttpUtils.SUPPORTED_XML_COMPRESSIONS[0]);
        httpPost.setHeader(HttpUtils.HDRS_DATA_CLASS, HttpUtils.AUDIT);
        httpPost.setHeader(HttpUtils.HDRS_VERSION, HttpUtils.SUPPORTED_VERSION);

        HttpClient client = HttpClientBuilder.create().build();

        final HttpResponse response = client.execute(httpPost);
        final String responseMessage = response.getFirstHeader(HttpUtils.HDRS_MESSAGE).getValue();
        final int responseStatus = response.getStatusLine().getStatusCode();
        assertEquals(responseStatus, 200);
        assertEquals(responseMessage, "initialize-ack");
    }

    @Test
    public void testInitializeLogReturnsInitializeAck() throws ClientProtocolException, IOException {

        final HttpPost httpPost = new HttpPost("http://localhost:" + HTTP_PORT + LOG_ENDPOINT);
        httpPost.setHeader(HttpUtils.HDRS_CONTENT_TYPE, HttpUtils.DEFAULT_CONTENT_TYPE);
        httpPost.setHeader(HttpUtils.HDRS_MESSAGE, HttpUtils.MSG_INIT);
        httpPost.setHeader(HttpUtils.HDRS_MODE, HttpUtils.MSG_PUBLISH_LIVE);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_DIGEST, DigestMethod.SHA256);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_XML_COMPRESSION, HttpUtils.SUPPORTED_XML_COMPRESSIONS[0]);
        httpPost.setHeader(HttpUtils.HDRS_DATA_CLASS, HttpUtils.LOG);
        httpPost.setHeader(HttpUtils.HDRS_VERSION, HttpUtils.SUPPORTED_VERSION);

        HttpClient client = HttpClientBuilder.create().build();

        final HttpResponse response = client.execute(httpPost);
        final String responseMessage = response.getFirstHeader(HttpUtils.HDRS_MESSAGE).getValue();
        final int responseStatus = response.getStatusLine().getStatusCode();
        assertEquals(responseStatus, 200);
        assertEquals(responseMessage, "initialize-ack");
    }

    @Test
    public void testInvalidModeReturnsInitializeNack() throws ClientProtocolException, IOException {

        final HttpPost httpPost = new HttpPost("http://localhost:" + HTTP_PORT + JOURNAL_ENDPOINT);
        httpPost.setHeader(HttpUtils.HDRS_CONTENT_TYPE, HttpUtils.DEFAULT_CONTENT_TYPE);
        httpPost.setHeader(HttpUtils.HDRS_MESSAGE, HttpUtils.MSG_INIT);
        httpPost.setHeader(HttpUtils.HDRS_MODE, "invalid");
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_DIGEST, DigestMethod.SHA256);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_XML_COMPRESSION, HttpUtils.SUPPORTED_XML_COMPRESSIONS[0]);
        httpPost.setHeader(HttpUtils.HDRS_DATA_CLASS, HttpUtils.JOURNAL);
        httpPost.setHeader(HttpUtils.HDRS_VERSION, HttpUtils.SUPPORTED_VERSION);

        HttpClient client = HttpClientBuilder.create().build();

        final HttpResponse response = client.execute(httpPost);
        final String responseMessage = response.getFirstHeader(HttpUtils.HDRS_MESSAGE).getValue();
        final int responseStatus = response.getStatusLine().getStatusCode();
        assertEquals(responseStatus, 200);
        assertEquals(responseMessage, "initialize-nack");
    }

    @Test
    public void testInvalidDigestReturnsInitializeNack() throws ClientProtocolException, IOException {

        final HttpPost httpPost = new HttpPost("http://localhost:" + HTTP_PORT + JOURNAL_ENDPOINT);
        httpPost.setHeader(HttpUtils.HDRS_CONTENT_TYPE, HttpUtils.DEFAULT_CONTENT_TYPE);
        httpPost.setHeader(HttpUtils.HDRS_MESSAGE, HttpUtils.MSG_INIT);
        httpPost.setHeader(HttpUtils.HDRS_MODE, HttpUtils.MSG_PUBLISH_LIVE);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_DIGEST, "invalid");
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_XML_COMPRESSION, HttpUtils.SUPPORTED_XML_COMPRESSIONS[0]);
        httpPost.setHeader(HttpUtils.HDRS_DATA_CLASS, HttpUtils.JOURNAL);
        httpPost.setHeader(HttpUtils.HDRS_VERSION, HttpUtils.SUPPORTED_VERSION);

        HttpClient client = HttpClientBuilder.create().build();

        final HttpResponse response = client.execute(httpPost);
        final String responseMessage = response.getFirstHeader(HttpUtils.HDRS_MESSAGE).getValue();
        final int responseStatus = response.getStatusLine().getStatusCode();
        assertEquals(responseStatus, 200);
        assertEquals(responseMessage, "initialize-nack");
    }

    @Test
    public void testInvalidXmlCompressionReturnsInitializeNack() throws ClientProtocolException, IOException {

        final HttpPost httpPost = new HttpPost("http://localhost:" + HTTP_PORT + JOURNAL_ENDPOINT);
        httpPost.setHeader(HttpUtils.HDRS_CONTENT_TYPE, HttpUtils.DEFAULT_CONTENT_TYPE);
        httpPost.setHeader(HttpUtils.HDRS_MESSAGE, HttpUtils.MSG_INIT);
        httpPost.setHeader(HttpUtils.HDRS_MODE, HttpUtils.MSG_PUBLISH_LIVE);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_DIGEST, DigestMethod.SHA256);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_XML_COMPRESSION, "invalid");
        httpPost.setHeader(HttpUtils.HDRS_DATA_CLASS, HttpUtils.JOURNAL);
        httpPost.setHeader(HttpUtils.HDRS_VERSION, HttpUtils.SUPPORTED_VERSION);

        HttpClient client = HttpClientBuilder.create().build();

        final HttpResponse response = client.execute(httpPost);
        final String responseMessage = response.getFirstHeader(HttpUtils.HDRS_MESSAGE).getValue();
        final int responseStatus = response.getStatusLine().getStatusCode();
        assertEquals(responseStatus, 200);
        assertEquals(responseMessage, "initialize-nack");
    }

    @Test
    public void testInvalidDataClassReturnsInitializeNack() throws ClientProtocolException, IOException {

        final HttpPost httpPost = new HttpPost("http://localhost:" + HTTP_PORT + JOURNAL_ENDPOINT);
        httpPost.setHeader(HttpUtils.HDRS_CONTENT_TYPE, HttpUtils.DEFAULT_CONTENT_TYPE);
        httpPost.setHeader(HttpUtils.HDRS_MESSAGE, HttpUtils.MSG_INIT);
        httpPost.setHeader(HttpUtils.HDRS_MODE, HttpUtils.MSG_PUBLISH_LIVE);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_DIGEST, DigestMethod.SHA256);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_XML_COMPRESSION, HttpUtils.SUPPORTED_XML_COMPRESSIONS[0]);
        httpPost.setHeader(HttpUtils.HDRS_DATA_CLASS, "invalid");
        httpPost.setHeader(HttpUtils.HDRS_VERSION, HttpUtils.SUPPORTED_VERSION);

        HttpClient client = HttpClientBuilder.create().build();

        final HttpResponse response = client.execute(httpPost);
        final String responseMessage = response.getFirstHeader(HttpUtils.HDRS_MESSAGE).getValue();
        final int responseStatus = response.getStatusLine().getStatusCode();
        assertEquals(responseStatus, 200);
        assertEquals(responseMessage, "initialize-nack");
    }

    @Test
    public void testInvalidVersionReturnsInitializeNack() throws ClientProtocolException, IOException {

        final HttpPost httpPost = new HttpPost("http://localhost:" + HTTP_PORT + JOURNAL_ENDPOINT);
        httpPost.setHeader(HttpUtils.HDRS_CONTENT_TYPE, HttpUtils.DEFAULT_CONTENT_TYPE);
        httpPost.setHeader(HttpUtils.HDRS_MESSAGE, HttpUtils.MSG_INIT);
        httpPost.setHeader(HttpUtils.HDRS_MODE, HttpUtils.MSG_PUBLISH_LIVE);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_DIGEST, DigestMethod.SHA256);
        httpPost.setHeader(HttpUtils.HDRS_ACCEPT_XML_COMPRESSION, HttpUtils.SUPPORTED_XML_COMPRESSIONS[0]);
        httpPost.setHeader(HttpUtils.HDRS_DATA_CLASS, HttpUtils.JOURNAL);
        httpPost.setHeader(HttpUtils.HDRS_VERSION,"invalid");

        HttpClient client = HttpClientBuilder.create().build();

        final HttpResponse response = client.execute(httpPost);
        final String responseMessage = response.getFirstHeader(HttpUtils.HDRS_MESSAGE).getValue();
        final int responseStatus = response.getStatusLine().getStatusCode();
        assertEquals(responseStatus, 200);
        assertEquals(responseMessage, "initialize-nack");
    }

    @Test
    public void testValidateModeWorksWithPublishLive() {

        final String mode = "publish-live";
        final HashMap <String,String> errorResponseHeaders = new HashMap<String,String>();
        final boolean returned = HttpUtils.validateMode(mode, errorResponseHeaders);
        assertTrue(returned);
        assertTrue(errorResponseHeaders.isEmpty());
    }

    @Test
    public void testValidateModeWorksWithPublishArchive() {

        final String mode = "publish-archival";
        final HashMap <String,String> errorResponseHeaders = new HashMap<String,String>();
        final boolean returned = HttpUtils.validateMode(mode, errorResponseHeaders);
        assertTrue(returned);
        assertTrue(errorResponseHeaders.isEmpty());
    }

    @Test
    public void testValidateModeFailsWithSubscribeLive() {

        final String mode = "subscribe-live";
        final HashMap <String,String> errorResponseHeaders = new HashMap<String,String>();
        final boolean returned = HttpUtils.validateMode(mode, errorResponseHeaders);
        assertFalse(returned);
        for (Map.Entry<String, String> entry : errorResponseHeaders.entrySet()) {
            assertEquals(entry.getKey(), "JAL-Unsupported-Mode");
            assertEquals(entry.getValue(), "");
        }
    }

    @Test
    public void testValidateModeFailsWithSubscribeArchive() {

        final String mode = "subscribe-archival";
        final HashMap <String,String> errorResponseHeaders = new HashMap<String,String>();
        final boolean returned = HttpUtils.validateMode(mode, errorResponseHeaders);
        assertFalse(returned);
        for (Map.Entry<String, String> entry : errorResponseHeaders.entrySet()) {
            assertEquals(entry.getKey(), "JAL-Unsupported-Mode");
            assertEquals(entry.getValue(), "");
        }
    }

    @Test
    public void testValidateModeFailsForEmptyMode() {

        final String mode = "";
        final HashMap <String,String> errorResponseHeaders = new HashMap<String,String>();
        final boolean returned = HttpUtils.validateMode(mode, errorResponseHeaders);
        assertFalse(returned);
        for (Map.Entry<String, String> entry : errorResponseHeaders.entrySet()) {
            assertEquals(entry.getKey(), "JAL-Unsupported-Mode");
            assertEquals(entry.getValue(), "");
        }
    }

    @Test
    public void testValidateDigestWorksWithSHA256() {

        final String digests = "http://www.w3.org/2001/04/xmlenc#sha256";
        final HashMap <String,String> successResponseHeaders = new HashMap<String,String>();
        final HashMap <String,String> errorResponseHeaders = new HashMap<String,String>();
        final boolean returned = HttpUtils.validateDigests(digests, successResponseHeaders, errorResponseHeaders);
        assertTrue(returned);
        for (Map.Entry<String, String> entry : successResponseHeaders.entrySet()) {
            assertEquals(entry.getKey(), "JAL-Digest");
            assertEquals(entry.getValue(), "http://www.w3.org/2001/04/xmlenc#sha256");
        }
        assertTrue(errorResponseHeaders.isEmpty());
    }

    @Test
    public void testValidateDigestWorksWithSHA256andJunkDigests() {

        final String digests = "junk digest,http://www.w3.org/2001/04/xmlenc#sha256";
        final HashMap <String,String> successResponseHeaders = new HashMap<String,String>();
        final HashMap <String,String> errorResponseHeaders = new HashMap<String,String>();
        final boolean returned = HttpUtils.validateDigests(digests, successResponseHeaders, errorResponseHeaders);
        assertTrue(returned);
        for (Map.Entry<String, String> entry : successResponseHeaders.entrySet()) {
            assertEquals(entry.getKey(), "JAL-Digest");
            assertEquals(entry.getValue(), "http://www.w3.org/2001/04/xmlenc#sha256");
        }
        assertTrue(errorResponseHeaders.isEmpty());
    }

    ///TODO add tests for the rest of the future supported digests

    @Test
    public void testValidateDigestFailsForEmptyDigest() {

        final String digests = "";
        final HashMap <String,String> successResponseHeaders = new HashMap<String,String>();
        final HashMap <String,String> errorResponseHeaders = new HashMap<String,String>();
        final boolean returned = HttpUtils.validateDigests(digests, successResponseHeaders, errorResponseHeaders);
        assertFalse(returned);
        assertTrue(successResponseHeaders.isEmpty());
        for (Map.Entry<String, String> entry : errorResponseHeaders.entrySet()) {
            assertEquals(entry.getKey(), "JAL-Unsupported-Digest");
            assertEquals(entry.getValue(), "");
        }
    }

    @Test
    public void testValidateXmlCompressionWorksWithNone() {

        final String xmlCompressions = "none";
        final HashMap <String,String> successResponseHeaders = new HashMap<String,String>();
        final HashMap <String,String> errorResponseHeaders = new HashMap<String,String>();
        final boolean returned = HttpUtils.validateXmlCompression(xmlCompressions, successResponseHeaders, errorResponseHeaders);
        assertTrue(returned);
        for (Map.Entry<String, String> entry : successResponseHeaders.entrySet()) {
            assertEquals(entry.getKey(), "JAL-XML-Compression");
            assertEquals(entry.getValue(), "none");
        }
        assertTrue(errorResponseHeaders.isEmpty());
    }

    @Test
    public void testValidateXmlCompressionWorksWithNoneandJunkCompression() {

        final String xmlCompressions = "junk compression,none";
        final HashMap <String,String> successResponseHeaders = new HashMap<String,String>();
        final HashMap <String,String> errorResponseHeaders = new HashMap<String,String>();
        final boolean returned = HttpUtils.validateXmlCompression(xmlCompressions, successResponseHeaders, errorResponseHeaders);
        assertTrue(returned);
        for (Map.Entry<String, String> entry : successResponseHeaders.entrySet()) {
            assertEquals(entry.getKey(), "JAL-XML-Compression");
            assertEquals(entry.getValue(), "none");
        }
        assertTrue(errorResponseHeaders.isEmpty());
    }

    @Test
    public void testValidateXmlCompressionWorksWithEXI10() {

        final String xmlCompressions = "exi-1.0";
        final HashMap <String,String> successResponseHeaders = new HashMap<String,String>();
        final HashMap <String,String> errorResponseHeaders = new HashMap<String,String>();
        final boolean returned = HttpUtils.validateXmlCompression(xmlCompressions, successResponseHeaders, errorResponseHeaders);
        assertTrue(returned);
        for (Map.Entry<String, String> entry : successResponseHeaders.entrySet()) {
            assertEquals(entry.getKey(), "JAL-XML-Compression");
            assertEquals(entry.getValue(), "exi-1.0");
        }
        assertTrue(errorResponseHeaders.isEmpty());
    }

    @Test
    public void testValidateXmlCompressionWorksWithDeflate() {

        final String xmlCompressions = "deflate";
        final HashMap <String,String> successResponseHeaders = new HashMap<String,String>();
        final HashMap <String,String> errorResponseHeaders = new HashMap<String,String>();
        final boolean returned = HttpUtils.validateXmlCompression(xmlCompressions, successResponseHeaders, errorResponseHeaders);
        assertTrue(returned);
        for (Map.Entry<String, String> entry : successResponseHeaders.entrySet()) {
            assertEquals(entry.getKey(), "JAL-XML-Compression");
            assertEquals(entry.getValue(), "deflate");
        }
        assertTrue(errorResponseHeaders.isEmpty());
    }

    @Test
    public void testValidateXmlCompressionWorksWithMultipleValid() {

        final String xmlCompressions = "exi-1.0,deflate";
        final HashMap <String,String> successResponseHeaders = new HashMap<String,String>();
        final HashMap <String,String> errorResponseHeaders = new HashMap<String,String>();
        final boolean returned = HttpUtils.validateXmlCompression(xmlCompressions, successResponseHeaders, errorResponseHeaders);
        assertTrue(returned);
        for (Map.Entry<String, String> entry : successResponseHeaders.entrySet()) {
            assertEquals(entry.getKey(), "JAL-XML-Compression");
            assertEquals(entry.getValue(), "exi-1.0");
        }
        assertTrue(errorResponseHeaders.isEmpty());
    }

    @Test
    public void testValidateXmlCompressionFailsForEmptyCompressions() {

        final String xmlCompressions = "";
        final HashMap <String,String> successResponseHeaders = new HashMap<String,String>();
        final HashMap <String,String> errorResponseHeaders = new HashMap<String,String>();
        final boolean returned = HttpUtils.validateXmlCompression(xmlCompressions, successResponseHeaders, errorResponseHeaders);
        assertFalse(returned);
        assertTrue(successResponseHeaders.isEmpty());
        for (Map.Entry<String, String> entry : errorResponseHeaders.entrySet()) {
            assertEquals(entry.getKey(), "JAL-Unsupported-XML-Compression");
            assertEquals(entry.getValue(), "");
        }
    }

    @Test
    public void testValidateXmlCompressionFailsForInvalidCompressions() {

        final String xmlCompressions = "invalid";
        final HashMap <String,String> successResponseHeaders = new HashMap<String,String>();
        final HashMap <String,String> errorResponseHeaders = new HashMap<String,String>();
        final boolean returned = HttpUtils.validateXmlCompression(xmlCompressions, successResponseHeaders, errorResponseHeaders);
        assertFalse(returned);
        assertTrue(successResponseHeaders.isEmpty());
        for (Map.Entry<String, String> entry : errorResponseHeaders.entrySet()) {
            assertEquals(entry.getKey(), "JAL-Unsupported-XML-Compression");
            assertEquals(entry.getValue(), "");
        }
    }

    @Test
    public void testValidateDataClassWorksForJournal() throws MissingMimeHeaderException, UnexpectedMimeValueException {
        final String dataClass = "journal";
        HashMap <String,String> errorResponseHeaders = new HashMap<String,String>();
        final boolean returned = HttpUtils.validateDataClass(dataClass, dataClass, errorResponseHeaders);
        assertTrue(returned);
    }

    @Test
    public void testValidateDataClassWorksForAudit() throws MissingMimeHeaderException, UnexpectedMimeValueException {
        final String dataClass = "audit";
        HashMap <String,String> errorResponseHeaders = new HashMap<String,String>();
        final boolean returned = HttpUtils.validateDataClass(dataClass, dataClass, errorResponseHeaders);
        assertTrue(returned);
    }

    @Test
    public void testValidateDataClassWorksForLog() throws MissingMimeHeaderException, UnexpectedMimeValueException {
        final String dataClass = "log";
        HashMap <String,String> errorResponseHeaders = new HashMap<String,String>();
        final boolean returned = HttpUtils.validateDataClass(dataClass, dataClass, errorResponseHeaders);
        assertTrue(returned);
    }

    public void testValidateDataClassFailsForEmptyDataClass() throws MissingMimeHeaderException, UnexpectedMimeValueException {
        final String dataClass = "";
        HashMap <String,String> errorResponseHeaders = new HashMap<String,String>();
        final boolean returned = HttpUtils.validateDataClass(dataClass, dataClass, errorResponseHeaders);
        assertFalse(returned);
    }

    public void testValidateDataClassFailsForInvalidDataClass() throws MissingMimeHeaderException, UnexpectedMimeValueException {
        final String dataClass = "invalid";
        final String mode = "journal";
        HashMap <String,String> errorResponseHeaders = new HashMap<String,String>();
        final boolean returned = HttpUtils.validateDataClass(dataClass, mode, errorResponseHeaders);
        assertFalse(returned);
    }

    @Test
    public void testValidateVersionWorks() {
        final String version = "2.0";
        final HashMap<String, String> errorResponseHeaders = new HashMap<String, String>();
        final boolean returned = HttpUtils.validateVersion(version, errorResponseHeaders);
        assertTrue(returned);
        assertTrue(errorResponseHeaders.isEmpty());
    }

    @Test
    public void testValidateVersionFailsForEmptyVersion() {
        final String version = "";
        final HashMap<String, String> errorResponseHeaders = new HashMap<String, String>();
        final boolean returned = HttpUtils.validateVersion(version, errorResponseHeaders);
        assertFalse(returned);
        for (Map.Entry<String, String> entry : errorResponseHeaders.entrySet()) {
            assertEquals(entry.getKey(), "JAL-Unsupported-Version");
            assertEquals(entry.getValue(), "");
        }
    }

    @Test
    public void testValidateVersionFailsForInvalidVersion() {
        final String version = "1.0";
        final HashMap<String, String> errorResponseHeaders = new HashMap<String, String>();
        final boolean returned = HttpUtils.validateVersion(version, errorResponseHeaders);
        assertFalse(returned);
        for (Map.Entry<String, String> entry : errorResponseHeaders.entrySet()) {
            assertEquals(entry.getKey(), "JAL-Unsupported-Version");
            assertEquals(entry.getValue(), "");
        }
    }

    @Test
    public void testCheckForEmptyStringReturnsTrimmedString() {

        final String padded = "   string   ";
        final String returned = HttpUtils.checkForEmptyString(padded, "padded string");
        assertEquals(padded.trim(), returned);
        assertEquals(returned, "string");
    }

    @Test
    public void testMakeStringListWorks() {

        final List<String> words = new ArrayList<String>();
        words.add("word1");
        words.add("word2");
        words.add("word3");
        final String wordList = HttpUtils.makeStringList(words, "listname");
        assertEquals(wordList, "word1, word2, word3");
    }

    @Test
    public void testMakeStringListHasComma() {

        final List<String> words = new ArrayList<String>();
        words.add("word1");
        words.add("word2");
        final String wordList = HttpUtils.makeStringList(words, "listname");
        assertTrue(wordList.contains(","));
    }

    @Test
    public void testMakeStringListOneWord() {

        final List<String> words = new ArrayList<String>();
        words.add("word1");
        final String wordList = HttpUtils.makeStringList(words, "listname");
        assertEquals(wordList, "word1");
    }

    @Test
    public void testMakeStringListReturnsNullWhenNull() {

        final String wordList = HttpUtils.makeStringList(null, "listname");
        assertNull(wordList);
    }

    @Test
    public void testMakeStringListReturnsNullWhenBlank() {

        final List<String> words = new ArrayList<String>();
        final String wordList = HttpUtils.makeStringList(words, "listname");
        assertNull(wordList);
    }
}