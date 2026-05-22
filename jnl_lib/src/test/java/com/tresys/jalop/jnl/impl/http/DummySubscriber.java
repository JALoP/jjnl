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

import java.io.InputStream;

import com.tresys.jalop.jnl.DigestStatus;
import com.tresys.jalop.jnl.Mode;
import com.tresys.jalop.jnl.RecordInfo;
import com.tresys.jalop.jnl.SubscribeRequest;
import com.tresys.jalop.jnl.Subscriber;
import com.tresys.jalop.jnl.SubscriberSession;

public class DummySubscriber implements Subscriber {


    @Override
    public SubscribeRequest getSubscribeRequest(SubscriberSession sess, boolean createConfirmedFile) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean notifySysMetadata(SubscriberSession sess, RecordInfo recordInfo, InputStream sysMetaData,
            Subscriber subscriber) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean notifyAppMetadata(SubscriberSession sess, RecordInfo recordInfo, InputStream appMetaData,
            Subscriber subscriber) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean notifyPayload(SubscriberSession sess, RecordInfo recordInfo, InputStream payload,
            Subscriber subscriber) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean notifyDigest(SubscriberSession sess, RecordInfo recordInfo, byte[] digest, Subscriber subscriber) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean notifyJournalMissing(SubscriberSession sess, String jalId, Subscriber subscriber) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean notifyDigestResponse(SubscriberSession sess, String nonce, DigestStatus status, Subscriber subscriber) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Mode getMode() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public SubscriberAndSession getSessionAndSubscriberBySessionId(String sessionId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean removeSession(String sessionId) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean getCreateConfirmedFile() {
        // TODO Auto-generated method stub
        return false;
    }
}
