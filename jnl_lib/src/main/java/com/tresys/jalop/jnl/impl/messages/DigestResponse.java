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
package com.tresys.jalop.jnl.impl.messages;

import java.util.Map;

import javax.xml.soap.MimeHeaders;

import com.tresys.jalop.jnl.DigestStatus;

/**
 * This represents a digest response.
 */
public class DigestResponse extends Message {
	/*
	 * Contains a mapping of nonces to DigestStatus
	 */
	private final Map<String, DigestStatus> statusMap;

	/**
	 * Create a new {@link DigestResponse}
	 *
	 * @param map
	 *            The map of nonces to DigestStatuses sent in the message.
	 * @param otherHeaders
	 *            Any additional headers sent as part of this message.
	 */
	public DigestResponse(Map<String, DigestStatus> map, MimeHeaders otherHeaders) {
		super(otherHeaders);
		this.statusMap = map;
	}

	/**
	 * Get the nonce indicated in this message.
	 *
	 * @return the nonce
	 */
	public Map<String, DigestStatus> getMap() {
		return statusMap;
	}
}
