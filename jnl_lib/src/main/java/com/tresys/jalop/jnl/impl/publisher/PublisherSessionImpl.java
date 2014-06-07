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

package com.tresys.jalop.jnl.impl.publisher;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.beepcore.beep.core.BEEPError;
import org.beepcore.beep.core.BEEPException;
import org.beepcore.beep.util.BufferSegment;
import org.beepcore.beep.core.Channel;
import org.beepcore.beep.core.MessageMSG;

import com.tresys.jalop.jnl.Publisher;
import com.tresys.jalop.jnl.PublisherSession;
import com.tresys.jalop.jnl.RecordType;
import com.tresys.jalop.jnl.Role;
import com.tresys.jalop.jnl.Session;
import com.tresys.jalop.jnl.SourceRecord;
import com.tresys.jalop.jnl.exceptions.JNLException;
import com.tresys.jalop.jnl.exceptions.MissingMimeHeaderException;
import com.tresys.jalop.jnl.exceptions.UnexpectedMimeValueException;
import com.tresys.jalop.jnl.impl.ContextImpl;
import com.tresys.jalop.jnl.impl.DigestRequestHandler;
import com.tresys.jalop.jnl.impl.JNLOutputDataStream;
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
	private final Map<String, byte[]> digestMap;

	public MessageMSG msg;

	static final int BUFFER_SIZE = 4096;
	static final int MAX_BUFFERS = 10;

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
	 * Get the locally calculated digest associated with the given nonce and
	 * then remove it from the map.
	 *
	 * @param nonce
	 * 				A String which is the nonce for the calculated digest
	 * @return
	 * 				The local digest associated with the nonce.
	 */
	public byte[] fetchAndRemoveDigest(final String nonce) {
		synchronized(this.digestMap) {
			final byte[] localDigest = this.digestMap.get(nonce);
			this.digestMap.remove(nonce);
			return localDigest;
		}
	}

	/**
	 * Add a locally calculated digest to the set of tracked digests.
	 *
	 * @param nonce
	 * 				A String which is the nonce for the calculated digest
	 * @param localDigest
	 * 				A byte[] which is the digest calculated locally by the publisher
	 * @throws JNLException
	 * 				If attempting to add a nonce that already exists in the map.
	 */
	public void addDigest(final String nonce, final byte[] localDigest)
			throws JNLException {
		synchronized(this.digestMap) {
			if (this.digestMap.containsKey(nonce)) {
				throw new JNLException(
						"Attempting to add multiple digests for the same nonce");
			}
			this.digestMap.put(nonce, localDigest);
		}
	}

	public void sendRecord(final SourceRecord rec) {
		int offset = 0; // TODO: This should be set elsewhere for journal resume
		String messageType = null;
		String payloadLengthHeader = null;
		switch(this.getRecordType()) {
		case Log:
			messageType = Utils.MSG_LOG;
			payloadLengthHeader = Utils.HDRS_LOG_LEN;
			break;
		case Audit:
			messageType = Utils.MSG_AUDIT;
			payloadLengthHeader = Utils.HDRS_AUDIT_LEN;
			break;
		case Journal:
			messageType = Utils.MSG_JOURNAL;
			payloadLengthHeader = Utils.HDRS_JOURNAL_LEN;
			break;
		default:
			if (log.isEnabledFor(Level.ERROR)) {
				log.error("Publisher session has bad record type");
			}
			return;
		}

		final MessageDigest md = getMd();

		final String nonce = rec.getNonce();

		try {

			final org.beepcore.beep.core.MimeHeaders mh = new org.beepcore.beep.core.MimeHeaders();
			mh.setContentType(Utils.CT_JALOP);
			mh.setHeader(Utils.HDRS_NONCE, nonce);
			mh.setHeader(Utils.HDRS_MESSAGE, messageType);
			mh.setHeader(payloadLengthHeader, String.valueOf(rec.getPayloadLength()));
			mh.setHeader(Utils.HDRS_SYS_META_LEN, String.valueOf(rec.getSysMetaLength()));
			mh.setHeader(Utils.HDRS_APP_META_LEN, String.valueOf(rec.getAppMetaLength()));

			final JNLOutputDataStream ods = new JNLOutputDataStream(mh, MAX_BUFFERS);
			msg.sendANS(ods);

			int bytesRead = 0;

			final List<InputStream> inputStreamList = new ArrayList<InputStream>();
			inputStreamList.add(rec.getSysMetadata());
			inputStreamList.add(new ByteArrayInputStream(Utils.BREAK.getBytes("utf-8")));
			inputStreamList.add(rec.getAppMetadata());
			inputStreamList.add(new ByteArrayInputStream(Utils.BREAK.getBytes("utf-8")));
			inputStreamList.add(rec.getPayload());
			inputStreamList.add(new ByteArrayInputStream(Utils.BREAK.getBytes("utf-8")));

			InputStream inStream = null;
			boolean shouldDigest = false;

			while(!inputStreamList.isEmpty()) {
				byte[] buffer =  new byte[BUFFER_SIZE];
				inStream = inputStreamList.remove(0);
				//every other stream is a break and shouldn't be included in the digest
				shouldDigest = !shouldDigest;

				if(inStream == null) {
					continue;
				}

				// When only one is remaining, that is the final BREAK, so we are on the payload.
				// For Journal Resume, we may have already sent part of the payload
				if(inputStreamList.size() == 1 && shouldDigest && offset > 0) {
					// need to digest payload up to offset but not send that part
					int toRead;
					while((toRead = (int) (offset > BUFFER_SIZE ? BUFFER_SIZE : offset)) > 0) {
						bytesRead = inStream.read(buffer, 0, toRead);
						md.update(buffer, 0, bytesRead);
						offset -= bytesRead;
						buffer = new byte[BUFFER_SIZE];
					}
				}

				while((bytesRead = inStream.read(buffer)) > -1) {

					if(shouldDigest) {
						md.update(buffer, 0, bytesRead);
					}

					ods.add(new BufferSegment(buffer, 0, bytesRead));
					buffer = new byte[BUFFER_SIZE];
				}
			}

			ods.setComplete();

			final byte[] digest = md.digest();

			this.addDigest(nonce, digest);
			publisher.notifyDigest(this, nonce, digest);
		} catch (final BEEPException e) {
			if (log.isEnabledFor(Level.ERROR)) {
				log.error("Error receiving message: " + e.getMessage());
			}
			sendERR(msg);
		} catch (final MissingMimeHeaderException e) {
			if (log.isEnabledFor(Level.ERROR)) {
				log.error("Error - Missing Mime Header: " + e.getMessage());
			}
			sendERR(msg);
		} catch (final UnexpectedMimeValueException e) {
			if (log.isEnabledFor(Level.ERROR)) {
				log.error("Error - Unexpected value: " + e.getMessage());
			}
			sendERR(msg);
		} catch (final JNLException e) {
			if (log.isEnabledFor(Level.ERROR)) {
				log.error("Error getting the PublisherSession: " + e.getMessage());
			}
			sendERR(msg);
		} catch (final IOException e) {
			if (log.isEnabledFor(Level.ERROR)) {
				log.error("Error reading the input: " + e.getMessage());
			}
			sendERR(msg);
		}

	} 
	public void sendERR(final MessageMSG message) {
		try {
			message.sendERR(new BEEPError(BEEPError.CODE_REQUESTED_ACTION_ABORTED));
		} catch (final BEEPException e) {
			if (log.isEnabledFor(Level.ERROR)) {
				log.error("Error trying to send Error message: " + e.getMessage());
			}
		}
	}

	public void complete() {
		try {
			msg.sendNUL();
		} catch (final BEEPException e) {
			if (log.isEnabledFor(Level.ERROR)) {
				log.error("Error sending NUL");
			}
		}
	}
}
