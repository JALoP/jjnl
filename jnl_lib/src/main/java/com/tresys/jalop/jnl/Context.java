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

package com.tresys.jalop.jnl;

import java.net.InetAddress;

import org.beepcore.beep.core.BEEPException;

import com.tresys.jalop.jnl.exceptions.JNLException;

/**
 * The {@link Context} is the primary Object that applications interact with. It
 * is responsible for initiating connections to, or listening for connections
 * from, remote JALoP Network Stores.
 */
public interface Context {

	/**
	 * Begin listening for incoming connections from remote JALoP Network
	 * Stores. By default, the library will allow all connections, provided
	 * there is a handler registered for the specific {@link Role}. That is, if
	 * there is a {@link Publisher} registered, the {@link Context} will allow
	 * subscribe requests from all remote JALoP Network Stores. Similarly, if
	 * there is a {@link Subscriber} registered, any request to publish is
	 * accepted. By registering a {@link ConnectionHandler}, applications may
	 * further tweak what connections are allowed.
	 *
	 * @param addr
	 *            The address to listen on. Currently only IPv4 address are
	 *            supported.
	 * @param port
	 *            The port to listen on.
	 * @throws IllegalArgumentException
	 *             If the address <tt>addr</tt> is not supported.
	 * @see Context#registerSubscriber(Subscriber)
	 * @see Context#registerPublisher(Publisher)
	 * @see Context#registerConnectionHandler(ConnectionHandler)
	 * @see Subscriber
	 * @see Publisher
	 * @see ConnectionHandler
	 */
	void listen(InetAddress addr, int port)
			throws IllegalArgumentException;

	/**
	 * Initiate a connection to a remote JALoP Network Store and publish the
	 * given record types. Once the library has initiated the connection to the
	 * remote JALoP Network Store, it starts creating JALoP sessions for each
	 * {@link RecordType} specified. Before calling this method, applications
	 * must register a {@link Publisher} with the {@link Context}. The library
	 * obtains JAL Records from, and delivers events to the registered
	 * {@link Publisher}.
	 *
	 * @param addr
	 *            The IP address of the remote to connect to.
	 * @param port
	 *            The port to connect to.
	 * @param types
	 *            The types of JALoP records to subscribe to.
	 * @throws IllegalArgumentException
	 *             if the address <tt>addr</tt> is not supported. Currently,
	 *             only IPv4 addresses are supported.
	 * @throws JNLException
	 * @throws BEEPException
	 * @see Context#registerSubscriber(Subscriber)
	 * @see Subscriber
	 */
	void publish(InetAddress addr, int port, RecordType... types)
			throws IllegalArgumentException, JNLException, BEEPException;

	/**
	 * Initiate a connection to a remote JALoP Network Store and subscribe to
	 * the given record types. Once the library has initiated the connection to
	 * the remote JALoP Network Store, it starts creating JALoP sessions for
	 * each {@link RecordType} specified. Before calling this method,
	 * applications must register a {@link Subscriber} with the {@link Context}.
	 * The library delivers JAL Records to, and obtains the information needed
	 * to send a "subscribe" or "journal-resume" message, from the registered
	 * {@link Subscriber}.
	 *
	 * @param addr
	 *            The IP address of the remote to connect to.
	 * @param port
	 *            The port to connect to.
	 * @param types
	 *            The types of JALoP records to subscribe to.
	 * @throws IllegalArgumentException
	 *             if the address <tt>addr</tt> is not supported. Currently,
	 *             only IPv4 addresses are supported.
	 * @throws BEEPException
	 * @throws JNLException
	 * @see Context#registerSubscriber(Subscriber)
	 * @see Subscriber
	 */
	void subscribe(InetAddress addr, int port, RecordType... types)
			throws BEEPException, JNLException;

	/**
	 * Forcibly shutdown all connections with remote JALoP Network Store.
	 */
	void close();

	/**
	 * Attempt to cleanly shutdown all connections with remote JALoP Network
	 * Store.
	 */
	void shutdown();

}
