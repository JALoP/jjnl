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

import javax.xml.soap.MimeHeaders;

/**
 * This represents a subscribe message.
 */
public class SyncMessage extends Message {
	private final String serialId;

	/**
	 * Create a new {@link SyncMessage}
	 *
	 * @param serialId
	 *            The serial ID sent in the message.
	 * @param otherHeaders
	 *            Any additional headers sent as part of this message.
	 */
	SyncMessage(String serialId, MimeHeaders otherHeaders) {
		super(otherHeaders);
		this.serialId = serialId;
	}

	/**
	 * Get the serial ID indicated in this message.
	 *
	 * @return the serialId
	 */
	public String getSerialId() {
		return serialId;
	}
}
