package com.tresys.jalop.jnl.impl.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Level;
import org.junit.BeforeClass;
import org.junit.Test;

import com.tresys.jalop.jnl.DigestStatus;
import com.tresys.jalop.jnl.Mode;
import com.tresys.jalop.jnl.RecordType;
import com.tresys.jalop.jnl.exceptions.MissingMimeHeaderException;
import com.tresys.jalop.jnl.exceptions.UnexpectedMimeValueException;

/**
 * Tests for common utility class.
 */
public class HttpUtilsTest {

    @BeforeClass
    public static void init() throws Exception {
        TestResources.configureLogging(Level.DEBUG);
    }

    @Test
    public void testGetAllowedConfigureDigests()
    {
        HttpUtils.setAllowedConfigureDigests(null);
        List<String> configureDigests = HttpUtils.getAllowedConfigureDigests();
        assertNotNull(configureDigests);
        assertEquals(1, configureDigests.size());
    }

    @Test
    public void testValidatePublisherIdWorksWithUUID() {

        final String publisherId = "ae8a54d7-dd7c-4c50-a7e7-f948a140c556";
        final List <String> errorResponseHeaders = new ArrayList<String>();
        final boolean returned = HttpUtils.validatePublisherId(publisherId, errorResponseHeaders);
        assertTrue(returned);
        assertTrue(errorResponseHeaders.isEmpty());
    }

    @Test
    public void testValidatePublisherIdFailsWithInvalidUUID() {

        final String publisherId = "ae8a54d7-4c50-a7e7-f948a140c556";
        final List <String> errorResponseHeaders = new ArrayList<String>();
        final boolean returned = HttpUtils.validatePublisherId(publisherId, errorResponseHeaders);
        assertFalse(returned);
        for (String entry : errorResponseHeaders) {
            assertEquals("JAL-Unsupported-Publisher-Id", entry);
        }
    }

    @Test
    public void testValidatePublisherIdFailsWithEmptyUUID() {

        final String publisherId = "";
        final List <String> errorResponseHeaders = new ArrayList<String>();
        final boolean returned = HttpUtils.validatePublisherId(publisherId, errorResponseHeaders);
        assertFalse(returned);
        for (String entry : errorResponseHeaders) {
            assertEquals("JAL-Unsupported-Publisher-Id", entry);
        }
    }

    @Test
    public void testValidateModeWorksWithLive() {

        final String mode = "live";
        final List <String> errorResponseHeaders = new ArrayList<String>();
        final boolean returned = HttpUtils.validateMode(mode, Mode.Live, errorResponseHeaders);
        assertTrue(returned);
        assertTrue(errorResponseHeaders.isEmpty());
    }

    @Test
    public void testValidateModeWorksWithArchive() {

        final String mode = "archival";
        final List <String> errorResponseHeaders = new ArrayList<String>();
        final boolean returned = HttpUtils.validateMode(mode, Mode.Archive, errorResponseHeaders);
        assertTrue(returned);
        assertTrue(errorResponseHeaders.isEmpty());
    }

    @Test
    public void testValidateModeFailsWithSubscribeLive() {

        final String mode = "subscribe-live";
        final List <String> errorResponseHeaders = new ArrayList<String>();
        final boolean returned = HttpUtils.validateMode(mode, Mode.Live, errorResponseHeaders);
        assertFalse(returned);
        for (String entry : errorResponseHeaders) {
            assertEquals("JAL-Unsupported-Mode", entry);
        }
    }

    @Test
    public void testValidateModeFailsWithSubscribeArchive() {

        final String mode = "subscribe-archival";
        final List <String> errorResponseHeaders = new ArrayList<String>();
        final boolean returned = HttpUtils.validateMode(mode, Mode.Archive, errorResponseHeaders);
        assertFalse(returned);
        for (String entry : errorResponseHeaders) {
            assertEquals("JAL-Unsupported-Mode", entry);
        }
    }

    @Test
    public void testValidateModeFailsWithNotSupportedLiveMode() {

        final String mode = "live";
        final List <String> errorResponseHeaders = new ArrayList<String>();
        final boolean returned = HttpUtils.validateMode(mode, Mode.Archive, errorResponseHeaders);
        assertFalse(returned);
        for (String entry : errorResponseHeaders) {
            assertEquals("JAL-Unsupported-Mode", entry);
        }
    }

    @Test
    public void testValidateModeFailsWithNotSupportedArchiveMode() {

        final String mode = "archival";
        final List <String> errorResponseHeaders = new ArrayList<String>();
        final boolean returned = HttpUtils.validateMode(mode, Mode.Live, errorResponseHeaders);
        assertFalse(returned);
        for (String entry : errorResponseHeaders) {
            assertEquals("JAL-Unsupported-Mode", entry);
        }
    }

    @Test
    public void testValidateModeFailsWithNullSupportedMode() {

        final String mode = "archival";
        final List <String> errorResponseHeaders = new ArrayList<String>();
        final boolean returned = HttpUtils.validateMode(mode, null, errorResponseHeaders);
        assertFalse(returned);
        for (String entry : errorResponseHeaders) {
            assertEquals("JAL-Unsupported-Mode", entry);
        }
    }

    @Test
    public void testValidateModeFailsWithUnsetSupportedMode() {

        final String mode = "unset";
        final List <String> errorResponseHeaders = new ArrayList<String>();
        final boolean returned = HttpUtils.validateMode(mode, Mode.Unset, errorResponseHeaders);
        assertFalse(returned);
        for (String entry : errorResponseHeaders) {
            assertEquals("JAL-Unsupported-Mode", entry);
        }
    }

    @Test
    public void testValidateModeFailsForEmptyMode() {

        final String mode = "";
        final List <String> errorResponseHeaders = new ArrayList<String>();
        final boolean returned = HttpUtils.validateMode(mode, Mode.Live, errorResponseHeaders);
        assertFalse(returned);
        for (String entry : errorResponseHeaders) {
            assertEquals("JAL-Unsupported-Mode", entry);
        }
    }

    @Test
    public void testValidateDigestWorksWithSHA256() {

        final String digests = "http://www.w3.org/2001/04/xmlenc#sha256";
        final HashMap <String,String> successResponseHeaders = new HashMap<String,String>();
        final List <String> errorResponseHeaders = new ArrayList<String>();
        final String selectedDigest = HttpUtils.validateDigests(digests, successResponseHeaders, errorResponseHeaders);
        assertEquals("http://www.w3.org/2001/04/xmlenc#sha256", selectedDigest);
        for (Map.Entry<String, String> entry : successResponseHeaders.entrySet()) {
            assertEquals("JAL-Digest", entry.getKey());
            assertEquals("http://www.w3.org/2001/04/xmlenc#sha256", entry.getValue());
        }
        assertTrue(errorResponseHeaders.isEmpty());
    }

    @Test
    public void testValidateDigestWorksWithSHA256andJunkDigests() {

        final String digests = "junk digest,http://www.w3.org/2001/04/xmlenc#sha256";
        final HashMap <String,String> successResponseHeaders = new HashMap<String,String>();
        final List <String> errorResponseHeaders = new ArrayList<String>();
        final String selectedDigest = HttpUtils.validateDigests(digests, successResponseHeaders, errorResponseHeaders);
        assertEquals("http://www.w3.org/2001/04/xmlenc#sha256", selectedDigest);
        for (Map.Entry<String, String> entry : successResponseHeaders.entrySet()) {
            assertEquals("JAL-Digest", entry.getKey());
            assertEquals("http://www.w3.org/2001/04/xmlenc#sha256", entry.getValue());
        }
        assertTrue(errorResponseHeaders.isEmpty());
    }

    ///TODO add tests for the rest of the future supported digests

    @Test
    public void testValidateDigestFailsForEmptyDigest() {

        final String digests = "";
        final HashMap <String,String> successResponseHeaders = new HashMap<String,String>();
        final List <String> errorResponseHeaders = new ArrayList<String>();
        final String selectedDigest = HttpUtils.validateDigests(digests, successResponseHeaders, errorResponseHeaders);
        assertEquals(null, selectedDigest);
        assertTrue(successResponseHeaders.isEmpty());
        for (String entry : errorResponseHeaders) {
            assertEquals("JAL-Unsupported-Digest", entry);
        }
    }

    @Test
    public void testValidateXmlCompressionWorksWithNone() {

        final String xmlCompressions = "none";
        final HashMap <String,String> successResponseHeaders = new HashMap<String,String>();
        final List <String> errorResponseHeaders = new ArrayList<String>();
        final String selectedCompression = HttpUtils.validateXmlCompression(xmlCompressions, successResponseHeaders, errorResponseHeaders);
        assertEquals("none", selectedCompression);
        for (Map.Entry<String, String> entry : successResponseHeaders.entrySet()) {
            assertEquals("JAL-XML-Compression", entry.getKey());
            assertEquals("none", entry.getValue());
        }
        assertTrue(errorResponseHeaders.isEmpty());
    }

    @Test
    public void testValidateXmlCompressionWorksWithNoneandJunkCompression() {

        final String xmlCompressions = "junk compression,none";
        final HashMap <String,String> successResponseHeaders = new HashMap<String,String>();
        final List <String> errorResponseHeaders = new ArrayList<String>();
        final String selectedCompression = HttpUtils.validateXmlCompression(xmlCompressions, successResponseHeaders, errorResponseHeaders);
        assertEquals("none", selectedCompression);
        for (Map.Entry<String, String> entry : successResponseHeaders.entrySet()) {
            assertEquals("JAL-XML-Compression", entry.getKey());
            assertEquals("none", entry.getValue());
        }
        assertTrue(errorResponseHeaders.isEmpty());
    }

    @Test
    public void testValidateXmlCompressionWorksWithEXI10() {

        final String xmlCompressions = "exi-1.0";
        final HashMap <String,String> successResponseHeaders = new HashMap<String,String>();
        final List <String> errorResponseHeaders = new ArrayList<String>();
        final String selectedCompression = HttpUtils.validateXmlCompression(xmlCompressions, successResponseHeaders, errorResponseHeaders);
        assertEquals("exi-1.0", selectedCompression);
        for (Map.Entry<String, String> entry : successResponseHeaders.entrySet()) {
            assertEquals("JAL-XML-Compression", entry.getKey());
            assertEquals("exi-1.0", entry.getValue());
        }
        assertTrue(errorResponseHeaders.isEmpty());
    }

    @Test
    public void testValidateXmlCompressionWorksWithDeflate() {

        final String xmlCompressions = "deflate";
        final HashMap <String,String> successResponseHeaders = new HashMap<String,String>();
        final List <String> errorResponseHeaders = new ArrayList<String>();
        final String selectedCompression = HttpUtils.validateXmlCompression(xmlCompressions, successResponseHeaders, errorResponseHeaders);
        assertEquals("deflate", selectedCompression);
        for (Map.Entry<String, String> entry : successResponseHeaders.entrySet()) {
            assertEquals("JAL-XML-Compression", entry.getKey());
            assertEquals("deflate", entry.getValue());
        }
        assertTrue(errorResponseHeaders.isEmpty());
    }

    @Test
    public void testValidateXmlCompressionWorksWithMultipleValid() {

        final String xmlCompressions = "exi-1.0,deflate";
        final HashMap <String,String> successResponseHeaders = new HashMap<String,String>();
        final List <String> errorResponseHeaders = new ArrayList<String>();
        final String selectedCompression = HttpUtils.validateXmlCompression(xmlCompressions, successResponseHeaders, errorResponseHeaders);
        assertEquals("exi-1.0", selectedCompression);
        for (Map.Entry<String, String> entry : successResponseHeaders.entrySet()) {
            assertEquals("JAL-XML-Compression", entry.getKey());
            assertEquals("exi-1.0", entry.getValue());
        }
        assertTrue(errorResponseHeaders.isEmpty());
    }

    @Test
    public void testValidateXmlCompressionFailsForEmptyCompressions() {

        final String xmlCompressions = "";
        final HashMap <String,String> successResponseHeaders = new HashMap<String,String>();
        final List <String> errorResponseHeaders = new ArrayList<String>();
        final String selectedCompression = HttpUtils.validateXmlCompression(xmlCompressions, successResponseHeaders, errorResponseHeaders);
        assertEquals(null, selectedCompression);
        assertTrue(successResponseHeaders.isEmpty());
        for (String entry : errorResponseHeaders) {
            assertEquals("JAL-Unsupported-XML-Compression", entry);
        }
    }

    @Test
    public void testValidateXmlCompressionFailsForInvalidCompressions() {

        final String xmlCompressions = "invalid";
        final HashMap <String,String> successResponseHeaders = new HashMap<String,String>();
        final List <String> errorResponseHeaders = new ArrayList<String>();
        final String selectedCompression = HttpUtils.validateXmlCompression(xmlCompressions, successResponseHeaders, errorResponseHeaders);
        assertEquals(null, selectedCompression);
        assertTrue(successResponseHeaders.isEmpty());
        for (String entry : errorResponseHeaders) {
            assertEquals("JAL-Unsupported-XML-Compression", entry);
        }
    }

    @Test
    public void testValidateDataClassWorksForJournal() throws MissingMimeHeaderException, UnexpectedMimeValueException {
        final String dataClass = "journal";
        List <String> errorResponseHeaders = new ArrayList<String>();
        final boolean returned = HttpUtils.validateDataClass(dataClass, RecordType.Journal, errorResponseHeaders);
        assertTrue(returned);
    }

    @Test
    public void testValidateDataClassWorksForAudit() throws MissingMimeHeaderException, UnexpectedMimeValueException {
        final String dataClass = "audit";
        List <String> errorResponseHeaders = new ArrayList<String>();
        final boolean returned = HttpUtils.validateDataClass(dataClass, RecordType.Audit, errorResponseHeaders);
        assertTrue(returned);
    }

    @Test
    public void testValidateDataClassWorksForLog() throws MissingMimeHeaderException, UnexpectedMimeValueException {
        final String dataClass = "log";
        List <String> errorResponseHeaders = new ArrayList<String>();
        final boolean returned = HttpUtils.validateDataClass(dataClass, RecordType.Log, errorResponseHeaders);
        assertTrue(returned);
    }

    public void testValidateDataClassFailsForEmptyDataClass() throws MissingMimeHeaderException, UnexpectedMimeValueException {
        final String dataClass = "";
        List <String> errorResponseHeaders = new ArrayList<String>();
        final boolean returned = HttpUtils.validateDataClass(dataClass,RecordType.Journal, errorResponseHeaders);
        assertFalse(returned);

        final String dataClass2 = null;
        List <String> errorResponseHeaders2 = new ArrayList<String>();
        final boolean returned2= HttpUtils.validateDataClass(dataClass2,RecordType.Journal, errorResponseHeaders2);
        assertFalse(returned2);
    }

    public void testValidateDataClassFailsForInvalidDataClass() throws MissingMimeHeaderException, UnexpectedMimeValueException {
        final String dataClass = "invalid";
        List <String> errorResponseHeaders = new ArrayList<String>();
        final boolean returned = HttpUtils.validateDataClass(dataClass, RecordType.Journal, errorResponseHeaders);
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
            assertEquals("JAL-Unsupported-Version", entry);
        }
    }

    @Test
    public void testValidateVersionFailsForInvalidVersion() {
        final String version = "1.0";
        final List<String> errorResponseHeaders = new ArrayList<String>();
        final boolean returned = HttpUtils.validateVersion(version, errorResponseHeaders);
        assertFalse(returned);
        for (String entry : errorResponseHeaders) {
            assertEquals("JAL-Unsupported-Version", entry);
        }
    }

    @Test
    public void testValidateConfigureDigestChallengeWorksWithOn() {
        final String configureDigestChallenge = "on";
        final HashMap <String,String> successResponseHeaders = new HashMap<String,String>();
        final List<String> errorResponseHeaders = new ArrayList<String>();
        final String selectedConfDigestChallenge = HttpUtils.validateConfigureDigestChallenge(configureDigestChallenge, successResponseHeaders, errorResponseHeaders);
        assertEquals("on", selectedConfDigestChallenge);
        assertTrue(errorResponseHeaders.isEmpty());
        for (Map.Entry<String, String> entry : successResponseHeaders.entrySet()) {
            assertEquals("JAL-Configure-Digest-Challenge", entry.getKey());
            assertEquals("on", entry.getValue());
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

        final String selectedConfDigestChallenge = HttpUtils.validateConfigureDigestChallenge(configureDigestChallenge, successResponseHeaders, errorResponseHeaders);
        assertEquals("off", selectedConfDigestChallenge);
        assertTrue(errorResponseHeaders.isEmpty());
        for (Map.Entry<String, String> entry : successResponseHeaders.entrySet()) {
            assertEquals("JAL-Configure-Digest-Challenge", entry.getKey());
            assertEquals("off", entry.getValue());
        }
    }

    @Test
    public void testValidateConfigureDigestChallengeWorksWithOnAndOff() {
        final String configureDigestChallenge = "on,off";
        final HashMap <String,String> successResponseHeaders = new HashMap<String,String>();
        final List<String> errorResponseHeaders = new ArrayList<String>();
        final String selectedConfDigestChallenge = HttpUtils.validateConfigureDigestChallenge(configureDigestChallenge, successResponseHeaders, errorResponseHeaders);
        assertEquals("on", selectedConfDigestChallenge);
        assertTrue(errorResponseHeaders.isEmpty());
        for (Map.Entry<String, String> entry : successResponseHeaders.entrySet()) {
            assertEquals("JAL-Configure-Digest-Challenge", entry.getKey());
            assertEquals("on", entry.getValue());
        }
    }

    @Test
    public void testValidateConfigureDigestChallengeWorksWithOffAndOn() {
        final String configureDigestChallenge = "off,on";
        final HashMap <String,String> successResponseHeaders = new HashMap<String,String>();
        final List<String> errorResponseHeaders = new ArrayList<String>();
        final String selectedConfDigestChallenge = HttpUtils.validateConfigureDigestChallenge(configureDigestChallenge, successResponseHeaders, errorResponseHeaders);
        assertEquals("off", selectedConfDigestChallenge);
        assertTrue(errorResponseHeaders.isEmpty());
        for (Map.Entry<String, String> entry : successResponseHeaders.entrySet()) {
            assertEquals("JAL-Configure-Digest-Challenge", entry.getKey());
            assertEquals("off", entry.getValue());
        }
    }

    @Test
    public void testValidateConfigureDigestChallengeFailsForEmptyDigestChallenge() {
        final String configureDigestChallenge = "";
        final HashMap <String,String> successResponseHeaders = new HashMap<String,String>();
        final List<String> errorResponseHeaders = new ArrayList<String>();
        final String selectedConfDigestChallenge = HttpUtils.validateConfigureDigestChallenge(configureDigestChallenge, successResponseHeaders, errorResponseHeaders);
        assertEquals(null, selectedConfDigestChallenge);
        assertTrue(successResponseHeaders.isEmpty());
        for (String entry : errorResponseHeaders) {
            assertEquals("JAL-Unsupported-Configure-Digest-Challenge", entry);
        }
    }

    @Test
    public void testValidateConfigureDigestChallengeFailsForInvalidDigestChallenge() {
        final String configureDigestChallenge = "invalid";
        final HashMap <String,String> successResponseHeaders = new HashMap<String,String>();
        final List<String> errorResponseHeaders = new ArrayList<String>();
        final String selectedConfDigestChallenge = HttpUtils.validateConfigureDigestChallenge(configureDigestChallenge, successResponseHeaders, errorResponseHeaders);
        assertEquals(null, selectedConfDigestChallenge);
        assertTrue(successResponseHeaders.isEmpty());
        for (String entry : errorResponseHeaders) {
            assertEquals("JAL-Unsupported-Configure-Digest-Challenge", entry);
        }
    }

    @Test
    public void testCheckForEmptyStringReturnsTrimmedString() {

        final String padded = "   string   ";
        final String returned = HttpUtils.checkForEmptyString(padded, "padded string");
        assertEquals(padded.trim(), returned);
        assertEquals("string", returned);
    }

    @Test
    public void testMakeStringListWorks() {

        final List<String> words = new ArrayList<String>();
        words.add("word1");
        words.add("word2");
        words.add("word3");
        final String wordList = HttpUtils.makeStringList(words, "listname");
        assertEquals("word1, word2, word3", wordList);
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
        assertEquals("word1", wordList);
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

    @Test
    public void testGetRecordTypeAudit()
    {
        RecordType recordType = HttpUtils.getRecordType("audit");
        assertFalse(recordType.equals("audit"));
        assertFalse(recordType.equals("Audit"));
        assertEquals(RecordType.Audit, recordType);
    }

    @Test
    public void testGetRecordTypeJournal()
    {
        RecordType recordType = HttpUtils.getRecordType("journal");
        assertFalse(recordType.equals("journal"));
        assertFalse(recordType.equals("Journal"));
        assertEquals(RecordType.Journal, recordType);
    }

    @Test
    public void testGetRecordTypeLog()
    {
        RecordType recordType = HttpUtils.getRecordType("log");
        assertFalse(recordType.equals("log"));
        assertFalse(recordType.equals("Log"));
        assertEquals(RecordType.Log, recordType);
    }

    @Test
    public void testGetRecordTypeInvalid()
    {
        RecordType recordType = HttpUtils.getRecordType("invalid_data_class");
        assertEquals(RecordType.Unset, recordType);

        RecordType recordTypeNull = HttpUtils.getRecordType(null);
        assertEquals(RecordType.Unset, recordTypeNull);
    }

    @Test
    public void testGetModeEmpty()
    {
        Mode mode = HttpUtils.getMode(null);
        assertEquals(Mode.Unset, mode);

        mode = HttpUtils.getMode("");
        assertEquals(Mode.Unset, mode);
    }

    @Test
    public void testGetModeLive()
    {
        Mode mode = HttpUtils.getMode(HttpUtils.MSG_LIVE);

        assertEquals(Mode.Live, mode);
    }

    @Test
    public void testGetModeArchive()
    {
        Mode mode = HttpUtils.getMode(HttpUtils.MSG_ARCHIVE);

        assertEquals(Mode.Archive, mode);
    }

    @Test
    public void testGetDigestStatusConfirmed()
    {
        DigestStatus digestStatus = HttpUtils.getDigestStatus("confirmed");

        assertEquals(DigestStatus.Confirmed, digestStatus);
    }

    @Test
    public void testGetDigestStatusInvalid()
    {
        DigestStatus digestStatus = HttpUtils.getDigestStatus("invalid");

        assertEquals(DigestStatus.Invalid, digestStatus);
    }

    @Test
    public void testGetDigestStatusNull()
    {
        DigestStatus digestStatus = HttpUtils.getDigestStatus(null);

        assertEquals(DigestStatus.Unknown, digestStatus);
    }

    @Test
    public void testGetDigestStatusEmpty()
    {
        DigestStatus digestStatus = HttpUtils.getDigestStatus("");

        assertEquals(DigestStatus.Unknown, digestStatus);
    }

    @Test
    public void testGetDigestStatusNotValid()
    {
        DigestStatus digestStatus = HttpUtils.getDigestStatus("notvalid");

        assertEquals(DigestStatus.Unknown, digestStatus);
    }
}