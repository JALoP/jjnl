package com.tresys.jalop.jnl.impl.http;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.Arrays;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.tresys.jalop.jnl.RecordType;
import com.tresys.jalop.jnl.Subscriber;
import com.tresys.jalop.jnl.impl.subscriber.SubscriberHttpSessionImpl;

public class SubscriberHttpSessionImplTest {

    public static final String SHA256_STR = "http://www.w3.org/2001/04/xmlenc#sha256";
    public static final String XML_COMPRESSION_NONE = "none";

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
        final SubscriberHttpSessionImpl sessionImpl = new SubscriberHttpSessionImpl("test",
                RecordType.Audit, getSubscriber(), SHA256_STR,
                XML_COMPRESSION_NONE, 1,
                2, HttpUtils.MSG_CONFIGURE_DIGEST_ON);

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
                HttpUtils.getRecordType("audit"), getSubscriber(), SHA256_STR,
                XML_COMPRESSION_NONE, 1,
                1, HttpUtils.MSG_CONFIGURE_DIGEST_ON);
    }

    @Test
    public void ConstructorNullPublisherIdTest()
    {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("'publisherId' is required.");

        SubscriberHttpSessionImpl sessionImpl = new SubscriberHttpSessionImpl(null,
                HttpUtils.getRecordType("audit"), getSubscriber(), SHA256_STR,
                XML_COMPRESSION_NONE, 1,
                1, HttpUtils.MSG_CONFIGURE_DIGEST_ON);
    }

    @Test
    public void ConstructorEmptyRecordTypeTest()
    {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("'recordType' cannot be null or Unset.");

        final SubscriberHttpSessionImpl sessionImpl = new SubscriberHttpSessionImpl("test",
                null, getSubscriber(), SHA256_STR,
                XML_COMPRESSION_NONE, 1,
                1, HttpUtils.MSG_CONFIGURE_DIGEST_ON);
    }

    @Test
    public void ConstructorUnsetRecordTypeTest()
    {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("'recordType' cannot be null or Unset.");

        final SubscriberHttpSessionImpl sessionImpl = new SubscriberHttpSessionImpl("test",
                RecordType.Unset, getSubscriber(), SHA256_STR,
                XML_COMPRESSION_NONE, 1,
                1, HttpUtils.MSG_CONFIGURE_DIGEST_ON);
    }

    @Test
    public void ConstructorNullSubscriberTest()
    {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("'subscriber' cannot be null.");

        final SubscriberHttpSessionImpl sessionImpl = new SubscriberHttpSessionImpl("test",
                RecordType.Audit, null, SHA256_STR,
                XML_COMPRESSION_NONE, 1,
                1, HttpUtils.MSG_CONFIGURE_DIGEST_ON);
    }

    @Test
    public void ConstructorNullDigestTest()
    {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("'digestMethod' is required.");

        final SubscriberHttpSessionImpl sessionImpl = new SubscriberHttpSessionImpl("test",
                RecordType.Audit, getSubscriber(), null,
                XML_COMPRESSION_NONE, 1,
                1, HttpUtils.MSG_CONFIGURE_DIGEST_ON);
    }

    @Test
    public void ConstructorEmptyDigestTest()
    {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("'digestMethod' is required.");

        final SubscriberHttpSessionImpl sessionImpl = new SubscriberHttpSessionImpl("test",
                RecordType.Audit, getSubscriber(), "",
                XML_COMPRESSION_NONE, 1,
                1, HttpUtils.MSG_CONFIGURE_DIGEST_ON);
    }

    @Test
    public void ConstructorNullXmlCompressionTest()
    {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("'xmlEncoding' is required.");

        final SubscriberHttpSessionImpl sessionImpl = new SubscriberHttpSessionImpl("test",
                RecordType.Audit, getSubscriber(), SHA256_STR,
                null, 1,
                1, HttpUtils.MSG_CONFIGURE_DIGEST_ON);
    }

    @Test
    public void ConstructorEmptyXmlCompressionTest()
    {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("'xmlEncoding' is required.");

        final SubscriberHttpSessionImpl sessionImpl = new SubscriberHttpSessionImpl("test",
                RecordType.Audit, getSubscriber(), SHA256_STR,
                "", 1,
                1, HttpUtils.MSG_CONFIGURE_DIGEST_ON);
    }

    @Test
    public void ConstructorNullConfigureDigestTest()
    {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("'configureDigest' is required.");

        final SubscriberHttpSessionImpl sessionImpl = new SubscriberHttpSessionImpl("test",
                RecordType.Audit, getSubscriber(), SHA256_STR,
                XML_COMPRESSION_NONE, 1,
                1, null);
    }

    @Test
    public void ConstructorEmptyConfigureDigestTest()
    {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("'configureDigest' is required.");

        final SubscriberHttpSessionImpl sessionImpl = new SubscriberHttpSessionImpl("test",
                RecordType.Audit, getSubscriber(), SHA256_STR,
                XML_COMPRESSION_NONE, 1,
                1, "");
    }

    @Test
    public void ConstructorInvalidPendingDigestTimeoutSecondsTest()
    {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("'pendingDigestTimeoutSeconds' "
                + "must be a positive number.");

        final SubscriberHttpSessionImpl sessionImpl = new SubscriberHttpSessionImpl("test",
                RecordType.Audit, getSubscriber(), SHA256_STR,
                XML_COMPRESSION_NONE, 0,
                1, HttpUtils.MSG_CONFIGURE_DIGEST_ON);
    }

    @Test
    public void ConstructorInvalidPendingDigestMaxTest()
    {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("'pendingDigestMax' "
                + "must be a positive number.");

        final SubscriberHttpSessionImpl sessionImpl = new SubscriberHttpSessionImpl("test",
                RecordType.Audit, getSubscriber(), SHA256_STR,
                XML_COMPRESSION_NONE, 1,
                0, HttpUtils.MSG_CONFIGURE_DIGEST_ON);
    }

    @Test
    public void ConstructorValidTest()
    {
        final SubscriberHttpSessionImpl sessionImpl = getValidSession();
        assertEquals(RecordType.Audit, sessionImpl.getRecordType());
        assertEquals(SHA256_STR, sessionImpl.getDigestMethod());
        assertEquals(XML_COMPRESSION_NONE, sessionImpl.getXmlEncoding());
        assertEquals(HttpUtils.MSG_CONFIGURE_DIGEST_ON, sessionImpl.getConfigureDigest());
        assertEquals("test", sessionImpl.getPublisherId());
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
