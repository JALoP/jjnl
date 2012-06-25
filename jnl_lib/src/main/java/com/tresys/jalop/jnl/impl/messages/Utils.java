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

import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.soap.MimeHeader;
import javax.xml.soap.MimeHeaders;

import org.beepcore.beep.core.BEEPException;
import org.beepcore.beep.core.InputDataStreamAdapter;

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
