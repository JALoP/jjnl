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

package com.tresys.jalop.utils.jnltest;

import java.net.InetAddress;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import com.tresys.jalop.jnl.ConnectAck;
import com.tresys.jalop.jnl.ConnectNack;
import com.tresys.jalop.jnl.Connection;
import com.tresys.jalop.jnl.ConnectionHandler;
import com.tresys.jalop.jnl.ConnectionRequest;
import com.tresys.jalop.jnl.Role;
import com.tresys.jalop.jnl.Session;
import com.tresys.jalop.utils.jnltest.Config.PeerConfig;

/**
 * Implementation of a {@link ConnectionHandler}
 */
public class ConnectionHandlerImpl implements ConnectionHandler {

	private static final Logger log = Logger.getLogger(JNLTest.class);

	private final Map<InetAddress, PeerConfig> peerConfigs;

	public ConnectionHandlerImpl(final Map<InetAddress, PeerConfig> peerConfigs) {
		this.peerConfigs = peerConfigs;
	}

	@Override
	public Set<ConnectError> handleConnectionRequest(final boolean rejecting,
			final ConnectionRequest connRequest) {

		if(log.isDebugEnabled()) {
			log.debug("Received connection request.");
		}

		final Set<ConnectError> errors = new HashSet<ConnectError>();
		if(!peerConfigs.containsKey(connRequest.getAddress())	||
				(connRequest.getRole().equals(Role.Publisher) &&
						peerConfigs.get(connRequest.getAddress()).getPublishAllow().isEmpty())	||
				(connRequest.getRole().equals(Role.Subscriber) &&
						peerConfigs.get(connRequest.getAddress()).getSubscribeAllow().isEmpty())) {
			errors.add(ConnectError.UnsupportedMode);
		} else if((connRequest.getRole().equals(Role.Publisher) &&
				!peerConfigs.get(connRequest.getAddress()).getPublishAllow().contains(connRequest.getRecordType()))	||
				(connRequest.getRole().equals(Role.Subscriber) &&
						!peerConfigs.get(connRequest.getAddress()).getSubscribeAllow().contains(connRequest.getRecordType()))) {
			errors.add(ConnectError.UnauthorizedMode);
		}

		return errors;
	}

	@Override
	public void sessionClosed(final Session sess) {
		if(log.isDebugEnabled()) {
			log.debug("Session has been closed.");
		}
	}

	@Override
	public void connectionClosed(final Connection conn) {
		if(log.isDebugEnabled()) {
			log.debug("Connection has been closed.");
		}
	}

	@Override
	public void connectAck(final Session sess, final ConnectAck ack) {
		if(log.isDebugEnabled()) {
			log.debug("Received connect ack.");
		}
	}

	@Override
	public void connectNack(final Session sess, final ConnectNack nack) {
		if(log.isDebugEnabled()) {
			log.debug("Received connect nack.");
		}
	}

}
