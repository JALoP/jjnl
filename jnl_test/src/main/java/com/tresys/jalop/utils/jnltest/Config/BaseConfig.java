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
import java.net.InetAddress;
import java.util.HashSet;
import java.util.Set;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.google.common.net.InetAddresses;
import com.tresys.jalop.jnl.Mode;
import com.tresys.jalop.jnl.RecordType;
import com.tresys.jalop.jnl.Role;

/**
 * The {@link BaseConfig} class is used to to parse a configuration file, and
 * configure the JNLTest program.
 */
public class BaseConfig {
    private static final String ADDRESS = "address";
    private static final String AUDIT = "audit";

    private static final String CONNECT = "connect";
    private static final String DATA_CLASS = "dataClass";
    protected static final String HOSTS = "hosts";
    protected static final String INPUT = "input";
    private static final String JOURNAL = "journal";
    protected static final String LISTENER = "listener";
    private static final String LOG = "log";
    protected static final String OUTPUT = "output";
    protected static final String MODE = "mode";
    private static final String MODE_ARCHIVE = "archive";
    private static final String MODE_LIVE = "live";
    protected static final String PEERS = "peers";
    protected static final String PENDING_DGST_MAX = "pendingDigestMax";
    protected static final String PENDING_DGST_TIMEOUT = "pendingDigestTimeout";
    protected static final String PORT = "port";
    protected static final String PUBLISH_ALLOW = "publishAllow";
    protected static final String PUBLISHER = "publisher";
    private static final String SESSION_TIMEOUT = "sessionTimeout";
    protected static final String SUBSCRIBE_ALLOW = "subscribeAllow";
    protected static final String SUBSCRIBER = "subscriber";

    protected InetAddress address;
    protected boolean listener;
    private File inputPath;
    private File outputPath;

    private int pendingDigestMax;
    private int pendingDigestTimeout;
    private int port;
    private final Set<RecordType> recordTypes;
    private Mode mode;
    private Role role;
    private long sessionTimeout;
    protected final String source;

    /**
     * Create a new {@link BaseConfig} object.
     *
     * @param source
     *            This string will be used in generated errors to indicate what
     *            resources caused a problem.
     */
    BaseConfig(final String source) {
        this.source = source;
        this.recordTypes = new HashSet<RecordType>();
        this.pendingDigestMax = -1;
        this.pendingDigestTimeout = -1;
        this.port = -1;
        this.mode = Mode.Unset;
    }

    /**
     * Get the IP address.
     *
     * @return The {@link InetAddress}. Currently on IPv4 addresses are
     *         supported.
     */
    public InetAddress getAddress() {
        return this.address;
    }

    /**
     * Retrieve the directory path to use when acting as publisher.
     *
     * @return The directory specified for where to obtain records from.
     */
    public File getInputPath() {
        return this.inputPath;
    }

    /**
     * Retrieve the directory path to use when acting as a subscriber.
     *
     * @return The directory specified for store records into.
     */
    public File getOutputPath() {
        return this.outputPath;
    }

    /**
     * Obtain the indicated maximum number of digests to calculate before
     * sending a "digest" message. This maximum is a per session maximum.
     *
     * @return The maximum number of digests to calculate before sending a
     *         "digest" message.
     */
    public int getPendingDigestMax() {
        return this.pendingDigestMax;
    }

    /**
     * Obtain the indicated number of seconds to wait before sending a "digest"
     * message.
     *
     * @return The maximum number of seconds to wait before sending a digest
     *         message.
     */
    public int getPendingDigestTimeout() {
        return this.pendingDigestTimeout;
    }

    /**
     * Get the port to listen on (for listeners) or connect to (for connector).
     *
     * @return The port
     * @see Config#isListener()
     */
    public int getPort() {
        return this.port;
    }

    /**
     * Obtain the set of record types to subscribe/publish. This is not
     * applicable for a listener.
     *
     * @return The JALoP records that should be transfered using this
     *         connection.
     * @see Config#isListener()
     */
    public Set<RecordType> getRecordTypes() {
        return this.recordTypes;
    }

    /**
     * Obtain the indicated role, {@link Role#Publisher} or
     * {@link Role#Subscriber}. This is not applicable for listeners.
     *
     * @see Config#isListener()
     * @return The designated role.
     */
    public Role getRole() {
        return this.role;
    }

    /**
     * Obtain the indicated role, {@link Mode#Live} or
     * {@link Mode#Archive}. This is not applicable for listeners.
     *
     * @see Config#isListener()
     * @return The designated mode.
     */
    public Mode getMode() {
        return this.mode;
    }

    /**
     * Obtain the session timeout. The session timeout indicates how long an
     * connector should wait (once the connection is established) before
     * disconnecting from a remote.
     *
     * @return time to wait in milliseconds
     */

    public long getSessionTimeout() {
        return this.sessionTimeout;
    }

    /**
     * Obtain the identifier (i.e. path) of the configuration used to create
     * this {@link BaseConfig} object.
     *
     * @see Config#Config(String)
     * @return The source identifier.
     */
    public String getSource() {
        return this.source;
    }

    /**
     * Get IP address from the {@link JSONObject}. This expects there to be a
     * key with the name "address" in the {@link JSONObject} obj.
     *
     * @param obj
     *            The context to look in.
     * @throws ConfigurationException
     *             If 'address' is not found.
     */
    void handleAddress(final JSONObject obj) throws ConfigurationException {
        final String addrString = itemAsString(ADDRESS, obj);
        this.address = InetAddresses.forString(addrString);
    }


    /**
     * Handle common configuration keys for connectors.
     *
     * @param obj
     *            The context to look up keys in.
     * @throws ConfigurationException
     *             If an error is detected in the configuration.
     */
    void handleSessionTimeout(final JSONObject obj)
            throws ConfigurationException {
        final String sessionTimeout = itemAsString(SESSION_TIMEOUT, obj);
        try {
            String[] tokens = sessionTimeout.split(":");
            if (tokens.length != 3) {
                throw new Exception("invalid session timeout");
            }
            int hours = Integer.parseInt(tokens[0]);
            int minutes = Integer.parseInt(tokens[1]);
            int seconds = Integer.parseInt(tokens[2]);

            // convert hours->seconds, minutes->seconds, and add
            long timeout = (hours * 60 * 60) + (minutes * 60) + seconds;
            // convert from seconds to milliseconds
            setSessionTimeout(timeout * 1000);
        } catch (final Exception e) {
            throw new ConfigurationException(this.source, "Bad format for '"
                    + SESSION_TIMEOUT + "' (" + sessionTimeout + ") "
                    + e.toString());
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
     * 'subscriber'.
     *
     * @param subscriber
     *            The context to look up keys in.
     * @throws ConfigurationException
     *             If an error is detected in the configuration.
     */
    void handleSubscriber(final JSONObject subscriber)
            throws ConfigurationException {
        this.listener = false;
        setRole(Role.Subscriber);
        handleSessionTimeout(subscriber);
        handleDataClass(subscriber);
        setOutputPath(new File(itemAsString(OUTPUT, subscriber, true)));
        setPendingDigestMax(itemAsNumber(PENDING_DGST_MAX, subscriber)
                .intValue());
        setPendingDigestTimeout(itemAsNumber(PENDING_DGST_TIMEOUT, subscriber)
                .intValue());
        setMode(itemAsString(MODE, subscriber, true));
    }

    /**
     * Lookup the required element named by 'key' in the {@link JSONObject} obj.
     *
     *
     * @see Config#itemAsArray(String, JSONObject, boolean)
     * @param key
     *            The name of the element to look up.
     * @param obj
     *            The {@link JSONObject} to find the key in.
     * @return The {@link JSONArray} for the key, or NULL if none exists.
     * @throws ConfigurationException
     */
    JSONArray itemAsArray(final String key, final JSONObject obj)
            throws ConfigurationException {
        return itemAsArray(key, obj, true);
    }

    /**
     * Simple helper function to retrieve an element as a {@link JSONArray}. If
     * the element is not an array, this function throws an appropriate
     * {@link ConfigurationException}.
     *
     * @param key
     *            The name of the element to look up.
     * @param obj
     *            The {@link JSONObject} to find the key in.
     * @param required
     *            If this is set to true, a {@link ConfigurationException} is
     *            thrown if the key is not found.
     * @return The {@link JSONArray} for the key, or NULL if none exists.
     * @throws ConfigurationException
     */
    JSONArray itemAsArray(final String key, final JSONObject obj,
            final boolean required) throws ConfigurationException {
        final Object o = obj.get(key);

        if (!required && (o == null)) {
            return (JSONArray) o;
        }
        return asJsonArray(this.source, key, o);
    }

    /**
     * Look up the required key as a Number in the {@link JSONObject} obj.
     *
     * @see Config#itemAsNumber(String, JSONObject, boolean)
     *
     * @param key
     *            The key to look up.
     * @param obj
     *            The {@link JSONObject} to find the key in.
     * @return The value of key as a {@link Number}.
     * @throws ConfigurationException
     */
    Number itemAsNumber(final String key, final JSONObject obj)
            throws ConfigurationException {
        return itemAsNumber(key, obj, true);
    }

    /**
     * Simple helper function to retrieve an element as a {@link Number}. If the
     * element is not a number, this function throws an appropriate
     * {@link ConfigurationException}.
     *
     * @param key
     *            The name of the element to look up.
     * @param obj
     *            The {@link JSONObject} to find the key in.
     * @param required
     *            If this is set to true, a {@link ConfigurationException} is
     *            thrown if the key is not found.
     * @return The {@link JSONArray} for the key, or NULL if none exists.
     * @throws ConfigurationException
     */
    Number itemAsNumber(final String key, final JSONObject obj,
            final boolean required) throws ConfigurationException {
        final Object o = obj.get(key);
        if (!required && (o == null)) {
            return (Number) o;
        }
        return asNumberValue(this.source, key, o);
    }

    /**
     * Look up the required key in the {@link JSONObject} obj.
     *
     * @param key
     *            The key to look up.
     * @param obj
     *            The {@link JSONObject} to find the key in.
     * @return The value for key.
     * @throws ConfigurationException
     * @see {@link Config#itemAsString(String, JSONObject, boolean)}
     */
    String itemAsString(final String key, final JSONObject obj)
            throws ConfigurationException {
        return itemAsString(key, obj, true);
    }

    /**
     * Simple helper function to retrieve an element as a {@link String}. If the
     * element is not a string, this function throws an appropriate
     * {@link ConfigurationException}.
     *
     * @param key
     *            The name of the element to look up.
     * @param obj
     *            The {@link JSONObject} to find the key in.
     * @param required
     *            If this is set to true, a {@link ConfigurationException} is
     *            thrown if the key is not found.
     * @return The value for the key, or NULL if none exists.
     * @throws ConfigurationException
     */
    String itemAsString(final String key, final JSONObject obj,
            final boolean required) throws ConfigurationException {
        final Object o = obj.get(key);
        if (!required && (o == null)) {
            return (String) o;
        }
        return asStringValue(this.source, key, o);
    }

    /**
     * Helper utility to create a {@link RecordType} from a JSON Object.
     *
     * @param o
     *            The object to convert to a {@link RecordType}. This is
     *            expected to be a value from a {@link JSONObject} or
     *            {@link JSONArray}.
     * @return The {@link RecordType}
     * @throws ConfigurationException
     *             If there is a problem converting to a {@link RecordType}
     */
    RecordType objectToRecordType(final Object o) throws ConfigurationException {
        final String dataClass = asStringValue(this.source, null, o);
        if (JOURNAL.equals(dataClass)) {
            return RecordType.Journal;
        } else if (AUDIT.equals(dataClass)) {
            return RecordType.Audit;
        } else if (LOG.equals(dataClass)) {
            return RecordType.Log;
        }
        throw new ConfigurationException(this.source, "Expected '" + DATA_CLASS
                + "' to be one of '" + JOURNAL + "', '" + AUDIT + "', or '"
                + LOG + "' (found '" + dataClass + "'.");
    }

    /**
     * Take a JSON array of strings and convert it to a set of
     * {@link RecordType}.
     *
     * @param recordTypesArr
     * @return A {@link Set} containing all the {@link RecordType} indicated in
     *         recordTypesArr.
     * @throws ConfigurationException
     *             If there is an error converting the values to a
     *             {@link RecordType}s.
     */
    Set<RecordType> recordSetFromArray(final JSONArray recordTypesArr)
            throws ConfigurationException {
        final Set<RecordType> rtSet = new HashSet<RecordType>();
        if (recordTypesArr != null) {
            for (final Object o : recordTypesArr) {
                final RecordType rt = objectToRecordType(o);
                rtSet.add(rt);
            }
        }
        return rtSet;
    }

    /**
     * Set the address for this {@link Config}.
     *
     * @param address
     *            The address.
     */
    public void setAddress(final InetAddress address) {
        this.address = address;
    }

    /**
     * Configure this to initiate a connection, or listen for connections.
     *
     * @param connect
     *            if set to true, specifies that a connection should be
     *            initiated. If set to false, specifies to listen for
     *            connections.
     */
    public void setConnect(final boolean connect) {
        this.listener = connect;
    }

    /**
     * Set the path to a directory to user for input. Not applicable to a
     * "Subscriber".
     *
     * @param inputPath
     *            The path to get records from.
     */
    public void setInputPath(final File inputPath) {
        this.inputPath = inputPath;
    }

    /**
     * Set the path to store records from remotes in. Not applicable to a
     * "Publisher".
     *
     * @param outputPath
     */
    public void setOutputPath(final File outputPath) {
        this.outputPath = outputPath;
    }

    /**
     * Set the maximum number of digests to store before sending a "digest"
     * message.
     *
     * @param pendingDigestMax
     *            The number of digests to calculate before sending a "digest"
     *            message.
     */
    public void setPendingDigestMax(final int pendingDigestMax) {
        this.pendingDigestMax = pendingDigestMax;
    }

    /**
     * Set the maximum number of seconds to wait before sending a "digest"
     * message.
     *
     * @param pendingDigestTimeout
     *            The time to wait, in seconds.
     */
    public void setPendingDigestTimeout(final int pendingDigestTimeout) {
        this.pendingDigestTimeout = pendingDigestTimeout;
    }

    /**
     * Set the port to connect to/listen on.
     *
     * @param port
     *            The port number.
     */
    public void setPort(final int port) {
        this.port = port;
    }

    /**
     * Set the role. This is only applicable for connectors (i.e.
     * {@link Config#isListener()} returns <code>false</code>).
     *
     * @param role
     *            The role.
     */
    public void setRole(final Role role) {
        this.role = role;
    }

    /**
     * Set the mode. This is only applicable for connectors (i.e.
     * {@link Config#isListener()} returns <code>false</code>).
     *
     * @param mode
     *            The mode.
     */
    public void setMode(final String mode)
            throws ConfigurationException {
        if (mode.equalsIgnoreCase(MODE_LIVE)) {
            this.mode = Mode.Live;
        } else if (mode.equalsIgnoreCase(MODE_ARCHIVE)) {
            this.mode = Mode.Archive;
        } else {
            throw new ConfigurationException(this.source,
                "Expected '" + MODE_LIVE + " or " + MODE_ARCHIVE);
        }
    }

    /**
     * Configure the session timeout. This is only applicable for connectors
     * (i.e. {@link Config#isListener()} returns <code>false</code>).
     *
     * @param sessionTimeout
     */
    public void setSessionTimeout(final long sessionTimeout) {
        this.sessionTimeout = sessionTimeout;
    }

    /**
     * Helper utility to cast a {@link Object} as a {@link String}.
     *
     * @param path
     *            The file where the error occurred.
     * @param key
     *            The key (if any) for <code>o</code>.
     * @param o
     *            The {@link Object} to cast.
     * @return <code>o</code> cast as a {@link String}.
     * @throws ConfigurationException
     *             if <code>o</code> is <code>null</code> or not a
     *             {@link String}.
     */
    static String asStringValue(final String path, final String key, final Object o)
            throws ConfigurationException {
        if (o instanceof String) {
            return (String) o;
        }
        if (key != null) {
            throw new ConfigurationException(path, "Expected String for '"
                    + key + "', found '" + o + "'");
        } else {
            throw new ConfigurationException(path, "Expected String, found '"
                    + o + "'");
        }
    }

    /**
     * Helper utility to cast a {@link Object} as a {@link Number}.
     *
     * @param path
     *            The file where the error occurred.
     * @param key
     *            The key (if any) for <code>o</code>.
     * @param o
     *            The {@link Object} to cast.
     * @return <code>o</code> cast as a {@link Number}.
     * @throws ConfigurationException
     *             if <code>o</code> is <code>null</code> or not a
     *             {@link Number}.
     */
    static Number asNumberValue(final String path, final String key, final Object o)
            throws ConfigurationException {
        if (o instanceof Number) {
            return (Number) o;
        }
        if (key != null) {
            throw new ConfigurationException(path, "Expected Number for '"
                    + key + "', found '" + o + "'");
        } else {
            throw new ConfigurationException(path, "Expected Number, found '"
                    + o + "'");
        }
    }

    /**
     * Helper utility to cast an {@link Object} as a {@link JSONObject}.
     *
     * @param path
     *            The file where the error occurred.
     * @param key
     *            The key (if any) for <code>o</code>.
     * @param o
     *            The {@link Object} to cast
     * @return <code>o</code> cast as a {@link JSONObject}.
     * @throws ConfigurationException
     *             if <code>o</code> is <code>null</code> or not a
     *             {@link JSONObject}.
     */
    static JSONObject asJsonObject(final String path, final String key, final Object o)
            throws ConfigurationException {
        return asJsonObject(path, key, o, true);
    }
    /**
     * Helper utility to cast an {@link Object} as a {@link JSONObject}.
     *
     * @param path
     *            The file where the error occurred.
     * @param key
     *            The key (if any) for <code>o</code>.
     * @param o
     *            The {@link Object} to cast
     * @return <code>o</code> cast as a {@link JSONObject}.
     * @throws ConfigurationException
     *             if <code>o</code> is <code>null</code> or not a
     *             {@link JSONObject}.
     */
    static JSONObject asJsonObject(String path, String key, Object o,
            boolean required)  throws ConfigurationException {
        if (o instanceof JSONObject) {
            return (JSONObject) o;
        }
        if (key != null && o != null) {
            throw new ConfigurationException(path, "Expected JSON Object for '"
                    + key + "', found '" + o + "'");
        } else if (required) {
            throw new ConfigurationException(path,
                    "Expected JSON Object, found '" + o + "'");
        }
        return null;
    }
    /**
     * Helper utility to cast a value to a {@link JSONArray}.
     *
     * @param path
     *            The file where the error occurred.
     * @param key
     *            The key (if any) for <code>o</code>.
     * @param o
     *            The {@link Object} to cast.
     * @return <code>o</code> cast as a {@link JSONArray}
     * @throws ConfigurationException
     *             if <code>o</code> is <code>null</code> or not a
     *             {@link JSONArray}.
     */
    static JSONArray asJsonArray(final String path, final String key, final Object o)
            throws ConfigurationException {
        if (o instanceof JSONArray) {
            return (JSONArray) o;
        }
        if (key != null) {
            throw new ConfigurationException(path, "Expected JSON Array for '"
                    + key + "', found '" + o + "'");
        } else {
            throw new ConfigurationException(path,
                    "Expected JSON Array, found '" + o + "'");
        }
    }
}
