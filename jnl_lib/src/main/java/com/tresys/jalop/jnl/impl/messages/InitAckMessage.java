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

import javax.xml.soap.MimeHeaders;

/**
 * This represents an 'initialize-ack' message.
 */
public class InitAckMessage extends Message {

	/** The XML encoding (EXI, gzip, deflate, none, etc) */
	private final String encoding;
	/** The message digest (sha256, sha512, etc). */
	private final String digest;

	/**
	 * Create a new InitAckMessage for the given XML encoding and message
	 * digest.
	 * 
	 * @param encoding
	 *            The XML encoding.
	 * @param digest
	 *            The message digest.
	 * @param unknownHeaders
	 *            Any additional headers sent in the message.
	 */
	public InitAckMessage(final String encoding, final String digest,
			final MimeHeaders unknownHeaders) {
		super(unknownHeaders);
		this.encoding = encoding;
		this.digest = digest;
	}

	/**
	 * Get the selected message digest.
	 * 
	 * @return the digest
	 */
	public String getDigest() {
		return this.digest;
	}

	/**
	 * Get the selected XML encoding.
	 * 
	 * @return the encoding
	 */
	public String getEncoding() {
		return this.encoding;
	}

}
