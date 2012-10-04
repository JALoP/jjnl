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

package com.tresys.jalop.utils.jnltest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import mockit.NonStrictExpectations;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.tresys.jalop.jnl.ConnectionHandler;
import com.tresys.jalop.jnl.ConnectionHandler.ConnectError;
import com.tresys.jalop.jnl.ConnectionRequest;
import com.tresys.jalop.jnl.RecordType;
import com.tresys.jalop.jnl.Role;
import com.tresys.jalop.utils.jnltest.Config.PeerConfig;


public class ConnectionHandlerImplTest {

	private Map<InetAddress, PeerConfig> subPeerConfigs;
	private Map<InetAddress, PeerConfig> pubPeerConfigs;
	private static Field peerConfigsField;
	private static Field publishAllowField;
	private static Field subscribeAllowField;

	@BeforeClass
    public static void setUpBeforeClass() throws SecurityException, NoSuchFieldException {
		peerConfigsField = ConnectionHandlerImpl.class.getDeclaredField("peerConfigs");
		peerConfigsField.setAccessible(true);

		publishAllowField = PeerConfig.class.getDeclaredField("publishAllow");
		publishAllowField.setAccessible(true);

		subscribeAllowField = PeerConfig.class.getDeclaredField("subscribeAllow");
		subscribeAllowField.setAccessible(true);
    }

	@Before
	public void setup() throws Exception {
		pubPeerConfigs = new HashMap<InetAddress, PeerConfig>();
		final PeerConfig pubPc = new PeerConfig();
		final Set<RecordType> pubRecTypes = new HashSet<RecordType>();
		pubRecTypes.add(RecordType.Audit);
		publishAllowField.set(pubPc, pubRecTypes);
		pubPeerConfigs.put(InetAddress.getByName("localhost"), pubPc);

		subPeerConfigs = new HashMap<InetAddress, PeerConfig>();
		final PeerConfig subPc = new PeerConfig();
		final Set<RecordType> subRecTypes = new HashSet<RecordType>();
		subRecTypes.add(RecordType.Audit);
		subscribeAllowField.set(subPc, subRecTypes);
		subPeerConfigs.put(InetAddress.getByName("localhost"), subPc);

		// Disable logging so the build doesn't get spammed.
		Logger.getRootLogger().setLevel(Level.OFF);
	}

	@Test
	public void testConstructor() throws  IllegalAccessException {
		final ConnectionHandler ch = new ConnectionHandlerImpl(pubPeerConfigs);
		assertEquals(pubPeerConfigs, peerConfigsField.get(ch));
	}

	@Test
	public void testHandleConnectionRequestNoErrors(final ConnectionRequest connRequest)
			throws UnknownHostException, IllegalAccessException {

		final ConnectionHandler ch = new ConnectionHandlerImpl(pubPeerConfigs);

		new NonStrictExpectations() {
			{
				connRequest.getAddress(); result = InetAddress.getByName("localhost");
				connRequest.getRecordType(); result = RecordType.Audit;
				connRequest.getRole(); result = Role.Publisher;
			}
		};

		final Set<ConnectError> errors = ch.handleConnectionRequest(false, connRequest);
		assertTrue(errors.isEmpty());
	}

	@Test
	public void testHandleConnectionRequestErrorWhenWrongAddress(final ConnectionRequest connRequest, final InetAddress address)
			throws UnknownHostException, IllegalAccessException {

		final ConnectionHandler ch = new ConnectionHandlerImpl(pubPeerConfigs);

		new NonStrictExpectations() {
			{
				connRequest.getAddress(); result = address;
			}
		};

		final Set<ConnectError> errors = ch.handleConnectionRequest(false, connRequest);
		assertTrue(errors.contains(ConnectError.UnsupportedMode));
	}

	@Test
	public void testHandleConnectionRequestErrorWhenWrongRolePub(final ConnectionRequest connRequest)
			throws UnknownHostException, IllegalAccessException {

		final ConnectionHandler ch = new ConnectionHandlerImpl(pubPeerConfigs);

		new NonStrictExpectations() {
			{
				connRequest.getAddress(); result = InetAddress.getByName("localhost");
				connRequest.getRole(); result = Role.Subscriber;
			}
		};

		final Set<ConnectError> errors = ch.handleConnectionRequest(false, connRequest);
		assertTrue(errors.contains(ConnectError.UnsupportedMode));
	}

	@Test
	public void testHandleConnectionRequestErrorWhenWrongRoleSub(final ConnectionRequest connRequest)
			throws UnknownHostException, IllegalAccessException {

		final ConnectionHandler ch = new ConnectionHandlerImpl(subPeerConfigs);

		new NonStrictExpectations() {
			{
				connRequest.getAddress(); result = InetAddress.getByName("localhost");
				connRequest.getRole(); result = Role.Publisher;
			}
		};

		final Set<ConnectError> errors = ch.handleConnectionRequest(false, connRequest);
		assertTrue(errors.contains(ConnectError.UnsupportedMode));
	}

	@Test
	public void testHandleConnectionRequestErrorWhenWrongRecPub(final ConnectionRequest connRequest)
			throws UnknownHostException, IllegalAccessException {

		final ConnectionHandler ch = new ConnectionHandlerImpl(pubPeerConfigs);

		new NonStrictExpectations() {
			{
				connRequest.getAddress(); result = InetAddress.getByName("localhost");
				connRequest.getRecordType(); result = RecordType.Log;
				connRequest.getRole(); result = Role.Publisher;
			}
		};

		final Set<ConnectError> errors = ch.handleConnectionRequest(false, connRequest);
		assertTrue(errors.contains(ConnectError.UnauthorizedMode));
	}

	@Test
	public void testHandleConnectionRequestErrorWhenWrongRecSub(final ConnectionRequest connRequest)
			throws UnknownHostException, IllegalAccessException {

		final ConnectionHandler ch = new ConnectionHandlerImpl(subPeerConfigs);

		new NonStrictExpectations() {
			{
				connRequest.getAddress(); result = InetAddress.getByName("localhost");
				connRequest.getRecordType(); result = RecordType.Log;
				connRequest.getRole(); result = Role.Subscriber;
			}
		};

		final Set<ConnectError> errors = ch.handleConnectionRequest(false, connRequest);
		assertTrue(errors.contains(ConnectError.UnauthorizedMode));
	}

}
