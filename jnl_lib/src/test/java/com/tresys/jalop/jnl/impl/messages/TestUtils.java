/*
 * Source code in 3rd-party is licensed and owned by their respective
 * copyright holders.
 *
 * All other source code is copyright Tresys Technology and licensed as below.
 *
 * Copyright (c) 2012,2014 Tresys Technology LLC, Columbia, Maryland, USA
 *
 * This software was developed by Tresys Technology LLC
 * with U.S. Government sponsorship.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tresys.jalop.jnl.impl.messages;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.soap.MimeHeaders;

import mockit.*;

import org.beepcore.beep.core.InputDataStream;
import org.beepcore.beep.core.InputDataStreamAdapter;
import org.beepcore.beep.core.OutputDataStream;
import org.beepcore.beep.util.BufferSegment;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.tresys.jalop.jnl.ConnectionHandler.ConnectError;
import com.tresys.jalop.jnl.DigestStatus;
import com.tresys.jalop.jnl.Mode;
import com.tresys.jalop.jnl.RecordType;
import com.tresys.jalop.jnl.Role;
import com.tresys.jalop.jnl.exceptions.MissingMimeHeaderException;
import com.tresys.jalop.jnl.exceptions.UnexpectedMimeValueException;

/**
 * Tests for common utility class.
 */
public class TestUtils {

	protected InputDataStream data;
	private static Field odsMimeHeaders;

    @BeforeClass
	public static void setUpBeforeClass() throws SecurityException, NoSuchFieldException {
	    TestUtils.odsMimeHeaders = OutputDataStream.class.getDeclaredField("mimeHeaders");
	    odsMimeHeaders.setAccessible(true);
	}

	@Before
	public void setUp() throws Exception {

		final Constructor<InputDataStream> constructor = InputDataStream.class
				.getDeclaredConstructor();
		constructor.setAccessible(true);

		data = constructor.newInstance();
	}

	private static String getMimeHeader(final OutputDataStream ods, final String headerName) throws IllegalArgumentException, IllegalAccessException {
	    final org.beepcore.beep.core.MimeHeaders headers = (org.beepcore.beep.core.MimeHeaders) odsMimeHeaders.get(ods);
	    return headers.getHeaderValue(headerName);
    }

	public void createDataStream(final org.beepcore.beep.core.MimeHeaders headers)
			throws Exception {

		final Method addMethod = InputDataStream.class.getDeclaredMethod("add",
				BufferSegment.class);
		addMethod.setAccessible(true);
		addMethod.invoke(data, headers.getBufferSegment());

		final Method completeMethod = InputDataStream.class
				.getDeclaredMethod("setComplete");
		completeMethod.setAccessible(true);
		completeMethod.invoke(data);
	}

	public void createDataStream(final org.beepcore.beep.core.MimeHeaders headers, final String digests)
		throws Exception {

		final Method addMethod = InputDataStream.class.getDeclaredMethod("add",
				BufferSegment.class);
		addMethod.setAccessible(true);
		addMethod.invoke(data, headers.getBufferSegment());
		addMethod.invoke(data, new BufferSegment(digests.getBytes("us-ascii")));

		final Method completeMethod = InputDataStream.class
				.getDeclaredMethod("setComplete");
		completeMethod.setAccessible(true);
		completeMethod.invoke(data);
	}

	@Test
	public void testCreateInitAckMessageWorks() throws SecurityException,
			NoSuchFieldException, IllegalArgumentException,
			IllegalAccessException {
		final OutputDataStream ods = Utils.createInitAckMessage(Utils.DGST_SHA256,
				Utils.BINARY);
		assertTrue(ods.isComplete());

		final Field headers = ods.getClass().getDeclaredField("mimeHeaders");
		headers.setAccessible(true);

		final org.beepcore.beep.core.MimeHeaders mimeHeaders = (org.beepcore.beep.core.MimeHeaders) headers
				.get(ods);

		assertEquals(mimeHeaders.getHeaderValue(Utils.HDRS_MESSAGE),
				Utils.MSG_INIT_ACK);
		assertEquals(mimeHeaders.getHeaderValue(Utils.HDRS_DIGEST),
				Utils.DGST_SHA256);
		assertEquals(mimeHeaders.getHeaderValue(Utils.HDRS_ENCODING),
				Utils.BINARY);
	}

	@Test
	public void testCheckForEmptyStringReturnsTrimmedString() {

		final String padded = "   string   ";
		final String returned = Utils.checkForEmptyString(padded, "padded string");
		assertEquals(padded.trim(), returned);
		assertEquals(returned, "string");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCheckForEmptyStringThrowsExceptionWhenNull()
			throws Exception {

		Utils.checkForEmptyString(null, null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCheckForEmptyStringThrowsExceptionWhenBlank()
			throws Exception {

		Utils.checkForEmptyString("", null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCheckForEmptyStringThrowsExceptionWithOnlyWhiteSpace()
			throws Exception {

		Utils.checkForEmptyString("   ", null);
	}

	@Test
	public void testMakeStringListWorks() {

		final List<String> words = new ArrayList<String>();
		words.add("word1");
		words.add("word2");
		words.add("word3");
		final String wordList = Utils.makeStringList(words, "listname");
		assertEquals(wordList, "word1, word2, word3");
	}

	@Test
	public void testMakeStringListHasComma() {

		final List<String> words = new ArrayList<String>();
		words.add("word1");
		words.add("word2");
		final String wordList = Utils.makeStringList(words, "listname");
		assertTrue(wordList.contains(","));
	}

	@Test
	public void testMakeStringListOneWord() {

		final List<String> words = new ArrayList<String>();
		words.add("word1");
		final String wordList = Utils.makeStringList(words, "listname");
		assertEquals(wordList, "word1");
	}

	@Test
	public void testMakeStringListReturnsNullWhenNull() {

		final String wordList = Utils.makeStringList(null, "listname");
		assertNull(wordList);
	}

	@Test
	public void testMakeStringListReturnsNullWhenBlank() {

		final List<String> words = new ArrayList<String>();
		final String wordList = Utils.makeStringList(words, "listname");
		assertNull(wordList);
	}

	@Test
	public void testProcessInitLiveMessageWorks() throws Exception {

		final org.beepcore.beep.core.MimeHeaders headers = new org.beepcore.beep.core.MimeHeaders(
				Utils.CT_JALOP,
				org.beepcore.beep.core.MimeHeaders.DEFAULT_CONTENT_TRANSFER_ENCODING);

		headers.setHeader(Utils.HDRS_MESSAGE, Utils.MSG_INIT);
		headers.setHeader(Utils.HDRS_ACCEPT_ENCODING, Utils.BINARY);
		headers.setHeader(Utils.HDRS_ACCEPT_DIGEST, Utils.DGST_SHA256);
		headers.setHeader(Utils.HDRS_MODE, Utils.MSG_SUBSCRIBE_LIVE);
		headers.setHeader(Utils.HDRS_DATA_CLASS, Utils.LOG);

		createDataStream(headers);

		final InputDataStreamAdapter ids = data.getInputStream();
		final InitMessage msg = Utils.processInitMessage(ids);

		assertEquals(msg.getAcceptDigests(), Arrays.asList(Utils.DGST_SHA256));
		assertEquals(msg.getRole(), Role.Subscriber);
		assertEquals(msg.getMode(), Mode.Live);
		assertEquals(msg.getAcceptEncodings(), Arrays.asList(Utils.BINARY));
		assertEquals(msg.getRecordType(), RecordType.Log);
	}

	@Test
	public void testProcessInitArchiveMessageWorks() throws Exception {

		final org.beepcore.beep.core.MimeHeaders headers = new org.beepcore.beep.core.MimeHeaders(
				Utils.CT_JALOP,
				org.beepcore.beep.core.MimeHeaders.DEFAULT_CONTENT_TRANSFER_ENCODING);

		headers.setHeader(Utils.HDRS_MESSAGE, Utils.MSG_INIT);
		headers.setHeader(Utils.HDRS_ACCEPT_ENCODING, Utils.BINARY);
		headers.setHeader(Utils.HDRS_ACCEPT_DIGEST, Utils.DGST_SHA256);
		headers.setHeader(Utils.HDRS_MODE, Utils.MSG_SUBSCRIBE_ARCHIVE);
		headers.setHeader(Utils.HDRS_DATA_CLASS, Utils.LOG);

		createDataStream(headers);

		final InputDataStreamAdapter ids = data.getInputStream();
		final InitMessage msg = Utils.processInitMessage(ids);

		assertEquals(msg.getAcceptDigests(), Arrays.asList(Utils.DGST_SHA256));
		assertEquals(msg.getRole(), Role.Subscriber);
		assertEquals(msg.getMode(), Mode.Archive);
		assertEquals(msg.getAcceptEncodings(), Arrays.asList(Utils.BINARY));
		assertEquals(msg.getRecordType(), RecordType.Log);
	}
	@Test(expected = MissingMimeHeaderException.class)
	public void testProcessInitMessageThrowsExceptionWithNoMode()
			throws Exception {

		final org.beepcore.beep.core.MimeHeaders headers = new org.beepcore.beep.core.MimeHeaders(
				Utils.CT_JALOP,
				org.beepcore.beep.core.MimeHeaders.DEFAULT_CONTENT_TRANSFER_ENCODING);

		headers.setHeader(Utils.HDRS_MESSAGE, Utils.MSG_INIT);
		headers.setHeader(Utils.HDRS_DATA_CLASS, Utils.LOG);
		createDataStream(headers);
		final InputDataStreamAdapter ids = data.getInputStream();
		Utils.processInitMessage(ids);
	}

	@Test
	public void testProcessInitMessageSubscriberArchive() throws Exception {

		final org.beepcore.beep.core.MimeHeaders headers = new org.beepcore.beep.core.MimeHeaders(
				Utils.CT_JALOP,
				org.beepcore.beep.core.MimeHeaders.DEFAULT_CONTENT_TRANSFER_ENCODING);

		headers.setHeader(Utils.HDRS_MESSAGE, Utils.MSG_INIT);
		headers.setHeader(Utils.HDRS_MODE, Utils.MSG_SUBSCRIBE_ARCHIVE);
		headers.setHeader(Utils.HDRS_DATA_CLASS, Utils.LOG);

		createDataStream(headers);

		final InputDataStreamAdapter ids = data.getInputStream();

		final InitMessage msg = Utils.processInitMessage(ids);
		assertEquals(msg.getRole(), Role.Subscriber);
		assertEquals(msg.getMode(), Mode.Archive);
	}

	public void testProcessInitMessageSubscriberLive() throws Exception {

		final org.beepcore.beep.core.MimeHeaders headers = new org.beepcore.beep.core.MimeHeaders(
				Utils.CT_JALOP,
				org.beepcore.beep.core.MimeHeaders.DEFAULT_CONTENT_TRANSFER_ENCODING);

		headers.setHeader(Utils.HDRS_MESSAGE, Utils.MSG_INIT);
		headers.setHeader(Utils.HDRS_MODE, Utils.MSG_SUBSCRIBE_LIVE);
		headers.setHeader(Utils.HDRS_DATA_CLASS, Utils.LOG);

		createDataStream(headers);

		final InputDataStreamAdapter ids = data.getInputStream();

		final InitMessage msg = Utils.processInitMessage(ids);
		assertEquals(msg.getRole(), Role.Subscriber);
		assertEquals(msg.getMode(), Mode.Live);
	}

	@Test
	public void testProcessInitMessagePublisherArchive() throws Exception {

		final org.beepcore.beep.core.MimeHeaders headers = new org.beepcore.beep.core.MimeHeaders(
				Utils.CT_JALOP,
				org.beepcore.beep.core.MimeHeaders.DEFAULT_CONTENT_TRANSFER_ENCODING);

		headers.setHeader(Utils.HDRS_MESSAGE, Utils.MSG_INIT);
		headers.setHeader(Utils.HDRS_MODE, Utils.MSG_PUBLISH_ARCHIVE);
		headers.setHeader(Utils.HDRS_DATA_CLASS, Utils.LOG);

		createDataStream(headers);

		final InputDataStreamAdapter ids = data.getInputStream();

		final InitMessage msg = Utils.processInitMessage(ids);
		assertEquals(msg.getRole(), Role.Publisher);
		assertEquals(msg.getMode(), Mode.Archive);
	}

	@Test
	public void testProcessInitMessagePublisherLive() throws Exception {

		final org.beepcore.beep.core.MimeHeaders headers = new org.beepcore.beep.core.MimeHeaders(
				Utils.CT_JALOP,
				org.beepcore.beep.core.MimeHeaders.DEFAULT_CONTENT_TRANSFER_ENCODING);

		headers.setHeader(Utils.HDRS_MESSAGE, Utils.MSG_INIT);
		headers.setHeader(Utils.HDRS_MODE, Utils.MSG_PUBLISH_LIVE);
		headers.setHeader(Utils.HDRS_DATA_CLASS, Utils.LOG);

		createDataStream(headers);

		final InputDataStreamAdapter ids = data.getInputStream();

		final InitMessage msg = Utils.processInitMessage(ids);
		assertEquals(Role.Publisher, msg.getRole());
		assertEquals(Mode.Live, msg.getMode());
	}

	@Test(expected = UnexpectedMimeValueException.class)
	public void testProcessInitMessageThrowsExceptionWithBadMode()
			throws Exception {

		final org.beepcore.beep.core.MimeHeaders headers = new org.beepcore.beep.core.MimeHeaders(
				Utils.CT_JALOP,
				org.beepcore.beep.core.MimeHeaders.DEFAULT_CONTENT_TRANSFER_ENCODING);

		headers.setHeader(Utils.HDRS_MESSAGE, Utils.MSG_INIT);
		headers.setHeader(Utils.HDRS_MODE, "bad");
		headers.setHeader(Utils.HDRS_DATA_CLASS, Utils.LOG);
		createDataStream(headers);
		final InputDataStreamAdapter ids = data.getInputStream();
		Utils.processInitMessage(ids);
	}

	@Test(expected = MissingMimeHeaderException.class)
	public void testProcessInitMessageFailsWithNoDataClass() throws Exception {

		final org.beepcore.beep.core.MimeHeaders headers = new org.beepcore.beep.core.MimeHeaders(
				Utils.CT_JALOP,
				org.beepcore.beep.core.MimeHeaders.DEFAULT_CONTENT_TRANSFER_ENCODING);

		headers.setHeader(Utils.HDRS_MESSAGE, Utils.MSG_INIT);
		headers.setHeader(Utils.HDRS_MODE, Utils.MSG_SUBSCRIBE_LIVE);
		createDataStream(headers);
		final InputDataStreamAdapter ids = data.getInputStream();
		Utils.processInitMessage(ids);
	}

	@Test
	public void testProcessInitMessageJournal() throws Exception {

		final org.beepcore.beep.core.MimeHeaders headers = new org.beepcore.beep.core.MimeHeaders(
				Utils.CT_JALOP,
				org.beepcore.beep.core.MimeHeaders.DEFAULT_CONTENT_TRANSFER_ENCODING);

		headers.setHeader(Utils.HDRS_MESSAGE, Utils.MSG_INIT);
		headers.setHeader(Utils.HDRS_MODE, Utils.MSG_PUBLISH_ARCHIVE);
		headers.setHeader(Utils.HDRS_DATA_CLASS, Utils.JOURNAL);

		createDataStream(headers);

		final InputDataStreamAdapter ids = data.getInputStream();

		final InitMessage msg = Utils.processInitMessage(ids);
		assertEquals(msg.getRecordType(), RecordType.Journal);
	}

	@Test
	public void testProcessInitMessageAudit() throws Exception {

		final org.beepcore.beep.core.MimeHeaders headers = new org.beepcore.beep.core.MimeHeaders(
				Utils.CT_JALOP,
				org.beepcore.beep.core.MimeHeaders.DEFAULT_CONTENT_TRANSFER_ENCODING);

		headers.setHeader(Utils.HDRS_MESSAGE, Utils.MSG_INIT);
		headers.setHeader(Utils.HDRS_MODE, Utils.MSG_PUBLISH_ARCHIVE);
		headers.setHeader(Utils.HDRS_DATA_CLASS, Utils.AUDIT);

		createDataStream(headers);

		final InputDataStreamAdapter ids = data.getInputStream();

		final InitMessage msg = Utils.processInitMessage(ids);
		assertEquals(msg.getRecordType(), RecordType.Audit);
	}

	@Test
	public void testProcessInitMessageLog() throws Exception {

		final org.beepcore.beep.core.MimeHeaders headers = new org.beepcore.beep.core.MimeHeaders(
				Utils.CT_JALOP,
				org.beepcore.beep.core.MimeHeaders.DEFAULT_CONTENT_TRANSFER_ENCODING);

		headers.setHeader(Utils.HDRS_MESSAGE, Utils.MSG_INIT);
		headers.setHeader(Utils.HDRS_MODE, Utils.MSG_PUBLISH_ARCHIVE);
		headers.setHeader(Utils.HDRS_DATA_CLASS, Utils.LOG);

		createDataStream(headers);

		final InputDataStreamAdapter ids = data.getInputStream();

		final InitMessage msg = Utils.processInitMessage(ids);
		assertEquals(msg.getRecordType(), RecordType.Log);
	}

	@Test(expected = UnexpectedMimeValueException.class)
	public void testProcessInitMessageThrowsExceptionWithBadDataClass()
			throws Exception {

		final org.beepcore.beep.core.MimeHeaders headers = new org.beepcore.beep.core.MimeHeaders(
				Utils.CT_JALOP,
				org.beepcore.beep.core.MimeHeaders.DEFAULT_CONTENT_TRANSFER_ENCODING);

		headers.setHeader(Utils.HDRS_MESSAGE, Utils.MSG_INIT);
		headers.setHeader(Utils.HDRS_MODE, Utils.MSG_PUBLISH_ARCHIVE);
		headers.setHeader(Utils.HDRS_DATA_CLASS, "bad");

		createDataStream(headers);

		final InputDataStreamAdapter ids = data.getInputStream();

		Utils.processInitMessage(ids);
	}

	@Test
	public void testProcessInitMessageSetsAgent() throws Exception {

		final org.beepcore.beep.core.MimeHeaders headers = new org.beepcore.beep.core.MimeHeaders(
				Utils.CT_JALOP,
				org.beepcore.beep.core.MimeHeaders.DEFAULT_CONTENT_TRANSFER_ENCODING);

		headers.setHeader(Utils.HDRS_MESSAGE, Utils.MSG_INIT);
		headers.setHeader(Utils.HDRS_MODE, Utils.MSG_PUBLISH_LIVE);
		headers.setHeader(Utils.HDRS_DATA_CLASS, Utils.LOG);
		headers.setHeader(Utils.HDRS_AGENT, "agent");

		createDataStream(headers);

		final InputDataStreamAdapter ids = data.getInputStream();

		final InitMessage msg = Utils.processInitMessage(ids);
		assertEquals(msg.getAgentString(), "agent");
	}

	@Test
	public void testProcessMessageCommonWorks() throws Exception {

		final org.beepcore.beep.core.MimeHeaders headers = new org.beepcore.beep.core.MimeHeaders(
				Utils.CT_JALOP,
				org.beepcore.beep.core.MimeHeaders.DEFAULT_CONTENT_TRANSFER_ENCODING);

		headers.setHeader(Utils.HDRS_MESSAGE, Utils.MSG_INIT);
		createDataStream(headers);
		final InputDataStreamAdapter ids = data.getInputStream();

		final MimeHeaders[] mimeHeaders = Utils.processMessageCommon(ids,
				Utils.MSG_INIT, Utils.HDRS_ACCEPT_ENCODING);

		assertEquals(mimeHeaders.length, 2);
	}

	@Test(expected = MissingMimeHeaderException.class)
	public void testProcessMessageCommonThrowsExceptionWithNoMessage()
			throws Exception {

		final org.beepcore.beep.core.MimeHeaders headers = new org.beepcore.beep.core.MimeHeaders(
				Utils.CT_JALOP,
				org.beepcore.beep.core.MimeHeaders.DEFAULT_CONTENT_TRANSFER_ENCODING);

		createDataStream(headers);
		final InputDataStreamAdapter ids = data.getInputStream();

		Utils.processMessageCommon(ids, Utils.MSG_INIT,
				Utils.HDRS_ACCEPT_ENCODING);
	}

	@Test(expected = UnexpectedMimeValueException.class)
	public void testProcessMessageCommonThrowsExceptionWithBadMessage()
			throws Exception {

		final org.beepcore.beep.core.MimeHeaders headers = new org.beepcore.beep.core.MimeHeaders(
				Utils.CT_JALOP,
				org.beepcore.beep.core.MimeHeaders.DEFAULT_CONTENT_TRANSFER_ENCODING);

		headers.setHeader(Utils.HDRS_MESSAGE, Utils.MSG_INIT);
		createDataStream(headers);
		final InputDataStreamAdapter ids = data.getInputStream();

		Utils.processMessageCommon(ids, Utils.MSG_AUDIT,
				Utils.HDRS_ACCEPT_ENCODING);
	}

	@Test
	public void testSplitHeadersWorks() throws Exception {

		final org.beepcore.beep.core.MimeHeaders headers = new org.beepcore.beep.core.MimeHeaders(
				Utils.CT_JALOP,
				org.beepcore.beep.core.MimeHeaders.DEFAULT_CONTENT_TRANSFER_ENCODING);

		headers.setHeader(Utils.HDRS_MODE, Utils.MSG_PUBLISH_LIVE);
		headers.setHeader(Utils.HDRS_DATA_CLASS, Utils.LOG);
		createDataStream(headers);
		final InputDataStreamAdapter ids = data.getInputStream();

		final MimeHeaders[] mimeHeaders = Utils.splitHeaders(ids, Utils.HDRS_MODE);
		assertEquals(mimeHeaders.length, 2);

		final MimeHeaders knownHeaders = mimeHeaders[0];
		final MimeHeaders unknownHeaders = mimeHeaders[1];

		assertTrue(knownHeaders.getHeader(Utils.HDRS_MODE) != null);
		assertTrue(unknownHeaders.getHeader(Utils.HDRS_DATA_CLASS) != null);
	}

	@Test
	public void testSplitHeadersAddsToKnown() throws Exception {

		final org.beepcore.beep.core.MimeHeaders headers = new org.beepcore.beep.core.MimeHeaders(
				Utils.CT_JALOP,
				org.beepcore.beep.core.MimeHeaders.DEFAULT_CONTENT_TRANSFER_ENCODING);

		headers.setHeader(Utils.HDRS_MODE, Utils.MSG_PUBLISH_ARCHIVE);
		createDataStream(headers);
		final InputDataStreamAdapter ids = data.getInputStream();

		final MimeHeaders[] mimeHeaders = Utils.splitHeaders(ids, Utils.HDRS_MODE);
		assertEquals(mimeHeaders.length, 2);

		final MimeHeaders knownHeaders = mimeHeaders[0];
		assertTrue(knownHeaders.getHeader(Utils.HDRS_MODE) != null);
	}

	@Test
	public void testSplitHeadersAddsToUnKnown() throws Exception {

		final org.beepcore.beep.core.MimeHeaders headers = new org.beepcore.beep.core.MimeHeaders(
				Utils.CT_JALOP,
				org.beepcore.beep.core.MimeHeaders.DEFAULT_CONTENT_TRANSFER_ENCODING);

		headers.setHeader(Utils.HDRS_DATA_CLASS, Utils.LOG);
		createDataStream(headers);
		final InputDataStreamAdapter ids = data.getInputStream();

		final MimeHeaders[] mimeHeaders = Utils.splitHeaders(ids, Utils.HDRS_MODE);
		assertEquals(mimeHeaders.length, 2);

		final MimeHeaders unknownHeaders = mimeHeaders[1];
		assertTrue(unknownHeaders.getHeader(Utils.HDRS_DATA_CLASS) != null);
	}

	@Test
	public void testCreateInitMessageWorks() throws SecurityException,
			NoSuchFieldException, IllegalArgumentException,
			IllegalAccessException {

		final OutputDataStream ods = Utils.createInitMessage(Role.Publisher,
				Mode.Live, RecordType.Log, Arrays.asList(Utils.BINARY),
				Arrays.asList(Utils.DGST_SHA256), "agent");
		assertTrue(ods.isComplete());

		final Field headers = ods.getClass().getDeclaredField("mimeHeaders");
		headers.setAccessible(true);

		final org.beepcore.beep.core.MimeHeaders mimeHeaders = (org.beepcore.beep.core.MimeHeaders) headers
				.get(ods);

		assertEquals(mimeHeaders.getHeaderValue(Utils.HDRS_MESSAGE),
				Utils.MSG_INIT);
		assertEquals(mimeHeaders.getHeaderValue(Utils.HDRS_ACCEPT_DIGEST),
				Utils.DGST_SHA256);
		assertEquals(mimeHeaders.getHeaderValue(Utils.HDRS_ACCEPT_ENCODING),
				Utils.BINARY);
		assertEquals(mimeHeaders.getHeaderValue(Utils.HDRS_MODE), Utils.MSG_PUBLISH_LIVE);
		assertEquals(mimeHeaders.getHeaderValue(Utils.HDRS_DATA_CLASS),
				Utils.LOG);
	}

	@Test
	public void testCreateInitMessageWorksWithSubscribeAudit()
			throws SecurityException, NoSuchFieldException,
			IllegalArgumentException, IllegalAccessException {

		final OutputDataStream ods = Utils.createInitMessage(Role.Subscriber,
				Mode.Live, RecordType.Audit, Arrays.asList(Utils.BINARY),
				Arrays.asList(Utils.DGST_SHA256), "agent");

		final Field headers = ods.getClass().getDeclaredField("mimeHeaders");
		headers.setAccessible(true);

		final org.beepcore.beep.core.MimeHeaders mimeHeaders = (org.beepcore.beep.core.MimeHeaders) headers
				.get(ods);

		assertEquals(mimeHeaders.getHeaderValue(Utils.HDRS_MODE),
				Utils.MSG_SUBSCRIBE_LIVE);
		assertEquals(mimeHeaders.getHeaderValue(Utils.HDRS_DATA_CLASS),
				Utils.AUDIT);
	}

	@Test
	public void testCreateInitMessageWorksWithJournal()
			throws SecurityException, NoSuchFieldException,
			IllegalArgumentException, IllegalAccessException {

		final OutputDataStream ods = Utils.createInitMessage(Role.Subscriber, Mode.Live,
				RecordType.Journal, Arrays.asList(Utils.BINARY),
				Arrays.asList(Utils.DGST_SHA256), "agent");

		final Field headers = ods.getClass().getDeclaredField("mimeHeaders");
		headers.setAccessible(true);

		final org.beepcore.beep.core.MimeHeaders mimeHeaders = (org.beepcore.beep.core.MimeHeaders) headers
				.get(ods);

		assertEquals(mimeHeaders.getHeaderValue(Utils.HDRS_DATA_CLASS),
				Utils.JOURNAL);
	}

	@Test
	public void testCreateInitMessageWorksWithNullEncoding()
			throws SecurityException, NoSuchFieldException,
			IllegalArgumentException, IllegalAccessException {

		final OutputDataStream ods = Utils
				.createInitMessage(Role.Publisher, Mode.Live, RecordType.Log, null,
						Arrays.asList(Utils.DGST_SHA256), "agent");

		final Field headers = ods.getClass().getDeclaredField("mimeHeaders");
		headers.setAccessible(true);

		final org.beepcore.beep.core.MimeHeaders mimeHeaders = (org.beepcore.beep.core.MimeHeaders) headers
				.get(ods);
		assertTrue(mimeHeaders.getHeaderValue(Utils.HDRS_ACCEPT_ENCODING) == null);
	}

	@Test
	public void testCreateInitMessageWorksWithNullDigest()
			throws SecurityException, NoSuchFieldException,
			IllegalArgumentException, IllegalAccessException {

		final OutputDataStream ods = Utils.createInitMessage(Role.Publisher, Mode.Live,
				RecordType.Log, Arrays.asList(Utils.BINARY), null, "agent");

		final Field headers = ods.getClass().getDeclaredField("mimeHeaders");
		headers.setAccessible(true);

		final org.beepcore.beep.core.MimeHeaders mimeHeaders = (org.beepcore.beep.core.MimeHeaders) headers
				.get(ods);
		assertTrue(mimeHeaders.getHeaderValue(Utils.HDRS_ACCEPT_DIGEST) == null);
	}

	@Test
	public void testCreateInitMessageWorksWithNoAgent()
			throws SecurityException, NoSuchFieldException,
			IllegalArgumentException, IllegalAccessException {

		final OutputDataStream ods = Utils.createInitMessage(Role.Publisher, Mode.Archive,
				RecordType.Log, Arrays.asList(Utils.BINARY),
				Arrays.asList(Utils.DGST_SHA256), null);

		final Field headers = ods.getClass().getDeclaredField("mimeHeaders");
		headers.setAccessible(true);

		final org.beepcore.beep.core.MimeHeaders mimeHeaders = (org.beepcore.beep.core.MimeHeaders) headers
				.get(ods);
		assertTrue(mimeHeaders.getHeaderValue(Utils.HDRS_AGENT) == null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCreateInitMessageThrowsExceptionWithNoRole()
			throws SecurityException, NoSuchFieldException,
			IllegalArgumentException, IllegalAccessException {

		Utils.createInitMessage(Role.Unset, Mode.Archive, RecordType.Journal,
				Arrays.asList(Utils.BINARY), Arrays.asList(Utils.DGST_SHA256),
				"agent");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCreateInitMessageThrowsExceptionWithNoRecordType()
			throws SecurityException, NoSuchFieldException,
			IllegalArgumentException, IllegalAccessException {

		Utils.createInitMessage(Role.Publisher, Mode.Live, RecordType.Unset,
				Arrays.asList(Utils.BINARY), Arrays.asList(Utils.DGST_SHA256),
				"agent");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCreateInitMessageThrowsExceptionWithUnsetMode()
			throws SecurityException, NoSuchFieldException,
			IllegalArgumentException, IllegalAccessException {

		Utils.createInitMessage(Role.Publisher, Mode.Unset, RecordType.Log,
				Arrays.asList(Utils.BINARY), Arrays.asList(Utils.DGST_SHA256),
				"agent");
	}

	@Test
	public void testCreateInitNackMessageWorks() throws SecurityException,
			NoSuchFieldException, IllegalArgumentException,
			IllegalAccessException {

		final List<ConnectError> connectErrors = new ArrayList<ConnectError>();
		connectErrors.add(ConnectError.UnauthorizedMode);
		connectErrors.add(ConnectError.UnsupportedDigest);
		connectErrors.add(ConnectError.UnsupportedEncoding);
		connectErrors.add(ConnectError.UnsupportedMode);
		connectErrors.add(ConnectError.UnsupportedVersion);
		final OutputDataStream ods = Utils.createInitNackMessage(connectErrors);
		assertTrue(ods.isComplete());

		final Field headers = ods.getClass().getDeclaredField("mimeHeaders");
		headers.setAccessible(true);

		final org.beepcore.beep.core.MimeHeaders mimeHeaders = (org.beepcore.beep.core.MimeHeaders) headers
				.get(ods);

		assertEquals(mimeHeaders.getHeaderValue(Utils.HDRS_UNAUTHORIZED_MODE),
				"");
		assertEquals(mimeHeaders.getHeaderValue(Utils.HDRS_UNSUPPORTED_DIGEST),
				"");
		assertEquals(
				mimeHeaders.getHeaderValue(Utils.HDRS_UNSUPPORTED_ENCODING), "");
		assertEquals(mimeHeaders.getHeaderValue(Utils.HDRS_UNSUPPORTED_MODE),
				"");
		assertEquals(
				mimeHeaders.getHeaderValue(Utils.HDRS_UNSUPPORTED_VERSION), "");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCreateInitNackMessageThrowsExceptionWithNullErrors()
			throws SecurityException, NoSuchFieldException,
			IllegalArgumentException, IllegalAccessException {

		Utils.createInitNackMessage(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCreateInitNackMessageThrowsExceptionWithBlankErrors()
			throws SecurityException, NoSuchFieldException,
			IllegalArgumentException, IllegalAccessException {

		Utils.createInitNackMessage(new ArrayList<ConnectError>());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCreateInitNackMessageThrowsExceptionWithAccept()
			throws SecurityException, NoSuchFieldException,
			IllegalArgumentException, IllegalAccessException {

		final List<ConnectError> connectErrors = new ArrayList<ConnectError>();
		connectErrors.add(ConnectError.Accept);
		Utils.createInitNackMessage(connectErrors);
	}

	@Test
	public void testProcessInitAckWorks() throws Exception {

		final org.beepcore.beep.core.MimeHeaders headers = new org.beepcore.beep.core.MimeHeaders(
				Utils.CT_JALOP,
				org.beepcore.beep.core.MimeHeaders.DEFAULT_CONTENT_TRANSFER_ENCODING);

		headers.setHeader(Utils.HDRS_MESSAGE, Utils.MSG_INIT_ACK);
		headers.setHeader(Utils.HDRS_ENCODING, Utils.BINARY);
		headers.setHeader(Utils.HDRS_DIGEST, Utils.DGST_SHA256);

		createDataStream(headers);

		final InputDataStreamAdapter ids = data.getInputStream();
		final InitAckMessage msg = Utils.processInitAck(ids);

		assertEquals(msg.getDigest(), Utils.DGST_SHA256);
		assertEquals(msg.getEncoding(), Utils.BINARY);
	}

	@Test(expected = MissingMimeHeaderException.class)
	public void testProcessInitAckThrowsExceptionWithNoDigest()
			throws Exception {

		final org.beepcore.beep.core.MimeHeaders headers = new org.beepcore.beep.core.MimeHeaders(
				Utils.CT_JALOP,
				org.beepcore.beep.core.MimeHeaders.DEFAULT_CONTENT_TRANSFER_ENCODING);

		headers.setHeader(Utils.HDRS_MESSAGE, Utils.MSG_INIT_ACK);
		headers.setHeader(Utils.HDRS_ENCODING, Utils.BINARY);

		createDataStream(headers);

		final InputDataStreamAdapter ids = data.getInputStream();
		Utils.processInitAck(ids);
	}

	@Test(expected = MissingMimeHeaderException.class)
	public void testProcessInitAckThrowsExceptionWithNoEncoding()
			throws Exception {

		final org.beepcore.beep.core.MimeHeaders headers = new org.beepcore.beep.core.MimeHeaders(
				Utils.CT_JALOP,
				org.beepcore.beep.core.MimeHeaders.DEFAULT_CONTENT_TRANSFER_ENCODING);

		headers.setHeader(Utils.HDRS_MESSAGE, Utils.MSG_INIT_ACK);
		headers.setHeader(Utils.HDRS_DIGEST, Utils.DGST_SHA256);

		createDataStream(headers);

		final InputDataStreamAdapter ids = data.getInputStream();
		Utils.processInitAck(ids);
	}

	@Test
	public void testProcessInitNackWorks() throws Exception {

		final org.beepcore.beep.core.MimeHeaders headers = new org.beepcore.beep.core.MimeHeaders(
				Utils.CT_JALOP,
				org.beepcore.beep.core.MimeHeaders.DEFAULT_CONTENT_TRANSFER_ENCODING);

		headers.setHeader(Utils.HDRS_MESSAGE, Utils.MSG_INIT_NACK);
		headers.setHeader(Utils.HDRS_UNSUPPORTED_VERSION, "this");
		headers.setHeader(Utils.HDRS_UNSUPPORTED_MODE, "this");
		headers.setHeader(Utils.HDRS_UNAUTHORIZED_MODE, "this");
		headers.setHeader(Utils.HDRS_UNSUPPORTED_ENCODING, "this");
		headers.setHeader(Utils.HDRS_UNSUPPORTED_DIGEST, "this");

		createDataStream(headers);

		final InputDataStreamAdapter ids = data.getInputStream();
		final InitNackMessage msg = Utils.processInitNack(ids);

		assertTrue(msg.getErrorList().contains(ConnectError.UnsupportedVersion));
		assertTrue(msg.getErrorList().contains(ConnectError.UnsupportedMode));
		assertTrue(msg.getErrorList().contains(ConnectError.UnauthorizedMode));
		assertTrue(msg.getErrorList()
				.contains(ConnectError.UnsupportedEncoding));
		assertTrue(msg.getErrorList().contains(ConnectError.UnsupportedDigest));
	}

	@Test(expected = MissingMimeHeaderException.class)
	public void testProcessInitNackThrowsExceptionWithNoErrors()
			throws Exception {

		final org.beepcore.beep.core.MimeHeaders headers = new org.beepcore.beep.core.MimeHeaders(
				Utils.CT_JALOP,
				org.beepcore.beep.core.MimeHeaders.DEFAULT_CONTENT_TRANSFER_ENCODING);

		headers.setHeader(Utils.HDRS_MESSAGE, Utils.MSG_INIT_NACK);

		createDataStream(headers);

		final InputDataStreamAdapter ids = data.getInputStream();
		final InitNackMessage msg = Utils.processInitNack(ids);

		assertTrue(msg.getErrorList().isEmpty());
	}

	@Test
	public void testCreateSubscribeMessageWorks() throws SecurityException,
			NoSuchFieldException, IllegalArgumentException,
			IllegalAccessException {

		final OutputDataStream ods = Utils.createSubscribeMessage();
		assertTrue(ods.isComplete());

		final Field headers = ods.getClass().getDeclaredField("mimeHeaders");
		headers.setAccessible(true);

		final org.beepcore.beep.core.MimeHeaders mimeHeaders = (org.beepcore.beep.core.MimeHeaders) headers
				.get(ods);

		assertEquals(mimeHeaders.getHeaderValue(Utils.HDRS_MESSAGE),
				Utils.MSG_SUBSCRIBE);
	}

	@Test
	public void testProcessSubscribeWorks() throws Exception {

		final org.beepcore.beep.core.MimeHeaders headers = new org.beepcore.beep.core.MimeHeaders(
				Utils.CT_JALOP,
				org.beepcore.beep.core.MimeHeaders.DEFAULT_CONTENT_TRANSFER_ENCODING);

		headers.setHeader(Utils.HDRS_MESSAGE, Utils.MSG_SUBSCRIBE);
		headers.setHeader(Utils.HDRS_NONCE, "0");

		createDataStream(headers);

		final InputDataStreamAdapter ids = data.getInputStream();
		final SubscribeMessage msg = Utils.processSubscribe(ids);

		assertEquals(msg.getNonce(), "0");
	}

	@Test
	public void testCreateJournalResumeMessageWorks() throws SecurityException,
			NoSuchFieldException, IllegalArgumentException,
			IllegalAccessException {

		final OutputDataStream ods = Utils.createJournalResumeMessage("0", 10);
		assertTrue(ods.isComplete());

		final Field headers = ods.getClass().getDeclaredField("mimeHeaders");
		headers.setAccessible(true);

		final org.beepcore.beep.core.MimeHeaders mimeHeaders = (org.beepcore.beep.core.MimeHeaders) headers
				.get(ods);

		assertEquals(mimeHeaders.getHeaderValue(Utils.HDRS_MESSAGE),
				Utils.MSG_JOURNAL_RESUME);
		assertEquals(mimeHeaders.getHeaderValue(Utils.HDRS_NONCE), "0");
		assertEquals(mimeHeaders.getHeaderValue(Utils.HDRS_JOURNAL_OFFSET),
				"10");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCreateJournalResumeMessageThrowsExceptionNullNonce()
			throws SecurityException, NoSuchFieldException,
			IllegalArgumentException, IllegalAccessException {

		Utils.createJournalResumeMessage(null, 10);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCreateJournalResumeMessageThrowsExceptionBlankNonce()
			throws SecurityException, NoSuchFieldException,
			IllegalArgumentException, IllegalAccessException {

		Utils.createJournalResumeMessage("", 10);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCreateJournalResumeMessageThrowsExceptionNegativeOffset()
			throws SecurityException, NoSuchFieldException,
			IllegalArgumentException, IllegalAccessException {

		Utils.createJournalResumeMessage("1", -1);
	}

	@Test
	public void testProcessJournalResumeWorks() throws Exception {

		final org.beepcore.beep.core.MimeHeaders headers = new org.beepcore.beep.core.MimeHeaders(
				Utils.CT_JALOP,
				org.beepcore.beep.core.MimeHeaders.DEFAULT_CONTENT_TRANSFER_ENCODING);

		headers.setHeader(Utils.HDRS_MESSAGE, Utils.MSG_JOURNAL_RESUME);
		headers.setHeader(Utils.HDRS_NONCE, "1");
		headers.setHeader(Utils.HDRS_JOURNAL_OFFSET, "10");

		createDataStream(headers);

		final InputDataStreamAdapter ids = data.getInputStream();
		final JournalResumeMessage jrm = Utils.processJournalResume(ids);

		assertEquals(jrm.getNonce(), "1");
		assertEquals(jrm.getOffset(), 10);
	}

	@Test(expected = MissingMimeHeaderException.class)
	public void testProcessJournalResumeThrowsExceptionWithNoNonce()
			throws Exception {

		final org.beepcore.beep.core.MimeHeaders headers = new org.beepcore.beep.core.MimeHeaders(
				Utils.CT_JALOP,
				org.beepcore.beep.core.MimeHeaders.DEFAULT_CONTENT_TRANSFER_ENCODING);

		headers.setHeader(Utils.HDRS_MESSAGE, Utils.MSG_JOURNAL_RESUME);
		headers.setHeader(Utils.HDRS_JOURNAL_OFFSET, "10");

		createDataStream(headers);

		final InputDataStreamAdapter ids = data.getInputStream();
		Utils.processJournalResume(ids);
	}

	@Test(expected = UnexpectedMimeValueException.class)
	public void testProcessJournalResumeThrowsExceptionWithBlankNonce()
			throws Exception {

		final org.beepcore.beep.core.MimeHeaders headers = new org.beepcore.beep.core.MimeHeaders(
				Utils.CT_JALOP,
				org.beepcore.beep.core.MimeHeaders.DEFAULT_CONTENT_TRANSFER_ENCODING);

		headers.setHeader(Utils.HDRS_MESSAGE, Utils.MSG_JOURNAL_RESUME);
		headers.setHeader(Utils.HDRS_NONCE, "");
		headers.setHeader(Utils.HDRS_JOURNAL_OFFSET, "10");

		createDataStream(headers);

		final InputDataStreamAdapter ids = data.getInputStream();
		Utils.processJournalResume(ids);
	}

	@Test(expected = MissingMimeHeaderException.class)
	public void testProcessJournalResumeThrowsExceptionWithNoOffset()
			throws Exception {

		final org.beepcore.beep.core.MimeHeaders headers = new org.beepcore.beep.core.MimeHeaders(
				Utils.CT_JALOP,
				org.beepcore.beep.core.MimeHeaders.DEFAULT_CONTENT_TRANSFER_ENCODING);

		headers.setHeader(Utils.HDRS_MESSAGE, Utils.MSG_JOURNAL_RESUME);
		headers.setHeader(Utils.HDRS_NONCE, "1");

		createDataStream(headers);

		final InputDataStreamAdapter ids = data.getInputStream();
		Utils.processJournalResume(ids);
	}

	@Test(expected = UnexpectedMimeValueException.class)
	public void testProcessJournalResumeThrowsExceptionWithBlankOffset()
			throws Exception {

		final org.beepcore.beep.core.MimeHeaders headers = new org.beepcore.beep.core.MimeHeaders(
				Utils.CT_JALOP,
				org.beepcore.beep.core.MimeHeaders.DEFAULT_CONTENT_TRANSFER_ENCODING);

		headers.setHeader(Utils.HDRS_MESSAGE, Utils.MSG_JOURNAL_RESUME);
		headers.setHeader(Utils.HDRS_NONCE, "1");
		headers.setHeader(Utils.HDRS_JOURNAL_OFFSET, "");

		createDataStream(headers);

		final InputDataStreamAdapter ids = data.getInputStream();
		Utils.processJournalResume(ids);
	}

	@Test(expected = UnexpectedMimeValueException.class)
	public void testProcessJournalResumeThrowsExceptionWithBadOffset()
			throws Exception {

		final org.beepcore.beep.core.MimeHeaders headers = new org.beepcore.beep.core.MimeHeaders(
				Utils.CT_JALOP,
				org.beepcore.beep.core.MimeHeaders.DEFAULT_CONTENT_TRANSFER_ENCODING);

		headers.setHeader(Utils.HDRS_MESSAGE, Utils.MSG_JOURNAL_RESUME);
		headers.setHeader(Utils.HDRS_NONCE, "1");
		headers.setHeader(Utils.HDRS_JOURNAL_OFFSET, "bad");

		createDataStream(headers);

		final InputDataStreamAdapter ids = data.getInputStream();
		Utils.processJournalResume(ids);
	}

	@Test(expected = UnexpectedMimeValueException.class)
	public void testProcessJournalResumeThrowsExceptionWithNegativeOffset()
			throws Exception {

		final org.beepcore.beep.core.MimeHeaders headers = new org.beepcore.beep.core.MimeHeaders(
				Utils.CT_JALOP,
				org.beepcore.beep.core.MimeHeaders.DEFAULT_CONTENT_TRANSFER_ENCODING);

		headers.setHeader(Utils.HDRS_MESSAGE, Utils.MSG_JOURNAL_RESUME);
		headers.setHeader(Utils.HDRS_NONCE, "1");
		headers.setHeader(Utils.HDRS_JOURNAL_OFFSET, "-1");

		createDataStream(headers);

		final InputDataStreamAdapter ids = data.getInputStream();
		Utils.processJournalResume(ids);
	}
	@Test
	public void testCreateSyncWorks() throws IllegalAccessException {
	    final OutputDataStream syncMsg = Utils.createSyncMessage("1234");
	    assertNotNull(syncMsg);
	    assertEquals("1234", getMimeHeader(syncMsg, Utils.HDRS_NONCE));
	    assertEquals(Utils.CT_JALOP, getMimeHeader(syncMsg, org.beepcore.beep.core.MimeHeaders.CONTENT_TYPE));
        assertEquals(org.beepcore.beep.core.MimeHeaders.DEFAULT_CONTENT_TRANSFER_ENCODING, getMimeHeader(syncMsg, org.beepcore.beep.core.MimeHeaders.CONTENT_TRANSFER_ENCODING));
	}

	@Test (expected = IllegalArgumentException.class)
	    public final void testCreateSyncThrowsExceptionForNullNonce() {
        final OutputDataStream syncMsg = Utils.createSyncMessage(null);
    }

	@Test (expected = IllegalArgumentException.class)
    public final void testCreateSyncThrowsExceptionForAllSpacesNonce() {
	    final OutputDataStream syncMsg = Utils.createSyncMessage("        ");
	}

	@Test (expected = IllegalArgumentException.class)
    public final void testCreateSyncThrowsExceptionForEmptyNonce() {
        final OutputDataStream syncMsg = Utils.createSyncMessage("");
    }

	@Test
    public void testProcessSyncWorks() throws Exception  {
	    final org.beepcore.beep.core.MimeHeaders headers = new org.beepcore.beep.core.MimeHeaders(
                      Utils.CT_JALOP,
	                  org.beepcore.beep.core.MimeHeaders.DEFAULT_CONTENT_TRANSFER_ENCODING);
	    headers.setHeader(Utils.HDRS_MESSAGE, Utils.MSG_SYNC);
	    headers.setHeader(Utils.HDRS_NONCE, "1234");
	    createDataStream(headers);
	    final InputDataStreamAdapter ids = data.getInputStream();
	    final SyncMessage syncMsg = Utils.processSyncMessage(ids);
	    assertEquals("1234", syncMsg.getNonce());
	}

	@Test (expected = MissingMimeHeaderException.class)
	public void testProcessSyncThrowsExceptionWhenMissingNonce() throws Exception  {
	    final org.beepcore.beep.core.MimeHeaders headers = new org.beepcore.beep.core.MimeHeaders(
                Utils.CT_JALOP, org.beepcore.beep.core.MimeHeaders.DEFAULT_CONTENT_TRANSFER_ENCODING);
	        headers.setHeader(Utils.HDRS_MESSAGE, Utils.MSG_SYNC);
	        createDataStream(headers);
	        final InputDataStreamAdapter ids = data.getInputStream();
	        final SyncMessage syncMsg = Utils.processSyncMessage(ids);
	}

	@Test (expected = UnexpectedMimeValueException.class)
    public void testProcessSyncThrowsExceptionWithEmptyNonce() throws Exception  {
        final org.beepcore.beep.core.MimeHeaders headers = new org.beepcore.beep.core.MimeHeaders(
                Utils.CT_JALOP, org.beepcore.beep.core.MimeHeaders.DEFAULT_CONTENT_TRANSFER_ENCODING);
            headers.setHeader(Utils.HDRS_MESSAGE, Utils.MSG_SYNC);
            headers.setHeader(Utils.HDRS_NONCE, "");
            createDataStream(headers);
            final InputDataStreamAdapter ids = data.getInputStream();
            final SyncMessage syncMsg = Utils.processSyncMessage(ids);
    }

	@Test (expected = UnexpectedMimeValueException.class)
    public void testProcessSyncThrowsExceptionWithNonceAllSpaces() throws Exception  {
        final org.beepcore.beep.core.MimeHeaders headers = new org.beepcore.beep.core.MimeHeaders(
                Utils.CT_JALOP, org.beepcore.beep.core.MimeHeaders.DEFAULT_CONTENT_TRANSFER_ENCODING);
            headers.setHeader(Utils.HDRS_MESSAGE, Utils.MSG_SYNC);
            headers.setHeader(Utils.HDRS_NONCE, "     ");
            createDataStream(headers);
            final InputDataStreamAdapter ids = data.getInputStream();
            final SyncMessage syncMsg = Utils.processSyncMessage(ids);
    }

	public static class MockOutputDataStream {
		@Mock
		public void $init(final org.beepcore.beep.core.MimeHeaders mh, final BufferSegment bs) throws Exception {
			assertEquals(Utils.CT_JALOP, mh.getContentType());
			assertEquals(Utils.MSG_DIGEST, mh.getHeaderValue(Utils.HDRS_MESSAGE));
			assertEquals("2", mh.getHeaderValue(Utils.HDRS_COUNT));
			final String digests = "abcdef123456789=2\r\n123456789abcdef=1\r\n";
			assertEquals(digests, new String(bs.getData()));
		}
	}

	@Test
	public void testCreateDigestMessageWorks() throws Exception {
		final Map<String, String> digests = new HashMap<String, String>();
		digests.put("1", "123456789abcdef");
		digests.put("2", "abcdef123456789");

		new MockResponseOutputDataStream();

		final OutputDataStream ods = Utils.createDigestMessage(digests);
		assertNotNull(ods);
		assertTrue(ods.isComplete());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCreateDigestMessageThrowsExceptionWhenNonceIsEmpty() throws Exception {
		final Map<String, String> digests = new HashMap<String, String>();
		digests.put("", "123456789abcdef");

		new MockResponseOutputDataStream();

		Utils.createDigestMessage(digests);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCreateDigestMessageThrowsExceptionWhenDigestIsEmpty() throws Exception {
		final Map<String, String> digests = new HashMap<String, String>();
		digests.put("1", "");

		new MockResponseOutputDataStream();

		Utils.createDigestMessage(digests);
	}

	@Test
	public void testProcessDigestMessageWorks() throws Exception {

		final String digests = "abcdef123456789=1\r\n123456789abcdef=2";
		final org.beepcore.beep.core.MimeHeaders mh = new org.beepcore.beep.core.MimeHeaders();
		mh.setContentType(Utils.CT_JALOP);
		mh.setHeader(Utils.HDRS_MESSAGE, Utils.MSG_DIGEST);
		mh.setHeader(Utils.HDRS_COUNT, "2");

		createDataStream(mh, digests);

		final InputDataStreamAdapter ids = data.getInputStream();

		final DigestMessage dm = Utils.processDigestMessage(ids);
		assertNotNull(dm);

		final HashMap<String, String> digestsMap = (HashMap<String, String>) dm.getMap();
		assertNotNull(digestsMap);
		assertFalse(digestsMap.isEmpty());
		assertTrue(digestsMap.containsKey("1"));
		assertTrue(digestsMap.containsKey("2"));
		assertEquals("abcdef123456789", digestsMap.get("1"));
		assertEquals("123456789abcdef", digestsMap.get("2"));
	}

	@Test(expected = MissingMimeHeaderException.class)
	public void testProcessDigestMessageThrowsMissingMimeHeaderException() throws Exception {

		final String digests = "abcdef123456789=1\r\n123456789abcdef=2";
		final org.beepcore.beep.core.MimeHeaders mh = new org.beepcore.beep.core.MimeHeaders();
		mh.setContentType(Utils.CT_JALOP);
		mh.setHeader(Utils.HDRS_COUNT, "2");

		createDataStream(mh, digests);

		final InputDataStreamAdapter ids = data.getInputStream();

		Utils.processDigestMessage(ids);
	}

	@Test(expected = UnexpectedMimeValueException.class)
	public void testProcessDigestMessageThrowsUnexpectedMimeValueException() throws Exception {

		final String digests = "abcdef123456789=1\r\n123456789abcdef=2";
		final org.beepcore.beep.core.MimeHeaders mh = new org.beepcore.beep.core.MimeHeaders();
		mh.setContentType(Utils.CT_JALOP);
		mh.setHeader(Utils.HDRS_MESSAGE, Utils.MSG_DIGEST_RESP);
		mh.setHeader(Utils.HDRS_COUNT, "2");

		createDataStream(mh, digests);

		final InputDataStreamAdapter ids = data.getInputStream();

		Utils.processDigestMessage(ids);
	}

	public static class MockResponseOutputDataStream extends MockUp<OutputDataStream> {
		@Mock
		public void $init(final org.beepcore.beep.core.MimeHeaders mh, final BufferSegment bs) throws Exception {
			assertEquals(Utils.CT_JALOP, mh.getContentType());
			assertEquals(Utils.MSG_DIGEST_RESP, mh.getHeaderValue(Utils.HDRS_MESSAGE));
			assertEquals("3", mh.getHeaderValue(Utils.HDRS_COUNT));
			final String digests = "Invalid=12346\r\nUnknown=12347\r\nConfirmed=12345\r\n";
			assertEquals(digests, new String(bs.getData()));
		}
	}

	@Test
	public void testCreateDigestResponseWorks() throws Exception {
		final Map<String, DigestStatus> digests = new HashMap<String, DigestStatus>();
		digests.put("12345", DigestStatus.Confirmed);
		digests.put("12346", DigestStatus.Invalid);
		digests.put("12347", DigestStatus.Unknown);

		new MockResponseOutputDataStream();

		final OutputDataStream ods = Utils.createDigestResponse(digests);
		assertNotNull(ods);
		assertTrue(ods.isComplete());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCreateDigestResponseThrowsExceptionWhenNonceIsEmpty() throws Exception {
		final Map<String, DigestStatus> digests = new HashMap<String, DigestStatus>();
		digests.put("", DigestStatus.Confirmed);
		digests.put("nothing", DigestStatus.Invalid);
		digests.put("blah", DigestStatus.Unknown);

		new MockResponseOutputDataStream();

		Utils.createDigestResponse(digests);
	}

	@Test
	public void testProcessDigestResponseWorks() throws Exception {

		final String messagePayload = "confirmed=12345\r\ninvalid=12346\r\nunknown=12347";
		final org.beepcore.beep.core.MimeHeaders mh = new org.beepcore.beep.core.MimeHeaders();
		mh.setContentType(Utils.CT_JALOP);
		mh.setHeader(Utils.HDRS_MESSAGE, Utils.MSG_DIGEST_RESP);
		mh.setHeader(Utils.HDRS_COUNT, "3");

		createDataStream(mh, messagePayload);

		final InputDataStreamAdapter ids = data.getInputStream();

		final DigestResponse dr = Utils.processDigestResponse(ids, messagePayload);
		assertNotNull(dr);

		final HashMap<String, DigestStatus> digestsMap = (HashMap<String, DigestStatus>) dr.getMap();
		assertNotNull(digestsMap);
		assertFalse(digestsMap.isEmpty());
		assertTrue(digestsMap.containsKey("12345"));
		assertTrue(digestsMap.containsKey("12346"));
		assertTrue(digestsMap.containsKey("12347"));
		assertEquals(DigestStatus.Confirmed, digestsMap.get("12345"));
		assertEquals(DigestStatus.Invalid, digestsMap.get("12346"));
		assertEquals(DigestStatus.Unknown, digestsMap.get("12347"));
	}

	@Test(expected = MissingMimeHeaderException.class)
	public void testProcessDigestResponseThrowsMissingMimeHeaderException() throws Exception {

		final String messagePayload = "confirmed=12345\r\ninvalid=12346\r\nunknown=12347";
		final org.beepcore.beep.core.MimeHeaders mh = new org.beepcore.beep.core.MimeHeaders();
		mh.setContentType(Utils.CT_JALOP);
		mh.setHeader(Utils.HDRS_COUNT, "3");

		createDataStream(mh, messagePayload);

		final InputDataStreamAdapter ids = data.getInputStream();

		Utils.processDigestResponse(ids, messagePayload);
	}

	@Test(expected = UnexpectedMimeValueException.class)
	public void testProcessDigestResponseThrowsUnexpectedMimeValueException() throws Exception {

		final String messagePayload = "confirmed=12345\r\ninvalid=12346\r\nunknown=12347";
		final org.beepcore.beep.core.MimeHeaders mh = new org.beepcore.beep.core.MimeHeaders();
		mh.setContentType(Utils.CT_JALOP);
		mh.setHeader(Utils.HDRS_MESSAGE, Utils.MSG_DIGEST);
		mh.setHeader(Utils.HDRS_COUNT, "3");

		createDataStream(mh, messagePayload);

		final InputDataStreamAdapter ids = data.getInputStream();

		Utils.processDigestResponse(ids, messagePayload);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testProcessDigestResponseThrowsIllegalArgumenException() throws Exception {

		final String messagePayload = "nothing=12345\r\nnull=12346\r\nnil=12347";
		final org.beepcore.beep.core.MimeHeaders mh = new org.beepcore.beep.core.MimeHeaders();
		mh.setContentType(Utils.CT_JALOP);
		mh.setHeader(Utils.HDRS_MESSAGE, Utils.MSG_DIGEST_RESP);
		mh.setHeader(Utils.HDRS_COUNT, "3");

		createDataStream(mh, messagePayload);

		final InputDataStreamAdapter ids = data.getInputStream();

		Utils.processDigestResponse(ids, messagePayload);
	}
}