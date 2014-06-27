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

import java.io.IOException;
import java.security.MessageDigest;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.beepcore.beep.core.BEEPError;
import org.beepcore.beep.core.BEEPException;
import org.beepcore.beep.core.InputDataStreamAdapter;
import org.beepcore.beep.core.MessageMSG;
import org.beepcore.beep.core.RequestHandler;

import com.tresys.jalop.jnl.Mode;
import com.tresys.jalop.jnl.Publisher;
import com.tresys.jalop.jnl.RecordType;
import com.tresys.jalop.jnl.SourceRecord;
import com.tresys.jalop.jnl.exceptions.JNLException;
import com.tresys.jalop.jnl.exceptions.MissingMimeHeaderException;
import com.tresys.jalop.jnl.exceptions.UnexpectedMimeValueException;
import com.tresys.jalop.jnl.impl.ContextImpl;
import com.tresys.jalop.jnl.impl.messages.JournalResumeMessage;
import com.tresys.jalop.jnl.impl.messages.SubscribeMessage;
import com.tresys.jalop.jnl.impl.messages.Utils;

/**
 * Class for receiving subscribe messages from the subscriber.
 */
public class PublisherRequestHandler implements RequestHandler {

	static Logger log = Logger.getLogger(PublisherRequestHandler.class);

	final RecordType recordType;
	final ContextImpl contextImpl;

	/**
	 * Create a new {@link PublisherRequestHandler}.
	 *
	 * @param recordType
	 *            The type of record.
	 * @param contextImpl
	 *            The {@link ContextImpl} that initiated the connection.
	 */
	public PublisherRequestHandler(final RecordType recordType, final ContextImpl contextImpl) {

		this.recordType = recordType;
		this.contextImpl = contextImpl;
	}

	@Override
	public void receiveMSG(final MessageMSG message) {

		if (log.isDebugEnabled()) {
			log.debug("received message in PublisherRequestHandler");
		}

		final InputDataStreamAdapter data = message.getDataStream().getInputStream();

		try {

			final Publisher publisher = this.contextImpl.getPublisher();

			final PublisherSessionImpl sess =
				contextImpl.getPublisherSession(message.getChannel().getSession(), this.recordType);

			String nonce = null;
			Mode mode = Mode.Unset;
			SourceRecord sourceRecord = null;
			long offset = 0;

			sess.msg = message;
			if(data.getHeaderValue(Utils.HDRS_MESSAGE).startsWith(Utils.MSG_SUBSCRIBE)) {
				if(log.isDebugEnabled()) {
					log.debug("Received a subscribe message.");
				}
				final SubscribeMessage msg = Utils.processSubscribe(data);
				nonce = msg.getNonce();
				mode = msg.getMode();
				if(!publisher.onSubscribe(sess, nonce, mode, msg.getOtherHeaders())) {
					if(log.isEnabledFor(Level.ERROR)) {
						log.error("Problem with subscribe - not sending any records.");
					}
					return;
				}
			} else if(Utils.MSG_JOURNAL_RESUME.equals(data.getHeaderValue(Utils.HDRS_MESSAGE))) {
				if(log.isDebugEnabled()) {
					log.debug("Received a journal resume message.");
				}
				final JournalResumeMessage msg = Utils.processJournalResume(data);
				nonce = msg.getNonce();
				offset = msg.getOffset();
				if(!publisher.onJournalResume(sess, nonce, offset, msg.getOtherHeaders())) {
					if(log.isEnabledFor(Level.ERROR)) {
						log.error("Problem with journal resume - not sending any records.");
					}
					return;
				}
			}
		} catch (final BEEPException e) {
			if (log.isEnabledFor(Level.ERROR)) {
				log.error("Error receiving message: " + e.getMessage());
			}
			sendERR(message);
		} catch (final MissingMimeHeaderException e) {
			if (log.isEnabledFor(Level.ERROR)) {
				log.error("Error - Missing Mime Header: " + e.getMessage());
			}
			sendERR(message);
		} catch (final UnexpectedMimeValueException e) {
			if (log.isEnabledFor(Level.ERROR)) {
				log.error("Error - Unexpected value: " + e.getMessage());
			}
			sendERR(message);
		} catch (final JNLException e) {
			if (log.isEnabledFor(Level.ERROR)) {
				log.error("Error getting the PublisherSession: " + e.getMessage());
			}
			sendERR(message);
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

}
