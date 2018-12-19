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

import java.lang.reflect.Field;

import mockit.*;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.beepcore.beep.core.BEEPError;
import org.beepcore.beep.core.BEEPException;
import org.beepcore.beep.core.Channel;
import org.beepcore.beep.core.Message;
import org.beepcore.beep.core.MessageMSG;
import org.beepcore.beep.core.OutputDataStream;
import org.beepcore.beep.core.RequestHandler;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.tresys.jalop.jnl.RecordType;
import com.tresys.jalop.jnl.impl.publisher.PublisherSessionImpl;

public class DigestInitHandlerTest {

	@Before
	public void setUp() {
		// Disable logging so the build doesn't get spammed.
		Logger.getRootLogger().setLevel(Level.OFF);
	}

	private static Field sessField;
	private static Field contextField;

	@BeforeClass
	public static void setUpBeforeClass() throws SecurityException, NoSuchFieldException {
		sessField = DigestInitHandler.class.getDeclaredField("sess");
		sessField.setAccessible(true);

		contextField = DigestInitHandler.class.getDeclaredField("contextImpl");
		contextField.setAccessible(true);
	}

	@Test
	public void testConstructorWorks(@Mocked final ContextImpl contextImpl, @Mocked final SessionImpl sess)
			throws IllegalArgumentException, IllegalAccessException {

		final DigestInitHandler dih = new DigestInitHandler(sess, contextImpl);
		assertEquals(sess, sessField.get(dih));
		assertEquals(contextImpl, contextField.get(dih));
	}

	@Test
	public void testReceiveMSGWorks(@Mocked final ContextImpl contextImpl, @Mocked final SessionImpl sess,
			@Mocked final MessageMSG message, @Mocked final Channel channel) throws BEEPException {

		final DigestInitHandler dih = new DigestInitHandler(sess, contextImpl);

		new NonStrictExpectations() {
			{
				message.getMsgno(); result = Message.PIGGYBACKED_MSGNO;
				message.getChannel(); result = channel;
			}
		};

		dih.receiveMSG(message);

		new VerificationsInOrder() {
			{
				channel.setRequestHandler((RequestHandler) any);
				message.sendRPY((OutputDataStream) any);
			}
		};
	}

	@Test
	public void testReceiveMSGSendsErrForNonPiggyback(@Mocked final ContextImpl contextImpl, @Mocked final SessionImpl sess, @Mocked final MessageMSG message)
			throws BEEPException {

		final DigestInitHandler dih = new DigestInitHandler(sess, contextImpl);

		new NonStrictExpectations() {
			{
				message.getMsgno(); result = 1;
			}
		};

		dih.receiveMSG(message);

		new VerificationsInOrder() {
			{
				message.sendERR((BEEPError) any);
			}
		};
	}

	@Test
	public void testReceiveMSGSetsRequestHandlerForPublisher(@Mocked final ContextImpl contextImpl, @Mocked final PublisherSessionImpl sess,
			@Mocked final MessageMSG message, @Mocked final Channel channel) throws BEEPException {

		final DigestInitHandler dih = new DigestInitHandler(sess, contextImpl);

		new NonStrictExpectations() {
			{
				message.getMsgno(); result = Message.PIGGYBACKED_MSGNO;
				message.getChannel(); result = channel;
				sess.getRecordType(); result = RecordType.Audit;
			}
		};

		dih.receiveMSG(message);

		new VerificationsInOrder() {
			{
				channel.setRequestHandler((DigestRequestHandler) any);
				message.sendRPY((OutputDataStream) any);
			}
		};
	}
}
