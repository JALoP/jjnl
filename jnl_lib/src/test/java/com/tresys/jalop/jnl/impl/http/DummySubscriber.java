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
