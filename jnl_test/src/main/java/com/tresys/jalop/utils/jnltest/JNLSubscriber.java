package com.tresys.jalop.utils.jnltest;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
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
import com.tresys.jalop.jnl.impl.http.JNLWebServer;
import com.tresys.jalop.jnl.impl.http.JNLTestInterface;
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
    /** Logger for this class */
    private static final Logger logger = Logger.getLogger(JNLSubscriber.class);

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
    private final Map<Session, SubscriberImpl> sessMap = new HashMap<Session, SubscriberImpl>();

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
    public SubscribeRequest getSubscribeRequest(final SubscriberSession sess) {
        // TODO: All the code here to manage the maps should really be happening in the
        // connection handler callbacks, but the library isn't generating those events
        // quite yet.
        SubscriberImpl sub;
        synchronized (this.sessMap) {
            sub = this.sessMap.get(sess);
            if (sub == null) {
                sub = new SubscriberImpl(sess.getRecordType(), http_config.getOutputPath(), null, this, sess.getPublisherId());
                this.sessMap.put(sess, sub);
            }
        }

        return sub.getSubscribeRequest(sess);
    }

    @Override
    public Session getSessionBySessionId(String sessionId)
    {
        Session foundSession = null;
        synchronized (this.sessMap) {

            for (Session currSession : this.sessMap.keySet())
            {
                if (((SubscriberHttpSessionImpl)currSession).getSessionId().equals(sessionId))
                {
                    foundSession = currSession;
                    break;
                }
            }
        }

        return foundSession;
    }


    @Override
    public void prepareForNewSession()
    {
        synchronized (this.sessMap) {

            if (this.sessMap.size() == http_config.getMaxSessionLimit())
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
                        continue;
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
        }
    }

    public void removeSession(String sessionId)
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

            this.sessMap.remove(removeKey);
        }
    }

    @Override
    public boolean notifySysMetadata(final SubscriberSession sess, final RecordInfo recordInfo, final InputStream sysMetaData) {
        return this.sessMap.get(sess).notifySysMetadata(sess, recordInfo, sysMetaData);
    }

    @Override
    public boolean notifyAppMetadata(final SubscriberSession sess, final RecordInfo recordInfo, final InputStream appMetaData) {
        return this.sessMap.get(sess).notifyAppMetadata(sess, recordInfo, appMetaData);
    }

    @Override
    public boolean notifyPayload(final SubscriberSession sess, final RecordInfo recordInfo, final InputStream payload) {
        return this.sessMap.get(sess).notifyPayload(sess, recordInfo, payload);
    }

    @Override
    public boolean notifyDigest(final SubscriberSession sess, final RecordInfo recordInfo, final byte[] digest) {
        return this.sessMap.get(sess).notifyDigest(sess, recordInfo, digest);
    }

    @Override
    public boolean notifyDigestResponse(final SubscriberSession sess, final String nonce, final DigestStatus status) {
        return this.sessMap.get(sess).notifyDigestResponse(sess, nonce, status);
    }

    @Override
    public boolean notifyJournalMissing(final SubscriberSession sess, final String nonce) {
        return this.sessMap.get(sess).notifyJournalMissing(sess, nonce);
    }

    @Override
    public Mode getMode() {
        return http_config.getMode();
    }
}

