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
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.beepcore.beep.core.BEEPError;
import org.beepcore.beep.core.BEEPException;
import org.beepcore.beep.core.InputDataStreamAdapter;
import org.beepcore.beep.core.MessageMSG;
import org.beepcore.beep.core.RequestHandler;
import org.beepcore.beep.util.BufferSegment;

import com.tresys.jalop.jnl.Publisher;
import com.tresys.jalop.jnl.RecordType;
import com.tresys.jalop.jnl.SourceRecord;
import com.tresys.jalop.jnl.exceptions.JNLException;
import com.tresys.jalop.jnl.exceptions.MissingMimeHeaderException;
import com.tresys.jalop.jnl.exceptions.UnexpectedMimeValueException;
import com.tresys.jalop.jnl.impl.ContextImpl;
import com.tresys.jalop.jnl.impl.JNLOutputDataStream;
import com.tresys.jalop.jnl.impl.messages.JournalResumeMessage;
import com.tresys.jalop.jnl.impl.messages.SubscribeMessage;
import com.tresys.jalop.jnl.impl.messages.Utils;

/**
 * Class for receiving subscribe messages from the subscriber.
 */
public class PublisherRequestHandler implements RequestHandler {

	static Logger log = Logger.getLogger(PublisherRequestHandler.class);

	static final int BUFFER_SIZE = 4096;
	static final int MAX_BUFFERS = 10;

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
			SourceRecord sourceRecord = null;
			long offset = 0;

			if(Utils.MSG_SUBSCRIBE.equals(data.getHeaderValue(Utils.HDRS_MESSAGE))) {
				if(log.isDebugEnabled()) {
					log.debug("Received a subscribe message.");
				}
				final SubscribeMessage msg = Utils.processSubscribe(data);
				nonce = msg.getNonce();
				if(!publisher.onSubscribe(sess, nonce, msg.getOtherHeaders())) {
					if(log.isEnabledFor(Level.ERROR)) {
						log.error("Problem with subscribe - not sending any records.");
					}
					return;
				}
				sourceRecord = publisher.getNextRecord(sess, nonce);
			} else if(Utils.MSG_JOURNAL_RESUME.equals(data.getHeaderValue(Utils.HDRS_MESSAGE))) {
				if(log.isDebugEnabled()) {
					log.debug("Received a journal resume message.");
				}
				final JournalResumeMessage msg = Utils.processJournalResume(data);
				nonce = msg.getNonce();
				offset = msg.getOffset();
				sourceRecord = publisher.onJournalResume(sess, nonce, msg.getOffset(), msg.getOtherHeaders());
			}

			String messageType = null;
			String payloadLengthHeader = null;
			switch(this.recordType) {
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
			}

			final MessageDigest md = sess.getMd();

			while(sourceRecord != null) {

				md.reset();

				final String currNonce = sourceRecord.getNonce();

				final org.beepcore.beep.core.MimeHeaders mh = new org.beepcore.beep.core.MimeHeaders();
				mh.setContentType(Utils.CT_JALOP);
				mh.setHeader(Utils.HDRS_NONCE, currNonce);
				mh.setHeader(Utils.HDRS_MESSAGE, messageType);
				mh.setHeader(payloadLengthHeader, String.valueOf(sourceRecord.getPayloadLength()));
				mh.setHeader(Utils.HDRS_SYS_META_LEN, String.valueOf(sourceRecord.getSysMetaLength()));
				mh.setHeader(Utils.HDRS_APP_META_LEN, String.valueOf(sourceRecord.getAppMetaLength()));

				final JNLOutputDataStream ods = new JNLOutputDataStream(mh, MAX_BUFFERS);
				message.sendANS(ods);

				byte[] buffer =  new byte[BUFFER_SIZE];
				int bytesRead = 0;

				final List<InputStream> inputStreamList = new ArrayList<InputStream>();
				inputStreamList.add(sourceRecord.getSysMetadata());
				inputStreamList.add(new ByteArrayInputStream(Utils.BREAK.getBytes("utf-8")));
				inputStreamList.add(sourceRecord.getAppMetadata());
				inputStreamList.add(new ByteArrayInputStream(Utils.BREAK.getBytes("utf-8")));
				inputStreamList.add(sourceRecord.getPayload());
				inputStreamList.add(new ByteArrayInputStream(Utils.BREAK.getBytes("utf-8")));

				InputStream inStream = null;
				boolean shouldDigest = false;

				while(!inputStreamList.isEmpty()) {
					inStream = inputStreamList.remove(0);
					//every other stream is a break and shouldn't be included in the digest
					shouldDigest = !shouldDigest;

					if(inStream == null) {
						continue;
					}

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

				sess.addDigest(currNonce, digest);
				publisher.notifyDigest(sess, currNonce, digest);

				sourceRecord = publisher.getNextRecord(sess, currNonce);
			}
			message.sendNUL();

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
		} catch (final IOException e) {
			if (log.isEnabledFor(Level.ERROR)) {
				log.error("Error reading the input: " + e.getMessage());
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
