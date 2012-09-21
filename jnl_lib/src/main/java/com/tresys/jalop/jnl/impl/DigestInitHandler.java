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

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.beepcore.beep.core.BEEPError;
import org.beepcore.beep.core.BEEPException;
import org.beepcore.beep.core.Channel;
import org.beepcore.beep.core.Message;
import org.beepcore.beep.core.MessageMSG;
import org.beepcore.beep.core.MimeHeaders;
import org.beepcore.beep.core.OutputDataStream;
import org.beepcore.beep.core.RequestHandler;
import org.beepcore.beep.util.BufferSegment;

import com.tresys.jalop.jnl.impl.publisher.PublisherSessionImpl;

/**
 * Class for responding to a PiggybackedMSG with a request to start a digest channel.
 * When a message is received this class will send back an empty reply in response
 * signaling that the channel can be created and messages can now be sent.
 */
public class DigestInitHandler implements RequestHandler {

	static Logger log = Logger.getLogger(DigestInitHandler.class);

	SessionImpl sess;
	ContextImpl contextImpl;

	/**
	 * Create a new {@link DigestInitHandler}.
	 *
	 * @param sess
	 *            The {@link SessionImpl} associated with the request.
	 * @param contextImpl
	 *            The {@link ContextImpl} that initiated the connection.
	 */
	public DigestInitHandler(final SessionImpl sess,
			final ContextImpl contextImpl) {

		this.sess = sess;
		this.contextImpl = contextImpl;
	}

	@Override
	public void receiveMSG(final MessageMSG message) {

		if (log.isDebugEnabled()) {
			log.debug("received message in DigestInitHandler");
		}

		try {

			if(message.getMsgno() != Message.PIGGYBACKED_MSGNO) {
				throw new BEEPException("Digest channel isn't using PiggybackedMSG");
			}

			final Channel channel = message.getChannel();
			// For publishers a request handler needs to be set for the incoming digest messages.
			// Subscribers will already have a thread started for sending messages and should not receive any other messages here.
			if(this.sess instanceof PublisherSessionImpl) {
				channel.setRequestHandler(new DigestRequestHandler(this.sess.getRecordType(), this.contextImpl, (PublisherSessionImpl) sess));
			} else {
				channel.setRequestHandler(new RequestHandler() {

					@Override
					public void receiveMSG(final MessageMSG message) {
						if (log.isEnabledFor(Level.ERROR)) {
							log.error("Invalid message sent. No messages should be received by this RequestHandler");
						}
						try {
							message.sendERR(new BEEPError(BEEPError.CODE_REQUESTED_ACTION_ABORTED));
						} catch (final BEEPException e) {
							if (log.isEnabledFor(Level.ERROR)) {
								log.error("Error trying to send Error message: " + e.getMessage());
							}
						}
					}
				});
			}

			final OutputDataStream ret = new OutputDataStream(new MimeHeaders(), new BufferSegment(new byte[0]));
			ret.setComplete();
			message.sendRPY(ret);

		} catch (final BEEPException e) {
			try {
				if (log.isEnabledFor(Level.ERROR)) {
					log.error("Error sending a reply to the peer: " + e.getMessage());
				}
				message.sendERR(new BEEPError(BEEPError.CODE_REQUESTED_ACTION_ABORTED));
			} catch (final BEEPException error) {
				if (log.isEnabledFor(Level.ERROR)) {
					log.error("Error trying to send Error message: " + error.getMessage());
				}
			}
		}

	}
}
