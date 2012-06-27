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

import java.util.Map;

import javax.xml.soap.MimeHeaders;

/**
 * This represents a digest message.
 */
public class DigestMessage extends Message {
	/**
	 * Maps serialIDs to digests
	 */
	private Map<String, String> digestMap;

	/**
	 * Create a new {@link DigestMessage}
	 * 
	 * @param serialId
	 *            The map of serialIDs and digests sent in the message.
	 * @param otherHeaders
	 *            Any additional headers sent as part of this message.
	 */
	DigestMessage(Map<String, String> map, MimeHeaders otherHeaders) {
		super(otherHeaders);
		this.digestMap = map;
	}

	/**
	 * Get the map of serialIDs and digests in this message.
	 * 
	 * @return the map
	 */
	public Map<String, String> getMap() {
		return digestMap;
	}
}
