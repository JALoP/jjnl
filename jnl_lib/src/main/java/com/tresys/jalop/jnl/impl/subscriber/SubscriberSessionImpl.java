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

package com.tresys.jalop.jnl.impl.subscriber;

import org.apache.log4j.Logger;
import org.beepcore.beep.core.ReplyListener;

import com.tresys.jalop.jnl.RecordType;
import com.tresys.jalop.jnl.Role;
import com.tresys.jalop.jnl.Session;
import com.tresys.jalop.jnl.Subscriber;
import com.tresys.jalop.jnl.SubscriberSession;

/**
 * Implementation of a {@link SubscriberSession}. This represents a {@link Session}
 * that is receiving JALoP records from a remote JALoP Network Store.
 */
public class SubscriberSessionImpl implements SubscriberSession {

	private static final Logger log = Logger.getLogger(SubscriberSessionImpl.class);

	private final RecordType recordType;
	private final String digestMethod;
	private final String xmlEncoding;
	private final ReplyListener listener;
	private final Subscriber subscriber;
	private volatile int pendingDigestTimeoutSeconds;
	private volatile int pendingDigestMax;
	private volatile boolean errored;

	/**
	 * Create a {@link SubscriberSessionImpl} object.
	 *
	 * @param recordType
	 *            The type of JAL records this {@link Session} transfers.
	 * @param subscriber
	 *            The {@link Subscriber} associated with this {@link Session}.
	 * @param digestMethod
	 *            The digest method to be used on this {@link Session}.
	 * @param xmlEncoding
	 *            The XML encoding to be used on this {@link Session}.
	 * @param pendingDigestTimeoutSeconds
	 *            The time to wait, in seconds before sending a "digest"
	 *            message.
	 * @param pendingDigestMax
	 *            The maximum number of digests to queue.
	 */
	public SubscriberSessionImpl(final RecordType recordType, final Subscriber subscriber,
			final String digestMethod, final String xmlEncoding, final int pendingDigestTimeoutSeconds,
			final int pendingDigestMax) {

		if(recordType == null || recordType.equals(RecordType.Unset)) {
			throw new IllegalArgumentException("'recordType' cannot be null or Unset.");
		}

		if(subscriber == null) {
			throw new IllegalArgumentException("'subscriber' cannot be null.");
		}

		if(digestMethod == null || digestMethod.trim().isEmpty()) {
			throw new IllegalArgumentException("'digestMethod' is required.");
		}

		if(xmlEncoding == null || xmlEncoding.trim().isEmpty()) {
			throw new IllegalArgumentException("'xmlEncoding' is required.");
		}

		if(pendingDigestTimeoutSeconds <= 0) {
			throw new IllegalArgumentException("'pendingDigestTimeoutSeconds' "
					+ "must be a positive number.");
		}

		if(pendingDigestMax <= 0) {
			throw new IllegalArgumentException("'pendingDigestMax' "
					+ "must be a positive number.");
		}

		this.recordType = recordType;
		this.subscriber = subscriber;
		this.listener = null;
		this.digestMethod = digestMethod.trim();
		this.xmlEncoding = xmlEncoding.trim();
		this.pendingDigestMax = pendingDigestMax;
		this.pendingDigestTimeoutSeconds = pendingDigestTimeoutSeconds;
	}

	@Override
	public RecordType getRecordType() {
		return this.recordType;
	}

	@Override
	public void setErrored() {
		this.errored = true;
	}

	@Override
	public boolean isOk() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getDigestMethod() {
		return this.digestMethod;
	}

	@Override
	public String getXmlEncoding() {
		return this.xmlEncoding;
	}

	/**
	 * @return the pendingDigestTimeoutSeconds
	 */
	public long getPendingDigestTimeoutSeconds() {
		return this.pendingDigestTimeoutSeconds;
	}

	/**
	 * @return the pendingDigestMax
	 */
	public int getPendingDigestMax() {
		return this.pendingDigestMax;
	}

	@Override
	public Role getRole() {
		return Role.Subscriber;
	}

	/**
	 * @return the listener
	 */
	public ReplyListener getListener() {
		return this.listener;
	}

	/**
	 * @return the subscriber
	 */
	public Subscriber getSubscriber() {
		return this.subscriber;
	}

	@Override
	public void setDigestTimeout(final int pendingDigestTimeoutSeconds) {

		if(pendingDigestTimeoutSeconds <= 0) {
			throw new IllegalArgumentException("'pendingDigestTimeoutSeconds' "
					+ "must be a positive number.");
		}

		this.pendingDigestTimeoutSeconds = pendingDigestTimeoutSeconds;
	}

	@Override
	public void setPendingDigestMax(final int pendingDigestMax) {

		if(pendingDigestMax <= 0) {
			throw new IllegalArgumentException("'pendingDigestMax' "
					+ "must be a positive number.");
		}

		this.pendingDigestMax = pendingDigestMax;
	}

}
