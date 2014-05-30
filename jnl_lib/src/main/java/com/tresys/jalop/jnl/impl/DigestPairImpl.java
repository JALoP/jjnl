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

package com.tresys.jalop.jnl.impl;

import com.tresys.jalop.jnl.DigestPair;
import com.tresys.jalop.jnl.DigestStatus;

/**
 * Implementation of a {@link DigestPair}. This holds the nonce, the locally calculated digest,
 * the digest calculated by the peer, and the {@link DigestStatus}.
 */
public class DigestPairImpl implements DigestPair {

	final String nonce;
	final byte[] localDigest;
	final byte[] peerDigest;
	final DigestStatus digestStatus;

	/**
	 * Create a {@link DigestPairImpl} object. The object is created with a
	 * status of Unknown.
	 *
	 * @param nonce
	 * 				A String which is the nonce for the calculated digests
	 * @param localDigest
	 * 				A byte[] which is the digest calculated locally by the publisher
	 * @param peerDigest
	 * 				A byte[] which is the digest calculated remotely by the subscriber
	 * @param digestStatus
	 * 				The {@link DigestStatus} indicating whether the two digests matched.
	 */
	public DigestPairImpl(final String nonce, final byte[] localDigest, final byte[] peerDigest,
			final DigestStatus digestStatus) {
		this.nonce = nonce;
		this.localDigest = localDigest;
		this.peerDigest = peerDigest;
		this.digestStatus = digestStatus;
	}

	@Override
	public String getNonce() {
		return this.nonce;
	}

	@Override
	public byte[] getLocalDigest() {
		return this.localDigest;
	}

	@Override
	public byte[] getPeerDigest() {
		return this.peerDigest;
	}

	@Override
	public DigestStatus getDigestStatus() {
		return this.digestStatus;
	}

}
