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
 *	http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tresys.jalop.jnl.impl;


import static org.junit.Assert.assertEquals;

import java.lang.reflect.Field;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.soap.MimeHeaders;

import mockit.Mocked;
import mockit.NonStrictExpectations;
import mockit.Verifications;
import mockit.VerificationsInOrder;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.beepcore.beep.core.AbortChannelException;
import org.beepcore.beep.core.BEEPException;
import org.beepcore.beep.core.Channel;
import org.beepcore.beep.core.InputDataStream;
import org.beepcore.beep.core.InputDataStreamAdapter;
import org.beepcore.beep.core.Message;
import org.beepcore.beep.core.OutputDataStream;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.tresys.jalop.jnl.DigestStatus;
import com.tresys.jalop.jnl.RecordType;
import com.tresys.jalop.jnl.Subscriber;
import com.tresys.jalop.jnl.exceptions.MissingMimeHeaderException;
import com.tresys.jalop.jnl.exceptions.UnexpectedMimeValueException;
import com.tresys.jalop.jnl.impl.messages.DigestResponse;
import com.tresys.jalop.jnl.impl.messages.Utils;
import com.tresys.jalop.jnl.impl.subscriber.SubscriberSessionImpl;

public class DigestListenerTest {
	// Needed to mock static functions in the Utils class.
	@Mocked
	private Utils utils;
	@Before
	public void setUp() {
		// Disable logging so the build doesn't get spammed.
	Logger.getRootLogger().setLevel(Level.OFF);
	}

	private static Field digestMapField;

	@BeforeClass
	public static void setUpBeforeClass() throws SecurityException, NoSuchFieldException {
		digestMapField = SubscriberSessionImpl.class.getDeclaredField("digestMap");
		digestMapField.setAccessible(true);
	}

	@SuppressWarnings("unchecked")
	private static Map<String, String> getDigestMap(final SubscriberSessionImpl s)
			throws IllegalArgumentException, IllegalAccessException {
		return (Map<String, String>) digestMapField.get(s);
	}

	@Test (expected = AbortChannelException.class)
	public void testDigestListenerReceiveErr(final SubscriberSessionImpl subSess, final Message message)
			throws AbortChannelException {
		final Map<String, String> map = new HashMap<String, String>();
		final DigestListener digestListener = new DigestListener(subSess, map);
		digestListener.receiveERR(message);
	}

	@Test (expected = AbortChannelException.class)
	public void testDigestListenerThrowsExceptionOnReceiveAns(final SubscriberSessionImpl subSess, final Message message)
			throws AbortChannelException {
		final Map<String, String> map = new HashMap<String, String>();
		final DigestListener digestListener = new DigestListener(subSess, map);
		digestListener.receiveANS(message);
	}

	@Test
	public void testDigestListenerDoesNothingOnReceiveNul(final SubscriberSessionImpl subSess, final Message message)
			throws AbortChannelException {
		final Map<String, String> map = new HashMap<String, String>();
		final DigestListener digestListener = new DigestListener(subSess, map);
		digestListener.receiveNUL(message);
	}

	@Test
	public void testDigestListenerReceiveRpy(final SubscriberSessionImpl subSess, final Message message, final Channel channel,
			final InputDataStream ids, final InputDataStreamAdapter isa, final Subscriber subscriber)
			throws MissingMimeHeaderException, UnexpectedMimeValueException, BEEPException {

		final Map<String, DigestStatus> statusMap = new HashMap<String, DigestStatus>();
		statusMap.put("1", DigestStatus.Confirmed);
		final DigestResponse dr = new DigestResponse(statusMap, new MimeHeaders());

		final Map<String, String> digestsSent = new HashMap<String, String>();
		digestsSent.put("1", "digest");

		new NonStrictExpectations() {
			{
				message.getDataStream(); result = ids;
				ids.getInputStream(); result = isa;
				Utils.processDigestResponse(isa); result = dr;
				dr.getMap(); result = statusMap;
				subSess.getSubscriber(); result = subscriber;
				message.getChannel(); result = channel;
				subscriber.notifyDigestResponse(subSess, "1", DigestStatus.Confirmed); result = true;
			}
		};

		final DigestListener digestListener = new DigestListener(subSess, digestsSent);
		digestListener.receiveRPY(message);

		new Verifications() {
			{
				subscriber.notifyDigestResponse(subSess, "1", DigestStatus.Confirmed);
				Utils.createSyncMessage("1");
				channel.sendMSG((OutputDataStream) any, null);
			}
		};
	}

	@Test
	public void testDigestListenerAddsDigestsBackInReceiveRpy(final Message message, final InputDataStream ids, final Channel channel,
			final InputDataStreamAdapter isa, final Subscriber subscriber, final org.beepcore.beep.core.Session sess, final InetAddress address)
			throws IllegalAccessException, MissingMimeHeaderException, UnexpectedMimeValueException,
			BEEPException {

		final Map<String, DigestStatus> statusMap = new HashMap<String, DigestStatus>();
		statusMap.put("1", DigestStatus.Confirmed);
		final DigestResponse dr = new DigestResponse(statusMap, new MimeHeaders());

		final Map<String, String> digestsSent = new HashMap<String, String>();
		digestsSent.put("1", "digest");
		digestsSent.put("2", "anotherDigest");

		final SubscriberSessionImpl subSess =
			new SubscriberSessionImpl(address, RecordType.Audit, subscriber, DigestMethod.SHA256,
				"barfoo", 1, 2, 0, sess);

		new NonStrictExpectations() {
			{
				message.getDataStream(); result = ids;
				ids.getInputStream(); result = isa;
				Utils.processDigestResponse(isa); result = dr;
				dr.getMap(); result = statusMap;
				message.getChannel(); result = channel;
				subscriber.notifyDigestResponse(subSess, "1", DigestStatus.Confirmed); result = true;
			}
		};

		final DigestListener digestListener = new DigestListener(subSess, digestsSent);
		digestListener.receiveRPY(message);

		new VerificationsInOrder() {
			{
				subscriber.notifyDigestResponse(subSess, "1", DigestStatus.Confirmed);
				Utils.createSyncMessage("1");
				channel.sendMSG((OutputDataStream) any, null);
				subSess.addAllDigests(digestsSent);
			}
		};

		assertEquals(digestsSent, getDigestMap(subSess));
	}
}