package com.tresys.jalop.jnl.impl.subscriber;

import java.io.InputStream;
import java.net.InetAddress;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.crypto.dsig.DigestMethod;

import org.apache.log4j.Logger;

import com.tresys.jalop.jnl.Mode;
import com.tresys.jalop.jnl.RecordType;
import com.tresys.jalop.jnl.Role;
import com.tresys.jalop.jnl.Session;
import com.tresys.jalop.jnl.Subscriber;
import com.tresys.jalop.jnl.SubscriberSession;
import com.tresys.jalop.jnl.impl.http.SubscriberHttpANSHandler;

public class SubscriberHttpSessionImpl implements SubscriberSession {

    private static final Logger log = Logger
            .getLogger(SubscriberHttpSessionImpl.class);

    private final Subscriber subscriber;
    protected volatile int pendingDigestTimeoutSeconds;
    protected volatile int pendingDigestMax;
    protected Map<String, String> digestMap;
    private long journalResumeOffset;
    private InputStream journalResumeIS;
    private SubscriberHttpANSHandler subscriberHandler;

    private final RecordType recordType;
    private final Mode mode;
    private final String digestMethod;
    private final String xmlEncoding;
    private volatile boolean errored;
    private String publisherId;
    private String sessionId;
    private boolean performDigest;
    private String certFingerprint;

    /**
     * Create a {@link SubscriberHttpSessionImpl} object.
     *
     * @param remoteAddress
     *            The InetAddress used for the transfers.
     * @param recordType
     *            The type of JAL records this {@link Session} transfers.
     * @param subscriber
     *            The {@link Subscriber} associated with this {@link Session}.
     * @param digestMethod
     *            The digest method to be used on this {@link Session}.
     * @param xmlEncoding
     *            The XML encoding to be used on this {@link Session}.
     * @param pendingDigestTimeoutSeconds
     *            The time to wait, in seconds before sending a "digest"
     *            message.
     * @param pendingDigestMax
     *            The maximum number of digests to queue.
     */
    public SubscriberHttpSessionImpl(final String publisherId, final String sessionId,
            final RecordType recordType, final Mode mode, final Subscriber subscriber,
            final String digestMethod, final String xmlEncoding,
            final int pendingDigestTimeoutSeconds, final int pendingDigestMax,final boolean performDigest, final String certFingerprint) {

        if (recordType == null || recordType.equals(RecordType.Unset)) {
            throw new IllegalArgumentException(
                    "'recordType' cannot be null or Unset.");
        }

        if (mode == null || mode.equals(Mode.Unset)) {
            throw new IllegalArgumentException("'mode' cannot be null or Unset.");
        }

        if (digestMethod == null || digestMethod.trim().isEmpty()) {
            throw new IllegalArgumentException("'digestMethod' is required.");
        }

        if (xmlEncoding == null || xmlEncoding.trim().isEmpty()) {
            throw new IllegalArgumentException("'xmlEncoding' is required.");
        }

        if (publisherId == null || publisherId.trim().isEmpty()) {
            throw new IllegalArgumentException("'publisherId' is required.");
        }

        if (sessionId == null || sessionId.trim().isEmpty()) {
            throw new IllegalArgumentException("'sessionId' is required.");
        }

        if (subscriber == null) {
            throw new IllegalArgumentException("'subscriber' cannot be null.");
        }

        if (pendingDigestTimeoutSeconds <= 0) {
            throw new IllegalArgumentException("'pendingDigestTimeoutSeconds' "
                    + "must be a positive number.");
        }

        if (pendingDigestMax <= 0) {
            throw new IllegalArgumentException("'pendingDigestMax' "
                    + "must be a positive number.");
        }

        if (certFingerprint == null || certFingerprint.trim().isEmpty())
        {
            throw new IllegalArgumentException("'certFingerprint' is required.");
        }

        try {
            this.subscriberHandler = new SubscriberHttpANSHandler(
                    MessageDigest
                            .getInstance(getDigestType(digestMethod.trim())),
                    this, performDigest);
        } catch (final NoSuchAlgorithmException e) {
            throw new IllegalArgumentException(
                    "'digestMethod' must be a valid DigestMethod", e);
        }

        this.recordType = recordType;
        this.mode = mode;
        this.digestMethod = digestMethod.trim();
        this.xmlEncoding = xmlEncoding.trim();
        this.performDigest = performDigest;
        this.publisherId = publisherId.trim();
        this.sessionId = sessionId.trim();
        this.subscriber = subscriber;
        this.pendingDigestMax = pendingDigestMax;
        this.pendingDigestTimeoutSeconds = pendingDigestTimeoutSeconds;
        this.digestMap = new HashMap<String, String>();
        this.journalResumeOffset = 0;
        this.certFingerprint = certFingerprint;
    }

    protected String getDigestType(final String algorithm) {

        if (DigestMethod.SHA256.equals(algorithm)) {
            return "SHA-256";
        } else if (DigestMethod.SHA512.equals(algorithm)) {
            return "SHA-512";
        } else if ("http://www.w3.org/2001/04/xmldsig-more#sha384"
                .equals(algorithm)) {
            return "SHA-384";
        }
        return "";
    }

    public SubscriberHttpANSHandler getsubscriberHandler()
    {
        return this.subscriberHandler;
    }

    public String getCertFingerprint()
    {
        return this.certFingerprint;
    }

    /**
     * @return the pendingDigestTimeoutSeconds
     */
    public long getPendingDigestTimeoutSeconds() {
        return this.pendingDigestTimeoutSeconds;
    }

    @Override
    public RecordType getRecordType() {
        return this.recordType;
    }

    @Override
    public InetAddress getAddress()
    {
        //Before this was the ip address of the publisher, we don't need this in the http model as the subscriber doesn't connect to the publisher.
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public String getPublisherId()
    {
        return publisherId;
    }

    @Override
    public String getSessionId()
    {
        return sessionId;
    }

    @Override
    public void setErrored() {
        this.errored = true;
    }

    public boolean getConfigureDigest()
    {
        return performDigest;
    }

    @Override
    public boolean isOk() {

        return (!this.errored);
    }

    @Override
    public String getDigestMethod() {
        return this.digestMethod;
    }

    @Override
    public String getXmlEncoding() {
        return this.xmlEncoding;
    }

    /**
     * @return the pendingDigestMax
     */
    public int getPendingDigestMax() {
        return this.pendingDigestMax;
    }

    /**
     * @return the journalResumeOffset
     */
    public long getJournalResumeOffset() {
        return this.journalResumeOffset;
    }

    /**
     * @param journalResumeOffset the journalResumeOffset to set
     */
    public void setJournalResumeOffset(final long journalResumeOffset) {
        this.journalResumeOffset = journalResumeOffset;
    }

    /**
     * @return the journalResumeIS
     */
    public InputStream getJournalResumeIS() {
        return journalResumeIS;
    }

    /**
     * @param journalResumeIS the {@link InputStream} to set
     */
    public void setJournalResumeIS(final InputStream journalResumeIS) {
        this.journalResumeIS = journalResumeIS;
    }

    @Override
    public Role getRole() {
        return Role.Subscriber;
    }

    @Override
    public Mode getMode() {
        return this.mode;
    }

    /**
     * @return the subscriber
     */
    public Subscriber getSubscriber() {
        return this.subscriber;
    }

    @Override
    public void setDigestTimeout(final int pendingDigestTimeoutSeconds) {

        if (pendingDigestTimeoutSeconds <= 0) {
            throw new IllegalArgumentException("'pendingDigestTimeoutSeconds' "
                    + "must be a positive number.");
        }

        this.pendingDigestTimeoutSeconds = pendingDigestTimeoutSeconds;
    }

    @Override
    public void setPendingDigestMax(final int pendingDigestMax) {

        if (pendingDigestMax <= 0) {
            throw new IllegalArgumentException("'pendingDigestMax' "
                    + "must be a positive number.");
        }

        this.pendingDigestMax = pendingDigestMax;
    }

    /**
     * Adds a map of nonces and their related digests to the current map to
     * be sent to the publisher
     *
     * @param toAdd
     *            A map of nonces and digests to add to the map to be sent.
     */
    public synchronized void addAllDigests(final Map<String, String> toAdd) {

        this.digestMap.putAll(toAdd);
        if (this.digestMap.size() >= this.pendingDigestMax) {
            synchronized (this) {
                this.notifyAll();
            }
        }
    }

    /**
     * Adds a nonce and the related digest to a map to be sent to the
     * publisher
     *
     * @param nonce
     *            A String which is the nonce to be added to the map of
     *            digests to send.
     * @param digest
     *            A String which is the digest for the nonce to be added to
     *            the map of digests to send.
     */
    public synchronized void addDigest(final String nonce,
            final String digest) {

        this.digestMap.put(nonce, digest);
        if (this.digestMap.size() >= this.pendingDigestMax) {
            synchronized (this) {
                this.notifyAll();
            }
        }
    }
}
