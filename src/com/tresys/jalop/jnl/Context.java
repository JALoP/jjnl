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

/**
 * The {@link Context} is the primary Object that applications interact with. It
 * is responsible for initiating connections to, or listening for connections
 * from, remote JALoP Network Stores.
 */
public interface Context {
	/**
	 * Configure the set of allowed message digest algorithms. When negotiating
	 * a connection to a remote JALoP Network Store, the context will use the
	 * <code>mesasgeDigests</code> {@link Iterable} to negotiate which digest
	 * method to use. The order of this list is important; The first element in
	 * the list is considered to have the highest priority, and the last element
	 * in the list is considered to have the lowest priority.
	 * <p>
	 * If this this method is never called, the {@link Context} will only
	 * present the digest method of "SHA-256".
	 * <p>
	 * The {@link Context} creates a copy of this list, so callers are free to
	 * modify it after the call.
	 * 
	 * @param messageDigests
	 *            The message digests to present to JALoP Network Stores.
	 */
	void setAllowedMessageDigests(Iterable<String> messageDigests);

	/**
	 * Configure the set of allowed XML encodings (exi, gzip, none, etc). This
	 * is NOT used to specify the XML character set (utf-8, iso8859-1, etc), but
	 * to specify a compression mechanism. Implementations must be prepared to
	 * handle an encoding of "none", that is plain utf-8 encoding XML. When
	 * negotiating a connection to a remote JALoP Network Store, the context
	 * will use the <code>encodings</code> {@link Iterable} to negotiate which
	 * digest method to use. The order of this list is important; The first
	 * element in the list is considered to have the highest priority, and the
	 * last element in the list is considered to have the lowest priority.
	 * <p>
	 * If this this method is never called, the {@link Context} will only
	 * present an encoding of "none".
	 * 
	 * @param encodings
	 *            The encodings to present to JALoP Network Stores.
	 */
	void setAllowedXmlEncodings(Iterable<String> encodings);

	/**
	 * Register a {@link Subscriber} with this {@link Context}. This must be
	 * called with a non-null pointer before a call to
	 * {@link Context#subscribe(InetAddress, int, RecordType...)}. Additionally,
	 * if the {@link Context} will be used as a listener, a {@link Subscriber}
	 * must be registered before a call to
	 * {@link Context#listen(InetAddress, int)}, or the {@link Context} will
	 * reject any attempts from other JALoP Network Stores to connect and
	 * publish records to this {@link Context} if no {@link Subscriber} is
	 * registered.
	 * <p>
	 * Once a JALoP Subscriber session is established with another JALoP Network
	 * Store, the {@link Context} will deliver various events, along with JAL
	 * records to the registered {@link Subscriber}.
	 * <p>
	 * Once a call to {@link Context#listen(InetAddress, int)} or
	 * {@link Context#subscribe(InetAddress, int, RecordType...)} are made,
	 * further changes to the {@link Subscriber} cannot be made. Successive
	 * calls to this method will overwrite any previously registered
	 * {@link Subscriber}s.
	 * 
	 * @param subscriber
	 *            The {@link Subscriber}.
	 */
	void registerSubscriber(Subscriber subscriber);

	/**
	 * Register a {@link Publisher} with this {@link Context}. This must be
	 * called with a non-null pointer before a call to
	 * {@link Context#publish(InetAddress, int, RecordType...)}. Additionally,
	 * if the {@link Context} will be used as a listener, a {@link Publisher}
	 * must be registered before a call to
	 * {@link Context#listen(InetAddress, int)}, or the {@link Context} will
	 * reject any attempts from other JALoP Network Stores to connect and
	 * subscribe records from this {@link Context} if no @{link Publisher} was
	 * registered.
	 * <p>
	 * Once a JALoP Publisher session is established with another JALoP Network
	 * Store, the {@link Context} will deliver various events to, and acquire
	 * JAL records from, the registered {@link Publisher}.
	 * <p>
	 * Once a call to {@link Context#listen(InetAddress, int)} or
	 * {@link Context#publish(InetAddress, int, RecordType...)} are made,
	 * further changes to the {@link Publisher} cannot be made. Successive calls
	 * to this method will overwrite any previously registered {@link Publisher}s.
	 * 
	 * @param publisher
	 *            The {@link Publisher}
	 */
	void registerPublisher(Publisher publisher);

	/**
	 * Register a {@link ConnectionHandler} with this {@link Context}. Without a
	 * {@link ConnectionHandler}, the default behavior is to allow all
	 * connections from remote JALoP Network Stores, so long as they can agree
	 * upon the tuning parameters. If a {@link ConnectionHandler} is registered,
	 * then the {@link Context} will consult with the {@link ConnectionHandler}
	 * before accepting/rejecting any connections. Note, however, that the
	 * {@link Context} will always reject connections from remote JALoP Network
	 * Stores wishing to publish records, if there is no {@link Subscriber}
	 * registered, and will always reject connections from remote JALoP Network
	 * Stores wishing to subscribe, if there is no {@link Publisher} registered.
	 * 
	 * @param connHandler
	 *            The {@link ConnectionHandler}
	 */
	void registerConnectionHandler(ConnectionHandler connHandler);

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
	void listen(InetAddress addr, int port) throws IllegalArgumentException;

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
	 * @see Context#registerSubscriber(Subscriber)
	 * @see Subscriber
	 */
	void publish(InetAddress addr, int port, RecordType... types)
			throws IllegalArgumentException;

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
	 * @see Context#registerSubscriber(Subscriber)
	 * @see Subscriber
	 */
	void subscribe(InetAddress addr, int port, RecordType... types)
			throws IllegalArgumentException;

	/**
	 * Forcibly shutdown all connections with remote JALoP Network Store.
	 */
	void close();

	/**
	 * Attempt to cleanly shutdown all connections with remote JALoP Network
	 * Store.
	 */
	void shutdown();

	/**
	 * Set the default number of seconds to wait in between the sending of
	 * digest messages. This only affects Subscriber sessions. Additionally,
	 * this will not modify any existing Sessions. To modify an active Session,
	 * use {@link SubscriberSession#setDigestTimeout(long)}.
	 * 
	 * @param pendingDigestTimeoutSeconds
	 *            The maximum number of seconds to between sending of digest
	 *            messages.
	 */
	void setDefaultDigestTimeout(int pendingDigestTimeoutSeconds);

	/**
	 * Set the default number of records to transfer before sending a digest
	 * message. This only affects Subscriber sessions. Additionally, this will
	 * not modify any existing Sessions. To modify an active session use
	 * {@link SubscriberSession#setPendingDigestMax(int)}.
	 * 
	 * @param pendingDigestMax
	 *            The maximum number of records to receive before sending a
	 *            'digest-message'
	 */
	void setDefaultPendingDigestMax(int pendingDigestMax);

	/**
	 * Force the use of TLS. This must be called before any connections are made
	 * (i.e. before calls to
	 * {@link Context#subscribe(InetAddress, int, RecordType...)},
	 * {@link Context#publish(InetAddress, int, RecordType...)}, or
	 * {@link Context#listen(InetAddress, int)}). Setting this flag will cause
	 * the {@link Context} to hide the JALoP BEEP profile in the BEEP greeting
	 * message until TLS is negotiated. It will also reject the creation of any
	 * JALoP BEEP channels until TLS is negotiated. If the {@link Context}
	 * initiates the connection (i.e. through
	 * {@link Context#publish(InetAddress, int, RecordType...)}, or
	 * {@link Context#subscribe(InetAddress, int, RecordType...)}, then as soon
	 * as the BEEP connection is made, the {@link Context} will request the
	 * channel be TLS encrypted. When this flag is set and TLS negotiation
	 * fails, the {@link Context} will disconnect from remote immediately.
	 * <p>
	 * If this flag is not set, TLS negotiation may still happen, but is not
	 * required or requested. It is inadvisable to run without TLS in a live
	 * environment.
	 */
	void requireTLS();
}
