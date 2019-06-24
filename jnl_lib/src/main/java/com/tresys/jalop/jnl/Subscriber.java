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

package com.tresys.jalop.jnl;

import java.io.InputStream;

/**
 * Users of this library should provide an <tt>Object</tt> that implements this
 * interface if they wish to act as a JALoP Subscriber. Their <tt>Object</tt>
 * must be registered with a {@link Context} prior to initiating, or listening
 * for, connections.
 *
 * @see Context#registerSubscriber(Subscriber)
 */
public interface Subscriber {
	/**
	 * A {@link SubscriberSession} executes this method so it may generate a
	 * "subscribe" or "journal-resume" message. If the {@link Subscriber}
	 * returns <tt>null</tt>, or an otherwise invalid {@link SubscribeRequest},
	 * then the {@link SubscriberSession} will shutdown.
	 *
	 * @param sess
	 *            The {@link SubscriberSession}
	 * @return Details to send in a "subscribe" or "journal-resume" message.
	 */
	SubscribeRequest getSubscribeRequest(SubscriberSession sess);

	/**
	 * The {@link SubscriberSession} executes this method to deliver the system
	 * meta-data for a specific record. The meta-data will be encoded (EXI,
	 * gzip, deflate, etc) as indicated by {@link Session#getXmlEncoding()}.
	 * Applications must read all the data from <tt>sys-metadata</tt> before
	 * returning from this function. Once control is returned to the library,
	 * the {@link SubscriberSession} will drain <tt>sysMetaData</tt>, and the
	 * contents will no longer be available.
	 *
	 * @param sess
	 *            The {@link SubscriberSession} that received the data.
	 * @param recordInfo
	 *            Details of the record, such as the size of the application
	 *            meta-data.
	 * @param sysMetaData
	 *            An {@link InputStream} that is the system meta-data. There is
	 *            no need to call {@link InputStream#close()} on
	 *            <tt>sysMetaData</tt>; the {@link SubscriberSession} handles
	 *            that internally.
	 * @return true to continue receiving JAL records on this
	 *         {@link SubscriberSession}, false otherwise.
	 */
	boolean notifySysMetadata(SubscriberSession sess,
			final RecordInfo recordInfo, InputStream sysMetaData);

	/**
	 * The {@link SubscriberSession} executes this method to deliver the
	 * application meta-data for a specific record. The meta-data will be
	 * encoded (EXI, gzip, deflate, etc) as indicated by
	 * {@link Session#getXmlEncoding()}. Applications must read all the data
	 * from <tt>appMetaData</tt> before returning from this function. Once
	 * control is returned to the library, the {@link SubscriberSession} will
	 * drain <tt>appMetaData</tt>, and the contents will no longer be available.
	 *
	 * @param sess
	 *            The {@link SubscriberSession} that received the data.
	 * @param recordInfo
	 *            Details of the record, such as the size of the application
	 *            meta-data.
	 * @param appMetaData
	 *            An {@link InputStream} that is the system meta-data. There is
	 *            no need to call {@link InputStream#close()} on
	 *            <tt>appMetaData</tt>; the {@link SubscriberSession} handles
	 *            that internally.
	 * @return true to continue receiving JAL records on this
	 *         {@link SubscriberSession}, false otherwise.
	 */
	boolean notifyAppMetadata(SubscriberSession sess,
			final RecordInfo recordInfo, InputStream appMetaData);

	/**
	 * The {@link SubscriberSession} executes this method to deliver the payload
	 * for a specific record. If this is an audit record, the payload will be
	 * encoded (EXI, gzip, deflate, etc) as indicated by
	 * {@link Session#getXmlEncoding()}. Applications must read all the data
	 * from <tt>payload</tt> before returning from this function. Once control
	 * is returned to the library, the {@link SubscriberSession} will drain
	 * <tt>payload</tt>, and the contents will no longer be available.
	 *
	 * @param sess
	 *            The {@link SubscriberSession} that received the data.
	 * @param recordInfo
	 *            Details of the record, such as the size of the application
	 *            meta-data.
	 * @param payload
	 *            An {@link InputStream} that is the payload. There is no need
	 *            to call {@link InputStream#close()} on this <tt>payload</tt>;
	 *            the {@link SubscriberSession} handles that internally.
	 * @return true to continue receiving JAL records on this
	 *         {@link SubscriberSession}, false otherwise.
	 */
	boolean notifyPayload(SubscriberSession sess, final RecordInfo recordInfo,
			InputStream payload);

	/**
	 * The {@link SubscriberSession} executes this method to notify the
	 * application of the digest value it calculated for a specific JAL record.
	 *
	 * @param sess
	 *            The {@link SubscriberSession} that calculated the digest
	 *            value.
	 * @param recordInfo
	 *            The details (nonce, etc) of the record this digest applies
	 *            to.
	 * @param digest
	 *            The digest value.
	 * @return true to continue receiving JAL records on this
	 *         {@link SubscriberSession}, false otherwise.
	 */
	boolean notifyDigest(SubscriberSession sess, final RecordInfo recordInfo,
			final byte[] digest);

    /**
     * The {@link SubscriberSession} executes this method to notify the
     * application of the need to remove the jal record due to a journal missing message
     * from the publisher.
     *
     * @param sess
     *            The {@link SubscriberSession} that calculated the digest
     *            value.
     * @param recordInfo
     *            The details (nonce, etc) of the record this digest applies
     *            to.
     * @param jalId
     *            The JAL Id of the record to be deleted from the output directory
     * @return true successful deletion of jal record
     *         {@link SubscriberSession}, false otherwise.
     */
    boolean notifyJournalMissing(final SubscriberSession sess,
            final String jalId);

	/**
	 * The {@link SubscriberSession} executes this method to notify the
	 * application when it receives a digest.
	 *
	 * @param sess
	 *            The {@link SubscriberSession} that received the message.
	 * @param nonce
	 *            Network nonce associated with digest status.
	 * @param status
	 *            {@link DigestStatus}, indicating if the
	 *            remote JALoP Network Store agrees with the digest value
	 *            calculated locally for the specified nonce.
     * @param testMode
     *            {boolean}, true if running in test mode, this will create an extra
     *            empty zero byte "confirmed" file in the confirm dir for the record
     *            so sub-test.sh stress test script will know which records it can successfully purge.
	 * @return true to continue receiving JAL records on this
	 *         {@link SubscriberSession}, false otherwise.
	 */
	boolean notifyDigestResponse(SubscriberSession sess,
			final String nonce, final DigestStatus status, boolean testMode);

	/**
	 * The {@link ContextImpl} executes this method to get the
	 * {@link Mode} to create the {@link SubscribeRequest}.
	 *
	 * @return {@link Mode}, either Archive or Live
	 */
	Mode getMode();

    //Methods below are to support session management
    Session getSessionBySessionId(String sessionId);

    boolean removeSession(String sessionId);

    void prepareForNewSession();

    boolean getTestMode();
}
