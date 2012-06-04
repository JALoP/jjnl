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

import java.util.Map;

import javax.xml.soap.MimeHeaders;

/**
 * Users of this library should provide an <tt>Object</tt> that implements this
 * interface if they wish to act as a JALoP Publisher. Their object must be
 * registered with a {@link Context} prior to initiating, or listening for,
 * connections.
 * 
 * @see Context#registerPublisher(Publisher)
 */
public interface Publisher {
	/**
	 * The library executes this method when it is ready to send another JAL
	 * record to the remote JALoP Network Store. The library sends the returned
	 * {@link SourceRecord} to the remote JALoP Network Store.
	 * 
	 * @param sess
	 *            The {@link PublisherSession} that is trying to send a record.
	 * @param lastSerialId
	 *            The serial ID of the last JAL record that was sent using
	 *            <tt>sess</tt>.
	 * @return a {@link SourceRecord} object that is the next JAL record to send
	 *         to the remote JALoP Network Store.
	 */
	SourceRecord getNextRecord(final PublisherSession sess,
			final String lastSerialId);

	/**
	 * The library executes this method when the remote JALoP Network Store
	 * attempts to resume a journal record.
	 * 
	 * @param sess
	 *            The {@link PublisherSession} that received the
	 *            "journal-resume" message.
	 * @param serialId
	 *            The serial ID the remote JALoP Network Store indicated it
	 *            wants to resume.
	 * @param offset
	 *            The number of bytes the remote JALoP Network Store has already
	 *            transferred.
	 * @param headers
	 *            Any additional headers that were sent as part of the
	 *            "journal-resume" message.
	 * @return a {@link SourceRecord} for the JAL record identified by
	 *         <tt>serialId</tt>. If JAL record does not exist, this function
	 *         should return null.
	 */
	SourceRecord onJournalResume(final PublisherSession sess,
			final String serialId, final long offset, final MimeHeaders headers);

	/**
	 * The library executes this method when the remote JALoP Network Store
	 * sends a "subscribe" message. If this function returns <tt>false</tt> then
	 * the library will start the process to close down this
	 * {@link PublisherSession}.
	 * 
	 * @param sess
	 *            The {@link PublisherSession} that received the "subscribe"
	 *            message.
	 * @param serialId
	 *            The serial ID indicated in the subscribe message.
	 * @param headers
	 *            Any additional headers that were sent as part of the
	 *            "subscribe" message.
	 * @return <tt>true</tt> to allow the subscribe to complete, <tt>false</tt>
	 *         otherwise.
	 */
	boolean onSubscribe(final PublisherSession sess, final String serialId,
			final MimeHeaders headers);

	/**
	 * The library executes this method when it finishes transferring a JAL
	 * record to a remote JALoP Network Store.
	 * 
	 * @param sess
	 *            The {@link PublisherSession} that sent the JAL record.
	 * @param serailId
	 *            The serial ID of the JAL record that was sent.
	 * @param record
	 *            The @{link SourceRecord} that was sent. This is the same
	 *            object that was returned from either
	 *            {@link Publisher#getNextRecord(PublisherSession, String)} or
	 *            {@link Publisher#onJournalResume(PublisherSession, String, long, MimeHeaders)}
	 * @return <tt>true</tt> to continue sending records on this session,
	 *         <tt>false</tt> otherwise.
	 */
	boolean onRecordComplete(final PublisherSession sess,
			final String serailId, final SourceRecord record);

	/**
	 * The library executes this method when it receives a "sync" message from
	 * the remote JALoP Network Store.
	 * 
	 * @param sess
	 *            The {@link PublisherSession} that received the "sync" message.
	 * @param serialId
	 *            The serial ID indicated in the "sync" message.
	 * @param headers
	 *            Any addition MIME headers in the message./
	 * @return <tt>true</tt> to continue sending records on this session,
	 *         <tt>false</tt> otherwise.
	 */
	boolean sync(final PublisherSession sess, final String serialId,
			final MimeHeaders headers);

	/**
	 * The library executes this method once it calculates the digest for a
	 * particular record.
	 * 
	 * @param sess
	 *            The {@link PublisherSession} that calculated the digest.
	 * @param serialId
	 *            The serial ID of the JAL record for this digest.
	 * @param digest
	 *            The digest value.
	 */
	void notifyDigest(final PublisherSession sess, final String serialId,
			final byte[] digest);

	/**
	 * The library executes this method after it receives a "digest" message
	 * from the remote JALoP Network Store.
	 * 
	 * @param sess
	 *            The {@link PublisherSession} that calculated the digest.
	 * @param digestPairs
	 *            A {@link Map} of serial IDs to {@link DigestPair}
	 *            <tt>Objects</tt> Each Key/Value in the map represents the
	 *            serial ID (as identified by the remote JALoP Network Store),
	 *            remotely calculated digest, and locally calculated digest.
	 */
	void notifyPeerDigest(final PublisherSession sess,
			final Map<String, DigestPair> digestPairs);
}
