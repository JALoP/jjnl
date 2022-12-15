package com.tresys.jalop.jnl.impl.http;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.Arrays;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.tresys.jalop.jnl.IncompleteRecordException;
import com.tresys.jalop.jnl.JNLLog;
import com.tresys.jalop.jnl.RecordInfo;
import com.tresys.jalop.jnl.RecordType;
import com.tresys.jalop.jnl.Subscriber;
import com.tresys.jalop.jnl.exceptions.UnexpectedMimeValueException;
import com.tresys.jalop.jnl.impl.JNLLogger;
import com.tresys.jalop.jnl.impl.subscriber.SubscriberHttpSessionImpl;

public class SubscriberHttpANSHandler {

    static final int BUFFER_SIZE = 4096;
    private static final int LINE_FEED = 10;

    /**
     * The MessageDigest to use for calculating the JALoP digest.
     */
    private final MessageDigest md;
    private boolean payloadCorrect;
    private boolean payloadComplete;
    private boolean performDigest;


    private final SubscriberHttpSessionImpl subsess;
    private JNLLog log = null;

    /**
     * Create a SubscriberHttpANSHandler for a record using the

     *
     * @param md
     *            the {@link MessageDigest} that should be used for digest
     *            calculations.
     */
    public SubscriberHttpANSHandler(final MessageDigest md,
            final SubscriberHttpSessionImpl subsess, boolean performDigest, JNLLog logger) {
        super();
        this.md = md;
        this.subsess = subsess;
        this.performDigest = performDigest;

        if (logger == null)
        {
            log = new JNLLogger(Logger.getLogger(SubscriberHttpANSHandler.class));
        }
        else
        {
            log = logger;
        }
    }

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
    private byte[] getRecordDigest() throws IncompleteRecordException {
        if (!this.payloadComplete || !this.payloadCorrect)
            throw new IncompleteRecordException();

        return md.digest();
    }

    public String handleJALRecord(final long sysMetadataSize, final long appMetadataSize,
            final long payloadSize, final String payloadType, final RecordType recType, final String jalId,  InputStream is, Subscriber subscriber)
    {
        JalopHttpDataStream js = null;

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
            if (!sub.notifySysMetadata(subsess, recInfo, js, subscriber)) {
                throw new IOException("Error in notifySysMetadata");
            }
            js.flush();

            js = new JalopHttpDataStream(appMetadataSize, is);
            if (!sub.notifyAppMetadata(subsess, recInfo, js, subscriber)) {
                throw new IOException("Error in notifyAppMetadata");
            }
            js.flush();

            long payloadSizeToRead = payloadSize;
            long journalResumeOffset = subsess.getJournalResumeOffset();
            if(subsess.getJournalResumeOffset() > 0) {

                //calculate already received payload before resuming
                final InputStream resumeInputStream = subsess.getJournalResumeIS();
                final byte[] buffer =  new byte[BUFFER_SIZE];
                int bytesRead = 0;

                while((bytesRead = resumeInputStream.read(buffer)) > -1) {

                    if (performDigest == true)
                    {
                        md.update(buffer, 0, bytesRead);
                    }
                }

                if(log.isDebugEnabled()) {
                    log.debug("Updating payloadSize to account for the offset.");
                }

                //NOTE, the old code performed this calculation, but it appears to result in a negative number causing a failure later, so removing for http implementation.
                //  payloadSizeToRead -= subsess.getJournalResumeOffset();
            }

            // only the first record is a journal resume, subsequent records are normal
            //TODO - need to determine if this is correct, this is how the java code worked before, however this only handles resuming one record.
            //if multiple recored resumes are required then this will not work.
            subsess.setJournalResumeOffset(0);
            subsess.setJournalResumeIS(null);

            js = new JalopHttpDataStream(payloadSizeToRead, is);
            if (!sub.notifyPayload(subsess, recInfo, js, subscriber)) {
                throw new IOException("Error in notifyPayload");
            }
            js.flush();

            int remainingBytes = is.available();
            if (remainingBytes> 1)
            {
                throw new IOException(
                        "Additional data exists when none is expected");
            }
            else if (remainingBytes == 1)
            {
                //Handles case where libcurl adds a line break to the binary body, ensure that the 1 byte read is a line feed
                int byteValue = is.read();

                if (byteValue != LINE_FEED)
                {
                    throw new IOException(
                            "Additional data exists when none is expected");
                }
            }

            payloadComplete = true;

            //Only perform digest if enabled.
            String hexDgst = "";
            if (performDigest == true)
            {
                final byte [] digest = getRecordDigest();
                if (!sub.notifyDigest(subsess, recInfo, digest, subscriber))
                {
                    throw new IOException("Error in notifyDigest");
                }

                for (byte b : digest)
                {
                    hexDgst = hexDgst + String.format("%02x",b);
                }
            }
            subsess.addDigest(recInfo.getNonce(), hexDgst);

            return hexDgst;
        } catch (final UnexpectedMimeValueException e) {
            if (log.isErrorEnabled())
            {
                log.error(e.getMessage());
            }
            return null;
        } catch (final IOException e) {
            if (log.isErrorEnabled())
            {
                log.error(e.getMessage());
            }
            return null;
        } catch (final IncompleteRecordException e) {
            if (log.isErrorEnabled())
            {
                log.error(e.getMessage());
            }
            return null;
        }
        finally
        {
            if (js != null)
            {
                try
                {
                    js.close();
                }
                catch (IOException ioe)
                {
                    log.error(ioe.getMessage(), ioe);
                }
            }
        }
    }

    /*
     * This function is only used for testing purposes. It returns a {@link
     * JalopDataStream}
     *
     * @param size the size to give the instance of JalopDataStream
     */
    JalopHttpDataStream getJalopDataStreamInstance(final long size, final InputStream is)
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

                if (performDigest == true)
                {
                    md.update((byte) ret);
                }
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
                    // continue waiting for bytes from InputStream
                    log.error("InputStream return -1 on read from SubscriberHttpANSHandler");
                }
            }

            if (performDigest == true)
            {
                md.update(b, off, new_len);
            }
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
