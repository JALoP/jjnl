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

import com.tresys.jalop.jnl.RecordType;
import com.tresys.jalop.jnl.impl.http.HttpSubscriberConfig;
import com.tresys.jalop.jnl.impl.http.HttpUtils;

/**
 * The {@link HttpConfig} class is used to to parse a configuration file, and
 * configure the JNLTest program.
 */
public class HttpConfig extends BaseConfig {
	private static final String CONFIGURE_DIGEST = "configureDigest";
    private static final String CONFIGURE_TLS = "configureTls";
    private static final String RECORD_TYPE = "recordType";

    private HashMap<String, String> sslConfig;
    private List<String> configureDigests;
    private String configureTls;
    private final Set<RecordType> recordTypes;

    private static final String KEY_STORE_PASSPHRASE = "Key Store Passphrase";
    private static final String KEY_STORE = "Key Store";

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
    @Override
    void handleSubscriber(final JSONObject subscriber)
            throws ConfigurationException {
        super.handleSubscriber(subscriber);

        handleConfigureDigest(subscriber);
        handleTls(subscriber);
        handleRecordType(subscriber);
    }


    /**
     * Create a new {@link HttpConfig} object.
     *
     * @param source
     *            This string will be used in generated errors to indicate what
     *            resources caused a problem.
     */
    HttpConfig(final String source) {
        super(source);
        this.recordTypes = new HashSet<RecordType>();
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

    public List<String> getConfigureDigests()
    {
        return this.configureDigests;
    }

    public String getTlsConfiguration()
    {
        return this.configureTls;
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
        httpSubscriberConfig.setRole(this.getRole());
        httpSubscriberConfig.setMode(this.getMode());
        httpSubscriberConfig.setOutputPath(this.getOutputPath());

        return httpSubscriberConfig;
    }
}
