/**
 * Copyright (C) 2026 Concurrent Technologies Corporation.
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
