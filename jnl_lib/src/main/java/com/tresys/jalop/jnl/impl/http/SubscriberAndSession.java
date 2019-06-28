package com.tresys.jalop.jnl.impl.http;

import com.tresys.jalop.jnl.Session;
import com.tresys.jalop.jnl.Subscriber;

public class SubscriberAndSession {

    public SubscriberAndSession(Subscriber subscriber, Session session)
    {
        this.subscriber = subscriber;
        this.session = session;
    }

    private Session session;
    private Subscriber subscriber;

    public Session getSession() {
        return session;
    }
    public Subscriber getSubscriber() {
        return subscriber;
    }
}
