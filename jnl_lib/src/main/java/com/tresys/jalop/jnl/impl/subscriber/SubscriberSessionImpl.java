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
import com.tresys.jalop.jnl.impl.ContextImpl;
import com.tresys.jalop.jnl.impl.DigestListener;
import com.tresys.jalop.jnl.impl.messages.Utils;

/**
 * Implementation of a {@link SubscriberSession}. This represents a {@link Session}
 * that is receiving JALoP records from a remote JALoP Network Store.
 */
public class SubscriberSessionImpl implements SubscriberSession, Runnable {

	private static final Logger log = Logger.getLogger(SubscriberSessionImpl.class);

	private final RecordType recordType;
	private final String digestMethod;
	private final String xmlEncoding;
	private final ReplyListener listener;
	private final Subscriber subscriber;
	private volatile int pendingDigestTimeoutSeconds;
	private volatile int pendingDigestMax;
	private volatile boolean errored;
	private final int channelNum;
	private final org.beepcore.beep.core.Session session;
	private Map<String, String> digestMap;

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
			final int pendingDigestMax, int channelNum, org.beepcore.beep.core.Session session) {

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

		if(session == null) {
			throw new IllegalArgumentException("'session' "
					+ "cannot be null.");
		}

		this.recordType = recordType;
		this.subscriber = subscriber;
		this.listener = null;
		this.digestMethod = digestMethod.trim();
		this.xmlEncoding = xmlEncoding.trim();
		this.pendingDigestMax = pendingDigestMax;
		this.pendingDigestTimeoutSeconds = pendingDigestTimeoutSeconds;
		this.channelNum = channelNum;
		this.session = session;
		this.digestMap = new HashMap<String, String>();
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

		return (this.session.getState() == org.beepcore.beep.core.Session.SESSION_STATE_ACTIVE
				&& !this.errored);
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

	/**
	 * @return the channelNum
	 */
	public int getChannelNum() {
		return channelNum;
	}

	/**
	 * @return the session
	 */
	public org.beepcore.beep.core.Session getSession() {
		return session;
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

	/**
	 * Adds a map of serialIds and their related digests to the current map
	 * to be sent to the publisher
	 *
	 * @param toAdd
	 *            A map of serialIDs and digests to add to the map to be sent.
	 */
	public synchronized void addAllDigests(Map<String, String> toAdd) {

		this.digestMap.putAll(toAdd);
		if(this.digestMap.size() >= this.pendingDigestMax) {
			synchronized(this) {
				this.notifyAll();
			}
		}
	}

	/**
	 * Adds a serialId and the related digest to a map to be sent to the publisher
	 *
	 * @param serialId
	 *            A String which is the serialId to be added to the map of
	 *            digests to send.
	 * @param digest
	 *            A String which is the digest for the serialId to be added
	 *            to the map of digests to send.
	 */
	public synchronized void addDigest(String serialId, String digest) {

		this.digestMap.put(serialId, digest);
		if(this.digestMap.size() >= this.pendingDigestMax) {
			synchronized(this) {
				this.notifyAll();
			}
		}
	}

	@Override
	public void run() {

		if(log.isDebugEnabled()) {
			log.debug("SubscriberSessionImpl running");
		}

		try {
			final Channel digestChannel = this.session.startChannel(
					ContextImpl.URI, false, Utils.DGST_CHAN_FORMAT_STR + this.channelNum);

			while(this.isOk()) {

				if(this.digestMap.size() < this.pendingDigestMax) {
					synchronized(this) {
						long waitTime = this.pendingDigestTimeoutSeconds * 1000;
						this.wait(waitTime);
					}
				}

				Map<String, String> digestsToSend = new HashMap<String, String>();
				synchronized(this) {
					if(this.digestMap.isEmpty()) {
						continue;
					}

					digestsToSend = this.digestMap;
					this.digestMap = new HashMap<String, String>();
				}

				final OutputDataStream digestOds = Utils.createDigestMessage(digestsToSend);
				digestChannel.sendMSG(digestOds, new DigestListener(this, digestsToSend));
			}

		} catch (BEEPError e) {
			if(log.isEnabledFor(Level.ERROR)) {
				log.error(e.getMessage());
			}
			setErrored();
		} catch (BEEPException e) {
			if(log.isEnabledFor(Level.ERROR)) {
				log.error(e.getMessage());
			}
			setErrored();
		} catch (InterruptedException e) {
			if(log.isEnabledFor(Level.ERROR)) {
				log.error(e.getMessage());
			}
			setErrored();
		}
	}

}
