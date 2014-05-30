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

import javax.xml.soap.MimeHeaders;

/**
 * This represents a 'journal-resume' message. A 'journal-resume' message
 * indicates a partially transferred journal record (by nonce) and an
 * offset. The offset indicates the number of bytes already transferred for a
 * particular journal record.
 */
public class JournalResumeMessage extends SubscribeMessage {
	private final long offset;

	/**
	 * Create a {@link JournalResumeMessage}
	 * 
	 * @param nonce
	 *            The nonce for the journal record to resume.
	 * @param offset
	 *            The offset into the journal record to start transferring data
	 *            from.
	 * @param otherHeaders
	 *            Any additional headers sent with this message.
	 */
	public JournalResumeMessage(final String nonce, final long offset,
			final MimeHeaders otherHeaders) {
		super(nonce, otherHeaders);
		this.offset = offset;
	}

	/**
	 * Obtain the offset for the journal-resume message, i.e. the number of
	 * bytes that have already been transferred.
	 * 
	 * @return the offset
	 */
	public long getOffset() {
		return this.offset;
	}
}
