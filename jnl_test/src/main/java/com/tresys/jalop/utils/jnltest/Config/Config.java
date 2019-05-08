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
package com.tresys.jalop.utils.jnltest.Config;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.beepcore.beep.profile.ProfileConfiguration;
import org.beepcore.beep.profile.tls.jsse.TLSProfileJSSE;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.google.common.net.InetAddresses;
import com.tresys.jalop.jnl.RecordType;
import com.tresys.jalop.jnl.Role;

/**
 * The {@link Config} class is used to to parse a configuration file, and
 * configure the JNLTest program.
 */
public class Config extends BaseConfig {
	private static final String BEEP_ACTION = "beepAction";
	private static final String DATA_CLASS = "dataClass";

    private ProfileConfiguration sslConfig;
    private Map<InetAddress, PeerConfig> peerConfigs;
    private final Set<RecordType> recordTypes;

    /**
     * Parses a configuration file for use by the JNLTest program.
     * @param path
     *            The path to a file to use as the configuration.
     * @return The {@link Config}
     * @throws ParseException
     *             If there is a problem parsing the config file.
     * @throws IOException
     *             If there is a problem reading the config file.
     * @throws ConfigurationException
     */
    public static Config parse(final String path) throws IOException,
            ParseException, ConfigurationException {
        final FileReader fr = new FileReader(new File(path));
        final JSONParser jsonParser = new JSONParser();
        final Object o = jsonParser.parse(fr);
        JSONObject parsedConfig;
        parsedConfig = asJsonObject(path, null, o);
        return createFromJson(path, parsedConfig);
    }


    /**
     * Method to create a {@link Config} from a {@link JSONObject}
     *
     * @param cfgFile
     *            An identifier for the configuration file (i.e. the path).
     * @param parsedConfig
     *            The root level {@link JSONObject} for the configuration file.
     * @return A {@link Config} object.
     * @throws ConfigurationException
     */
    public static Config createFromJson(final String cfgFile,
            final JSONObject parsedConfig) throws ConfigurationException {
        final Config config = new Config(cfgFile);
        config.handleCommon(parsedConfig);

        final Object subscriber = parsedConfig.get(SUBSCRIBER);
        final Object publisher = parsedConfig.get(PUBLISHER);
        final Object listener = parsedConfig.get(LISTENER);
        final String exceptionMsg = new StringBuilder().append("Must specify one of '")
            .append(SUBSCRIBER).append("', '")
            .append(PUBLISHER).append("', ")
            .append(LISTENER).append('\'').toString();
        if (subscriber != null) {
            if (publisher != null || listener != null) {
                throw new ConfigurationException(cfgFile, exceptionMsg);
            }
            config.handleSubscriber(asJsonObject(cfgFile, SUBSCRIBER, subscriber));
        } else if (publisher != null) {
            if (listener != null) {
                throw new ConfigurationException(cfgFile, exceptionMsg);
            }
            config.handlePublisher(asJsonObject(cfgFile, PUBLISHER, publisher));
        } else if (listener != null) {
            config.handleListener(asJsonObject(cfgFile, LISTENER, listener));
        } else {
            throw new ConfigurationException(cfgFile, exceptionMsg);
        }

        return config;
    }

	/**
	 * Create a new {@link Config} object.
	 *
	 * @param source
	 *            This string will be used in generated errors to indicate what
	 *            resources caused a problem.
	 */
	Config(final String source) {
	    super(source);

	    this.peerConfigs = new HashMap<InetAddress, PeerConfig>();
	    this.recordTypes = new HashSet<RecordType>();
	}

    /**
     * Set the peer configurations.
     *
     * @param peerConfigs
     *            a {@link Map} of {@link InetAddress} to {@link PeerConfig}
     *            objects indicating what records each remote is allowed to
     *            publish/subscribe to.
     */
    public void setPeerConfigs(final Map<InetAddress, PeerConfig> peerConfigs) {
        this.peerConfigs = peerConfigs;
    }

    /**
     * Helper utility to append permissions to the {@link PeerConfig} map.
     *
     * @param elm
     *            A "PeerConfig" object.
     * @throws ConfigurationException
     *             If an problem is detected in the configuration file.
     */
    void updateKnownHosts(final JSONObject elm) throws ConfigurationException {
        final JSONArray hosts = itemAsArray(HOSTS, elm, false);
        final JSONArray pubAllowArray = itemAsArray(PUBLISH_ALLOW, elm, false);
        final Set<RecordType> pubAllow = recordSetFromArray(pubAllowArray);
        final JSONArray subAllowArray = itemAsArray(SUBSCRIBE_ALLOW, elm, false);
        final Set<RecordType> subAllow = recordSetFromArray(subAllowArray);

        for (final Object o : hosts) {
            String host;
            host = asStringValue(this.source, null, o);
            final InetAddress hostAddress = InetAddresses.forString(host);
            PeerConfig pc;
            if (this.peerConfigs.containsKey(hostAddress)) {
                pc = this.peerConfigs.get(hostAddress);
            } else {
                pc = new PeerConfig();
                this.peerConfigs.put(hostAddress, pc);
            }
            pc.getSubscribeAllow().addAll(subAllow);
            pc.getPublishAllow().addAll(pubAllow);
        }
    }

    /**
     * Obtains the {@link PeerConfig} map for this configuration.
     *
     * @return The a {@link Map} of {@link PeerConfig} objects.
     */
    public Map<InetAddress, PeerConfig> getPeerConfigs() {
        return this.peerConfigs;
    }

	/**
	 * Helper utility to handle the common configuration keys.
	 *
	 * @param obj
	 *            The context to lookup keys in.
	 * @throws ConfigurationException
	 *             If an error is detected in the configuration.
	 */
	void handleCommon(final JSONObject obj) throws ConfigurationException {
		handleAddress(obj);
		handleDataClass(obj);
		setPort(itemAsNumber(PORT, obj).intValue());
		obj.get("ssl");
		JSONObject ssl = asJsonObject(this.source, "ssl", obj.get("ssl"), false);
		if (ssl != null) {
		    handleSslConfig(ssl);
		}
	}

	/**
	 * Retrieve the {@link ProfileConfiguration}, if any, for setting up SSL.
	 * The object returned by this function should be passed to the
	 * {@link TLSProfileJSSE#init(String, ProfileConfiguration)} function to
	 * finish the SSL configuration.
	 * @return The {@link ProfileConfiguration} for SSL.
	 */
	public ProfileConfiguration getSslConfiguration() {
	    return this.sslConfig;
	}

	/**
	 * Helper utility to process the remainder of a configuration as a
	 * 'listener'.
	 *
	 * @param obj
	 *            The context to look up keys in.
	 * @throws ConfigurationException
	 *             If an error is detected in the configuration.
	 */
    void handleListener(final JSONObject obj) throws ConfigurationException {
	    this.listener = true;
		setPendingDigestMax(itemAsNumber(PENDING_DGST_MAX, obj).intValue());
		setPendingDigestTimeout(itemAsNumber(PENDING_DGST_TIMEOUT, obj)
				.intValue());
		setInputPath(new File(itemAsString(INPUT, obj, true)));
		setOutputPath(new File(itemAsString(OUTPUT, obj, true)));
		final JSONArray peers = itemAsArray(PEERS, obj);
		for (final Object o : peers) {
			final JSONObject elm = asJsonObject(this.source, null, o);
			updateKnownHosts(elm);
		}
	}

    /**
     * Handle parsing the dataClass field for a publisher or listener.
     * @param obj
     *            The context to look up keys in.
     * @throws ConfigurationException
     *            If an error is detected in the configuration.
     */
    void handleDataClass(final JSONObject obj) throws ConfigurationException {
        final JSONArray dataClasses = itemAsArray(DATA_CLASS, obj);
        for (final Object o : dataClasses) {
            this.recordTypes.add(objectToRecordType(o));
        }
    }

	/**
	 * Helper utility to process a segment of the configuration as a
	 * 'publisher'.
	 *
	 * @param publisher
	 *            The context to look up keys in.
	 * @throws ConfigurationException
	 *             If an error is detected in the configuration.
	 */
    void handlePublisher(final JSONObject publisher)
			throws ConfigurationException {
	    this.listener = false;
        handleSessionTimeout(publisher);
        handleDataClass(publisher);
		setRole(Role.Publisher);
		setInputPath(new File(itemAsString(INPUT, publisher, true)));
		setMode(itemAsString(MODE, publisher, true));
	}


	/**
	 * Returns whether or not this Config specifies a listener, or connector.
	 *
	 * @return <code>true</code> if the configuration indicates to a listener.
	 * <code>false</code> if the configuration specifies a connection should be
	 * initiated.
	 */
    public boolean isListener() {
		return this.listener;
	}

    public Set<RecordType> getRecordTypes()
    {
        return this.recordTypes;
    }

	/**
	 * Build a structure suitable to pass into the beepcore framework for SSL.
	 * @param ssl The JSON object that contains all the keys to configure SSL.
	 *
	 * These keys are passed directly to the
	 * {@link TLSProfileJSSE#init(String, ProfileConfiguration)}, so any keys
	 * recognized by that class are valid here.
	 *
	 * @see TLSProfileJSSE#init(String, ProfileConfiguration)
	 */
    @SuppressWarnings("rawtypes") // because the JSON map doesn't use generics
    void handleSslConfig(JSONObject ssl) {
	    this.sslConfig = new ProfileConfiguration();
       Iterator iter = ssl.entrySet().iterator();
	   while (iter.hasNext()) {
	       Entry e = (Entry) iter.next();
	       this.sslConfig.setProperty(e.getKey().toString(),
	                                  e.getValue().toString());
	    }
	}
}
