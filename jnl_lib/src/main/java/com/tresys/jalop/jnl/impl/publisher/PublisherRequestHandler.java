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

package com.tresys.jalop.jnl.impl.publisher;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.beepcore.beep.core.BEEPError;
import org.beepcore.beep.core.BEEPException;
import org.beepcore.beep.core.InputDataStreamAdapter;
import org.beepcore.beep.core.MessageMSG;
import org.beepcore.beep.core.OutputDataStream;
import org.beepcore.beep.core.RequestHandler;
import org.beepcore.beep.util.BufferSegment;

import com.tresys.jalop.jnl.Publisher;
import com.tresys.jalop.jnl.RecordType;
import com.tresys.jalop.jnl.SourceRecord;
import com.tresys.jalop.jnl.exceptions.JNLException;
import com.tresys.jalop.jnl.exceptions.MissingMimeHeaderException;
import com.tresys.jalop.jnl.exceptions.UnexpectedMimeValueException;
import com.tresys.jalop.jnl.impl.ContextImpl;
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
			final SubscribeMessage msg = Utils.processSubscribe(data);

			final Publisher publisher = this.contextImpl.getPublisher();

			final PublisherSessionImpl sess =
				contextImpl.getPublisherSession(message.getChannel().getSession(), this.recordType);
			final String serialId = msg.getSerialId();

			if(publisher.onSubscribe(sess, serialId, msg.getOtherHeaders())) {

				final MessageDigest md = sess.getMd();
				SourceRecord sourceRecord = publisher.getNextRecord(sess, serialId);
				while(sourceRecord != null) {

					md.reset();
					final InputStream sysMeta = sourceRecord.getSysMetadata();
					final InputStream appMeta = sourceRecord.getAppMetadata();
					final InputStream payload = sourceRecord.getPayload();
					final String currSerialId = sourceRecord.getSerialId();

					final org.beepcore.beep.core.MimeHeaders mh = new org.beepcore.beep.core.MimeHeaders();
					mh.setContentType(Utils.CT_JALOP);
					mh.setHeader(Utils.HDRS_SERIAL_ID, currSerialId);

					switch(this.recordType) {
					case Log:
						mh.setHeader(Utils.HDRS_MESSAGE, Utils.MSG_LOG);
						mh.setHeader(Utils.HDRS_LOG_LEN, String.valueOf(sourceRecord.getPayloadLength()));
						break;
					case Audit:
						mh.setHeader(Utils.HDRS_MESSAGE, Utils.MSG_AUDIT);
						mh.setHeader(Utils.HDRS_AUDIT_LEN, String.valueOf(sourceRecord.getPayloadLength()));
						break;
					case Journal:
						mh.setHeader(Utils.HDRS_MESSAGE, Utils.MSG_JOURNAL);
						mh.setHeader(Utils.HDRS_JOURNAL_LEN, String.valueOf(sourceRecord.getPayloadLength()));
						break;
					}

					mh.setHeader(Utils.HDRS_SYS_META_LEN, String.valueOf(sourceRecord.getSysMetaLength()));
					mh.setHeader(Utils.HDRS_APP_META_LEN, String.valueOf(sourceRecord.getAppMetaLength()));

					byte[] buffer =  new byte[(int)sourceRecord.getSysMetaLength()];
					sysMeta.read(buffer);
					md.update(buffer);
					final OutputDataStream ods = new OutputDataStream(mh, new BufferSegment(buffer));
					ods.add(new BufferSegment(Utils.BREAK.getBytes("utf-8")));

					buffer =  new byte[(int)sourceRecord.getAppMetaLength()];
					appMeta.read(buffer);
					md.update(buffer);
					ods.add(new BufferSegment(buffer));
					ods.add(new BufferSegment(Utils.BREAK.getBytes("utf-8")));

					buffer =  new byte[(int)sourceRecord.getPayloadLength()];
					payload.read(buffer);
					md.update(buffer);
					ods.add(new BufferSegment(buffer));
					ods.add(new BufferSegment(Utils.BREAK.getBytes("utf-8")));

					ods.setComplete();

					final byte[] digest = md.digest();

					sess.addDigest(currSerialId, digest);
					publisher.notifyDigest(sess, currSerialId, digest);

					message.sendANS(ods);

					sourceRecord = publisher.getNextRecord(sess, currSerialId);
				}
				message.sendNUL();
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
