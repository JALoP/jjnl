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

import java.util.List;

import jakarta.xml.soap.MimeHeaders;

import com.tresys.jalop.jnl.ConnectionHandler.ConnectError;

/**
 * This represents an initialize-nack message.
 */
public class InitNackMessage extends Message {

	/** A list of errors indicating why this connection cannot be established. */
	private final List<ConnectError> errors;

	/**
	 * Create an InitNackMessage with the provided errors.
	 *
	 * @param errors
	 *            The list of errors.
	 * @param unknownHeaders
	 *            Any additional MIME headers.
	 */
	public InitNackMessage(final List<ConnectError> errors,
			final MimeHeaders unknownHeaders) {
		super(unknownHeaders);
		this.errors = errors;
	}

	/**
	 * Get the list of errors.
	 *
	 * @return The list of {@link ConnectError}s.
	 */
	public List<ConnectError> getErrorList() {
		return this.errors;
	}

}
