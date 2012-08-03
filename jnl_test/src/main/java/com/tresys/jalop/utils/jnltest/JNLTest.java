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
import java.util.HashMap;
import java.util.Map;

import javax.xml.soap.MimeHeaders;

import org.apache.log4j.Logger;
import org.beepcore.beep.core.BEEPException;
import org.json.simple.parser.ParseException;

import com.tresys.jalop.jnl.DigestPair;
import com.tresys.jalop.jnl.DigestStatus;
import com.tresys.jalop.jnl.Publisher;
import com.tresys.jalop.jnl.PublisherSession;
import com.tresys.jalop.jnl.RecordInfo;
import com.tresys.jalop.jnl.RecordType;
import com.tresys.jalop.jnl.Role;
import com.tresys.jalop.jnl.Session;
import com.tresys.jalop.jnl.SourceRecord;
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
public class JNLTest implements Subscriber, Publisher {
    /** Logger for this class */
    private static final Logger logger = Logger.getLogger(JNLTest.class);
	/**
	 * Configuration for this instance of JNLTest.
	 */
	private final Config config;
	/**
	 * From Sessions to associated {@link SubscriberImpl}
	 */
	private final Map<Session, Map<RecordType, SubscriberImpl>> sessMap = new HashMap<Session, Map<RecordType,SubscriberImpl>>();
	/**
	 * From Sessions to associated {@link PublisherImpl}
	 */
	private final Map<Session, Map<RecordType, PublisherImpl>> pubSessMap = new HashMap<Session, Map<RecordType,PublisherImpl>>();
	/**
	 * Create a JNLTest object based on the specified configuration.
	 *
	 * @param config
	 *            A {@link Config}
	 */
	public JNLTest(final Config config) {
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
	public static void main(final String[] args) throws JNLException, BEEPException {
		if (args.length != 1) {
			System.err.println("Must specify exactly one argument that is "
					+ " the configuration file to use");
            System.exit(1);
		}
		Config config;
		try {
			config = Config.parse(args[0]);
		} catch (final IOException e) {
			System.err.println("Caught IO exception: " + e.getMessage());
			System.exit(1);
			throw new RuntimeException("Failed to call exit()");
		} catch (final ParseException e) {
			System.err.print(e.toString());
			System.exit(1);
			throw new RuntimeException("Failed to call exit()");
		} catch (final ConfigurationException e) {
			System.err.println("Exception processing the config file: "
					+ e.getMessage());
			System.exit(1);
			throw new RuntimeException("Failed to call exit()");
		}
		final JNLTest jt = new JNLTest(config);
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
                final ContextImpl contextImpl = new ContextImpl(null, this, null, this.config.getPendingDigestTimeout(), config.getPendingDigestMax(), "agent", null, null, config.getSslConfiguration());
				contextImpl.subscribe(this.config.getAddress(), this.config.getPort(), this.config.getRecordTypes().toArray(new RecordType[0]));

            } else if (this.config.getRole() == Role.Publisher) {
				final ContextImpl contextImpl = new ContextImpl(this, null, null, this.config.getPendingDigestTimeout(), config.getPendingDigestMax(), "agent", null, null, config.getSslConfiguration());
				contextImpl.publish(this.config.getAddress(), this.config.getPort(), this.config.getRecordTypes().toArray(new RecordType[0]));
            }
            this.logger.info("Waiting: " + config.getSessionTimeout());
            synchronized(this) {
                try {
                    this.wait(config.getSessionTimeout().getTime());
                } catch (final InterruptedException e) {
                    this.logger.info("Someone woke us up");
                }
            }
        } else {
            // TODO: do things as a listener
            this.logger.error("Acting as listener not currently supported");
        }
    }

    @Override
    public SubscribeRequest getSubscribeRequest(final SubscriberSession sess) {
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
    public boolean notifySysMetadata(final SubscriberSession sess, final RecordInfo recordInfo, final InputStream sysMetaData) {
        return this.sessMap.get(sess).get(sess.getRecordType()).notifySysMetadata(sess, recordInfo, sysMetaData);
    }

    @Override
    public boolean notifyAppMetadata(final SubscriberSession sess, final RecordInfo recordInfo, final InputStream appMetaData) {
        return this.sessMap.get(sess).get(sess.getRecordType()).notifyAppMetadata(sess, recordInfo, appMetaData);
    }

    @Override
    public boolean notifyPayload(final SubscriberSession sess, final RecordInfo recordInfo, final InputStream payload) {
        return this.sessMap.get(sess).get(sess.getRecordType()).notifyPayload(sess, recordInfo, payload);
    }

    @Override
    public boolean notifyDigest(final SubscriberSession sess, final RecordInfo recordInfo, final byte[] digest) {
        return this.sessMap.get(sess).get(sess.getRecordType()).notifyDigest(sess, recordInfo, digest);
    }

    @Override
    public boolean notifyDigestResponse(final SubscriberSession sess, final Map<String, DigestStatus> statuses) {
        return this.sessMap.get(sess).get(sess.getRecordType()).notifyDigestResponse(sess, statuses);
    }

	@Override
	public SourceRecord getNextRecord(final PublisherSession sess, final String lastSerialId) {
		return this.pubSessMap.get(sess).get(sess.getRecordType()).getNextRecord(sess, lastSerialId);
	}

	@Override
	public SourceRecord onJournalResume(final PublisherSession sess, final String serialId,
			final long offset, final MimeHeaders headers) {
		setPubMap(sess);
		return this.pubSessMap.get(sess).get(sess.getRecordType()).onJournalResume(sess, serialId, offset, headers);
	}

	@Override
	public boolean onSubscribe(final PublisherSession sess, final String serialId,
			final MimeHeaders headers) {

		setPubMap(sess);
        return this.pubSessMap.get(sess).get(sess.getRecordType()).onSubscribe(sess, serialId, headers);
	}

	private void setPubMap(final PublisherSession sess) {

		// TODO: All the code here to manage the maps should really be happening in the
        // connection handler callbacks, but the library isn't generating those events
        // quite yet.
        Map<RecordType, PublisherImpl> map;
        synchronized (this.pubSessMap) {
            map = this.pubSessMap.get(sess);
            if (map == null) {
                map = new HashMap<RecordType, PublisherImpl>();
                this.pubSessMap.put(sess, map);
            }
        }

        synchronized(map) {
            if (map.get(sess.getRecordType()) == null) {
                map.put(sess.getRecordType(), new PublisherImpl(
						this.config.getInputPath(), sess.getRecordType()));
            }
        }
	}

	@Override
	public boolean onRecordComplete(final PublisherSession sess, final String serailId,
			final SourceRecord record) {
		return this.pubSessMap.get(sess).get(sess.getRecordType()).onRecordComplete(sess, serailId, record);
	}

	@Override
	public boolean sync(final PublisherSession sess, final String serialId,
			final MimeHeaders headers) {
		return this.pubSessMap.get(sess).get(sess.getRecordType()).sync(sess, serialId, headers);
	}

	@Override
	public void notifyDigest(final PublisherSession sess, final String serialId,
			final byte[] digest) {
		this.pubSessMap.get(sess).get(sess.getRecordType()).notifyDigest(sess, serialId, digest);
	}

	@Override
	public void notifyPeerDigest(final PublisherSession sess,
			final Map<String, DigestPair> digestPairs) {
		this.pubSessMap.get(sess).get(sess.getRecordType()).notifyPeerDigest(sess, digestPairs);
	}
}
