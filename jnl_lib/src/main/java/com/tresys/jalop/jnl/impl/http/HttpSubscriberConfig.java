package com.tresys.jalop.jnl.impl.http;

import java.io.File;
import java.util.List;
import java.util.Set;

import com.tresys.jalop.jnl.Mode;
import com.tresys.jalop.jnl.RecordType;
import com.tresys.jalop.jnl.Role;

public class HttpSubscriberConfig {

    private String keystorePath;
    private String keystorePassword;
    private int port;
    private String address;
    private Set<RecordType> recordTypes;
    private List<String> allowedConfigureDigests;
    private String tlsConfiguration;
    private int maxSessionLimit;
    private Role role;
    private Mode mode;
    private File outputPath;
    private boolean createConfirmedFile;

    public String getKeystorePath() {
        return keystorePath;
    }
    public void setKeystorePath(String keyStorePath) {
        this.keystorePath = keyStorePath;
    }
    public String getKeystorePassword() {
        return keystorePassword;
    }
    public void setKeystorePassword(String keystorePassword) {
        this.keystorePassword = keystorePassword;
    }
    public int getPort() {
        return port;
    }
    public void setPort(int port) {
        this.port = port;
    }
    public String getAddress() {
        return address;
    }
    public void setAddress(String address) {
        this.address = address;
    }
    public Set<RecordType> getRecordTypes() {
        return recordTypes;
    }
    public void setRecordTypes(Set<RecordType> recordTypes) {
        this.recordTypes = recordTypes;
    }
    public String getTlsConfiguration() {
        return tlsConfiguration;
    }
    public void setTlsConfiguration(String tlsConfiguration) {
        this.tlsConfiguration = tlsConfiguration;
    }
    public int getMaxSessionLimit() {
        return maxSessionLimit;
    }
    public void setMaxSessionLimit(int maxSessionLimit) {
        this.maxSessionLimit = maxSessionLimit;
    }
    public List<String> getAllowedConfigureDigests() {
        return allowedConfigureDigests;
    }
    public void setAllowedConfigureDigests(List<String> allowedConfigureDigests) {
        this.allowedConfigureDigests = allowedConfigureDigests;
    }
    public Role getRole() {
        return role;
    }
    public void setRole(Role role) {
        this.role = role;
    }
    public Mode getMode() {
        return mode;
    }
    public void setMode(Mode mode) {
        this.mode = mode;
    }
    public File getOutputPath() {
        return outputPath;
    }
    public void setOutputPath(File outputPath) {
        this.outputPath = outputPath;
    }
    public boolean getCreateConfirmedFile() {
        return createConfirmedFile;
    }
    public void setCreateConfirmedFile(String createConfirmedFile) {

        if (createConfirmedFile != null && createConfirmedFile.equalsIgnoreCase("on"))
        {
            this.createConfirmedFile = true;
        }
        else
        {
            this.createConfirmedFile = false;
        }
    }
}
