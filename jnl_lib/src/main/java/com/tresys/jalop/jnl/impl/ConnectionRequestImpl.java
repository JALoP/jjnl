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

package com.tresys.jalop.jnl.impl;

import java.net.InetAddress;
import java.util.List;

import com.tresys.jalop.jnl.ConnectionRequest;
import com.tresys.jalop.jnl.RecordType;
import com.tresys.jalop.jnl.Role;
import com.tresys.jalop.jnl.exceptions.JNLException;

/**
 * Implementation of a {@link ConnectionRequest}.
 */
public class ConnectionRequestImpl implements ConnectionRequest {

	final InetAddress address;
	final RecordType recordType;
	final int jalopVersion;
	final List<String> xmlEncodings;
	final List<String> messageDigests;
	final Role role;
	final String agent;
	String selectedXmlEncoding;
	String selectedXmlDigest;

	/**
	 * Create a {@link ConnectionRequest} object.
	 *
	 * @param address
	 * 				The {@link InetAddress} being used for this connection.
	 * @param recordType
	 * 				The {@link RecordType} being requested for this connection.
	 * @param jalopVersion
	 * 				The version of JALoP requested for this connection.
	 * @param xmlEncodings
	 * 				The list of xml encodings that could be allowed.
	 * @param messageDigests
	 * 				The list of message digests that could eb allowed.
	 * @param role
	 * 				The {@link Role} that the initiator of the connection will have.
	 * @param agent
	 * 				The agent that is requested for this connection.
	 * @throws JNLException
	 * 				if an invalid (@link RecordType} is sent
	 */
	public ConnectionRequestImpl(final InetAddress address, final RecordType recordType,
			final int jalopVersion, final List<String> xmlEncodings,
			final List<String> messageDigests, final Role role, final String agent) throws JNLException {

		if(recordType == null || recordType == RecordType.Unset) {
			throw new JNLException("Cannot establish a connection with a RecordType of 'Unset'");
		}

		this.address = address;
		this.recordType = recordType;
		this.jalopVersion = jalopVersion;
		this.xmlEncodings = xmlEncodings;
		this.messageDigests = messageDigests;
		this.role = role;
		this.agent = agent;
	}

	@Override
	public InetAddress getAddress() {
		return this.address;
	}

	@Override
	public RecordType getRecordType() {
		return this.recordType;
	}

	@Override
	public int getJalopVersion() {
		return this.jalopVersion;
	}

	@Override
	public List<String> getXmlEncodings() {
		return this.xmlEncodings;
	}

	@Override
	public List<String> getMessageDigests() {
		return this.messageDigests;
	}

	@Override
	public Role getRole() {
		return this.role;
	}

	@Override
	public String getAgent() {
		return this.agent;
	}

	@Override
	public void setSelectedXmlEncoding(final String encoding) {
		this.selectedXmlEncoding = encoding;
	}

	@Override
	public void setSelectedMessageDigest(final String messageDigest) {
		this.selectedXmlDigest = messageDigest;
	}

	@Override
	public String getSelectedXmlEncoding() {
		return this.selectedXmlEncoding;
	}

	@Override
	public String getSelectedXmlDigest() {
		return this.selectedXmlDigest;
	}

}
