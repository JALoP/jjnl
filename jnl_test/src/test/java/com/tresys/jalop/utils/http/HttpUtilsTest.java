package com.tresys.jalop.utils.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Iterator;
import java.io.IOException;

import org.apache.log4j.Level;
import org.junit.BeforeClass;
import org.junit.Test;

import com.tresys.jalop.jnl.DigestStatus;
import com.tresys.jalop.jnl.Mode;
import com.tresys.jalop.jnl.RecordType;
import com.tresys.jalop.jnl.exceptions.MissingMimeHeaderException;
import com.tresys.jalop.jnl.exceptions.UnexpectedMimeValueException;
import com.tresys.jalop.jnl.impl.http.HttpUtils;
import com.tresys.jalop.jnl.DigestAlgorithms;
import com.tresys.jalop.utils.jnltest.Config.HttpConfig;

import com.tresys.jalop.utils.jnltest.Config.ConfigurationException;
import org.json.simple.parser.ParseException;

import com.tresys.jalop.jnl.DigestAlgorithms;
import com.tresys.jalop.jnl.DigestAlgorithms.DigestAlgorithmEnum;


/**
 * Tests for common utility class.
 */
public class HttpUtilsTest {

    @BeforeClass
    public static void init() throws Exception {
        TestResources.configureLogging(Level.DEBUG);
    }

    // Test that the list is a length of 1 if we haven't added a digest
    @Test
    public void testGetAllowedConfigureDigests()
    {
        HttpUtils httpUtils = new HttpUtils();
        httpUtils.setAllowedConfigureDigests(null);
        List<String> configureDigests = httpUtils.getAllowedConfigureDigests();
        assertNotNull(configureDigests);
        assertEquals(1, configureDigests.size());
    }

    @Test
    // Test that the list is 0 length if we've only added invalid digests
    public void testGetSupportedDigestAlgorithms()
    {
        DigestAlgorithms digestAlgorithms = new DigestAlgorithms();
        digestAlgorithms.addDigestAlgorithmByName("sha999");

        HttpUtils httpUtils = new HttpUtils();
        List<String> supportedDigests = httpUtils.getSupportedDigestAlgorithms();

        assertNotNull(supportedDigests);
        assertEquals(0, supportedDigests.size());
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
    public void testValidateAuditFormatWorksWithXML() {

        String auditFormat = "xml";
        List <String> errorResponseHeaders = new ArrayList<String>();
        boolean returned = HttpUtils.validateAuditFormat(auditFormat, errorResponseHeaders);
        assertTrue(returned);
        assertTrue(errorResponseHeaders.isEmpty());

        auditFormat = "XML";
        errorResponseHeaders = new ArrayList<String>();
        returned = HttpUtils.validateAuditFormat(auditFormat, errorResponseHeaders);
        assertTrue(returned);
        assertTrue(errorResponseHeaders.isEmpty());

        auditFormat = "xMl";
        errorResponseHeaders = new ArrayList<String>();
        returned = HttpUtils.validateAuditFormat(auditFormat, errorResponseHeaders);
        assertTrue(returned);
        assertTrue(errorResponseHeaders.isEmpty());
    }

    @Test
    public void testValidateAuditFormatFailsWithNull() {

        String auditFormat = null;
        List <String> errorResponseHeaders = new ArrayList<String>();
        boolean returned = HttpUtils.validateAuditFormat(auditFormat, errorResponseHeaders);
        assertFalse(returned);
        for (String entry : errorResponseHeaders) {
            assertEquals("JAL-Unsupported-Audit-Format", entry);
        }
    }

    @Test
    public void testValidateAuditFormatFailsWithEmpty() {

        String auditFormat = "";
        List <String> errorResponseHeaders = new ArrayList<String>();
        boolean returned = HttpUtils.validateAuditFormat(auditFormat, errorResponseHeaders);
        assertFalse(returned);
        for (String entry : errorResponseHeaders) {
            assertEquals("JAL-Unsupported-Audit-Format", entry);
        }
    }

    @Test
    public void testValidateAuditFormatFailsWithInvalidValue() {

        String auditFormat = "invalid";
        List <String> errorResponseHeaders = new ArrayList<String>();
        boolean returned = HttpUtils.validateAuditFormat(auditFormat, errorResponseHeaders);
        assertFalse(returned);
        for (String entry : errorResponseHeaders) {
            assertEquals("JAL-Unsupported-Audit-Format", entry);
        }
    }

    /**
     * Checks to ensure SHA256 will be the default digest algorithm
     */
    @Test
    public void testDigestDefaultSHA256() {
        DigestAlgorithms digestAlgorithms = new DigestAlgorithms();

        HttpUtils httpUtils = new HttpUtils();

        String defaultDigest = DigestAlgorithms.JJNL_DEFAULT_ALGORITHM.toName();
        assertEquals(defaultDigest, DigestAlgorithms.JJNL_SHA256_ALGORITHM_NAME);
    }

    /**
     * Checks to ensure SHA256 will be supported by during digest selection
     */
    @Test
    public void testValidateDigestWorksWithSHA256() {
        DigestAlgorithms digestAlgorithms = new DigestAlgorithms();
        digestAlgorithms.addDigestAlgorithmByName(DigestAlgorithms.JJNL_SHA256_ALGORITHM_NAME);

        HttpUtils httpUtils = new HttpUtils();
        httpUtils.setSupportedDigestAlgorithms(digestAlgorithms.getDigestAlgorithmUris());

        final HashMap <String,String> successResponseHeaders = new HashMap<String,String>();
        final List <String> errorResponseHeaders = new ArrayList<String>();

        final String selectedDigest = httpUtils.validateDigests(DigestAlgorithms.JJNL_SHA256_ALGORITHM_URI, digestAlgorithms.getDigestAlgorithmUris(), successResponseHeaders, errorResponseHeaders);

        assertEquals(DigestAlgorithms.JJNL_SHA256_ALGORITHM_URI, selectedDigest);
        for (Map.Entry<String, String> entry : successResponseHeaders.entrySet()) {
            assertEquals(HttpUtils.HDRS_DIGEST, entry.getKey());
            assertEquals(DigestAlgorithms.JJNL_SHA256_ALGORITHM_URI, entry.getValue());
        }
        assertTrue(errorResponseHeaders.isEmpty());
    }

    /**
     * Checks to ensure SHA384 will be supported during digest selection
     * JVM support for SHA384 was introduced in version 9.0.1.  The software
     * will check so see if the JVM is at the correct version.
     */
    @Test
    public void testValidateDigestWorksWithSHA384() {
        DigestAlgorithms digestAlgorithms = new DigestAlgorithms();
        // Need to hard-code this so that it runs the same on Java 8 and 11
        digestAlgorithms.setSHA384Supported(true);
        digestAlgorithms.addDigestAlgorithmByName(DigestAlgorithms.JJNL_SHA384_ALGORITHM_NAME);

        HttpUtils httpUtils = new HttpUtils();
        httpUtils.setSupportedDigestAlgorithms(digestAlgorithms.getDigestAlgorithmUris());

        final HashMap <String,String> successResponseHeaders = new HashMap<String,String>();
        final List <String> errorResponseHeaders = new ArrayList<String>();

        final String selectedDigest = httpUtils.validateDigests(DigestAlgorithms.JJNL_SHA384_ALGORITHM_URI, digestAlgorithms.getDigestAlgorithmUris(), successResponseHeaders, errorResponseHeaders);

        assertEquals(DigestAlgorithms.JJNL_SHA384_ALGORITHM_URI, selectedDigest);
        for (Map.Entry<String, String> entry : successResponseHeaders.entrySet()) {
            assertEquals(HttpUtils.HDRS_DIGEST, entry.getKey());
            assertEquals(DigestAlgorithms.JJNL_SHA384_ALGORITHM_URI, entry.getValue());
        }
        assertTrue(errorResponseHeaders.isEmpty());
    }

    /**
     * Checks to ensure SHA384 will fail when unsupported
     */
    @Test
    public void testValidateDigestFailsWithSHA384Unsupported() {
        DigestAlgorithms digestAlgorithms = new DigestAlgorithms();
        // Need to hard-code this so that it fails the same on Java 8 and 11
        digestAlgorithms.setSHA384Supported(false);
        digestAlgorithms.addDigestAlgorithmByName(DigestAlgorithms.JJNL_SHA384_ALGORITHM_NAME);

        HttpUtils httpUtils = new HttpUtils();
        httpUtils.setSupportedDigestAlgorithms(digestAlgorithms.getDigestAlgorithmUris());

        final HashMap <String,String> successResponseHeaders = new HashMap<String,String>();
        final List <String> errorResponseHeaders = new ArrayList<String>();

        final String selectedDigest = httpUtils.validateDigests(DigestAlgorithms.JJNL_SHA384_ALGORITHM_URI, digestAlgorithms.getDigestAlgorithmUris(), successResponseHeaders, errorResponseHeaders);
 
        assertTrue(successResponseHeaders.isEmpty());
        for (String entry : errorResponseHeaders) {
            assertEquals(HttpUtils.HDRS_UNSUPPORTED_DIGEST, entry);
        }
    }


    /**
     * Checks to ensure SHA512 will be supported during digest selection
     */
    @Test
    public void testValidateDigestWorksWithSHA512() {
        DigestAlgorithms digestAlgorithms = new DigestAlgorithms();
        digestAlgorithms.addDigestAlgorithmByName(DigestAlgorithms.JJNL_SHA512_ALGORITHM_NAME);

        HttpUtils httpUtils = new HttpUtils();
        httpUtils.setSupportedDigestAlgorithms(digestAlgorithms.getDigestAlgorithmUris());

        final HashMap <String,String> successResponseHeaders = new HashMap<String,String>();
        final List <String> errorResponseHeaders = new ArrayList<String>();

        final String selectedDigest = httpUtils.validateDigests(DigestAlgorithms.JJNL_SHA512_ALGORITHM_URI, digestAlgorithms.getDigestAlgorithmUris(), successResponseHeaders, errorResponseHeaders);

        assertEquals(DigestAlgorithms.JJNL_SHA512_ALGORITHM_URI, selectedDigest);
        for (Map.Entry<String, String> entry : successResponseHeaders.entrySet()) {
            assertEquals(HttpUtils.HDRS_DIGEST, entry.getKey());
            assertEquals(DigestAlgorithms.JJNL_SHA512_ALGORITHM_URI, entry.getValue());
        }
        assertTrue(errorResponseHeaders.isEmpty());
    }

    /**
     * Checks to ensure the digest defaults to SHA256 if the supported digests from
     * the config file constain sha256 and the http header digest value is missing
     */
    @Test
    public void testValidateDigestWorksDefaultsToSHA256() {
        DigestAlgorithms digestAlgorithms = new DigestAlgorithms();
        digestAlgorithms.addDigestAlgorithmByName(DigestAlgorithms.JJNL_SHA256_ALGORITHM_NAME);

        HttpUtils httpUtils = new HttpUtils();
        httpUtils.setSupportedDigestAlgorithms(digestAlgorithms.getDigestAlgorithmUris());

        final HashMap <String,String> successResponseHeaders = new HashMap<String,String>();
        final List <String> errorResponseHeaders = new ArrayList<String>();

        final String selectedDigest = httpUtils.validateDigests(null, digestAlgorithms.getDigestAlgorithmUris(), successResponseHeaders, errorResponseHeaders);

        assertEquals(DigestAlgorithms.JJNL_SHA256_ALGORITHM_URI, selectedDigest);
        for (Map.Entry<String, String> entry : successResponseHeaders.entrySet()) {
            assertEquals(HttpUtils.HDRS_DIGEST, entry.getKey());
            assertEquals(DigestAlgorithms.JJNL_SHA256_ALGORITHM_URI, entry.getValue());
        }
        assertTrue(errorResponseHeaders.isEmpty());
    }

    /**
     * Checks to ensure an error is reported if there are not mutually agreed on digests
     */
    @Test
    public void testValidateDigestUnsupportedDigest() {
        DigestAlgorithms digestAlgorithms = new DigestAlgorithms();
        digestAlgorithms.addDigestAlgorithmByName(DigestAlgorithms.JJNL_SHA256_ALGORITHM_NAME);

        final HashMap <String,String> successResponseHeaders = new HashMap<String,String>();
        final List <String> errorResponseHeaders = new ArrayList<String>();

        final String digests = DigestAlgorithms.JJNL_SHA512_ALGORITHM_URI;
        HttpUtils httpUtils = new HttpUtils();
        httpUtils.setSupportedDigestAlgorithms(digestAlgorithms.getDigestAlgorithmUris());
        final String selectedDigest = HttpUtils.validateDigests(digests, digestAlgorithms.getDigestAlgorithmUris(), successResponseHeaders, errorResponseHeaders);
        assertEquals(null, selectedDigest);
        assertTrue(successResponseHeaders.isEmpty());
        for (String entry : errorResponseHeaders) {
            assertEquals(HttpUtils.HDRS_UNSUPPORTED_DIGEST, entry);
        }
    }

    /**
     * Checks to ensure a NAK and report unsupported digest if junk in JAL-Accept-Digest header
     */
    @Test
    public void testValidateDigestFailsWithSHA256andJunkDigests() {
        DigestAlgorithms digestAlgorithms = new DigestAlgorithms();
        digestAlgorithms.addDigestAlgorithmByName(DigestAlgorithms.JJNL_SHA256_ALGORITHM_NAME);
        digestAlgorithms.addDigestAlgorithmByName(DigestAlgorithms.JJNL_SHA512_ALGORITHM_NAME);

        final String digests = "http://www.w3.org/2001/04/xmlenc#sha999, " + DigestAlgorithms.JJNL_SHA256_ALGORITHM_URI;

        HttpUtils httpUtils = new HttpUtils();
        httpUtils.setSupportedDigestAlgorithms(digestAlgorithms.getDigestAlgorithmUris());

        final HashMap <String,String> successResponseHeaders = new HashMap<String,String>();
        final List <String> errorResponseHeaders = new ArrayList<String>();

        final String selectedDigest = httpUtils.validateDigests(digests, digestAlgorithms.getDigestAlgorithmUris(), successResponseHeaders, errorResponseHeaders);

        assertEquals(null, selectedDigest);
        assertTrue(successResponseHeaders.isEmpty());
        for (String entry : errorResponseHeaders) {
            assertEquals(HttpUtils.HDRS_UNSUPPORTED_DIGEST, entry);
        }
    }

    /**
     * Checks to ensure inability to create an entry in the digest enum class
     * with an unsupported digest name
     */
    @Test
    public void testCantAddJunkDigests() {
        DigestAlgorithms digestAlgorithms = new DigestAlgorithms();
        boolean ret = digestAlgorithms.addDigestAlgorithmByName("sha999");
        assertEquals(false, ret);
    }

    /**
     * Checks that a ConfigurationException is thrown with an unsupported digest
     */
    @Test
    public void testUnsupportedDigest() {
        boolean configerr = false;
        boolean parseerr = false;
        boolean ioerr = false;

        final String path = "../jnl_test/target/test-classes/sampleHttpSubscriberUnsupportedDigest.json";
        try
        {
            HttpConfig.parse(path);
        }
        catch(ParseException pe)
        {
            parseerr = true;
        }
        catch(ConfigurationException ce)
        {
            configerr = true;
        }
        catch(IOException e)
        {
            ioerr = true;
        }

        assertEquals(true, configerr);
        assertEquals(false, parseerr);
        assertEquals(false, ioerr);
    }

    /**
     * Checks to ensure defaults to SHA256 if no config file entry for algorithms
     */
    @Test
    public void testNo_digestAlgorithms_Default256() {
        boolean configerr = false;
        boolean parseerr = false;
        boolean ioerr = false;

        final String path = "../jnl_test/target/test-classes/sampleHttpSubscriberNoEntry.json";
	try
        {
            HttpConfig.parse(path);
	}
	catch(ParseException pe)
        {
            parseerr = true;
	}
	catch(ConfigurationException ce)
        {
            configerr = true;
	}
	catch(IOException e)
        {
            ioerr = true;
	}

        assertEquals(false, configerr);
        assertEquals(false, parseerr);
        assertEquals(false, ioerr);

        DigestAlgorithmEnum digestAlg = DigestAlgorithmEnum.fromName(DigestAlgorithms.JJNL_SHA256_ALGORITHM_NAME);
        assertNotNull(digestAlg);
        String algName = digestAlg.toName();
        assertEquals(algName, DigestAlgorithms.JJNL_SHA256_ALGORITHM_NAME);
    }

    /**
     * Checks that a ConfigurationException is thrown with an empty digestAlrorithms entry
     */
    @Test
    public void testEmptyDigest() {
        boolean configerr = false;
        boolean parseerr = false;
        boolean ioerr = false;

        final String path = "../jnl_test/target/test-classes/sampleHttpSubscriberDigestEmpty.json";
        try
        {
            HttpConfig.parse(path);
        }
        catch(ParseException pe)
        {
            parseerr = true;
        }
        catch(ConfigurationException ce)
        {
            configerr = true;
        }
        catch(IOException e)
        {
            ioerr = true;
        }

        assertEquals(false, configerr);
        assertEquals(false, parseerr);
        assertEquals(false, ioerr);

        DigestAlgorithmEnum digestAlg = DigestAlgorithmEnum.fromName(DigestAlgorithms.JJNL_SHA256_ALGORITHM_NAME);
        assertNotNull(digestAlg);
        String algName = digestAlg.toName();
        assertEquals(algName, DigestAlgorithms.JJNL_SHA256_ALGORITHM_NAME);
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
    public void testValidateRecordTypeWorksForJournal() throws MissingMimeHeaderException, UnexpectedMimeValueException {
        final String recordType = "journal";
        List <String> errorResponseHeaders = new ArrayList<String>();
        HttpUtils httpUtils = new HttpUtils();
        final boolean returned = httpUtils.validateRecordType(recordType, RecordType.Journal, errorResponseHeaders);
        assertTrue(returned);
    }

    @Test
    public void testValidateRecordTypeWorksForAudit() throws MissingMimeHeaderException, UnexpectedMimeValueException {
        final String recordType = "audit";
        List <String> errorResponseHeaders = new ArrayList<String>();
        HttpUtils httpUtils = new HttpUtils();
        final boolean returned = httpUtils.validateRecordType(recordType, RecordType.Audit, errorResponseHeaders);
        assertTrue(returned);
    }

    @Test
    public void testValidateRecordTypeWorksForLog() throws MissingMimeHeaderException, UnexpectedMimeValueException {
        final String recordType = "log";
        List <String> errorResponseHeaders = new ArrayList<String>();
        HttpUtils httpUtils = new HttpUtils();
        final boolean returned = httpUtils.validateRecordType(recordType, RecordType.Log, errorResponseHeaders);
        assertTrue(returned);
    }

    public void testValidateRecordTypeFailsForEmptyRecordType() throws MissingMimeHeaderException, UnexpectedMimeValueException {
        final String recordType = "";
        List <String> errorResponseHeaders = new ArrayList<String>();
        HttpUtils httpUtils = new HttpUtils();
        final boolean returned = httpUtils.validateRecordType(recordType, RecordType.Journal, errorResponseHeaders);
        assertFalse(returned);

        final String recordType2 = null;
        List <String> errorResponseHeaders2 = new ArrayList<String>();
        final boolean returned2 = httpUtils.validateRecordType(recordType2, RecordType.Journal, errorResponseHeaders2);
        assertFalse(returned2);
    }

    public void testValidateRecordTypeFailsForInvalidRecordType() throws MissingMimeHeaderException, UnexpectedMimeValueException {
        final String recordType = "invalid";
        List <String> errorResponseHeaders = new ArrayList<String>();
        HttpUtils httpUtils = new HttpUtils();
        final boolean returned = httpUtils.validateRecordType(recordType, RecordType.Journal, errorResponseHeaders);
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
        HttpUtils httpUtils = new HttpUtils();
        final String selectedConfDigestChallenge = httpUtils.validateConfigureDigestChallenge(configureDigestChallenge, successResponseHeaders, errorResponseHeaders);
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
        HttpUtils httpUtils = new HttpUtils();
        List<String> allowedConfigureDigests = httpUtils.getAllowedConfigureDigests();
        allowedConfigureDigests.add("off");
        httpUtils.setAllowedConfigureDigests(allowedConfigureDigests);

        final String selectedConfDigestChallenge = httpUtils.validateConfigureDigestChallenge(configureDigestChallenge, successResponseHeaders, errorResponseHeaders);
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
        HttpUtils httpUtils = new HttpUtils();
        final String selectedConfDigestChallenge = httpUtils.validateConfigureDigestChallenge(configureDigestChallenge, successResponseHeaders, errorResponseHeaders);
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
        HttpUtils httpUtils = new HttpUtils();
        List<String> allowedConfigureDigests = Arrays.asList(configureDigestChallenge.split(","));
        httpUtils.setAllowedConfigureDigests(allowedConfigureDigests);
        final String selectedConfDigestChallenge = httpUtils.validateConfigureDigestChallenge(configureDigestChallenge, successResponseHeaders, errorResponseHeaders);
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
        HttpUtils httpUtils = new HttpUtils();
        final String selectedConfDigestChallenge = httpUtils.validateConfigureDigestChallenge(configureDigestChallenge, successResponseHeaders, errorResponseHeaders);
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
        HttpUtils httpUtils = new HttpUtils();
        final String selectedConfDigestChallenge = httpUtils.validateConfigureDigestChallenge(configureDigestChallenge, successResponseHeaders, errorResponseHeaders);
        assertEquals(null, selectedConfDigestChallenge);
        assertTrue(successResponseHeaders.isEmpty());
        for (String entry : errorResponseHeaders) {
            assertEquals("JAL-Unsupported-Configure-Digest-Challenge", entry);
        }
    }

    @Test
    public void testCheckForEmptyStringReturnsTrimmedString() {

        final String padded = "   string   ";
        final String returned = HttpUtils.checkForEmptyString(padded);
        assertEquals(padded.trim(), returned);
        assertEquals("string", returned);
    }

    @Test
    public void testMakeStringListWorks() {

        final List<String> words = new ArrayList<String>();
        words.add("word1");
        words.add("word2");
        words.add("word3");
        final String wordList = HttpUtils.makeStringList(words);
        assertEquals("word1, word2, word3", wordList);
    }

    @Test
    public void testMakeStringListHasComma() {

        final List<String> words = new ArrayList<String>();
        words.add("word1");
        words.add("word2");
        final String wordList = HttpUtils.makeStringList(words);
        assertTrue(wordList.contains(","));
    }

    @Test
    public void testMakeStringListOneWord() {

        final List<String> words = new ArrayList<String>();
        words.add("word1");
        final String wordList = HttpUtils.makeStringList(words);
        assertEquals("word1", wordList);
    }

    @Test
    public void testMakeStringListReturnsNullWhenNull() {

        final String wordList = HttpUtils.makeStringList(null);
        assertNull(wordList);
    }

    @Test
    public void testMakeStringListReturnsNullWhenBlank() {

        final List<String> words = new ArrayList<String>();
        final String wordList = HttpUtils.makeStringList(words);
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
        RecordType recordType = HttpUtils.getRecordType("invalid_record_type");
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
