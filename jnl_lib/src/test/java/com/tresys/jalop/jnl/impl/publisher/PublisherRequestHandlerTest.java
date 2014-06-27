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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.security.MessageDigest;

import javax.xml.soap.MimeHeaders;

import mockit.Expectations;
import mockit.Mocked;
import mockit.VerificationsInOrder;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.beepcore.beep.core.BEEPError;
import org.beepcore.beep.core.BEEPException;
import org.beepcore.beep.core.Channel;
import org.beepcore.beep.core.InputDataStream;
import org.beepcore.beep.core.InputDataStreamAdapter;
import org.beepcore.beep.core.MessageMSG;
import org.beepcore.beep.core.Session;
import org.junit.Before;
import org.junit.Test;

import com.tresys.jalop.jnl.Mode;
import com.tresys.jalop.jnl.Publisher;
import com.tresys.jalop.jnl.RecordType;
import com.tresys.jalop.jnl.SourceRecord;
import com.tresys.jalop.jnl.exceptions.JNLException;
import com.tresys.jalop.jnl.impl.ContextImpl;
import com.tresys.jalop.jnl.impl.JNLOutputDataStream;
import com.tresys.jalop.jnl.impl.messages.JournalResumeMessage;
import com.tresys.jalop.jnl.impl.messages.SubscribeMessage;
import com.tresys.jalop.jnl.impl.messages.Utils;


public class PublisherRequestHandlerTest {

	// Needed to mock static functions in the Utils class.
    @Mocked
    private Utils utils;

	@Before
	public void setUp() {
		// Disable logging so the build doesn't get spammed.
		Logger.getRootLogger().setLevel(Level.OFF);
	}

	@Test
	public void testPublisherRequestHandlerWorks(final ContextImpl contextImpl) {

		final PublisherRequestHandler prh =
			new PublisherRequestHandler(RecordType.Log, contextImpl);

		assertEquals(RecordType.Log, prh.recordType);
		assertEquals(contextImpl, prh.contextImpl);
	}
/*
	@Test
	public void testReceiveMSGWorks(final ContextImpl contextImpl, final MessageMSG message,
			final InputDataStream ids, final InputDataStreamAdapter isa,
			final PublisherSessionImpl publisherSess, final Publisher publisher,
			final Channel channel, final Session sess, final SourceRecord sourceRecord,
			final InputStream sysMeta, final InputStream appMeta, final InputStream payload,
			final MessageDigest md, final ByteArrayInputStream breakStream)
			throws BEEPException,
			SecurityException, NoSuchMethodException, InstantiationException,
			IllegalAccessException, InvocationTargetException, JNLException, IOException {

		final PublisherRequestHandler prh =
			new PublisherRequestHandler(RecordType.Log, contextImpl);

		final Constructor<SubscribeMessage> constructor = SubscribeMessage.class.getDeclaredConstructor(String.class, MimeHeaders.class);
		constructor.setAccessible(true);
		final SubscribeMessage sm = constructor.newInstance("nonce", new MimeHeaders());

		new Expectations() {
			{
				message.getDataStream(); result = ids;
				ids.getInputStream(); result = isa;
				contextImpl.getPublisher(); result = publisher;
				message.getChannel(); result = channel;
				channel.getSession(); result = sess;
				contextImpl.getPublisherSession(sess, (RecordType)any); result = publisherSess;
				isa.getHeaderValue(anyString); result = Utils.MSG_SUBSCRIBE_ARCHIVE;
				Utils.processSubscribe(isa); result = sm;
				publisher.onSubscribe(publisherSess, anyString, (Mode) Mode.Archive, (MimeHeaders) any); result = true;
			}
		};

		prh.receiveMSG(message);
	}

	@Test
	public void testReceiveMSGWorksForJournalResume(final ContextImpl contextImpl, final MessageMSG message,
			final InputDataStream ids, final InputDataStreamAdapter isa,
			final PublisherSessionImpl publisherSess, final Publisher publisher,
			final Channel channel, final Session sess, final SourceRecord sourceRecord,
			final InputStream sysMeta, final InputStream appMeta, final InputStream payload,
			final MessageDigest md, final ByteArrayInputStream breakStream)
			throws BEEPException,
			SecurityException, NoSuchMethodException, InstantiationException,
			IllegalAccessException, InvocationTargetException, JNLException, IOException {

		final PublisherRequestHandler prh =
			new PublisherRequestHandler(RecordType.Journal, contextImpl);

		final Constructor<JournalResumeMessage> constructor = JournalResumeMessage.class.getDeclaredConstructor(String.class, long.class, MimeHeaders.class);
		constructor.setAccessible(true);
		final JournalResumeMessage jrm = constructor.newInstance("nonce", (long) 25, new MimeHeaders());

		new Expectations() {
			{
				message.getDataStream(); result = ids;
				ids.getInputStream(); result = isa;
				contextImpl.getPublisher(); result = publisher;
				message.getChannel(); result = channel;
				channel.getSession(); result = sess;
				contextImpl.getPublisherSession(sess, (RecordType)any); result = publisherSess;
				isa.getHeaderValue(anyString); result = Utils.MSG_JOURNAL_RESUME; times = 3;
				Utils.processJournalResume(isa); result = jrm;
				publisher.onJournalResume(publisherSess, anyString, 25, (MimeHeaders) any); result = true;
			}
		};

		prh.receiveMSG(message);
	}
*/
	@Test
	public void testSendErrWorks(final ContextImpl contextImpl, final MessageMSG message)
			throws BEEPException {
		final PublisherRequestHandler prh =
			new PublisherRequestHandler(RecordType.Log, contextImpl);
		prh.sendERR(message);

		new VerificationsInOrder() {
			{
				message.sendERR((BEEPError) any);
			}
		};
	}
}
