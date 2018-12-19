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

package com.tresys.jalop.jnl.impl.publisher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.net.InetAddress;
import java.util.Map;

import javax.xml.crypto.dsig.DigestMethod;

import mockit.*;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.beepcore.beep.core.BEEPException;
import org.beepcore.beep.core.Channel;
import org.beepcore.beep.core.RequestHandler;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.tresys.jalop.jnl.Publisher;
import com.tresys.jalop.jnl.RecordType;
import com.tresys.jalop.jnl.Role;
import com.tresys.jalop.jnl.exceptions.JNLException;
import com.tresys.jalop.jnl.impl.ContextImpl;
import com.tresys.jalop.jnl.impl.SessionImpl;

public class PublisherSessionImplTest {

	private static Field errored;
	private static Field digestMapField;

	@BeforeClass
	public static void setupBeforeClass() throws SecurityException,
			NoSuchFieldException {
		errored = SessionImpl.class.getDeclaredField("errored");
		errored.setAccessible(true);

		digestMapField = PublisherSessionImpl.class.getDeclaredField("digestMap");
        digestMapField.setAccessible(true);
	}

	@SuppressWarnings("unchecked")
	private static Map<String, byte[]> getDigestMap(final PublisherSessionImpl p)
			throws IllegalArgumentException, IllegalAccessException {
		return (Map<String,  byte[]>) digestMapField.get(p);
	}

	@Before
	public void setUp() {
		// Disable logging so the build doesn't get spammed.
		Logger.getRootLogger().setLevel(Level.OFF);
	}

	@Test
	public void testConstructorWorks(@Mocked final InetAddress address, @Mocked final ContextImpl contextImpl,
			@Mocked final Publisher publisher, @Mocked final org.beepcore.beep.core.Session sess)
			throws IllegalArgumentException, IllegalAccessException {

		final PublisherSessionImpl p = new PublisherSessionImpl(address, RecordType.Log, publisher,
				DigestMethod.SHA256, "xml", 0, sess, contextImpl);

		assertEquals(DigestMethod.SHA256, p.getDigestMethod());
		assertEquals(RecordType.Log, p.getRecordType());
		assertEquals(Role.Publisher, p.getRole());
		assertEquals(publisher, p.getPublisher());
		assertEquals("xml", p.getXmlEncoding());
		assertFalse(errored.getBoolean(p));
		assertEquals(0, p.getChannelNum());
		assertEquals(sess, p.getSession());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testConstructorThrowsExceptionForNullPublisher(@Mocked final ContextImpl contextImpl,
			@Mocked final org.beepcore.beep.core.Session sess, @Mocked final InetAddress address) {
		new PublisherSessionImpl(address, RecordType.Log, null,
				DigestMethod.SHA256, "xml", 0, sess, contextImpl);
	}

	@Test
	public void testRunWorks(@Mocked final ContextImpl contextImpl, @Mocked final Publisher publisher,
			@Mocked final org.beepcore.beep.core.Session sess, @Mocked final InetAddress address, @Mocked final Channel channel)
			throws BEEPException {

		final PublisherSessionImpl p = new PublisherSessionImpl(address, RecordType.Log, publisher,
				DigestMethod.SHA256, "xml", 0, sess, contextImpl);


		new Expectations(p) {
			{
				sess.startChannel(anyString, false, anyString); result = channel;
			}
		};

		p.run();

		new VerificationsInOrder() {
			{
				channel.setRequestHandler((RequestHandler) any);
			}
		};
	}

	@Test
	public final void testAddDigestWorks(@Mocked final ContextImpl contextImpl, @Mocked final Publisher publisher,
			@Mocked final org.beepcore.beep.core.Session sess, @Mocked final InetAddress address)
			throws JNLException, IllegalAccessException {

		final PublisherSessionImpl p = new PublisherSessionImpl(address, RecordType.Log, publisher,
				DigestMethod.SHA256, "xml", 0, sess, contextImpl);
		final byte[] local = "local".getBytes();
		final String nonce = "nonce";
		p.addDigest(nonce, local);
		final Map<String, byte[]> map = getDigestMap(p);
		assertTrue(map.containsKey(nonce));
		assertEquals(local, map.get(nonce));
	}

	@Test(expected = JNLException.class)
	public final void testAddDigestThrowsExceptionWithDuplicate(@Mocked final ContextImpl contextImpl,
			@Mocked final Publisher publisher, @Mocked final org.beepcore.beep.core.Session sess, @Mocked final InetAddress address)
			throws JNLException, IllegalAccessException {

		final PublisherSessionImpl p = new PublisherSessionImpl(address, RecordType.Log, publisher,
				DigestMethod.SHA256, "xml", 0, sess, contextImpl);
		final byte[] local = "local".getBytes();
		final String nonce = "nonce";
		p.addDigest(nonce, local);
		p.addDigest(nonce, local);
	}

	@Test
	public final void testFetchAndRemoveWorks(@Mocked final ContextImpl contextImpl,@Mocked final Publisher publisher,
			@Mocked final org.beepcore.beep.core.Session sess, @Mocked final InetAddress address)
			throws JNLException, IllegalAccessException {

		final PublisherSessionImpl p = new PublisherSessionImpl(address, RecordType.Log, publisher,
				DigestMethod.SHA256, "xml", 0, sess, contextImpl);
		final byte[] local = "local".getBytes();
		final String nonce = "nonce";
		p.addDigest(nonce, local);
		final byte[] fetched = p.fetchAndRemoveDigest(nonce);
		assertEquals(local, fetched);
		final Map<String, byte[]> map = getDigestMap(p);
		assertFalse(map.containsKey(nonce));
	}

}
