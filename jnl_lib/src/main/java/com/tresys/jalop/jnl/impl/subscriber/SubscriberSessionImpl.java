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

package com.tresys.jalop.jnl.impl.subscriber;

import java.io.InputStream;
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
import org.beepcore.beep.core.OutputDataStream;
import org.beepcore.beep.core.ReplyListener;

import com.tresys.jalop.jnl.RecordType;
import com.tresys.jalop.jnl.Role;
import com.tresys.jalop.jnl.Session;
import com.tresys.jalop.jnl.Subscriber;
import com.tresys.jalop.jnl.SubscriberSession;
import com.tresys.jalop.jnl.impl.DigestListener;
import com.tresys.jalop.jnl.impl.SessionImpl;
import com.tresys.jalop.jnl.impl.SubscriberANSHandler;
import com.tresys.jalop.jnl.impl.messages.Utils;

/**
 * Implementation of a {@link SubscriberSession}. This represents a
 * {@link Session} that is receiving JALoP records from a remote JALoP Network
 * Store.
 */
public class SubscriberSessionImpl extends SessionImpl implements
		SubscriberSession, Runnable {

	private static final Logger log = Logger
			.getLogger(SubscriberSessionImpl.class);

	private final Subscriber subscriber;
	protected ReplyListener listener;
	protected volatile int pendingDigestTimeoutSeconds;
	protected volatile int pendingDigestMax;
	protected Map<String, String> digestMap;
	private long journalResumeOffset;
	private InputStream journalResumeIS;

	/**
	 * Create a {@link SubscriberSessionImpl} object.
	 *
	 * @param remoteAddress
	 *            The InetAddress used for the transfers.
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
	public SubscriberSessionImpl(final InetAddress remoteAddress,
			final RecordType recordType, final Subscriber subscriber,
			final String digestMethod, final String xmlEncoding,
			final int pendingDigestTimeoutSeconds, final int pendingDigestMax,
			final int channelNum, final org.beepcore.beep.core.Session session) {

		super(remoteAddress, recordType, digestMethod, xmlEncoding,
				channelNum, session);

		if (subscriber == null) {
			throw new IllegalArgumentException("'subscriber' cannot be null.");
		}

		if (pendingDigestTimeoutSeconds <= 0) {
			throw new IllegalArgumentException("'pendingDigestTimeoutSeconds' "
					+ "must be a positive number.");
		}

		if (pendingDigestMax <= 0) {
			throw new IllegalArgumentException("'pendingDigestMax' "
					+ "must be a positive number.");
		}

		try {
			this.listener = new SubscriberANSHandler(
					MessageDigest
							.getInstance(getDigestType(digestMethod.trim())),
					this);
		} catch (final NoSuchAlgorithmException e) {
			throw new IllegalArgumentException(
					"'digestMethod' must be a valid DigestMethod", e);
		}

		this.subscriber = subscriber;
		this.pendingDigestMax = pendingDigestMax;
		this.pendingDigestTimeoutSeconds = pendingDigestTimeoutSeconds;
		this.digestMap = new HashMap<String, String>();
		this.journalResumeOffset = 0;
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

	/**
	 * @return the journalResumeOffset
	 */
	public long getJournalResumeOffset() {
		return this.journalResumeOffset;
	}

	/**
	 * @param journalResumeOffset the journalResumeOffset to set
	 */
	public void setJournalResumeOffset(final long journalResumeOffset) {
		this.journalResumeOffset = journalResumeOffset;
	}

	/**
	 * @return the journalResumeIS
	 */
	public InputStream getJournalResumeIS() {
		return journalResumeIS;
	}

	/**
	 * @param journalResumeIS the {@link InputStream} to set
	 */
	public void setJournalResumeIS(final InputStream journalResumeIS) {
		this.journalResumeIS = journalResumeIS;
	}

	@Override
	public Role getRole() {
		return Role.Subscriber;
	}

	/**
	 * @return the subscriber
	 */
	public Subscriber getSubscriber() {
		return this.subscriber;
	}

	/**
	 * @return the listener
	 */
	public ReplyListener getListener() {
		return this.listener;
	}

	@Override
	public void setDigestTimeout(final int pendingDigestTimeoutSeconds) {

		if (pendingDigestTimeoutSeconds <= 0) {
			throw new IllegalArgumentException("'pendingDigestTimeoutSeconds' "
					+ "must be a positive number.");
		}

		this.pendingDigestTimeoutSeconds = pendingDigestTimeoutSeconds;
	}

	@Override
	public void setPendingDigestMax(final int pendingDigestMax) {

		if (pendingDigestMax <= 0) {
			throw new IllegalArgumentException("'pendingDigestMax' "
					+ "must be a positive number.");
		}

		this.pendingDigestMax = pendingDigestMax;
	}

	/**
	 * Adds a map of nonces and their related digests to the current map to
	 * be sent to the publisher
	 *
	 * @param toAdd
	 *            A map of nonces and digests to add to the map to be sent.
	 */
	public synchronized void addAllDigests(final Map<String, String> toAdd) {

		this.digestMap.putAll(toAdd);
		if (this.digestMap.size() >= this.pendingDigestMax) {
			synchronized (this) {
				this.notifyAll();
			}
		}
	}

	/**
	 * Adds a nonce and the related digest to a map to be sent to the
	 * publisher
	 *
	 * @param nonce
	 *            A String which is the nonce to be added to the map of
	 *            digests to send.
	 * @param digest
	 *            A String which is the digest for the nonce to be added to
	 *            the map of digests to send.
	 */
	public synchronized void addDigest(final String nonce,
			final String digest) {

		this.digestMap.put(nonce, digest);
		if (this.digestMap.size() >= this.pendingDigestMax) {
			synchronized (this) {
				this.notifyAll();
			}
		}
	}

	@Override
	public InetAddress getAddress() {
		return this.address;
	}

	@Override
	public void run() {

		if (log.isDebugEnabled()) {
			log.debug("SubscriberSessionImpl running");
		}

		try {

			final Channel digestChannel = createDigestChannel();

			while (this.isOk()) {

				if (this.digestMap.size() < this.pendingDigestMax) {
					synchronized (this) {
						final long waitTime = this.pendingDigestTimeoutSeconds * 1000;
						this.wait(waitTime);
					}
				}

				Map<String, String> digestsToSend = new HashMap<String, String>();
				synchronized (this) {
					if (this.digestMap.isEmpty()) {
						continue;
					}

					digestsToSend = this.digestMap;
					this.digestMap = new HashMap<String, String>();
				}

				final OutputDataStream digestOds = Utils
						.createDigestMessage(digestsToSend);

				digestChannel.sendMSG(digestOds, new DigestListener(this,
						digestsToSend));
			}

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
		} catch (final InterruptedException e) {
			if (log.isEnabledFor(Level.ERROR)) {
				log.error(e.getMessage());
			}
			setErrored();
		}
	}

}
