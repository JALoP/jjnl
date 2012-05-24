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
 * A {@link DigestPair} represents both the locally and remotely calculated
 * digest for a single JAL record. The {@link Context} delivers
 * {@link DigestPair}s to a {@link Publisher}.
 * 
 */
public interface DigestPair {
	/**
	 * Retrieve the serial ID for the record.
	 * 
	 * @return the serial ID
	 */
	String getSerialId();

	/**
	 * Retrieve the digest value that the {@link Context} calculated for this
	 * particular record. If {@link DigestPair#getDigestStatus()} reports
	 * {@link DigestStatus#Unknown}, this method returns null.
	 * 
	 * @return A byte array that is the message digest.
	 */
	byte[] getLocalDigest();

	/**
	 * Retrieve the digest value that the remote JALoP Network Store calculated
	 * for this particular record.
	 * 
	 * @return A byte array that is the message digest.
	 */
	byte[] getPeerDigest();

	/**
	 * Retrieve the status that was reported to the remote JALoP Network Store.
	 * 
	 * @return The {@link DigestStatus}.
	 */
	DigestStatus getDigestStatus();
}
