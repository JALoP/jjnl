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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.soap.MimeHeader;
import javax.xml.soap.MimeHeaders;

import org.beepcore.beep.core.BEEPException;
import org.beepcore.beep.core.InputDataStreamAdapter;
import org.beepcore.beep.core.OutputDataStream;
import org.beepcore.beep.util.BufferSegment;

import com.tresys.jalop.jnl.ConnectionHandler.ConnectError;
import com.tresys.jalop.jnl.RecordType;
import com.tresys.jalop.jnl.Role;
import com.tresys.jalop.jnl.exceptions.MissingMimeHeaderException;
import com.tresys.jalop.jnl.exceptions.UnexpectedMimeValueException;

/**
 * Utility class for creating and parsing JALoP/BEEP messages.
 */
public class Utils {

	public static final String AUDIT = "audit";
	public static final String BINARY = "binary";
	public static final String BREAK = "BREAK";
	public static final String CONFIRMED = "confirmed";
	public static final String CONFIRMED_EQUALS = CONFIRMED + "=";
	public static final String CT_JALOP = "application/beep+jalop";
	public static final String DGST_CHAN_FORMAT_STR = "digest:%d";
	public static final String DGST_SHA256 = "sha256";
	public static final String ENC_XML = "xml";
	public static final String INVALID = "invalid";
	public static final String INVALID_EQUALS = INVALID + "=";
	public static final String JOURNAL = "journal";
	public static final String LOG = "log";
	public static final String PUBLISH = "publish";
	public static final String SUBSCRIBE = "subscribe";
	public static final String UNKNOWN = "unknown";
	public static final String UNKNOWN_EQUALS = UNKNOWN + "=";

	public static final String HDRS_ACCEPT_DIGEST = "JAL-Accept-Digest";
	public static final String HDRS_ACCEPT_ENCODING = "JAL-Accept-Encoding";
	public static final String HDRS_AGENT = "JAL-Agent";
	public static final String HDRS_APP_META_LEN = "JAL-Application-Metadata-Length";
	public static final String HDRS_AUDIT_LEN = "JAL-Audit-Length";
	public static final String HDRS_CONTENT_TXFR_ENCODING = "Content-Transfer-Encoding";
	public static final String HDRS_CONTENT_TYPE = "Content-Type";
	public static final String HDRS_COUNT = "JAL-Count";
	public static final String HDRS_DATA_CLASS = "JAL-Data-Class";
	public static final String HDRS_DIGEST = "JAL-Digest";
	public static final String HDRS_ENCODING = "JAL-Encoding";
	public static final String HDRS_JOURNAL_LEN = "JAL-Journal-Length";
	public static final String HDRS_JOURNAL_OFFSET = "JAL-Journal-Offset";
	public static final String HDRS_LOG_LEN = "JAL-Log-Length";
	public static final String HDRS_MESSAGE = "JAL-Message";
	public static final String HDRS_MODE = "JAL-Mode";
	public static final String HDRS_SERIAL_ID = "JAL-Serial-Id";
	public static final String HDRS_SYS_META_LEN = "JAL-System-Metadata-Length";
	public static final String HDRS_UNAUTHORIZED_MODE = "JAL-Unauthorized-Mode";
	public static final String HDRS_UNSUPPORTED_DIGEST = "JAL-Unsupported-Digest";
	public static final String HDRS_UNSUPPORTED_ENCODING = "JAL-Unsupported-Encoding";
	public static final String HDRS_UNSUPPORTED_MODE = "JAL-Unsupported-Mode";
	public static final String HDRS_UNSUPPORTED_VERSION = "JAL-Unsupported-Version";

	public static final String MSG_AUDIT = "audit-record";
	public static final String MSG_DIGEST = "digest";
	public static final String MSG_DIGEST_RESP = "digest-response";
	public static final String MSG_INIT = "initialize";
	public static final String MSG_INIT_ACK = "initialize-ack";
	public static final String MSG_INIT_NACK = "initialize-nack";
	public static final String MSG_JOURNAL = "journal-record";
	public static final String MSG_JOURNAL_RESUME = "journal-resume";
	public static final String MSG_LOG = "log-record";
	public static final String MSG_SYNC = "sync";
	public static final String MSG_SUBSCRIBE = "subscribe";

	public static final String SERIAL_ID = "serialId";
	public static final String STATUS = "status";

	/**
	 * Utility function to perform common tasks related to parsing incoming
	 * messages.
	 *
	 * @param is
	 *            The {@link InputDataStreamAdapter} for this message.
	 * @param expectedMessage
	 *            The expected message type (initialize, subscribe, etc)
	 * @param expectedHeaders
	 *            An array of expected (not necessarily required) MIME headers.
	 * @return an array of 2 {@link MimeHeaders}, the first {@link MimeHeader}
	 *         is the expected headers, the second is any remaining headers.
	 * @throws MissingMimeHeaderException
	 *             If the MIME headers to not contain a JAL-Message header.
	 * @throws UnexpectedMimeValueException
	 *             If the value of the JAL-Message header is not
	 *             <code>expectedMessage</code>.
	 * @throws BEEPException
	 *             If an underlying {@link BEEPException} occurs.
	 * @see {@link Utils#splitHeaders(InputDataStreamAdapter, String...)}
	 */
	static MimeHeaders[] processMessageCommon(final InputDataStreamAdapter is,
			final String expectedMessage, final String... expectedHeaders)
			throws MissingMimeHeaderException, UnexpectedMimeValueException,
			BEEPException {
		final String messageType = is.getHeaderValue(HDRS_MESSAGE);
		if (messageType == null) {
			throw new MissingMimeHeaderException(HDRS_MESSAGE);
		}

		if (!messageType.equalsIgnoreCase(expectedMessage)) {
			throw new UnexpectedMimeValueException(HDRS_MESSAGE,
					expectedMessage, messageType);
		}

		final MimeHeaders[] headers = splitHeaders(is, expectedHeaders);
		return headers;
	}

	/**
	 * Create an {@link OutputDataStream} for an initialize-ack message. The
	 * returned object is already marked as complete since an initialize-ack
	 * message carries no payload.
	 *
	 * @param digest
	 *            The selected digest algorithm. This must be a non-empty string
	 *            that contains at least one non-whitespace character.
	 * @param encoding
	 *            The selected XML encoding. This must be a non-empty string
	 *            that contains at least one non-whitespace character.
	 * @return The {@link OutputDataStream}
	 */
	public static OutputDataStream createInitAckMessage(String digest,
			String encoding) {

		final org.beepcore.beep.core.MimeHeaders headers = new org.beepcore.beep.core.MimeHeaders(
				CT_JALOP,
				org.beepcore.beep.core.MimeHeaders.DEFAULT_CONTENT_TRANSFER_ENCODING);
		digest = checkForEmptyString(digest, "digest");
		encoding = checkForEmptyString(encoding, "encoding");

		headers.setHeader(HDRS_MESSAGE, MSG_INIT_ACK);
		headers.setHeader(HDRS_DIGEST, digest);
		headers.setHeader(HDRS_ENCODING, encoding);

		final OutputDataStream ods = new OutputDataStream(headers, new BufferSegment(new byte[0]));
		ods.setComplete();
		return ods;
	}

	/**
	 * Create an {@link OutputDataStream} for an initialize message. The
	 * returned object is already marked as complete since an initialize message
	 * carries no payload.
	 *
	 * @param role
	 *            The {@link Role} ('JAL-Mode') to send.
	 * @param dataClass
	 *            The type of records to transfer over this channel.
	 * @param digestAlgorithms
	 *            The list of digest algorithms to propose. This may be
	 *            <code>null</code> or empty, in which case no algorithms are
	 *            proposed. If it is not empty, then each element must be
	 *            <code>non-null</code> and not be the empty string.
	 * @param agent
	 *            The string to send for the "JAL-Agent" header, this may be
	 *            <code>null</code>
	 * @param xmlEncodings
	 *            The list of digest algorithms to propose. This may be
	 *            <code>null</code> or empty, in which case no algorithms are
	 *            proposed. If it is not empty, then each element must be
	 *            <code>non-null</code> and not be the empty string.
	 * @return The {@link OutputDataStream}
	 */
	public static OutputDataStream createInitMessage(final Role role,
			final RecordType dataClass, final List<String> xmlEncodings,
			final List<String> digestAlgorithms, final String agent) {

		final org.beepcore.beep.core.MimeHeaders headers = new org.beepcore.beep.core.MimeHeaders(
				CT_JALOP,
				org.beepcore.beep.core.MimeHeaders.DEFAULT_CONTENT_TRANSFER_ENCODING);
		final String encodingsString = makeStringList(xmlEncodings, "encodings");
		final String digestsString = makeStringList(digestAlgorithms, "digests");

		headers.setHeader(HDRS_MESSAGE, MSG_INIT);
		if (encodingsString != null) {
			headers.setHeader(HDRS_ACCEPT_ENCODING, encodingsString);
		}
		if (digestsString != null) {
			headers.setHeader(HDRS_ACCEPT_DIGEST, digestsString);
		}
		switch (role) {
		case Publisher:
			headers.setHeader(HDRS_MODE, PUBLISH);
			break;
		case Subscriber:
			headers.setHeader(HDRS_MODE, SUBSCRIBE);
			break;
		default:
			throw new IllegalArgumentException("Illegal value for 'role'");
		}
		switch (dataClass) {
		case Journal:
			headers.setHeader(HDRS_DATA_CLASS, JOURNAL);
			break;
		case Audit:
			headers.setHeader(HDRS_DATA_CLASS, AUDIT);
			break;
		case Log:
			headers.setHeader(HDRS_DATA_CLASS, LOG);
			break;
		default:
			throw new IllegalArgumentException("Illegal value for 'dataClass'");
		}
		if (agent != null) {
			headers.setHeader(HDRS_AGENT, agent);
		}

		final OutputDataStream ods = new OutputDataStream(headers, new BufferSegment(new byte[0]));

		ods.setComplete();
		return ods;
	}

	/**
	 * Create an {@link OutputDataStream} for an initialize-nack message. The
	 * returned object is already marked as complete since an initialize-nack
	 * message carries no payload.
	 *
	 * @param errors
	 *            The list of {@link ConnectError}s to send in this message.
	 * @return The {@link OutputDataStream}
	 */

	public static OutputDataStream createInitNackMessage(
			final List<ConnectError> errors) {
		if ((errors == null) || errors.isEmpty()) {
			throw new IllegalArgumentException(
					"Must specify at least one error for an '" + MSG_INIT_NACK
							+ "' message.");
		}
		final org.beepcore.beep.core.MimeHeaders headers = new org.beepcore.beep.core.MimeHeaders(
				CT_JALOP,
				org.beepcore.beep.core.MimeHeaders.DEFAULT_CONTENT_TRANSFER_ENCODING);
		headers.setHeader(HDRS_MESSAGE, MSG_INIT_NACK);

		for (final ConnectError connectError : errors) {
			switch (connectError) {
			case UnauthorizedMode:
				headers.setHeader(HDRS_UNAUTHORIZED_MODE, "");
				break;
			case UnsupportedMode:
				headers.setHeader(HDRS_UNSUPPORTED_MODE, "");
				break;
			case UnsupportedDigest:
				headers.setHeader(HDRS_UNSUPPORTED_DIGEST, "");
				break;
			case UnsupportedEncoding:
				headers.setHeader(HDRS_UNSUPPORTED_ENCODING, "");
				break;
			case UnsupportedVersion:
				headers.setHeader(HDRS_UNSUPPORTED_VERSION, "");
				break;
			default:
				throw new IllegalArgumentException(
						"Cannot specify 'accept' as an error");
			}
		}
		final OutputDataStream ods = new OutputDataStream(headers, new BufferSegment(new byte[0]));
		ods.setComplete();
		return ods;
	}

	/**
	 * Create an {@link OutputDataStream} for a 'subscribe' message. Note that
	 * the {@link OutputDataStream} returned by this function has already had
	 * {@link OutputDataStream#setComplete()} called on it since a 'subscribe'
	 * message contains no payload.
	 *
	 * @param serialId
	 *            The serialId to send in the 'subscribe' message. This must be
	 *            non-null and contain at least one non-whitespace character.
	 * @return The {@link OutputDataStream}.
	 */
	static public OutputDataStream createSubscribeMessage(String serialId) {
		serialId = checkForEmptyString(serialId, HDRS_SERIAL_ID);
		final org.beepcore.beep.core.MimeHeaders headers = new org.beepcore.beep.core.MimeHeaders(
				CT_JALOP,
				org.beepcore.beep.core.MimeHeaders.DEFAULT_CONTENT_TRANSFER_ENCODING);
		headers.setHeader(HDRS_MESSAGE, MSG_SUBSCRIBE);
		headers.setHeader(HDRS_SERIAL_ID, serialId);
		final OutputDataStream ods = new OutputDataStream(headers, new BufferSegment(new byte[0]));
		ods.setComplete();
		return ods;
	}

	/**
	 * Process an initialize-ack message.
	 *
	 * @return an {@link InitAckMessage}
	 * @param is
	 *            The BEEP {@link InputDataStreamAdapter} that holds the
	 *            message.
	 * @throws BEEPException
	 *             If there is an underlying beep exception.
	 * @throws MissingMimeHeaderException
	 *             If a required MIME header is missing.
	 * @throws UnexpectedMimeValueException
	 *             If a MIME header has an unexpected value.
	 */
	public static InitAckMessage processInitAck(final InputDataStreamAdapter is)
			throws BEEPException, MissingMimeHeaderException,
			UnexpectedMimeValueException {
		final MimeHeaders[] headers = processMessageCommon(is, MSG_INIT_ACK,
				HDRS_MESSAGE, HDRS_ENCODING, HDRS_DIGEST);

		final MimeHeaders knownHeaders = headers[0];
		final MimeHeaders unknownHeaders = headers[1];

		String encoding;
		if (knownHeaders.getHeader(HDRS_ENCODING) != null) {
			encoding = knownHeaders.getHeader(HDRS_ENCODING)[0];
		} else {
			throw new MissingMimeHeaderException(HDRS_ENCODING);
		}

		String digest;
		if (knownHeaders.getHeader(HDRS_DIGEST) != null) {
			digest = knownHeaders.getHeader(HDRS_DIGEST)[0];
		} else {
			throw new MissingMimeHeaderException(HDRS_DIGEST);
		}
		return new InitAckMessage(encoding, digest, unknownHeaders);
	}

	/**
	 * Process an initialize message.
	 *
	 * @param is
	 *            The BEEP {@link InputDataStreamAdapter} that holds the
	 *            message.
	 * @return an {@link InitMessage}
	 * @throws BEEPException
	 *             If there is an error from the underlying BEEP connection.
	 * @throws UnexpectedMimeValueException
	 *             If the message contains illegal values for known MIME headers
	 * @throws MissingMimeHeaderException
	 *             If {@link Message} is missing a required MIME header.
	 */
	public static InitMessage processInitMessage(final InputDataStreamAdapter is)
			throws BEEPException, UnexpectedMimeValueException,
			MissingMimeHeaderException {
		final MimeHeaders[] headers = processMessageCommon(is, MSG_INIT,
				HDRS_ACCEPT_ENCODING, HDRS_MODE, HDRS_DATA_CLASS,
				HDRS_ACCEPT_DIGEST, HDRS_AGENT);
		final MimeHeaders knownHeaders = headers[0];
		final MimeHeaders unknownHeaders = headers[1];

		final String[] encodings = knownHeaders.getHeader(HDRS_ACCEPT_ENCODING);

		final String[] digests = knownHeaders.getHeader(HDRS_ACCEPT_DIGEST);

		final String[] mode = knownHeaders.getHeader(HDRS_MODE);
		if (mode == null) {
			throw new MissingMimeHeaderException(HDRS_MODE);
		}
		Role role;
		if (mode[0].equalsIgnoreCase(PUBLISH)) {
			role = Role.Publisher;
		} else if (mode[0].equalsIgnoreCase(SUBSCRIBE)) {
			role = Role.Subscriber;
		} else {
			throw new UnexpectedMimeValueException(HDRS_MODE, PUBLISH + ", or "
					+ SUBSCRIBE, mode[0]);
		}

		final String[] dataClass = knownHeaders.getHeader(HDRS_DATA_CLASS);
		if (dataClass == null) {
			throw new MissingMimeHeaderException(HDRS_DATA_CLASS);
		}
		RecordType recordType;
		if (dataClass[0].equalsIgnoreCase(JOURNAL)) {
			recordType = RecordType.Journal;
		} else if (dataClass[0].equalsIgnoreCase(AUDIT)) {
			recordType = RecordType.Audit;
		} else if (dataClass[0].equalsIgnoreCase(LOG)) {
			recordType = RecordType.Log;
		} else {
			throw new UnexpectedMimeValueException(HDRS_DATA_CLASS, JOURNAL
					+ ", " + AUDIT + ", or " + LOG, dataClass[0]);
		}

		final String[] agent = knownHeaders.getHeader(HDRS_AGENT);
		String agentString = null;
		if (agent != null) {
			agentString = agent[0];
		}

		return new InitMessage(recordType, role, encodings, digests,
				agentString, unknownHeaders);

	}

	/**
	 * Helper utility to check that a passed in string is non-null and contains
	 * non-whitespace characters. This method returns the original string with
	 * leading/trailing whitespace removed.
	 *
	 * @param toCheck
	 *            The string to check.
	 * @param parameterName
	 *            A human readable name to add to the exception.
	 * @return <code>toCheck</code> with leading/trailing whitespace removed.
	 * @throws IllegalArgumentException
	 *             if <code>toCheck</code> is <code>null</code> or is comprised
	 *             entirely of whitespace.
	 */
	public static String checkForEmptyString(String toCheck,
			final String parameterName) {
		if (toCheck == null) {
			throw new IllegalArgumentException("'" + parameterName
					+ "' cannot be null");
		}
		toCheck = toCheck.trim();
		if (toCheck.length() == 0) {
			throw new IllegalArgumentException("'" + parameterName
					+ "' must contain non-whitespace characaters");
		}
		return toCheck;
	}

	/**
	 * Helper utility to build a comma separated list of strings.
	 *
	 * @param stringList
	 *            The list of strings to join.
	 * @param listName
	 *            A name for the list, this is used if the list contains
	 *            <code>null</code> or empty strings.
	 * @return A {@link String} that is the comma separated list of the values
	 *         in <code>stringList</code>
	 */
	public static String makeStringList(final List<String> stringList,
			final String listName) {
		if ((stringList == null) || stringList.isEmpty()) {
			return null;
		}
		final Iterator<String> iter = stringList.iterator();
		final StringBuilder sb = new StringBuilder();
		while (iter.hasNext()) {
			String s = iter.next();
			s = checkForEmptyString(s, listName);
			sb.append(s);
			if (iter.hasNext()) {
				sb.append(", ");
			}
		}
		return sb.toString();
	}

	/**
	 * Process a subscribe message.
	 *
	 * @return an {@link InitAckMessage}
	 * @param is
	 *            The BEEP {@link InputDataStreamAdapter} that holds the
	 *            message.
	 * @throws BEEPException
	 *             If there is an underlying beep exception.
	 * @throws MissingMimeHeaderException
	 *             If a required MIME header is missing.
	 * @throws UnexpectedMimeValueException
	 *             If a MIME header has an unexpected value.
	 */
	static public SubscribeMessage processSubscribe(
			final InputDataStreamAdapter is) throws BEEPException,
			MissingMimeHeaderException, UnexpectedMimeValueException {
		final MimeHeaders[] headers = processMessageCommon(is, MSG_SUBSCRIBE,
				HDRS_MESSAGE, HDRS_SERIAL_ID);

		final MimeHeaders knownHeaders = headers[0];
		final MimeHeaders unknownHeaders = headers[1];

		String serialId;
		if (knownHeaders.getHeader(HDRS_SERIAL_ID) == null) {
			throw new MissingMimeHeaderException(HDRS_SERIAL_ID);
		}
		serialId = knownHeaders.getHeader(HDRS_SERIAL_ID)[0].trim();
		if (serialId.length() == 0) {
			throw new UnexpectedMimeValueException(HDRS_SERIAL_ID,
					"non-empty-string", serialId);
		}
		return new SubscribeMessage(serialId, unknownHeaders);
	}

	/**
	 * Create an {@link OutputDataStream} for a 'journal-resume' message. Note
	 * that the {@link OutputDataStream} returned by this function has already
	 * had {@link OutputDataStream#setComplete()} called on it since a
	 * 'journal-resume' message contains no payload.
	 *
	 * @param serialId
	 *            The serialId to send in the 'journal-resume' message. This
	 *            must be non-null and contain at least one non-whitespace
	 *            character.
	 * @param offset
	 *            The number offset to begin transferring data from for the
	 *            journal record.
	 * @return The {@link OutputDataStream}.
	 */
	static public OutputDataStream createJournalResumeMessage(String serialId,
			final long offset) {
		checkForEmptyString(serialId, HDRS_SERIAL_ID);

		if (offset < 0) {
			throw new IllegalArgumentException("offsset for '"
					+ MSG_JOURNAL_RESUME + "' must be positive");
		}
		final org.beepcore.beep.core.MimeHeaders headers = new org.beepcore.beep.core.MimeHeaders(
				CT_JALOP,
				org.beepcore.beep.core.MimeHeaders.DEFAULT_CONTENT_TRANSFER_ENCODING);
		headers.setHeader(HDRS_MESSAGE, MSG_JOURNAL_RESUME);
		headers.setHeader(HDRS_SERIAL_ID, serialId);
		headers.setHeader(HDRS_JOURNAL_OFFSET, Long.toString(offset));
		final OutputDataStream ods = new OutputDataStream(headers, new BufferSegment(new byte[0]));
		ods.setComplete();
		return ods;
	}

	/**
	 * Extract the details of an initialize-nack message.
	 *
	 * @return an {@link InitNackMessage}
	 * @param is
	 *            The BEEP {@link InputDataStreamAdapter} that holds the
	 *            message.
	 * @throws BEEPException
	 *             If there is an underlying beep exception.
	 * @throws MissingMimeHeaderException
	 *             If a required MIME header is missing.
	 * @throws UnexpectedMimeValueException
	 *             If a MIME header has an unexpected value.
	 */
	static public InitNackMessage processInitNack(
			final InputDataStreamAdapter is) throws BEEPException,
			MissingMimeHeaderException, UnexpectedMimeValueException {
		final MimeHeaders[] headers = processMessageCommon(is, MSG_INIT_NACK, HDRS_MESSAGE,
				HDRS_UNSUPPORTED_VERSION, HDRS_UNSUPPORTED_ENCODING,
				HDRS_UNSUPPORTED_MODE, HDRS_UNAUTHORIZED_MODE,
				HDRS_UNSUPPORTED_DIGEST);

		final MimeHeaders knownHeaders = headers[0];
		final MimeHeaders unknownHeaders = headers[1];

		final List<ConnectError> errors = new LinkedList<ConnectError>();
		if (knownHeaders.getHeader(HDRS_UNSUPPORTED_VERSION) != null) {
			errors.add(ConnectError.UnsupportedVersion);
		}
		if (knownHeaders.getHeader(HDRS_UNSUPPORTED_MODE) != null) {
			errors.add(ConnectError.UnsupportedMode);
		}
		if (knownHeaders.getHeader(HDRS_UNAUTHORIZED_MODE) != null) {
			errors.add(ConnectError.UnauthorizedMode);
		}

		if (knownHeaders.getHeader(HDRS_UNSUPPORTED_ENCODING) != null) {
			errors.add(ConnectError.UnsupportedEncoding);
		}
		if (knownHeaders.getHeader(HDRS_UNSUPPORTED_DIGEST) != null) {
			errors.add(ConnectError.UnsupportedDigest);
		}
		if (errors.isEmpty()) {
			throw new MissingMimeHeaderException("Error Headers");
		}
		return new InitNackMessage(errors, unknownHeaders);
	}

	/**
	 * Process a 'journal-resume' message.
	 *
	 * @return an {@link JournalResumeMessage}
	 * @param is
	 *            The BEEP {@link InputDataStreamAdapter} that holds the
	 *            message.
	 * @throws BEEPException
	 *             If there is an underlying beep exception.
	 * @throws MissingMimeHeaderException
	 *             If a required MIME header is missing.
	 * @throws UnexpectedMimeValueException
	 *             If a MIME header has an unexpected value.
	 */
	static public JournalResumeMessage processJournalResume(
			final InputDataStreamAdapter is) throws BEEPException,
			MissingMimeHeaderException, UnexpectedMimeValueException {

		final MimeHeaders[] headers = processMessageCommon(is,
				MSG_JOURNAL_RESUME, HDRS_MESSAGE, HDRS_SERIAL_ID,
				HDRS_JOURNAL_OFFSET);

		final MimeHeaders knownHeaders = headers[0];
		final MimeHeaders unknownHeaders = headers[1];

		String serialId;
		if (knownHeaders.getHeader(HDRS_SERIAL_ID) == null) {
			throw new MissingMimeHeaderException(HDRS_SERIAL_ID);
		}
		serialId = knownHeaders.getHeader(HDRS_SERIAL_ID)[0].trim();
		if (serialId.length() == 0) {
			throw new UnexpectedMimeValueException(HDRS_SERIAL_ID,
					"non-empty-string", serialId);
		}

		String offsetStr;
		if (knownHeaders.getHeader(HDRS_JOURNAL_OFFSET) == null) {
			throw new MissingMimeHeaderException(HDRS_JOURNAL_OFFSET);
		}
		offsetStr = knownHeaders.getHeader(HDRS_JOURNAL_OFFSET)[0].trim();
		if (offsetStr.length() == 0) {
			throw new UnexpectedMimeValueException(HDRS_JOURNAL_OFFSET,
					"non-empty-string", serialId);
		}
		long offset = -1;
		try {
			offset = Long.parseLong(offsetStr);
		} catch (final NumberFormatException e) {
			// Do nothing here, the following 'if' statement will generate an
			// exception if needed.
		}
		if (offset < 0) {
			throw new UnexpectedMimeValueException(HDRS_JOURNAL_OFFSET,
					"positive integer", offsetStr);
		}
		return new JournalResumeMessage(serialId, offset, unknownHeaders);
	}

	/**
	 * Process a Sync Message.
	 *
	 * @param is
	 *            The BEEP {@link InputDataStreamAdapter} that holds the sync
	 *            message.
	 * @return a {@link SyncMessage} that holds the Serial ID that is being synced
	 * @throws BEEPException
	 *             If there is an error from the underlying BEEP connection.
	 * @throws UnexpectedMimeValueException
	 *             If the message contains illegal values for known MIME headers
	 * @throws MissingMimeHeaderException
	 *             If {@link Message} is missing a required MIME header.
	 */

	static public SyncMessage processSyncMessage(final InputDataStreamAdapter is)
			throws MissingMimeHeaderException, UnexpectedMimeValueException,
			BEEPException {

		final MimeHeaders[] headers = processMessageCommon(is, MSG_SYNC,
				HDRS_MESSAGE, HDRS_SERIAL_ID);

		final MimeHeaders knownHeaders = headers[0];
		final MimeHeaders unknownHeaders = headers[1];

		String serialId;
		if (knownHeaders.getHeader(HDRS_SERIAL_ID) == null) {
			throw new MissingMimeHeaderException(HDRS_SERIAL_ID);
		}
		serialId = knownHeaders.getHeader(HDRS_SERIAL_ID)[0].trim();
		if (serialId.length() == 0) {
			throw new UnexpectedMimeValueException(HDRS_SERIAL_ID,
					"non-empty-string", serialId);
		}
		return new SyncMessage(serialId, unknownHeaders);
	}

	/**
	 * Create a sync message from a String (Serial ID). Note
	 * that the {@link OutputDataStream} returned by this function has already
	 * had {@link OutputDataStream#setComplete()} called on it since a
	 * 'sync' message contains no payload.
	 *
	 * @param serialId
	 *            The String that holds the synced serialID
	 * @return an {@link OutputDataStream} that holds the sync message
	 */
	static public OutputDataStream createSyncMessage(String serialId) {
		final org.beepcore.beep.core.MimeHeaders mh = new org.beepcore.beep.core.MimeHeaders();
		mh.setContentType(CT_JALOP);
		mh.setHeader(HDRS_MESSAGE, MSG_SYNC);
		mh.setHeader(HDRS_SERIAL_ID, checkForEmptyString(serialId, SERIAL_ID));

		final OutputDataStream ret = new OutputDataStream(mh, new BufferSegment(new byte[0]));
		ret.setComplete();

		return ret;
	}

	/**
	 * Process a Digest Message.
	 *
	 * @param is
	 *            The BEEP {@link InputDataStreamAdapter} that holds the
	 *            message.
	 * @return an {@link Map<String, String>}
	 * @throws BEEPException
	 *             If there is an error from the underlying BEEP connection.
	 * @throws UnexpectedMimeValueException
	 *             If the message contains illegal values for known MIME headers
	 * @throws MissingMimeHeaderException
	 *             If {@link Message} is missing a required MIME header.
	 */
	static public DigestMessage processDigestMessage(
			final InputDataStreamAdapter is) throws MissingMimeHeaderException,
			UnexpectedMimeValueException, BEEPException {

		final MimeHeaders[] headers = processMessageCommon(is, MSG_DIGEST,
				HDRS_MESSAGE, HDRS_COUNT);
		final MimeHeaders knownHeaders = headers[0];
		final MimeHeaders unknownHeaders = headers[1];
		int count = Integer.valueOf(knownHeaders.getHeader(HDRS_COUNT)[0]
				.trim());

		// get the digest map from the input stream
		Map<String, String> digestMap = new HashMap<String, String>();
		int numLeft = is.available();
		byte[] messageArray = new byte[numLeft];
		try {
			is.read(messageArray);
		} catch (IOException e) {
			e.printStackTrace();
		}
		String msgStr = new String(messageArray);

		String[] pairs = checkForEmptyString(msgStr, "payload").split("\\s+|=");

		for (int x = 0; x < count * 2; x += 2) {
			pairs[x] = checkForEmptyString(pairs[x], MSG_DIGEST);
			pairs[x + 1] = checkForEmptyString(pairs[x + 1], SERIAL_ID);
			digestMap.put(pairs[x + 1], pairs[x]);
		}

		return new DigestMessage(digestMap, unknownHeaders);
	}

	/**
	 * Create a digest message from a Map<String (serialID), String (digest)>.
	 *
	 * @param digestMap
	 *            The Map<String, String> that holds the serialID to digest
	 *            mappings
	 * @return an {@link OutputDataStream}
	 */
	static public OutputDataStream createDigestMessage(
			Map<String, String> digestMap) {

		StringBuilder message = new StringBuilder();
		final org.beepcore.beep.core.MimeHeaders mh = new org.beepcore.beep.core.MimeHeaders();
		mh.setContentType(CT_JALOP);
		mh.setHeader(HDRS_MESSAGE, MSG_DIGEST);
		mh.setHeader(HDRS_COUNT, String.valueOf(digestMap.size()));

		Iterator<String> sIDs = digestMap.keySet().iterator();
		while (sIDs.hasNext()) {
			String id = sIDs.next();
			message.append(checkForEmptyString(digestMap.get(id), MSG_DIGEST));
			message.append("=");
			message.append(checkForEmptyString(id, SERIAL_ID));
			message.append("\r\n");
		}

		OutputDataStream ret;
		try {
			ret = new OutputDataStream(mh, new BufferSegment(message.toString().getBytes("utf-8")));
		} catch (UnsupportedEncodingException e) {
			// We should never get here
			e.printStackTrace();
			return null;
		}
		ret.setComplete();

		return ret;
	}

	/**
	 * @param is
	 *            The BEEP {@link InputDataStreamAdapter} that holds the
	 *            message.
	 * @param expectedHeaders
	 *            List of expected headers
	 * @return an array of 2 {@link MimeHeaders}, the first {@link MimeHeader}
	 *         is the expected headers, the second is any remaining headers.
	 * @throws BEEPException
	 *             If there is an underlying BEEP exception.
	 */
	static MimeHeaders[] splitHeaders(final InputDataStreamAdapter is,
			final String... expectedHeaders) throws BEEPException {
		final Set<String> ehs = new TreeSet<String>(
				String.CASE_INSENSITIVE_ORDER);
		ehs.addAll(Arrays.asList(expectedHeaders));
		ehs.add(org.beepcore.beep.core.MimeHeaders.CONTENT_TYPE);
		ehs.add(org.beepcore.beep.core.MimeHeaders.CONTENT_TRANSFER_ENCODING);

		final MimeHeaders[] toReturn = new MimeHeaders[2];
		final MimeHeaders knownHeaders = new MimeHeaders();
		final MimeHeaders otherHeaders = new MimeHeaders();
		toReturn[0] = knownHeaders;
		toReturn[1] = otherHeaders;

		for (@SuppressWarnings("unchecked")
		final Enumeration<String> e = is.getHeaderNames(); e.hasMoreElements();) {
			final String header = e.nextElement();
			if (ehs.contains(header)) {
				knownHeaders.addHeader(header, is.getHeaderValue(header));
			} else {
				otherHeaders.addHeader(header, is.getHeaderValue(header));
			}
		}
		return toReturn;
	}
}
