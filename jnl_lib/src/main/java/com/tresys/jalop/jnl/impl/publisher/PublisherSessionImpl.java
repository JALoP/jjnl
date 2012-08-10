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

import com.tresys.jalop.jnl.DigestPair;
import com.tresys.jalop.jnl.DigestStatus;
import com.tresys.jalop.jnl.Publisher;
import com.tresys.jalop.jnl.PublisherSession;
import com.tresys.jalop.jnl.RecordType;
import com.tresys.jalop.jnl.Role;
import com.tresys.jalop.jnl.Session;
import com.tresys.jalop.jnl.exceptions.JNLException;
import com.tresys.jalop.jnl.impl.ContextImpl;
import com.tresys.jalop.jnl.impl.DigestPairImpl;
import com.tresys.jalop.jnl.impl.DigestRequestHandler;
import com.tresys.jalop.jnl.impl.SessionImpl;
import com.tresys.jalop.jnl.impl.messages.Utils;

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
	private final Map<String, DigestPairImpl> digestPairsMap;

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
		this.digestPairsMap = new HashMap<String, DigestPairImpl>();

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
			final Channel digestChannel = this.session.startChannel(
					ContextImpl.URI, false, Utils.DGST_CHAN_FORMAT_STR
							+ this.channelNum);

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
	 * Add a digestPair to the set of tracked digests.
	 *
	 * @param serialId
	 * 				A String which is the serial id for the calculated digests
	 * @param localDigest
	 * 				A byte[] which is the digest calculated locally by the publisher
	 */
	public void addDigestPair(final String serialId,
			final byte[] localDigest) throws JNLException {

		synchronized (this.digestPairsMap) {
			if (this.digestPairsMap.containsKey(serialId)) {
				throw new JNLException(
						"Attempting to add multiple DigestPairs for the same serialId");
			}
			final DigestPairImpl dp = new DigestPairImpl(serialId, localDigest);
			this.digestPairsMap.put(serialId, dp);
		}
	}

	/**
	 * Update the DigestPair for the given serialId with the digest calculated
	 * by the peer and the status.
	 *
	 * @param serialId
	 * 				A String which is the serial id for the calculated digests
	 * @param peerDigest
	 * 				A byte[] which is the digest calculated remotely by the subscriber
	 * @param digestStatus
	 * 				The DigestStatus for this pair
	 * @throws JNLException
	 * 				If attempting to update a digestPair that doesn't exist in the map.
	 */
	public void updateDigestPair(final String serialId, final byte[] peerDigest,
			final DigestStatus digestStatus) throws JNLException {

		synchronized (this.digestPairsMap) {
			if (!this.digestPairsMap.containsKey(serialId)) {
				throw new JNLException(
						"Attempting to update a DigestPair that doesn't exist in the map.");
			}
			final DigestPairImpl dp = this.digestPairsMap.get(serialId);
			dp.setDigestStatus(digestStatus);
			dp.setPeerDigest(peerDigest);
		}
	}

	/**
	 * @return the digestPairsMap
	 */
	public Map<String, DigestPair> getDigestPairsMap() {
		final Map<String, DigestPair> map = new HashMap<String, DigestPair>();
		synchronized (this.digestPairsMap) {
			map.putAll(this.digestPairsMap);
		}
		return map;
	}

	/**
	 * Returns the DigestPair for the given serialId
	 *
	 * @param serialId
	 * 				The serialId to find the DigestPair for.
	 * @return
	 * 				The DigestPair that is mapped to the give serialId.
	 */
	public DigestPair getDigestPair(final String serialId) {
		synchronized(this.digestPairsMap) {
			return this.digestPairsMap.get(serialId);
		}
	}

}