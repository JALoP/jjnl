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

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import jakarta.xml.soap.MimeHeaders;

import org.junit.Test;

import com.tresys.jalop.jnl.DigestStatus;

public class DigestResponseTest {
	@Test
	public void testDigestResponseWorks() {
		Map<String, DigestStatus> digests = new HashMap<String, DigestStatus>();
		digests.put("12345", DigestStatus.Confirmed);
		digests.put("12346", DigestStatus.Invalid);
		digests.put("12347", DigestStatus.Unknown);
		
		MimeHeaders mimeHeaders = new MimeHeaders();
		DigestResponse dr = new DigestResponse(digests, mimeHeaders);
		assertEquals(dr.getMap(), digests);
		assertEquals(dr.getOtherHeaders(), mimeHeaders);
	}
}
