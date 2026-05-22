/**
 * Copyright (C) 2026 Concurrent Technologies Corporation.
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

import static org.junit.Assert.assertEquals;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import mockit.*;

import org.junit.Before;
import org.junit.Test;

import com.tresys.jalop.jnl.RecordType;
import com.tresys.jalop.jnl.Role;
import com.tresys.jalop.jnl.exceptions.JNLException;

public class ConnectionRequestTest {

	private List<String> encodings;
    private List<String> digests;
    private InetAddress address;

    @Before
    public void setUp() throws Exception {
        encodings = new ArrayList<String>();
        encodings.add("enc_foo");
        encodings.add("enc_bar");

        digests = new ArrayList<String>();
        digests.add("dgst_foo");
        digests.add("dgst_bar");
        address = InetAddress.getByName("localhost");
    }

    @Test
	public void testConnectionRequestConstructorWorks() throws JNLException {
		final ConnectionRequestImpl cr = new ConnectionRequestImpl(address.getHostAddress(), RecordType.Log, 1, encodings, digests, Role.Publisher, "agent");
		assertEquals(address.getHostAddress(), cr.getAddress());
		assertEquals(RecordType.Log, cr.getRecordType());
		assertEquals(1, cr.getJalopVersion());
		assertEquals(encodings, cr.getXmlEncodings());
		assertEquals(digests, cr.getMessageDigests());
		assertEquals(Role.Publisher, cr.getRole());
		assertEquals("agent", cr.getAgent());
	}

	@Test(expected = JNLException.class)
	public void testConstructorThrowsExceptionWithUnsetRecordType() throws JNLException {
		new ConnectionRequestImpl(address.getHostAddress(), RecordType.Unset, 1, encodings, digests, Role.Publisher, "agent");
	}

	@Test
	public void testSetSelectedWorks() throws JNLException {
		final ConnectionRequestImpl cr = new ConnectionRequestImpl(address.getHostAddress(), RecordType.Log, 1, encodings, digests, Role.Publisher, "agent");
		cr.setSelectedMessageDigest(digests.get(0));
		cr.setSelectedXmlEncoding(encodings.get(0));
		assertEquals(digests.get(0), cr.getSelectedXmlDigest());
		assertEquals(encodings.get(0), cr.getSelectedXmlEncoding());
	}
}
