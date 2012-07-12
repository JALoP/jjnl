/*
 * Source code in 3rd-party is licensed and owned by their respective
 * copyright holders.
 *
 * All other source code is copyright Tresys Technology and licensed as below.
 *
 * Copyright (c) 2012 Tresys Technology LLC, Columbia, Maryland, USA
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

import mockit.Mock;
import mockit.Mockit;

import org.beepcore.beep.core.InputDataStream;
import org.beepcore.beep.core.InputDataStreamAdapter;
import org.beepcore.beep.core.OutputDataStream;
import org.beepcore.beep.util.BufferSegment;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.tresys.jalop.jnl.ConnectionHandler.ConnectError;
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

		Constructor<InputDataStream> constructor = InputDataStream.class
				.getDeclaredConstructor();
		constructor.setAccessible(true);

		data = constructor.newInstance();
	}

	private static String getMimeHeader(OutputDataStream ods, String headerName) throws IllegalArgumentException, IllegalAccessException {
	    org.beepcore.beep.core.MimeHeaders headers = (org.beepcore.beep.core.MimeHeaders) odsMimeHeaders.get(ods);
	    return headers.getHeaderValue(headerName);
    }

	public void createDataStream(org.beepcore.beep.core.MimeHeaders headers)
			throws Exception {

		Method addMethod = InputDataStream.class.getDeclaredMethod("add",
				BufferSegment.class);
		addMethod.setAccessible(true);
		addMethod.invoke(data, headers.getBufferSegment());

		Method completeMethod = InputDataStream.class
				.getDeclaredMethod("setComplete");
		completeMethod.setAccessible(true);
		completeMethod.invoke(data);
	}

	public void createDataStream(org.beepcore.beep.core.MimeHeaders headers, String digests)
		throws Exception {

		Method addMethod = InputDataStream.class.getDeclaredMethod("add",
				BufferSegment.class);
		addMethod.setAccessible(true);
		addMethod.invoke(data, headers.getBufferSegment());
		addMethod.invoke(data, new BufferSegment(digests.getBytes("us-ascii")));

		Method completeMethod = InputDataStream.class
				.getDeclaredMethod("setComplete");
		completeMethod.setAccessible(true);
		completeMethod.invoke(data);
	}

	@Test
	public void testCreateInitAckMessageWorks() throws SecurityException,
			NoSuchFieldException, IllegalArgumentException,
			IllegalAccessException {
		OutputDataStream ods = Utils.createInitAckMessage(Utils.DGST_SHA256,
				Utils.BINARY);
		assertTrue(ods.isComplete());

		Field headers = ods.getClass().getDeclaredField("mimeHeaders");
		headers.setAccessible(true);

		org.beepcore.beep.core.MimeHeaders mimeHeaders = (org.beepcore.beep.core.MimeHeaders) headers
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

		String padded = "   string   ";
		String returned = Utils.checkForEmptyString(padded, "padded string");
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

		List<String> words = new ArrayList<String>();
		words.add("word1");
		words.add("word2");
		words.add("word3");
		String wordList = Utils.makeStringList(words, "listname");
		assertEquals(wordList, "word1, word2, word3");
	}

	@Test
	public void testMakeStringListHasComma() {

		List<String> words = new ArrayList<String>();
		words.add("word1");
		words.add("word2");
		String wordList = Utils.makeStringList(words, "listname");
		assertTrue(wordList.contains(","));
	}

	@Test
	public void testMakeStringListOneWord() {

		List<String> words = new ArrayList<String>();
		words.add("word1");
		String wordList = Utils.makeStringList(words, "listname");
		assertEquals(wordList, "word1");
	}

	@Test
	public void testMakeStringListReturnsNullWhenNull() {

		String wordList = Utils.makeStringList(null, "listname");
		assertNull(wordList);
	}

	@Test
	public void testMakeStringListReturnsNullWhenBlank() {

		List<String> words = new ArrayList<String>();
		String wordList = Utils.makeStringList(words, "listname");
		assertNull(wordList);
	}

	@Test
	public void testProcessInitMessageWorks() throws Exception {

		org.beepcore.beep.core.MimeHeaders headers = new org.beepcore.beep.core.MimeHeaders(
				Utils.CT_JALOP,
				org.beepcore.beep.core.MimeHeaders.DEFAULT_CONTENT_TRANSFER_ENCODING);

		headers.setHeader(Utils.HDRS_MESSAGE, Utils.MSG_INIT);
		headers.setHeader(Utils.HDRS_ACCEPT_ENCODING, Utils.BINARY);
		headers.setHeader(Utils.HDRS_ACCEPT_DIGEST, Utils.DGST_SHA256);
		headers.setHeader(Utils.HDRS_MODE, Utils.SUBSCRIBE);
		headers.setHeader(Utils.HDRS_DATA_CLASS, Utils.LOG);

		createDataStream(headers);

		InputDataStreamAdapter ids = data.getInputStream();
		InitMessage msg = Utils.processInitMessage(ids);

		assertEquals(msg.getAcceptDigests(), Arrays.asList(Utils.DGST_SHA256));
		assertEquals(msg.getRole(), Role.Subscriber);
		assertEquals(msg.getAcceptEncodings(), Arrays.asList(Utils.BINARY));
		assertEquals(msg.getRecordType(), RecordType.Log);
	}

	@Test(expected = MissingMimeHeaderException.class)
	public void testProcessInitMessageThrowsExceptionWithNoMode()
			throws Exception {

		org.beepcore.beep.core.MimeHeaders headers = new org.beepcore.beep.core.MimeHeaders(
				Utils.CT_JALOP,
				org.beepcore.beep.core.MimeHeaders.DEFAULT_CONTENT_TRANSFER_ENCODING);

		headers.setHeader(Utils.HDRS_MESSAGE, Utils.MSG_INIT);
		headers.setHeader(Utils.HDRS_DATA_CLASS, Utils.LOG);
		createDataStream(headers);
		InputDataStreamAdapter ids = data.getInputStream();
		Utils.processInitMessage(ids);
	}

	@Test
	public void testProcessInitMessageSubscriber() throws Exception {

		org.beepcore.beep.core.MimeHeaders headers = new org.beepcore.beep.core.MimeHeaders(
				Utils.CT_JALOP,
				org.beepcore.beep.core.MimeHeaders.DEFAULT_CONTENT_TRANSFER_ENCODING);

		headers.setHeader(Utils.HDRS_MESSAGE, Utils.MSG_INIT);
		headers.setHeader(Utils.HDRS_MODE, Utils.SUBSCRIBE);
		headers.setHeader(Utils.HDRS_DATA_CLASS, Utils.LOG);

		createDataStream(headers);

		InputDataStreamAdapter ids = data.getInputStream();

		InitMessage msg = Utils.processInitMessage(ids);
		assertEquals(msg.getRole(), Role.Subscriber);
	}

	@Test
	public void testProcessInitMessagePublisher() throws Exception {

		org.beepcore.beep.core.MimeHeaders headers = new org.beepcore.beep.core.MimeHeaders(
				Utils.CT_JALOP,
				org.beepcore.beep.core.MimeHeaders.DEFAULT_CONTENT_TRANSFER_ENCODING);

		headers.setHeader(Utils.HDRS_MESSAGE, Utils.MSG_INIT);
		headers.setHeader(Utils.HDRS_MODE, Utils.PUBLISH);
		headers.setHeader(Utils.HDRS_DATA_CLASS, Utils.LOG);

		createDataStream(headers);

		InputDataStreamAdapter ids = data.getInputStream();

		InitMessage msg = Utils.processInitMessage(ids);
		assertEquals(msg.getRole(), Role.Publisher);
	}

	@Test(expected = UnexpectedMimeValueException.class)
	public void testProcessInitMessageThrowsExceptionWithBadMode()
			throws Exception {

		org.beepcore.beep.core.MimeHeaders headers = new org.beepcore.beep.core.MimeHeaders(
				Utils.CT_JALOP,
				org.beepcore.beep.core.MimeHeaders.DEFAULT_CONTENT_TRANSFER_ENCODING);

		headers.setHeader(Utils.HDRS_MESSAGE, Utils.MSG_INIT);
		headers.setHeader(Utils.HDRS_MODE, "bad");
		headers.setHeader(Utils.HDRS_DATA_CLASS, Utils.LOG);
		createDataStream(headers);
		InputDataStreamAdapter ids = data.getInputStream();
		Utils.processInitMessage(ids);
	}

	@Test(expected = MissingMimeHeaderException.class)
	public void testProcessInitMessageFailsWithNoDataClass() throws Exception {

		org.beepcore.beep.core.MimeHeaders headers = new org.beepcore.beep.core.MimeHeaders(
				Utils.CT_JALOP,
				org.beepcore.beep.core.MimeHeaders.DEFAULT_CONTENT_TRANSFER_ENCODING);

		headers.setHeader(Utils.HDRS_MESSAGE, Utils.MSG_INIT);
		headers.setHeader(Utils.HDRS_MODE, Utils.SUBSCRIBE);
		createDataStream(headers);
		InputDataStreamAdapter ids = data.getInputStream();
		Utils.processInitMessage(ids);
	}

	@Test
	public void testProcessInitMessageJournal() throws Exception {

		org.beepcore.beep.core.MimeHeaders headers = new org.beepcore.beep.core.MimeHeaders(
				Utils.CT_JALOP,
				org.beepcore.beep.core.MimeHeaders.DEFAULT_CONTENT_TRANSFER_ENCODING);

		headers.setHeader(Utils.HDRS_MESSAGE, Utils.MSG_INIT);
		headers.setHeader(Utils.HDRS_MODE, Utils.PUBLISH);
		headers.setHeader(Utils.HDRS_DATA_CLASS, Utils.JOURNAL);

		createDataStream(headers);

		InputDataStreamAdapter ids = data.getInputStream();

		InitMessage msg = Utils.processInitMessage(ids);
		assertEquals(msg.getRecordType(), RecordType.Journal);
	}

	@Test
	public void testProcessInitMessageAudit() throws Exception {

		org.beepcore.beep.core.MimeHeaders headers = new org.beepcore.beep.core.MimeHeaders(
				Utils.CT_JALOP,
				org.beepcore.beep.core.MimeHeaders.DEFAULT_CONTENT_TRANSFER_ENCODING);

		headers.setHeader(Utils.HDRS_MESSAGE, Utils.MSG_INIT);
		headers.setHeader(Utils.HDRS_MODE, Utils.PUBLISH);
		headers.setHeader(Utils.HDRS_DATA_CLASS, Utils.AUDIT);

		createDataStream(headers);

		InputDataStreamAdapter ids = data.getInputStream();

		InitMessage msg = Utils.processInitMessage(ids);
		assertEquals(msg.getRecordType(), RecordType.Audit);
	}

	@Test
	public void testProcessInitMessageLog() throws Exception {

		org.beepcore.beep.core.MimeHeaders headers = new org.beepcore.beep.core.MimeHeaders(
				Utils.CT_JALOP,
				org.beepcore.beep.core.MimeHeaders.DEFAULT_CONTENT_TRANSFER_ENCODING);

		headers.setHeader(Utils.HDRS_MESSAGE, Utils.MSG_INIT);
		headers.setHeader(Utils.HDRS_MODE, Utils.PUBLISH);
		headers.setHeader(Utils.HDRS_DATA_CLASS, Utils.LOG);

		createDataStream(headers);

		InputDataStreamAdapter ids = data.getInputStream();

		InitMessage msg = Utils.processInitMessage(ids);
		assertEquals(msg.getRecordType(), RecordType.Log);
	}

	@Test(expected = UnexpectedMimeValueException.class)
	public void testProcessInitMessageThrowsExceptionWithBadDataClass()
			throws Exception {

		org.beepcore.beep.core.MimeHeaders headers = new org.beepcore.beep.core.MimeHeaders(
				Utils.CT_JALOP,
				org.beepcore.beep.core.MimeHeaders.DEFAULT_CONTENT_TRANSFER_ENCODING);

		headers.setHeader(Utils.HDRS_MESSAGE, Utils.MSG_INIT);
		headers.setHeader(Utils.HDRS_MODE, Utils.PUBLISH);
		headers.setHeader(Utils.HDRS_DATA_CLASS, "bad");

		createDataStream(headers);

		InputDataStreamAdapter ids = data.getInputStream();

		Utils.processInitMessage(ids);
	}

	@Test
	public void testProcessInitMessageSetsAgent() throws Exception {

		org.beepcore.beep.core.MimeHeaders headers = new org.beepcore.beep.core.MimeHeaders(
				Utils.CT_JALOP,
				org.beepcore.beep.core.MimeHeaders.DEFAULT_CONTENT_TRANSFER_ENCODING);

		headers.setHeader(Utils.HDRS_MESSAGE, Utils.MSG_INIT);
		headers.setHeader(Utils.HDRS_MODE, Utils.PUBLISH);
		headers.setHeader(Utils.HDRS_DATA_CLASS, Utils.LOG);
		headers.setHeader(Utils.HDRS_AGENT, "agent");

		createDataStream(headers);

		InputDataStreamAdapter ids = data.getInputStream();

		InitMessage msg = Utils.processInitMessage(ids);
		assertEquals(msg.getAgentString(), "agent");
	}

	@Test
	public void testProcessMessageCommonWorks() throws Exception {

		org.beepcore.beep.core.MimeHeaders headers = new org.beepcore.beep.core.MimeHeaders(
				Utils.CT_JALOP,
				org.beepcore.beep.core.MimeHeaders.DEFAULT_CONTENT_TRANSFER_ENCODING);

		headers.setHeader(Utils.HDRS_MESSAGE, Utils.MSG_INIT);
		createDataStream(headers);
		InputDataStreamAdapter ids = data.getInputStream();

		MimeHeaders[] mimeHeaders = Utils.processMessageCommon(ids,
				Utils.MSG_INIT, Utils.HDRS_ACCEPT_ENCODING);

		assertEquals(mimeHeaders.length, 2);
	}

	@Test(expected = MissingMimeHeaderException.class)
	public void testProcessMessageCommonThrowsExceptionWithNoMessage()
			throws Exception {

		org.beepcore.beep.core.MimeHeaders headers = new org.beepcore.beep.core.MimeHeaders(
				Utils.CT_JALOP,
				org.beepcore.beep.core.MimeHeaders.DEFAULT_CONTENT_TRANSFER_ENCODING);

		createDataStream(headers);
		InputDataStreamAdapter ids = data.getInputStream();

		Utils.processMessageCommon(ids, Utils.MSG_INIT,
				Utils.HDRS_ACCEPT_ENCODING);
	}

	@Test(expected = UnexpectedMimeValueException.class)
	public void testProcessMessageCommonThrowsExceptionWithBadMessage()
			throws Exception {

		org.beepcore.beep.core.MimeHeaders headers = new org.beepcore.beep.core.MimeHeaders(
				Utils.CT_JALOP,
				org.beepcore.beep.core.MimeHeaders.DEFAULT_CONTENT_TRANSFER_ENCODING);

		headers.setHeader(Utils.HDRS_MESSAGE, Utils.MSG_INIT);
		createDataStream(headers);
		InputDataStreamAdapter ids = data.getInputStream();

		Utils.processMessageCommon(ids, Utils.MSG_AUDIT,
				Utils.HDRS_ACCEPT_ENCODING);
	}

	@Test
	public void testSplitHeadersWorks() throws Exception {

		org.beepcore.beep.core.MimeHeaders headers = new org.beepcore.beep.core.MimeHeaders(
				Utils.CT_JALOP,
				org.beepcore.beep.core.MimeHeaders.DEFAULT_CONTENT_TRANSFER_ENCODING);

		headers.setHeader(Utils.HDRS_MODE, Utils.PUBLISH);
		headers.setHeader(Utils.HDRS_DATA_CLASS, Utils.LOG);
		createDataStream(headers);
		InputDataStreamAdapter ids = data.getInputStream();

		MimeHeaders[] mimeHeaders = Utils.splitHeaders(ids, Utils.HDRS_MODE);
		assertEquals(mimeHeaders.length, 2);

		final MimeHeaders knownHeaders = mimeHeaders[0];
		final MimeHeaders unknownHeaders = mimeHeaders[1];

		assertTrue(knownHeaders.getHeader(Utils.HDRS_MODE) != null);
		assertTrue(unknownHeaders.getHeader(Utils.HDRS_DATA_CLASS) != null);
	}

	@Test
	public void testSplitHeadersAddsToKnown() throws Exception {

		org.beepcore.beep.core.MimeHeaders headers = new org.beepcore.beep.core.MimeHeaders(
				Utils.CT_JALOP,
				org.beepcore.beep.core.MimeHeaders.DEFAULT_CONTENT_TRANSFER_ENCODING);

		headers.setHeader(Utils.HDRS_MODE, Utils.PUBLISH);
		createDataStream(headers);
		InputDataStreamAdapter ids = data.getInputStream();

		MimeHeaders[] mimeHeaders = Utils.splitHeaders(ids, Utils.HDRS_MODE);
		assertEquals(mimeHeaders.length, 2);

		final MimeHeaders knownHeaders = mimeHeaders[0];
		assertTrue(knownHeaders.getHeader(Utils.HDRS_MODE) != null);
	}

	@Test
	public void testSplitHeadersAddsToUnKnown() throws Exception {

		org.beepcore.beep.core.MimeHeaders headers = new org.beepcore.beep.core.MimeHeaders(
				Utils.CT_JALOP,
				org.beepcore.beep.core.MimeHeaders.DEFAULT_CONTENT_TRANSFER_ENCODING);

		headers.setHeader(Utils.HDRS_DATA_CLASS, Utils.LOG);
		createDataStream(headers);
		InputDataStreamAdapter ids = data.getInputStream();

		MimeHeaders[] mimeHeaders = Utils.splitHeaders(ids, Utils.HDRS_MODE);
		assertEquals(mimeHeaders.length, 2);

		final MimeHeaders unknownHeaders = mimeHeaders[1];
		assertTrue(unknownHeaders.getHeader(Utils.HDRS_DATA_CLASS) != null);
	}

	@Test
	public void testCreateInitMessageWorks() throws SecurityException,
			NoSuchFieldException, IllegalArgumentException,
			IllegalAccessException {

		OutputDataStream ods = Utils.createInitMessage(Role.Publisher,
				RecordType.Log, Arrays.asList(Utils.BINARY),
				Arrays.asList(Utils.DGST_SHA256), "agent");
		assertTrue(ods.isComplete());

		Field headers = ods.getClass().getDeclaredField("mimeHeaders");
		headers.setAccessible(true);

		org.beepcore.beep.core.MimeHeaders mimeHeaders = (org.beepcore.beep.core.MimeHeaders) headers
				.get(ods);

		assertEquals(mimeHeaders.getHeaderValue(Utils.HDRS_MESSAGE),
				Utils.MSG_INIT);
		assertEquals(mimeHeaders.getHeaderValue(Utils.HDRS_ACCEPT_DIGEST),
				Utils.DGST_SHA256);
		assertEquals(mimeHeaders.getHeaderValue(Utils.HDRS_ACCEPT_ENCODING),
				Utils.BINARY);
		assertEquals(mimeHeaders.getHeaderValue(Utils.HDRS_MODE), Utils.PUBLISH);
		assertEquals(mimeHeaders.getHeaderValue(Utils.HDRS_DATA_CLASS),
				Utils.LOG);
	}

	@Test
	public void testCreateInitMessageWorksWithSubscribeAudit()
			throws SecurityException, NoSuchFieldException,
			IllegalArgumentException, IllegalAccessException {

		OutputDataStream ods = Utils.createInitMessage(Role.Subscriber,
				RecordType.Audit, Arrays.asList(Utils.BINARY),
				Arrays.asList(Utils.DGST_SHA256), "agent");

		Field headers = ods.getClass().getDeclaredField("mimeHeaders");
		headers.setAccessible(true);

		org.beepcore.beep.core.MimeHeaders mimeHeaders = (org.beepcore.beep.core.MimeHeaders) headers
				.get(ods);

		assertEquals(mimeHeaders.getHeaderValue(Utils.HDRS_MODE),
				Utils.SUBSCRIBE);
		assertEquals(mimeHeaders.getHeaderValue(Utils.HDRS_DATA_CLASS),
				Utils.AUDIT);
	}

	@Test
	public void testCreateInitMessageWorksWithJournal()
			throws SecurityException, NoSuchFieldException,
			IllegalArgumentException, IllegalAccessException {

		OutputDataStream ods = Utils.createInitMessage(Role.Subscriber,
				RecordType.Journal, Arrays.asList(Utils.BINARY),
				Arrays.asList(Utils.DGST_SHA256), "agent");

		Field headers = ods.getClass().getDeclaredField("mimeHeaders");
		headers.setAccessible(true);

		org.beepcore.beep.core.MimeHeaders mimeHeaders = (org.beepcore.beep.core.MimeHeaders) headers
				.get(ods);

		assertEquals(mimeHeaders.getHeaderValue(Utils.HDRS_DATA_CLASS),
				Utils.JOURNAL);
	}

	@Test
	public void testCreateInitMessageWorksWithNullEncoding()
			throws SecurityException, NoSuchFieldException,
			IllegalArgumentException, IllegalAccessException {

		OutputDataStream ods = Utils
				.createInitMessage(Role.Publisher, RecordType.Log, null,
						Arrays.asList(Utils.DGST_SHA256), "agent");

		Field headers = ods.getClass().getDeclaredField("mimeHeaders");
		headers.setAccessible(true);

		org.beepcore.beep.core.MimeHeaders mimeHeaders = (org.beepcore.beep.core.MimeHeaders) headers
				.get(ods);
		assertTrue(mimeHeaders.getHeaderValue(Utils.HDRS_ACCEPT_ENCODING) == null);
	}

	@Test
	public void testCreateInitMessageWorksWithNullDigest()
			throws SecurityException, NoSuchFieldException,
			IllegalArgumentException, IllegalAccessException {

		OutputDataStream ods = Utils.createInitMessage(Role.Publisher,
				RecordType.Log, Arrays.asList(Utils.BINARY), null, "agent");

		Field headers = ods.getClass().getDeclaredField("mimeHeaders");
		headers.setAccessible(true);

		org.beepcore.beep.core.MimeHeaders mimeHeaders = (org.beepcore.beep.core.MimeHeaders) headers
				.get(ods);
		assertTrue(mimeHeaders.getHeaderValue(Utils.HDRS_ACCEPT_DIGEST) == null);
	}

	@Test
	public void testCreateInitMessageWorksWithNoAgent()
			throws SecurityException, NoSuchFieldException,
			IllegalArgumentException, IllegalAccessException {

		OutputDataStream ods = Utils.createInitMessage(Role.Publisher,
				RecordType.Log, Arrays.asList(Utils.BINARY),
				Arrays.asList(Utils.DGST_SHA256), null);

		Field headers = ods.getClass().getDeclaredField("mimeHeaders");
		headers.setAccessible(true);

		org.beepcore.beep.core.MimeHeaders mimeHeaders = (org.beepcore.beep.core.MimeHeaders) headers
				.get(ods);
		assertTrue(mimeHeaders.getHeaderValue(Utils.HDRS_AGENT) == null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCreateInitMessageThrowsExceptionWithNoRole()
			throws SecurityException, NoSuchFieldException,
			IllegalArgumentException, IllegalAccessException {

		Utils.createInitMessage(Role.Unset, RecordType.Journal,
				Arrays.asList(Utils.BINARY), Arrays.asList(Utils.DGST_SHA256),
				"agent");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCreateInitMessageThrowsExceptionWithNoRecordType()
			throws SecurityException, NoSuchFieldException,
			IllegalArgumentException, IllegalAccessException {

		Utils.createInitMessage(Role.Publisher, RecordType.Unset,
				Arrays.asList(Utils.BINARY), Arrays.asList(Utils.DGST_SHA256),
				"agent");
	}

	@Test
	public void testCreateInitNackMessageWorks() throws SecurityException,
			NoSuchFieldException, IllegalArgumentException,
			IllegalAccessException {

		List<ConnectError> connectErrors = new ArrayList<ConnectError>();
		connectErrors.add(ConnectError.UnauthorizedMode);
		connectErrors.add(ConnectError.UnsupportedDigest);
		connectErrors.add(ConnectError.UnsupportedEncoding);
		connectErrors.add(ConnectError.UnsupportedMode);
		connectErrors.add(ConnectError.UnsupportedVersion);
		OutputDataStream ods = Utils.createInitNackMessage(connectErrors);
		assertTrue(ods.isComplete());

		Field headers = ods.getClass().getDeclaredField("mimeHeaders");
		headers.setAccessible(true);

		org.beepcore.beep.core.MimeHeaders mimeHeaders = (org.beepcore.beep.core.MimeHeaders) headers
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

		List<ConnectError> connectErrors = new ArrayList<ConnectError>();
		connectErrors.add(ConnectError.Accept);
		Utils.createInitNackMessage(connectErrors);
	}

	@Test
	public void testProcessInitAckWorks() throws Exception {

		org.beepcore.beep.core.MimeHeaders headers = new org.beepcore.beep.core.MimeHeaders(
				Utils.CT_JALOP,
				org.beepcore.beep.core.MimeHeaders.DEFAULT_CONTENT_TRANSFER_ENCODING);

		headers.setHeader(Utils.HDRS_MESSAGE, Utils.MSG_INIT_ACK);
		headers.setHeader(Utils.HDRS_ENCODING, Utils.BINARY);
		headers.setHeader(Utils.HDRS_DIGEST, Utils.DGST_SHA256);

		createDataStream(headers);

		InputDataStreamAdapter ids = data.getInputStream();
		InitAckMessage msg = Utils.processInitAck(ids);

		assertEquals(msg.getDigest(), Utils.DGST_SHA256);
		assertEquals(msg.getEncoding(), Utils.BINARY);
	}

	@Test(expected = MissingMimeHeaderException.class)
	public void testProcessInitAckThrowsExceptionWithNoDigest()
			throws Exception {

		org.beepcore.beep.core.MimeHeaders headers = new org.beepcore.beep.core.MimeHeaders(
				Utils.CT_JALOP,
				org.beepcore.beep.core.MimeHeaders.DEFAULT_CONTENT_TRANSFER_ENCODING);

		headers.setHeader(Utils.HDRS_MESSAGE, Utils.MSG_INIT_ACK);
		headers.setHeader(Utils.HDRS_ENCODING, Utils.BINARY);

		createDataStream(headers);

		InputDataStreamAdapter ids = data.getInputStream();
		Utils.processInitAck(ids);
	}

	@Test(expected = MissingMimeHeaderException.class)
	public void testProcessInitAckThrowsExceptionWithNoEncoding()
			throws Exception {

		org.beepcore.beep.core.MimeHeaders headers = new org.beepcore.beep.core.MimeHeaders(
				Utils.CT_JALOP,
				org.beepcore.beep.core.MimeHeaders.DEFAULT_CONTENT_TRANSFER_ENCODING);

		headers.setHeader(Utils.HDRS_MESSAGE, Utils.MSG_INIT_ACK);
		headers.setHeader(Utils.HDRS_DIGEST, Utils.DGST_SHA256);

		createDataStream(headers);

		InputDataStreamAdapter ids = data.getInputStream();
		Utils.processInitAck(ids);
	}

	@Test
	public void testProcessInitNackWorks() throws Exception {

		org.beepcore.beep.core.MimeHeaders headers = new org.beepcore.beep.core.MimeHeaders(
				Utils.CT_JALOP,
				org.beepcore.beep.core.MimeHeaders.DEFAULT_CONTENT_TRANSFER_ENCODING);

		headers.setHeader(Utils.HDRS_MESSAGE, Utils.MSG_INIT_NACK);
		headers.setHeader(Utils.HDRS_UNSUPPORTED_VERSION, "this");
		headers.setHeader(Utils.HDRS_UNSUPPORTED_MODE, "this");
		headers.setHeader(Utils.HDRS_UNAUTHORIZED_MODE, "this");
		headers.setHeader(Utils.HDRS_UNSUPPORTED_ENCODING, "this");
		headers.setHeader(Utils.HDRS_UNSUPPORTED_DIGEST, "this");

		createDataStream(headers);

		InputDataStreamAdapter ids = data.getInputStream();
		InitNackMessage msg = Utils.processInitNack(ids);

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

		org.beepcore.beep.core.MimeHeaders headers = new org.beepcore.beep.core.MimeHeaders(
				Utils.CT_JALOP,
				org.beepcore.beep.core.MimeHeaders.DEFAULT_CONTENT_TRANSFER_ENCODING);

		headers.setHeader(Utils.HDRS_MESSAGE, Utils.MSG_INIT_NACK);

		createDataStream(headers);

		InputDataStreamAdapter ids = data.getInputStream();
		InitNackMessage msg = Utils.processInitNack(ids);

		assertTrue(msg.getErrorList().isEmpty());
	}

	@Test
	public void testCreateSubscribeMessageWorks() throws SecurityException,
			NoSuchFieldException, IllegalArgumentException,
			IllegalAccessException {

		OutputDataStream ods = Utils.createSubscribeMessage("0");
		assertTrue(ods.isComplete());

		Field headers = ods.getClass().getDeclaredField("mimeHeaders");
		headers.setAccessible(true);

		org.beepcore.beep.core.MimeHeaders mimeHeaders = (org.beepcore.beep.core.MimeHeaders) headers
				.get(ods);

		assertEquals(mimeHeaders.getHeaderValue(Utils.HDRS_MESSAGE),
				Utils.MSG_SUBSCRIBE);
		assertEquals(mimeHeaders.getHeaderValue(Utils.HDRS_SERIAL_ID), "0");
	}

	@Test
	public void testProcessSubscribeWorks() throws Exception {

		org.beepcore.beep.core.MimeHeaders headers = new org.beepcore.beep.core.MimeHeaders(
				Utils.CT_JALOP,
				org.beepcore.beep.core.MimeHeaders.DEFAULT_CONTENT_TRANSFER_ENCODING);

		headers.setHeader(Utils.HDRS_MESSAGE, Utils.MSG_SUBSCRIBE);
		headers.setHeader(Utils.HDRS_SERIAL_ID, "0");

		createDataStream(headers);

		InputDataStreamAdapter ids = data.getInputStream();
		SubscribeMessage msg = Utils.processSubscribe(ids);

		assertEquals(msg.getSerialId(), "0");
	}

	@Test(expected = MissingMimeHeaderException.class)
	public void testProcessSubscribeThrowsExceptionWithNoSerial()
			throws Exception {

		org.beepcore.beep.core.MimeHeaders headers = new org.beepcore.beep.core.MimeHeaders(
				Utils.CT_JALOP,
				org.beepcore.beep.core.MimeHeaders.DEFAULT_CONTENT_TRANSFER_ENCODING);

		headers.setHeader(Utils.HDRS_MESSAGE, Utils.MSG_SUBSCRIBE);

		createDataStream(headers);

		InputDataStreamAdapter ids = data.getInputStream();
		Utils.processSubscribe(ids);
	}

	@Test(expected = UnexpectedMimeValueException.class)
	public void testProcessSubscribeThrowsExceptionWithBlankSerial()
			throws Exception {

		org.beepcore.beep.core.MimeHeaders headers = new org.beepcore.beep.core.MimeHeaders(
				Utils.CT_JALOP,
				org.beepcore.beep.core.MimeHeaders.DEFAULT_CONTENT_TRANSFER_ENCODING);

		headers.setHeader(Utils.HDRS_MESSAGE, Utils.MSG_SUBSCRIBE);
		headers.setHeader(Utils.HDRS_SERIAL_ID, "");

		createDataStream(headers);

		InputDataStreamAdapter ids = data.getInputStream();
		Utils.processSubscribe(ids);
	}

	@Test
	public void testCreateJournalResumeMessageWorks() throws SecurityException,
			NoSuchFieldException, IllegalArgumentException,
			IllegalAccessException {

		OutputDataStream ods = Utils.createJournalResumeMessage("0", 10);
		assertTrue(ods.isComplete());

		Field headers = ods.getClass().getDeclaredField("mimeHeaders");
		headers.setAccessible(true);

		org.beepcore.beep.core.MimeHeaders mimeHeaders = (org.beepcore.beep.core.MimeHeaders) headers
				.get(ods);

		assertEquals(mimeHeaders.getHeaderValue(Utils.HDRS_MESSAGE),
				Utils.MSG_JOURNAL_RESUME);
		assertEquals(mimeHeaders.getHeaderValue(Utils.HDRS_SERIAL_ID), "0");
		assertEquals(mimeHeaders.getHeaderValue(Utils.HDRS_JOURNAL_OFFSET),
				"10");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCreateJournalResumeMessageThrowsExceptionNullSerial()
			throws SecurityException, NoSuchFieldException,
			IllegalArgumentException, IllegalAccessException {

		Utils.createJournalResumeMessage(null, 10);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCreateJournalResumeMessageThrowsExceptionBlankSerial()
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

		org.beepcore.beep.core.MimeHeaders headers = new org.beepcore.beep.core.MimeHeaders(
				Utils.CT_JALOP,
				org.beepcore.beep.core.MimeHeaders.DEFAULT_CONTENT_TRANSFER_ENCODING);

		headers.setHeader(Utils.HDRS_MESSAGE, Utils.MSG_JOURNAL_RESUME);
		headers.setHeader(Utils.HDRS_SERIAL_ID, "1");
		headers.setHeader(Utils.HDRS_JOURNAL_OFFSET, "10");

		createDataStream(headers);

		InputDataStreamAdapter ids = data.getInputStream();
		JournalResumeMessage jrm = Utils.processJournalResume(ids);

		assertEquals(jrm.getSerialId(), "1");
		assertEquals(jrm.getOffset(), 10);
	}

	@Test(expected = MissingMimeHeaderException.class)
	public void testProcessJournalResumeThrowsExceptionWithNoSerial()
			throws Exception {

		org.beepcore.beep.core.MimeHeaders headers = new org.beepcore.beep.core.MimeHeaders(
				Utils.CT_JALOP,
				org.beepcore.beep.core.MimeHeaders.DEFAULT_CONTENT_TRANSFER_ENCODING);

		headers.setHeader(Utils.HDRS_MESSAGE, Utils.MSG_JOURNAL_RESUME);
		headers.setHeader(Utils.HDRS_JOURNAL_OFFSET, "10");

		createDataStream(headers);

		InputDataStreamAdapter ids = data.getInputStream();
		Utils.processJournalResume(ids);
	}

	@Test(expected = UnexpectedMimeValueException.class)
	public void testProcessJournalResumeThrowsExceptionWithBlankSerial()
			throws Exception {

		org.beepcore.beep.core.MimeHeaders headers = new org.beepcore.beep.core.MimeHeaders(
				Utils.CT_JALOP,
				org.beepcore.beep.core.MimeHeaders.DEFAULT_CONTENT_TRANSFER_ENCODING);

		headers.setHeader(Utils.HDRS_MESSAGE, Utils.MSG_JOURNAL_RESUME);
		headers.setHeader(Utils.HDRS_SERIAL_ID, "");
		headers.setHeader(Utils.HDRS_JOURNAL_OFFSET, "10");

		createDataStream(headers);

		InputDataStreamAdapter ids = data.getInputStream();
		Utils.processJournalResume(ids);
	}

	@Test(expected = MissingMimeHeaderException.class)
	public void testProcessJournalResumeThrowsExceptionWithNoOffset()
			throws Exception {

		org.beepcore.beep.core.MimeHeaders headers = new org.beepcore.beep.core.MimeHeaders(
				Utils.CT_JALOP,
				org.beepcore.beep.core.MimeHeaders.DEFAULT_CONTENT_TRANSFER_ENCODING);

		headers.setHeader(Utils.HDRS_MESSAGE, Utils.MSG_JOURNAL_RESUME);
		headers.setHeader(Utils.HDRS_SERIAL_ID, "1");

		createDataStream(headers);

		InputDataStreamAdapter ids = data.getInputStream();
		Utils.processJournalResume(ids);
	}

	@Test(expected = UnexpectedMimeValueException.class)
	public void testProcessJournalResumeThrowsExceptionWithBlankOffset()
			throws Exception {

		org.beepcore.beep.core.MimeHeaders headers = new org.beepcore.beep.core.MimeHeaders(
				Utils.CT_JALOP,
				org.beepcore.beep.core.MimeHeaders.DEFAULT_CONTENT_TRANSFER_ENCODING);

		headers.setHeader(Utils.HDRS_MESSAGE, Utils.MSG_JOURNAL_RESUME);
		headers.setHeader(Utils.HDRS_SERIAL_ID, "1");
		headers.setHeader(Utils.HDRS_JOURNAL_OFFSET, "");

		createDataStream(headers);

		InputDataStreamAdapter ids = data.getInputStream();
		Utils.processJournalResume(ids);
	}

	@Test(expected = UnexpectedMimeValueException.class)
	public void testProcessJournalResumeThrowsExceptionWithBadOffset()
			throws Exception {

		org.beepcore.beep.core.MimeHeaders headers = new org.beepcore.beep.core.MimeHeaders(
				Utils.CT_JALOP,
				org.beepcore.beep.core.MimeHeaders.DEFAULT_CONTENT_TRANSFER_ENCODING);

		headers.setHeader(Utils.HDRS_MESSAGE, Utils.MSG_JOURNAL_RESUME);
		headers.setHeader(Utils.HDRS_SERIAL_ID, "1");
		headers.setHeader(Utils.HDRS_JOURNAL_OFFSET, "bad");

		createDataStream(headers);

		InputDataStreamAdapter ids = data.getInputStream();
		Utils.processJournalResume(ids);
	}

	@Test(expected = UnexpectedMimeValueException.class)
	public void testProcessJournalResumeThrowsExceptionWithNegativeOffset()
			throws Exception {

		org.beepcore.beep.core.MimeHeaders headers = new org.beepcore.beep.core.MimeHeaders(
				Utils.CT_JALOP,
				org.beepcore.beep.core.MimeHeaders.DEFAULT_CONTENT_TRANSFER_ENCODING);

		headers.setHeader(Utils.HDRS_MESSAGE, Utils.MSG_JOURNAL_RESUME);
		headers.setHeader(Utils.HDRS_SERIAL_ID, "1");
		headers.setHeader(Utils.HDRS_JOURNAL_OFFSET, "-1");

		createDataStream(headers);

		InputDataStreamAdapter ids = data.getInputStream();
		Utils.processJournalResume(ids);
	}
	@Test
	public void testCreateSyncWorks() throws IllegalAccessException {
	    OutputDataStream syncMsg = Utils.createSyncMessage("1234");
	    assertNotNull(syncMsg);
	    assertEquals("1234", getMimeHeader(syncMsg, Utils.HDRS_SERIAL_ID));
	    assertEquals(Utils.CT_JALOP, getMimeHeader(syncMsg, org.beepcore.beep.core.MimeHeaders.CONTENT_TYPE));
        assertEquals(org.beepcore.beep.core.MimeHeaders.DEFAULT_CONTENT_TRANSFER_ENCODING, getMimeHeader(syncMsg, org.beepcore.beep.core.MimeHeaders.CONTENT_TRANSFER_ENCODING));
	}

	@Test (expected = IllegalArgumentException.class)
	    public final void testCreateSyncThrowsExceptionForNullSerialId() {
        OutputDataStream syncMsg = Utils.createSyncMessage(null);
    }

	@Test (expected = IllegalArgumentException.class)
    public final void testCreateSyncThrowsExceptionForAllSpacesSerialId() {
	    OutputDataStream syncMsg = Utils.createSyncMessage("        ");
	}

	@Test (expected = IllegalArgumentException.class)
    public final void testCreateSyncThrowsExceptionForEmptySerialId() {
        OutputDataStream syncMsg = Utils.createSyncMessage("");
    }

	@Test
    public void testProcessSyncWorks() throws Exception  {
	    org.beepcore.beep.core.MimeHeaders headers = new org.beepcore.beep.core.MimeHeaders(
                      Utils.CT_JALOP,
	                  org.beepcore.beep.core.MimeHeaders.DEFAULT_CONTENT_TRANSFER_ENCODING);
	    headers.setHeader(Utils.HDRS_MESSAGE, Utils.MSG_SYNC);
	    headers.setHeader(Utils.HDRS_SERIAL_ID, "1234");
	    createDataStream(headers);
	    InputDataStreamAdapter ids = data.getInputStream();
	    SyncMessage syncMsg = Utils.processSyncMessage(ids);
	    assertEquals("1234", syncMsg.getSerialId());
	}

	@Test (expected = MissingMimeHeaderException.class)
	public void testProcessSyncThrowsExceptionWhenMissingSerialId() throws Exception  {
	    org.beepcore.beep.core.MimeHeaders headers = new org.beepcore.beep.core.MimeHeaders(
                Utils.CT_JALOP, org.beepcore.beep.core.MimeHeaders.DEFAULT_CONTENT_TRANSFER_ENCODING);
	        headers.setHeader(Utils.HDRS_MESSAGE, Utils.MSG_SYNC);
	        createDataStream(headers);
	        InputDataStreamAdapter ids = data.getInputStream();
	        SyncMessage syncMsg = Utils.processSyncMessage(ids);
	}

	@Test (expected = UnexpectedMimeValueException.class)
    public void testProcessSyncThrowsExceptionWithEmptySerialId() throws Exception  {
        org.beepcore.beep.core.MimeHeaders headers = new org.beepcore.beep.core.MimeHeaders(
                Utils.CT_JALOP, org.beepcore.beep.core.MimeHeaders.DEFAULT_CONTENT_TRANSFER_ENCODING);
            headers.setHeader(Utils.HDRS_MESSAGE, Utils.MSG_SYNC);
            headers.setHeader(Utils.HDRS_SERIAL_ID, "");
            createDataStream(headers);
            InputDataStreamAdapter ids = data.getInputStream();
            SyncMessage syncMsg = Utils.processSyncMessage(ids);
    }

	@Test (expected = UnexpectedMimeValueException.class)
    public void testProcessSyncThrowsExceptionWithSerialIdAllSpaces() throws Exception  {
        org.beepcore.beep.core.MimeHeaders headers = new org.beepcore.beep.core.MimeHeaders(
                Utils.CT_JALOP, org.beepcore.beep.core.MimeHeaders.DEFAULT_CONTENT_TRANSFER_ENCODING);
            headers.setHeader(Utils.HDRS_MESSAGE, Utils.MSG_SYNC);
            headers.setHeader(Utils.HDRS_SERIAL_ID, "     ");
            createDataStream(headers);
            InputDataStreamAdapter ids = data.getInputStream();
            SyncMessage syncMsg = Utils.processSyncMessage(ids);
    }

	public static class MockOutputDataStream {
		@Mock
		public void $init(org.beepcore.beep.core.MimeHeaders mh, BufferSegment bs) throws Exception {
			assertEquals(Utils.CT_JALOP, mh.getContentType());
			assertEquals(Utils.MSG_DIGEST, mh.getHeaderValue(Utils.HDRS_MESSAGE));
			assertEquals("2", mh.getHeaderValue(Utils.HDRS_COUNT));
			String digests = "abcdef123456789=2\r\n123456789abcdef=1\r\n";
			assertEquals(digests, new String(bs.getData()));
		}
	}

	@Test
	public void testCreateDigestMessageWorks() throws Exception {
		Map<String, String> digests = new HashMap<String, String>();
		digests.put("1", "123456789abcdef");
		digests.put("2", "abcdef123456789");

		Mockit.setUpMock(OutputDataStream.class, new MockOutputDataStream());

		OutputDataStream ods = Utils.createDigestMessage(digests);
		assertNotNull(ods);
		assertTrue(ods.isComplete());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCreateDigestMessageThrowsExceptionWhenSidIsEmpty() throws Exception {
		Map<String, String> digests = new HashMap<String, String>();
		digests.put("", "123456789abcdef");

		Mockit.setUpMock(OutputDataStream.class, new MockOutputDataStream());

		Utils.createDigestMessage(digests);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCreateDigestMessageThrowsExceptionWhenDigestIsEmpty() throws Exception {
		Map<String, String> digests = new HashMap<String, String>();
		digests.put("1", "");

		Mockit.setUpMock(OutputDataStream.class, new MockOutputDataStream());

		Utils.createDigestMessage(digests);
	}

	@Test
	public void testProcessDigestMessageWorks() throws Exception {

		String digests = "abcdef123456789=1\r\n123456789abcdef=2";
		org.beepcore.beep.core.MimeHeaders mh = new org.beepcore.beep.core.MimeHeaders();
		mh.setContentType(Utils.CT_JALOP);
		mh.setHeader(Utils.HDRS_MESSAGE, Utils.MSG_DIGEST);
		mh.setHeader(Utils.HDRS_COUNT, "2");

		createDataStream(mh, digests);

		InputDataStreamAdapter ids = data.getInputStream();

		DigestMessage dm = Utils.processDigestMessage(ids);
		assertNotNull(dm);

		HashMap<String, String> digestsMap = (HashMap<String, String>) dm.getMap();
		assertNotNull(digestsMap);
		assertFalse(digestsMap.isEmpty());
		assertTrue(digestsMap.containsKey("1"));
		assertTrue(digestsMap.containsKey("2"));
		assertEquals("abcdef123456789", digestsMap.get("1"));
		assertEquals("123456789abcdef", digestsMap.get("2"));
	}

	@Test(expected = MissingMimeHeaderException.class)
	public void testProcessDigestMessageThrowsMissingMimeHeaderException() throws Exception {

		String digests = "abcdef123456789=1\r\n123456789abcdef=2";
		org.beepcore.beep.core.MimeHeaders mh = new org.beepcore.beep.core.MimeHeaders();
		mh.setContentType(Utils.CT_JALOP);
		mh.setHeader(Utils.HDRS_COUNT, "2");

		createDataStream(mh, digests);

		InputDataStreamAdapter ids = data.getInputStream();

		Utils.processDigestMessage(ids);
	}

	@Test(expected = UnexpectedMimeValueException.class)
	public void testProcessDigestMessageThrowsUnexpectedMimeValueException() throws Exception {

		String digests = "abcdef123456789=1\r\n123456789abcdef=2";
		org.beepcore.beep.core.MimeHeaders mh = new org.beepcore.beep.core.MimeHeaders();
		mh.setContentType(Utils.CT_JALOP);
		mh.setHeader(Utils.HDRS_MESSAGE, Utils.MSG_DIGEST_RESP);
		mh.setHeader(Utils.HDRS_COUNT, "2");

		createDataStream(mh, digests);

		InputDataStreamAdapter ids = data.getInputStream();

		Utils.processDigestMessage(ids);
	}
}
