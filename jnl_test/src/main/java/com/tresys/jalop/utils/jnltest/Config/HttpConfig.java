/*
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.google.common.net.InetAddresses;
import com.tresys.jalop.jnl.Mode;
import com.tresys.jalop.jnl.RecordType;
import com.tresys.jalop.jnl.Role;
import com.tresys.jalop.jnl.impl.http.HttpSubscriberConfig;
import com.tresys.jalop.jnl.impl.http.HttpUtils;

/**
 * The {@link HttpConfig} class is used to to parse a configuration file, and
 * configure the JNLTest program.
 */
public class HttpConfig {
    private static final String ADDRESS = "address";
    private static final String AUDIT = "audit";
    private static final String CONFIGURE_DIGEST = "configureDigest";
    private static final String CONFIGURE_TLS = "configureTls";
    private static final String TEST_MODE = "testMode";
    protected static final String HOSTS = "hosts";
    protected static final String INPUT = "input";
    private static final String JOURNAL = "journal";
    private static final String KEY_STORE_PASSPHRASE = "Key Store Passphrase";
    private static final String KEY_STORE = "Key Store";
    protected static final String LISTENER = "listener";
    private static final String LOG = "log";
    protected static final String OUTPUT = "output";
    private static final String MAX_SESSION_LIMIT = "maxSessionLimit";
    protected static final String MODE = "mode";
    private static final String MODE_ARCHIVE = "archive";
    private static final String MODE_LIVE = "live";
    protected static final String PORT = "port";
    protected static final String PUBLISHER = "publisher";
    private static final String RECORD_TYPE = "recordType";
    protected static final String SUBSCRIBER = "subscriber";

    protected InetAddress address;
    private List<String> configureDigests;
    private String configureTls;
    private String testMode;
    private int maxSessionLimit;
    private Mode mode;
    private File outputPath;
    private int port;
    private final Set<RecordType> recordTypes;
    private Role role;
    protected final String source;
    private HashMap<String, String> sslConfig;

    /**
     * Parses a configuration file for use by the JNLTest program.
     * @param path
     *            The path to a file to use as the configuration.
     * @return The {@link HttpConfig}
     * @throws ParseException
     *             If there is a problem parsing the config file.
     * @throws IOException
     *             If there is a problem reading the config file.
     * @throws ConfigurationException
     */
    public static HttpConfig parse(final String path) throws IOException,
            ParseException, ConfigurationException {
        final FileReader fr = new FileReader(new File(path));
        final JSONParser jsonParser = new JSONParser();
        final Object o = jsonParser.parse(fr);
        JSONObject parsedConfig;
        parsedConfig = asJsonObject(path, null, o);
        return createFromJson(path, parsedConfig);
    }

    /**
     * Method to create a {@link HttpConfig} from a {@link JSONObject}
     *
     * @param cfgFile
     *            An identifier for the configuration file (i.e. the path).
     * @param parsedConfig
     *            The root level {@link JSONObject} for the configuration file.
     * @return A {@link HttpConfig} object.
     * @throws ConfigurationException
     */
    public static HttpConfig createFromJson(final String cfgFile,
            final JSONObject parsedConfig) throws ConfigurationException {
        final HttpConfig config = new HttpConfig(cfgFile);
        config.handleCommon(parsedConfig);

        final Object subscriber = parsedConfig.get(SUBSCRIBER);
        final String exceptionMsg = new StringBuilder().append("Must be only '")
            .append(SUBSCRIBER).toString();
        if (subscriber != null) {
            config.handleSubscriber(asJsonObject(cfgFile, SUBSCRIBER, subscriber));
        } else {
            throw new ConfigurationException(cfgFile, exceptionMsg);
        }

        return config;
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
        setRole(Role.Subscriber);
        setOutputPath(new File(itemAsString(OUTPUT, subscriber, true)));
        setMode(itemAsString(MODE, subscriber, true));

        handleConfigureDigest(subscriber);
        handleTls(subscriber);
        handleTestMode(subscriber);
        handleRecordType(subscriber);
        handleMaxSessionLimit(subscriber);
    }


    /**
     * Create a new {@link HttpConfig} object.
     *
     * @param source
     *            This string will be used in generated errors to indicate what
     *            resources caused a problem.
     */
    HttpConfig(final String source) {
        this.source = source;
        this.recordTypes = new HashSet<RecordType>();
        this.maxSessionLimit = -1;
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
     * Retrieve the directory path to use when acting as a subscriber.
     *
     * @return The directory specified for store records into.
     */
    public File getOutputPath() {
        return this.outputPath;
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
     * Retrieve the {@link HashMap<String,String> }, if any, for setting up SSL.
     * The object returned by this function should be passed to the
     * @return The {@link HashMap<String,String>} for SSL.
     */
    public HashMap<String,String> getSslConfiguration() {
        return this.sslConfig;
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
        setPort(itemAsNumber(PORT, obj).intValue());
        obj.get("ssl");
        JSONObject ssl = asJsonObject(this.source, "ssl", obj.get("ssl"), false);
        if (ssl != null) {
            handleSslConfig(ssl);
        }
    }

    /**
     * Handle parsing the dataClass field for a publisher or listener.
     * @param obj
     *            The context to look up keys in.
     * @throws ConfigurationException
     *            If an error is detected in the configuration.
     */
    void handleRecordType(final JSONObject obj) throws ConfigurationException {
        final JSONArray dataClasses = itemAsArray(RECORD_TYPE, obj);
        for (final Object o : dataClasses) {
            this.recordTypes.add(objectToRecordType(o));
        }
    }

    /**
     * Handle parsing the configure digest challenge field.
     * @param obj
     *            The context to look up keys in.
     * @throws ConfigurationException
     *            If an error is detected in the configuration.
     */
    public void handleConfigureDigest(final JSONObject obj) throws ConfigurationException {
        final JSONArray configureDigestsList = itemAsArray(CONFIGURE_DIGEST, obj);
        this.configureDigests = new ArrayList<String>();
        for (final Object o : configureDigestsList) {
            this.configureDigests.add((String)o);
        }

        //Ensures that at least "on" is present
        if (this.configureDigests == null || !this.configureDigests.contains(HttpUtils.MSG_ON))
        {
           throw new ConfigurationException (this.source, HttpConfig.CONFIGURE_DIGEST + " must contain at least " + HttpUtils.MSG_ON);
        }

        //Ensures that only contains "on" and "off"
        if (this.configureDigests.size() > 1 && !this.configureDigests.contains(HttpUtils.MSG_OFF) ||
            this.configureDigests.size() > 2)
        {
            throw new ConfigurationException (this.source, HttpConfig.CONFIGURE_DIGEST + " must only contain " + HttpUtils.MSG_ON + " and/or " + HttpUtils.MSG_OFF);
        }
    }

    /**
     * Handle parsing the recordType field for a publisher or listener.
     * @param obj
     *            The context to look up keys in.
     * @throws ConfigurationException
     *            If an error is detected in the configuration.
     */
    /*void handleRecordType(final JSONObject obj) throws ConfigurationException {
        final JSONArray recordType = itemAsArray(RECORD_TYPE, obj);
        for (final Object o : recordType) {
            this.recordTypes.add(objectToRecordType(o));
        }
    }*/

    public void handleTls(final JSONObject obj) throws ConfigurationException {
        final String tlsString = itemAsString(CONFIGURE_TLS, obj);
        this.configureTls = tlsString;
        if (!this.configureTls.equals("on") && !this.configureTls.equals("off")) {
            throw new ConfigurationException (this.source, HttpConfig.CONFIGURE_TLS + " must only contain " + HttpUtils.MSG_ON + " or " + HttpUtils.MSG_OFF);
        }
    }

    public void handleTestMode(final JSONObject obj) throws ConfigurationException {
        final String testModeString = itemAsString(TEST_MODE, obj);
        this.testMode = testModeString;

        if (!this.testMode.equals("on") && !this.testMode.equals("off")) {
            throw new ConfigurationException (this.source, HttpConfig.TEST_MODE + " must only contain " + HttpUtils.MSG_ON + " or " + HttpUtils.MSG_OFF);
        }
    }

    public void handleMaxSessionLimit(final JSONObject obj) throws ConfigurationException {
        final int maxSessionLimit = itemAsNumber(MAX_SESSION_LIMIT, obj).intValue();
        this.maxSessionLimit = maxSessionLimit;
        if (this.maxSessionLimit <= 0) {
            throw new ConfigurationException (this.source, HttpConfig.MAX_SESSION_LIMIT + " must be a positive, non-zero value.");
        }
    }

    public List<String> getConfigureDigests()
    {
        return this.configureDigests;
    }

    public String getTlsConfiguration()
    {
        return this.configureTls;
    }

    public String getTestMode()
    {
        return this.testMode;
    }

    public int getMaxSessionLimit()
    {
        return this.maxSessionLimit;
    }

    public Set<RecordType> getRecordTypes()
    {
        return this.recordTypes;
    }

    /**
     * Build a structure to store the ssl configuration
     * @param ssl The JSON object that contains all the keys to configure SSL.
     *
     *
     */
    @SuppressWarnings("rawtypes") // because the JSON map doesn't use generics
    void handleSslConfig(JSONObject ssl) {
       this.sslConfig = new HashMap<String, String>();
       Iterator iter = ssl.entrySet().iterator();
       while (iter.hasNext()) {
           Entry e = (Entry) iter.next();
           this.sslConfig.put(e.getKey().toString(),
                                      e.getValue().toString());
        }
    }

    public String getKeystorePath()
    {
        String keyStorePath = "";
        if (this.sslConfig != null)
        {
           keyStorePath = this.sslConfig.get(HttpConfig.KEY_STORE);
        }

        return keyStorePath;
    }

    public String getKeystorePassword()
    {
        String keyStorePassword = "";
        if (this.sslConfig != null)
        {
           keyStorePassword = this.sslConfig.get(HttpConfig.KEY_STORE_PASSPHRASE);
        }

        return keyStorePassword;
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
    public RecordType objectToRecordType(final Object o) throws ConfigurationException {
        final String dataClass = asStringValue(this.source, null, o);
        if (JOURNAL.equalsIgnoreCase(dataClass)) {
            return RecordType.Journal;
        } else if (AUDIT.equalsIgnoreCase(dataClass)) {
            return RecordType.Audit;
        } else if (LOG.equalsIgnoreCase(dataClass)) {
            return RecordType.Log;
        }
        throw new ConfigurationException(this.source, "Expected Record Type'"
                + "' to be one of '" + JOURNAL + "', '" + AUDIT + "', or '"
                + LOG + "' (found '" + dataClass + "').");
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
     * Set the path to store records from remotes in. Not applicable to a
     * "Publisher".
     *
     * @param outputPath
     */
    public void setOutputPath(final File outputPath) {
        this.outputPath = outputPath;
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

    public HttpSubscriberConfig getHttpSubscriberConfig()
    {
        HttpSubscriberConfig httpSubscriberConfig = new HttpSubscriberConfig();
        httpSubscriberConfig.setKeystorePath(this.getKeystorePath());
        httpSubscriberConfig.setKeystorePassword(this.getKeystorePassword());
        httpSubscriberConfig.setAddress(this.getAddress().getHostAddress());
        httpSubscriberConfig.setPort(this.getPort());
        httpSubscriberConfig.setRecordTypes(this.getRecordTypes());
        httpSubscriberConfig.setAllowedConfigureDigests(this.getConfigureDigests());
        httpSubscriberConfig.setTlsConfiguration(this.getTlsConfiguration());
        httpSubscriberConfig.setTestMode(this.getTestMode());
        httpSubscriberConfig.setMaxSessionLimit(this.getMaxSessionLimit());
        httpSubscriberConfig.setRole(this.getRole());
        httpSubscriberConfig.setMode(this.getMode());
        httpSubscriberConfig.setOutputPath(this.getOutputPath());

        return httpSubscriberConfig;
    }
}
