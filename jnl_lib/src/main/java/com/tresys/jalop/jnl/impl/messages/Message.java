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

import jakarta.xml.soap.MimeHeaders;

/**
 * Generic base class for JALoP Messages.
 */
public class Message {

	/**
	 * Any additional headers in the message that are not part of JALoP.
	 */
	private final MimeHeaders otherHeaders;

	/**
	 * Create a JALoP Message.
	 *
	 * @param otherHeaders
	 *            Any additional (unrecognized) headers;
	 */
	protected Message(MimeHeaders otherHeaders) {
		this.otherHeaders = otherHeaders;
	}

	/**
	 * @return Any MIME headers sent that are not part of JALoP.
	 */
	public MimeHeaders getOtherHeaders() {
		return this.otherHeaders;
	}
}
