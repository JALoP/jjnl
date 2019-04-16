package com.tresys.jalop.jnl.impl.http;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.Arrays;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.tresys.jalop.jnl.Mode;
import com.tresys.jalop.jnl.RecordType;
import com.tresys.jalop.jnl.Subscriber;
import com.tresys.jalop.jnl.impl.subscriber.SubscriberHttpSessionImpl;

public class SubscriberHttpSessionImplTest {

    public static final String SHA256_STR = "http://www.w3.org/2001/04/xmlenc#sha256";
    public static final String XML_COMPRESSION_NONE = "none";
    private static String CERT_FINGERPRINT = "0a7be6d47ac528d8ce0e1c59b4d052be88f5ddc4601caec1f1dcda8a09766723";

    @BeforeClass
    public static void init() throws Exception {
        TestResources.configureLogging();
    }

    public Subscriber getSubscriber()
    {
        HttpSubscriberConfig config = new HttpSubscriberConfig();

        config.setOutputPath(new File("./output"));
        config.setAddress("127.0.0.1");
        config.setAllowedConfigureDigests(Arrays.asList(new String[] {HttpUtils.MSG_CONFIGURE_DIGEST_ON}));
        config.setKeystorePassword("changeit");
        config.setKeystorePath("keystore.jks");
        config.setPort(8443);

        JNLSubscriber subscriber = new JNLSubscriber(config);
        return subscriber;
    }

    public SubscriberHttpSessionImpl getValidSession()
    {
        final SubscriberHttpSessionImpl sessionImpl = new SubscriberHttpSessionImpl("ae8a54d7-dd7c-4c50-a7e7-f948a140c556",
                "ae8a54d7-dd7c-4c50-a7e7-f948a140c556", RecordType.Audit, Mode.Archive, getSubscriber(), SHA256_STR,
                XML_COMPRESSION_NONE, 1,
                2, true, CERT_FINGERPRINT);

        return sessionImpl;
    }


    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void ConstructorEmptyPublisherIdTest()
    {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("'publisherId' is required.");

        final SubscriberHttpSessionImpl sessionImpl = new SubscriberHttpSessionImpl("",
                "ae8a54d7-dd7c-4c50-a7e7-f948a140c556", HttpUtils.getRecordType("audit"), HttpUtils.getMode("live"),
                getSubscriber(), SHA256_STR, XML_COMPRESSION_NONE, 1, 1, true, CERT_FINGERPRINT);
    }

    @Test
    public void ConstructorNullPublisherIdTest()
    {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("'publisherId' is required.");

        SubscriberHttpSessionImpl sessionImpl = new SubscriberHttpSessionImpl(null,
                "ae8a54d7-dd7c-4c50-a7e7-f948a140c556", HttpUtils.getRecordType("audit"), HttpUtils.getMode("live"),
                getSubscriber(), SHA256_STR, XML_COMPRESSION_NONE, 1, 1, true, CERT_FINGERPRINT);
    }

    @Test
    public void ConstructorEmptySessionIdTest()
    {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("'sessionId' is required.");

        final SubscriberHttpSessionImpl sessionImpl = new SubscriberHttpSessionImpl("ae8a54d7-dd7c-4c50-a7e7-f948a140c556",
                "", HttpUtils.getRecordType("audit"), HttpUtils.getMode("live"), getSubscriber(), SHA256_STR,
                XML_COMPRESSION_NONE, 1, 1, true, CERT_FINGERPRINT);
    }

    @Test
    public void ConstructorNullSessionIdTest()
    {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("'sessionId' is required.");

        SubscriberHttpSessionImpl sessionImpl = new SubscriberHttpSessionImpl("ae8a54d7-dd7c-4c50-a7e7-f948a140c556",
                null, HttpUtils.getRecordType("audit"), HttpUtils.getMode("live"), getSubscriber(), SHA256_STR,
                XML_COMPRESSION_NONE, 1, 1, true, CERT_FINGERPRINT);
    }

    @Test
    public void ConstructorEmptyRecordTypeTest()
    {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("'recordType' cannot be null or Unset.");

        final SubscriberHttpSessionImpl sessionImpl = new SubscriberHttpSessionImpl("ae8a54d7-dd7c-4c50-a7e7-f948a140c556",
                "ae8a54d7-dd7c-4c50-a7e7-f948a140c556", null, HttpUtils.getMode("live"), getSubscriber(), SHA256_STR,
                XML_COMPRESSION_NONE, 1, 1, true, CERT_FINGERPRINT);
    }

    @Test
    public void ConstructorUnsetRecordTypeTest()
    {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("'recordType' cannot be null or Unset.");

        final SubscriberHttpSessionImpl sessionImpl = new SubscriberHttpSessionImpl("ae8a54d7-dd7c-4c50-a7e7-f948a140c556",
                "ae8a54d7-dd7c-4c50-a7e7-f948a140c556", RecordType.Unset, Mode.Archive, getSubscriber(), SHA256_STR,
                XML_COMPRESSION_NONE, 1, 1, true, CERT_FINGERPRINT);
    }

    @Test
    public void ConstructorEmptyModeTest()
    {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("'mode' cannot be null or Unset.");

        final SubscriberHttpSessionImpl sessionImpl = new SubscriberHttpSessionImpl("ae8a54d7-dd7c-4c50-a7e7-f948a140c556",
                "ae8a54d7-dd7c-4c50-a7e7-f948a140c556", HttpUtils.getRecordType("audit"), null, getSubscriber(), SHA256_STR,
                XML_COMPRESSION_NONE, 1, 1, true, CERT_FINGERPRINT);
    }

    @Test
    public void ConstructorUnsetModeTest()
    {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("'mode' cannot be null or Unset.");

        final SubscriberHttpSessionImpl sessionImpl = new SubscriberHttpSessionImpl("ae8a54d7-dd7c-4c50-a7e7-f948a140c556",
                "ae8a54d7-dd7c-4c50-a7e7-f948a140c556", HttpUtils.getRecordType("audit"), Mode.Unset,
                getSubscriber(), SHA256_STR, XML_COMPRESSION_NONE, 1, 1, true, CERT_FINGERPRINT);
    }

    @Test
    public void ConstructorNullSubscriberTest()
    {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("'subscriber' cannot be null.");

        final SubscriberHttpSessionImpl sessionImpl = new SubscriberHttpSessionImpl("ae8a54d7-dd7c-4c50-a7e7-f948a140c556",
                "ae8a54d7-dd7c-4c50-a7e7-f948a140c556", RecordType.Audit, Mode.Archive, null, SHA256_STR,
                XML_COMPRESSION_NONE, 1, 1, true, CERT_FINGERPRINT);
    }

    @Test
    public void ConstructorNullDigestTest()
    {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("'digestMethod' is required.");

        final SubscriberHttpSessionImpl sessionImpl = new SubscriberHttpSessionImpl("ae8a54d7-dd7c-4c50-a7e7-f948a140c556",
                "ae8a54d7-dd7c-4c50-a7e7-f948a140c556", RecordType.Audit, Mode.Archive, getSubscriber(), null,
                XML_COMPRESSION_NONE, 1, 1, true, CERT_FINGERPRINT);
    }

    @Test
    public void ConstructorEmptyDigestTest()
    {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("'digestMethod' is required.");

        final SubscriberHttpSessionImpl sessionImpl = new SubscriberHttpSessionImpl("ae8a54d7-dd7c-4c50-a7e7-f948a140c556",
                "ae8a54d7-dd7c-4c50-a7e7-f948a140c556", RecordType.Audit, Mode.Archive, getSubscriber(), "",
                XML_COMPRESSION_NONE, 1, 1, true, CERT_FINGERPRINT);
    }

    @Test
    public void ConstructorNullXmlCompressionTest()
    {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("'xmlEncoding' is required.");

        final SubscriberHttpSessionImpl sessionImpl = new SubscriberHttpSessionImpl("ae8a54d7-dd7c-4c50-a7e7-f948a140c556",
                "ae8a54d7-dd7c-4c50-a7e7-f948a140c556", RecordType.Audit, Mode.Archive, getSubscriber(), SHA256_STR,
                null, 1, 1, true, CERT_FINGERPRINT);
    }

    @Test
    public void ConstructorEmptyCertFingerprint()
    {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("'certFingerprint' is required.");

        final SubscriberHttpSessionImpl sessionImpl = new SubscriberHttpSessionImpl("ae8a54d7-dd7c-4c50-a7e7-f948a140c556",
                "ae8a54d7-dd7c-4c50-a7e7-f948a140c556", RecordType.Audit, Mode.Archive, getSubscriber(), "SHA256_STR",
                XML_COMPRESSION_NONE, 1, 1, true, "");
    }

    @Test
    public void ConstructorNullCertFingerprint()
    {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("'certFingerprint' is required.");

        final SubscriberHttpSessionImpl sessionImpl = new SubscriberHttpSessionImpl("ae8a54d7-dd7c-4c50-a7e7-f948a140c556",
                "ae8a54d7-dd7c-4c50-a7e7-f948a140c556", RecordType.Audit, Mode.Archive, getSubscriber(), "SHA256_STR",
                XML_COMPRESSION_NONE, 1, 1, true, null);
    }

    @Test
    public void ConstructorEmptyXmlCompressionTest()
    {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("'xmlEncoding' is required.");

        final SubscriberHttpSessionImpl sessionImpl = new SubscriberHttpSessionImpl("ae8a54d7-dd7c-4c50-a7e7-f948a140c556",
                "ae8a54d7-dd7c-4c50-a7e7-f948a140c556", RecordType.Audit, Mode.Archive, getSubscriber(), SHA256_STR,
                "", 1, 1, true, CERT_FINGERPRINT);
    }

    @Test
    public void ConstructorInvalidPendingDigestTimeoutSecondsTest()
    {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("'pendingDigestTimeoutSeconds' "
                + "must be a positive number.");

        final SubscriberHttpSessionImpl sessionImpl = new SubscriberHttpSessionImpl("ae8a54d7-dd7c-4c50-a7e7-f948a140c556",
                "ae8a54d7-dd7c-4c50-a7e7-f948a140c556", RecordType.Audit, Mode.Archive, getSubscriber(), SHA256_STR,
                XML_COMPRESSION_NONE, 0, 1, true, CERT_FINGERPRINT);
    }

    @Test
    public void ConstructorInvalidPendingDigestMaxTest()
    {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("'pendingDigestMax' "
                + "must be a positive number.");

        final SubscriberHttpSessionImpl sessionImpl = new SubscriberHttpSessionImpl("ae8a54d7-dd7c-4c50-a7e7-f948a140c556",
                "ae8a54d7-dd7c-4c50-a7e7-f948a140c556", RecordType.Audit, Mode.Archive, getSubscriber(), SHA256_STR,
                XML_COMPRESSION_NONE, 1, 0, true, CERT_FINGERPRINT);
    }

    @Test
    public void ConstructorValidTest()
    {
        final SubscriberHttpSessionImpl sessionImpl = getValidSession();
        assertEquals(RecordType.Audit, sessionImpl.getRecordType());
        assertEquals(Mode.Archive, sessionImpl.getMode());
        assertEquals(SHA256_STR, sessionImpl.getDigestMethod());
        assertEquals(XML_COMPRESSION_NONE, sessionImpl.getXmlEncoding());
        assertEquals(true, sessionImpl.getConfigureDigest());
        assertEquals("ae8a54d7-dd7c-4c50-a7e7-f948a140c556", sessionImpl.getPublisherId());
        assertEquals("ae8a54d7-dd7c-4c50-a7e7-f948a140c556", sessionImpl.getSessionId());
        assertEquals(2, sessionImpl.getPendingDigestMax());
        assertEquals(1, sessionImpl.getPendingDigestTimeoutSeconds());
    }

    @Test
    public void getAddressTest()
    {
        exception.expect(UnsupportedOperationException.class);
        exception.expectMessage("Not implemented");

        final SubscriberHttpSessionImpl sessionImpl = getValidSession();
        sessionImpl.getAddress();
    }
}
