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
package com.tresys.jalop.utils.jnltest.Config;

import java.util.HashSet;
import java.util.Set;

import com.tresys.jalop.jnl.RecordType;

/**
 * A {@link PeerConfig} is part of the JNLTest configuration file. Each
 * {@link PeerConfig} indicates the types of operations a remote JALoP Network
 * Store may perform.
 */
public class PeerConfig {
	/** The types of records the remote may publish. */
	private Set<RecordType> publishAllow;
	/** The types of records the remote may subscribe. */
	private Set<RecordType> subscribeAllow;

	/**
	 * Create a new {@link PeerConfig}. This creates a {@link PeerConfig} that
	 * is not allowed to publish or subscribe any records.
	 */
	public PeerConfig() {
		this.publishAllow = new HashSet<RecordType>();
		this.subscribeAllow = new HashSet<RecordType>();
	}

	/**
	 * Get the set of {@link RecordType}s that a peer may subscribe to.
	 * @return A set of {@link RecordType}s.
	 */
	public Set<RecordType> getSubscribeAllow() {
		return subscribeAllow;
	}

	/**
	 * Get the set of {@link RecordType}s that a peer may publish.
	 * @return A set of {@link RecordType}s.
	 */
	public Set<RecordType> getPublishAllow() {
		return publishAllow;
	}
}
