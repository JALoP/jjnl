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
package com.tresys.jalop.jnl.impl.subscriber;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

import javax.xml.crypto.dsig.DigestMethod;

import mockit.NonStrictExpectations;
import mockit.VerificationsInOrder;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.beepcore.beep.core.BEEPException;
import org.beepcore.beep.core.Channel;
import org.beepcore.beep.core.OutputDataStream;
import org.beepcore.beep.core.Session;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.tresys.jalop.jnl.RecordType;
import com.tresys.jalop.jnl.Role;
import com.tresys.jalop.jnl.Subscriber;
import com.tresys.jalop.jnl.impl.DigestListener;

public class SubscriberSessionImplTest {

    private static Field errored;
    private static Field digestMapField;
    @BeforeClass
    public static void setupBeforeClass() throws SecurityException, NoSuchFieldException {
        errored = SubscriberSessionImpl.class.getDeclaredField("errored");
        errored.setAccessible(true);
        digestMapField = SubscriberSessionImpl.class.getDeclaredField("digestMap");
        digestMapField.setAccessible(true);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String> getDigestMap(final SubscriberSessionImpl s)
            throws IllegalArgumentException, IllegalAccessException {
        return (Map<String, String>) digestMapField.get(s);
    }

    @Before
    public void setUp() {
        // Disable logging so the build doesn't get spammed.
        Logger.getRootLogger().setLevel(Level.OFF);
    }

    @Test
	public void testSubscriberImplConstructor(final InetAddress address, final Subscriber subscriber, final org.beepcore.beep.core.Session sess)
			throws IllegalArgumentException, IllegalAccessException {
        SubscriberSessionImpl s =
            new SubscriberSessionImpl(address, RecordType.Audit, subscriber, "foobar",
                                      "barfoo", 1, 2, 0, sess);
        assertEquals(DigestMethod.SHA256, s.getDigestMethod());
        assertEquals(2, s.getPendingDigestMax());
        assertEquals(1, s.getPendingDigestTimeoutSeconds());
        assertEquals(RecordType.Audit, s.getRecordType());
        assertEquals(Role.Subscriber, s.getRole());
        assertEquals(subscriber, s.getSubscriber());
        assertEquals("barfoo", s.getXmlEncoding());
        assertFalse(errored.getBoolean(s));
        assertNotNull(s.getListener());
        assertEquals(0, s.getChannelNum());
        assertEquals(sess, s.getSession());

        s = new SubscriberSessionImpl(address, RecordType.Journal, subscriber, "foobar",
                                      "barfoo", 1, 2, 0, sess);
        assertEquals(DigestMethod.SHA256, s.getDigestMethod());
        assertEquals(2, s.getPendingDigestMax());
        assertEquals(1, s.getPendingDigestTimeoutSeconds());
        assertEquals(RecordType.Journal, s.getRecordType());
        assertEquals(Role.Subscriber, s.getRole());
        assertEquals(subscriber, s.getSubscriber());
        assertEquals("barfoo", s.getXmlEncoding());
        assertNotNull(s.getListener());
        assertFalse(errored.getBoolean(s));
        assertEquals(0, s.getChannelNum());
        assertEquals(sess, s.getSession());

        s = new SubscriberSessionImpl(address, RecordType.Log, subscriber, "foobar",
                                      "barfoo", 1, 2, 0, sess);
        assertEquals(DigestMethod.SHA256, s.getDigestMethod());
        assertEquals(2, s.getPendingDigestMax());
        assertEquals(1, s.getPendingDigestTimeoutSeconds());
        assertEquals(RecordType.Log, s.getRecordType());
        assertEquals(Role.Subscriber, s.getRole());
        assertEquals(subscriber, s.getSubscriber());
        assertEquals("barfoo", s.getXmlEncoding());
        assertNotNull(s.getListener());
        assertFalse(errored.getBoolean(s));
        assertEquals(0, s.getChannelNum());
        assertEquals(sess, s.getSession());

        s = new SubscriberSessionImpl(address, RecordType.Log, subscriber, "   foobar   ",
                                      "   barfoo   ", 1, 2, 0, sess);
        assertEquals(DigestMethod.SHA256, s.getDigestMethod());
        assertEquals(2, s.getPendingDigestMax());
        assertEquals(1, s.getPendingDigestTimeoutSeconds());
        assertEquals(RecordType.Log, s.getRecordType());
        assertEquals(Role.Subscriber, s.getRole());
        assertEquals(subscriber, s.getSubscriber());
        assertEquals("barfoo", s.getXmlEncoding());
        assertNotNull(s.getListener());
        assertFalse(errored.getBoolean(s));
        assertEquals(0, s.getChannelNum());
        assertEquals(sess, s.getSession());

    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorThrowsExceptionForBadRecordType(final Subscriber subscriber,
			final org.beepcore.beep.core.Session sess, final InetAddress address) {
        new SubscriberSessionImpl(address, RecordType.Unset, subscriber, "foobar", "barfoo", 1, 2, 0, sess);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorThrowsExceptionForEmptyEncoding(final InetAddress address, final Subscriber subscriber,
			final org.beepcore.beep.core.Session sess) {
        new SubscriberSessionImpl(address, RecordType.Audit, subscriber, "foobar", "   ", 1, 2, 0, sess);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorThrowsExceptionForZeroLengthEncoding(final InetAddress address, final Subscriber subscriber,
			final org.beepcore.beep.core.Session sess) {
        new SubscriberSessionImpl(address, RecordType.Audit, subscriber, "foobar", "", 1, 2, 0, sess);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorThrowsExceptionForNullEncoding(final Subscriber subscriber,
			final org.beepcore.beep.core.Session sess, final InetAddress address) {
        new SubscriberSessionImpl(address, RecordType.Audit, subscriber, "foobar", null, 1, 2, 0, sess);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorThrowsExceptionForEmptyDigest(final Subscriber subscriber,
			final org.beepcore.beep.core.Session sess, final InetAddress address) {
        new SubscriberSessionImpl(address, RecordType.Audit, subscriber, "    ", "enc", 1, 2, 0, sess);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorThrowsExceptionForZeroLengthDigest(final Subscriber subscriber,
			final org.beepcore.beep.core.Session sess, final InetAddress address) {
        new SubscriberSessionImpl(address, RecordType.Audit, subscriber, "", "enc", 1, 2, 0, sess);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorThrowsExceptionForNullDigest(final Subscriber subscriber,
			final org.beepcore.beep.core.Session sess, final InetAddress address) {
        new SubscriberSessionImpl(address, RecordType.Audit, subscriber, null, "enc", 1, 2, 0, sess);
    }

	@Test(expected = IllegalArgumentException.class)
	public void testConstructorThrowsExceptionForBadDigest(final Subscriber subscriber,
			final org.beepcore.beep.core.Session sess) {
		new SubscriberSessionImpl(RecordType.Audit, subscriber, "notADigest", "enc", 1, 2, 0, sess);
	}

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorThrowsExceptionZeroDigestMax(final Subscriber subscriber,
			final org.beepcore.beep.core.Session sess, final InetAddress address) {
        new SubscriberSessionImpl(address, RecordType.Audit, subscriber, "digest", "enc", 1, 0, 0, sess);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorThrowsExceptionNegativeDigestMax(final Subscriber subscriber,
			final org.beepcore.beep.core.Session sess, final InetAddress address) {
        new SubscriberSessionImpl(address, RecordType.Audit, subscriber, "digest", "enc", 1, -1, 0, sess);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorThrowsExceptionZeroDigestTimeout(final Subscriber subscriber,
			final org.beepcore.beep.core.Session sess, final InetAddress address) {
        new SubscriberSessionImpl(address, RecordType.Audit, subscriber, "digest", "enc", 0, 1, 0, sess);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorThrowsExceptionNegativeDigestTimeout(final Subscriber subscriber,
			final org.beepcore.beep.core.Session sess, final InetAddress address) {
        new SubscriberSessionImpl(address, RecordType.Audit, subscriber, "digest", "enc", -1, 1, 0, sess);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorThrowsExceptionForNullSubscriber(final org.beepcore.beep.core.Session sess, final InetAddress address) {
        new SubscriberSessionImpl(address, RecordType.Audit, null, "digest", "enc", 1, 1, 0, sess);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorThrowsExceptionForNullRecordType(final Subscriber subscriber,
			final org.beepcore.beep.core.Session sess, final InetAddress address) {
        new SubscriberSessionImpl(address, null, subscriber, "digest", "enc", 1, 1, 0, sess);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorThrowsExceptionForNullSession(final Subscriber subscriber, final InetAddress address) {
        new SubscriberSessionImpl(address, RecordType.Audit, subscriber, "digest", "enc", 1, 1, 0, null);
    }

    @Test
    public void testSetPendingTimeout(final Subscriber subscriber, final org.beepcore.beep.core.Session sess, final InetAddress address) {
        final SubscriberSessionImpl s =
            new SubscriberSessionImpl(address, RecordType.Audit, subscriber, "foobar",
                                      "barfoo", 1, 2, 0, sess);
        s.setDigestTimeout(5);
        assertEquals(5, s.getPendingDigestTimeoutSeconds());
    }

    @Test (expected = IllegalArgumentException.class)
    public void testConstructorThrowsExceptionWhenMissingAddress(final Subscriber subscriber, final InetAddress address, final Session sess) {
        new SubscriberSessionImpl(null, RecordType.Unset, subscriber, "foobar", "barfoo", 1, 1, 2, sess);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testSetPendingTimeoutThrowsExceptionForNegative(final Subscriber subscriber,
			final org.beepcore.beep.core.Session sess, final InetAddress address) {
        final SubscriberSessionImpl s =
            new SubscriberSessionImpl(address, RecordType.Audit, subscriber, "foobar",
                                      "barfoo", 1, 2, 0, sess);
        s.setDigestTimeout(-1);
    }
    @Test (expected = IllegalArgumentException.class)
    public void testSetPendingTimeoutThrowsExceptionForZero(final Subscriber subscriber,
			final org.beepcore.beep.core.Session sess, final InetAddress address) {
        final SubscriberSessionImpl s =
            new SubscriberSessionImpl(address, RecordType.Audit, subscriber, "foobar",
                                      "barfoo", 1, 2, 0, sess);
        s.setDigestTimeout(0);
    }

    @Test
    public void testSetPendingDigestMax(final Subscriber subscriber, final org.beepcore.beep.core.Session sess, final InetAddress address) {
        final SubscriberSessionImpl s =
            new SubscriberSessionImpl(address, RecordType.Audit, subscriber, "foobar",
                                      "barfoo", 1, 2, 0, sess);
        s.setPendingDigestMax(5);
        assertEquals(5, s.getPendingDigestMax());
    }

    @Test (expected = IllegalArgumentException.class)
    public void testSetPendingMaxThrowsExceptionForNegative(final Subscriber subscriber,
			final org.beepcore.beep.core.Session sess, final InetAddress address) {
        final SubscriberSessionImpl s =
            new SubscriberSessionImpl(address, RecordType.Audit, subscriber, "foobar",
                                      "barfoo", 1, 2, 0, sess);
        s.setPendingDigestMax(-1);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testSetPendingMaxThrowsExceptionForZero(final Subscriber subscriber,
			final org.beepcore.beep.core.Session sess, final InetAddress address) {
        final SubscriberSessionImpl s =
            new SubscriberSessionImpl(address, RecordType.Audit, subscriber, "foobar",
                                      "barfoo", 1, 2, 0, sess);
        s.setPendingDigestMax(0);
    }

    @Test
    public void testSetErroredWorks(final Subscriber subscriber, final org.beepcore.beep.core.Session sess, final InetAddress address)
			throws IllegalArgumentException, IllegalAccessException {
        final SubscriberSessionImpl s =
            new SubscriberSessionImpl(address, RecordType.Audit, subscriber, "foobar",
                                      "barfoo", 1, 2, 0, sess);
        s.setErrored();
        assertTrue(errored.getBoolean(s));

    }

    @Test
    public void testAddAllDigestsWorks(final Subscriber subscriber, final org.beepcore.beep.core.Session sess, final InetAddress address)
			throws IllegalArgumentException, IllegalAccessException {
		final SubscriberSessionImpl s =
	        new SubscriberSessionImpl(address, RecordType.Audit, subscriber, "foobar",
	                                  "barfoo", 1, 1, 0, sess);
		final Map<String, String> mapToAdd = new HashMap<String, String>();
		mapToAdd.put("key1", "value1");
		mapToAdd.put("key2", "value2");
		s.addAllDigests(mapToAdd);
		final Map<String, String> map = getDigestMap(s);
		assertTrue(map.containsKey("key1"));
		assertEquals("value1", map.get("key1"));
		assertTrue(map.containsKey("key2"));
		assertEquals("value2", map.get("key2"));
    }

    @Test
    public void testAddDigestsWorks(final Subscriber subscriber, final org.beepcore.beep.core.Session sess, final InetAddress address)
			throws IllegalArgumentException, IllegalAccessException {
		final SubscriberSessionImpl s =
	        new SubscriberSessionImpl(address, RecordType.Audit, subscriber, "foobar",
	                                  "barfoo", 1, 1, 0, sess);
		s.addDigest("serialId", "digest");
		final Map<String, String> map = getDigestMap(s);
		assertTrue(map.containsKey("serialId"));
		assertEquals("digest", map.get("serialId"));
    }

    @Test
    public void testIsOkTrueWorks(final Subscriber subscriber, final org.beepcore.beep.core.Session sess, final InetAddress address)
			throws IllegalArgumentException, IllegalAccessException {
		final SubscriberSessionImpl s =
	        new SubscriberSessionImpl(address, RecordType.Audit, subscriber, "foobar",
	                                  "barfoo", 1, 2, 0, sess);

		new NonStrictExpectations() {
			{
				sess.getState(); result = Session.SESSION_STATE_ACTIVE;
			}
		};

	    assertTrue(s.isOk());
		assertEquals(org.beepcore.beep.core.Session.SESSION_STATE_ACTIVE, sess.getState());
		assertFalse(errored.getBoolean(s));
    }

    @Test
    public void testIsOkFalseWhenErrored(final Subscriber subscriber, final org.beepcore.beep.core.Session sess, final InetAddress address)
			throws IllegalArgumentException, IllegalAccessException {
		final SubscriberSessionImpl s =
	        new SubscriberSessionImpl(address, RecordType.Audit, subscriber, "foobar",
	                                  "barfoo", 1, 2, 0, sess);

		new NonStrictExpectations() {
			{
				sess.getState(); result = Session.SESSION_STATE_ACTIVE;
			}
		};

		s.setErrored();
		assertFalse(s.isOk());
		assertEquals(org.beepcore.beep.core.Session.SESSION_STATE_ACTIVE, sess.getState());
		assertTrue(errored.getBoolean(s));
    }

    @Test
	public void testIsOkFalseWhenInactive(final Subscriber subscriber, final org.beepcore.beep.core.Session sess, final InetAddress address)
			throws IllegalArgumentException, IllegalAccessException {
		final SubscriberSessionImpl s =
	        new SubscriberSessionImpl(address, RecordType.Audit, subscriber, "foobar",
	                                  "barfoo", 1, 2, 0, sess);

		assertFalse(s.isOk());
		assertTrue(org.beepcore.beep.core.Session.SESSION_STATE_ACTIVE != sess.getState());
		assertFalse(errored.getBoolean(s));
    }

	@Test
	public void testRunWorks(final Subscriber subscriber, final org.beepcore.beep.core.Session sess,
			final Channel channel, final DigestListener listener)
			throws InterruptedException, BEEPException {

		final SubscriberSessionImpl s =
	        new SubscriberSessionImpl(RecordType.Audit, subscriber, DigestMethod.SHA256,
	                                  "barfoo", 1, 2, 0, sess);

		final Map<String, String> digestMap = new HashMap<String, String>();
		digestMap.put("serial", "digest");

		new NonStrictExpectations(s) {
	        {
				s.isOk(); result = true;
				sess.startChannel(anyString, false, anyString); result = channel;
	        }
	    };

	    final Thread digestThread = new Thread(s, "digestThreadWorks");
	    digestThread.start();

	    s.addAllDigests(digestMap);
	    digestThread.join(1000);
	    assertTrue(digestThread.isAlive());

	    Thread.sleep(1000);

		new VerificationsInOrder() {
		    {
				sess.startChannel(anyString, false, anyString);
				channel.sendMSG((OutputDataStream)any, (DigestListener)any);
		    }
		};
	}

	@Test
	public void testRunStopsWhenNotOk(final Subscriber subscriber, final org.beepcore.beep.core.Session sess)
			throws InterruptedException {

		final SubscriberSessionImpl s =
	        new SubscriberSessionImpl(RecordType.Audit, subscriber, DigestMethod.SHA256,
	                                  "barfoo", 1, 2, 0, sess);

		new NonStrictExpectations(s) {
	        {
				s.isOk(); result = false;
	        }
	    };

	    final Thread digestThread = new Thread(s, "digestThread");
	    digestThread.start();

	    digestThread.join(1000);

	    assertFalse(digestThread.isAlive());
	}

	@Test
	public void testRunSetsErrorOnException(final Subscriber subscriber, final org.beepcore.beep.core.Session sess)
			throws InterruptedException, BEEPException, IllegalArgumentException, IllegalAccessException {

		final SubscriberSessionImpl s =
	        new SubscriberSessionImpl(RecordType.Audit, subscriber, DigestMethod.SHA256,
	                                  "barfoo", 1, 2, 0, sess);

		new NonStrictExpectations(s) {
	        {
				s.isOk(); result = true;
				sess.startChannel(anyString, false, anyString); result = new BEEPException("");
	        }
	    };

	    final Thread digestThread = new Thread(s, "digestThread");
	    digestThread.start();
	    digestThread.join(1000);

	    assertTrue(errored.getBoolean(s));
	}

	@Test
	public void testGetDigestTypeWorks(final Subscriber subscriber, final org.beepcore.beep.core.Session sess)
			throws SecurityException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {

		final SubscriberSessionImpl s =
	        new SubscriberSessionImpl(RecordType.Audit, subscriber, DigestMethod.SHA256,
	                                  "barfoo", 1, 2, 0, sess);

		final Method digestTypeMethod = SubscriberSessionImpl.class.getDeclaredMethod("getDigestType", String.class);
		digestTypeMethod.setAccessible(true);
		final String sha256Type = (String) digestTypeMethod.invoke(s, DigestMethod.SHA256);
		assertEquals("SHA-256", sha256Type);
		final String sha512Type = (String) digestTypeMethod.invoke(s, DigestMethod.SHA512);
		assertEquals("SHA-512", sha512Type);
		final String sha384Type = (String) digestTypeMethod.invoke(s, "http://www.w3.org/2001/04/xmldsig-more#sha384");
		assertEquals("SHA-384", sha384Type);
		final String invalid = (String) digestTypeMethod.invoke(s, "invalid");
		assertEquals("", invalid);
	}

}
