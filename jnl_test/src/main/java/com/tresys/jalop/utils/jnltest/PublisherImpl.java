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
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import jakarta.xml.soap.MimeHeaders;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.google.common.io.PatternFilenameFilter;
import com.tresys.jalop.jnl.DigestPair;
import com.tresys.jalop.jnl.Mode;
import com.tresys.jalop.jnl.Publisher;
import com.tresys.jalop.jnl.PublisherSession;
import com.tresys.jalop.jnl.RecordType;
import com.tresys.jalop.jnl.SourceRecord;

public class PublisherImpl implements Publisher {

	/** A logger for this class. */
	private static final Logger LOGGER = Logger.getLogger(PublisherImpl.class);

	/** The filename for the system meta-data document. */
	private static final String SYS_META_FILENAME = "sys_metadata.xml";

	/** The filename for the application meta-data document. */
	private static final String APP_META_FILENAME = "app_metadata.xml";

	/** The filename for the payload. */
	private static final String PAYLOAD_FILENAME = "payload";

	/** Filename where status information is written to. */
	private static final String STATUS_FILENAME = "status.js";

	/** The format string for output files. */
	private static final String NONCE_FORMAT_STRING = "0000000000";

	/** Formatter used to generate the sub-directories for each record. */
	static final DecimalFormat NONCE_FORMATER =
        new DecimalFormat(NONCE_FORMAT_STRING);

	/** Key in the status file to indicate if a 'sync' message was sent. */
    private static final String SYNCED = "synced";

    /** Key in the status file for the local digest. */
    private static final String LOCALDGST = "local_digest";

    /** Key in the status file for the peer digest. */
    private static final String PEERDGST = "peer_digest";

    /** Sys metadata xml tags needed to construct remote nonce */
    private static final String RECORD_ID_TAG = "RecordID";
    private static final String TIMESTAMP_TAG = "Timestamp";

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
     * Root of the input directories. Each record has its own
     * sub-directory.
     */
    final File inputRoot;

    /** The type of records to transfer. */
    private final RecordType recordType;

	public PublisherImpl(final File inputRoot, final RecordType recordType) {

		this.recordType = recordType;

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
        this.inputRoot = new File(inputRoot, type);
        if (!(this.inputRoot.exists() && this.inputRoot.isDirectory())) {
            throw new RuntimeException("Subdirs don't exist for "
                                       + type);
        }
	}

	private SourceRecord getJournalRecord(final String nonce, String localNonce, final long offset) {

		String formatedNonce = null;
		try {
			formatedNonce = NONCE_FORMATER.format(Long.valueOf(localNonce));
		} catch (NumberFormatException nfe) {
			if (LOGGER.isEnabledFor(Level.ERROR)) {
				LOGGER.error("Nonce is not numeric - returning null");
			}
			return null;
		}

		final File nonceDir = new File(this.inputRoot, formatedNonce
					);

		if(!nonceDir.exists()) {
			if(LOGGER.isInfoEnabled()) {
				LOGGER.info("Directory structure for nonce: " + localNonce +
						" does not exist. Returning null.");
			}
			return null;
		}
		return new SourceRecordImpl(nonce, localNonce, offset);
	}

	private String getXmlValue(String tagName, Document document) {
		String value = null;
		NodeList list = document.getElementsByTagName(tagName);

		if (list != null && list.getLength() > 0) {
			NodeList subList = list.item(0).getChildNodes();

			if (subList != null && subList.getLength() > 0) {
				value = subList.item(0).getNodeValue();
			}
		}
		return value;
	}

	private String getRemoteNonceFromSystemMetadata(String sysMetadataFilePath) {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		File sysMetadataFile = new File(sysMetadataFilePath);
		String remoteNonce = null;
		try {
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document document = builder.parse(sysMetadataFile);

			String recordId = getXmlValue(RECORD_ID_TAG, document);
	 		String timestamp = getXmlValue(TIMESTAMP_TAG, document);

	 		//JAL-974 - Need to add a dummy process id and thread id (extra underscore) to work with c subscriber using lmdb to successfully extract the nonce timestamp
 			remoteNonce = recordId + "_" + timestamp + "_0_0";
		} catch (IOException ioe) {
			LOGGER.error("Failed to parse system metadata file due to IO exception: " + sysMetadataFilePath, ioe);
			return null;
		} catch (SAXException sax) {
			LOGGER.error("Failed to parse system metadata file: " + sysMetadataFilePath, sax);
			return null;
		} catch (ParserConfigurationException pce) {
			LOGGER.error("Parser configruation exception occurred while parsing system metadata file: " + sysMetadataFilePath, pce);
			return null;
		}

		return remoteNonce;
	}

	public long lookupLocalNonce(final PublisherSession sess, final String remoteNonce) {

		long localNonce = 1;
		File[] directories = this.inputRoot.listFiles(File::isDirectory);

		while (directories != null && localNonce <= directories.length) {

			final File nonceDir =
					new File(this.inputRoot,
						NONCE_FORMATER.format(Long.valueOf(localNonce)));

			if(!nonceDir.exists()) {
				if(LOGGER.isInfoEnabled()) {
					LOGGER.info("Directory structure for nonce: " + localNonce +
							" does not exist. Local nonce was not found");
				}
				return -1;
			}

			File sysFile = new File(nonceDir, SYS_META_FILENAME);
			if(!sysFile.exists()) {
				throw new RuntimeException("SysMetadata file doesn't exist");
			}
			String currRemoteNonce = getRemoteNonceFromSystemMetadata(sysFile.getAbsolutePath());

			//Record with remote nonce was found, retgurn local nonce
			if (currRemoteNonce != null && currRemoteNonce.equals(remoteNonce))
			{
				return localNonce;
			}

			localNonce = localNonce + 1;
		}

		return -1;
	}

	public SourceRecord getNextRecord(final PublisherSession sess, final String lastNonce) {

		final long nextNonce;

		try {

			if(Long.valueOf(lastNonce) == 0) {

				if(LOGGER.isInfoEnabled()) {
					LOGGER.info("Nonce of 0 sent. Finding the oldest file.");
				}
				LOGGER.info("Path: " + this.inputRoot.getAbsolutePath());

				final File[] recordDirs =
					this.inputRoot.listFiles(PublisherImpl.FILE_FILTER);


				if (recordDirs == null || recordDirs.length == 0) {
					return null;
				}

				Arrays.sort(recordDirs);

				final File firstFile = recordDirs[0];
				final String fileName = firstFile.getName();

				nextNonce = Long.valueOf(fileName);

				if(nextNonce == 0) {
					if(LOGGER.isEnabledFor(Level.ERROR)) {
						LOGGER.error("Found a record with the invalid nonce of 0.");
					}
					throw new RuntimeException("Nonce of 0 is reserved and cannot be associated with a record.");
				}

				if(LOGGER.isInfoEnabled()) {
					LOGGER.info("Oldest file found with nonce: " + nextNonce);
				}

			} else {
				nextNonce = Long.valueOf(lastNonce) + 1;
			}

			final File nonceDir =
				new File(this.inputRoot,
						NONCE_FORMATER.format(Long.valueOf(nextNonce)));

			if(!nonceDir.exists()) {
				if(LOGGER.isInfoEnabled()) {
					LOGGER.info("Directory structure for nonce: " + nextNonce +
							" does not exist. Returning null.");
				}
				return null;
			}

			String localNonce = Long.toString(nextNonce);

			//remote nonce must be in this format:
			//<record uid from system metadata file>_<timestamp from system metadata file>
			//ex: "f9032e9c-7e9a-4f2c-b40e-621b0e66c47b_2024-02-26T16:16:08.028292";
			File sysFile = new File(nonceDir, SYS_META_FILENAME);
			if(!sysFile.exists()) {
				throw new RuntimeException("SysMetadata file doesn't exist");
			}
			String remoteNonce = getRemoteNonceFromSystemMetadata(sysFile.getAbsolutePath());

			if (remoteNonce == null) {
				LOGGER.error("Failed to create remote nonce from system metadata file.");
				return null;
			}

			return new SourceRecordImpl(remoteNonce, localNonce, 0);

		} catch (final NumberFormatException e) {
			if(LOGGER.isEnabledFor(Level.ERROR)) {
				LOGGER.error("Nonce is not numeric - returning null");
			}
			return null;
		}
	}

	@Override
	public boolean onJournalResume(final PublisherSession sess, final String nonce,
					final long offset, final MimeHeaders headers) {

		//Looks up local nonce
		long localNonceNum = lookupLocalNonce(sess, nonce);

		if (localNonceNum == -1) {
			if (LOGGER.isEnabledFor(Level.ERROR)) {
				LOGGER.error("Journal record could not be found for remote nonce: " + nonce);
			}
			return false;
		}

		// Get the Journal record.
		String localNonce = String.valueOf(localNonceNum);
		SourceRecord rec = getJournalRecord(nonce, localNonce, offset);
		if (rec == null) {
			if (LOGGER.isEnabledFor(Level.ERROR)) {
				LOGGER.error("Journal record does not exist");
			}
			return false;
		}
		sess.sendRecord(rec);

		rec = getNextRecord(sess, localNonce);
		while ( rec != null ){
			String currNonce = rec.getNonce();
			sess.sendRecord(rec);
			rec = getNextRecord(sess, currNonce);
		}
		return true;
	}

	@Override
	public boolean onSubscribe(final PublisherSession sess, final String nonce, Mode mode,
			final MimeHeaders headers) {
		if (mode == Mode.Archive) {
			LOGGER.info("onSubscribe mode: archive");
		} else if (mode == Mode.Live) {
			LOGGER.info("onSubscribe mode: live");
		} else {
			LOGGER.error("onSubscribe mode: unset");
		}
		try{
			final long myNonce = Long.parseLong(nonce);
			if(myNonce < 0) {
				if(LOGGER.isEnabledFor(Level.ERROR)) {
					LOGGER.error("nonce must be a postive number");
				}
				sess.complete();
				return false;
			}
			SourceRecord rec = getNextRecord(sess, nonce);
			while (rec != null) {
				final String currNonce = rec.getNonce();
				sess.sendRecord(rec);
				rec = getNextRecord(sess, currNonce);
			}
		} catch (final NumberFormatException nfe) {
			if(LOGGER.isEnabledFor(Level.ERROR)) {
				LOGGER.error("nonce sent is not numeric - " + nonce);
			}
			sess.complete();
			return false;
		}

		return true;
	}

	@Override
	public boolean onRecordComplete(final PublisherSession sess, final String serailId,
			final SourceRecord record) {

		final Map<String, String> complete = new HashMap<String, String>();
		complete.put("recordComplete", "true");
		return dumpStatus(record.getNonce(), complete);
	}

	@Override
	public boolean sync(final PublisherSession sess, final String nonce,
			final MimeHeaders headers) {

		final Map<String, String> synched = new HashMap<String, String>();
		synched.put(SYNCED, "true");

		String localNonce = sess.getLocalNonce(nonce);
		if (localNonce == null) {
			LOGGER.error("Could not find local nonce for remote nonce: " + nonce);
			return false;
		}

		//Clear out remote/local nonce map entry
		sess.deleteNonceMapEntry(nonce);
		return dumpStatus(localNonce, synched);
	}

	@Override
	public void notifyDigest(final PublisherSession sess, final String nonce,
			final byte[] digest) {

		String hexString = "";
		for (byte b : digest) {
		    hexString = hexString + String.format("%02x",b);
		}

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Calculated digest for " + nonce
                        + ": " + hexString);
        }

        final Map<String, String> map = new HashMap<String, String>();
        map.put(LOCALDGST, hexString);
        dumpStatus(nonce, map);
	}

	@Override
	public void notifyPeerDigest(final PublisherSession sess,
			final Map<String, DigestPair> digestPairs) {

		for(final String key : digestPairs.keySet()) {
			final DigestPair pair = digestPairs.get(key);
			if (LOGGER.isInfoEnabled()) {
				LOGGER.info("Digest status for " + pair.getNonce()
						+ ": " + pair.getDigestStatus());
			}
			final String hexString = (new BigInteger(1, pair.getPeerDigest())).toString(16);
			final Map<String, String> map = new HashMap<String, String>();
	        map.put(PEERDGST, hexString);

	        String localNonce = sess.getLocalNonce(pair.getNonce());
			if (localNonce != null) {
				dumpStatus(NONCE_FORMATER.format(Long.valueOf(localNonce)), map);
			} else {
				LOGGER.error("No local nonce exists for remote nonce: " + pair.getNonce());
			}
		}
	}
	/**
	 * Write status information about a record out to disk.
	 * @param lri The {@link LocalRecordInfo} object output stats for.
	 * @return <code>true</code> If the data was successfully written out.
	 *         <code>false</code> otherwise.
	 */
	@SuppressWarnings("unchecked")
	final boolean dumpStatus(final String nonce, final Map<String, String> statusMap) {

    	final JSONParser p  = new JSONParser();
    	JSONObject status;
    	File statusFile;
    	BufferedOutputStream w;
    	FileReader r;

    	final File nonceDir =
            new File(this.inputRoot,
            		NONCE_FORMATER.format(Long.valueOf(nonce)));

    	try {
    		statusFile = new File(nonceDir, STATUS_FILENAME);
    		if(!statusFile.exists()) {
    			if (LOGGER.isInfoEnabled()) {
    				LOGGER.info("The '" + STATUS_FILENAME + "' file does not exist so creating it.");
    			}
    			statusFile.createNewFile();
    			status = new JSONObject();
    		} else {
    			r = new FileReader(statusFile);
				try {
					status = (JSONObject)p.parse(r);
				} catch (final ParseException e ) {
					if (LOGGER.isInfoEnabled()) {
						LOGGER.info("'" + STATUS_FILENAME + "' file");
					}
					status = new JSONObject();
				}
    			r.close();
    		}

    		status.putAll(statusMap);
    		w = new BufferedOutputStream(new FileOutputStream(statusFile));
    		w.write(status.toJSONString().getBytes("utf-8"));
    		w.close();
    	} catch (final FileNotFoundException e) {
    		LOGGER.error("Failed to open status file for writing:"
                    + e.getMessage());
    		return false;
        } catch (final UnsupportedEncodingException e) {
        	LOGGER.error("cannot find UTF-8 encoder?");
        	return false;
        } catch (final IOException e) {
        	LOGGER.error("failed to write to the status file, aborting");
            return false;
		}

        return true;
    }

	private class SourceRecordImpl implements SourceRecord {

		private final String remoteNonce;
		private final String localNonce;
		private final long offset;
		final File nonceDir;
		final File sysFile;
		final File appFile;
		final File payloadFile;

		@SuppressWarnings("unchecked")
		public SourceRecordImpl(final String remoteNonce, final String localNonce, final long offset) {

			this.remoteNonce = remoteNonce;
			this.localNonce = localNonce;
			this.offset = offset;

			this.nonceDir =
                new File(PublisherImpl.this.inputRoot,
                		PublisherImpl.NONCE_FORMATER.format(Long.valueOf(localNonce)));

			if (LOGGER.isInfoEnabled()) {
				LOGGER.info("Getting files for sysMetadata, appMetadata, and payload.");
			}
			this.sysFile = new File(this.nonceDir, SYS_META_FILENAME);
			if(!this.sysFile.exists()) {
				throw new RuntimeException("SysMetadata file doesn't exist");
			}

			this.appFile = new File(this.nonceDir, APP_META_FILENAME);

			this.payloadFile = new File(this.nonceDir, PAYLOAD_FILENAME);
			if(offset > 0 && PublisherImpl.this.recordType == RecordType.Journal
					&& !this.payloadFile.exists()) {
				throw new RuntimeException("Payload file doesn't exist");
			}

		}

		@Override
		public String getNonce() {
			return this.localNonce;
		}

		@Override
		public String getRemoteNonce() {
			return this.remoteNonce;
		}

		@Override
		public long getOffset() {
			return this.offset;
		}

		@Override
		public RecordType getRecordType() {
			return recordType;
		}

		@Override
		public long getSysMetaLength() {
			if(this.sysFile == null) {
				return 0;
			}
			return this.sysFile.length();
		}

		@Override
		public long getAppMetaLength() {
			if(this.appFile == null) {
				return 0;
			}
			return this.appFile.length();
		}

		@Override
		public long getPayloadLength() {
			if(this.payloadFile == null) {
				return 0;
			}
			return this.payloadFile.length();
		}

		@Override
		public InputStream getSysMetadata() {

			try {
				return new FileInputStream(this.sysFile);
			} catch (final FileNotFoundException e) {
				if(LOGGER.isEnabledFor(Level.ERROR)) {
					LOGGER.error("No SysMetadata file has been set.");
				}
				//Throw an error since sysmetadata file is required
				throw new RuntimeException("SysMetadata file does not exist.");
			}
		}

		@Override
		public InputStream getAppMetadata() {
			try {
				return new FileInputStream(this.appFile);
			} catch (final FileNotFoundException e) {
				if(LOGGER.isInfoEnabled()) {
					LOGGER.info("No AppMetadata file has been set.");
				}
				return null;
			}
		}

		@Override
		public InputStream getPayload() {
			try {
				final InputStream is = new FileInputStream(this.payloadFile);
				return is;
			} catch (final FileNotFoundException e) {
				if(LOGGER.isInfoEnabled()) {
					LOGGER.info("No payload file has been set.");
				}
				return null;
			}
		}
	}

}
