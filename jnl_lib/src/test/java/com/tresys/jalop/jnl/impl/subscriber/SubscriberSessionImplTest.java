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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;

import org.junit.BeforeClass;
import org.junit.Test;

import com.tresys.jalop.jnl.RecordType;
import com.tresys.jalop.jnl.Role;
import com.tresys.jalop.jnl.Subscriber;

public class SubscriberSessionImplTest {
    private static Field errored;
    @BeforeClass
    public static void setupBeforeClass() throws SecurityException, NoSuchFieldException {
        errored = SubscriberSessionImpl.class.getDeclaredField("errored");
        errored.setAccessible(true);
    }

    @Test
    public void testSubscriberImplConstructor(Subscriber subscriber, final org.beepcore.beep.core.Session sess)
			throws IllegalArgumentException, IllegalAccessException {
        SubscriberSessionImpl s =
            new SubscriberSessionImpl(RecordType.Audit, subscriber, "foobar",
                                      "barfoo", 1, 2, 0, sess);
        assertEquals("foobar", s.getDigestMethod());
        assertEquals(2, s.getPendingDigestMax());
        assertEquals(1, s.getPendingDigestTimeoutSeconds());
        assertEquals(RecordType.Audit, s.getRecordType());
        assertEquals(Role.Subscriber, s.getRole());
        assertEquals(subscriber, s.getSubscriber());
        assertEquals("barfoo", s.getXmlEncoding());
        assertFalse(errored.getBoolean(s));
        // TODO: needs to get switched to assertNotNull(),
        // Adding the assert now so the unit tests get updated later.
        assertNull(s.getListener());
        assertEquals(0, s.getChannelNum());
        assertEquals(sess, s.getSession());
        s = new SubscriberSessionImpl(RecordType.Journal, subscriber, "foobar",
                                      "barfoo", 1, 2, 0, sess);
        assertEquals("foobar", s.getDigestMethod());
        assertEquals(2, s.getPendingDigestMax());
        assertEquals(1, s.getPendingDigestTimeoutSeconds());
        assertEquals(RecordType.Journal, s.getRecordType());
        assertEquals(Role.Subscriber, s.getRole());
        assertEquals(subscriber, s.getSubscriber());
        assertEquals("barfoo", s.getXmlEncoding());
        // TODO: needs to get switched to assertNotNull(),
        // Adding the assert now so the unit tests get updated later.
        assertNull(s.getListener());
        assertFalse(errored.getBoolean(s));
        assertEquals(0, s.getChannelNum());
        assertEquals(sess, s.getSession());

        s = new SubscriberSessionImpl(RecordType.Log, subscriber, "foobar",
                                      "barfoo", 1, 2, 0, sess);
        assertEquals("foobar", s.getDigestMethod());
        assertEquals(2, s.getPendingDigestMax());
        assertEquals(1, s.getPendingDigestTimeoutSeconds());
        assertEquals(RecordType.Log, s.getRecordType());
        assertEquals(Role.Subscriber, s.getRole());
        assertEquals(subscriber, s.getSubscriber());
        assertEquals("barfoo", s.getXmlEncoding());
        // TODO: needs to get switched to assertNotNull(),
        // Adding the assert now so the unit tests get updated later.
        assertNull(s.getListener());
        assertFalse(errored.getBoolean(s));
        assertEquals(0, s.getChannelNum());
        assertEquals(sess, s.getSession());

        s = new SubscriberSessionImpl(RecordType.Log, subscriber, "   foobar   ",
                                      "   barfoo   ", 1, 2, 0, sess);
        assertEquals("foobar", s.getDigestMethod());
        assertEquals(2, s.getPendingDigestMax());
        assertEquals(1, s.getPendingDigestTimeoutSeconds());
        assertEquals(RecordType.Log, s.getRecordType());
        assertEquals(Role.Subscriber, s.getRole());
        assertEquals(subscriber, s.getSubscriber());
        assertEquals("barfoo", s.getXmlEncoding());
        // TODO: needs to get switched to assertNotNull(),
        // Adding the assert now so the unit tests get updated later.
        assertNull(s.getListener());
        assertFalse(errored.getBoolean(s));
        assertEquals(0, s.getChannelNum());
        assertEquals(sess, s.getSession());

    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorThrowsExceptionForBadRecordType(Subscriber subscriber,
			final org.beepcore.beep.core.Session sess) {
        new SubscriberSessionImpl(RecordType.Unset, subscriber, "foobar", "barfoo", 1, 2, 0, sess);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorThrowsExceptionForEmptyEncoding(Subscriber subscriber,
			final org.beepcore.beep.core.Session sess) {
        new SubscriberSessionImpl(RecordType.Audit, subscriber, "foobar", "   ", 1, 2, 0, sess);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorThrowsExceptionForZeroLengthEncoding(Subscriber subscriber,
			final org.beepcore.beep.core.Session sess) {
        new SubscriberSessionImpl(RecordType.Audit, subscriber, "foobar", "", 1, 2, 0, sess);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorThrowsExceptionForNullEncoding(Subscriber subscriber,
			final org.beepcore.beep.core.Session sess) {
        new SubscriberSessionImpl(RecordType.Audit, subscriber, "foobar", null, 1, 2, 0, sess);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorThrowsExceptionForEmptyDigest(Subscriber subscriber,
			final org.beepcore.beep.core.Session sess) {
        new SubscriberSessionImpl(RecordType.Audit, subscriber, "    ", "enc", 1, 2, 0, sess);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorThrowsExceptionForZeroLengthDigest(Subscriber subscriber,
			final org.beepcore.beep.core.Session sess) {
        new SubscriberSessionImpl(RecordType.Audit, subscriber, "", "enc", 1, 2, 0, sess);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorThrowsExceptionForNullDigest(Subscriber subscriber,
			final org.beepcore.beep.core.Session sess) {
        new SubscriberSessionImpl(RecordType.Audit, subscriber, null, "enc", 1, 2, 0, sess);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorThrowsExceptionZeroDigestMax(Subscriber subscriber,
			final org.beepcore.beep.core.Session sess) {
        new SubscriberSessionImpl(RecordType.Audit, subscriber, "digest", "enc", 1, 0, 0, sess);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorThrowsExceptionNegativeDigestMax(Subscriber subscriber,
			final org.beepcore.beep.core.Session sess) {
        new SubscriberSessionImpl(RecordType.Audit, subscriber, "digest", "enc", 1, -1, 0, sess);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorThrowsExceptionZeroDigestTimeout(Subscriber subscriber,
			final org.beepcore.beep.core.Session sess) {
        new SubscriberSessionImpl(RecordType.Audit, subscriber, "digest", "enc", 0, 1, 0, sess);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorThrowsExceptionNegativeDigestTimeout(Subscriber subscriber,
			final org.beepcore.beep.core.Session sess) {
        new SubscriberSessionImpl(RecordType.Audit, subscriber, "digest", "enc", -1, 1, 0, sess);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorThrowsExceptionForNullSubscriber(final org.beepcore.beep.core.Session sess) {
        new SubscriberSessionImpl(RecordType.Audit, null, "digest", "enc", 1, 1, 0, sess);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorThrowsExceptionForNullRecordType(Subscriber subscriber,
			final org.beepcore.beep.core.Session sess) {
        new SubscriberSessionImpl(null, subscriber, "digest", "enc", 1, 1, 0, sess);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorThrowsExceptionForNullSession(Subscriber subscriber) {
        new SubscriberSessionImpl(RecordType.Audit, subscriber, "digest", "enc", 1, 1, 0, null);
    }

    @Test
    public void testSetPendingTimeout(Subscriber subscriber, final org.beepcore.beep.core.Session sess) {
        SubscriberSessionImpl s =
            new SubscriberSessionImpl(RecordType.Audit, subscriber, "foobar",
                                      "barfoo", 1, 2, 0, sess);
        s.setDigestTimeout(5);
        assertEquals(5, s.getPendingDigestTimeoutSeconds());
    }
    @Test (expected = IllegalArgumentException.class)
    public void testSetPendingTimeoutThrowsExceptionForNegative(Subscriber subscriber,
			final org.beepcore.beep.core.Session sess) {
        SubscriberSessionImpl s =
            new SubscriberSessionImpl(RecordType.Audit, subscriber, "foobar",
                                      "barfoo", 1, 2, 0, sess);
        s.setDigestTimeout(-1);
    }
    @Test (expected = IllegalArgumentException.class)
    public void testSetPendingTimeoutThrowsExceptionForZero(Subscriber subscriber,
			final org.beepcore.beep.core.Session sess) {
        SubscriberSessionImpl s =
            new SubscriberSessionImpl(RecordType.Audit, subscriber, "foobar",
                                      "barfoo", 1, 2, 0, sess);
        s.setDigestTimeout(0);
    }

    @Test
    public void testSetPendingDigestMax(Subscriber subscriber, final org.beepcore.beep.core.Session sess) {
        SubscriberSessionImpl s =
            new SubscriberSessionImpl(RecordType.Audit, subscriber, "foobar",
                                      "barfoo", 1, 2, 0, sess);
        s.setPendingDigestMax(5);
        assertEquals(5, s.getPendingDigestMax());
    }

    @Test (expected = IllegalArgumentException.class)
    public void testSetPendingMaxThrowsExceptionForNegative(Subscriber subscriber,
			final org.beepcore.beep.core.Session sess) {
        SubscriberSessionImpl s =
            new SubscriberSessionImpl(RecordType.Audit, subscriber, "foobar",
                                      "barfoo", 1, 2, 0, sess);
        s.setPendingDigestMax(-1);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testSetPendingMaxThrowsExceptionForZero(Subscriber subscriber,
			final org.beepcore.beep.core.Session sess) {
        SubscriberSessionImpl s =
            new SubscriberSessionImpl(RecordType.Audit, subscriber, "foobar",
                                      "barfoo", 1, 2, 0, sess);
        s.setPendingDigestMax(0);
    }
    @Test
    public void testSetErroredWorks(Subscriber subscriber, final org.beepcore.beep.core.Session sess)
			throws IllegalArgumentException, IllegalAccessException {
        SubscriberSessionImpl s =
            new SubscriberSessionImpl(RecordType.Audit, subscriber, "foobar",
                                      "barfoo", 1, 2, 0, sess);
        s.setErrored();
        assertTrue(errored.getBoolean(s));

    }
}