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

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import javax.xml.soap.MimeHeaders;

import org.junit.Test;

import com.tresys.jalop.jnl.Mode;
import com.tresys.jalop.jnl.RecordType;
import com.tresys.jalop.jnl.Role;

/**
 * Tests for the InitMessage class.
 */
public class TestInitMessage {

	@Test
	public void testInitMessage() {

		String[] encodings = new String[]{Utils.BINARY};
		String[] digests = new String[]{Utils.DGST_SHA256};
		MimeHeaders otherHeaders = new MimeHeaders();

		InitMessage init =  new InitMessage(RecordType.Audit, Role.Publisher, Mode.Live, 
					encodings, digests, "agent", otherHeaders);

		assertEquals(init.getAcceptDigests(), Arrays.asList(digests));
		assertEquals(init.getAcceptEncodings(), Arrays.asList(encodings));
		assertEquals(init.getAgentString(), "agent");
		assertEquals(init.getOtherHeaders(), otherHeaders);
		assertEquals(init.getRecordType(), RecordType.Audit);
		assertEquals(init.getRole(), Role.Publisher);
		assertEquals(init.getMode(), Mode.Live);
	}

	@Test
	public void testInitMessageNoEncoding() {

		InitMessage init =  new InitMessage(RecordType.Audit, Role.Publisher, Mode.Live, null,
				new String[]{Utils.DGST_SHA256}, "agent", new MimeHeaders());

		assertEquals(init.getAcceptEncodings(), Arrays.asList("none"));
	}

	@Test
	public void testInitMessageNoDigests() {

		InitMessage init =  new InitMessage(RecordType.Audit, Role.Publisher, Mode.Live,
					new String[]{Utils.BINARY}, null, "agent", new MimeHeaders());

		assertEquals(init.getAcceptDigests(), Arrays.asList("sha256"));

	}
}
