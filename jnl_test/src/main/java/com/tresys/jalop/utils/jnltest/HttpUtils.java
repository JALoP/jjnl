package com.tresys.jalop.utils.jnltest;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.servlet.http.HttpServletResponse;
import javax.xml.soap.MimeHeader;
import javax.xml.soap.MimeHeaders;

import org.beepcore.beep.core.InputDataStreamAdapter;
import org.beepcore.beep.core.OutputDataStream;

import com.tresys.jalop.jnl.ConnectionHandler.ConnectError;
import com.tresys.jalop.jnl.DigestStatus;
import com.tresys.jalop.jnl.Mode;
import com.tresys.jalop.jnl.RecordType;
import com.tresys.jalop.jnl.Role;
import com.tresys.jalop.jnl.exceptions.MissingMimeHeaderException;
import com.tresys.jalop.jnl.exceptions.UnexpectedMimeValueException;
import com.tresys.jalop.jnl.impl.messages.DigestMessage;
import com.tresys.jalop.jnl.impl.messages.DigestResponse;
import com.tresys.jalop.jnl.impl.messages.InitAckMessage;
import com.tresys.jalop.jnl.impl.messages.InitMessage;
import com.tresys.jalop.jnl.impl.messages.InitNackMessage;
import com.tresys.jalop.jnl.impl.messages.JournalResumeMessage;

/**
 * Utility class for creating and parsing JALoP/BEEP messages.
 */
public class HttpUtils {

    public static final String AUDIT = "audit";
    public static final String BINARY = "binary";
    public static final String BREAK = "BREAK";
    public static final String CONFIRMED = "confirmed";
    public static final String CONFIRMED_EQUALS = CONFIRMED + "=";
    public static final String DGST_CHAN_FORMAT_STR = "digest:";
    public static final String DGST_SHA256 = "sha256";
    public static final String ENC_XML = "xml";
    public static final String INVALID = "invalid";
    public static final String INVALID_EQUALS = INVALID + "=";
    public static final String JOURNAL = "journal";
    public static final String LOG = "log";
    public static final String UNKNOWN = "unknown";
    public static final String UNKNOWN_EQUALS = UNKNOWN + "=";

    public static final String HDRS_ACCEPT_DIGEST = "JAL-Accept-Digest";
    public static final String HDRS_ACCEPT_ENCODING = "JAL-Accept-Encoding";
    public static final String HDRS_AGENT = "JAL-Agent";
    public static final String HDRS_APP_META_LEN = "JAL-Application-Metadata-Length";
    public static final String HDRS_AUDIT_LEN = "JAL-Audit-Length";
    public static final String HDRS_CONTENT_TXFR_ENCODING = "Content-Transfer-Encoding";
    public static final String HDRS_CONTENT_TYPE = "Content-Type";
    public static final String HDRS_COUNT = "JAL-Count";
    public static final String HDRS_DATA_CLASS = "JAL-Data-Class";
    public static final String HDRS_DIGEST = "JAL-Digest";
    public static final String HDRS_ENCODING = "JAL-Encoding";
    public static final String HDRS_JOURNAL_LEN = "JAL-Journal-Length";
    public static final String HDRS_JOURNAL_OFFSET = "JAL-Journal-Offset";
    public static final String HDRS_LOG_LEN = "JAL-Log-Length";
    public static final String HDRS_MESSAGE = "JAL-Message";
    public static final String HDRS_MODE = "JAL-Mode";
    public static final String HDRS_NONCE = "JAL-Id";
    public static final String HDRS_SYS_META_LEN = "JAL-System-Metadata-Length";
    public static final String HDRS_UNAUTHORIZED_MODE = "JAL-Unauthorized-Mode";
    public static final String HDRS_UNSUPPORTED_DIGEST = "JAL-Unsupported-Digest";
    public static final String HDRS_UNSUPPORTED_ENCODING = "JAL-Unsupported-Encoding";
    public static final String HDRS_UNSUPPORTED_MODE = "JAL-Unsupported-Mode";
    public static final String HDRS_UNSUPPORTED_VERSION = "JAL-Unsupported-Version";


    public static final String DEFAULT_CONTENT_TYPE =
            "application/octet-stream";

    /**
     * The default <code>DataStream</code> content transfer encoding
     * ("binary").
     */
    public static final String DEFAULT_CONTENT_TRANSFER_ENCODING = "binary";

    public static final String MSG_DIGEST = "digest";
    public static final String MSG_DIGEST_RESP = "digest-response";
    public static final String MSG_INIT = "initialize";
    public static final String MSG_INIT_ACK = "initialize-ack";
    public static final String MSG_INIT_NACK = "initialize-nack";
    public static final String MSG_JOURNAL = "journal-record";
    public static final String MSG_JOURNAL_RESUME = "journal-resume";
    public static final String MSG_LOG = "log-record";
    public static final String MSG_SYNC = "sync";
    public static final String MSG_PUBLISH = "publish";
    public static final String MSG_SUBSCRIBE = "subscribe";
    public static final String MSG_PUBLISH_LIVE = "publish-live";
    public static final String MSG_SUBSCRIBE_LIVE = "subscribe-live";
    public static final String MSG_PUBLISH_ARCHIVE = "publish-archival";
    public static final String MSG_SUBSCRIBE_ARCHIVE = "subscribe-archival";

    public static final String NONCE = "nonce";
    public static final String STATUS = "status";

    /**
     * Utility function to perform common tasks related to parsing incoming
     * messages.
     *
     * @param is
     *            The {@link InputDataStreamAdapter} for this message.
     * @param expectedMessage
     *            The expected message type (initialize, subscribe, etc)
     * @param expectedHeaders
     *            An array of expected (not necessarily required) MIME headers.
     * @return an array of 2 {@link MimeHeaders}, the first {@link MimeHeader}
     *         is the expected headers, the second is any remaining headers.
     * @throws MissingMimeHeaderException
     *             If the MIME headers to not contain a JAL-Message header.
     * @throws UnexpectedMimeValueException
     *             If the value of the JAL-Message header is not
     *             <code>expectedMessage</code>.
     * @see {@link Utils#splitHeaders(InputDataStreamAdapter, String...)}
     */
    static MimeHeaders[] processMessageCommon(HashMap<String, String> httpHeaders,
            final String expectedMessage, final String... expectedHeaders)
            throws MissingMimeHeaderException, UnexpectedMimeValueException {
        final String messageType = httpHeaders.get(HDRS_MESSAGE);
        if (messageType == null) {
            throw new MissingMimeHeaderException(HDRS_MESSAGE);
        }

        if (!messageType.equalsIgnoreCase(expectedMessage)) {
            throw new UnexpectedMimeValueException(HDRS_MESSAGE,
                    expectedMessage, messageType);
        }

        final MimeHeaders[] headers = splitHeaders(httpHeaders, expectedHeaders);
        return headers;
    }

    /**
     * Create an {@link OutputDataStream} for an initialize-ack message. The
     * returned object is already marked as complete since an initialize-ack
     * message carries no payload.
     *
     * @param digest
     *            The selected digest algorithm. This must be a non-empty string
     *            that contains at least one non-whitespace character.
     * @param encoding
     *            The selected XML encoding. This must be a non-empty string
     *            that contains at least one non-whitespace character.
     * @return The {@link OutputDataStream}
     */
    public static void createInitAckMessage(String digest,
            String encoding, HttpServletResponse response) {

    /*    final org.beepcore.beep.core.MimeHeaders headers = new javax.xml.soap.MimeHeaders(
                CT_JALOP,
                org.beepcore.beep.core.MimeHeaders.DEFAULT_CONTENT_TRANSFER_ENCODING); */
        digest = checkForEmptyString(digest, "digest");
        encoding = checkForEmptyString(encoding, "encoding");

        response.setHeader(HDRS_MESSAGE, MSG_INIT_ACK);
        response.setHeader(HDRS_DIGEST, digest);
        response.setHeader(HDRS_ENCODING, encoding);

        //TODO, need to determine how to set journal resume here

        //Embed subscribe for now
        response.setHeader(HDRS_MESSAGE, MSG_SUBSCRIBE);
    }

    /**
     * Create an {@link OutputDataStream} for an initialize message. The
     * returned object is already marked as complete since an initialize message
     * carries no payload.
     *
     * @param role
     *            The {@link Role} in ('JAL-Mode') to send.
     * @param mode
     *            The {@link Mode} in ('JAL-Mode') to send.
     * @param dataClass
     *            The type of records to transfer over this channel.
     * @param digestAlgorithms
     *            The list of digest algorithms to propose. This may be
     *            <code>null</code> or empty, in which case no algorithms are
     *            proposed. If it is not empty, then each element must be
     *            <code>non-null</code> and not be the empty string.
     * @param agent
     *            The string to send for the "JAL-Agent" header, this may be
     *            <code>null</code>
     * @param xmlEncodings
     *            The list of digest algorithms to propose. This may be
     *            <code>null</code> or empty, in which case no algorithms are
     *            proposed. If it is not empty, then each element must be
     *            <code>non-null</code> and not be the empty string.
     * @return The {@link OutputDataStream}
     */
  /*  public static void createInitMessage(final Role role, final Mode mode,
            final RecordType dataClass, final List<String> xmlEncodings,
            final List<String> digestAlgorithms, final String agent, HttpServletRequest request) {

       final org.beepcore.beep.core.MimeHeaders headers = new org.beepcore.beep.core.MimeHeaders(
                CT_JALOP,
                org.beepcore.beep.core.MimeHeaders.DEFAULT_CONTENT_TRANSFER_ENCODING);
        final String encodingsString = makeStringList(xmlEncodings, "encodings");
        final String digestsString = makeStringList(digestAlgorithms, "digests");

        headers.setHeader(HDRS_MESSAGE, MSG_INIT);
        if (encodingsString != null) {
            headers.setHeader(HDRS_ACCEPT_ENCODING, encodingsString);
        }
        if (digestsString != null) {
            headers.setHeader(HDRS_ACCEPT_DIGEST, digestsString);
        }
        if (Role.Publisher == role && Mode.Live == mode) {
            headers.setHeader(HDRS_MODE, MSG_PUBLISH_LIVE);
        } else if (Role.Publisher == role && Mode.Archive == mode) {
            headers.setHeader(HDRS_MODE, MSG_PUBLISH_ARCHIVE);
        } else if (Role.Subscriber == role && Mode.Live == mode) {
            headers.setHeader(HDRS_MODE, MSG_SUBSCRIBE_LIVE);
        } else if (Role.Subscriber == role && Mode.Archive == mode) {
            headers.setHeader(HDRS_MODE, MSG_SUBSCRIBE_ARCHIVE);
        } else {
            throw new IllegalArgumentException("Illegal value for 'JAL-Mode'");
        }
        switch (dataClass) {
        case Journal:
            headers.setHeader(HDRS_DATA_CLASS, JOURNAL);
            break;
        case Audit:
            headers.setHeader(HDRS_DATA_CLASS, AUDIT);
            break;
        case Log:
            headers.setHeader(HDRS_DATA_CLASS, LOG);
            break;
        default:
            throw new IllegalArgumentException("Illegal value for 'dataClass'");
        }
        if (agent != null) {
            headers.setHeader(HDRS_AGENT, agent);
        }

        final OutputDataStream ods = new OutputDataStream(headers, new BufferSegment(new byte[0]));

        ods.setComplete();
        return ods;
    }   */

    /**
     * Create an {@link OutputDataStream} for an initialize-nack message. The
     * returned object is already marked as complete since an initialize-nack
     * message carries no payload.
     *
     * @param errors
     *            The list of {@link ConnectError}s to send in this message.
     * @return The {@link OutputDataStream}
     */

    public static void createInitNackMessage(
            final List<ConnectError> errors, HttpServletResponse response) {
        if ((errors == null) || errors.isEmpty()) {
            throw new IllegalArgumentException(
                    "Must specify at least one error for an '" + MSG_INIT_NACK
                            + "' message.");
        }
     /*   final org.beepcore.beep.core.MimeHeaders headers = new org.beepcore.beep.core.MimeHeaders(
                CT_JALOP,
                org.beepcore.beep.core.MimeHeaders.DEFAULT_CONTENT_TRANSFER_ENCODING); */
        response.setHeader(HDRS_MESSAGE, MSG_INIT_NACK);

        for (final ConnectError connectError : errors) {
            switch (connectError) {
            case UnauthorizedMode:
                response.setHeader(HDRS_UNAUTHORIZED_MODE, "");
                break;
            case UnsupportedMode:
                response.setHeader(HDRS_UNSUPPORTED_MODE, "");
                break;
            case UnsupportedDigest:
                response.setHeader(HDRS_UNSUPPORTED_DIGEST, "");
                break;
            case UnsupportedEncoding:
                response.setHeader(HDRS_UNSUPPORTED_ENCODING, "");
                break;
            case UnsupportedVersion:
                response.setHeader(HDRS_UNSUPPORTED_VERSION, "");
                break;
            default:
                throw new IllegalArgumentException(
                        "Cannot specify 'accept' as an error");
            }
        }
    }

    /**
     * Process an initialize-ack message.
     *
     * @return an {@link InitAckMessage}
     * @param is
     *            The BEEP {@link InputDataStreamAdapter} that holds the
     *            message.
     * @throws MissingMimeHeaderException
     *             If a required MIME header is missing.
     * @throws UnexpectedMimeValueException
     *             If a MIME header has an unexpected value.
     */
    public static InitAckMessage processInitAck(final HashMap<String, String> httpHeaders)
            throws MissingMimeHeaderException,
            UnexpectedMimeValueException {
        final MimeHeaders[] headers = processMessageCommon(httpHeaders, MSG_INIT_ACK,
                HDRS_MESSAGE, HDRS_ENCODING, HDRS_DIGEST);

        final MimeHeaders knownHeaders = headers[0];
        final MimeHeaders unknownHeaders = headers[1];

        String encoding;
        if (knownHeaders.getHeader(HDRS_ENCODING) != null) {
            encoding = knownHeaders.getHeader(HDRS_ENCODING)[0];
        } else {
            throw new MissingMimeHeaderException(HDRS_ENCODING);
        }

        String digest;
        if (knownHeaders.getHeader(HDRS_DIGEST) != null) {
            digest = knownHeaders.getHeader(HDRS_DIGEST)[0];
        } else {
            throw new MissingMimeHeaderException(HDRS_DIGEST);
        }
        return new InitAckMessage(encoding, digest, unknownHeaders);
    }

    /**
     * Process an initialize message.
     *
     * @param is
     *            The BEEP {@link InputDataStreamAdapter} that holds the
     *            message.
     * @return an {@link InitMessage}
     * @throws UnexpectedMimeValueException
     *             If the message contains illegal values for known MIME headers
     * @throws MissingMimeHeaderException
     *             If {@link Message} is missing a required MIME header.
     */
    public static InitMessage processInitMessage(final HashMap<String, String> httpHeaders)
            throws UnexpectedMimeValueException,
            MissingMimeHeaderException {
        final MimeHeaders[] headers = processMessageCommon(httpHeaders, MSG_INIT,
                HDRS_ACCEPT_ENCODING, HDRS_MODE, HDRS_DATA_CLASS,
                HDRS_ACCEPT_DIGEST, HDRS_AGENT);
        final MimeHeaders knownHeaders = headers[0];
        final MimeHeaders unknownHeaders = headers[1];

        final String[] encodings = knownHeaders.getHeader(HDRS_ACCEPT_ENCODING);

        final String[] digests = knownHeaders.getHeader(HDRS_ACCEPT_DIGEST);

        final String[] hdrsMode = knownHeaders.getHeader(HDRS_MODE);
        if (hdrsMode == null) {
            throw new MissingMimeHeaderException(HDRS_MODE);
        }
        Role role;
        Mode mode;
        if (hdrsMode[0].equalsIgnoreCase(MSG_PUBLISH_LIVE)) {
            role = Role.Publisher;
            mode = Mode.Live;
        } else if (hdrsMode[0].equalsIgnoreCase(MSG_PUBLISH_ARCHIVE)) {
            role = Role.Publisher;
            mode = Mode.Archive;
        } else if (hdrsMode[0].equalsIgnoreCase(MSG_SUBSCRIBE_LIVE)) {
            role = Role.Subscriber;
            mode = Mode.Live;
        } else if (hdrsMode[0].equalsIgnoreCase(MSG_SUBSCRIBE_ARCHIVE)) {
            role = Role.Subscriber;
            mode = Mode.Archive;
        } else {
            throw new UnexpectedMimeValueException(HDRS_MODE, MSG_PUBLISH_LIVE + ", or "
                    + ", or " + MSG_PUBLISH_ARCHIVE + ", or " + MSG_SUBSCRIBE_LIVE
                    + ", or " + MSG_SUBSCRIBE_ARCHIVE, hdrsMode[0]);
        }

        final String[] dataClass = knownHeaders.getHeader(HDRS_DATA_CLASS);
        if (dataClass == null) {
            throw new MissingMimeHeaderException(HDRS_DATA_CLASS);
        }
        RecordType recordType;
        if (dataClass[0].equalsIgnoreCase(JOURNAL)) {
            recordType = RecordType.Journal;
        } else if (dataClass[0].equalsIgnoreCase(AUDIT)) {
            recordType = RecordType.Audit;
        } else if (dataClass[0].equalsIgnoreCase(LOG)) {
            recordType = RecordType.Log;
        } else {
            throw new UnexpectedMimeValueException(HDRS_DATA_CLASS, JOURNAL
                    + ", " + AUDIT + ", or " + LOG, dataClass[0]);
        }

        final String[] agent = knownHeaders.getHeader(HDRS_AGENT);
        String agentString = null;
        if (agent != null) {
            agentString = agent[0];
        }

        return new InitMessage(recordType, role, mode, encodings, digests,
                agentString, unknownHeaders);

    }

    /**
     * Helper utility to check that a passed in string is non-null and contains
     * non-whitespace characters. This method returns the original string with
     * leading/trailing whitespace removed.
     *
     * @param toCheck
     *            The string to check.
     * @param parameterName
     *            A human readable name to add to the exception.
     * @return <code>toCheck</code> with leading/trailing whitespace removed.
     * @throws IllegalArgumentException
     *             if <code>toCheck</code> is <code>null</code> or is comprised
     *             entirely of whitespace.
     */
    public static String checkForEmptyString(String toCheck,
            final String parameterName) {
        if (toCheck == null) {
            throw new IllegalArgumentException("'" + parameterName
                    + "' cannot be null");
        }
        toCheck = toCheck.trim();
        if (toCheck.length() == 0) {
            throw new IllegalArgumentException("'" + parameterName
                    + "' must contain non-whitespace characaters");
        }
        return toCheck;
    }

    /**
     * Helper utility to build a comma separated list of strings.
     *
     * @param stringList
     *            The list of strings to join.
     * @param listName
     *            A name for the list, this is used if the list contains
     *            <code>null</code> or empty strings.
     * @return A {@link String} that is the comma separated list of the values
     *         in <code>stringList</code>
     */
    public static String makeStringList(final List<String> stringList,
            final String listName) {
        if ((stringList == null) || stringList.isEmpty()) {
            return null;
        }
        final Iterator<String> iter = stringList.iterator();
        final StringBuilder sb = new StringBuilder();
        while (iter.hasNext()) {
            String s = iter.next();
            s = checkForEmptyString(s, listName);
            sb.append(s);
            if (iter.hasNext()) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }

    /**
     * Process a subscribe message.
     *
     * @return an {@link InitAckMessage}
     * @param is
     *            The BEEP {@link InputDataStreamAdapter} that holds the
     *            message.
     * @throws MissingMimeHeaderException
     *             If a required MIME header is missing.
     * @throws UnexpectedMimeValueException
     *             If a MIME header has an unexpected value.
     */
  /*  static public SubscribeMessage processSubscribe(
            final HashMap<String, String> httpHeaders) throws
            MissingMimeHeaderException, UnexpectedMimeValueException {
        final MimeHeaders[] headers = processMessageCommon(httpHeaders, MSG_SUBSCRIBE,
                    HDRS_MESSAGE);

        final MimeHeaders knownHeaders = headers[0];
        final MimeHeaders unknownHeaders = headers[1];

        String nonce = "0";
        return new SubscribeMessage(nonce, unknownHeaders);
    } */

    /**
     * Create an {@link OutputDataStream} for a 'journal-resume' message. Note
     * that the {@link OutputDataStream} returned by this function has already
     * had {@link OutputDataStream#setComplete()} called on it since a
     * 'journal-resume' message contains no payload.
     *
     * @param nonce
     *            The nonce to send in the 'journal-resume' message. This
     *            must be non-null and contain at least one non-whitespace
     *            character.
     * @param offset
     *            The number offset to begin transferring data from for the
     *            journal record.
     * @return The {@link OutputDataStream}.
     */
  /*  static public OutputDataStream createJournalResumeMessage(final String nonce,
            final long offset) {
        checkForEmptyString(nonce, HDRS_NONCE);

        if (offset < 0) {
            throw new IllegalArgumentException("offsset for '"
                    + MSG_JOURNAL_RESUME + "' must be positive");
        }
        final org.beepcore.beep.core.MimeHeaders headers = new org.beepcore.beep.core.MimeHeaders(
                CT_JALOP,
                org.beepcore.beep.core.MimeHeaders.DEFAULT_CONTENT_TRANSFER_ENCODING);
        headers.setHeader(HDRS_MESSAGE, MSG_JOURNAL_RESUME);
        headers.setHeader(HDRS_NONCE, nonce);
        headers.setHeader(HDRS_JOURNAL_OFFSET, Long.toString(offset));
        final OutputDataStream ods = new OutputDataStream(headers, new BufferSegment(new byte[0]));
        ods.setComplete();
        return ods;
    }
    TODO, this needs placed inside the initializeAck message on the response
    *
    */

    /**
     * Extract the details of an initialize-nack message.
     *
     * @return an {@link InitNackMessage}
     * @param is
     *            The BEEP {@link InputDataStreamAdapter} that holds the
     *            message.
     * @throws MissingMimeHeaderException
     *             If a required MIME header is missing.
     * @throws UnexpectedMimeValueException
     *             If a MIME header has an unexpected value.
     */
    static public InitNackMessage processInitNack(
            final HashMap<String, String> httpHeaders) throws
            MissingMimeHeaderException, UnexpectedMimeValueException {
        final MimeHeaders[] headers = processMessageCommon(httpHeaders, MSG_INIT_NACK, HDRS_MESSAGE,
                HDRS_UNSUPPORTED_VERSION, HDRS_UNSUPPORTED_ENCODING,
                HDRS_UNSUPPORTED_MODE, HDRS_UNAUTHORIZED_MODE,
                HDRS_UNSUPPORTED_DIGEST);

        final MimeHeaders knownHeaders = headers[0];
        final MimeHeaders unknownHeaders = headers[1];

        final List<ConnectError> errors = new LinkedList<ConnectError>();
        if (knownHeaders.getHeader(HDRS_UNSUPPORTED_VERSION) != null) {
            errors.add(ConnectError.UnsupportedVersion);
        }
        if (knownHeaders.getHeader(HDRS_UNSUPPORTED_MODE) != null) {
            errors.add(ConnectError.UnsupportedMode);
        }
        if (knownHeaders.getHeader(HDRS_UNAUTHORIZED_MODE) != null) {
            errors.add(ConnectError.UnauthorizedMode);
        }

        if (knownHeaders.getHeader(HDRS_UNSUPPORTED_ENCODING) != null) {
            errors.add(ConnectError.UnsupportedEncoding);
        }
        if (knownHeaders.getHeader(HDRS_UNSUPPORTED_DIGEST) != null) {
            errors.add(ConnectError.UnsupportedDigest);
        }
        if (errors.isEmpty()) {
            throw new MissingMimeHeaderException("Error Headers");
        }
        return new InitNackMessage(errors, unknownHeaders);
    }

    /**
     * Process a 'journal-resume' message.
     *
     * @return an {@link JournalResumeMessage}
     * @param is
     *            The BEEP {@link InputDataStreamAdapter} that holds the
     *            message.
     * @throws MissingMimeHeaderException
     *             If a required MIME header is missing.
     * @throws UnexpectedMimeValueException
     *             If a MIME header has an unexpected value.
     */
    static public JournalResumeMessage processJournalResume(
            final HashMap<String, String> httpHeaders) throws
            MissingMimeHeaderException, UnexpectedMimeValueException {

        final MimeHeaders[] headers = processMessageCommon(httpHeaders,
                MSG_JOURNAL_RESUME, HDRS_MESSAGE, HDRS_NONCE,
                HDRS_JOURNAL_OFFSET);

        final MimeHeaders knownHeaders = headers[0];
        final MimeHeaders unknownHeaders = headers[1];

        String nonce;
        if (knownHeaders.getHeader(HDRS_NONCE) == null) {
            throw new MissingMimeHeaderException(HDRS_NONCE);
        }
        nonce = knownHeaders.getHeader(HDRS_NONCE)[0].trim();
        if (nonce.length() == 0) {
            throw new UnexpectedMimeValueException(HDRS_NONCE,
                    "non-empty-string", nonce);
        }

        String offsetStr;
        if (knownHeaders.getHeader(HDRS_JOURNAL_OFFSET) == null) {
            throw new MissingMimeHeaderException(HDRS_JOURNAL_OFFSET);
        }
        offsetStr = knownHeaders.getHeader(HDRS_JOURNAL_OFFSET)[0].trim();
        if (offsetStr.length() == 0) {
            throw new UnexpectedMimeValueException(HDRS_JOURNAL_OFFSET,
                    "non-empty-string", nonce);
        }
        long offset = -1;
        try {
            offset = Long.parseLong(offsetStr);
        } catch (final NumberFormatException e) {
            // Do nothing here, the following 'if' statement will generate an
            // exception if needed.
        }
        if (offset < 0) {
            throw new UnexpectedMimeValueException(HDRS_JOURNAL_OFFSET,
                    "positive integer", offsetStr);
        }
        return new JournalResumeMessage(nonce, offset, unknownHeaders);
    }

    /**
     * Process a Sync Message.
     *
     * @param is
     *            The BEEP {@link InputDataStreamAdapter} that holds the sync
     *            message.
     * @return a {@link SyncMessage} that holds the Nonce that is being synced
     *             If there is an error from the underlying BEEP connection.
     * @throws UnexpectedMimeValueException
     *             If the message contains illegal values for known MIME headers
     * @throws MissingMimeHeaderException
     *             If {@link Message} is missing a required MIME header.
     */

 /*   static public SyncMessage processSyncMessage(final HashMap<String, String> httpHeaders)
            throws MissingMimeHeaderException, UnexpectedMimeValueException {

        final MimeHeaders[] headers = processMessageCommon(httpHeaders, MSG_SYNC,
                HDRS_MESSAGE, HDRS_NONCE);

        final MimeHeaders knownHeaders = headers[0];
        final MimeHeaders unknownHeaders = headers[1];

        String nonce;
        if (knownHeaders.getHeader(HDRS_NONCE) == null) {
            throw new MissingMimeHeaderException(HDRS_NONCE);
        }
        nonce = knownHeaders.getHeader(HDRS_NONCE)[0].trim();
        if (nonce.length() == 0) {
            throw new UnexpectedMimeValueException(HDRS_NONCE,
                    "non-empty-string", nonce);
        }
        return new SyncMessage(nonce, unknownHeaders);
    } */

    /**
     * Create a sync message from a String (Nonce). Note
     * that the {@link OutputDataStream} returned by this function has already
     * had {@link OutputDataStream#setComplete()} called on it since a
     * 'sync' message contains no payload.
     *
     * @param nonce
     *            The String that holds the synced nonce
     * @return an {@link OutputDataStream} that holds the sync message
     */
/*    static public OutputDataStream createSyncMessage(final String nonce) {
        final org.beepcore.beep.core.MimeHeaders mh = new org.beepcore.beep.core.MimeHeaders();
        mh.setContentType(CT_JALOP);
        mh.setHeader(HDRS_MESSAGE, MSG_SYNC);
        mh.setHeader(HDRS_NONCE, checkForEmptyString(nonce, NONCE));

        final OutputDataStream ret = new OutputDataStream(mh, new BufferSegment(new byte[0]));
        ret.setComplete();

        return ret;
    } */

    /**
     * Process a Digest Message.
     *
     * @param is
     *            The BEEP {@link InputDataStreamAdapter} that holds the
     *            message.
     * @return an {@link Map<String, String>}
     *             If there is an error from the underlying BEEP connection.
     * @throws UnexpectedMimeValueException
     *             If the message contains illegal values for known MIME headers
     * @throws MissingMimeHeaderException
     *             If {@link Message} is missing a required MIME header.
     */
    static public DigestMessage processDigestMessage(
            final HashMap<String, String> httpHeaders, InputStream is) throws MissingMimeHeaderException,
            UnexpectedMimeValueException, IOException{

        final MimeHeaders[] headers = processMessageCommon(httpHeaders, MSG_DIGEST,
                HDRS_MESSAGE, HDRS_COUNT);
        final MimeHeaders knownHeaders = headers[0];
        final MimeHeaders unknownHeaders = headers[1];
        final int count = Integer.valueOf(knownHeaders.getHeader(HDRS_COUNT)[0]
                .trim());

        // get the digest map from the input stream
        final Map<String, String> digestMap = new HashMap<String, String>();
        final int numLeft = is.available();
        final byte[] messageArray = new byte[numLeft];
        try {
            is.read(messageArray);
        } catch (final IOException e) {
            e.printStackTrace();
        }
        final String msgStr = new String(messageArray);

        final String[] pairs = checkForEmptyString(msgStr, "payload").split("\\s+|=");

        for (int x = 0; x < count * 2; x += 2) {
            pairs[x] = checkForEmptyString(pairs[x], MSG_DIGEST);
            pairs[x + 1] = checkForEmptyString(pairs[x + 1], NONCE);
            digestMap.put(pairs[x + 1], pairs[x]);
        }


        return new DigestMessage(digestMap, unknownHeaders);
    }

    /**
     * Create a digest message from a Map<String (nonce), String (digest)>.
     *
     * @param digestMap
     *            The Map<String, String> that holds the nonce to digest
     *            mappings
     * @return an {@link OutputDataStream}
     */
  /*  static public OutputDataStream createDigestMessage(
            final Map<String, String> digestMap) {

        final StringBuilder message = new StringBuilder();
        final org.beepcore.beep.core.MimeHeaders mh = new org.beepcore.beep.core.MimeHeaders();
        mh.setContentType(CT_JALOP);
        mh.setHeader(HDRS_MESSAGE, MSG_DIGEST);
        mh.setHeader(HDRS_COUNT, String.valueOf(digestMap.size()));

        final Iterator<String> nonces = digestMap.keySet().iterator();
        while (nonces.hasNext()) {
            final String id = nonces.next();
            message.append(checkForEmptyString(digestMap.get(id), MSG_DIGEST));
            message.append("=");
            message.append(checkForEmptyString(id, NONCE));
            message.append("\r\n");
        }

        OutputDataStream ret;
        try {
            ret = new OutputDataStream(mh, new BufferSegment(message.toString().getBytes("utf-8")));
        } catch (final UnsupportedEncodingException e) {
            // We should never get here
            e.printStackTrace();
            return null;
        }
        ret.setComplete();

        return ret;
    } */

    /**
     * Process a Digest Response.
     *
     * @param is
     *            The BEEP {@link InputDataStreamAdapter} that holds the
     *            message.
     * @param messagePayload
     *            Holds the combined payload string for segmented
     *            messages.
     * @return an {@link DigestResponse}
     *             If there is an error from the underlying BEEP connection.
     * @throws UnexpectedMimeValueException
     *             If the message contains illegal values for known MIME headers
     * @throws MissingMimeHeaderException
     *             If {@link Message} is missing a required MIME
     *             header.
     */
    static public DigestResponse processDigestResponse(
            final HashMap<String, String> httpHeaders, String messagePayload)
                throws MissingMimeHeaderException, UnexpectedMimeValueException{

        final MimeHeaders[] headers = processMessageCommon(httpHeaders,
                MSG_DIGEST_RESP, HDRS_MESSAGE, HDRS_COUNT);

        final MimeHeaders knownHeaders = headers[0];
        final MimeHeaders unknownHeaders = headers[1];

        final int count = Integer.valueOf(knownHeaders.getHeader(HDRS_COUNT)[0].trim());

        return new DigestResponse(getDigestStatuses(count, messagePayload), unknownHeaders);

    }

    private static Map<String, DigestStatus> getDigestStatuses(
            final int count, String messagePayload)
                {
        final Map<String, DigestStatus> ret = new HashMap<String, DigestStatus>();
        ret.clear();

        final String[] pairs = checkForEmptyString(messagePayload, "payload").split("\\s+|=");
        if (pairs.length != count * 2) {
            throw new IllegalArgumentException("The data provided does not match the count or is poorly formed");
        }
        for (int x = 0; x < (count * 2); x += 2) {
            pairs[x + 1] = checkForEmptyString(pairs[x + 1], NONCE);
            pairs[x] = checkForEmptyString(pairs[x], STATUS);
            if (pairs[x].equalsIgnoreCase(CONFIRMED)) {
                ret.put(pairs[x + 1], DigestStatus.Confirmed);
            } else if (pairs[x].equalsIgnoreCase(INVALID)) {
                ret.put(pairs[x + 1], DigestStatus.Invalid);
            } else if (pairs[x].equalsIgnoreCase(UNKNOWN)) {
                ret.put(pairs[x + 1], DigestStatus.Unknown);
            } else {
                throw new IllegalArgumentException("'" + pairs[x + 1]
                        + "' must be confirmed, invalid, or unknown");
            }
        }

        return ret;
    }

    /**
     * Generate a digest response from a Map<String (nonce), DigestStatus (digest status)>.
     *
     * @param statusMap
     *            The Map<String, DigestStatus> that holds the nonce to digest status mappings
     * @return an {@link OutputDataStream}
     */
  /*  static public OutputDataStream createDigestResponse(final Map<String, DigestStatus> statusMap) {
        final StringBuffer message = new StringBuffer();
        final org.beepcore.beep.core.MimeHeaders mh = new org.beepcore.beep.core.MimeHeaders();
        mh.setContentType(CT_JALOP);
        mh.setHeader(HDRS_MESSAGE, MSG_DIGEST_RESP);
        mh.setHeader(HDRS_COUNT, String.valueOf(statusMap.size()));

        final Iterator<String> nonces = statusMap.keySet().iterator();
        while (nonces.hasNext()) {
            final String id = nonces.next();
            message.append(checkForEmptyString(statusMap.get(id).toString(), STATUS));
            message.append("=");
            message.append(checkForEmptyString(id, NONCE));
            message.append("\r\n");
        }

        final OutputDataStream ret = new OutputDataStream(mh, new BufferSegment(
                message.toString().getBytes()));
        ret.setComplete();
        return ret;
    } */

    /**
     * @param is
     *            The BEEP {@link InputDataStreamAdapter} that holds the
     *            message.
     * @param expectedHeaders
     *            List of expected headers
     * @return an array of 2 {@link MimeHeaders}, the first {@link MimeHeader}
     *         is the expected headers, the second is any remaining headers.
     *             If there is an underlying BEEP exception.
     */
    static MimeHeaders[] splitHeaders(HashMap<String, String> httpHeaders,
            final String... expectedHeaders)  {
        final Set<String> ehs = new TreeSet<String>(
                String.CASE_INSENSITIVE_ORDER);
        ehs.addAll(Arrays.asList(expectedHeaders));
        ehs.add(HDRS_CONTENT_TYPE);
        ehs.add(HDRS_CONTENT_TXFR_ENCODING);

        final MimeHeaders[] toReturn = new MimeHeaders[2];
        final MimeHeaders knownHeaders = new MimeHeaders();
        final MimeHeaders otherHeaders = new MimeHeaders();
        toReturn[0] = knownHeaders;
        toReturn[1] = otherHeaders;

        for (Map.Entry<String, String> entry : httpHeaders.entrySet()) {
            if (ehs.contains(entry.getKey())) {
                knownHeaders.addHeader(entry.getKey(), entry.getValue());
            } else {
                otherHeaders.addHeader(entry.getKey(), entry.getValue());
            }
        }

        return toReturn;
    }
}
