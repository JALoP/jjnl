package com.tresys.jalop.jnl.impl.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.tresys.jalop.jnl.RecordType;

/**
 * Tests for common utility class.
 */
public class MessageProcessorTest {

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @BeforeClass
    public static void init() throws Exception {

    }

    @Test
    public void testProcessInitializeMessageNullRequestHeaders() throws IOException
    {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("requestHeaders is required");
        HashMap<String, String> successResponseHeaders = new HashMap<String, String>();
        List<String> errorMessages = new ArrayList<String>();
        boolean result = MessageProcessor.processInitializeMessage(null, RecordType.Audit, successResponseHeaders, errorMessages);
        assertEquals(false, result);
    }

    @Test
    public void testProcessInitializeMessageNullSuccessResponseHeaders() throws IOException
    {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("successResponseHeaders is required");
        TreeMap<String, String> requestHeaders = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
        List<String> errorMessages = new ArrayList<String>();
        boolean result = MessageProcessor.processInitializeMessage(requestHeaders, RecordType.Audit, null, errorMessages);
        assertEquals(false, result);
    }

    @Test
    public void testProcessInitializeMessageNullErrorMessages() throws IOException
    {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("errorMessages is required");
        HashMap<String, String> successResponseHeaders = new HashMap<String, String>();
        TreeMap<String, String> requestHeaders = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
        boolean result = MessageProcessor.processInitializeMessage(requestHeaders, RecordType.Audit, successResponseHeaders, null);
        assertEquals(false, result);
    }

    @Test
    public void testProcessInitializeMessageNullSupportedRecType() throws IOException
    {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("supportedRecType is required");
        HashMap<String, String> successResponseHeaders = new HashMap<String, String>();
        TreeMap<String, String> requestHeaders = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
        List<String> errorMessages = new ArrayList<String>();
        boolean result = MessageProcessor.processInitializeMessage(requestHeaders, null, successResponseHeaders, errorMessages);
        assertEquals(false, result);
    }

    @Test
    public void testProcessJALRecordMessageNullDigestResultParam()
    {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("digestResult is required");
        byte[] test = new byte[10];
        boolean result = MessageProcessor.processJALRecordMessage(new TreeMap<String, String>(), new ByteArrayInputStream(test), RecordType.Audit, null, null, new ArrayList<String>());
        assertEquals(false, result);
    }

    @Test
    public void testProcessJALRecordMessageNullErrorMessagesParam()
    {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("errorMessages is required");
        byte[] test = new byte[10];
        boolean result = MessageProcessor.processJALRecordMessage(new TreeMap<String, String>(), new ByteArrayInputStream(test), RecordType.Audit, null, new DigestResult(), null);
        assertEquals(false, result);
    }

    @Test
    public void testProcessJALRecordMessageNullRequestHeadersParam()
    {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("requestHeaders is required");
        byte[] test = new byte[10];
        boolean result = MessageProcessor.processJALRecordMessage(null, new ByteArrayInputStream(test), RecordType.Audit, null, new DigestResult(), new ArrayList<String>());
        assertEquals(false, result);
    }

    @Test
    public void testProcessJALRecordMessageNullRequestInputStreamParam()
    {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("requestInputStream is required");
        boolean result = MessageProcessor.processJALRecordMessage(new TreeMap<String, String>(), null, RecordType.Audit, null, new DigestResult(), new ArrayList<String>());
        assertEquals(false, result);
    }

    @Test
    public void testProcessJALRecordMessageNullSupportedRecordTypeParam()
    {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("supportedRecType is required");
        byte[] test = new byte[10];
        boolean result = MessageProcessor.processJALRecordMessage(new TreeMap<String, String>(), new ByteArrayInputStream(test), null, null, new DigestResult(), new ArrayList<String>());
        assertEquals(false, result);
    }

    public void testProcessDigestResponseMessageNullErrorMessagesParam()
    {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("errorMessages is required");
        boolean result = MessageProcessor.processDigestResponseMessage(new TreeMap<String, String>(), null, new DigestResult(), null);
        assertEquals(false, result);
    }

    @Test
    public void testProcessDigestResponseMessageNullRequestHeadersParam()
    {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("requestHeaders is required");
        boolean result = MessageProcessor.processDigestResponseMessage(null, null, new DigestResult(), new ArrayList<String>());
        assertEquals(false, result);
    }

    @Test
    public void testProcessDigestResponseMessageEmptySessionId()
    {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("sess is required");
        boolean result = MessageProcessor.processDigestResponseMessage(new TreeMap<String, String>(), null, new DigestResult(), new ArrayList<String>());
        assertEquals(false, result);
    }

    @Test
    public void testProcessDigestResponseMessageNullDigestResult()
    {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("digestResult is required");
        boolean result = MessageProcessor.processDigestResponseMessage(new TreeMap<String, String>(), null, null, new ArrayList<String>());
        assertEquals(false, result);
    }

    @Test
    public void testProcessJournalMissingMessageNullRequestHeadersParam()
    {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("requestHeaders is required");
        boolean result = MessageProcessor.processJournalMissingMessage(null, null, null, new DigestResult(), new ArrayList<String>());
        assertEquals(false, result);
    }

    @Test
    public void testProcessJournalMissingEmptyRecordType()
    {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("supportedRecType is required");
        boolean result = MessageProcessor.processJournalMissingMessage(new TreeMap<String, String>(), null, null, new DigestResult(), new ArrayList<String>());
        assertEquals(false, result);
    }

    @Test
    public void testProcessJournalMissingEmptyDigestResult()
    {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("digestResult is required");
        boolean result = MessageProcessor.processJournalMissingMessage(new TreeMap<String, String>(), RecordType.Journal, null, null, new ArrayList<String>());
        assertEquals(false, result);
    }

    @Test
    public void testProcessJournalMissingEmptySession()
    {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("sess is required");
        boolean result = MessageProcessor.processJournalMissingMessage(new TreeMap<String, String>(), RecordType.Journal, null, new DigestResult(), new ArrayList<String>());
        assertEquals(false, result);
    }

    @Test
    public void testProcessJournalMissingEmptyErrorMessage()
    {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("errorMessages is required");
        boolean result = MessageProcessor.processJournalMissingMessage(new TreeMap<String, String>(), RecordType.Journal, null, new DigestResult(), null);
        assertEquals(false, result);
    }

    @Test
    public void testSetJournalResumeMessageEmptyNounce()
    {
        HashMap<String, String> successHeaders = new HashMap<String, String>();
        List<String> errorHeaders = new ArrayList<String>();
        boolean result = MessageProcessor.setJournalResumeMessage(null, 0, successHeaders, errorHeaders);
        assertEquals(null, successHeaders.get(HttpUtils.HDRS_NONCE));
        assertEquals("0", successHeaders.get(HttpUtils.HDRS_JOURNAL_OFFSET));
        assertEquals(0, errorHeaders.size());
        assertTrue(result);
    }

    @Test
    public void testSetJournalResumeMessageInvalidJalOffset()
    {
        HashMap<String, String> headers = new HashMap<String, String>();
        List<String> errorHeaders = new ArrayList<String>();
        boolean result = MessageProcessor.setJournalResumeMessage("test", -1, headers, errorHeaders);
        assertEquals(false, result);
        assertEquals(0, headers.size());
        assertEquals(true, errorHeaders.contains(HttpUtils.HDRS_INVALID_JOURNAL_OFFSET));
    }
}
