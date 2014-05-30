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
package com.tresys.jalop.jnl.impl;

import java.util.Map;
import java.util.Set;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.beepcore.beep.core.AbortChannelException;
import org.beepcore.beep.core.BEEPException;
import org.beepcore.beep.core.InputDataStreamAdapter;
import org.beepcore.beep.core.Message;
import org.beepcore.beep.core.OutputDataStream;
import org.beepcore.beep.core.ReplyListener;

import com.tresys.jalop.jnl.DigestStatus;
import com.tresys.jalop.jnl.exceptions.MissingMimeHeaderException;
import com.tresys.jalop.jnl.exceptions.UnexpectedMimeValueException;
import com.tresys.jalop.jnl.impl.messages.DigestResponse;
import com.tresys.jalop.jnl.impl.messages.Utils;
import com.tresys.jalop.jnl.impl.subscriber.SubscriberSessionImpl;

/**
 * Listener class to be used for digest messages. This will
 * listen for replies after a message is sent.
 */
public class DigestListener implements ReplyListener {

	private final static Logger log = Logger.getLogger(DigestListener.class);

	private final SubscriberSessionImpl subscriberSession;
	private final Map<String, String> digestsSent;

	/**
	 * Create a new {@link DigestListener}.
	 *
	 * @param subscriberSession
	 *            The {@link SubscriberSessionImpl} associated with this listener.
	 * @param digestsSent
	 *            A Map of nonces to digests that have been sent to the publisher.
	 */
	public DigestListener(final SubscriberSessionImpl subscriberSession, final Map<String, String> digestsSent) {
		this.subscriberSession = subscriberSession;
		this.digestsSent = digestsSent;
	}

	@Override
	public void receiveRPY(final Message message) throws AbortChannelException {

		if(log.isDebugEnabled()) {
			log.debug("DigestListener receiveRPY");
		}

		final InputDataStreamAdapter data = message.getDataStream().getInputStream();

		try {

			final DigestResponse msg = Utils.processDigestResponse(data);
			final Map<String, DigestStatus> statusMap = msg.getMap();

			final Set<String> nonces = statusMap.keySet();
			String maxNonce = "";

			for(final String nonce : nonces) {
				if(this.digestsSent.containsKey(nonce)) {
					if(nonce.compareTo(maxNonce) > 0) {
						maxNonce = nonce;
					}
					this.digestsSent.remove(nonce);
				}
			}
			//Add back in any digests that were sent but didn't receive a response
			if(!this.digestsSent.isEmpty()) {
				this.subscriberSession.addAllDigests(this.digestsSent);
			}

			this.subscriberSession.getSubscriber().notifyDigestResponse(this.subscriberSession, statusMap);

			final OutputDataStream ods = Utils.createSyncMessage(maxNonce);
			message.getChannel().sendMSG(ods, this);

		} catch (final BEEPException e) {
			if(log.isEnabledFor(Level.ERROR)) {
				log.error("Error receiving reply: " + e.getMessage());
			}
			throw new AbortChannelException(e.getMessage());
		} catch (final MissingMimeHeaderException e) {
			if(log.isEnabledFor(Level.ERROR)) {
				log.error("Error - Missing Mime Header: " + e.getMessage());
			}
			throw new AbortChannelException(e.getMessage());
		} catch (final UnexpectedMimeValueException e) {
			if(log.isEnabledFor(Level.ERROR)) {
				log.error("Error - Unexpected value: " + e.getMessage());
			}
			throw new AbortChannelException(e.getMessage());
		}
	}

	@Override
	public void receiveERR(final Message message)
			throws AbortChannelException {

		if(log.isEnabledFor(Level.ERROR)) {
			log.error("DigestListener received an error. Closing the channel.");
		}
		throw new AbortChannelException("DigestListener received ERR");

	}

	@Override
	public void receiveANS(final Message message)
			throws AbortChannelException {

		if(log.isEnabledFor(Level.ERROR)) {
			log.error("DigestListener received ANS which shouldn't happen.");
		}
		throw new AbortChannelException("DigestListener should not receive ANS");
	}

	@Override
	public void receiveNUL(final Message message)
			throws AbortChannelException {

		//Received NUL from sync message - do nothing
		if(log.isDebugEnabled()) {
			log.debug("DigestListener received NUL.");
		}
	}

}
