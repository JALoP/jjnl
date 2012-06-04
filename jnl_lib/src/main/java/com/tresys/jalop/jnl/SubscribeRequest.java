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

import java.io.InputStream;

/**
 * A {@link SubscriberSession} obtains {@link SubscribeRequest}s from the
 * application provided {@link Subscriber}. Once the {@link SubscriberSession}
 * has established a BEEP connection to a remote, it requests a
 * {@link SubscribeRequest} from the {@link Subscriber} by calling
 * {@link Subscriber#getSubscribeRequest(SubscriberSession)}.
 */
public interface SubscribeRequest {
	/**
	 * Special case Serial ID to indicated the Publisher should start sending
	 * with the oldest records.
	 */
	final String EPOC = "0";

	/**
	 * The library will call this method to determine the serial ID to send as
	 * part of of a "subscribe" or "journal-resume" message.
	 * 
	 * @return the serial ID to send.
	 */
	String getSerialId();

	/**
	 * For a "journal-resume", retrieve the number bytes that were previously
	 * transferred.The Library ignores the return value for audit and log
	 * records. If the value returned by this function is <= 0, or <tt>null</tt>
	 * is returned by {@link SubscribeRequest#getResumeInputStream()}, no resume
	 * is attempted, and a "subscribe" request is generated.
	 * 
	 * @return the number of bytes that were previously transfered.
	 */
	long getResumeOffset();

	/**
	 * For a "journal-resume", retrieve an {@link InputStream} of the previously
	 * transferred journal data. The library needs this input stream so it may
	 * calculate the digest value for the record. The library does not call this
	 * function for audit or log records. If the value returned by this function
	 * is <tt>null</tt>, or 0 is returned by
	 * {@link SubscribeRequest#getResumeOffset()}, no resume is attempted, and a
	 * "subscribe" request is generated.
	 * 
	 * @return an {@link InputStream} for the previously transferred data.
	 */
	InputStream getResumeInputStream();

}
