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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.soap.MimeHeaders;

import org.beepcore.beep.core.InputDataStream;
import org.beepcore.beep.core.InputDataStreamAdapter;
import org.beepcore.beep.core.OutputDataStream;
import org.beepcore.beep.util.BufferSegment;
import org.junit.Before;
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

	@Before
	public void setUp() throws Exception {

		Constructor<InputDataStream> constructor = InputDataStream.class
				.getDeclaredConstructor();
		constructor.setAccessible(true);

		data = constructor.newInstance();
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

}
