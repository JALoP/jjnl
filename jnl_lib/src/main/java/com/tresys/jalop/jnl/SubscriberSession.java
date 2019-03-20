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
 * This represents a {@link Session} that is receiving JALoP records from a
 * remote JALoP Network Store. The {@link SubscriberSession} is responsible for
 * parsing the JALoP messages from a remote JALoP Network Store that contains
 * JAL Records. Additionally, the {@link SubscriberSession} will generate the
 * message digest for the JAL records and periodically send "digest" messages to
 * the remote JALoP Network Store. The {@link SubscriberSession} does not
 * normally send "digest" messages immediately, but will queue up calculated
 * digests, and wait until either a timeout is reached, or a maximum limit on
 * the number of outstanding digest values is met. The length of this timeout
 * and the size of the queue are controlled by calls to
 * {@link SubscriberSession#setDigestTimeout(long)} and
 * {@link SubscriberSession#setPendingDigestMax(int)}.
 */
public interface SubscriberSession extends Session {
	/**
	 * Configure the maximum amount of time to wait between sending a "digest"
	 * message. A value <= 0 will cause the {@link SubscriberSession} to send
	 * "digest" messages immediately.
	 *
	 * @param pendingDigestTimeoutSeconds
	 *            The time to wait, in seconds before sending a "digest"
	 *            message.
	 */
	void setDigestTimeout(int pendingDigestTimeoutSeconds);

	/**
	 * Configure the maximum number of digests to queue before sending a
	 * "digest" message. Any value <= 0 will cause the {@link SubscriberSession}
	 * to send "digest" messages immediately.
	 *
	 * @param pendingDigestMax
	 *            The maximum number of digests to queue.
	 */
	void setPendingDigestMax(int pendingDigestMax);

    InetAddress getAddress();

    String getPublisherId();
}
