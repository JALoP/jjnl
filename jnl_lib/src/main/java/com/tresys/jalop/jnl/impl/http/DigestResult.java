package com.tresys.jalop.jnl.impl.http;

public class DigestResult {

    private String digest;
    private boolean performDigest;
    private String jalId;

    public String getDigest() {
        return digest;
    }

    public void setDigest(String digest) {
        this.digest = digest;
    }

    public boolean isPerformDigest() {
        return performDigest;
    }

    public void setPerformDigest(boolean performDigest) {
        this.performDigest = performDigest;
    }

    public String getJalId() {
        return jalId;
    }

    public void setJalId(String jalId) {
        this.jalId = jalId;
    }
}
