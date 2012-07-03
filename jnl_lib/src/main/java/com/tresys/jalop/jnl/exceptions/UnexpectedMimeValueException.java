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
package com.tresys.jalop.jnl.exceptions;

/**
 * This is an exception that is when a MIME header contains an unexpected value.
 */
public class UnexpectedMimeValueException extends JNLException {
	private static final long serialVersionUID = 1L;
	private final String expected;
	private final String actual;
	private final String headerName;

	/**
	 * Create an exception for when a MIME header contains an unexpected value.
	 *
	 * @param headerName
	 *            The name of the MIME header.
	 *
	 * @param expected
	 *            The expected message type.
	 * @param actual
	 *            The actual message type.
	 *
	 */
	public UnexpectedMimeValueException(final String headerName,
			final String expected, final String actual) {
		super("Was expecting '" + expected + " for " + "'" + headerName
				+ "', found '" + actual + "'");
		this.headerName = headerName;
		this.expected = expected;
		this.actual = actual;
	}

	/**
	 * Get a string that describes the expected values.
	 *
	 * @return The expected values.
	 */
	public String getExpectedMessage() {
		return this.expected;
	}

	/**
	 * Get the actual value for of the MIME header.
	 *
	 * @return The actual message type.
	 */
	public String getFoundMessage() {
		return this.actual;
	}

	/**
	 * Get the name of the MIME header.
	 *
	 * @return The relevant MIME header.
	 */
	public String getHeaderName() {
		return this.headerName;
	}
}
