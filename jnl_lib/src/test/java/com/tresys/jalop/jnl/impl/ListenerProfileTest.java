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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.soap.MimeHeaders;

import mockit.Mock;
import mockit.MockUp;
import mockit.Mocked;
import mockit.NonStrictExpectations;
import mockit.VerificationsInOrder;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.beepcore.beep.core.BEEPError;
import org.beepcore.beep.core.BEEPException;
import org.beepcore.beep.core.Channel;
import org.beepcore.beep.core.InputDataStream;
import org.beepcore.beep.core.InputDataStreamAdapter;
import org.beepcore.beep.core.MessageMSG;
import org.beepcore.beep.core.OutputDataStream;
import org.beepcore.beep.core.ReplyListener;
import org.beepcore.beep.core.RequestHandler;
import org.beepcore.beep.core.Session;
import org.beepcore.beep.core.SessionTuningProperties;
import org.beepcore.beep.core.StartChannelException;
import org.beepcore.beep.profile.ProfileConfiguration;
import org.beepcore.beep.transport.tcp.TCPSession;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.tresys.jalop.jnl.ConnectionHandler;
import com.tresys.jalop.jnl.ConnectionHandler.ConnectError;
import com.tresys.jalop.jnl.ConnectionRequest;
import com.tresys.jalop.jnl.Mode;
import com.tresys.jalop.jnl.Publisher;
import com.tresys.jalop.jnl.RecordType;
import com.tresys.jalop.jnl.Role;
import com.tresys.jalop.jnl.SubscribeRequest;
import com.tresys.jalop.jnl.Subscriber;
import com.tresys.jalop.jnl.SubscriberSession;
import com.tresys.jalop.jnl.exceptions.JNLException;
import com.tresys.jalop.jnl.impl.messages.InitMessage;
import com.tresys.jalop.jnl.impl.messages.Utils;
import com.tresys.jalop.jnl.impl.publisher.PublisherSessionImpl;
import com.tresys.jalop.jnl.impl.subscriber.SubscriberSessionImpl;

public class ListenerProfileTest {

	// Needed to mock static functions in the Utils class.
    @Mocked
    private Utils utils;

	private static Field contextImplField;
	private static Field addressField;
	private static Field uriField;
	private static Field configField;
	private static Field roleField;

	@BeforeClass
    public static void setUpBeforeClass() throws SecurityException, NoSuchFieldException {
		contextImplField = ListenerProfile.class.getDeclaredField("contextImpl");
		contextImplField.setAccessible(true);

		addressField = ListenerProfile.class.getDeclaredField("address");
		addressField.setAccessible(true);

		uriField = ListenerProfile.class.getDeclaredField("uri");
		uriField.setAccessible(true);

		configField = ListenerProfile.class.getDeclaredField("config");
		configField.setAccessible(true);

		roleField = ListenerProfile.class.getDeclaredField("role");
		roleField.setAccessible(true);

		//Need to initialize SessionTuningProperties before trying to mock it
		try {
			new SessionTuningProperties();
		} catch(final Exception e) {
			//do nothing
		}
    }

	@Before
	public void setUp() {
		// Disable logging so the build doesn't get spammed.
		Logger.getRootLogger().setLevel(Level.OFF);
	}

	@Test
	public void testConstructorWorks(final ContextImpl contextImpl, final InetAddress address)
			throws IllegalAccessException {
		final ListenerProfile profile = new ListenerProfile(contextImpl, address);
		assertEquals(contextImpl, contextImplField.get(profile));
		assertEquals(address, addressField.get(profile));
	}

	@Test
	public void testAdvertiseProfileTrueWhenEncrypted(final Session sess, final SessionTuningProperties tuning,
			final ContextImpl contextImpl, final InetAddress address) throws BEEPException {

		final ListenerProfile profile = new ListenerProfile(contextImpl, address);

		new NonStrictExpectations() {
			{
				sess.getTuningProperties(); result = tuning;
				tuning.getEncrypted(); result = true;
			}
		};

		assertTrue(profile.advertiseProfile(sess));
	}

	@Test
	public void testAdvertiseProfileFalseWhenNotEncrypted(final Session sess, final SessionTuningProperties tuning,
			final ContextImpl contextImpl, final InetAddress address) throws BEEPException {

		final ListenerProfile profile = new ListenerProfile(contextImpl, address);

		new NonStrictExpectations() {
			{
				sess.getTuningProperties(); result = tuning;
				tuning.getEncrypted(); result = false;
			}
		};

		assertFalse(profile.advertiseProfile(sess));
	}

	@Test
	public void testCloseChannel(final ContextImpl contextImpl, final InetAddress address, final Channel channel)
			throws BEEPException {
		final ListenerProfile profile = new ListenerProfile(contextImpl, address);
		profile.closeChannel(channel);
	}

	@Test
	public void testStartChannelWorksWithNullData(final ContextImpl contextImpl, final InetAddress address, final Channel channel)
			throws StartChannelException {

		final ListenerProfile profile = new ListenerProfile(contextImpl, address);
		profile.startChannel(channel, null, null);

		new VerificationsInOrder() {
			{
				channel.setRequestHandler((RequestHandler) any);
			}
		};
	}

	@Test
	public void testStartChannelWorksWithDataAsSubscriber(final ContextImpl contextImpl, final InetAddress address, final Channel channel,
			final SubscriberSessionImpl subSess)
			throws StartChannelException, IllegalAccessException, JNLException {

		final ListenerProfile profile = new ListenerProfile(contextImpl, address);
		roleField.set(profile, Role.Subscriber);

		// mock up thread since this function is supposed to spawn a new thread, but
        // don't actually want it to do that.
        new MockUp<Thread>() {
            @Mock
            public void start() {
                // do nothing
            }
        };

		new NonStrictExpectations() {
			{
				contextImpl.findSubscriberSession((Session) any, anyInt); result = subSess;
			}
		};

		profile.startChannel(channel, null, "digest:1");

		new VerificationsInOrder() {
			{
				subSess.setDigestChannel((Channel) any);
				channel.setRequestHandler((RequestHandler) any);
			}
		};
	}

	@Test
	public void testStartChannelWorksWithDataAsPublisher(final ContextImpl contextImpl, final InetAddress address, final Channel channel,
			final PublisherSessionImpl pubSess)
			throws StartChannelException, IllegalAccessException, JNLException {

		final ListenerProfile profile = new ListenerProfile(contextImpl, address);
		roleField.set(profile, Role.Publisher);

		new NonStrictExpectations() {
			{
				contextImpl.findPublisherSession((Session) any, anyInt); result = pubSess;
			}
		};

		profile.startChannel(channel, null, "digest:1");

		new VerificationsInOrder() {
			{
				pubSess.setDigestChannel((Channel) any);
				channel.setRequestHandler((RequestHandler) any);
			}
		};
	}

	@Test
	public void testInitWorks(final ContextImpl contextImpl, final InetAddress address, final ProfileConfiguration profileConfig)
			throws BEEPException, IllegalAccessException {
		final ListenerProfile profile = new ListenerProfile(contextImpl, address);
		profile.init("uri", profileConfig);
		assertEquals(profileConfig, configField.get(profile));
		assertEquals("uri", uriField.get(profile));
	}

	@Test
	public void testReceiveMsgWorksAsSubscriber(final ContextImpl contextImpl, final InetAddress address, final MessageMSG msg,
			final InputDataStream ids, final InputDataStreamAdapter isa, final Channel channel, final TCPSession sess,
			final OutputDataStream ods, final Socket socket, final ConnectionHandler connectionHandler, final Subscriber subscriber,
			final SubscribeRequest request)
			throws BEEPException, JNLException {

		final ListenerProfile profile = new ListenerProfile(contextImpl, address);
		final String[] encodings = new String[]{Utils.BINARY};
		final String[] digests = new String[]{DigestMethod.SHA256};
		final MimeHeaders otherHeaders = new MimeHeaders();
		final InitMessage im = new InitMessage(RecordType.Log, Role.Publisher, Mode.Live, encodings,
				digests, "agent", otherHeaders);
		final Set<ConnectError> connectErrors = new HashSet<ConnectError>();

		new NonStrictExpectations() {
			{
				msg.getDataStream(); result = ids;
                ids.getInputStream(); result = isa;
                Utils.processInitMessage(isa); result = im;
				msg.getChannel(); result = channel;
                channel.getSession(); result = sess;
                sess.getSocket(); result = socket;
                socket.getInetAddress(); result = address;
                contextImpl.getConnectionHandler(); result = connectionHandler;
                connectionHandler.handleConnectionRequest(false, (ConnectionRequest) any); result = connectErrors;
                contextImpl.getAllowedXmlEncodings(); result = Arrays.asList(encodings);
                contextImpl.getAllowedMessageDigests(); result = Arrays.asList(digests);
                contextImpl.getSubscriber(); result = subscriber;
                contextImpl.getDefaultDigestTimeout(); result = 1;
                contextImpl.getDefaultPendingDigestMax(); result = 1;
                channel.getNumber(); result = 5;
                subscriber.getSubscribeRequest((SubscriberSession) any); result = request;
                request.getNonce(); result = "1";
                Utils.createInitAckMessage(anyString, anyString); result = ods;
			}
		};

		profile.receiveMSG(msg);

		new VerificationsInOrder() {
			{
				contextImpl.addSession(sess, (SubscriberSessionImpl) any);
				msg.sendRPY((OutputDataStream) any);
				channel.sendMSG((OutputDataStream) any, (ReplyListener) any);
			}
		};
	}

	@Test
	public void testReceiveMsgSendsJournalResume(final ContextImpl contextImpl, final InetAddress address, final MessageMSG msg,
			final InputDataStream ids, final InputDataStreamAdapter isa, final Channel channel, final TCPSession sess,
			final OutputDataStream ods, final Socket socket, final ConnectionHandler connectionHandler, final Subscriber subscriber,
			final SubscribeRequest request, final InputStream is)
			throws BEEPException, JNLException {

		final ListenerProfile profile = new ListenerProfile(contextImpl, address);
		final String[] encodings = new String[]{Utils.BINARY};
		final String[] digests = new String[]{DigestMethod.SHA256};
		final MimeHeaders otherHeaders = new MimeHeaders();
		final InitMessage im = new InitMessage(RecordType.Journal, Role.Publisher, Mode.Archive, encodings,
				digests, "agent", otherHeaders);
		final Set<ConnectError> connectErrors = new HashSet<ConnectError>();

		new NonStrictExpectations() {
			{
				msg.getDataStream(); result = ids;
                ids.getInputStream(); result = isa;
                Utils.processInitMessage(isa); result = im;
				msg.getChannel(); result = channel;
                channel.getSession(); result = sess;
                sess.getSocket(); result = socket;
                socket.getInetAddress(); result = address;
                contextImpl.getConnectionHandler(); result = connectionHandler;
                connectionHandler.handleConnectionRequest(false, (ConnectionRequest) any); result = connectErrors;
                contextImpl.getAllowedXmlEncodings(); result = Arrays.asList(encodings);
                contextImpl.getAllowedMessageDigests(); result = Arrays.asList(digests);
                contextImpl.getSubscriber(); result = subscriber;
                contextImpl.getDefaultDigestTimeout(); result = 1;
                contextImpl.getDefaultPendingDigestMax(); result = 1;
                channel.getNumber(); result = 5;
                subscriber.getSubscribeRequest((SubscriberSession) any); result = request;
                request.getResumeOffset(); result = (long) 25;
                request.getResumeInputStream(); result = is;
                request.getNonce(); result = "1";
                Utils.createInitAckMessage(anyString, anyString); result = ods;
			}
		};

		profile.receiveMSG(msg);

		new VerificationsInOrder() {
			{
				contextImpl.addSession(sess, (SubscriberSessionImpl) any);
				Utils.createJournalResumeMessage(anyString, 25);
				msg.sendRPY((OutputDataStream) any);
				channel.sendMSG((OutputDataStream) any, (ReplyListener) any);
			}
		};
	}

	@Test
	public void testReceiveMsgWorksAsPublisher(final ContextImpl contextImpl, final InetAddress address, final MessageMSG msg,
			final InputDataStream ids, final InputDataStreamAdapter isa, final Channel channel, final TCPSession sess,
			final OutputDataStream ods, final Socket socket, final ConnectionHandler connectionHandler, final Publisher publisher)
			throws BEEPException, JNLException {

		final ListenerProfile profile = new ListenerProfile(contextImpl, address);
		final String[] encodings = new String[]{Utils.BINARY};
		final String[] digests = new String[]{DigestMethod.SHA256};
		final MimeHeaders otherHeaders = new MimeHeaders();
		final InitMessage im = new InitMessage(RecordType.Log, Role.Subscriber, Mode.Live, encodings,
				digests, "agent", otherHeaders);
		final Set<ConnectError> connectErrors = new HashSet<ConnectError>();

		new NonStrictExpectations() {
			{
				msg.getDataStream(); result = ids;
                ids.getInputStream(); result = isa;
                Utils.processInitMessage(isa); result = im;
				msg.getChannel(); result = channel;
                channel.getSession(); result = sess;
                sess.getSocket(); result = socket;
                socket.getInetAddress(); result = address;
                contextImpl.getConnectionHandler(); result = connectionHandler;
                connectionHandler.handleConnectionRequest(false, (ConnectionRequest) any); result = connectErrors;
                contextImpl.getAllowedXmlEncodings(); result = Arrays.asList(encodings);
                contextImpl.getAllowedMessageDigests(); result = Arrays.asList(digests);
                contextImpl.getPublisher(); result = publisher;
                Utils.createInitAckMessage(anyString, anyString); result = ods;
			}
		};

		profile.receiveMSG(msg);

		new VerificationsInOrder() {
			{
				contextImpl.addSession(sess, (PublisherSessionImpl) any);
				channel.setRequestHandler((RequestHandler) any);
				msg.sendRPY((OutputDataStream) any);
			}
		};
	}

	@Test
	public void testReceiveMsgSendsInitNack(final ContextImpl contextImpl, final InetAddress address, final MessageMSG msg,
			final InputDataStream ids, final InputDataStreamAdapter isa, final Channel channel, final TCPSession sess,
			final OutputDataStream ods, final Socket socket, final ConnectionHandler connectionHandler, final Publisher publisher)
			throws BEEPException, JNLException {

		final ListenerProfile profile = new ListenerProfile(contextImpl, address);
		final String[] encodings = new String[]{Utils.BINARY};
		final String[] digests = new String[]{DigestMethod.SHA256};
		final MimeHeaders otherHeaders = new MimeHeaders();
		final InitMessage im = new InitMessage(RecordType.Log, Role.Subscriber, Mode.Live, encodings,
				digests, "agent", otherHeaders);
		final Set<ConnectError> connectErrors = new HashSet<ConnectError>();
		connectErrors.add(ConnectError.UnauthorizedMode);

		new NonStrictExpectations() {
			{
				msg.getDataStream(); result = ids;
                ids.getInputStream(); result = isa;
                Utils.processInitMessage(isa); result = im;
				msg.getChannel(); result = channel;
                channel.getSession(); result = sess;
                sess.getSocket(); result = socket;
                socket.getInetAddress(); result = address;
                contextImpl.getConnectionHandler(); result = connectionHandler;
                connectionHandler.handleConnectionRequest(false, (ConnectionRequest) any); result = connectErrors;
                contextImpl.getAllowedXmlEncodings(); result = Arrays.asList(encodings);
                contextImpl.getAllowedMessageDigests(); result = Arrays.asList(digests);
                Utils.createInitNackMessage(new ArrayList<ConnectError>(connectErrors)); result = ods;
			}
		};

		profile.receiveMSG(msg);

		new VerificationsInOrder() {
			{
				msg.sendERR((OutputDataStream) any);
			}
		};
	}

	@Test
	public void testReceiveMsgSendsInitNackForBadEncoding(final ContextImpl contextImpl, final InetAddress address, final MessageMSG msg,
			final InputDataStream ids, final InputDataStreamAdapter isa, final Channel channel, final TCPSession sess,
			final OutputDataStream ods, final Socket socket, final ConnectionHandler connectionHandler, final Publisher publisher)
			throws BEEPException, JNLException {

		final ListenerProfile profile = new ListenerProfile(contextImpl, address);
		final String[] encodings = new String[]{Utils.BINARY};
		final String[] otherEncodings = new String[]{Utils.ENC_XML};
		final String[] digests = new String[]{DigestMethod.SHA256};
		final MimeHeaders otherHeaders = new MimeHeaders();
		final InitMessage im = new InitMessage(RecordType.Log, Role.Subscriber, Mode.Live, encodings,
				digests, "agent", otherHeaders);
		final Set<ConnectError> connectErrors = new HashSet<ConnectError>();
		connectErrors.add(ConnectError.UnauthorizedMode);

		new NonStrictExpectations() {
			{
				msg.getDataStream(); result = ids;
                ids.getInputStream(); result = isa;
                Utils.processInitMessage(isa); result = im;
				msg.getChannel(); result = channel;
                channel.getSession(); result = sess;
                sess.getSocket(); result = socket;
                socket.getInetAddress(); result = address;
                contextImpl.getConnectionHandler(); result = connectionHandler;
                connectionHandler.handleConnectionRequest(false, (ConnectionRequest) any); result = connectErrors;
                contextImpl.getAllowedXmlEncodings(); result = Arrays.asList(otherEncodings);
                contextImpl.getAllowedMessageDigests(); result = Arrays.asList(digests);
                Utils.createInitNackMessage(new ArrayList<ConnectError>(connectErrors)); result = ods;
			}
		};

		profile.receiveMSG(msg);

		new VerificationsInOrder() {
			{
				msg.sendERR((OutputDataStream) any);
			}
		};
	}

	@Test
	public void testReceiveMsgSendsInitNackForBadDigest(final ContextImpl contextImpl, final InetAddress address, final MessageMSG msg,
			final InputDataStream ids, final InputDataStreamAdapter isa, final Channel channel, final TCPSession sess,
			final OutputDataStream ods, final Socket socket, final ConnectionHandler connectionHandler, final Publisher publisher)
			throws BEEPException, JNLException {

		final ListenerProfile profile = new ListenerProfile(contextImpl, address);
		final String[] encodings = new String[]{Utils.BINARY};
		final String[] otherDigests = new String[]{DigestMethod.SHA1};
		final String[] digests = new String[]{DigestMethod.SHA256};
		final MimeHeaders otherHeaders = new MimeHeaders();
		final InitMessage im = new InitMessage(RecordType.Log, Role.Subscriber, Mode.Live, encodings,
				digests, "agent", otherHeaders);
		final Set<ConnectError> connectErrors = new HashSet<ConnectError>();
		connectErrors.add(ConnectError.UnauthorizedMode);

		new NonStrictExpectations() {
			{
				msg.getDataStream(); result = ids;
                ids.getInputStream(); result = isa;
                Utils.processInitMessage(isa); result = im;
				msg.getChannel(); result = channel;
                channel.getSession(); result = sess;
                sess.getSocket(); result = socket;
                socket.getInetAddress(); result = address;
                contextImpl.getConnectionHandler(); result = connectionHandler;
                connectionHandler.handleConnectionRequest(false, (ConnectionRequest) any); result = connectErrors;
                contextImpl.getAllowedXmlEncodings(); result = Arrays.asList(encodings);
                contextImpl.getAllowedMessageDigests(); result = Arrays.asList(otherDigests);
                Utils.createInitNackMessage(new ArrayList<ConnectError>(connectErrors)); result = ods;
			}
		};

		profile.receiveMSG(msg);

		new VerificationsInOrder() {
			{
				msg.sendERR((OutputDataStream) any);
			}
		};
	}

	@Test
	public void testSendErrWorks(final ContextImpl contextImpl, final InetAddress address, final MessageMSG msg)
			throws BEEPException {

		final ListenerProfile profile = new ListenerProfile(contextImpl, address);
		profile.sendERR(msg);

		new VerificationsInOrder() {
			{
				msg.sendERR((BEEPError) any);
			}
		};
	}
}
