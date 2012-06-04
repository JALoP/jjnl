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
import java.util.List;

/**
 * The {@link ConnectionRequest} represents the request from a remote JALoP
 * Network Store to create a new JALoP session. The {@link Context} delivers
 * this to the registered {@link ConnectionHandler} when it receives a request
 * from a remote JALoP Network Store to create a new JALoP Session.
 * 
 * @see ConnectionHandler#handleConnectionRequest(boolean, ConnectionRequest)
 */
public interface ConnectionRequest {
	/**
	 * Retrieve the {@link InetAddress} of the remote JALoP Network Store.
	 * 
	 * @return an {@link InetAddress}.
	 */
	InetAddress getAddress();

	/**
	 * Retrieve the record type that the remote JALoP Network Store wishes to
	 * transfer over this session.
	 * 
	 * @return the {@link RecordType}
	 */
	RecordType getRecordType();

	/**
	 * Retrieve the version of JALoP the remote JALoP Network Store wishes to
	 * speak.
	 * 
	 * @return The requested JALoP version.
	 */
	int getJalopVersion();

	/**
	 * Retrieve the ordered list of XML encodings the remote JALoP Network Store
	 * 
	 * @return the list of XML encodings.
	 * @see Context#setAllowedXmlEncodings(Iterable)
	 */
	List<String> getXmlEncodings();

	/**
	 * Retrieve the ordered list of message digests proposed by the remote JALoP
	 * Network Store.
	 * 
	 * @return The list of message digests.
	 * @see Context#setAllowedMessageDigests(Iterable)
	 */
	List<String> getMessageDigests();

	/**
	 * Retrieve the role indicated by the remote JALoP Network Store. When the
	 * role is set to {@link Role#Publisher}, this indicates the remote wishes
	 * to act as a publisher. Conversely, when the {@link Role} is set to
	 * {@link Role#Subscriber}, this indicates the remote JALoP Network Store
	 * wishes to act as a subscriber.
	 * 
	 * @return The desired role as indicated by the remote JALoP Network Store.
	 */
	Role getRole();

	/**
	 * Retrieve the jalop-agent (if any) for the remote JALoP Network Store.
	 * 
	 * @return the JALoP agent string.
	 */
	String getAgent();

	/**
	 * Set the selected encoding to <tt>encoding</tt>. This must be one of the
	 * encodings in the list returned by
	 * {@link ConnectionRequest#getXmlEncodings()}. The encoding may be set any
	 * number of times. Each subsequent call will replace any previously
	 * selected encodings..
	 * 
	 * @param encoding
	 *            The XML encoding to use.
	 */
	void setSelectedXmlEncoding(String encoding);

	/**
	 * Set the selected message digest <tt>messageDigest</tt>. This must be one
	 * of the message digests in the list returned by
	 * {@link ConnectionRequest#getMessageDigests()}. The message digest may be
	 * set any number of times. Each subsequent call will replace any previously
	 * selected encodings.
	 * 
	 * @param messageDigest
	 *            the message digest to use.
	 */
	void setSelectedMessageDigest(String messageDigest);

	/**
	 * Retrieve the currently selected XML encoding.
	 * 
	 * @return The currently selected XML encoding.
	 */
	String getSelectedXmlEncoding();

	/**
	 * Retrieve the currently selected message digest.
	 * 
	 * @return The currently selected message digest.
	 */
	String getSelectedXmlDigest();
}
