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

package com.tresys.jalop.jnl.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;

import javax.xml.crypto.dsig.DigestMethod;

import mockit.*;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.beepcore.beep.core.BEEPError;
import org.beepcore.beep.core.BEEPException;
import org.beepcore.beep.core.Channel;
import org.beepcore.beep.core.Session;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.tresys.jalop.jnl.RecordType;
import com.tresys.jalop.jnl.Subscriber;
import com.tresys.jalop.jnl.impl.subscriber.SubscriberSessionImpl;

public class SessionImplTest {

	private static Field errored;

	@BeforeClass
	public static void setupBeforeClass() throws SecurityException,
			NoSuchFieldException {
		errored = SessionImpl.class.getDeclaredField("errored");
		errored.setAccessible(true);
	}

	@Before
	public void setUp() {
		// Disable logging so the build doesn't get spammed.
		Logger.getRootLogger().setLevel(Level.OFF);
	}

	@Test
	public void testIsOkTrueWorks(@Mocked final Subscriber subscriber,
			@Mocked final org.beepcore.beep.core.Session sess, @Mocked final InetAddress address)
			throws IllegalArgumentException, IllegalAccessException {
		final SubscriberSessionImpl s = new SubscriberSessionImpl(address,
				RecordType.Audit, subscriber, DigestMethod.SHA256, "barfoo", 1,
				2, 0, sess);

		new NonStrictExpectations() {
			{
				sess.getState();
				result = Session.SESSION_STATE_ACTIVE;
			}
		};

		assertTrue(s.isOk());
		assertEquals(org.beepcore.beep.core.Session.SESSION_STATE_ACTIVE,
				sess.getState());
		assertFalse(errored.getBoolean(s));
	}

	@Test
	public void testIsOkFalseWhenErrored(@Mocked final Subscriber subscriber,
			@Mocked final org.beepcore.beep.core.Session sess, @Mocked final InetAddress address)
			throws IllegalArgumentException, IllegalAccessException {
		final SubscriberSessionImpl s = new SubscriberSessionImpl(address,
				RecordType.Audit, subscriber, DigestMethod.SHA256, "barfoo", 1,
				2, 0, sess);

		new NonStrictExpectations() {
			{
				sess.getState();
				result = Session.SESSION_STATE_ACTIVE;
			}
		};

		s.setErrored();
		assertFalse(s.isOk());
		assertEquals(org.beepcore.beep.core.Session.SESSION_STATE_ACTIVE,
				sess.getState());
		assertTrue(errored.getBoolean(s));
	}

	@Test
	public void testIsOkFalseWhenInactive(@Mocked final Subscriber subscriber,
			final org.beepcore.beep.core.Session sess, @Mocked final InetAddress address)
			throws IllegalArgumentException, IllegalAccessException {
		final SubscriberSessionImpl s = new SubscriberSessionImpl(address,
				RecordType.Audit, subscriber, DigestMethod.SHA256, "barfoo", 1,
				2, 0, sess);

		assertFalse(s.isOk());
		assertTrue(org.beepcore.beep.core.Session.SESSION_STATE_ACTIVE != sess
				.getState());
		assertFalse(errored.getBoolean(s));
	}

	@Test
	public void testSetErroredWorks(@Mocked final Subscriber subscriber,
			@Mocked final org.beepcore.beep.core.Session sess, @Mocked final InetAddress address)
			throws IllegalArgumentException, IllegalAccessException {
		final SubscriberSessionImpl s = new SubscriberSessionImpl(address,
				RecordType.Audit, subscriber, DigestMethod.SHA256, "barfoo", 1,
				2, 0, sess);
		s.setErrored();
		assertTrue(errored.getBoolean(s));

	}

	@Test
	public void testGetDigestTypeWorks(@Mocked final Subscriber subscriber,
			@Mocked final org.beepcore.beep.core.Session sess, @Mocked final InetAddress address)
			throws SecurityException, NoSuchMethodException,
			IllegalAccessException, InvocationTargetException {

		final SubscriberSessionImpl s = new SubscriberSessionImpl(address,
				RecordType.Audit, subscriber, DigestMethod.SHA256, "barfoo", 1,
				2, 0, sess);

		final Method digestTypeMethod = SessionImpl.class.getDeclaredMethod(
				"getDigestType", String.class);
		digestTypeMethod.setAccessible(true);
		final String sha256Type = (String) digestTypeMethod.invoke(s,
				DigestMethod.SHA256);
		assertEquals("SHA-256", sha256Type);
		final String sha512Type = (String) digestTypeMethod.invoke(s,
				DigestMethod.SHA512);
		assertEquals("SHA-512", sha512Type);
		final String sha384Type = (String) digestTypeMethod.invoke(s,
				"http://www.w3.org/2001/04/xmldsig-more#sha384");
		assertEquals("SHA-384", sha384Type);
		final String invalid = (String) digestTypeMethod.invoke(s, "invalid");
		assertEquals("", invalid);
	}

	@Test
	public void testCreateDigestChannelWorks(@Mocked final Subscriber subscriber,
			@Mocked final org.beepcore.beep.core.Session sess, @Mocked final InetAddress address,
			@Mocked final Channel channel)
			throws BEEPError, BEEPException {

		final SubscriberSessionImpl s = new SubscriberSessionImpl(address,
				RecordType.Audit, subscriber, DigestMethod.SHA256, "barfoo", 1,
				2, 0, sess);

		assertNull(s.getDigestChannel());

		new NonStrictExpectations() {
			{
				sess.startChannel(anyString, false, anyString); result = channel;
			}
		};
		s.createDigestChannel();
		assertNotNull(s.getDigestChannel());
	}

	@Test
	public void testCreateDigestChannelOnlyCreatesIfNull(@Mocked final Subscriber subscriber,
			@Mocked final org.beepcore.beep.core.Session sess, @Mocked final InetAddress address,
			@Mocked final Channel channel)
			throws BEEPError, BEEPException {

		final SubscriberSessionImpl s = new SubscriberSessionImpl(address,
				RecordType.Audit, subscriber, DigestMethod.SHA256, "barfoo", 1,
				2, 0, sess);

		s.setDigestChannel(channel);
		s.createDigestChannel();
		assertEquals(channel, s.getDigestChannel());
	}
}
