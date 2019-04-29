package com.tresys.jalop.jnl.impl.http;

public class DigestResult {

    private String digest;
    private boolean performDigest;
    private String jalId;
    private boolean failedDueToSync;

    public String getDigest() {
        return digest;
    }

    public void setDigest(String digest) {
        this.digest = digest;
    }

    public boolean getPerformDigest() {
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

    public boolean getFailedDueToSync() {
        return failedDueToSync;
    }

    public void setFailedDueToSync(boolean failedDueToSync) {
        this.failedDueToSync = failedDueToSync;
    }
}
