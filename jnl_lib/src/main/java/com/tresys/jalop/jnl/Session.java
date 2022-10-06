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

/**
 * Represents a specific JALoP session. A {@link Session} the connection between
 * two JALoP Network Stores used to transfer JAL records from the Publisher the
 * Subscriber.
 *
 */
public interface Session {
	/**
	 * Retrieve the type of record that is being transferred using this
	 * {@link Session}.
	 *
	 * @return The type of JAL records this {@link Session} transfers.
	 */
	RecordType getRecordType();

	/**
	 * Get the {@link Role} of this {@link Session}. If the {@link Role} is
	 * {@link Role#Publisher}, then this {@link Session} may be safely cast to
	 * {@link PublisherSession}. If the {@link Role} is {@link Role#Subscriber},
	 * then this {@link Session} may be safely cast to {@link SubscriberSession}
	 * . If {@link Role#Unset} is returned, it means negotiation with the remote
	 * JALoP Network Store has failed, or is not complete yet.
	 *
	 * @return The {@link Role} of this {@link Session}
	 */
	Role getRole();

	/**
	 * Get the {@link Mode} of this {@link Session}.
	 *
	 * @return The {@link Mode} of this {@link Session}
	 */
	Mode getMode();

	/**
	 * Set an error flag on the {@link Session}. Once an error is set, the
	 * library will not continue to deliver events to any of the registered
	 * handlers, and will attempt to close the connection with the remote JALoP
	 * Network Store.
	 */
	void setErrored();

	/**
	 * Checks the {@link Session} to determine if it is still active.
	 *
	 * @return <tt>true</tt> if the {@link Session} is still connected and
	 *         active, <tt>false</tt> otherwise.
	 */
	boolean isOk();

	/**
	 * Get the digest method that is being used on this {@link Session}.
	 *
	 * @return The digest method, or null if the digest method is not set, or
	 *         session negotiation has failed.
	 * @see Context#setAllowedMessageDigests(Iterable)
	 */
	String getDigestMethod();

	/**
	 * Get the XML encoding that is being used on this {@link Session}.
	 *
	 * @return The XML encoding, or null if no XML encoding is set, or session
	 *         negotiation has failed.
	 * @see Context#setAllowedXmlEncodings(Iterable)
	 */
	String getXmlEncoding();

}
