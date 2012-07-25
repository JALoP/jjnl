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

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.beepcore.beep.core.BEEPException;
import org.json.simple.parser.ParseException;

import com.tresys.jalop.jnl.DigestStatus;
import com.tresys.jalop.jnl.RecordInfo;
import com.tresys.jalop.jnl.RecordType;
import com.tresys.jalop.jnl.Role;
import com.tresys.jalop.jnl.Session;
import com.tresys.jalop.jnl.SubscribeRequest;
import com.tresys.jalop.jnl.Subscriber;
import com.tresys.jalop.jnl.SubscriberSession;
import com.tresys.jalop.jnl.exceptions.JNLException;
import com.tresys.jalop.jnl.impl.ContextImpl;
import com.tresys.jalop.utils.jnltest.Config.Config;
import com.tresys.jalop.utils.jnltest.Config.ConfigurationException;

/**
 * Main class for JNLTest
 */
public class JNLTest implements Subscriber {
    /** Logger for this class */
    private static final Logger logger = Logger.getLogger(JNLTest.class);
	/**
	 * Configuration for this instance of JNLTest.
	 */
	private Config config;
	/**
	 * From Sessions to associated {@link SubscriberImpl} 
	 */
	private final Map<Session, Map<RecordType, SubscriberImpl>> sessMap = new HashMap<Session, Map<RecordType,SubscriberImpl>>();
	/**
	 * Create a JNLTest object based on the specified configuration.
	 *
	 * @param config
	 *            A {@link Config}
	 */
	public JNLTest(Config config) {
		this.config = config;
	}

	/**
	 * Main entry point for the JNLTest program. This takes a single argument,
	 * the full path to a configuration file to use.
	 *
	 * @param args
	 *            The command line arguments
	 * @throws BEEPException 
	 * @throws JNLException 
	 */
	public static void main(String[] args) throws JNLException, BEEPException {
		if (args.length != 1) {
			System.err.println("Must specify exactly one argument that is "
					+ " the configuration file to use");
            System.exit(1);
		}
		Config config;
		try {
			config = Config.parse(args[0]);
		} catch (IOException e) {
			System.err.println("Caught IO exception: " + e.getMessage());
			System.exit(1);
			throw new RuntimeException("Failed to call exit()");
		} catch (ParseException e) {
			System.err.print(e.toString());
			System.exit(1);
			throw new RuntimeException("Failed to call exit()");
		} catch (ConfigurationException e) {
			System.err.println("Exception processing the config file: "
					+ e.getMessage());
			System.exit(1);
			throw new RuntimeException("Failed to call exit()");
		}
		JNLTest jt = new JNLTest(config);
        System.out.println("Started Connections");
		jt.start();
	}

	/**
	 * Once a {@link JNLTest} object has a config, call this method to connect to the 
	 * remotes, or wait for incoming connections.
	 * @throws BEEPException 
	 * @throws JNLException 
	 */
    private void start() throws JNLException, BEEPException {
        if (!this.config.isListener()) {
            if (this.config.getRole() == Role.Subscriber) {
                ContextImpl contextImpl = new ContextImpl(null, this, null, this.config.getPendingDigestTimeout(), config.getPendingDigestMax(), false, "agent", null, null);
				contextImpl.subscribe(this.config.getAddress(), this.config.getPort(), this.config.getRecordTypes().toArray(new RecordType[0]));

            } else if (this.config.getRole() == Role.Publisher) {
                // TODO: do things as publisher:
                this.logger.error("Role of Publisher not currently supported");
            }
            this.logger.info("Waiting: " + config.getSessionTimeout());
            synchronized(this) {
                try {
                    this.wait(config.getSessionTimeout().getTime());
                } catch (InterruptedException e) {
                    this.logger.info("Someone woke us up");
                }
            }
        } else {
            // TODO: do things as a listener
            this.logger.error("Acting as listener not currently supported");
        }
    }

    @Override
    public SubscribeRequest getSubscribeRequest(SubscriberSession sess) {
        // TODO: All the code here to manage the maps should really be happening in the 
        // connection handler callbacks, but the library isn't generating those events 
        // quite yet.
        Map<RecordType, SubscriberImpl> map;
        synchronized (this.sessMap) {
            map = this.sessMap.get(sess);
            if (map == null) {
                map = new HashMap<RecordType, SubscriberImpl>();
                this.sessMap.put(sess, map);
            }
        }
        SubscriberImpl sub;
        synchronized(map) {
            sub = map.get(sess.getRecordType());
            if (sub == null) {
                sub = new SubscriberImpl(sess.getRecordType(), this.config.getOutputPath(), sess.getAddress());
                map.put(sess.getRecordType(), sub);
            }
        }
        return sub.getSubscribeRequest(sess);
    }

    @Override
    public boolean notifySysMetadata(SubscriberSession sess, RecordInfo recordInfo, InputStream sysMetaData) {
        return this.sessMap.get(sess).get(sess.getRecordType()).notifySysMetadata(sess, recordInfo, sysMetaData);
    }

    @Override
    public boolean notifyAppMetadata(SubscriberSession sess, RecordInfo recordInfo, InputStream appMetaData) {
        return this.sessMap.get(sess).get(sess.getRecordType()).notifyAppMetadata(sess, recordInfo, appMetaData);
    }

    @Override
    public boolean notifyPayload(SubscriberSession sess, RecordInfo recordInfo, InputStream payload) {
        return this.sessMap.get(sess).get(sess.getRecordType()).notifyPayload(sess, recordInfo, payload);
    }

    @Override
    public boolean notifyDigest(SubscriberSession sess, RecordInfo recordInfo, byte[] digest) {
        return this.sessMap.get(sess).get(sess.getRecordType()).notifyDigest(sess, recordInfo, digest);
    }

    @Override
    public boolean notifyDigestResponse(SubscriberSession sess, Map<String, DigestStatus> statuses) {
        return this.sessMap.get(sess).get(sess.getRecordType()).notifyDigestResponse(sess, statuses);
    }
}
