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

import java.io.InputStream;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.beepcore.beep.core.BEEPError;
import org.beepcore.beep.core.BEEPException;
import org.beepcore.beep.core.Channel;
import org.beepcore.beep.core.CloseChannelException;
import org.beepcore.beep.core.InputDataStreamAdapter;
import org.beepcore.beep.core.MessageMSG;
import org.beepcore.beep.core.OutputDataStream;
import org.beepcore.beep.core.ReplyListener;
import org.beepcore.beep.core.RequestHandler;
import org.beepcore.beep.core.Session;
import org.beepcore.beep.core.StartChannelException;
import org.beepcore.beep.core.StartChannelListener;
import org.beepcore.beep.profile.Profile;
import org.beepcore.beep.profile.ProfileConfiguration;
import org.beepcore.beep.transport.tcp.TCPSession;

import com.tresys.jalop.jnl.ConnectionHandler.ConnectError;
import com.tresys.jalop.jnl.Publisher;
import com.tresys.jalop.jnl.RecordType;
import com.tresys.jalop.jnl.Role;
import com.tresys.jalop.jnl.SubscribeRequest;
import com.tresys.jalop.jnl.Subscriber;
import com.tresys.jalop.jnl.exceptions.JNLException;
import com.tresys.jalop.jnl.exceptions.MissingMimeHeaderException;
import com.tresys.jalop.jnl.exceptions.UnexpectedMimeValueException;
import com.tresys.jalop.jnl.impl.messages.InitMessage;
import com.tresys.jalop.jnl.impl.messages.Utils;
import com.tresys.jalop.jnl.impl.publisher.PublisherRequestHandler;
import com.tresys.jalop.jnl.impl.publisher.PublisherSessionImpl;
import com.tresys.jalop.jnl.impl.subscriber.SubscriberSessionImpl;

/**
 * Class to listen for and handle init requests from a peer.
 */
public class ListenerProfile implements Profile, StartChannelListener, RequestHandler {

	static final Logger log = Logger.getLogger(ListenerProfile.class);

	final ContextImpl contextImpl;
	final InetAddress address;
	Role role;
	String uri;
	ProfileConfiguration config;

	/**
	 * Create a new {@link ListenerProfile}
	 *
	 * @param contextImpl
	 *            The {@link ContextImpl} that initiated the listener.
	 * @param address
	 *            The {@link InetAddress} the listener is using.
	 */
	public ListenerProfile(final ContextImpl contextImpl, final InetAddress address) {
		this.contextImpl = contextImpl;
		this.address = address;
		this.role = Role.Unset;
	}

	@Override
	public boolean advertiseProfile(final Session session) {
		return session.getTuningProperties().getEncrypted();
	}

	@Override
	public void closeChannel(final Channel channel) throws CloseChannelException {
		if(log.isDebugEnabled()) {
			log.debug("Closing channel number: " + channel.getNumber());
		}
	}

	@Override
	public void startChannel(final Channel channel, final String encoding, final String data)
			throws StartChannelException {

		if(data != null) {
			try {
				final String[] dataSplit = data.split(":");

				if(this.role == Role.Subscriber) {
					final SubscriberSessionImpl subSess = this.contextImpl.findSubscriberSession(channel.getSession(), Integer.parseInt(dataSplit[1]));
					subSess.setDigestChannel(channel);

					new Thread(subSess, "digestThread").start();
					channel.setRequestHandler(new DigestInitHandler(subSess, this.contextImpl));

				} else if(this.role == Role.Publisher) {

					final PublisherSessionImpl pubSess = this.contextImpl.findPublisherSession(channel.getSession(), Integer.parseInt(dataSplit[1]));
					pubSess.setDigestChannel(channel);
					channel.setRequestHandler(new DigestInitHandler(pubSess, this.contextImpl));
				}
			} catch (final JNLException e) {
				if(log.isEnabledFor(Level.ERROR)) {
					log.error("Error starting the channel: " + e.getMessage());
				}
				throw new StartChannelException(BEEPError.CODE_REQUESTED_ACTION_ABORTED, e.getMessage());
			}
		} else {
			channel.setRequestHandler(this);
		}
	}

	@Override
	public StartChannelListener init(final String uri, final ProfileConfiguration config)
			throws BEEPException {

		this.uri = uri;
		this.config = config;
		return this;
	}

	@Override
	public void receiveMSG(final MessageMSG message) {
		if (log.isDebugEnabled()) {
			log.debug("received message in ListenerProfile");
		}

		final InputDataStreamAdapter data = message.getDataStream().getInputStream();

		try {

			final InitMessage msg = Utils.processInitMessage(data);

			final TCPSession tcpSession = (TCPSession) message.getChannel().getSession();
			final InetAddress peerAddress = tcpSession.getSocket().getInetAddress();
			final ConnectionRequestImpl connRequest = new ConnectionRequestImpl(peerAddress, msg.getRecordType(), 1,
					msg.getAcceptEncodings(), msg.getAcceptDigests(), msg.getRole(), msg.getAgentString());

			final Set<ConnectError> connectErrors = contextImpl.getConnectionHandler().handleConnectionRequest(false, connRequest);

			if(Collections.disjoint(this.contextImpl.getAllowedXmlEncodings(), connRequest.getXmlEncodings())) {
				connectErrors.add(ConnectError.UnsupportedEncoding);
			}
			if(Collections.disjoint(this.contextImpl.getAllowedMessageDigests(), connRequest.getMessageDigests())) {
				connectErrors.add(ConnectError.UnsupportedDigest);
			}

			OutputDataStream ods;

			if(connectErrors == null || connectErrors.isEmpty() ||
					(connectErrors.size() == 1 && connectErrors.contains(ConnectError.Accept))) {

				if(log.isDebugEnabled()) {
					log.debug("Accepting the connection request");
				}

				final List<String> commonDigests = new ArrayList<String>(connRequest.getMessageDigests());
				commonDigests.retainAll(this.contextImpl.getAllowedMessageDigests());
				connRequest.setSelectedMessageDigest(commonDigests.get(0));

				final List<String> commonEncodings = new ArrayList<String>(connRequest.getXmlEncodings());
				commonEncodings.retainAll(this.contextImpl.getAllowedXmlEncodings());
				connRequest.setSelectedXmlEncoding(commonEncodings.get(0));

				OutputDataStream subscriberOds = null;
				ReplyListener subscriberListener = null;

				if(msg.getRole() == Role.Publisher) {
					if(log.isDebugEnabled()) {
						log.debug("Listener is going to be the Subscriber.");
					}

					this.role = Role.Subscriber;

					final Subscriber subscriber = this.contextImpl.getSubscriber();
					final SubscriberSessionImpl sessionImpl = new SubscriberSessionImpl(
							this.address, msg.getRecordType(), subscriber, connRequest.getSelectedXmlDigest(),
							connRequest.getSelectedXmlEncoding(), this.contextImpl.getDefaultDigestTimeout(),
							this.contextImpl.getDefaultPendingDigestMax(), message.getChannel().getNumber(),
							message.getChannel().getSession());

					this.contextImpl.addSession(message.getChannel().getSession(), sessionImpl);

					final SubscribeRequest request = subscriber.getSubscribeRequest(sessionImpl);

					subscriberListener = sessionImpl.getListener();

					if(request.getResumeOffset() > 0 && RecordType.Journal.equals(msg.getRecordType())) {
						if(log.isDebugEnabled()) {
							log.debug("Sending a journal resume message.");
						}
						final InputStream resumeInputStream = request.getResumeInputStream();
						sessionImpl.setJournalResumeIS(resumeInputStream);
						sessionImpl.setJournalResumeOffset(request.getResumeOffset());
						subscriberOds = Utils.createJournalResumeMessage(request.getSerialId(), request.getResumeOffset());
					} else {
						subscriberOds = Utils.createSubscribeMessage(request.getSerialId());
					}

				} else if(msg.getRole() == Role.Subscriber) {

					if(log.isDebugEnabled()) {
						log.debug("Listener is going to be the Publisher.");
					}

					this.role = Role.Publisher;

					final Publisher publisher = contextImpl.getPublisher();
					final PublisherSessionImpl sessionImpl = new PublisherSessionImpl(
							this.address, msg.getRecordType(), publisher, connRequest.getSelectedXmlDigest(),
							connRequest.getSelectedXmlEncoding(), message.getChannel().getNumber(),
							message.getChannel().getSession(), this.contextImpl);

					this.contextImpl.addSession(message.getChannel().getSession(),
							sessionImpl);
				}

				ods = Utils.createInitAckMessage(connRequest.getSelectedXmlDigest(), connRequest.getSelectedXmlEncoding());

				if(msg.getRole() == Role.Subscriber) {
					message.getChannel().setRequestHandler(new PublisherRequestHandler(msg.getRecordType(), this.contextImpl));
				}

				message.sendRPY(ods);

				if(msg.getRole() == Role.Publisher) {
					message.getChannel().sendMSG(subscriberOds, subscriberListener);
				}

			} else {

				if(log.isDebugEnabled()) {
					final StringBuilder sb = new StringBuilder();
					for (final ConnectError ce : connectErrors) {
						if (sb.length() != 0) {
							sb.append(", ");
						}
						sb.append(ce);
					}
					log.debug("Rejecting the connection request: " + sb.toString());
				}
				ods = Utils.createInitNackMessage(new ArrayList<ConnectError>(connectErrors));

				message.sendRPY(ods);
				return;
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
				log.error("Error: " + e.getMessage());
			}
			sendERR(message);
		}
	}

	/**
	 * Sends an error message back to the peer.
	 *
	 * @param message
	 * 				The received message that has caused an error.
	 */
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
