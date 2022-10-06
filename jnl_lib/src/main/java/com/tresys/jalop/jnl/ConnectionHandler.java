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

import java.util.Set;

/**
 * A {@link ConnectionHandler} may be registered with a {@link Context} to make
 * more informed decisions regarding what connections should be allowed from
 * which remote JALoP Network Store.
 */
public interface ConnectionHandler {
	/**
	 * Represents various errors that can be returned by
	 * {@link ConnectionHandler#handleConnectionRequest(boolean, ConnectionRequest)}
	 * .
	 */
	public enum ConnectError {
		/** Accept the connection request */
		Accept,
		/** Reject the connection request, indicating a version mismatch */
		UnsupportedVersion,
		/**
		 * Reject the connection request, indicating an unsupported XML Encoding
		 */
		UnsupportedEncoding,
		/**
		 * Reject the connection request, indicating an unsupported message
		 * digest
		 */
		UnsupportedDigest,
		/**
		 * Reject the connection request, indicating the requested {@link Role}
		 * is not supported by this JALoP Network Store.
		 */
		UnsupportedMode,
		/**
		 * Reject the connection request, indicating the remote JALoP Network
		 * Store is not authorized to act in the requested {@link Role}.
		 */
		UnauthorizedMode,
	}

	/**
	 * The library executes this function when it receives a request from a
	 * remote JALoP Network Store to create/configure a JALoP Session.
	 * <p>
	 * When the library receives a connection request from a remote JALoP
	 * Network Store, it will do it's best to determine if the connection should
	 * be allowed or not. It will try and match requested message digests and
	 * XML encodings indicated by the remote JALoP Network Store, with the ones
	 * registered with the {@link Context}.
	 * <p>
	 * If the library finds a suitable match, <tt>rejecting</tt> is set to
	 * false, and the <tt>connRequest</tt> will be configured with the selected
	 * XML encoding and message digest. The application may modify
	 * <tt>connRequest</tt> to select a different XML encoding, or message
	 * digest. The application may also choose to reject the connection by any
	 * number of {@link ConnectError}s other than {@link ConnectError#Accept}.
	 * <p>
	 * If the library cannot find a suitable match for either the message
	 * digest, or the XML encoding, <tt>rejecting</tt> is set to true.
	 *
	 * @param rejecting
	 *            Set to <tt>true</tt> if the library is planning on rejecting
	 *            this connection.
	 * @param connRequest
	 *            The {@link ConnectionRequest}
	 * @return A {@link Set} of {@link ConnectError}s. If the return value is
	 *         null, an empty set, or contains a single element that is
	 *         {@link ConnectError#Accept}, then the library will accept this
	 *         connection.
	 */
	Set<ConnectError> handleConnectionRequest(boolean rejecting,
			final ConnectionRequest connRequest);

	/**
	 * This notification is called any time a {@link Session} is closed (whether
	 * gracefully or not).
	 *
	 * @param sess
	 *            The {@link Session} that was closed.
	 */
	void sessionClosed(Session sess);

	/**
	 * This notification is called any time the underlying {@link Connection} is
	 * closed (whether gracefully or not).
	 *
	 * @param conn
	 *            The {@link Connection} that was closed.
	 */
	void connectionClosed(Connection conn);

	/**
	 * This notification is called after the library receives a "connect-ack"
	 * message that signals a JALoP session was successfully created.
	 * <tt>ack</tt> contains all the details of the negotiated session.
	 *
	 * @param sess
	 *            The {@link Session}
	 * @param ack
	 *            details of the configured connection.
	 */
	void connectAck(Session sess, ConnectAck ack);

	/**
	 * This notification is called after the library receives a "connect-nack"
	 * message that signals the remote JALoP Network Store rejected the creation
	 * of the JALoP session. <tt>nack</tt> contains all the details of the
	 * negotiated session.
	 *
	 * @param sess
	 *            The {@link Session} where the channel was reject
	 * @param nack
	 *            details of the rejection.
	 */
	void connectNack(Session sess, ConnectNack nack);

}
