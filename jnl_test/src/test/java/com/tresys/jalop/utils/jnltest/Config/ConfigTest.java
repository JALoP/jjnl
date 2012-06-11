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

package com.tresys.jalop.utils.jnltest.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.FileNotFoundException;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.Set;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import org.junit.Test;
import org.junit.Before;

import com.tresys.jalop.jnl.RecordType;

import com.tresys.jalop.utils.jnltest.Config.Config;
import com.tresys.jalop.utils.jnltest.Config.ConfigurationException;

@SuppressWarnings("unchecked")
public class ConfigTest {
	private JSONObject jsonCfg;

	@Before
	public void setup() throws Exception {
		jsonCfg = new JSONObject();
		jsonCfg.put("address", "localhost");
		jsonCfg.put("beepAction", "listen");
		jsonCfg.put("port", 1234);
		jsonCfg.put("pendingDigestMax", 128);
		jsonCfg.put("pendingDigestTimeout", 120);
		jsonCfg.put("input", "./input");
		jsonCfg.put("output", "./output");
		jsonCfg.put("peers", new JSONArray());
	}

	@Test
	public void configWorks() {
		Config cfg = new Config("path/to/nothing");
		assertNotNull(cfg);
		assertEquals("path/to/nothing", cfg.getSource());
		assertNotNull(cfg.getRecordTypes());
		assertNotNull(cfg.getPeerConfigs());
		assertEquals((short)-1, cfg.getPendingDigestMax());
		assertEquals((long)-1, cfg.getPendingDigestTimeout());
		assertEquals((int)-1, cfg.getPort());
	}

	@Test
	public void createFromJsonReturnsValidConfigWithBeepActionListen() throws Exception {
		Config cfg = Config.createFromJson("path/to/nothing", jsonCfg);
		assertNotNull(cfg);
		assertEquals("path/to/nothing", cfg.getSource());
		assertEquals("localhost/0.0.0.0", cfg.getAddress().toString());
		assertEquals(128, cfg.getPendingDigestMax());
		assertEquals(120, cfg.getPendingDigestTimeout());
		assertEquals(1234, cfg.getPort());
		assertNotNull(cfg.getPeerConfigs());
	}

	@Test
	public void createFromJsonReturnsValidConfigWithBeepActionConnect() throws Exception {
		jsonCfg.put("beepAction", "connect");
		JSONObject pub = new JSONObject();
		pub.put("input", "/path/to/input");
		pub.put("sessionTimeout", "00:00:00");
		pub.put("dataClass", new JSONArray());
		jsonCfg.put("publisher", pub);
		Config cfg = Config.createFromJson("path/to/nothing", jsonCfg);
		assertNotNull(cfg);
		assertEquals("path/to/nothing", cfg.getSource());
		assertEquals("localhost/0.0.0.0", cfg.getAddress().toString());
		assertEquals(-1, cfg.getPendingDigestMax());
		assertEquals(-1, cfg.getPendingDigestTimeout());
		assertEquals(1234, cfg.getPort());
		assertNotNull(cfg.getPeerConfigs());
		assertTrue(cfg.getPeerConfigs().isEmpty());
		assertEquals(new SimpleDateFormat("hh:mm:ss").parse("00:00:00"), cfg.getSessionTimeout());
	}

	@Test(expected = ConfigurationException.class)
	public void createFromJsonFailsWithMissingBeepAction() throws Exception {
		jsonCfg.remove("beepAction");
		Config.createFromJson("path/to/nothing", jsonCfg);
	}

	@Test(expected = ConfigurationException.class)
	public void createFromJsonFailsWithImproperBeepAction() throws Exception {
		jsonCfg.remove("beepAction");
		jsonCfg.put("beepAction", "nothing");
		Config.createFromJson("path/to/nothing", jsonCfg);
	}

	@Test(expected = FileNotFoundException.class)
	public void parseThrowsExceptionGivenInvalidPath() throws Exception {
		Config.parse("/path/to/nothing");
	}

	@Test
	public void itemAsArrayWorks() throws Exception {
		JSONObject pub = new JSONObject();
		pub.put("sessionTimeout", "00:00:00");
		JSONArray data = new JSONArray();
		data.add("audit");
		pub.put("dataClass", data);
		jsonCfg.put("publisher", pub);
		Config cfg = new Config("/path/to/nothing");

		JSONArray data2 = cfg.itemAsArray("dataClass", pub, true);

		assertEquals(data, data2);
	}

	@Test(expected = ConfigurationException.class)
	public void itemAsArrayRequiredTrueFailsWithNoArray() throws Exception {
		JSONObject pub = new JSONObject();
		Config cfg = new Config("/path/to/nothing");

		cfg.itemAsArray("nothing", pub, true);
	}

	@Test
	public void itemAsArrayRequiredFalseReturnsNullWithNoArray() throws Exception {
		JSONObject pub = new JSONObject();
		Config cfg = new Config("/path/to/nothing");

		JSONArray data = cfg.itemAsArray("nothing", pub, false);
		assertNull(data);
	}

	@Test
	public void itemAsNumberWorks() throws Exception {
		Config cfg = new Config("/path/to/nothing");
		Number port = cfg.itemAsNumber("port", jsonCfg, true);

		assertEquals(1234, port.intValue());
	}

	@Test(expected = ConfigurationException.class)
	public void itemAsNumberRequiredTrueFailsWithNoArray() throws Exception {
		Config cfg = new Config("/path/to/nothing");

		cfg.itemAsNumber("nothing", jsonCfg, true);
	}

	@Test
	public void itemAsNumberRequiredFalseReturnsNullWithNoNumber() throws Exception {
		Config cfg = new Config("/path/to/nothing");

		Number port = cfg.itemAsNumber("nothing", jsonCfg, false);
		assertNull(port);
	}

	@Test
	public void itemAsStringWorks() throws Exception {
		Config cfg = new Config("/path/to/nothing");
		String action = cfg.itemAsString("beepAction", jsonCfg, true);

		assertEquals("listen", action);
	}

	@Test(expected = ConfigurationException.class)
	public void itemAsStringRequiredTrueFailsWithNoString() throws Exception {
		Config cfg = new Config("/path/to/nothing");

		cfg.itemAsString("nothing", jsonCfg, true);
	}

	@Test
	public void itemAsStringRequiredFalseReturnsNullWithNoString() throws Exception {
		Config cfg = new Config("/path/to/nothing");

		String action = cfg.itemAsString("nothing", jsonCfg, false);
		assertNull(action);
	}

	@Test
	public void objectToRecordTypeWorksWithJournal() throws Exception {
		Config cfg = new Config("/path/to/nothing");
		RecordType rt = cfg.objectToRecordType("journal");
		assertEquals(RecordType.Journal, rt);
	}

	@Test
	public void objectToRecordTypeWorksWithAudit() throws Exception {
		Config cfg = new Config("/path/to/nothing");
		RecordType rt = cfg.objectToRecordType("audit");
		assertEquals(RecordType.Audit, rt);
	}

	@Test
	public void objectToRecordTypeWorksWithLog() throws Exception {
		Config cfg = new Config("/path/to/nothing");
		RecordType rt = cfg.objectToRecordType("log");
		assertEquals(RecordType.Log, rt);
	}

	@Test(expected = ConfigurationException.class)
	public void objectToRecordTypeFailsOnInvalidType() throws Exception {
		Config cfg = new Config("/path/to/nothing");
		cfg.objectToRecordType("invalid");
	}

	@Test(expected = ConfigurationException.class)
	public void objectToRecordTypeFailsOnInvalidObject() throws Exception {
		Config cfg = new Config("/path/to/nothing");
		cfg.objectToRecordType((Object)cfg);
		// Keeps throwing a ClassCastException
	}

	@Test
	public void recordSetFromArrayWorks() throws Exception {
		JSONArray types = new JSONArray();
		types.add("journal");
		types.add("audit");
		types.add("log");
		Config cfg = new Config("/path/to/nothing");
		Set<RecordType> typeSet = cfg.recordSetFromArray(types);
		assertNotNull(typeSet);
		assertFalse(typeSet.isEmpty());
		assertTrue(typeSet.contains(RecordType.Journal));
		assertTrue(typeSet.contains(RecordType.Audit));
		assertTrue(typeSet.contains(RecordType.Log));
	}

	@Test
	public void recordSetFromArrayHandlesEmptyArray() throws Exception {
		JSONArray types = new JSONArray();
		Config cfg = new Config("/path/to/nothing");
		Set<RecordType> typeSet = cfg.recordSetFromArray(types);
		assertNotNull(typeSet);
		assertTrue(typeSet.isEmpty());
	}

	@Test
	public void recordSetFromArrayHandlesNullArray() throws Exception {
		Config cfg = new Config("/path/to/nothing");
		Set<RecordType> typeSet = cfg.recordSetFromArray(null);
		assertNotNull(typeSet);
		assertTrue(typeSet.isEmpty());
	}

	@Test
	public void updateKnownHostsWorks() throws Exception {
		Config cfg = new Config("/path/to/nothing");

		assertTrue(cfg.getPeerConfigs().isEmpty());

		JSONObject peer0 = new JSONObject();
		JSONArray hosts = new JSONArray();
		JSONArray pubAllow = new JSONArray();
		JSONArray subAllow = new JSONArray();
		peer0.put("hosts", hosts);
		peer0.put("publishAllow", pubAllow);
		peer0.put("subscribeAllow", subAllow);

		assertTrue(cfg.getPeerConfigs().isEmpty());

		cfg.updateKnownHosts(peer0);

		JSONObject peer1 = new JSONObject();
		hosts.add("192.168.1.1");
		peer1.put("hosts", hosts);

		pubAllow.add("audit");
		peer1.put("publishAllow", pubAllow);

		subAllow.add("log");
		peer1.put("subscribeAllow", subAllow);

		JSONObject peer2 = new JSONObject();
		hosts.clear();
		hosts.add("192.168.1.2");
		peer2.put("hosts", hosts);

		pubAllow.clear();
		pubAllow.add("audit");
		peer2.put("publishAllow", pubAllow);

		subAllow.clear();
		subAllow.add("log");
		peer2.put("subscribeAllow", subAllow);

		cfg.updateKnownHosts(peer1);

		Map<InetAddress, PeerConfig> peerCfgs = cfg.getPeerConfigs();

		InetAddress addr1 = cfg.stringToAddress("192.168.1.1");

		assertTrue(peerCfgs.containsKey(addr1));
		assertTrue(peerCfgs.get(cfg.stringToAddress("192.168.1.1")).getPublishAllow().contains(RecordType.Audit));
		assertFalse(peerCfgs.get(cfg.stringToAddress("192.168.1.1")).getPublishAllow().contains(RecordType.Journal));
		assertFalse(peerCfgs.get(cfg.stringToAddress("192.168.1.1")).getPublishAllow().contains(RecordType.Log));
		assertTrue(peerCfgs.get(cfg.stringToAddress("192.168.1.1")).getSubscribeAllow().contains(RecordType.Log));
		assertFalse(peerCfgs.get(cfg.stringToAddress("192.168.1.1")).getSubscribeAllow().contains(RecordType.Audit));
		assertFalse(peerCfgs.get(cfg.stringToAddress("192.168.1.1")).getSubscribeAllow().contains(RecordType.Journal));

		cfg.updateKnownHosts(peer2);

		peerCfgs = cfg.getPeerConfigs();

		assertTrue(peerCfgs.containsKey(cfg.stringToAddress("192.168.1.2")));
		assertTrue(peerCfgs.get(cfg.stringToAddress("192.168.1.2")).getPublishAllow().contains(RecordType.Audit));
		assertFalse(peerCfgs.get(cfg.stringToAddress("192.168.1.2")).getPublishAllow().contains(RecordType.Journal));
		assertFalse(peerCfgs.get(cfg.stringToAddress("192.168.1.2")).getPublishAllow().contains(RecordType.Log));
		assertTrue(peerCfgs.get(cfg.stringToAddress("192.168.1.2")).getSubscribeAllow().contains(RecordType.Log));
		assertFalse(peerCfgs.get(cfg.stringToAddress("192.168.1.2")).getSubscribeAllow().contains(RecordType.Journal));
		assertFalse(peerCfgs.get(cfg.stringToAddress("192.168.1.2")).getSubscribeAllow().contains(RecordType.Audit));

		peer2.remove("subscribeAllow");

		JSONArray subAllows = new JSONArray();
		subAllows.add("journal");

		peer2.put("subscribeAllow", subAllows);

		cfg.updateKnownHosts(peer2);

		peerCfgs = cfg.getPeerConfigs();

		assertTrue(peerCfgs.containsKey(cfg.stringToAddress("192.168.1.2")));
		assertTrue(peerCfgs.get(cfg.stringToAddress("192.168.1.2")).getPublishAllow().contains(RecordType.Audit));
		assertFalse(peerCfgs.get(cfg.stringToAddress("192.168.1.2")).getPublishAllow().contains(RecordType.Journal));
		assertFalse(peerCfgs.get(cfg.stringToAddress("192.168.1.2")).getPublishAllow().contains(RecordType.Log));
		assertTrue(peerCfgs.get(cfg.stringToAddress("192.168.1.2")).getSubscribeAllow().contains(RecordType.Log));
		assertTrue(peerCfgs.get(cfg.stringToAddress("192.168.1.2")).getSubscribeAllow().contains(RecordType.Journal));
		assertFalse(peerCfgs.get(cfg.stringToAddress("192.168.1.2")).getSubscribeAllow().contains(RecordType.Audit));
	}

	@Test(expected = ConfigurationException.class)
	public void updateKnownHostsFailsWhenHostIsNotAString() throws Exception {
		JSONObject peer1 = new JSONObject();
		JSONArray hosts = new JSONArray();
		hosts.add(jsonCfg);
		peer1.put("hosts", hosts);

		Config cfg = new Config("/path/to/nothing");

		cfg.updateKnownHosts(peer1);
	}
}
