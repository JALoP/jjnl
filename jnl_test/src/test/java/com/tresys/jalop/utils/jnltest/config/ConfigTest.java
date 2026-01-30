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

package com.tresys.jalop.utils.jnltest.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.io.IOException;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import org.junit.Test;
import org.junit.Before;

import com.google.common.net.InetAddresses;
import com.tresys.jalop.jnl.Mode;
import com.tresys.jalop.jnl.RecordType;
import com.tresys.jalop.jnl.impl.DigestAlgorithms;

import com.tresys.jalop.utils.jnltest.config.Config;
import com.tresys.jalop.utils.jnltest.config.ConfigurationException;

@SuppressWarnings("unchecked")
public class ConfigTest {
	private JSONObject jsonCfg;
	private JSONObject sub;
	private JSONObject pub;
	private JSONObject listener;

	@Before
	public void setup() throws Exception {
		jsonCfg = new JSONObject();
		jsonCfg.put("address", "127.0.0.1");
		jsonCfg.put("port", 1234);
		jsonCfg.put("configureTLS", "on");
        JSONArray dataClassArray = new JSONArray();
        dataClassArray.add("audit");

        sub = new JSONObject();
        sub.put("dataClass", dataClassArray);
        sub.put("pendingDigestMax", 128);
        sub.put("pendingDigestTimeout", 120);
        sub.put("output", "./output");
        sub.put("sessionTimeout", "00:00:00");
        sub.put("mode", "Live");
        sub.put("bufferSize", 4096);
        sub.put("journalResumeThresholdSize", 0);

	    pub = new JSONObject();
        pub.put("dataClass", dataClassArray);
	    pub.put("input", "./input");
	    pub.put("sessionTimeout", "00:00:00");
		pub.put("mode", "Live");

	    listener = new JSONObject();
	    listener.put("pendingDigestMax", 128);
	    listener.put("pendingDigestTimeout", 120);
	    listener.put("output", "./output");
	    listener.put("input", "./input");
	    listener.put("peers", new JSONArray());
	    listener.put("sessionTimeout", "00:00:00");
	    listener.put("journalResumeThresholdSize", 0);
	}


	@Test
	public void configWorks() {
		JSONArray digestArray = new JSONArray();
		digestArray.add(DigestAlgorithms.JJNL_SHA256_ALGORITHM_NAME);
		digestArray.add(DigestAlgorithms.JJNL_SHA512_ALGORITHM_NAME);
		sub.put(Config.getSupportedDigestAlgorithmsConfigKey(), digestArray);

		Config cfg = new Config("path/to/nothing");
		assertNotNull(cfg);
		assertEquals("path/to/nothing", cfg.getSource());
		assertNotNull(cfg.getRecordTypes());
		assertNotNull(cfg.getPeerConfigs());
		assertEquals((short)-1, cfg.getPendingDigestMax());
		assertEquals(-1, cfg.getPendingDigestTimeout());
		assertEquals(-1, cfg.getPort());
	}

	@Test
	public void createFromJsonReturnsValidConfigForListener() throws Exception {
	    jsonCfg.put("listener", listener);
		Config cfg = Config.createFromJson("path/to/nothing", jsonCfg);
		assertNotNull(cfg);
		assertEquals("path/to/nothing", cfg.getSource());
		assertTrue(Arrays.equals(new byte[]{127,0,0,1}, cfg.getAddress().getAddress()));
		assertEquals(128, cfg.getPendingDigestMax());
		assertEquals(120, cfg.getPendingDigestTimeout());
		assertEquals(1234, cfg.getPort());
		assertNotNull(cfg.getPeerConfigs());
	}

	@Test
	public void createFromJsonReturnsValidConfigWithPublisher() throws Exception {
		jsonCfg.put("publisher", pub);
		Config cfg = Config.createFromJson("path/to/nothing", jsonCfg);
		assertNotNull(cfg);
		assertEquals("path/to/nothing", cfg.getSource());
		assertTrue(Arrays.equals(new byte[]{127,0,0,1}, cfg.getAddress().getAddress()));
		assertEquals(-1, cfg.getPendingDigestMax());
		assertEquals(-1, cfg.getPendingDigestTimeout());
		assertEquals(1234, cfg.getPort());
		assertNotNull(cfg.getPeerConfigs());
		assertTrue(cfg.getPeerConfigs().isEmpty());
		assertEquals(new File("./input").getPath(), cfg.getInputPath().getPath());
		assertEquals(0, cfg.getSessionTimeout());
	}

	@Test
	public void createFromJsonReturnsValidConfigWithSubscriber() throws Exception {
		boolean configerr = false;

		DigestAlgorithms digestAlgorithms = DigestAlgorithms.getInstance();
		// Need to hard-code this so that it runs the same on Java 8 and 11
		digestAlgorithms.setSHA384Supported(true);

		JSONArray digestArray = new JSONArray();
		digestArray.add(DigestAlgorithms.JJNL_SHA256_ALGORITHM_NAME);
		digestArray.add(DigestAlgorithms.JJNL_SHA384_ALGORITHM_NAME);
		digestArray.add(DigestAlgorithms.JJNL_SHA512_ALGORITHM_NAME);
		sub.put(Config.getSupportedDigestAlgorithmsConfigKey(), digestArray);

		jsonCfg.put("subscriber", sub);

		Config cfg = null;
		try
		{
        	cfg = Config.createFromJson("path/to/nothing", jsonCfg);
		}
		catch(ConfigurationException ce)
		{
			configerr = true;
		}

		assertNotNull(cfg);
		assertEquals("path/to/nothing", cfg.getSource());
		assertTrue(Arrays.equals(new byte[]{127,0,0,1}, cfg.getAddress().getAddress()));
		assertEquals(128, cfg.getPendingDigestMax());
		assertEquals(120, cfg.getPendingDigestTimeout());
		assertEquals(1234, cfg.getPort());
		assertEquals(Mode.Live, cfg.getMode());
		assertNotNull(cfg.getPeerConfigs());
		assertTrue(cfg.getPeerConfigs().isEmpty());
		assertEquals(new File("./output").getPath(), cfg.getOutputPath().getPath());
		assertEquals(0, cfg.getSessionTimeout());
    }

	/**
	* Checks that a ConfigurationException is thrown with an unsupported digest
	*/
	@Test
	public void testUnsupportedDigest()
	{
		boolean configerr = false;
		boolean parseerr = false;
		boolean ioerr = false;

		final String path = "../jnl_test/src/test/resources/sampleSubscriberUnsupportedDigest.json";
		try
		{
			Config.parse(path);
		}
		catch(ParseException pe)
		{
			parseerr = true;
		}
		catch(ConfigurationException ce)
		{
			configerr = true;
		}
		catch(IOException e)
		{
			ioerr = true;
		}

		assertEquals(true, configerr);
		assertEquals(false, parseerr);
		assertEquals(false, ioerr);
	}

	/**
	 * Checks to ensure defaults to SHA256 if no config file entry for algorithms
	 */
	@Test
	public void testConfigDefaultDigestAlgorithmSHA256() {
		boolean configerr = false;
		boolean parseerr = false;
		boolean ioerr = false;

		final String path = "../jnl_test/src/test/resources/sampleSubscriberNoDigest.json";
		try
		{
			Config.parse(path);
		}
		catch(ParseException pe)
		{
			parseerr = true;
		}
		catch(ConfigurationException ce)
		{
			configerr = true;
		}
		catch(IOException e)
		{
			ioerr = true;
		}

		assertEquals(false, configerr);
		assertEquals(false, parseerr);
		assertEquals(false, ioerr);

		DigestAlgorithms digestAlgorithms = DigestAlgorithms.getInstance();
		List<String> algorithmList = digestAlgorithms.getDigestAlgorithmUris();
		assertEquals(1, algorithmList.size());
		assertEquals(algorithmList.get(0), DigestAlgorithms.JJNL_SHA256_ALGORITHM_URI);
	}

	/*
	 * A simple test for adding digest names to the DigestAlgorithms class
	 */
	@Test
	public void testDigestAlgorithmsDigestAddNameWorks() throws Exception
	{
		DigestAlgorithms da = DigestAlgorithms.getInstance();
		da.setSHA384Supported(true);

		da.addDigestAlgorithmByName(DigestAlgorithms.JJNL_SHA256_ALGORITHM_NAME);
		da.addDigestAlgorithmByName(DigestAlgorithms.JJNL_SHA384_ALGORITHM_NAME);
		da.addDigestAlgorithmByName(DigestAlgorithms.JJNL_SHA512_ALGORITHM_NAME);

		List<String> dalist = da.getDigestAlgorithmNames();
		int cnt = dalist.size();
		assertEquals(cnt, 3);
		da.clear();
	}

	/*
	 * A simple test for adding digest URI's to the DigestAlgorithms class
	 */
	@Test
	public void testDigestAlgorithmsDigestAddUriWorks() throws Exception
	{
		DigestAlgorithms da = DigestAlgorithms.getInstance();
		da.setSHA384Supported(true);

		da.addDigestAlgorithmByUri(DigestAlgorithms.JJNL_SHA256_ALGORITHM_URI);
		da.addDigestAlgorithmByUri(DigestAlgorithms.JJNL_SHA384_ALGORITHM_URI);
		da.addDigestAlgorithmByUri(DigestAlgorithms.JJNL_SHA512_ALGORITHM_URI);

		List<String> dalist = da.getDigestAlgorithmUris();
		int cnt = dalist.size();
		assertEquals(cnt, 3);
		da.clear();
	}

	/*
	 * A simple test for removing a digest from the DigestAlgorithms class
	 */
	@Test
	public void testDigestAlgorithmsDigestRemoveWorks() throws Exception
	{
		DigestAlgorithms da = DigestAlgorithms.getInstance();
		da.setSHA384Supported(true);

		da.addDigestAlgorithmByUri(DigestAlgorithms.JJNL_SHA256_ALGORITHM_URI);
		da.addDigestAlgorithmByUri(DigestAlgorithms.JJNL_SHA384_ALGORITHM_URI);
		da.addDigestAlgorithmByUri(DigestAlgorithms.JJNL_SHA512_ALGORITHM_URI);

		List<String> dalist = da.getDigestAlgorithmUris();
		int cnt = dalist.size();

		assertEquals(cnt, 3);

		da.removeDigestAlgorithmByUri(DigestAlgorithms.JJNL_SHA256_ALGORITHM_URI);

		dalist = da.getDigestAlgorithmUris();
		cnt = dalist.size();

		assertEquals(cnt, 2);

		da.clear();
	}

	/*
	 * A simple test to ensure the default digest is available
	 */
	@Test
	public void testDigestAlgorithmsDefaultDigestWorks() throws Exception
	{
		String name = DigestAlgorithms.JJNL_DEFAULT_ALGORITHM.toName();

		assertNotNull(name);
	}

	/*
	 * Checks to ensure SHA384 will fail when unsupported
	 */
	@Test
	public void testDigestAlgorithmsUnsupportedSHA384() {
		DigestAlgorithms digestAlgorithms = DigestAlgorithms.getInstance();
		// Need to hard-code this so that it fails the same on Java 8 and 11
		digestAlgorithms.setSHA384Supported(false);

		List<String> algorithmList = digestAlgorithms.getDigestAlgorithmUris();
		int cnt = algorithmList.size();

		assertEquals(cnt, 0);

		digestAlgorithms.addDigestAlgorithmByName(DigestAlgorithms.JJNL_SHA384_ALGORITHM_NAME);

		algorithmList = digestAlgorithms.getDigestAlgorithmUris();
		cnt = algorithmList.size();

		assertEquals(cnt, 0);
	}

	@Test(expected = ConfigurationException.class)
	public void createFromJsonFailsWithWithNoListenerPublisherOrSubscriber() throws Exception {
		Config.createFromJson("path/to/nothing", jsonCfg);
	}

	@Test(expected = FileNotFoundException.class)
	public void parseThrowsExceptionGivenInvalidPath() throws Exception {
		Config.parse("/path/to/nothing");
	}

	@Test
	public void itemAsArrayWorks() throws Exception {
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
		String action = cfg.itemAsString("address", jsonCfg, true);

		assertEquals("127.0.0.1", action);
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
		cfg.objectToRecordType(cfg);
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

		cfg.updateKnownHosts(peer1);

		JSONObject peer2 = new JSONObject();
		hosts.clear();
		hosts.add("192.168.1.2");
		peer2.put("hosts", hosts);

		pubAllow.clear();
		pubAllow.add("log");
		peer2.put("publishAllow", pubAllow);

		subAllow.clear();
		subAllow.add("audit");
		peer2.put("subscribeAllow", subAllow);

		Map<InetAddress, PeerConfig> peerCfgs = cfg.getPeerConfigs();

		InetAddress addr1 = InetAddresses.forString("192.168.1.1");

		assertTrue(peerCfgs.containsKey(addr1));
		assertTrue(peerCfgs.get(InetAddresses.forString("192.168.1.1")).getPublishAllow().contains(RecordType.Audit));
		assertFalse(peerCfgs.get(InetAddresses.forString("192.168.1.1")).getPublishAllow().contains(RecordType.Journal));
		assertFalse(peerCfgs.get(InetAddresses.forString("192.168.1.1")).getPublishAllow().contains(RecordType.Log));
		assertTrue(peerCfgs.get(InetAddresses.forString("192.168.1.1")).getSubscribeAllow().contains(RecordType.Log));
		assertFalse(peerCfgs.get(InetAddresses.forString("192.168.1.1")).getSubscribeAllow().contains(RecordType.Audit));
		assertFalse(peerCfgs.get(InetAddresses.forString("192.168.1.1")).getSubscribeAllow().contains(RecordType.Journal));

		cfg.updateKnownHosts(peer2);

		peerCfgs = cfg.getPeerConfigs();

		assertTrue(peerCfgs.containsKey(InetAddresses.forString("192.168.1.2")));
		assertFalse(peerCfgs.get(InetAddresses.forString("192.168.1.2")).getPublishAllow().contains(RecordType.Audit));
		assertFalse(peerCfgs.get(InetAddresses.forString("192.168.1.2")).getPublishAllow().contains(RecordType.Journal));
		assertTrue(peerCfgs.get(InetAddresses.forString("192.168.1.2")).getPublishAllow().contains(RecordType.Log));
		assertFalse(peerCfgs.get(InetAddresses.forString("192.168.1.2")).getSubscribeAllow().contains(RecordType.Log));
		assertFalse(peerCfgs.get(InetAddresses.forString("192.168.1.2")).getSubscribeAllow().contains(RecordType.Journal));
		assertTrue(peerCfgs.get(InetAddresses.forString("192.168.1.2")).getSubscribeAllow().contains(RecordType.Audit));

		peer2.remove("subscribeAllow");

		JSONArray subAllows = new JSONArray();
		subAllows.add("journal");

		peer2.put("subscribeAllow", subAllows);

		cfg.updateKnownHosts(peer2);

		peerCfgs = cfg.getPeerConfigs();

		assertTrue(peerCfgs.containsKey(InetAddresses.forString("192.168.1.2")));
		assertFalse(peerCfgs.get(InetAddresses.forString("192.168.1.2")).getPublishAllow().contains(RecordType.Audit));
		assertFalse(peerCfgs.get(InetAddresses.forString("192.168.1.2")).getPublishAllow().contains(RecordType.Journal));
		assertTrue(peerCfgs.get(InetAddresses.forString("192.168.1.2")).getPublishAllow().contains(RecordType.Log));
		assertFalse(peerCfgs.get(InetAddresses.forString("192.168.1.2")).getSubscribeAllow().contains(RecordType.Log));
		assertTrue(peerCfgs.get(InetAddresses.forString("192.168.1.2")).getSubscribeAllow().contains(RecordType.Journal));
		assertTrue(peerCfgs.get(InetAddresses.forString("192.168.1.2")).getSubscribeAllow().contains(RecordType.Audit));
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

	@Test (expected = ConfigurationException.class)
	public void throwsExceptionWhenHasListenerAndSubscriber() throws ConfigurationException {
	    jsonCfg.put("subscriber", sub);
        jsonCfg.put("listener", listener);
        Config.createFromJson("path/to/nothing", jsonCfg);
	}

    @Test (expected = ConfigurationException.class)
    public void throwsExceptionWhenHasListenerAndPublisher() throws ConfigurationException {
        jsonCfg.put("publisher", pub);
        jsonCfg.put("listener", listener);
        Config.createFromJson("path/to/nothing", jsonCfg);
    }
    @Test (expected = ConfigurationException.class)
    public void throwsExceptionWhenHasPublisherAndSubscriber() throws ConfigurationException {
        jsonCfg.put("subscriber", sub);
        jsonCfg.put("publisher", pub);
        Config.createFromJson("path/to/nothing", jsonCfg);
    }
    @Test (expected = ConfigurationException.class)
    public void throwsExceptionWhenHasPublisherAndListenerAndSubscriber() throws ConfigurationException {
        jsonCfg.put("subscriber", sub);
        jsonCfg.put("publisher", pub);
        jsonCfg.put("listener", listener);
        Config.createFromJson("path/to/nothing", jsonCfg);
    }

/*    @Test (expected = ConfigurationException.class)
    public void throwsExceptionWhenDoesNotHaveAnyPublisherOrListenerOrSubscriber() throws ConfigurationException {
        Config.createFromJson("path/to/nothing", jsonCfg);
    }
*/}
