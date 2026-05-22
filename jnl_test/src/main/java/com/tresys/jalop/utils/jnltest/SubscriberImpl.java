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

package com.tresys.jalop.utils.jnltest;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedInputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import com.google.common.io.PatternFilenameFilter;
import com.tresys.jalop.jnl.DigestStatus;
import com.tresys.jalop.jnl.JNLLog;
import com.tresys.jalop.jnl.Mode;
import com.tresys.jalop.jnl.RecordInfo;
import com.tresys.jalop.jnl.RecordType;
import com.tresys.jalop.jnl.SubscribeRequest;
import com.tresys.jalop.jnl.Subscriber;
import com.tresys.jalop.jnl.SubscriberSession;
import com.tresys.jalop.jnl.impl.JNLLogger;
import com.tresys.jalop.jnl.impl.http.JNLTestInterface;
import com.tresys.jalop.jnl.impl.http.SubscriberAndSession;

/**
 * Sample implementation of a {@link Subscriber}. This {@link Subscriber} simply
 * writes records to disk using a very simple hierarchy. Each record gets it's
 * own directory.
 * Each record is given a unique nonce when it is transferred, and this
 * ID is used as the directory name. In addition to the actual records, this
 * {@link Subscriber} records a small file that provides additional status
 * information for each record.
 */
public class SubscriberImpl implements Subscriber {

    /** Filename where status information is written to. */
    private static final String STATUS_FILENAME = "status.js";

    /**
     * Key in the status file for the expected size of the application
     * meta-data.
     * */
    private static final String APP_META_SZ = "app_meta_sz";

    /** Key in the status file for the expected size of the system meta-data. */
    private static final String SYS_META_SZ = "sys_meta_sz";

    /** Key in the status file for the expected size of the current payload. */
    private static final String PAYLOAD_SZ = "payload_sz";

    /** Key in the status file for the expected size of the original payload. */
    private static final String ORIGINAL_PAYLOAD_SZ = "original_payload_sz";

    /**
     * Key in the status file for the nonce the remote uses to identify
     * this record.
     */
    private static final String REMOTE_NONCE = "remote_nonce";

    /** Key in the status file for the remote IP address for this record */
    private static final String REMOTE_PUB_ID = "remote_ip";

    /**
     * Key in the status file for the nonce used locally to identify
     * this record after it has been synced.
     */
    private static final String LOCAL_NONCE = "local_nonce";

    /** Key in the status file for the calculated digest. */
    private static final String DGST = "digest";

    /** The filename for the system meta-data document. */
    private static final String SYS_META_FILENAME = "sys_metadata.xml";

    /** The filename for the application meta-data document. */
    private static final String APP_META_FILENAME = "app_metadata.xml";

    /** The filename for the payload. */
    private static final String PAYLOAD_FILENAME = "payload";

    private boolean createConfirmedFile;

    /**
     * Root of the output directories. Each record gets it's own
     * sub-directory. Records that have been confirmed are
     * transfered here.
     */
    final File outputRoot;

    /**
     * Root of the output directories at the ip level. Each record
     * type contains its own sub-directory. Records are transfered here
     * before they are confirmed.
     */
    final File outputIpRoot;

    /** A logger for this class. */
    private JNLLog LOGGER = null;

    /** The format string for output files. */
    private static final String NONCE_FORMAT_STRING = "0000000000";

    /**
     * Regular expression used for filtering directories, i.e. only directories
     * which have exactly ten digits as a filename.
     */
    private static final String NONCE_REGEX = "^\\d{10}$";

    /**
     * Filter used for searching an existing file system tree for previously
     * downloaded records.
     */
    static final FilenameFilter FILENAME_FILTER =
            new PatternFilenameFilter(NONCE_REGEX);

    /** Formatter used to generate the sub-directories for each record. */
    static final DecimalFormat NONCE_FORMATER =
            new DecimalFormat(NONCE_FORMAT_STRING);

    /** Local nonce counter. */
    private long nonce = 1;

    /** Maps remote NONCE to {@link LocalRecordInfo}. */
    private final Map<String, LocalRecordInfo> nonceMap =
            new HashMap<String, SubscriberImpl.LocalRecordInfo>();

    /** Buffer size for read data from the network and writing to disk. */
    private int bufferSize;

    /** The minimum size of the partially received payload in bytes to perform journal resume on.
     * If the current payload is smaller than this, then journal resume its not peformed.
     * A value of 0 will perform journal resume on any size payload.
     * A value of -1 or less will disable journal resume
     */
    private long journalResumeThresholdSize;

    /** The type of records to transfer. */
    private final RecordType recordType;

    /** The ip address of the remote. */
    private final String remotePubId;

    /** The nonce to send in a subscribe message. */
    String lastNonceFromRemote = null;

    /** The offset to send in a journal subscribe message. */
    long journalOffset = -1;

    /** Flag that indicates journal resume is in progress, so if normal records are sent concurrently, they are not processed as journal resume records. */
    boolean journalResumeInProgress = false;

    /** Stores the local journal resume record that is in progress **/
    LocalRecordInfo journalResumeRecord = null;

    /** The input stream to use for a journal resume. */
    InputStream journalInputStream = null;

    /** The JNLTest associated with this SubscriberImpl. */
    private final JNLTestInterface jnlTest;

    /**
     * FileFilter to get all sub-directories that match the nonce
     * pattern.
     */
    private static final FileFilter FILE_FILTER = new FileFilter() {
        @Override
        public boolean accept(final File pathname) {
            if (pathname.isDirectory()) {
                return FILENAME_FILTER.accept(pathname.getParentFile(),
                        pathname.getName());
            }
            return false;
        }
    };

    /**
     * This is just an object used to track stats about a specific record.
     */
    private class LocalRecordInfo {
        /** The directory to store all information regarding this record. */
        public final File recordDir;
        /** The file to write the status information to. */
        public final File statusFile;
        /** Cached copy of the status file values. */
        public final Properties status;

        public boolean appMetadataExists = false;
        public boolean payloadExists = false;
        public boolean sysMetadataExists = false;

        public long appMetadataLastModified = 0;
        public long sysMetadataLastModified = 0;
        public long payloadLastModified = 0;

        /**
         * Create a new {@link LocalRecordInfo} object.
         *
         * @param info
         *            The record info obtained from the remote.
         * @param localNonce
         *            The NONCE to assign this record to locally.
         */
        public LocalRecordInfo(final RecordInfo info, final long localNonce) {
            this(info.getNonce(), info.getAppMetaLength(),
                    info.getSysMetaLength(), info.getPayloadLength(), localNonce);
        }

        /**
         * Create a new LocalRecordInfo.
         * @param remoteNonce
         *          The nonce of the record as the remote identifies it.
         * @param appMetaLen
         *          The length, in bytes, of the application meta-data.
         * @param sysMetaLen
         *          The length, in bytes, of the system meta-data.
         * @param payloadLen
         *          The length, in bytes, of the payload.
         * @param localNonce
         *          The nonce as it is tracked internally.
         */
        // suppress warnings about raw types for the JSON map
        @SuppressWarnings("unchecked")
        public LocalRecordInfo(final String remoteNonce, final long appMetaLen,
                final long sysMetaLen, final long payloadLen,
                final long localNonce) {

            this.recordDir =
                    new File(SubscriberImpl.this.outputIpRoot,
                            SubscriberImpl.NONCE_FORMATER.format(localNonce));
            this.statusFile = new File(this.recordDir, STATUS_FILENAME);
            this.status = new Properties();
            this.status.setProperty(APP_META_SZ, Long.toString(appMetaLen));
            this.status.setProperty(SYS_META_SZ, Long.toString(sysMetaLen));
            this.status.setProperty(PAYLOAD_SZ, Long.toString(payloadLen));
            this.status.setProperty(REMOTE_NONCE, remoteNonce);

            //Stores the total expected payload size for the record, this is the payload size sent by the publisher added to the current record size
            //to account for journal resume
            File currPayloadFile = new File(this.recordDir.getAbsolutePath(), PAYLOAD_FILENAME);
            long originalPayloadSize = payloadLen + currPayloadFile.length();
            this.status.setProperty(ORIGINAL_PAYLOAD_SZ, Long.toString(originalPayloadSize));

            //TODO might need changed for uuid for http subscriber, remotePubId stores the publisher id
            this.status.setProperty(REMOTE_PUB_ID, SubscriberImpl.this.remotePubId);
        }
    }

    /**
     * Create a {@link SubscriberImpl} object. Instances of this class will
     * create sub-directories under <code>outputIpRoot</code> for each downloaded
     * record.
     *
     * @param recordType
     *          The type of record that will be transfered using this instance.
     * @param outputRoot
     *          The output directory that records will be written to.
     * @param remoteAddr
     *          The {@link InetAddress} of the remote.
     */
    public SubscriberImpl(final RecordType recordType, final File outputRoot,
            final InetAddress remoteAddr, final JNLTestInterface jnlTest, String publisherId, boolean createConfirmedFile, JNLLog logger, int bufferSize, long journalResumeThresholdSize) {
        this.recordType = recordType;
        this.createConfirmedFile = createConfirmedFile;
        this.bufferSize = bufferSize;
        this.journalResumeThresholdSize = journalResumeThresholdSize;

        //Sets logger
        if (logger == null)
        {
            LOGGER = new JNLLogger(Logger.getLogger(SubscriberImpl.class));
        }
        else
        {
            LOGGER = logger;
        }

        //If publisherId is not null, then use publisher uuid instead of ip for dir names
        if (publisherId != null)
        {
            this.remotePubId = publisherId;
        }
        else
        {
            this.remotePubId = remoteAddr.getHostAddress();
        }
        this.jnlTest = jnlTest;
        final File tmp = new File(outputRoot, this.remotePubId);
        final String type;
        switch (recordType) {
        case Audit:
            type = "audit";
            break;
        case Journal:
            type = "journal";
            break;
        case Log:
            type = "log";
            break;
        default:
            throw new IllegalArgumentException("illegal record type");
        }

        this.outputRoot = new File(outputRoot, type);
        if(!this.outputRoot.exists()) {
            this.outputRoot.mkdirs();
        }
        if (!(this.outputRoot.exists() && this.outputRoot.isDirectory())) {
            throw new RuntimeException("Failed to create subdirs for " + type);
        }

        this.outputIpRoot = new File(tmp, type);
        this.outputIpRoot.mkdirs();
        if (!(this.outputIpRoot.exists() && this.outputIpRoot.isDirectory())) {
            throw new RuntimeException("Failed to create subdirs for "
                    + remoteAddr.getHostAddress() + "/"
                    + type);
        }

        try {
            prepareForSubscribe();
        } catch (final Exception e) {
            LOGGER.error("Failed to clean existing directories: ", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Helper utility to run through all records that have been transferred,
     * but not yet synced. For log & audit records, this will remove all
     * records that are not synced (even if they are completely downloaded).
     * For journal records, this finds the record after the most recently
     * synced record record, and deletes all the other unsynced records. For
     * example, if the journal records 1, 2, 3, and 4 have been downloaded, and
     * record number 2 is marked as 'synced', then this will remove record
     * number 4, and try to resume the transfer for record number 3.
     *
     * @throws IOException If there is an error reading existing files, or an
     *          error removing stale directories.
     * @throws java.text.ParseException If there is an error parsing a
     *          directory name.
     */
    final void prepareForSubscribe() throws IOException, java.text.ParseException {

        this.lastNonceFromRemote = SubscribeRequest.EPOC;
        this.journalOffset = 0;
        final Properties p  = new Properties();
        final File[] recordDirs =
                this.outputIpRoot.listFiles(SubscriberImpl.FILE_FILTER);

        final Set<File> deleteDirs = new HashSet<File>();

        if (this.recordType == RecordType.Journal && recordDirs.length > 0 && Mode.Archive.equals(this.getMode())) {
            // Checking the first record to see if it can be resumed, the rest will be deleted
            Arrays.sort(recordDirs);
            final List<File> sortedRecords = new ArrayList<File>(java.util.Arrays.asList(recordDirs));

            final File firstRecord = sortedRecords.remove(0);
            deleteDirs.addAll(sortedRecords);

            Properties status = new Properties();
            try {
                status.load(new FileReader(
                    new File(firstRecord, STATUS_FILENAME)));

                Number originalPayloadSize = null;

                //#1226 - Handle case where incomplete record exists due to reconnects and a 0 byte status.js file exists
                //which results in a null value for original payload size
                //If this occurs originalPayloadSize above will stay null and journal resume will be skipped.
                try
                {
                    originalPayloadSize = (Number) Long.parseLong(status.getProperty(ORIGINAL_PAYLOAD_SZ));
                }
                catch (NumberFormatException nfe)
                {
                    //Log and ignore error
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Deleting " + firstRecord + ", because the '" + STATUS_FILENAME + "' file is empty");
                    }
                }

                //#548 - Special case for journal resume, if the record completely uploaded, but wasn't synced and the publisher sends the same record again.
                //Only resume if the uploaded payload length is less than the expected length.  Delete the temp record and completely re-upload again if greater than or equal
                //to expected payload length.
                //#887 - special case to check for journal resume threshold size from config file in the journalResumeThresholdSize setting.
                //If partially received payload size is equal or greater than this minimal size, then perform journal resume.
                //If this setting is 0 then journal resume is performed on any partially received payload size
                //If this setting is -1 or less then journal resume is disabled
                File payloadFile = new File(firstRecord, PAYLOAD_FILENAME);

                // If the record was confirmed, it will have been removed from this directory
                // We can assume that if we found a record, it is unconfirmed
                if (originalPayloadSize != null &&
                        payloadFile.length() < originalPayloadSize.longValue() &&
                        payloadFile.length() >= journalResumeThresholdSize && journalResumeThresholdSize > -1) {
                    // journal record can be resumed
                    this.lastNonceFromRemote =
                            (String) status.getProperty(REMOTE_NONCE);
                    this.journalOffset = payloadFile.length();
                    FileUtils.forceDelete(new File(firstRecord, APP_META_FILENAME));
                    FileUtils.forceDelete(new File(firstRecord, SYS_META_FILENAME));
                    this.nonce =
                            NONCE_FORMATER.parse(firstRecord.getName()).longValue();

                    try
                    {
                        this.journalInputStream = new FileInputStream(
                                new File(firstRecord, PAYLOAD_FILENAME));
                    }
                    catch (final FileNotFoundException e) {
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("Deleting " + firstRecord + ", because it is missing the '" + PAYLOAD_FILENAME + "' file");
                        }
                        //Resets offset back to 0 due to record being deleted
                        this.journalOffset = 0;
                        deleteDirs.add(firstRecord);
                    }
                } else {
                    deleteDirs.add(firstRecord);
                }

            } catch (final FileNotFoundException e) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Deleting " + firstRecord + ", because it is missing the '" + STATUS_FILENAME + "' file");
                }
                //Resets offset back to 0 due to record being deleted
                this.journalOffset = 0;
                deleteDirs.add(firstRecord);
            }

        } else {
            // Any confirmed record should have been moved so deleting all that are left
            deleteDirs.addAll(java.util.Arrays.asList(recordDirs));
        }

        for (final File f: deleteDirs) {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Removing directory for unsynced record: "
                        + f.getAbsolutePath());
            }
            FileUtils.forceDelete(f);
        }
    }

    private void deleteAllJournalTempRecords()
    {
        final Set<File> deleteDirs = new HashSet<File>();

        final File[] recordDirs =
                this.outputIpRoot.listFiles(SubscriberImpl.FILE_FILTER);

        //Ensures this deletion only occurs on journal records in temp storage
        String dirName = this.outputIpRoot.getName();
        if (dirName!= null && !dirName.equalsIgnoreCase("journal"))
        {
            LOGGER.error("Directory: " + this.outputIpRoot.getAbsolutePath() + " is not a journal record directory and cannot be deleted on journal resume/missing.");
            return;
        }

        deleteDirs.addAll(java.util.Arrays.asList(recordDirs));

        for (final File f: deleteDirs) {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Removing directory for unsynced journal record: "
                        + f.getAbsolutePath());
            }

            try
            {
                FileUtils.forceDelete(f);
            }
            catch (IOException e)
            {
                LOGGER.error("Failed to delete temp journal record directory: " + f.getAbsolutePath(), e);
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public final SubscribeRequest
    getSubscribeRequest(final SubscriberSession sess, boolean createConfirmedFile) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Returning subscriber request for: " + sess.getRole()
            + sess.getRecordType());
            LOGGER.info("nonce: " + this.lastNonceFromRemote);
        }
        return new SubscribeRequest() {
            @Override
            public String getNonce() {
                return SubscriberImpl.this.lastNonceFromRemote;
            }

            @Override
            public long getResumeOffset() {
                return SubscriberImpl.this.journalOffset;
            }

            @Override
            public InputStream getResumeInputStream() {
                return SubscriberImpl.this.journalInputStream;
            }
        };
    }

    @Override
    public final Mode getMode() {
        return this.jnlTest.getMode();
    }

    @Override
    public final boolean notifySysMetadata(final SubscriberSession sess,
            final RecordInfo recordInfo,
            final InputStream sysMetaData, Subscriber subscriber) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Got sysmetadata for " + recordInfo.getNonce());
        }
        LocalRecordInfo lri;
        synchronized (this.nonceMap) {
            if (this.nonceMap.containsKey(recordInfo.getNonce())) {
                LOGGER.error("Already contain a record for "
                        + recordInfo.getNonce());
                return false;
            }
            lri = new LocalRecordInfo(recordInfo, this.nonce);
            this.nonce += 1;
            this.nonceMap.put(recordInfo.getNonce(), lri);
        }
        lri.statusFile.getParentFile().mkdirs();

        if (!dumpPropertiesToFile(lri.statusFile, lri.status)) {
            return false;
        }

        //Sets the first record with a journal resume offset as the journal resume record.
        //Once this record completes, then this is cleared out to indicate journal resume is complete.
        if (this.journalOffset > 0 && this.journalResumeInProgress == false)
        {
            this.journalResumeRecord = lri;
            this.journalResumeInProgress = true;
        }

        final boolean retVal = handleRecordData(lri, recordInfo.getSysMetaLength(),
                SYS_META_FILENAME,
                sysMetaData, sess, this.journalResumeRecord);

        //Update local record modified date and if it exists for future verification
        //Before moving to confirmed location.
        if (retVal == true)
        {
            File sysMetadataFile = new File(lri.recordDir, SYS_META_FILENAME);
            lri.sysMetadataExists = sysMetadataFile.exists();
            lri.sysMetadataLastModified = sysMetadataFile.lastModified();
        }
        else if (journalResumeRecord == lri)
        {
            //reset journal resume since an error occurred on the journal resume record.
            resetJournalResume();
        }

        return retVal;
    }

    private void resetJournalResume()
    {
        this.journalOffset = 0;
        this.journalResumeInProgress = false;
        this.journalResumeRecord = null;
    }

    /**
     * Write a Properties object out to disk.
     * @param file The {@link File} object to write to
     * @param toWrite The {@link Properties} that will be written to the file
     * @return <code>true</code> If the data was successfully written out.
     *         <code>false</code> otherwise.
     */
    final boolean dumpPropertiesToFile(final File file, final Properties toWrite) {
        BufferedOutputStream w;
        try {
            w = new BufferedOutputStream(new FileOutputStream(file));
            toWrite.store(w, "last confirmed status");
            w.close();
        } catch (final FileNotFoundException e) {
            LOGGER.error("Failed to open file (" + file.getPath() + ") for writing:"
                    + e.getMessage());
            return false;
        } catch (final UnsupportedEncodingException e) {
            LOGGER.error("cannot find UTF-8 encoder?");
            return false;
        } catch (final IOException e) {
            LOGGER.error("failed to write to the file (" + file.getPath() + "), aborting");
            return false;
        }
        return true;
    }

    /**
     * Helper utility to write out different sections of the record data.
     *
     * @param lri
     *            The {@link LocalRecordInfo} for this record.
     * @param dataSize
     *            The size of the data, in bytes.
     * @param outputFilename
     *            The filename to use for the data section.
     * @param incomingData
     *            The {@link InputStream} to write to disk.
     * @return <code>true</code> if the data was successfully written to disk,
     *         <code>false</code> otherwise.
     */
    // suppress warnings about raw types for the JSON map
    @SuppressWarnings("unchecked")
    final boolean handleRecordData(final LocalRecordInfo lri,
            final long dataSize,
            final String outputFilename,
            final InputStream incomingData,
            SubscriberSession sess, final LocalRecordInfo journal_resume_record) {
        final byte[] buffer = new byte[this.bufferSize];
        BufferedOutputStream w;
        final File outputFile = new File(lri.recordDir, outputFilename);
        long total = 0;
        long totalDataSize = dataSize;
        boolean isValidJournalResume = false;
        boolean isJournalResume = false;
        BufferedInputStream bufferedInputStream = new BufferedInputStream(
          incomingData, this.bufferSize);

        //Checks if this is a valid journal resume
        if (this.journalOffset > 0 && journal_resume_record != null && lri == journal_resume_record)
        {
            isValidJournalResume = true;
        }

        //#580 - need one additional check to ensure the record being sent has the exact same jalId of the record that was partially uploaded and being resumed
        //if jal ids do not match the same destination dir, then delete the current record, and reset journal resume so record being uploaded is treated as a new record.
        if (isValidJournalResume == true)
        {
            LocalRecordInfo checkLri = null;
            synchronized (this.nonceMap) {
                checkLri = this.nonceMap.get(this.lastNonceFromRemote);
            }

            if (checkLri == null || lri == null || (!lri.recordDir.getAbsolutePath().equalsIgnoreCase(checkLri.recordDir.getAbsolutePath())))
            {
                //Jal ids do not match between record being uploaded and the existing partial record, therefore delete the partial record and perform a fresh upload instead of resume.
                this.journalOffset = 0;
                sess.resetJournalOffset();

                deleteAllJournalTempRecords();

                lri.recordDir.mkdir();
            }
        }

        if(isValidJournalResume && PAYLOAD_FILENAME.equals(outputFilename)) {

            isJournalResume = true;
            total = this.journalOffset;
        }

        boolean ret = true;
        try {
            w = new BufferedOutputStream(new FileOutputStream(outputFile, true), this.bufferSize);
            int cnt = bufferedInputStream.read(buffer);
            while (cnt != -1) {
                w.write(buffer, 0, cnt);
                w.flush();
                total += cnt;
                cnt = bufferedInputStream.read(buffer);
            }
            w.close();
        } catch (final FileNotFoundException e) {
            LOGGER.error("Failed to open '" + outputFile.getAbsolutePath()
            + "' for writing");
            return false;
        } catch (final IOException e) {
            LOGGER.error("Error while trying to write to '"
                    + outputFile.getAbsolutePath() + "' for writing: "
                    + e.getMessage());
            return false;
        }

        //Need special case to account for journal resume offset.  The dataSize is the size of the partial record being uploaded
        //and total is the total bytes of the partial payload plus the resumed payload, therefore need to substract the journal resume offset from the total.
        if (isJournalResume)
        {
            totalDataSize = total - this.journalOffset;
        }

        if (totalDataSize != dataSize) {
            LOGGER.error("System metadata reported to be: " + dataSize
                    + ", received " + total);
            ret = false;
        }

        return ret;
    }

    @Override
    public final boolean notifyAppMetadata(final SubscriberSession sess,
            final RecordInfo recordInfo,
            final InputStream appMetaData, Subscriber subscriber) {
        if (recordInfo.getAppMetaLength() != 0) {
            LocalRecordInfo lri;
            synchronized (this.nonceMap) {
                lri = this.nonceMap.get(recordInfo.getNonce());
            }
            if (lri == null) {
                LOGGER.error("Can't find local status for: "
                        + recordInfo.getNonce());
                return false;
            }

            final boolean result =  handleRecordData(lri, recordInfo.getAppMetaLength(),
                    APP_META_FILENAME,
                    appMetaData, sess, null);

            //Update local record modified date and if it exists for future verification
            //before moving to confirmed location.
            if (result == true)
            {
                File appMetadataFile = new File(lri.recordDir, APP_META_FILENAME);
                lri.appMetadataExists = appMetadataFile.exists();
                lri.appMetadataLastModified = appMetadataFile.lastModified();
            }
            else if (journalResumeRecord == lri)
            {
                //reset journal resume since an error occurred on the journal resume record.
                resetJournalResume();
            }

            return result;
        }

        return true;
    }

    @Override
    public final boolean notifyPayload(final SubscriberSession sess,
            final RecordInfo recordInfo,
            final InputStream payload, Subscriber subscriber) {
        if (recordInfo.getPayloadLength() != 0) {
            LocalRecordInfo lri;
            synchronized (this.nonceMap) {
                lri = this.nonceMap.get(recordInfo.getNonce());
            }
            if (lri == null) {
                LOGGER.error("Can't find local status for: "
                        + recordInfo.getNonce());
                return false;
            }

            //Set exists flag to true since payload is being uploaded.
            lri.payloadExists = true;

            final boolean retVal = handleRecordData(lri, recordInfo.getPayloadLength(),
                    PAYLOAD_FILENAME,
                    payload, sess, journalResumeRecord);

            //This journal resume record completed, reset journal resume to continue with normal record processing.
            if (journalResumeRecord == lri)
            {
                resetJournalResume();
            }

            //Update local record modified date for future verification
            //before moving to confirmed location.
            File payloadFile = new File(lri.recordDir, PAYLOAD_FILENAME);
            lri.payloadLastModified = payloadFile.lastModified();

            return retVal;
        }
        return true;
    }

    // suppress warnings about raw types for the JSON map
    @SuppressWarnings("unchecked")
    @Override
    public final boolean notifyDigest(final SubscriberSession sess,
            final RecordInfo recordInfo,
            final byte[] digest, Subscriber subscriber) {
        String hexString = "";
        for (byte b : digest) {
            hexString = hexString + String.format("%02x",b);
        }

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Calculated digest for " + recordInfo.getNonce()
            + ": " + hexString);
        }
        LocalRecordInfo lri;
        synchronized (this.nonceMap) {
            lri = this.nonceMap.get(recordInfo.getNonce());
        }
        if (lri == null) {
            LOGGER.error("Can't find local status for: "
                    + recordInfo.getNonce());
            return false;
        }

        lri.status.setProperty(DGST, hexString);
        return true;
    }

    @SuppressWarnings("unchecked")
    @Override
    public final boolean notifyJournalMissing(final SubscriberSession sess,
            final String jalId, Subscriber subscriber)
    {
        boolean ret = true;

        LOGGER.debug("notifyJournalMissing for jalId: " + jalId + " and local nonce: " + nonce);

        //Resets journal offset
        this.journalOffset = 0;
        sess.resetJournalOffset();

        //Clears out all temp records to allow for fresh jal record uploads
        try
        {
            deleteAllJournalTempRecords();
        }
        catch (RuntimeException re)
        {
            ret = false;
        }

        return ret;
    }

    @SuppressWarnings("unchecked")
    @Override
    public final boolean notifyDigestResponse(final SubscriberSession sess,
            final String nonce, final DigestStatus status, Subscriber subscriber) {
        LocalRecordInfo lri;

        LOGGER.debug("notifyDigestResponse for nonce: " + nonce + ", status: " + status);
        // try to locate the provided nonce in the map of received digests
        synchronized (this.nonceMap) {
            lri = this.nonceMap.remove(nonce);
        }

        if (lri == null) {
            LOGGER.error("Can't find local status for: " + nonce);
            return true;
        }

        // The status file isn't useful anymore, skip dumping the latest values for
        // the sake of performance

        // If the digest is confirmed, go ahead and move the record from temp directory
        // Otherwise delete the record
        if(DigestStatus.Confirmed.equals(status)) {
            if(!moveConfirmedRecord(lri)) {
                LOGGER.error("Failed to sync record:  " + lri.recordDir.getAbsolutePath());
                return false;
            }
        }
        else
        {
            if (!deleteRecord(lri))
            {
                LOGGER.error("Failed to delete record:  " + lri.recordDir.getAbsolutePath());
                return false;
            }
        }

        return true;
    }

    private boolean checkFiles(LocalRecordInfo lri)
    {
        File payloadFile = new File(lri.recordDir, PAYLOAD_FILENAME);

        if (lri.payloadExists != payloadFile.exists())
        {
            LOGGER.error(PAYLOAD_FILENAME + " file was added or removed since being uploaded.");
            return false;
        }
        else if (lri.payloadLastModified != payloadFile.lastModified())
        {
            LOGGER.error(PAYLOAD_FILENAME + " file was modified since being uploaded.");
            return false;
        }

        File sysMetadataFile = new File(lri.recordDir, SYS_META_FILENAME);

        if (lri.sysMetadataExists != sysMetadataFile.exists())
        {
            LOGGER.error(SYS_META_FILENAME + " was added or removed since being uploaded.");
            return false;
        }
        else if (lri.sysMetadataLastModified != sysMetadataFile.lastModified())
        {
            LOGGER.error(SYS_META_FILENAME + " file was modified since being uploaded.");
            return false;
        }

        File appMetadataFile = new File(lri.recordDir, APP_META_FILENAME);

        if (lri.appMetadataExists != appMetadataFile.exists())
        {
            LOGGER.error(APP_META_FILENAME + " was added or removed since being uploaded.");
            return false;
        }
        else if (lri.appMetadataLastModified != appMetadataFile.lastModified())
        {
            LOGGER.error(APP_META_FILENAME + " file was modified since being uploaded.");
            return false;
        }

        return true;
    }

    //11006ad2-d6fd-44ec-a2f8-c67025ba1635_2025-11-24T18:06:52.995703_910964_182449728
    public static String buildDirectoryName(String nonce){
        String[] info = nonce.split("_");
        if (info.length != 4){
            return null;
        }
        StringBuilder name = new StringBuilder("");
        name.append(info[1]).append("_");
        name.append(info[2]).append("_");
        name.append(info[3]).append("_");
        name.append(info[0]);
        return name.toString();
    }

    @SuppressWarnings("unchecked")
    private boolean moveConfirmedRecord(final LocalRecordInfo lri) {
        String nonce = (String) lri.status.get(REMOTE_NONCE);
        if(nonce == null){
            LOGGER.error("REMOTE_NONCE is NULL");
            return false;
        }
        String directoryName = buildDirectoryName(nonce);
        if(directoryName == null){
            LOGGER.error("REMOTE_NONCE is JAL_ID: " + nonce);
            return false;
        }
        final File dest = new File(this.outputRoot, directoryName);

        //Ensures the files uploaded weren't deleted or modified before confirming.
        boolean result = checkFiles(lri);
        if (!result)
        {
            LOGGER.error("Error: the contents of the temporary record dir " + lri.recordDir.getAbsolutePath() + " were modified since being uploaded and this record can not be confirmed.");
            return false;
        }

		//Handles moving the confirmed files
		try
		{
			//JAL-1225 - deletes any dest dir if it exists due to jald reconnects and incomplete sync
			if (dest.exists())
			{
				try
				{
					LOGGER.info("Deleting existing destination record directory due to previous incomplete sync: " + dest.getAbsolutePath());
					FileUtils.deleteDirectory(dest);
				}
				catch (IOException ioe)
				{
					LOGGER.error("Error deleting destination record directory: " + dest.getAbsolutePath(), ioe);
					return false;
				}
			}
			Path sourcePath = Paths.get(lri.recordDir.getAbsolutePath());
			Path destPath = Paths.get(dest.getAbsolutePath());

			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Renaming directory from: " + lri.recordDir.getAbsolutePath() + " to: "
						+ dest.getAbsolutePath());
			}

			Files.move(sourcePath, destPath, StandardCopyOption.REPLACE_EXISTING);

			//Creates confirmed file if configured to be on
			if (this.createConfirmedFile == true)
			{
 				File confirmedFile = new File(dest, "confirmed");

				try
				{
					confirmedFile.createNewFile();
				}
				catch(IOException ie)
				{
					LOGGER.error("Error creating empty confirmed file in directory.");
					return false;
				}
			}
		}
		catch (IOException ioe)
		{
			LOGGER.error("Error trying to move confirmed file.", ioe);
			return false;
		}

		return true;
	}

    @SuppressWarnings("unchecked")
    private boolean deleteRecord(final LocalRecordInfo lri) {

        if(LOGGER.isDebugEnabled()) {
            LOGGER.debug("Deleting record directory: " + lri.recordDir.getAbsolutePath());
        }

        if (!lri.recordDir.exists())
        {
            LOGGER.error("Record directory does not exist: " + lri.recordDir.getAbsolutePath());
            return false;
        }

        try
        {
            FileUtils.deleteDirectory(lri.recordDir);
        }
        catch (IOException ioe)
        {
            LOGGER.error("Error deleting record directory: " + lri.recordDir.getAbsolutePath(), ioe);
            return false;
        }

        return true;
    }

    @Override
    public SubscriberAndSession getSessionAndSubscriberBySessionId(String sessionId)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeSession(String sessionId)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean getCreateConfirmedFile()
    {
        throw new UnsupportedOperationException();
    }
}

