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

package com.tresys.jalop.jnl.impl.publisher;

import java.net.InetAddress;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.beepcore.beep.core.BEEPError;
import org.beepcore.beep.core.BEEPException;
import org.beepcore.beep.core.Channel;

import com.tresys.jalop.jnl.Publisher;
import com.tresys.jalop.jnl.PublisherSession;
import com.tresys.jalop.jnl.RecordType;
import com.tresys.jalop.jnl.Role;
import com.tresys.jalop.jnl.Session;
import com.tresys.jalop.jnl.exceptions.JNLException;
import com.tresys.jalop.jnl.impl.ContextImpl;
import com.tresys.jalop.jnl.impl.DigestRequestHandler;
import com.tresys.jalop.jnl.impl.SessionImpl;

/**
 * Implementation of a {@link PublisherSession}. This represents a
 * {@link Session} that is sending JALoP records to a remote JALoP Network
 * Store.
 */
public class PublisherSessionImpl extends SessionImpl implements
		PublisherSession, Runnable {

	private static final Logger log = Logger
			.getLogger(PublisherSessionImpl.class);

	private final Publisher publisher;
	private final ContextImpl contextImpl;
	private final Map<String, byte[]> digestMap;

	/**
	 * The MessageDigest to use for calculating the JALoP digest.
	 */
	private final MessageDigest md;

	/**
	 * Create a {@link PublisherSessionImpl} object.
	 *
	 * @param remoteAddress
	 *            The InetAddress used for the transfers.
	 * @param recordType
	 *            The type of JAL records this {@link Session} transfers.
	 * @param publisher
	 *            The {@link Publisher} associated with this {@link Session}.
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
	public PublisherSessionImpl(final InetAddress remoteAddress,
			final RecordType recordType, final Publisher publisher,
			final String digestMethod, final String xmlEncoding,
			final int channelNum, final org.beepcore.beep.core.Session session,
			final ContextImpl contextImpl) {

		super(remoteAddress, recordType, digestMethod, xmlEncoding,
				channelNum, session);

		if (publisher == null) {
			throw new IllegalArgumentException("'publisher' cannot be null.");
		}

		this.publisher = publisher;
		this.contextImpl = contextImpl;
		this.digestMap = new HashMap<String, byte[]>();

		try {
			final MessageDigest md = MessageDigest.getInstance(getDigestType(digestMethod.trim()));
			this.md = md;
		} catch (final NoSuchAlgorithmException e) {
			throw new IllegalArgumentException(
					"'digestMethod' must be a valid DigestMethod", e);
		}

	}

	@Override
	public Role getRole() {
		return Role.Publisher;
	}

	/**
	 * @return the publisher
	 */
	public Publisher getPublisher() {
		return this.publisher;
	}

	/**
	 * @return the md
	 */
	public MessageDigest getMd() {
		return md;
	}

	@Override
	public void run() {

		if (log.isDebugEnabled()) {
			log.debug("PublisherSessionImpl running");
		}

		try {
			final Channel digestChannel = createDigestChannel();
			digestChannel.setRequestHandler(new DigestRequestHandler(this.getRecordType(), this.contextImpl, this));

		} catch (final BEEPError e) {
			if (log.isEnabledFor(Level.ERROR)) {
				log.error(e.getMessage());
			}
			setErrored();
		} catch (final BEEPException e) {
			if (log.isEnabledFor(Level.ERROR)) {
				log.error(e.getMessage());
			}
			setErrored();
		}
	}

	/**
	 * Get the locally calculated digest associated with the given serialId and
	 * then remove it from the map.
	 *
	 * @param serialId
	 * 				A String which is the serial id for the calculated digest
	 * @return
	 * 				The local digest associated with the serialId.
	 */
	public byte[] fetchAndRemoveDigest(final String serialId) {
		synchronized(this.digestMap) {
			final byte[] localDigest = this.digestMap.get(serialId);
			this.digestMap.remove(serialId);
			return localDigest;
		}
	}

	/**
	 * Add a locally calculated digest to the set of tracked digests.
	 *
	 * @param serialId
	 * 				A String which is the serial id for the calculated digest
	 * @param localDigest
	 * 				A byte[] which is the digest calculated locally by the publisher
	 * @throws JNLException
	 * 				If attempting to add a serialId that already exists in the map.
	 */
	public void addDigest(final String serialId, final byte[] localDigest)
			throws JNLException {
		synchronized(this.digestMap) {
			if (this.digestMap.containsKey(serialId)) {
				throw new JNLException(
						"Attempting to add multiple digests for the same serialId");
			}
			this.digestMap.put(serialId, localDigest);
		}
	}

}