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

package com.tresys.jalop.jnl.impl;

import java.net.InetAddress;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;

import javax.xml.crypto.dsig.DigestMethod;

import org.apache.log4j.Logger;
import org.beepcore.beep.core.BEEPError;
import org.beepcore.beep.core.BEEPException;
import org.beepcore.beep.core.Channel;

import com.tresys.jalop.jnl.RecordType;
import com.tresys.jalop.jnl.Session;
import com.tresys.jalop.jnl.impl.messages.Utils;

public abstract class SessionImpl implements Session {

	private static final Logger log = Logger
			.getLogger(SessionImpl.class);

	private final RecordType recordType;
	private final String digestMethod;
	private final String xmlEncoding;
	protected final InetAddress address;
	private volatile boolean errored;
	protected final int channelNum;
	protected final org.beepcore.beep.core.Session session;
	protected Channel digestChannel;

	/**
	 * Create a {@link SessionImpl} object.
	 *
	 * @param remoteAddress
	 *            The InetAddress used for the transfers.
	 * @param recordType
	 *            The type of JAL records this {@link Session} deals with.
	 * @param digestMethod
	 *            The digest method to be used on this {@link Session}.
	 * @param xmlEncoding
	 *            The XML encoding to be used on this {@link Session}.
	 * @param pendingDigestTimeoutSeconds
	 *            The time to wait, in seconds before sending a "digest"
	 *            message.
	 * @param pendingDigestMax
	 *            The maximum number of digests to queue.
	 * @throws InvalidAlgorithmParameterException
	 * @throws NoSuchAlgorithmException
	 */
	public SessionImpl(final InetAddress remoteAddress,
			final RecordType recordType, final String digestMethod,
			final String xmlEncoding, final int channelNum,
			final org.beepcore.beep.core.Session session) {

		if (remoteAddress == null) {
			throw new IllegalArgumentException(
					"SessionImpl must be created with an address");
		}
		this.address = remoteAddress;

		if (recordType == null || recordType.equals(RecordType.Unset)) {
			throw new IllegalArgumentException(
					"'recordType' cannot be null or Unset.");
		}

		if (digestMethod == null || digestMethod.trim().isEmpty()) {
			throw new IllegalArgumentException("'digestMethod' is required.");
		}

		if (xmlEncoding == null || xmlEncoding.trim().isEmpty()) {
			throw new IllegalArgumentException("'xmlEncoding' is required.");
		}

		if (session == null) {
			throw new IllegalArgumentException("'session' " + "cannot be null.");
		}

		this.recordType = recordType;
		this.digestMethod = digestMethod.trim();
		this.xmlEncoding = xmlEncoding.trim();
		this.channelNum = channelNum;
		this.session = session;
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

		return (this.session.getState() == org.beepcore.beep.core.Session.SESSION_STATE_ACTIVE && !this.errored);
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

	protected String getDigestType(final String algorithm) {

		if (DigestMethod.SHA256.equals(algorithm)) {
			return "SHA-256";
		} else if (DigestMethod.SHA512.equals(algorithm)) {
			return "SHA-512";
		} else if ("http://www.w3.org/2001/04/xmldsig-more#sha384"
				.equals(algorithm)) {
			return "SHA-384";
		}
		return "";
	}

	/**
	 * @return the digestChannel
	 */
	public Channel getDigestChannel() {
		return digestChannel;
	}

	/**
	 * @param digestChannel the digestChannel to set
	 */
	public void setDigestChannel(final Channel digestChannel) {
		this.digestChannel = digestChannel;
	}

	/**
	 * Creates a digest channel if one hasn't been set yet.
	 *
	 * @return
	 *            the {@link Channel} which is the digest channel
	 * @throws BEEPError
	 * @throws BEEPException
	 */
	protected Channel createDigestChannel()
			throws BEEPError, BEEPException {
		Channel digestChannel = this.getDigestChannel();
		if(digestChannel == null) {

			if(log.isDebugEnabled()) {
				log.debug("creating new digest channel");
			}

			digestChannel = this.session.startChannel(
					ContextImpl.URI, false, Utils.DGST_CHAN_FORMAT_STR
							+ this.channelNum);
			this.setDigestChannel(digestChannel);
		}
		return digestChannel;
	}

}
