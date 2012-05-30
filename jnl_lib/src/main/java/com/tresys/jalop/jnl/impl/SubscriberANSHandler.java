/*
 * Source code in 3rd-party is licensed and owned by their respective
 * copyright holders.
 *
 * All other source code is copyright Tresys Technology and licensed as below.
 *
 * Copyright (c) ${year} Tresys Technology LLC, Columbia, Maryland, USA
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
package com.tresys.jalop.jnl.impl;

import java.io.InputStream;
import java.security.MessageDigest;

import javax.xml.soap.MimeHeader;

import org.beepcore.beep.core.InputDataStream;

import com.tresys.jalop.jnl.IncompleteRecordException;

/**
 * This class reads the ANS reply for a 'journal-resume' or 'subscribe' message
 * and can be used to present the various data segments as
 * {@link java.io.InputStream}s.
 */
public class SubscriberANSHandler {
	/**
	 * The MessageDigset to use for calculating the JALoP digest.
	 */
	private MessageDigest md;
	/**
	 * The {@link InputDataStream} for a particular ANS message.
	 */
	private InputDataStream ds;

	/**
	 * Create a SubscriberANSHandler for a record using the
	 * {@link InputDataStream}
	 * 
	 * @param md
	 *            the {@link MessageDigest} that should be used for digest
	 *            calculations.
	 * @param ds
	 *            the {@link InputDataStream} to read
	 */
	public SubscriberANSHandler(MessageDigest md, InputDataStream ds) {
		super();
		this.md = md;
		this.ds = ds;
	}

	/**
	 * Retrieve an {@link InputStream} for the system meta-data.
	 * 
	 * @return an {@link InputStream}, or NULL if the system meta-data size is
	 *         0.
	 */
	public InputStream getSystemMetadata() {
		// TODO: Implement this method.
		return null;
	}

	/**
	 * Retrieve an {@link InputStream} for the application meta-data.
	 * 
	 * @return an {@link InputStream}, or NULL if the system meta-data size is
	 *         0.
	 */
	public InputStream getAppMetadata() {
		// TODO: Implement this method.
		return null;
	}

	/**
	 * Retrieve an {@link InputStream} for the payload (journal, audit, or log
	 * data).
	 * 
	 * @return an {@link InputStream}, or NULL if there is no payload.
	 */
	public InputStream getPayload() {
		// TODO: Implement this method.
		return null;
	}

	/**
	 * Retrieve any additional MIME headers that were sent as part of this ANS
	 * reply. Recognized MIME headers (i.e. Content-Type, Transfer-Encoding, and
	 * JALoP headers) are not included in the returned map. If there are no
	 * additional MIME headers, then this function returns {@link MimeHeaders}
	 * object.
	 * 
	 * @return The {@link MimeHeaders}
	 */
	public MimeHeader getAddtionalHeaders() {
		// TODO: implement this method.
		return null;
	}

	/**
	 * Retrieve the calculated digest.
	 * 
	 * @return The digest of the record according to the JALoP specification.
	 * @throws IncompleteRecordException
	 *             is thrown if this method is called before reading all data
	 *             has not been read from the stream, or if the ANS message from
	 *             the remote is invalid (i.e. reports 100 bytes of payload, but
	 *             only contains 50).
	 */
	public byte[] getRecordDigest() throws IncompleteRecordException {
		// TODO: implement this method.
		return null;
	}
}
