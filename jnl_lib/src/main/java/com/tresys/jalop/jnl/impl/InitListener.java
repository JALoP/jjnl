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

import java.io.InputStream;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.beepcore.beep.core.AbortChannelException;
import org.beepcore.beep.core.BEEPException;
import org.beepcore.beep.core.Channel;
import org.beepcore.beep.core.InputDataStreamAdapter;
import org.beepcore.beep.core.Message;
import org.beepcore.beep.core.OutputDataStream;
import org.beepcore.beep.core.ReplyListener;

import com.tresys.jalop.jnl.ConnectionHandler.ConnectError;
import com.tresys.jalop.jnl.Mode;
import com.tresys.jalop.jnl.Publisher;
import com.tresys.jalop.jnl.RecordType;
import com.tresys.jalop.jnl.Role;
import com.tresys.jalop.jnl.SubscribeRequest;
import com.tresys.jalop.jnl.Subscriber;
import com.tresys.jalop.jnl.exceptions.JNLException;
import com.tresys.jalop.jnl.exceptions.MissingMimeHeaderException;
import com.tresys.jalop.jnl.exceptions.UnexpectedMimeValueException;
import com.tresys.jalop.jnl.impl.messages.InitAckMessage;
import com.tresys.jalop.jnl.impl.messages.InitNackMessage;
import com.tresys.jalop.jnl.impl.messages.Utils;
import com.tresys.jalop.jnl.impl.publisher.PublisherSessionImpl;
import com.tresys.jalop.jnl.impl.subscriber.SubscriberSessionImpl;

/**
 * Listener class to be used for init messages. This will listen for replies
 * after a message is sent.
 */
public class InitListener implements ReplyListener {

	static Logger log = Logger.getLogger(InitListener.class);

	final InetAddress address;
	Role role;
	RecordType recordType;
	ContextImpl contextImpl;

	/**
	 * Create a new {@link InitListener}.
	 *
	 * @param role
	 *            The role that will be used, either Publisher or Subscriber.
	 * @param recordType
	 *            The type of record.
	 * @param contextImpl
	 *            The {@link ContextImpl} that initiated the connection and will
	 *            be associated with the created {@link Session}.
	 */
	public InitListener(final InetAddress address, final Role role,
			final RecordType recordType, final ContextImpl contextImpl) {
		this.address = address;
		this.role = role;
		this.recordType = recordType;
		this.contextImpl = contextImpl;
	}

	@Override
	public void receiveRPY(final Message message)
			throws AbortChannelException {

		if (log.isDebugEnabled()) {
			log.debug("***** InitListener receiveRPY");
		}

		final InputDataStreamAdapter data = message.getDataStream().getInputStream();

		try {
			final InitAckMessage msg = Utils.processInitAck(data);

			if (!contextImpl.getAllowedMessageDigests().contains(msg.getDigest())) {
				throw new UnexpectedMimeValueException(Utils.HDRS_DIGEST,
						Utils.makeStringList(contextImpl.getAllowedMessageDigests(), "digests"),
						msg.getDigest());
			}

			if (!contextImpl.getAllowedXmlEncodings().contains(msg.getEncoding())) {
				throw new UnexpectedMimeValueException(Utils.HDRS_ENCODING,
						Utils.makeStringList(contextImpl.getAllowedXmlEncodings(), "encodings"),
						msg.getEncoding());
			}

			if (Role.Subscriber.equals(this.role)) {
				final Subscriber subscriber = contextImpl.getSubscriber();
				final SubscriberSessionImpl sessionImpl = new SubscriberSessionImpl(
						this.address, this.recordType, subscriber, msg.getDigest(),
						msg.getEncoding(), contextImpl.getDefaultDigestTimeout(),
						contextImpl.getDefaultPendingDigestMax(), message.getChannel().getNumber(),
						message.getChannel().getSession());

				this.contextImpl.addSession(message.getChannel().getSession(), sessionImpl);

				final SubscribeRequest request = subscriber.getSubscribeRequest(sessionImpl);
				final OutputDataStream ods;
				if(request.getResumeOffset() > 0 && RecordType.Journal.equals(this.recordType)) {
					if(log.isDebugEnabled()) {
						log.debug("Sending a journal resume message.");
					}
					final InputStream resumeInputStream = request.getResumeInputStream();
					sessionImpl.setJournalResumeIS(resumeInputStream);
					sessionImpl.setJournalResumeOffset(request.getResumeOffset());
					ods = Utils.createJournalResumeMessage(request.getNonce(), request.getResumeOffset());
				} else {
					ods = Utils.createSubscribeMessage();
				}

				message.getChannel().sendMSG(ods, sessionImpl.getListener());

				new Thread(sessionImpl, "digestThread").start();

			} else if (Role.Publisher.equals(this.role)) {

				final Publisher publisher = contextImpl.getPublisher();
				final PublisherSessionImpl sessionImpl = new PublisherSessionImpl(
						this.address, this.recordType, publisher, msg.getDigest(),
						msg.getEncoding(), message.getChannel().getNumber(),
						message.getChannel().getSession(), this.contextImpl);

				this.contextImpl.addSession(message.getChannel().getSession(),
						sessionImpl);

				new Thread(sessionImpl, "digestThread").start();
			}

		} catch (final BEEPException e) {
			if (log.isEnabledFor(Level.ERROR)) {
				log.error("Error receiving reply: " + e.getMessage());
			}
			throw new AbortChannelException(e.getMessage());
		} catch (final MissingMimeHeaderException e) {
			if (log.isEnabledFor(Level.ERROR)) {
				log.error("Error - Missing Mime Header: " + e.getMessage());
			}
			throw new AbortChannelException(e.getMessage());
		} catch (final UnexpectedMimeValueException e) {
			if (log.isEnabledFor(Level.ERROR)) {
				log.error("Error - Unexpected value: " + e.getMessage());
			}
			throw new AbortChannelException(e.getMessage());
		} catch (final JNLException e) {
			if (log.isEnabledFor(Level.ERROR)) {
				log.error("Error adding the Session: " + e.getMessage());
			}
			throw new AbortChannelException(e.getMessage());
		}

	}

	@Override
	public void receiveERR(final Message message)
			throws AbortChannelException {

		List<ConnectError> connectErrors = new ArrayList<ConnectError>();

		try {
			final InputDataStreamAdapter data = message.getDataStream().getInputStream();
			final InitNackMessage msg = Utils.processInitNack(data);
			connectErrors = msg.getErrorList();

		} catch (final BEEPException e) {
			if (log.isEnabledFor(Level.ERROR)) {
				log.error("Error receiving ERR: " + e.getMessage());
			}
			throw new AbortChannelException(e.getMessage());

		} catch (final MissingMimeHeaderException e) {
			if (log.isEnabledFor(Level.ERROR)) {
				log.error("Error - Missing Mime Header: " + e.getMessage());
			}
			throw new AbortChannelException(e.getMessage());

		} catch (final UnexpectedMimeValueException e) {
			if (log.isEnabledFor(Level.ERROR)) {
				log.error("Error - Unexpected value: " + e.getMessage());
			}
			throw new AbortChannelException(e.getMessage());
		}

		final StringBuilder sb = new StringBuilder();

		for (final ConnectError ce : connectErrors) {
			if (sb.length() != 0) {
				sb.append(", ");
			}
			sb.append(ce);
		}

		if (log.isEnabledFor(Level.ERROR)) {
			log.error("InitListener received ERR: " + sb.toString());
		}

		message.getChannel().setRequestHandler(new ErrorRequestHandler());

		final Thread t = new Thread(new ChannelCloser(message.getChannel()));
		t.start();
	}

	@Override
	public void receiveANS(final Message message)
			throws AbortChannelException {

		if (log.isEnabledFor(Level.ERROR)) {
			log.error("InitListener received ANS which shouldn't happen.");
		}
		throw new AbortChannelException("InitListener should not receive ANS");
	}

	@Override
	public void receiveNUL(final Message message)
			throws AbortChannelException {

		if (log.isEnabledFor(Level.ERROR)) {
			log.error("InitListener received NUL which shouldn't happen.");
		}
		throw new AbortChannelException("InitListener should not receive NUL");
	}

	/**
	 * Used to create a thread to close channels
	 */
	class ChannelCloser implements Runnable {

		private final Channel channel;

		public ChannelCloser(final Channel channel) {
			this.channel = channel;
		}

		@Override
		public void run() {
			try {
				if(log.isDebugEnabled()) {
					log.debug("Closing channel number: " + channel.getNumber());
				}
				channel.close();
			} catch (final BEEPException e) {
				if (log.isEnabledFor(Level.ERROR)) {
					log.error("Error - Received error while trying to close channel number " +
							channel.getNumber() + ": " + e.getMessage());
				}
			}
		}
	}

}
