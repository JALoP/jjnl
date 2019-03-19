package com.tresys.jalop.jnl.impl.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.tresys.jalop.jnl.exceptions.MissingMimeHeaderException;
import com.tresys.jalop.jnl.exceptions.UnexpectedMimeValueException;

/**
 * Tests for common utility class.
 */
public class HttpUtilsTest {

    @Test
    public void testValidateModeWorksWithPublishLive() {

        final String mode = "publish-live";
        final List <String> errorResponseHeaders = new ArrayList<String>();
        final boolean returned = HttpUtils.validateMode(mode, errorResponseHeaders);
        assertTrue(returned);
        assertTrue(errorResponseHeaders.isEmpty());
    }

    @Test
    public void testValidateModeWorksWithPublishArchive() {

        final String mode = "publish-archival";
        final List <String> errorResponseHeaders = new ArrayList<String>();
        final boolean returned = HttpUtils.validateMode(mode, errorResponseHeaders);
        assertTrue(returned);
        assertTrue(errorResponseHeaders.isEmpty());
    }

    @Test
    public void testValidateModeFailsWithSubscribeLive() {

        final String mode = "subscribe-live";
        final List <String> errorResponseHeaders = new ArrayList<String>();
        final boolean returned = HttpUtils.validateMode(mode, errorResponseHeaders);
        assertFalse(returned);
        for (String entry : errorResponseHeaders) {
            assertEquals(entry, "JAL-Unsupported-Mode");
        }
    }

    @Test
    public void testValidateModeFailsWithSubscribeArchive() {

        final String mode = "subscribe-archival";
        final List <String> errorResponseHeaders = new ArrayList<String>();
        final boolean returned = HttpUtils.validateMode(mode, errorResponseHeaders);
        assertFalse(returned);
        for (String entry : errorResponseHeaders) {
            assertEquals(entry, "JAL-Unsupported-Mode");
        }
    }

    @Test
    public void testValidateModeFailsForEmptyMode() {

        final String mode = "";
        final List <String> errorResponseHeaders = new ArrayList<String>();
        final boolean returned = HttpUtils.validateMode(mode, errorResponseHeaders);
        assertFalse(returned);
        for (String entry : errorResponseHeaders) {
            assertEquals(entry, "JAL-Unsupported-Mode");
        }
    }

    @Test
    public void testValidateDigestWorksWithSHA256() {

        final String digests = "http://www.w3.org/2001/04/xmlenc#sha256";
        final HashMap <String,String> successResponseHeaders = new HashMap<String,String>();
        final List <String> errorResponseHeaders = new ArrayList<String>();
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
        final List <String> errorResponseHeaders = new ArrayList<String>();
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
        final List <String> errorResponseHeaders = new ArrayList<String>();
        final boolean returned = HttpUtils.validateDigests(digests, successResponseHeaders, errorResponseHeaders);
        assertFalse(returned);
        assertTrue(successResponseHeaders.isEmpty());
        for (String entry : errorResponseHeaders) {
            assertEquals(entry, "JAL-Unsupported-Digest");
        }
    }

    @Test
    public void testValidateXmlCompressionWorksWithNone() {

        final String xmlCompressions = "none";
        final HashMap <String,String> successResponseHeaders = new HashMap<String,String>();
        final List <String> errorResponseHeaders = new ArrayList<String>();
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
        final List <String> errorResponseHeaders = new ArrayList<String>();
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
        final List <String> errorResponseHeaders = new ArrayList<String>();
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
        final List <String> errorResponseHeaders = new ArrayList<String>();
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
        final List <String> errorResponseHeaders = new ArrayList<String>();
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
        final List <String> errorResponseHeaders = new ArrayList<String>();
        final boolean returned = HttpUtils.validateXmlCompression(xmlCompressions, successResponseHeaders, errorResponseHeaders);
        assertFalse(returned);
        assertTrue(successResponseHeaders.isEmpty());
        for (String entry : errorResponseHeaders) {
            assertEquals(entry, "JAL-Unsupported-XML-Compression");
        }
    }

    @Test
    public void testValidateXmlCompressionFailsForInvalidCompressions() {

        final String xmlCompressions = "invalid";
        final HashMap <String,String> successResponseHeaders = new HashMap<String,String>();
        final List <String> errorResponseHeaders = new ArrayList<String>();
        final boolean returned = HttpUtils.validateXmlCompression(xmlCompressions, successResponseHeaders, errorResponseHeaders);
        assertFalse(returned);
        assertTrue(successResponseHeaders.isEmpty());
        for (String entry : errorResponseHeaders) {
            assertEquals(entry, "JAL-Unsupported-XML-Compression");
        }
    }

    @Test
    public void testValidateDataClassWorksForJournal() throws MissingMimeHeaderException, UnexpectedMimeValueException {
        final String dataClass = "journal";
        List <String> errorResponseHeaders = new ArrayList<String>();
        final boolean returned = HttpUtils.validateDataClass(dataClass, dataClass, errorResponseHeaders);
        assertTrue(returned);
    }

    @Test
    public void testValidateDataClassWorksForAudit() throws MissingMimeHeaderException, UnexpectedMimeValueException {
        final String dataClass = "audit";
        List <String> errorResponseHeaders = new ArrayList<String>();
        final boolean returned = HttpUtils.validateDataClass(dataClass, dataClass, errorResponseHeaders);
        assertTrue(returned);
    }

    @Test
    public void testValidateDataClassWorksForLog() throws MissingMimeHeaderException, UnexpectedMimeValueException {
        final String dataClass = "log";
        List <String> errorResponseHeaders = new ArrayList<String>();
        final boolean returned = HttpUtils.validateDataClass(dataClass, dataClass, errorResponseHeaders);
        assertTrue(returned);
    }

    public void testValidateDataClassFailsForEmptyDataClass() throws MissingMimeHeaderException, UnexpectedMimeValueException {
        final String dataClass = "";
        List <String> errorResponseHeaders = new ArrayList<String>();
        final boolean returned = HttpUtils.validateDataClass(dataClass, dataClass, errorResponseHeaders);
        assertFalse(returned);
    }

    public void testValidateDataClassFailsForInvalidDataClass() throws MissingMimeHeaderException, UnexpectedMimeValueException {
        final String dataClass = "invalid";
        final String mode = "journal";
        List <String> errorResponseHeaders = new ArrayList<String>();
        final boolean returned = HttpUtils.validateDataClass(dataClass, mode, errorResponseHeaders);
        assertFalse(returned);
    }

    @Test
    public void testValidateVersionWorks() {
        final String version = "2.0.0.0";
        final List<String> errorResponseHeaders = new ArrayList<String>();
        final boolean returned = HttpUtils.validateVersion(version, errorResponseHeaders);
        assertTrue(returned);
        assertTrue(errorResponseHeaders.isEmpty());
    }

    @Test
    public void testValidateVersionFailsForEmptyVersion() {
        final String version = "";
        final List<String> errorResponseHeaders = new ArrayList<String>();
        final boolean returned = HttpUtils.validateVersion(version, errorResponseHeaders);
        assertFalse(returned);
        for (String entry : errorResponseHeaders) {
            assertEquals(entry, "JAL-Unsupported-Version");
        }
    }

    @Test
    public void testValidateVersionFailsForInvalidVersion() {
        final String version = "1.0";
        final List<String> errorResponseHeaders = new ArrayList<String>();
        final boolean returned = HttpUtils.validateVersion(version, errorResponseHeaders);
        assertFalse(returned);
        for (String entry : errorResponseHeaders) {
            assertEquals(entry, "JAL-Unsupported-Version");
        }
    }

    @Test
    public void testValidateConfigureDigestChallengeWorksWithOn() {
        final String configureDigestChallenge = "on";
        final HashMap <String,String> successResponseHeaders = new HashMap<String,String>();
        final List<String> errorResponseHeaders = new ArrayList<String>();
        final boolean returned = HttpUtils.validateConfigureDigestChallenge(configureDigestChallenge, successResponseHeaders, errorResponseHeaders);
        assertTrue(returned);
        assertTrue(errorResponseHeaders.isEmpty());
        for (Map.Entry<String, String> entry : successResponseHeaders.entrySet()) {
            assertEquals(entry.getKey(), "JAL-Configure-Digest-Challenge");
            assertEquals(entry.getValue(), "on");
        }
    }

    @Test
    public void testValidateConfigureDigestChallengeWorksWithOff() {
        final String configureDigestChallenge = "off";
        final HashMap <String,String> successResponseHeaders = new HashMap<String,String>();
        final List<String> errorResponseHeaders = new ArrayList<String>();
        List<String> allowedConfigureDigests = HttpUtils.getAllowedConfigureDigests();
        allowedConfigureDigests.add("off");
        HttpUtils.setAllowedConfigureDigests(allowedConfigureDigests);

        final boolean returned = HttpUtils.validateConfigureDigestChallenge(configureDigestChallenge, successResponseHeaders, errorResponseHeaders);
        assertTrue(returned);
        assertTrue(errorResponseHeaders.isEmpty());
        for (Map.Entry<String, String> entry : successResponseHeaders.entrySet()) {
            assertEquals(entry.getKey(), "JAL-Configure-Digest-Challenge");
            assertEquals(entry.getValue(), "off");
        }
    }

    @Test
    public void testValidateConfigureDigestChallengeWorksWithOnAndOff() {
        final String configureDigestChallenge = "on,off";
        final HashMap <String,String> successResponseHeaders = new HashMap<String,String>();
        final List<String> errorResponseHeaders = new ArrayList<String>();
        final boolean returned = HttpUtils.validateConfigureDigestChallenge(configureDigestChallenge, successResponseHeaders, errorResponseHeaders);
        assertTrue(returned);
        assertTrue(errorResponseHeaders.isEmpty());
        for (Map.Entry<String, String> entry : successResponseHeaders.entrySet()) {
            assertEquals(entry.getKey(), "JAL-Configure-Digest-Challenge");
            assertEquals(entry.getValue(), "on");
        }
    }

    @Test
    public void testValidateConfigureDigestChallengeWorksWithOffAndOn() {
        final String configureDigestChallenge = "off,on";
        final HashMap <String,String> successResponseHeaders = new HashMap<String,String>();
        final List<String> errorResponseHeaders = new ArrayList<String>();
        final boolean returned = HttpUtils.validateConfigureDigestChallenge(configureDigestChallenge, successResponseHeaders, errorResponseHeaders);
        assertTrue(returned);
        assertTrue(errorResponseHeaders.isEmpty());
        for (Map.Entry<String, String> entry : successResponseHeaders.entrySet()) {
            assertEquals(entry.getKey(), "JAL-Configure-Digest-Challenge");
            assertEquals(entry.getValue(), "off");
        }
    }

    @Test
    public void testValidateConfigureDigestChallengeFailsForEmptyDigestChallenge() {
        final String configureDigestChallenge = "";
        final HashMap <String,String> successResponseHeaders = new HashMap<String,String>();
        final List<String> errorResponseHeaders = new ArrayList<String>();
        final boolean returned = HttpUtils.validateConfigureDigestChallenge(configureDigestChallenge, successResponseHeaders, errorResponseHeaders);
        assertFalse(returned);
        assertTrue(successResponseHeaders.isEmpty());
        for (String entry : errorResponseHeaders) {
            assertEquals(entry, "JAL-Unsupported-Configure-Digest-Challenge");
        }
    }

    @Test
    public void testValidateConfigureDigestChallengeFailsForInvalidDigestChallenge() {
        final String configureDigestChallenge = "invalid";
        final HashMap <String,String> successResponseHeaders = new HashMap<String,String>();
        final List<String> errorResponseHeaders = new ArrayList<String>();
        final boolean returned = HttpUtils.validateConfigureDigestChallenge(configureDigestChallenge, successResponseHeaders, errorResponseHeaders);
        assertFalse(returned);
        assertTrue(successResponseHeaders.isEmpty());
        for (String entry : errorResponseHeaders) {
            assertEquals(entry, "JAL-Unsupported-Configure-Digest-Challenge");
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