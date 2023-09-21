/*
 * Copyright (C) 2023 The National Security Agency (NSA)
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

package com.tresys.jalop.jnl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;
import java.util.Map;
import java.util.EnumMap;

public class DigestAlgorithms
{
    public static final String JJNL_SHA256_ALGORITHM_NAME = "sha256";
    public static final String JJNL_SHA384_ALGORITHM_NAME = "sha384";
    public static final String JJNL_SHA512_ALGORITHM_NAME = "sha512";

    // Ideally, we shouldn't be hard-coding the strings and should be relying on DigestMethod.SHA256, but
    // if SHA384 is not supported, the value does not exist.
    public static final String JJNL_SHA256_ALGORITHM_URI = "http://www.w3.org/2001/04/xmlenc#sha256";
    public static final String JJNL_SHA384_ALGORITHM_URI = "http://www.w3.org/2001/04/xmldsig-more#sha384";
    public static final String JJNL_SHA512_ALGORITHM_URI = "http://www.w3.org/2001/04/xmlenc#sha512";

    public static final DigestAlgorithmEnum JJNL_DEFAULT_ALGORITHM = DigestAlgorithmEnum.JJNL_DIGEST_ALGORITHM_SHA256;

    private boolean isSHA384Supported;
    private ArrayList<DigestAlgorithmEnum> supportedDigestAlgorithms;

    public DigestAlgorithms() {
        this.supportedDigestAlgorithms = new ArrayList<DigestAlgorithmEnum>();
        this.isSHA384Supported = checkSHA384Supported();
    }

    public enum DigestAlgorithmEnum {
        JJNL_DIGEST_ALGORITHM_SHA256(JJNL_SHA256_ALGORITHM_NAME, JJNL_SHA256_ALGORITHM_URI),
        JJNL_DIGEST_ALGORITHM_SHA384(JJNL_SHA384_ALGORITHM_NAME, JJNL_SHA384_ALGORITHM_URI),
        JJNL_DIGEST_ALGORITHM_SHA512(JJNL_SHA512_ALGORITHM_NAME, JJNL_SHA512_ALGORITHM_URI),
        ;

        private final String algorithmName;
        private final String algorithmUri;

        DigestAlgorithmEnum(String algorithmName, String algorithmUri) {
            this.algorithmName = algorithmName;
            this.algorithmUri = algorithmUri;
        }

        @Override
        public String toString() {
            return this.toName();
        }

        public String toName() {
            return this.algorithmName;
        }

        public String toUri() {
            return this.algorithmUri;
        }

        public static DigestAlgorithmEnum fromName(String name) {
            for (DigestAlgorithmEnum alg : values()) {
                if (alg.toName().equalsIgnoreCase(name)) {
                    return alg;
                }
            }

            return null;
        }

        public static DigestAlgorithmEnum fromUri(String uri) {
            for (DigestAlgorithmEnum alg : values()) {
                if (alg.toUri().equalsIgnoreCase(uri)) {
                    return alg;
                }
            }

            return null;
        }
    }

    public boolean addDigestAlgorithmByName(String name) {
        DigestAlgorithmEnum alg = DigestAlgorithmEnum.fromName(name);

        if (alg != null && !this.supportedDigestAlgorithms.contains(alg) && this.canAddAlgorithm(alg)) {
            this.supportedDigestAlgorithms.add(alg);
            return true;
        }

        return false;
    }

    public boolean removeDigestAlgorithmByName(String name) {
        DigestAlgorithmEnum alg = DigestAlgorithmEnum.fromName(name);

        if (alg != null) {
            return this.supportedDigestAlgorithms.remove(alg);
        }

        return false;
    }

    public boolean addDigestAlgorithmByUri(String uri) {
        DigestAlgorithmEnum alg = DigestAlgorithmEnum.fromUri(uri);

        if (alg != null && !this.supportedDigestAlgorithms.contains(alg) && this.canAddAlgorithm(alg)) {
            this.supportedDigestAlgorithms.add(alg);
            return true;
        }

        return false;
    }

    public boolean removeDigestAlgorithmByUri(String uri) {
        DigestAlgorithmEnum alg = DigestAlgorithmEnum.fromUri(uri);

        if (alg != null) {
            return this.supportedDigestAlgorithms.remove(alg);
        }

        return false;
    }

    public List<DigestAlgorithmEnum> getDigestAlgorithmList() {
        return this.supportedDigestAlgorithms;
    }

    public List<String> getDigestAlgorithmNames() {
        ArrayList<String> digestAlgorithmNames = new ArrayList<String>();
        
        for (DigestAlgorithmEnum alg : this.supportedDigestAlgorithms) {
            digestAlgorithmNames.add(alg.toName());
        }
        
        return digestAlgorithmNames;
    }

    public List<String> getDigestAlgorithmUris() {
        ArrayList<String> digestAlgorithmUris = new ArrayList<String>();

        for (DigestAlgorithmEnum alg : this.supportedDigestAlgorithms) {
            digestAlgorithmUris.add(alg.toUri());
        }

        return digestAlgorithmUris;
    }

    // Testing function only
    public void setSHA384Supported(boolean val) {
        this.isSHA384Supported = val;
    }

    private boolean canAddAlgorithm(DigestAlgorithmEnum alg) {
        if (alg == DigestAlgorithmEnum.JJNL_DIGEST_ALGORITHM_SHA384 && !this.isSHA384Supported) {
            return false;
        }

        return true;
    }

    public int size()
    {
        return this.supportedDigestAlgorithms.size();
    }

    // SHA384 support started at jvm version 9.0.1
    private static boolean checkSHA384Supported() {
        String ver = System.getProperty("java.version");
        String[] split = ver.split("\\.");
        int cnt = split.length;
        // "1.8.0_322" "11.0.19"
        int maj = Integer.parseInt(split[0]); // major version

        if(maj > 9) // SHA384 will be supported
        {
            return true;
        }
        if(maj < 9) // SHA384 will be NOT supported
        {
            return false;
        }

        // version 9.something.
        int sec = -1;
        int minor = -1;
        if(cnt > 1)  // version is split
        {
            sec = Integer.parseInt(split[1]); // secondary version
        }
        if(cnt > 2) // minor version too
        {
            String smin = split[2].substring(0, 1);  // get 1st char
            minor = Integer.parseInt(split[2]); // secondary version
        }
        // check that we have at least 9.0.1        
        // if we are here we are at major == 9
        if(sec > 0)  // supported at least 9.1
        {
            return true;
        }
        // now at 9.0.x
        if(minor > 0)
        {
            return true;
        }
        return false;
    }
}

