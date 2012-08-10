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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.crypto.dsig.DigestMethod;

import mockit.Mocked;
import mockit.NonStrictExpectations;
import mockit.VerificationsInOrder;

import org.beepcore.beep.core.BEEPException;
import org.beepcore.beep.core.Channel;
import org.beepcore.beep.core.OutputDataStream;
import org.beepcore.beep.core.ReplyListener;
import org.beepcore.beep.transport.tcp.TCPSession;
import org.beepcore.beep.transport.tcp.TCPSessionCreator;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.Lists;
import com.tresys.jalop.jnl.ConnectionHandler;
import com.tresys.jalop.jnl.Publisher;
import com.tresys.jalop.jnl.RecordType;
import com.tresys.jalop.jnl.Role;
import com.tresys.jalop.jnl.Session;
import com.tresys.jalop.jnl.Subscriber;
import com.tresys.jalop.jnl.exceptions.ConnectionException;
import com.tresys.jalop.jnl.exceptions.JNLException;
import com.tresys.jalop.jnl.impl.ContextImpl.ConnectionState;
import com.tresys.jalop.jnl.impl.messages.Utils;
import com.tresys.jalop.jnl.impl.publisher.PublisherSessionImpl;
import com.tresys.jalop.jnl.impl.subscriber.SubscriberSessionImpl;

public class ContextImplTest {

	// Needed to mock static functions in the Utils class.
    @Mocked
    private Utils utils;

    private LinkedList<String> encodings;
    private LinkedList<String> digests;
    private static Field       tlsField;
    private static Field       jalSessionsField;
    private static Field       connectionStateField;
    private static Field       subscriberMapField;
    private static Field       publisherMapField;

    @BeforeClass
    public static void setUpBeforeClass() throws SecurityException, NoSuchFieldException {
        tlsField = ContextImpl.class.getDeclaredField("tlsRequired");
        tlsField.setAccessible(true);

        jalSessionsField = ContextImpl.class.getDeclaredField("jalSessions");
        jalSessionsField.setAccessible(true);

        connectionStateField = ContextImpl.class.getDeclaredField("connectionState");
        connectionStateField.setAccessible(true);

        subscriberMapField = ContextImpl.class.getDeclaredField("subscriberMap");
        subscriberMapField.setAccessible(true);

        publisherMapField = ContextImpl.class.getDeclaredField("publisherMap");
        publisherMapField.setAccessible(true);
    }

    @Before
    public void setUp() throws Exception {
        encodings = new LinkedList<String>();
        encodings.push("enc_foo");
        encodings.push("enc_bar");

        digests = new LinkedList<String>();
        digests.push("dgst_foo");
        digests.push("dgst_bar");
    }

    @SuppressWarnings("unchecked")
    private static List<Session> getSessions(final ContextImpl c) throws IllegalArgumentException, IllegalAccessException {
        return (List<Session>) jalSessionsField.get(c);
    }

    @SuppressWarnings("unchecked")
    private static Map<org.beepcore.beep.core.Session, Map<RecordType, SubscriberSessionImpl>> getSubscriberMap(final ContextImpl c)
            throws IllegalArgumentException, IllegalAccessException {
        return (Map<org.beepcore.beep.core.Session, Map<RecordType, SubscriberSessionImpl>>) subscriberMapField.get(c);
    }

    @SuppressWarnings("unchecked")
    private static Map<org.beepcore.beep.core.Session, Map<RecordType, PublisherSessionImpl>> getPublisherMap(final ContextImpl c)
            throws IllegalArgumentException, IllegalAccessException {
        return (Map<org.beepcore.beep.core.Session, Map<RecordType, PublisherSessionImpl>>) publisherMapField.get(c);
    }

    @Test
    public final void testContextImplConstructorWithoutPublisher(final Subscriber subscriber,
            final ConnectionHandler connectionHandler) throws IllegalArgumentException, IllegalAccessException {
        final ContextImpl c = new ContextImpl(null, subscriber, connectionHandler, 100, 150, false, "agent", digests, encodings);
        assertEquals(null, c.getPublisher());
        assertEquals(subscriber, c.getSubscriber());
        assertEquals(connectionHandler, c.getConnectionHandler());
        assertEquals(100, c.getDefaultDigestTimeout());
        assertEquals(150, c.getDefaultPendingDigestMax());
        assertFalse(tlsField.getBoolean(c));
        assertEquals("agent", c.getAgent());

        assertArrayEquals(encodings.toArray(new String[0]),
                          Lists.newArrayList(c.getAllowedXmlEncodings()).toArray(new String[0]));
        assertArrayEquals(digests.toArray(new String[0]),
                          Lists.newArrayList(c.getAllowedMessageDigests()).toArray(new String[0]));
        assertEquals(ContextImpl.ConnectionState.DISCONNECTED, connectionStateField.get(c));
        assertNotNull(getSessions(c));
        assertTrue(getSessions(c).isEmpty());

    }

    @Test
    public final void testContextImplConstructorWorksWithoutSubscriber(final Publisher publisher,
            final ConnectionHandler connectionHandler) throws IllegalArgumentException, IllegalAccessException {
        final ContextImpl c = new ContextImpl(publisher, null, connectionHandler, 100, 150, false, null, digests, encodings);
        assertEquals(publisher, c.getPublisher());
        assertEquals(null, c.getSubscriber());
        assertEquals(connectionHandler, c.getConnectionHandler());
        assertEquals(100, c.getDefaultDigestTimeout());
        assertEquals(150, c.getDefaultPendingDigestMax());
        assertFalse(tlsField.getBoolean(c));

        assertArrayEquals(encodings.toArray(new String[0]),
                          Lists.newArrayList(c.getAllowedXmlEncodings()).toArray(new String[0]));
        assertArrayEquals(digests.toArray(new String[0]),
                          Lists.newArrayList(c.getAllowedMessageDigests()).toArray(new String[0]));
        assertEquals(ContextImpl.ConnectionState.DISCONNECTED, connectionStateField.get(c));
        assertNotNull(getSessions(c));
        assertTrue(getSessions(c).isEmpty());

    }

    @Test
    public final void testContextImplConstructorWorksWithoutConnectionHandler(final Publisher publisher, final Subscriber subscriber)
            throws IllegalArgumentException, IllegalAccessException {
        final ContextImpl c = new ContextImpl(publisher, subscriber, null, 100, 150, false, null, digests, encodings);
        assertEquals(publisher, c.getPublisher());
        assertEquals(subscriber, c.getSubscriber());
        assertEquals(null, c.getConnectionHandler());
        assertEquals(100, c.getDefaultDigestTimeout());
        assertEquals(150, c.getDefaultPendingDigestMax());
        assertFalse(tlsField.getBoolean(c));

        assertArrayEquals(encodings.toArray(new String[0]),
                          Lists.newArrayList(c.getAllowedXmlEncodings()).toArray(new String[0]));
        assertArrayEquals(digests.toArray(new String[0]),
                          Lists.newArrayList(c.getAllowedMessageDigests()).toArray(new String[0]));
        assertEquals(ContextImpl.ConnectionState.DISCONNECTED, connectionStateField.get(c));
        assertNotNull(getSessions(c));
        assertTrue(getSessions(c).isEmpty());

    }

    @Test
    public final void testContextImplConstructorWorksWithTlsRequired(final Publisher publisher, final Subscriber subscriber,
            final ConnectionHandler connectionHandler) throws IllegalArgumentException, IllegalAccessException {
        final ContextImpl c = new ContextImpl(publisher, subscriber, connectionHandler, 100, 150, true, null, digests, encodings);
        assertEquals(publisher, c.getPublisher());
        assertEquals(subscriber, c.getSubscriber());
        assertEquals(connectionHandler, c.getConnectionHandler());
        assertEquals(100, c.getDefaultDigestTimeout());
        assertEquals(150, c.getDefaultPendingDigestMax());
        assertTrue(tlsField.getBoolean(c));

        assertArrayEquals(encodings.toArray(new String[0]),
                          Lists.newArrayList(c.getAllowedXmlEncodings()).toArray(new String[0]));
        assertArrayEquals(digests.toArray(new String[0]),
                          Lists.newArrayList(c.getAllowedMessageDigests()).toArray(new String[0]));
        assertEquals(ContextImpl.ConnectionState.DISCONNECTED, connectionStateField.get(c));
        assertNotNull(getSessions(c));
        assertTrue(getSessions(c).isEmpty());

    }

    @Test
    public final void testContextImplConstructorWorksNullDigests(final Publisher publisher, final Subscriber subscriber,
            final ConnectionHandler connectionHandler) throws IllegalArgumentException, IllegalAccessException {
        final ContextImpl c = new ContextImpl(publisher, subscriber, connectionHandler, 100, 150, false, null, null, encodings);
        assertEquals(publisher, c.getPublisher());
        assertEquals(subscriber, c.getSubscriber());
        assertEquals(connectionHandler, c.getConnectionHandler());
        assertEquals(100, c.getDefaultDigestTimeout());
        assertEquals(150, c.getDefaultPendingDigestMax());
        assertFalse(tlsField.getBoolean(c));

        assertArrayEquals(encodings.toArray(new String[0]),
                          Lists.newArrayList(c.getAllowedXmlEncodings()).toArray(new String[0]));
        assertNotNull(c.getAllowedMessageDigests());
        final List<String> dgsts = Lists.newArrayList(c.getAllowedMessageDigests());
        assertNotNull(dgsts);
        assertEquals(1, dgsts.size());
        assertEquals(DigestMethod.SHA256, dgsts.get(0));
        assertEquals(ContextImpl.ConnectionState.DISCONNECTED, connectionStateField.get(c));
        assertNotNull(getSessions(c));
        assertTrue(getSessions(c).isEmpty());

    }

    @Test
    public final void testContextImplConstructorWorksWithEmptyDigests(final Publisher publisher, final Subscriber subscriber,
            final ConnectionHandler connectionHandler) throws IllegalArgumentException, IllegalAccessException {
        digests.clear();
        final ContextImpl c = new ContextImpl(publisher, subscriber, connectionHandler, 100, 150, false, null, digests, encodings);
        assertEquals(publisher, c.getPublisher());
        assertEquals(subscriber, c.getSubscriber());
        assertEquals(connectionHandler, c.getConnectionHandler());
        assertEquals(100, c.getDefaultDigestTimeout());
        assertEquals(150, c.getDefaultPendingDigestMax());
        assertFalse(tlsField.getBoolean(c));

        assertArrayEquals(encodings.toArray(new String[0]),
                          Lists.newArrayList(c.getAllowedXmlEncodings()).toArray(new String[0]));

        assertNotNull(c.getAllowedMessageDigests());
        final List<String> dgsts = Lists.newArrayList(c.getAllowedMessageDigests());
        assertEquals(1, dgsts.size());
        assertEquals(DigestMethod.SHA256, dgsts.get(0));

        assertEquals(ContextImpl.ConnectionState.DISCONNECTED, connectionStateField.get(c));
        assertNotNull(getSessions(c));
        assertTrue(getSessions(c).isEmpty());

    }

    @Test
    public final void testContextImplConstructorWorksWithNullEncodings(final Publisher publisher, final Subscriber subscriber,
            final ConnectionHandler connectionHandler) throws IllegalArgumentException, IllegalAccessException {
        final ContextImpl c = new ContextImpl(publisher, subscriber, connectionHandler, 100, 150, false, null, digests, null);
        assertEquals(publisher, c.getPublisher());
        assertEquals(subscriber, c.getSubscriber());
        assertEquals(connectionHandler, c.getConnectionHandler());
        assertEquals(100, c.getDefaultDigestTimeout());
        assertEquals(150, c.getDefaultPendingDigestMax());
        assertFalse(tlsField.getBoolean(c));

        assertNotNull(c.getAllowedXmlEncodings());
        final List<String> encs = Lists.newArrayList(c.getAllowedXmlEncodings());
        assertNotNull(encs);
        assertEquals(1, encs.size());
        assertEquals("xml", encs.get(0));
        assertArrayEquals(digests.toArray(new String[0]),
                          Lists.newArrayList(c.getAllowedMessageDigests()).toArray(new String[0]));
        assertEquals(ContextImpl.ConnectionState.DISCONNECTED, connectionStateField.get(c));
        assertNotNull(getSessions(c));
        assertTrue(getSessions(c).isEmpty());

    }

    @Test
    public final void testContextImplConstructorWorksEmptyEncodings(final Publisher publisher, final Subscriber subscriber,
            final ConnectionHandler connectionHandler) throws IllegalArgumentException, IllegalAccessException {
        encodings.clear();
        final ContextImpl c = new ContextImpl(publisher, subscriber, connectionHandler, 100, 150, false, null, digests, encodings);
        assertEquals(publisher, c.getPublisher());
        assertEquals(subscriber, c.getSubscriber());
        assertEquals(connectionHandler, c.getConnectionHandler());
        assertEquals(100, c.getDefaultDigestTimeout());
        assertEquals(150, c.getDefaultPendingDigestMax());
        assertFalse(tlsField.getBoolean(c));

        assertNotNull(c.getAllowedXmlEncodings());
        final List<String> encs = Lists.newArrayList(c.getAllowedXmlEncodings());
        assertEquals(1, encs.size());
        assertEquals("xml", encs.get(0));

        assertArrayEquals(digests.toArray(new String[0]),
                          Lists.newArrayList(c.getAllowedMessageDigests()).toArray(new String[0]));
        assertEquals(ContextImpl.ConnectionState.DISCONNECTED, connectionStateField.get(c));
        assertNotNull(getSessions(c));
        assertTrue(getSessions(c).isEmpty());

    }

    @Test(expected = IllegalArgumentException.class)
    public final void testContextImplConstructorThrowsExceptionForZeroDigestTimeout(final Publisher publisher,
            final Subscriber subscriber, final ConnectionHandler connectionHandler) {
        final ContextImpl c = new ContextImpl(publisher, subscriber, connectionHandler, 0, 150, false, null, digests, encodings);
    }

    @Test(expected = IllegalArgumentException.class)
    public final void testContextImplConstructorThrowsExceptionForNegativeDigestTimeout(final Publisher publisher,
            final Subscriber subscriber, final ConnectionHandler connectionHandler) {
        final ContextImpl c = new ContextImpl(publisher, subscriber, connectionHandler, -1, 150, false, null, digests, encodings);
    }

    @Test(expected = IllegalArgumentException.class)
    public final void testContextImplConstructorThrowsExceptionForZeroDigestMax(final Publisher publisher,
            final Subscriber subscriber, final ConnectionHandler connectionHandler) {
        final ContextImpl c = new ContextImpl(publisher, subscriber, connectionHandler, 100, 0, false, null, digests, encodings);
    }

    @Test(expected = IllegalArgumentException.class)
    public final void testContextImplConstructorThrowsExceptionForNegativeDigestMax(final Publisher publisher,
            final Subscriber subscriber, final ConnectionHandler connectionHandler) {
        final ContextImpl c = new ContextImpl(publisher, subscriber, connectionHandler, 100, -1, false, null, digests, encodings);
    }

    @Test(expected = IllegalArgumentException.class)
    public final void testContextImplConstructorThrowsExceptionWhenSubscribeAndPublisherAreNull(
            final ConnectionHandler connectionHandler) {
        final ContextImpl c = new ContextImpl(null, null, connectionHandler, 100, 10, false, null, digests, encodings);

    }

    @Test
    public final void testAddSessionsWorksForSubscriber(final org.beepcore.beep.core.Session sess,
            final SubscriberSessionImpl subSess, final Subscriber subscriber,
            final ConnectionHandler connectionHandler) throws JNLException, IllegalAccessException {

        final ContextImpl c = new ContextImpl(null, subscriber, connectionHandler, 100, 10, false, null, digests, encodings);
        c.addSession(sess, subSess);
        final Map<org.beepcore.beep.core.Session, Map<RecordType, SubscriberSessionImpl>> map = getSubscriberMap(c);
        assertTrue(map.containsKey(sess));

        final Map<RecordType, SubscriberSessionImpl> subSessionMap = map.get(sess);
        assertTrue(subSessionMap.containsKey(subSess.getRecordType()));
        assertEquals(subSess, subSessionMap.get(subSess.getRecordType()));
    }

    @Test
    public final void testAddSessionsWorksForPublisher(final org.beepcore.beep.core.Session sess,
            final PublisherSessionImpl pubSess, final Publisher publisher,
            final ConnectionHandler connectionHandler) throws JNLException, IllegalAccessException {

        final ContextImpl c = new ContextImpl(publisher, null, connectionHandler, 100, 10, false, null, digests, encodings);
        c.addSession(sess, pubSess);
        final Map<org.beepcore.beep.core.Session, Map<RecordType, PublisherSessionImpl>> map = getPublisherMap(c);
        assertTrue(map.containsKey(sess));

        final Map<RecordType, PublisherSessionImpl> pubSessionMap = map.get(sess);
        assertTrue(pubSessionMap.containsKey(pubSess.getRecordType()));
        assertEquals(pubSess, pubSessionMap.get(pubSess.getRecordType()));
    }

    @Test
    public final void testAddSessionsAddsToExistingMap(final InetAddress address, final org.beepcore.beep.core.Session sess,
            final Subscriber subscriber, final ConnectionHandler connectionHandler)
            throws JNLException, IllegalAccessException {

		final ContextImpl c = new ContextImpl(null, subscriber, connectionHandler, 100, 10, false, null, digests, encodings);
        final SubscriberSessionImpl subSess = new SubscriberSessionImpl(address, RecordType.Log, subscriber, DigestMethod.SHA256, "bar", 1, 1, 1, sess);
        c.addSession(sess, subSess);
        final SubscriberSessionImpl nextSubSess = new SubscriberSessionImpl(address, RecordType.Journal, subscriber, DigestMethod.SHA256, "bar", 1, 1, 1, sess);
        c.addSession(sess, nextSubSess);

        final Map<RecordType, SubscriberSessionImpl> subSessionMap = getSubscriberMap(c).get(sess);
        assertTrue(subSessionMap.containsKey(subSess.getRecordType()));
        assertEquals(subSess, subSessionMap.get(subSess.getRecordType()));
        assertTrue(subSessionMap.containsKey(nextSubSess.getRecordType()));
        assertEquals(nextSubSess, subSessionMap.get(nextSubSess.getRecordType()));
    }

    @Test(expected = IllegalArgumentException.class)
    public final void testAddSessionThrowsExceptionWithUnsetRecordType(final InetAddress address, final org.beepcore.beep.core.Session sess, final Subscriber subscriber,
            final ConnectionHandler connectionHandler) throws JNLException {

    	final ContextImpl c = new ContextImpl(null, subscriber, connectionHandler, 100, 10, false, null, digests, encodings);
        final SubscriberSessionImpl subSess = new SubscriberSessionImpl(address, RecordType.Unset, null, null, null, 0, 0, 1, sess);
        c.addSession(sess, subSess);
    }

    @Test(expected = JNLException.class)
    public final void testAddSessionsFailsWithDuplicateRecordType(final InetAddress address, final org.beepcore.beep.core.Session sess,
            final Subscriber subscriber, final ConnectionHandler connectionHandler)
            throws JNLException, IllegalAccessException {

		final ContextImpl c = new ContextImpl(null, subscriber, connectionHandler, 100, 10, false, null, digests, encodings);
        final SubscriberSessionImpl subSess = new SubscriberSessionImpl(address, RecordType.Log, subscriber, DigestMethod.SHA256, "bar", 1, 1, 1, sess);
        c.addSession(sess, subSess);
        final SubscriberSessionImpl nextSubSess = new SubscriberSessionImpl(address, RecordType.Log, subscriber, DigestMethod.SHA256, "bar", 1, 1, 1, sess);
        c.addSession(sess, nextSubSess);
    }

	@SuppressWarnings({ "unchecked" })
	@Test
	public final void testSubscribeWorks(final Subscriber subscriber,
			final TCPSession session, final Channel channel, final TCPSessionCreator tcpSessionCreator,
			final OutputDataStream ods)
			throws IllegalAccessException, JNLException, BEEPException, UnknownHostException {

		final ContextImpl c = new ContextImpl(null, subscriber, null, 100, 150, false, "agent", digests, encodings);

		new NonStrictExpectations() {
			{
				TCPSessionCreator.initiate((InetAddress) any, anyInt); result = session;
				session.startChannel(anyString); result = channel;
				channel.getState(); result = Channel.STATE_ACTIVE;
				Utils.createInitMessage((Role)any,
						(RecordType)any,
						(List<String>)any,
						(List<String>)any,
						anyString);
					result = ods;
			}
		};

		c.subscribe(InetAddress.getByName("localhost"), 0, RecordType.Log);
		assertEquals(ConnectionState.CONNECTED, connectionStateField.get(c));

		new VerificationsInOrder() {
			{
				channel.sendMSG(ods, (ReplyListener)any);
			}
		};
	}

	@Test(expected = ConnectionException.class)
	public final void testSubscribeThrowsExceptionIfAlreadyConnected(final Subscriber subscriber)
			throws IllegalAccessException, JNLException, BEEPException, UnknownHostException {
		final ContextImpl c = new ContextImpl(null, subscriber, null, 100, 150, false, "agent", digests, encodings);
		connectionStateField.set(c, ConnectionState.CONNECTED);
		c.subscribe(InetAddress.getByName("localhost"), 1234, RecordType.Log);
	}

	@Test(expected = JNLException.class)
	public final void testSubscribeThrowsExceptionWithNullSubscriber(final Publisher publisher)
			throws IllegalAccessException, JNLException, BEEPException, UnknownHostException {
		final ContextImpl c = new ContextImpl(publisher, null, null, 100, 150, false, "agent", digests, encodings);
		c.subscribe(InetAddress.getByName("localhost"), 0, RecordType.Log);
	}

	@Test(expected = IllegalArgumentException.class)
	public final void testSubscribeThrowsExceptionWithNullAddress(final Subscriber subscriber)
			throws IllegalAccessException, JNLException, BEEPException {
		final ContextImpl c = new ContextImpl(null, subscriber, null, 100, 150, false, "agent", digests, encodings);
		c.subscribe(null, 0, RecordType.Log);
	}

	@Test(expected = JNLException.class)
	public final void testSubscribeThrowsExceptionWithUnsetRecordType(final Subscriber subscriber,
			final TCPSession session, final Channel channel, final TCPSessionCreator tcpSessionCreator)
			throws IllegalAccessException, JNLException, BEEPException, UnknownHostException {
		final ContextImpl c = new ContextImpl(null, subscriber, null, 100, 150, false, "agent", digests, encodings);

		new NonStrictExpectations() {
			{
				TCPSessionCreator.initiate((InetAddress) any, anyInt); result = session;
				session.startChannel(anyString); result = channel;
				channel.getState(); result = Channel.STATE_ACTIVE;
			}
		};

		c.subscribe(InetAddress.getByName("localhost"), 0, RecordType.Unset);
	}
}
