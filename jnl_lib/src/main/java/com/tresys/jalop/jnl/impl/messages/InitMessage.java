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
package com.tresys.jalop.jnl.impl.messages;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.soap.MimeHeaders;

import com.tresys.jalop.jnl.Mode;
import com.tresys.jalop.jnl.RecordType;
import com.tresys.jalop.jnl.Role;

/**
 * This represents the 'initialize' message received from a remote.
 */
public class InitMessage extends Message {
	/**
	 * Stores the list of proposed digest algorithms.
	 */
	private final List<String> acceptDigests;

	/**
	 * Stores the list of proposed XML encodings.
	 */
	private final List<String> acceptEncodings;

	/**
	 * Stores the JAL-Agent string (if any).
	 */
	private final String agentString;

	/**
	 * Stores the proposed {@link RecordType} to transfer over this channel.
	 */
	private final RecordType recordType;

	/**
	 * Stores the proposed {@link Role}
	 */
	private final Role role;

	/**
	 * Stores the proposed {@link Mode}
	 */
	private final Mode mode;

	/**
	 * Create an {@link InitMessage} object.
	 * 
	 * @param recordType
	 *            The type of record to transfer over this connection
	 * @param role
	 *            The role indicated in the message
	 * @param mode
	 *            The mode indicated in the message
	 * @param encodingsArray
	 *            The list of proposed encodings.
	 * @param digestsArray
	 *            The list of proposed digest algorithms.
	 * @param agent
	 *            The JALoP Agent string
	 * @param otherHeaders
	 *            Any additional (unrecognized) headers
	 */
	public InitMessage(final RecordType recordType, final Role role, final Mode mode,
			String[] encodingsArray, String[] digestsArray, final String agent,
			final MimeHeaders otherHeaders) {
		super(otherHeaders);
		this.recordType = recordType;
		this.role = role;
		this.mode = mode;

		this.agentString = agent;
		List<String> encodingsList;
		if (encodingsArray == null) {
			encodingsArray = new String[0];
		}
		encodingsList = Arrays.asList(encodingsArray);
		if (encodingsList.isEmpty()) {
			encodingsList = new ArrayList<String>(1);
			encodingsList.add("none");
		}
		this.acceptEncodings = encodingsList;

		if (digestsArray == null) {
			digestsArray = new String[0];
		}
		List<String> digestList = Arrays.asList(digestsArray);
		if (digestList.isEmpty()) {
			digestList = new ArrayList<String>(1);
			digestList.add("sha256");
		}
		this.acceptDigests = digestList;

	}

	/**
	 * Get the ordered list of proposed digest algorithms.
	 * 
	 * @return The list of proposed digest algorithms
	 */
	public List<String> getAcceptDigests() {
		return this.acceptDigests;
	}

	/**
	 * Get the ordered list of proposed digest encodings.
	 * 
	 * @return The list of proposed encodings.
	 */
	public List<String> getAcceptEncodings() {
		return this.acceptEncodings;
	}

	/**
	 * Retrieve the JAL-Agent string (if any).
	 * 
	 * @return The JAL-Agent string indicated in the message
	 */
	public String getAgentString() {
		return this.agentString;
	}

	/**
	 * Retrieve the {@link RecordType} that the should be transferred over this
	 * channel.
	 * 
	 * @return The RecordType indicated in the message.
	 */
	public RecordType getRecordType() {
		return this.recordType;
	}

	/**
	 * Retrieve the {@link Role} that should be adopoted.
	 * 
	 * @return The Role indicated in the message.
	 */
	public Role getRole() {
		return this.role;
	}

	/**
	 * Retrieve the {@link Mode} that should be adopted.
	 * 
	 * @return The Mode indicated in the message.
	 */
	public Mode getMode() {
		return this.mode;
	}
}
