package com.tresys.jalop.jnl.impl.http;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.Arrays;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.tresys.jalop.jnl.IncompleteRecordException;
import com.tresys.jalop.jnl.RecordInfo;
import com.tresys.jalop.jnl.RecordType;
import com.tresys.jalop.jnl.Subscriber;
import com.tresys.jalop.jnl.exceptions.UnexpectedMimeValueException;
import com.tresys.jalop.jnl.impl.subscriber.SubscriberHttpSessionImpl;

public class SubscriberHttpANSHandler {

    static final int BUFFER_SIZE = 4096;

    /**
     * The MessageDigest to use for calculating the JALoP digest.
     */
    private final MessageDigest md;
    private boolean payloadCorrect;
    private boolean payloadComplete;

    private final SubscriberHttpSessionImpl subsess;
    static Logger log = Logger.getLogger(SubscriberHttpANSHandler.class);

    /**
     * Create a SubscriberANSHandler for a record using the

     *
     * @param md
     *            the {@link MessageDigest} that should be used for digest
     *            calculations.
     */
    public SubscriberHttpANSHandler(final MessageDigest md,
            final SubscriberHttpSessionImpl subsess) {
        super();
        this.md = md;
        this.subsess = subsess;
    }

    /**
     * Retrieve any additional MIME headers that were sent as part of this ANS
     * reply. Recognized MIME headers (i.e. Content-Type, Transfer-Encoding, and
     * JALoP headers) are not included in the returned map. If there are no
     * additional MIME headers, then this function returns {@link MimeHeaders}
     * object.
     *
     * @return The {@link MimeHeaders}

     */
    /*   @SuppressWarnings("unchecked")
    public List<MimeHeader> getAdditionalHeaders(final InputDataStream ds) throws BEEPException {
        final InputDataStreamAdapter dsa = ds.getInputStream();
        final Enumeration<String> headers = dsa.getHeaderNames();
        final ArrayList<MimeHeader> ret = new ArrayList<MimeHeader>();
        String hdr = "";
        while (headers.hasMoreElements()) {
            hdr = headers.nextElement();
            if (!Arrays.asList(KNOWN_HEADERS).contains(hdr.toLowerCase()))
                ret.add(new MimeHeader(hdr, dsa.getHeaderValue(hdr)));
        }
        return ret;
    } */

    /**
     * Retrieve the calculated digest.
     *
     * @return The digest of the record according to the JALoP specification.
     * @throws IncompleteRecordException
     *             is thrown if this method is called before reading all data
     *             has not been read from the stream, or if the ANS message from
     *             the remote is invalid (i.e. reports 100 bytes of payload, but
     *             only contains 50).
     */
    public byte[] getRecordDigest() throws IncompleteRecordException {
        if (!this.payloadComplete || !this.payloadCorrect)
            throw new IncompleteRecordException();

        return md.digest();
    }

    public String handleJALRecord(final long sysMetadataSize, final long appMetadataSize,
            final long payloadSize, final String payloadType, final RecordType recType, final String jalId,  InputStream is)
    {
        JalopHttpDataStream js;

        md.reset();

        final Subscriber sub = subsess.getSubscriber();
        try {
            final RecordInfo recInfo = new RecordInfo() {

                private final long sysMetadataLength = sysMetadataSize;
                private final long appMetadataLength = appMetadataSize;
                private final long payloadLength = payloadSize;
                private final String nonce = jalId;
                private final RecordType recordType = recType;

                @Override
                public long getSysMetaLength() {
                    return this.sysMetadataLength;
                }

                @Override
                public String getNonce() {
                    return this.nonce;
                }

                @Override
                public long getOffset() {
                    return subsess.getJournalResumeOffset();
                }

                @Override
                public RecordType getRecordType() {
                    return this.recordType;
                }

                @Override
                public long getPayloadLength() {
                    return this.payloadLength;
                }

                @Override
                public long getAppMetaLength() {
                    return this.appMetadataLength;
                }
            };

            js = new JalopHttpDataStream(sysMetadataSize, is);
            if (!sub.notifySysMetadata(subsess, recInfo, js)) {
                //TODO throw new AbortChannelException("Error in notifySysMetadata");
            }
            js.flush();

            js = new JalopHttpDataStream(appMetadataSize, is);
            if (!sub.notifyAppMetadata(subsess, recInfo, js)) {
                //TODO throw new AbortChannelException("Error in notifyAppMetadata");
            }
            js.flush();

            long payloadSizeToRead = payloadSize;
            if(subsess.getJournalResumeOffset() > 0) {

                //calculate already received payload before resuming
                final InputStream resumeInputStream = subsess.getJournalResumeIS();
                final byte[] buffer =  new byte[BUFFER_SIZE];
                int bytesRead = 0;

                while((bytesRead = resumeInputStream.read(buffer)) > -1) {
                    md.update(buffer, 0, bytesRead);
                }

                if(log.isDebugEnabled()) {
                    log.debug("Updating payloadSize to account for the offset.");
                }
                payloadSizeToRead -= subsess.getJournalResumeOffset();
            }
            js = new JalopHttpDataStream(payloadSizeToRead, is);
            if (!sub.notifyPayload(subsess, recInfo, js)) {
                //TODO throw new AbortChannelException("Error in notifyPayload");
            }
            js.flush();
            // only the first record is a journal resume, subsequent records are normal
            subsess.setJournalResumeOffset(0);
            subsess.setJournalResumeIS(null);

            if (is.read() != -1) {
                //TODO, not sure why error is being thrown here, this might have been needed for beep, but fails with the http input stream.
                //This check does not appear to be needed.
             //   throw new IOException(
                     //   "Additional data exists when none is expected");
            }
            payloadComplete = true;

            final byte [] digest = getRecordDigest();
            if (!sub.notifyDigest(subsess, recInfo, digest)) {
                //TODO throw new AbortChannelException("Error in notifyDigest");
            }

            String hexDgst = "";
            for (byte b : digest) {
                hexDgst = hexDgst + String.format("%02x",b);
            }

            subsess.addDigest(recInfo.getNonce(), hexDgst);

            return hexDgst;
        } catch (final UnexpectedMimeValueException e) {
            if(log.isEnabledFor(Level.ERROR)) {
                log.error(e.getMessage());
            }
            return null;
        } catch (final IOException e) {
            if(log.isEnabledFor(Level.ERROR)) {
                log.error(e.getMessage());
            }
            return null;
        } catch (final IncompleteRecordException e) {
            if(log.isEnabledFor(Level.ERROR)) {
                log.error(e.getMessage());
            }
            return null;
        }
    }

    /*
     * This function is only used for testing purposes. It returns a {@link
     * JalopDataStream}
     *
     * @param size the size to give the instance of JalopDataStream
     */
    JalopHttpDataStream getJalopDataStreamInstance(final long size, final InputStream is,
            final MessageDigest md)
                    throws UnexpectedMimeValueException, IOException {
        return new JalopHttpDataStream(size, is);
    }

    private class JalopHttpDataStream extends InputStream {
        private boolean finishedReading;
        InputStream is;

        private final long dataSize;
        private long bytesRead;

        public JalopHttpDataStream(final long dataSize, final InputStream is) throws IOException,
        UnexpectedMimeValueException {
            this.finishedReading = false;
            this.is = is;

            this.bytesRead = 0;
            if (dataSize < 0) {
                throw new IllegalArgumentException(
                        "dataSize must be 0 or greater");
            }
            this.dataSize = dataSize;
        }

        @Override
        public int read() throws IOException {
            if (this.finishedReading == true)
                return -1;

            int ret = 0;
            // only read if data is expected
            if (this.dataSize != 0) {
                ret = this.is.read();
                if (ret == -1) {
                    throw new IOException();
                }

                md.update((byte) ret);
                this.bytesRead++;
            }

            if (this.bytesRead == this.dataSize) {
                // we should be done. Try to read the BREAK string
                final byte b[] = new byte[5];
                this.is.read(b, 0, 5);
                if (!Arrays.equals("BREAK".getBytes("utf-8"), b)) {
                    payloadCorrect = false;
                    throw new IOException("BREAK string is not where it is expected");
                } else {
                    payloadCorrect = true;
                }
                this.finishedReading = true;
            }
            return ret;
        }

        @Override
        public int read(final byte[] b) throws IOException {
            return read(b, 0, b.length);
        }

        @Override
        public int read(final byte[] b, final int off, final int len) throws IOException {
            if (this.finishedReading == true)
                return -1;

            int bytesRead = 0;

            // Calculate number of bytes to be read
            // If last frame, get remaining bytes
            // otherwise, full buffer
            int new_len = len;
            if ((len + this.bytesRead) > this.dataSize) {
                new_len = (int) (this.dataSize - this.bytesRead);
            }

            // Keep reading until requested amount is finished
            while (bytesRead < new_len) {
                int n = this.is.read(b, off + bytesRead, new_len - bytesRead);
                if ( n > 0 ) {
                    bytesRead += n;
                    this.bytesRead += n;
                } else if (this.is.available() <= 0) {
                    // Shouldn't get to state where expecting more data
                    // but no more data in the stream
                    throw new IOException("Could not read data of requested length");
                } else {
                    // continue waiting for bytes from DataStreamAdapter
                    log.error("DataStreamAdapter return -1 on read from SubscriberANSHandler");
                }
            }

            md.update(b, off, new_len);
            if (this.bytesRead == this.dataSize) {
                // check for break string
                final byte brk[] = new byte[5];
                this.is.read(brk, 0, 5);
                if (!Arrays.equals("BREAK".getBytes("utf-8"), brk)) {
                    payloadCorrect = false;
                    throw new IOException("BREAK string is not where it is expected");
                } else {
                    payloadCorrect = true;
                }
                this.finishedReading = true;
            }
            return bytesRead;
        }

        public void flush() throws IOException {
            if (!this.finishedReading) {
                int ret = read();
                while (ret != -1) {
                    ret = read();
                }
            }

            return;
        }
    }
}
/*
    @Override
    public void receiveANS(final Message message) throws AbortChannelException {
        try {
            final MessageDigest mdClone = (MessageDigest) this.md.clone();
            final Thread t = new Thread(new Dispatcher(message.getDataStream(), mdClone));
            t.start();

        } catch (final CloneNotSupportedException e) {
            throw new AbortChannelException(e.getMessage());
        }
    }

    @Override
    public void receiveNUL(final Message message) throws AbortChannelException {
        if (log.isDebugEnabled()) {
            log.debug("SubscriberANSHandler received NUL");
        }
        //do nothing
    } */


