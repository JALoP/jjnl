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
import java.util.HashMap;
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

import java.io.IOException;

/**
 * Listener class to be used for digest messages. This will
 * listen for replies after a message is sent.
 */
public class DigestListener implements ReplyListener {

	private final static Logger log = Logger.getLogger(DigestListener.class);

	private final SubscriberSessionImpl subscriberSession;
	private final Map<String, String> digestsSent;

	// Map of partially received payloads, with key being associated data channel
	private static Map<String, String> messagePayload = new HashMap<String, String>();

	/**
	 * Create a new {@link DigestListener}.
	 *
	 * @param subscriberSession
	 *            The {@link SubscriberSessionImpl} associated with this listener.
	 * @param digestsSent
	 *  		  A Map of nonces to digests that have been sent to the publisher.
	 */
	public DigestListener(final SubscriberSessionImpl subscriberSession, final Map<String, String> digestsSent) {
		this.subscriberSession = subscriberSession;
		this.digestsSent = digestsSent;
	}

	@Override
	public void receiveRPY(final Message message) throws AbortChannelException {

		final InputDataStreamAdapter data = message.getDataStream().getInputStream();

		try {
			// Read the avaialable payload
			final int numLeft = data.available();
			final int digestChannel = message.getChannel().getNumber();
			final int msgno = message.getMsgno();
			final String key = digestChannel + "-" + msgno;
			String newPayload = new String("");

			log.debug("receiveRPY for channel " + digestChannel + ", msgno " + msgno + ", isComplete " + data.isComplete() + ", numLeft " + numLeft);
			try {
				newPayload = data.readMessage();
			} catch (final IOException e) {
				e.printStackTrace();
			}
			log.trace("[" + newPayload + "]");

			if (this.messagePayload.containsKey(key)) {
				// Append payload to any previous partially received payloads and save
				final String previousPayload = this.messagePayload.get(key);
				this.messagePayload.put(key, previousPayload + newPayload);
			} else {
				// Otherwise save the new payload
				this.messagePayload.put(key, newPayload);
			}

			if (data.isComplete() == true) {
				final DigestResponse msg = Utils.processDigestResponse(data, this.messagePayload.get(key));
				final Map<String, DigestStatus> statusMap = msg.getMap();

				final Set<String> nonces = statusMap.keySet();

				for(final String nonce : nonces) {
					log.trace("Processing: " + nonce);
					if(this.digestsSent.containsKey(nonce)) {
						// Execute the notify digest callback which will take care of moving the record from temp to perm
						if (this.subscriberSession.getSubscriber().notifyDigestResponse(this.subscriberSession, nonce, statusMap.get(nonce))) {
							// For a confirmed digest, send a sync message and remove the nonce from the sent queue 																
							if(statusMap.get(nonce) == DigestStatus.Confirmed) {
								final OutputDataStream ods = Utils.createSyncMessage(nonce);
								message.getChannel().sendMSG(ods, this);
							}
							else {
								log.warn("Non-confirmed digest received: " + nonce + ", " + statusMap.get(nonce));
							}
							// As long as we didn't get a fatal response, remove the digest from the sent list.
							log.trace("Removing from digestsSent: " + nonce);
							this.digestsSent.remove(nonce);

						}
						else {
							log.error("notifyDigestResponse failure: " + nonce + ", " + statusMap.get(nonce));
							throw new AbortChannelException("Unrecoverable error in notifyDigestResponse");
						}
					}
					else {
						log.debug("Digest not found in digestsSent list: " + nonce);
					}
				}
				// Add back in any digests that were sent but didn't receive a response
				if(!this.digestsSent.isEmpty()) {
					log.debug("Reading digests with no response.");
					this.subscriberSession.addAllDigests(this.digestsSent);
				}
				// Complete message processed, so clear out the received message payload storage for this channel.
				this.messagePayload.remove(key);
			}
			else {
				log.debug("Partial RPY for channel " + digestChannel + ", msgno " + msgno + ", [" + this.messagePayload.get(key) + "]");
				return;
			}
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
			log.debug("DigestListener channel " + this.subscriberSession.getChannelNum() + " received NUL.");
		}
	}

}
