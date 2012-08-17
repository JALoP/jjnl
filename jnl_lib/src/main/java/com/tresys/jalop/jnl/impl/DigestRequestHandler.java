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

import java.util.Arrays;

import javax.xml.bind.DatatypeConverter;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.beepcore.beep.core.BEEPException;
import org.beepcore.beep.core.InputDataStreamAdapter;
import org.beepcore.beep.core.MessageMSG;
import org.beepcore.beep.core.RequestHandler;

import com.tresys.jalop.jnl.DigestStatus;
import com.tresys.jalop.jnl.Publisher;
import com.tresys.jalop.jnl.RecordType;
import com.tresys.jalop.jnl.exceptions.JNLException;
import com.tresys.jalop.jnl.exceptions.MissingMimeHeaderException;
import com.tresys.jalop.jnl.exceptions.UnexpectedMimeValueException;
import com.tresys.jalop.jnl.impl.messages.DigestMessage;
import com.tresys.jalop.jnl.impl.messages.Utils;
import com.tresys.jalop.jnl.impl.publisher.PublisherSessionImpl;

/**
 * Class for receiving digest messages from the subscriber.
 */
public class DigestRequestHandler implements RequestHandler {

	static Logger log = Logger.getLogger(DigestRequestHandler.class);

	RecordType recordType;
	ContextImpl contextImpl;
	PublisherSessionImpl sess;

	/**
	 * Create a new {@link DigestRequestHandler}.
	 *
	 * @param recordType
	 *            The type of record.
	 * @param contextImpl
	 *            The {@link ContextImpl} that initiated the connection.
	 */
	public DigestRequestHandler(final RecordType recordType,
			final ContextImpl contextImpl, final PublisherSessionImpl sess) {

		this.recordType = recordType;
		this.contextImpl = contextImpl;
		this.sess = sess;
	}

	@Override
	public void receiveMSG(final MessageMSG message) {

		if (log.isDebugEnabled()) {
			log.debug("received message in DigestRequestHandler");
		}

		final InputDataStreamAdapter data = message.getDataStream()
			.getInputStream();

		try {

			final DigestMessage msg = Utils.processDigestMessage(data);
			final Publisher publisher = this.contextImpl.getPublisher();

			for(final String serialId : msg.getMap().keySet()) {

				final byte[] localDigest = this.sess.getDigestPair(serialId).getLocalDigest();
				final byte[] peerDigest =  DatatypeConverter.parseHexBinary(msg.getMap().get(serialId));

				DigestStatus ds;
				if(Arrays.equals(localDigest, peerDigest)) {
					ds = DigestStatus.Confirmed;
				} else {
					ds = DigestStatus.Invalid;
				}

				this.sess.updateDigestPair(serialId, peerDigest, ds);
			}

			publisher.notifyPeerDigest(this.sess, this.sess.getDigestPairsMap());

		} catch (final BEEPException e) {
			if (log.isEnabledFor(Level.ERROR)) {
				log.error("Error receiving message: " + e.getMessage());
			}
		} catch (final MissingMimeHeaderException e) {
			if (log.isEnabledFor(Level.ERROR)) {
				log.error("Error - Missing Mime Header: " + e.getMessage());
			}
		} catch (final UnexpectedMimeValueException e) {
			if (log.isEnabledFor(Level.ERROR)) {
				log.error("Error - Unexpected value: " + e.getMessage());
			}
		} catch (final JNLException e) {
			if (log.isEnabledFor(Level.ERROR)) {
				log.error("Error getting the PublisherSession: " + e.getMessage());
			}
		}
	}

}