/**
 * Copyright (C) 2026 Concurrent Technologies Corporation.
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
 * This represents a JALoP session acting as a publisher. At present, there are
 * no additional methods defined for a {@link Publisher}.
 */
public interface PublisherSession extends Session {
	/**
	 * The network store executes this method to give a record to the library
	 * to be sent across the network.
	 *
	 * @param rec
	 *            The record to send
	 */
	void sendRecord(final SourceRecord rec);

	/**
	 * Called by the publishing network store to indicate it is done sending records
	 */
	void complete();

	/**
	 * @param remoteNonce
	 * @return The local nonce corresponding to the remote nonce uuid.
	 */
	public String getLocalNonce(String remoteNonce);

	/**
	 * Deletes the entry for the remote nonce to local nonce mapping
	 *
	 * @param nonce
	 * 				A String which is the nonce for the calculated digest
	 */
	public void deleteNonceMapEntry(String remoteNonce);

	/**
	 * Adds the entry for the remote nonce to local nonce mapping
	 *
	 * @param remoteNonce
	 * 				A String which is the remote nonce for the calculated digest
	 * @param localNonce
	 * 				A String which is the local nonce for the calculated digest
	 */
	public boolean addNonceMapEntry(final String remoteNonce, final String localNonce);
}
