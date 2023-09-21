package com.tresys.jalop.utils.jnltest;

import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedInputStream;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.json.simple.parser.ParseException;

import com.tresys.jalop.jnl.DigestStatus;
import com.tresys.jalop.jnl.Mode;
import com.tresys.jalop.jnl.RecordInfo;
import com.tresys.jalop.jnl.Session;
import com.tresys.jalop.jnl.SubscribeRequest;
import com.tresys.jalop.jnl.Subscriber;
import com.tresys.jalop.jnl.SubscriberSession;
import com.tresys.jalop.jnl.impl.http.HttpSubscriberConfig;
import com.tresys.jalop.jnl.impl.http.JNLTestInterface;
import com.tresys.jalop.jnl.impl.http.JNLWebServer;
import com.tresys.jalop.jnl.impl.http.SubscriberAndSession;
import com.tresys.jalop.jnl.impl.subscriber.SubscriberHttpSessionImpl;
import com.tresys.jalop.utils.jnltest.Config.ConfigurationException;
import com.tresys.jalop.utils.jnltest.Config.HttpConfig;

@SuppressWarnings("serial")
public class JNLSubscriber implements Subscriber, JNLTestInterface
{
    public JNLSubscriber(HttpSubscriberConfig config)
    {
        http_config = config;
    }

    /**
     * Counter to keep track of the last used nonce for log records
     */
    private long latestLogNONCE;
    /**
     * Counter to keep track of the last used nonce for audit records
     */
    private long latestAuditNONCE;
    /**
     * Counter to keep track of the last used nonce for journal records
     */
    private long latestJournalNONCE;

    /**
     * Configuration for this instance of JNLTest.
     */
    private static HttpSubscriberConfig http_config;

    /**
     * From Sessions to associated {@link SubscriberImpl}
     */
    private final Map<Session, SubscriberImpl> sessMap = Collections.synchronizedMap(new HashMap<Session, SubscriberImpl>());

    /**
     * ConnectionHandler implementation
     */

    public static void main( String[] args ) throws Exception
    {
        if (args.length != 1) {
            System.err.println("Must specify exactly one argument that is "
                    + " the configuration file to use");
            System.exit(1);
        }
        HttpConfig config;
        try {
            config = HttpConfig.parse(args[0]);
        } catch (final IOException e) {
            System.err.println("Caught IO exception: " + e.getMessage());
            System.exit(1);
            throw new RuntimeException("Failed to call exit()");
        } catch (final ParseException e) {
            System.err.print(e.toString());
            System.exit(1);
            throw new RuntimeException("Failed to call exit()");
        } catch (final ConfigurationException e) {
            System.err.println("Exception processing the config file: "
                    + e.getMessage());
            System.exit(1);
            throw new RuntimeException("Failed to call exit()");
        }
        http_config = config.getHttpSubscriberConfig();
        final JNLWebServer jt = new JNLWebServer(config.getHttpSubscriberConfig());
        System.out.println("Starting Connections");

        jt.start(new JNLSubscriber(http_config));
    }

    public HttpSubscriberConfig getConfig()
    {
        return http_config;
    }

    /**
     * @return the latestLogNONCE
     */
    @Override
    public long getLatestLogNONCE() {
        return latestLogNONCE;
    }

    /**
     * @param latestLogNONCE the latestLogNONCE to set
     */
    @Override
    public void setLatestLogNONCE(final long latestLogNONCE) {
        this.latestLogNONCE = latestLogNONCE;
    }

    /**
     * @return the latestAuditNONCE
     */
    @Override
    public long getLatestAuditNONCE() {
        return latestAuditNONCE;
    }

    /**
     * @param latestAuditNONCE the latestAuditNONCE to set
     */
    @Override
    public void setLatestAuditNONCE(final long latestAuditNONCE) {
        this.latestAuditNONCE = latestAuditNONCE;
    }

    /**
     * @return the latestJournalNONCE
     */
    @Override
    public long getLatestJournalNONCE() {
        return latestJournalNONCE;
    }

    /**
     * @param latestJournalNONCE the latestJournalNONCE to set
     */
    @Override
    public void setLatestJournalNONCE(final long latestJournalNONCE) {
        this.latestJournalNONCE = latestJournalNONCE;
    }

    @Override
    public SubscribeRequest getSubscribeRequest(final SubscriberSession sess, boolean createConfirmedFile) {
        // TODO: All the code here to manage the maps should really be happening in the
        // connection handler callbacks, but the library isn't generating those events
        // quite yet.
        SubscriberImpl sub;
        synchronized (this.sessMap) {

            //Handles removing the oldest session before new one is added if session limit has been reached.
            if (this.sessMap.size() >= http_config.getMaxSessionLimit())
            {
                String oldestSessionId = null;
                LocalDateTime oldestLastTouchedTimestamp = null;
                for (Map.Entry<Session, SubscriberImpl> entry : sessMap.entrySet())
                {
                    if (oldestSessionId == null && oldestLastTouchedTimestamp == null)
                    {
                        SubscriberHttpSessionImpl session = ((SubscriberHttpSessionImpl)entry.getKey());
                        oldestSessionId = session.getSessionId();
                        oldestLastTouchedTimestamp = session.getLastTouchedTimestamp();
                    }

                    SubscriberHttpSessionImpl session = ((SubscriberHttpSessionImpl)entry.getKey());

                    if (oldestLastTouchedTimestamp.compareTo(session.getLastTouchedTimestamp()) > 0)
                    {
                        oldestSessionId = session.getSessionId();
                        oldestLastTouchedTimestamp = session.getLastTouchedTimestamp();
                    }
                }

                removeSession(oldestSessionId);
            }

            sub = this.sessMap.get(sess);
            if (sub == null) {
                sub = new SubscriberImpl(sess.getRecordType(), http_config.getOutputPath(), null, this, sess.getPublisherId(), createConfirmedFile, http_config.getLogger(), http_config.getBufferSize());
                this.sessMap.put(sess, sub);
            }
        }

        return sub.getSubscribeRequest(sess, createConfirmedFile);
    }

    @Override
    public SubscriberAndSession getSessionAndSubscriberBySessionId(String sessionId)
    {
        Session foundSession = null;
        SubscriberAndSession subscriberAndSession = null;
        synchronized (this.sessMap) {

            for (Session currSession : this.sessMap.keySet())
            {
                if (((SubscriberHttpSessionImpl)currSession).getSessionId().equals(sessionId))
                {
                    foundSession = currSession;
                    break;
                }
            }

            if (foundSession != null)
            {
                subscriberAndSession = new SubscriberAndSession(this.sessMap.get(foundSession), foundSession);
            }
        }

        return subscriberAndSession;
    }

    @Override
    public boolean getCreateConfirmedFile()
    {
        return http_config.getCreateConfirmedFile();
    }

    @Override
    public boolean removeSession(String sessionId)
    {
        synchronized (this.sessMap) {

            Session removeKey = null;

            for (Session currSession : this.sessMap.keySet())
            {
                SubscriberHttpSessionImpl session = ((SubscriberHttpSessionImpl)currSession);
                if (session.getSessionId().equals(sessionId))
                {
                    removeKey = currSession;
                    break;
                }
            }

            SubscriberImpl removedSession = this.sessMap.remove(removeKey);

            if (removedSession == null)
            {
                return false;
            }
            else
            {
                return true;
            }
        }
    }

    @Override
    public boolean notifySysMetadata(final SubscriberSession sess, final RecordInfo recordInfo, final InputStream sysMetaData, Subscriber subscriber) {
        SubscriberImpl currSubscriberImpl = (SubscriberImpl)subscriber;
        return currSubscriberImpl.notifySysMetadata(sess, recordInfo, sysMetaData, subscriber);
    }

    @Override
    public boolean notifyAppMetadata(final SubscriberSession sess, final RecordInfo recordInfo, final InputStream appMetaData, Subscriber subscriber) {
        SubscriberImpl currSubscriberImpl = (SubscriberImpl)subscriber;
        return currSubscriberImpl.notifyAppMetadata(sess, recordInfo, appMetaData, subscriber);
    }

    @Override
    public boolean notifyPayload(final SubscriberSession sess, final RecordInfo recordInfo, final InputStream payload, Subscriber subscriber) {
        SubscriberImpl currSubscriberImpl = (SubscriberImpl)subscriber;
        return currSubscriberImpl.notifyPayload(sess, recordInfo, payload, subscriber);
    }

    @Override
    public boolean notifyDigest(final SubscriberSession sess, final RecordInfo recordInfo, final byte[] digest, Subscriber subscriber) {
        SubscriberImpl currSubscriberImpl = (SubscriberImpl)subscriber;
        return currSubscriberImpl.notifyDigest(sess, recordInfo, digest, subscriber);
    }

    @Override
    public boolean notifyDigestResponse(final SubscriberSession sess, final String nonce, final DigestStatus status, Subscriber subscriber) {
        SubscriberImpl currSubscriberImpl = (SubscriberImpl)subscriber;
        return currSubscriberImpl.notifyDigestResponse(sess, nonce, status, subscriber);
    }

    @Override
    public boolean notifyJournalMissing(final SubscriberSession sess, final String nonce, Subscriber subscriber) {
        SubscriberImpl currSubscriberImpl = (SubscriberImpl)subscriber;
        return currSubscriberImpl.notifyJournalMissing(sess, nonce, subscriber);
    }

    @Override
    public Mode getMode() {
        return http_config.getMode();
    }
}

