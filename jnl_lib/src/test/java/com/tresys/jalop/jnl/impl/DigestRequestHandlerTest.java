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

package com.tresys.jalop.jnl.impl;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.soap.MimeHeaders;

import mockit.*;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.beepcore.beep.core.BEEPException;
import org.beepcore.beep.core.Channel;
import org.beepcore.beep.core.InputDataStream;
import org.beepcore.beep.core.InputDataStreamAdapter;
import org.beepcore.beep.core.MessageMSG;
import org.beepcore.beep.core.OutputDataStream;
import org.beepcore.beep.core.Session;
import org.junit.Before;
import org.junit.Test;

import com.tresys.jalop.jnl.DigestPair;
import com.tresys.jalop.jnl.DigestStatus;
import com.tresys.jalop.jnl.Publisher;
import com.tresys.jalop.jnl.RecordType;
import com.tresys.jalop.jnl.exceptions.JNLException;
import com.tresys.jalop.jnl.impl.messages.DigestMessage;
import com.tresys.jalop.jnl.impl.messages.SyncMessage;
import com.tresys.jalop.jnl.impl.messages.Utils;
import com.tresys.jalop.jnl.impl.publisher.PublisherSessionImpl;

public class DigestRequestHandlerTest {

	// Needed to mock static functions in the Utils class.
    @Mocked
    private Utils utils;

	@Before
	public void setUp() {
		// Disable logging so the build doesn't get spammed.
		Logger.getRootLogger().setLevel(Level.OFF);
	}

	@Test
	public void testDigestRequestHandlerWorks(@Mocked final ContextImpl contextImpl, @Mocked final PublisherSessionImpl sess) {
		final DigestRequestHandler drh = new DigestRequestHandler(RecordType.Audit, contextImpl, sess);
		assertEquals(contextImpl, drh.contextImpl);
		assertEquals(RecordType.Audit, drh.recordType);
		assertEquals(sess, drh.sess);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testReceiveMSGWorks(@Mocked final ContextImpl contextImpl, @Mocked final MessageMSG msg,
			@Mocked final Publisher publisher, @Mocked final DigestMessage digestMessage,
			@Mocked final PublisherSessionImpl publisherSessionImpl, @Mocked final Session sess,
			@Mocked final InputDataStream ids, @Mocked final InputDataStreamAdapter isa, @Mocked final Channel channel,
			@Mocked final OutputDataStream ods)
			throws JNLException, SecurityException, NoSuchMethodException, InstantiationException,
			IllegalAccessException, InvocationTargetException, BEEPException {

		final DigestRequestHandler drh = new DigestRequestHandler(RecordType.Audit, contextImpl, publisherSessionImpl);
		final Constructor<DigestMessage> constructor = DigestMessage.class.getDeclaredConstructor(Map.class, MimeHeaders.class);
		constructor.setAccessible(true);
		final DigestMessage dm = constructor.newInstance(new HashMap<String, String>(), new MimeHeaders());
		final Map<String, String> map = new HashMap<String, String>();
		map.put("nonce", "313233343536");

		new NonStrictExpectations() {
			{
				msg.getDataStream(); result = ids;
                ids.getInputStream(); result = isa;
                isa.getHeaderValue(Utils.HDRS_MESSAGE); result = Utils.MSG_DIGEST;
                Utils.processDigestMessage(isa); result = dm;
				msg.getChannel(); result = channel;
                channel.getSession(); result = sess;
                dm.getMap(); result = map;
                publisherSessionImpl.fetchAndRemoveDigest(anyString); result = "123456".getBytes();
                contextImpl.getPublisher(); result = publisher;
                Utils.createDigestResponse((Map<String, DigestStatus>) any); result = ods;
			}
		};

		drh.receiveMSG(msg);

		new VerificationsInOrder() {
			{
				publisher.notifyPeerDigest(publisherSessionImpl, (Map<String, DigestPair>) any);
				msg.sendRPY(ods);
			}
		};
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testReceiveMSGSetsInvalid(@Mocked final ContextImpl contextImpl, @Mocked final MessageMSG msg,
			@Mocked final Publisher publisher, @Mocked final DigestMessage digestMessage,
			@Mocked final PublisherSessionImpl publisherSessionImpl, @Mocked final Session sess,
			@Mocked final InputDataStream ids, @Mocked final InputDataStreamAdapter isa, @Mocked final Channel channel)
			throws JNLException, SecurityException, NoSuchMethodException, InstantiationException,
			IllegalAccessException, InvocationTargetException, BEEPException {

		final DigestRequestHandler drh = new DigestRequestHandler(RecordType.Audit, contextImpl, publisherSessionImpl);
		final Constructor<DigestMessage> constructor = DigestMessage.class.getDeclaredConstructor(Map.class, MimeHeaders.class);
		constructor.setAccessible(true);
		final DigestMessage dm = constructor.newInstance(new HashMap<String, String>(), new MimeHeaders());
		final Map<String, String> map = new HashMap<String, String>();
		map.put("nonce1", "9876543210");

		new NonStrictExpectations() {
			{
				msg.getDataStream(); result = ids;
                ids.getInputStream(); result = isa;
                isa.getHeaderValue(Utils.HDRS_MESSAGE); result = Utils.MSG_DIGEST;
                Utils.processDigestMessage(isa); result = dm;
				msg.getChannel(); result = channel;
                channel.getSession(); result = sess;
                dm.getMap(); result = map;
                publisherSessionImpl.fetchAndRemoveDigest(anyString); result = "123456".getBytes();
                contextImpl.getPublisher(); result = publisher;
			}
		};

		drh.receiveMSG(msg);

		new VerificationsInOrder() {
			{
				new DigestPairImpl(anyString, (byte[])any, (byte[])any, DigestStatus.Invalid);
				publisher.notifyPeerDigest(publisherSessionImpl, (Map<String, DigestPair>) any);
			}
		};
	}

	@Test
	public void testReceiveMSGWorksForSync(@Mocked final ContextImpl contextImpl, @Mocked final MessageMSG msg,
			@Mocked final Publisher publisher, @Mocked final PublisherSessionImpl publisherSessionImpl,
			@Mocked final InputDataStream ids, @Mocked final InputDataStreamAdapter isa)
			throws JNLException, SecurityException, NoSuchMethodException, InstantiationException,
			IllegalAccessException, InvocationTargetException, BEEPException {

		final DigestRequestHandler drh = new DigestRequestHandler(RecordType.Audit, contextImpl, publisherSessionImpl);
		final Constructor<SyncMessage> constructor = SyncMessage.class.getDeclaredConstructor(String.class, MimeHeaders.class);
		constructor.setAccessible(true);
		final SyncMessage sm = constructor.newInstance("nonce", new MimeHeaders());

		new NonStrictExpectations() {
			{
				msg.getDataStream(); result = ids;
                ids.getInputStream(); result = isa;
                isa.getHeaderValue(Utils.HDRS_MESSAGE); result = Utils.MSG_SYNC;
                Utils.processSyncMessage(isa); result = sm;
                contextImpl.getPublisher(); result = publisher;
			}
		};

		drh.receiveMSG(msg);

		new VerificationsInOrder() {
			{
				publisher.sync(publisherSessionImpl, anyString, (MimeHeaders) any);
				msg.sendNUL();
			}
		};
	}

}
