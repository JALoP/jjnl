package com.tresys.jalop.jnl.impl.http;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
//import javax.xml.crypto.dsig.DigestMethod;

import org.apache.log4j.Logger;

import com.google.common.annotations.VisibleForTesting;
import com.tresys.jalop.jnl.DigestStatus;
import com.tresys.jalop.jnl.JNLLog;
import com.tresys.jalop.jnl.Mode;
import com.tresys.jalop.jnl.RecordType;
import com.tresys.jalop.jnl.SubscribeRequest;
import com.tresys.jalop.jnl.Subscriber;
import com.tresys.jalop.jnl.exceptions.JNLSessionInvalidException;
import com.tresys.jalop.jnl.impl.JNLLogger;
import com.tresys.jalop.jnl.impl.subscriber.SubscriberHttpSessionImpl;
import com.tresys.jalop.jnl.DigestAlgorithms;

/**
 * Utility class for creating and parsing JALoP/HTTP messages.
 */
public class MessageProcessor {

    private static final Lock lock = new ReentrantLock();

    private static void updateSessionTimestamp(SubscriberHttpSessionImpl currSession)
    {
        lock.lock();
        try
        {
            currSession.updateLastTouchedTimestamp();
        }

        finally
        {
            lock.unlock();
        }
    }

    /**
     * Null-safe way to add strings to an arraylist of strings. This is specially intended for the use of adding errorMessages.
     * @param errorMessages : The arrayList storing the error messages
     * @param errorMessage : The new error message to add to the list.
     */
    @VisibleForTesting
    static void addErrorMessage(Collection<String> errorMessages, String errorMessage)
    {
        if (errorMessages == null)
        {
            errorMessages = new ArrayList<>();
        }
        errorMessages.add(errorMessage);
    }

    @VisibleForTesting
    static boolean processJournalMissingMessage(final TreeMap<String, String> requestHeaders, final RecordType supportedRecType, final SubscriberAndSession subscriberAndSession, DigestResult digestResult, final Subscriber subscriber, JNLLog logger, List<String> errorMessages)
    {
        if (digestResult == null)
        {
            throw new IllegalArgumentException("digestResult is required");
        }

        if (errorMessages == null)
        {
            throw new IllegalArgumentException("errorMessages is required");
        }

        if (requestHeaders == null)
        {
            throw new IllegalArgumentException("requestHeaders is required");
        }

        if (supportedRecType == null)
        {
            throw new IllegalArgumentException("supportedRecType is required");
        }

        if (subscriberAndSession == null)
        {
            throw new IllegalArgumentException("subscriberAndSession is required");
        }

        if (subscriber == null)
        {
            throw new IllegalArgumentException("subscriber is required");
        }

        final SubscriberHttpSessionImpl sess = (SubscriberHttpSessionImpl)subscriberAndSession.getSession();

        if (sess == null)
        {
            throw new IllegalArgumentException("session cannot be null");
        }

        //Sets logger
        if (logger == null)
        {
            logger = new JNLLogger(Logger.getLogger(MessageProcessor.class));
        }

        logger.debug(HttpUtils.MSG_JOURNAL_MISSING + " message received with record type " + supportedRecType);

        //Checks the jal Id
        String jalId = requestHeaders.get(HttpUtils.HDRS_NONCE);
        jalId = HttpUtils.checkForEmptyString(jalId);
        if (jalId == null)
        {
            //Set to empty string so header is correctly set.
            digestResult.setJalId("");
            logger.error("Digest response message failed due to invalid JAL Id value of: " + jalId);
            errorMessages.add(HttpUtils.HDRS_INVALID_JAL_ID);
            return false;
        }
        digestResult.setJalId(jalId);

        if (!RecordType.Journal.equals(supportedRecType))
        {
            errorMessages.add(HttpUtils.HDRS_UNSUPPORTED_RECORD_TYPE);
            return false;
        }

        //Execute the notifyJournalMissing callback which will take care deleting the downloaded record in the output dir
        if (!subscriber.notifyJournalMissing(sess, jalId, subscriberAndSession.getSubscriber()))
        {
            logger.error("notifyJournalMissing failure: " + jalId);

            errorMessages.add(HttpUtils.HDRS_JOURNAL_MISSING_FAILURE);
            return false;
        }

        return true;
    }

    @VisibleForTesting
    static boolean processInitializeMessage(final TreeMap<String, String> requestHeaders, final RecordType supportedRecType, HashMap<String, String> successResponseHeaders, final HttpUtils httpUtils, List<String> errorMessages) throws IOException
    {
        if (errorMessages == null)
        {
            throw new IllegalArgumentException("errorMessages is required");
        }

        if (requestHeaders == null)
        {
            throw new IllegalArgumentException("requestHeaders is required");
        }

        if (successResponseHeaders == null)
        {
            throw new IllegalArgumentException("successResponseHeaders is required");
        }

        if (supportedRecType == null)
        {
            throw new IllegalArgumentException("supportedRecType is required");
        }

        if (httpUtils == null)
        {
            throw new IllegalArgumentException("httpUtils is required");
        }

        if (httpUtils.getSubscriber() == null)
        {
            throw new IllegalArgumentException("httpUtils subscriber is required");
        }

        //Sets logger
        JNLLog logger = null;
        if (httpUtils.getExternalLogger() == null)
        {
            logger = new JNLLogger(Logger.getLogger(MessageProcessor.class));
        }
        else
        {
            logger = httpUtils.getExternalLogger();
        }

        logger.info(HttpUtils.MSG_INIT + " message received.");

        String publisherIdStr = requestHeaders.get(HttpUtils.HDRS_PUBLISHER_ID);
        if (!HttpUtils.validatePublisherId(publisherIdStr, errorMessages))
        {
            logger.error("Initialize message failed due to invalid Publisher ID value of: " + publisherIdStr);
            return false;
        }

        final Subscriber subscriber = httpUtils.getSubscriber();

        //Validates mode, must be live or archive, sets any error in response.
        String modeStr = requestHeaders.get(HttpUtils.HDRS_MODE);
        if (!HttpUtils.validateMode(modeStr, subscriber.getMode(), errorMessages))
        {
            logger.error("Initialize message failed due to invalid mode value of: " + modeStr);
            return false;
        }

        //Validates supported digest...defaults to SHA256
        String digestStr = requestHeaders.get(HttpUtils.HDRS_ACCEPT_DIGEST);
        if (digestStr == null || digestStr.isEmpty())
        {
            digestStr = DigestAlgorithms.JJNL_DEFAULT_ALGORITHM.toUri();
        }

        List<String> supportedDigests = httpUtils.getSupportedDigestAlgorithms();
        String selectedDigest = HttpUtils.validateDigests(digestStr, supportedDigests, successResponseHeaders, errorMessages);
        if (selectedDigest == null)
        {
            logger.error("Initialize message failed due to none of the following digests supported: " + digestStr);
            return false;
        }

        //Validates supported xml compression
        String xmlCompressionsStr = requestHeaders.get(HttpUtils.HDRS_ACCEPT_XML_COMPRESSION);
        if (xmlCompressionsStr == null || xmlCompressionsStr.isEmpty())
        {
            xmlCompressionsStr = HttpUtils.SUPPORTED_XML_COMPRESSIONS[0];
        }

        String selectedXmlCompression = HttpUtils.validateXmlCompression(xmlCompressionsStr, successResponseHeaders, errorMessages);
        if (selectedXmlCompression == null)
        {
            logger.error("Initialize message failed due to none of the following xml compressions supported: " + xmlCompressionsStr);
            return false;
        }

        //Validates record type
        String recordTypeStr = requestHeaders.get(HttpUtils.HDRS_RECORD_TYPE);
        if (!httpUtils.validateRecordType(recordTypeStr, supportedRecType, errorMessages))
        {
            logger.error("Initialize message failed due to unsupported record type: " + recordTypeStr);
            return false;
        }

        //Validates version
        String versionStr = requestHeaders.get(HttpUtils.HDRS_VERSION);
        if (!HttpUtils.validateVersion(versionStr, errorMessages))
        {
            logger.error("Initialize message failed due to unsupported version: " + versionStr);
            return false;
        }

        //Validates configure digest challenge
        String confDigestChallengeStr = requestHeaders.get(HttpUtils.HDRS_ACCEPT_CONFIGURE_DIGEST_CHALLENGE);
        if (confDigestChallengeStr == null || confDigestChallengeStr.isEmpty())
        {
            confDigestChallengeStr = HttpUtils.MSG_ON;
        }

        String selectedConfDigestChallenge = httpUtils.validateConfigureDigestChallenge(confDigestChallengeStr, successResponseHeaders, errorMessages);
        if (selectedConfDigestChallenge == null)
        {
            logger.error("Initialize message failed due to unsupported configure digest challenge: " + confDigestChallengeStr);
            return false;
        }

        boolean performDigest = true;
        if (selectedConfDigestChallenge.equalsIgnoreCase(HttpUtils.MSG_OFF))
        {
            performDigest = false;
        }

        //TODO remove default values of 1 for pending digest values, once we've refactored the code
        UUID sessionUUID = UUID.randomUUID();
        final String sessionId = sessionUUID.toString();
        final SubscriberHttpSessionImpl sessionImpl = new SubscriberHttpSessionImpl(publisherIdStr, sessionId,
                supportedRecType, HttpUtils.getMode(modeStr), subscriber, selectedDigest,
                selectedXmlCompression, 1, 1, performDigest, httpUtils.getExternalLogger());

        final SubscribeRequest subRequest = subscriber.getSubscribeRequest(sessionImpl, subscriber.getCreateConfirmedFile());

        //If there is a Journal Offset, the Record Type is journal, and the communication Mode is Archive,
        //Send a journal resume message
        if( subRequest.getResumeOffset() > 0 &&
                RecordType.Journal.equals(supportedRecType) &&
                Mode.Archive.equals(HttpUtils.getMode(modeStr))) {
            if(logger.isDebugEnabled()) {
                logger.debug("Journal resume initiated.");
            }

            //Sets the resume offset to the session.
            sessionImpl.setJournalResumeOffset(subRequest.getResumeOffset());

            //Sets the input stream to the current partial record being resumed to the session
            sessionImpl.setJournalResumeIS(subRequest.getResumeInputStream());

            //Adds journal resume header
            if (!setJournalResumeMessage(subRequest.getNonce(), subRequest.getResumeOffset(), successResponseHeaders, logger, errorMessages))
            {
                logger.error("Journal resume failed.");
                return false;
            }
            //Add the session ID before we send the initialize-ack message
            successResponseHeaders.put(HttpUtils.HDRS_SESSION_ID, sessionId);
        } else {

            //Add the session ID before we send the initialize-ack message
            successResponseHeaders.put(HttpUtils.HDRS_SESSION_ID, sessionId);
        }

        //If no errors, return initialize-ack with supported digest/encoding
        logger.info("Initialize message is valid, sending intialize-ack");

        return true;
    }

    @VisibleForTesting
    static boolean processCloseSessionMessage(String sessionId, final Subscriber subscriber)
    {
        if (subscriber == null)
        {
            throw new IllegalArgumentException("subscriber is required");
        }

        return subscriber.removeSession(sessionId);
    }

    @VisibleForTesting
    static boolean processDigestResponseMessage(final TreeMap<String, String> requestHeaders, final SubscriberAndSession subscriberAndSession, DigestResult digestResult, final Subscriber subscriber, JNLLog logger, List<String> errorMessages)
    {
        if (digestResult == null)
        {
            throw new IllegalArgumentException("digestResult is required");
        }

        if (errorMessages == null)
        {
            throw new IllegalArgumentException("errorMessages is required");
        }

        if (requestHeaders == null)
        {
            throw new IllegalArgumentException("requestHeaders is required");
        }

        if (subscriberAndSession == null)
        {
            throw new IllegalArgumentException("subscriberAndSession is required");
        }

        if (subscriber == null)
        {
            throw new IllegalArgumentException("subscriber is required");
        }

        final SubscriberHttpSessionImpl sess = (SubscriberHttpSessionImpl)subscriberAndSession.getSession();

        if (sess == null)
        {
            throw new IllegalArgumentException("session cannot be null");
        }

        //Sets logger
        if (logger == null)
        {
            logger = new JNLLogger(Logger.getLogger(MessageProcessor.class));
        }

        digestResult.setFailedDueToSync(false);

        //Checks the jal Id
        String jalId = requestHeaders.get(HttpUtils.HDRS_NONCE);
        jalId = HttpUtils.checkForEmptyString(jalId);
        if (jalId == null)
        {
            //Set to empty string so header is correctly set.
            digestResult.setJalId("");
            logger.error("Digest response message failed due to invalid JAL Id value of: " + jalId);
            errorMessages.add(HttpUtils.HDRS_INVALID_JAL_ID);
            return false;
        }

        digestResult.setJalId(jalId);

        //Checks digest status
        String digestStatusStr = requestHeaders.get(HttpUtils.HDRS_DIGEST_STATUS);
        DigestStatus digestStatus = HttpUtils.getDigestStatus(digestStatusStr);
        if (digestStatus.equals(DigestStatus.Unknown))
        {
            logger.error("Digest response message failed due to invalid digest status value of: " + digestStatusStr + "  Forcing digest status of 'invalid' to delete temporary record with jalId of " + jalId );
            errorMessages.add(HttpUtils.HDRS_INVALID_DIGEST_STATUS);

            //Ticket #612 - force a digest response of invalid to delete record if invalid status is received.
            subscriber.notifyDigestResponse(sess, jalId, DigestStatus.Invalid, subscriberAndSession.getSubscriber());

            return false;
        }

        //Return sync if successful digest response
        logger.trace("Processing: " + jalId);

        // Execute the notify digest callback which will take care of moving the record from temp to perm
        if (subscriber.notifyDigestResponse(sess, jalId, digestStatus, subscriberAndSession.getSubscriber())) {
            // For a confirmed digest, send a sync message and remove the nonce from the sent queue
            if(digestStatus != DigestStatus.Confirmed)
            {
                //If not confirmed, then send back record-failure with JAL-Invalid-Digest error
                logger.warn("Non-confirmed digest received: " + jalId + ", " + digestStatus);
                digestResult.setFailedDueToSync(false);
                errorMessages.add(HttpUtils.HDRS_INVALID_DIGEST);
                return false;
            }
        }
        else {
            logger.error("notifyDigestResponse failure: " + jalId + ", " + digestStatus);
            digestResult.setFailedDueToSync(true);
            errorMessages.add(HttpUtils.HDRS_SYNC_FAILURE);
            return false;
        }

        return true;
    }

    @VisibleForTesting
    static boolean processJALRecordMessage(final TreeMap<String, String> requestHeaders, final InputStream requestInputStream, final RecordType supportedRecType, final SubscriberAndSession subscriberAndSession, DigestResult digestResult, final Subscriber subscriber, final JNLLog currLogger, List<String> errorMessages)
    {
        if (digestResult == null)
        {
            throw new IllegalArgumentException("digestResult is required");
        }

        if (errorMessages == null)
        {
            throw new IllegalArgumentException("errorMessages is required");
        }

        if (requestHeaders == null)
        {
            throw new IllegalArgumentException("requestHeaders is required");
        }

        if (requestInputStream == null)
        {
            throw new IllegalArgumentException("requestInputStream is required");
        }

        if (supportedRecType == null)
        {
            throw new IllegalArgumentException("supportedRecType is required");
        }

        if (subscriberAndSession == null)
        {
            throw new IllegalArgumentException("subscriberAndSession is required");
        }

        if (subscriber == null)
        {
            throw new IllegalArgumentException("subscriber is required");
        }

        final SubscriberHttpSessionImpl sess = (SubscriberHttpSessionImpl)subscriberAndSession.getSession();

        if (sess == null)
        {
            throw new IllegalArgumentException("session cannot be null");
        }

        //Sets logger
        JNLLog logger = null;
        if (currLogger == null)
        {
            logger = new JNLLogger(Logger.getLogger(MessageProcessor.class));
        }
        else
        {
            logger = currLogger;
        }

        digestResult.setFailedDueToSync(false);
        digestResult.setPerformDigest(sess.getPerformDigest());


        //Gets JAL-Id
        String jalId = requestHeaders.get(HttpUtils.HDRS_NONCE);

        //Verifies not empty
        jalId = HttpUtils.checkForEmptyString(jalId);
        if (jalId == null)
        {
            //Set to empty string so header is correctly set.
            digestResult.setJalId("");
            errorMessages.add(HttpUtils.HDRS_INVALID_JAL_ID);
            return false;
        }
        digestResult.setJalId(jalId);

        // Get the segment lengths from the header
        Long sysMetadataSize = Long.valueOf(0);
        try
        {
            sysMetadataSize = Long.valueOf(requestHeaders.get(HttpUtils.HDRS_SYS_META_LEN));

            if (sysMetadataSize <= 0)
            {
                errorMessages.add(HttpUtils.HDRS_INVALID_SYS_META_LEN);
                return false;
            }
        }
        catch(NumberFormatException nfe )
        {
            errorMessages.add(HttpUtils.HDRS_INVALID_SYS_META_LEN);
            return false;
        }

        Long appMetadataSize = Long.valueOf(0);
        try
        {
            appMetadataSize = Long.valueOf(requestHeaders.get(HttpUtils.HDRS_APP_META_LEN));

            if (appMetadataSize < 0)
            {
                errorMessages.add(HttpUtils.HDRS_INVALID_APP_META_LEN);
                return false;
            }
        }
        catch(NumberFormatException nfe )
        {
            errorMessages.add(HttpUtils.HDRS_INVALID_APP_META_LEN);
            return false;
        }

        RecordType recType = RecordType.Unset;
        String payloadType = requestHeaders.get(HttpUtils.HDRS_MESSAGE);
        Long payloadSize = Long.valueOf(0);
        if (payloadType.equalsIgnoreCase(HttpUtils.MSG_LOG) && RecordType.Log.equals(supportedRecType))
        {
            try
            {
                payloadSize = Long.valueOf(requestHeaders.get(HttpUtils.HDRS_LOG_LEN));

                if (payloadSize < 0)
                {
                    errorMessages.add(HttpUtils.HDRS_INVALID_LOG_LEN);
                    return false;
                }
            }
            catch (NumberFormatException nfe)
            {
                errorMessages.add(HttpUtils.HDRS_INVALID_LOG_LEN);
                return false;
            }

            //Special case for log records only.  A log record cannot have a zero length application metadata and zero length log length at the same time
            if (payloadSize == 0 && appMetadataSize == 0)
            {
                errorMessages.add(HttpUtils.HDRS_INVALID_LOG_RECORD);
                return false;
            }

            recType = RecordType.Log;
        }
        else if (payloadType.equalsIgnoreCase(HttpUtils.MSG_AUDIT) && RecordType.Audit.equals(supportedRecType))
        {
            try
            {
                payloadSize = Long.valueOf(requestHeaders.get(HttpUtils.HDRS_AUDIT_LEN));

                if (payloadSize <= 0)
                {
                    errorMessages.add(HttpUtils.HDRS_INVALID_AUDIT_LEN);
                    return false;
                }
            }
            catch (NumberFormatException nfe)
            {
                errorMessages.add(HttpUtils.HDRS_INVALID_AUDIT_LEN);
                return false;
            }
            recType = RecordType.Audit;

            //If audit-record perform additional check on JAL-Audit-Format, currently only format of xml is supported.
            if (!HttpUtils.validateAuditFormat(requestHeaders.get(HttpUtils.HDRS_AUDIT_FORMAT), errorMessages))
            {
                return false;
            }

        }
        else if (payloadType.equalsIgnoreCase(HttpUtils.MSG_JOURNAL) && RecordType.Journal.equals(supportedRecType))
        {
            try
            {
                payloadSize = Long.valueOf(requestHeaders.get(HttpUtils.HDRS_JOURNAL_LEN));

                if (payloadSize < 0)
                {
                    errorMessages.add(HttpUtils.HDRS_INVALID_JOURNAL_LEN);
                    return false;
                }
            }
            catch (NumberFormatException nfe)
            {
                errorMessages.add(HttpUtils.HDRS_INVALID_JOURNAL_LEN);
                return false;
            }
            recType = RecordType.Journal;
        }
        else
        {
            errorMessages.add(HttpUtils.HDRS_UNSUPPORTED_RECORD_TYPE);
            return false;
        }

        //Process the JAL record
        try {
            MessageDigest md = MessageDigest
                    .getInstance(sess.getDigestType(sess.getDigestMethod()));

            SubscriberHttpANSHandler subscriberHandler = new SubscriberHttpANSHandler(md, sess, sess.getPerformDigest(), currLogger);
            String digest = subscriberHandler.handleJALRecord(sysMetadataSize, appMetadataSize, payloadSize, payloadType, recType, jalId, requestInputStream, subscriberAndSession.getSubscriber());

            //If null, then failure occurred
            if (digest == null)
            {
                errorMessages.add(HttpUtils.HDRS_RECORD_FAILURE);
                return false;
            }

            //Sets digest return value if digest is enabled, otherwise process/send sync
            if (sess.getPerformDigest())
            {
                //Sets digest return value
                digestResult.setDigest(digest);
            }
            else
            {
                //Execute the notify digest callback which will take care of moving the record from temp to perm
                //Status is always confirmed if digesting is disabled
                if (!subscriber.notifyDigestResponse(sess, jalId, DigestStatus.Confirmed, subscriberAndSession.getSubscriber()))
                {
                    logger.error("notifyDigestResponse failure: " + jalId + ", " + DigestStatus.Confirmed);
                    digestResult.setFailedDueToSync(true);
                    errorMessages.add(HttpUtils.HDRS_SYNC_FAILURE);
                    return false;
                }
            }
        }
        catch (final NoSuchAlgorithmException e) {
            throw new IllegalArgumentException(
                    "'digestMethod' must be a valid DigestMethod", e);
        }

        return true;
    }

    @VisibleForTesting
    static void setInitializeNackResponse(final List<String> errorMessages, final HttpServletResponse response)
    {
        response.setHeader(HttpUtils.HDRS_MESSAGE, HttpUtils.MSG_INIT_NACK);
        response.setHeader(HttpUtils.HDRS_ERROR_MESSAGE, HttpUtils.convertListToString(errorMessages));
    }

    @VisibleForTesting
    static void setInitializeAckResponse(final HashMap<String, String> responseHeaders, final HttpServletResponse response)
    {
        response.setHeader(HttpUtils.HDRS_MESSAGE, HttpUtils.MSG_INIT_ACK);

        for (Map.Entry<String, String> entry : responseHeaders.entrySet()) {
            response.setHeader(entry.getKey(), entry.getValue());
        }
    }

    @VisibleForTesting
    static void setJournalMissingResponse(final String jalId, final HttpServletResponse response, JNLLog logger)
    {
        //Sets logger
        if (logger == null)
        {
            logger = new JNLLogger(Logger.getLogger(MessageProcessor.class));
        }

        response.setHeader(HttpUtils.HDRS_MESSAGE, HttpUtils.MSG_JOURNAL_MISSING_RESPONSE);
        response.setHeader(HttpUtils.HDRS_NONCE, jalId);
        logger.debug(HttpUtils.MSG_JOURNAL_MISSING_RESPONSE + " message processed");
    }

    @VisibleForTesting
    static void setRecordFailureResponse(final String jalId, final List<String> errorMessages, final HttpServletResponse response, JNLLog logger)
    {
        //Sets logger
        if (logger == null)
        {
            logger = new JNLLogger(Logger.getLogger(MessageProcessor.class));
        }

        response.setHeader(HttpUtils.HDRS_MESSAGE, HttpUtils.MSG_RECORD_FAILURE);
        response.setHeader(HttpUtils.HDRS_NONCE, jalId);
        response.setHeader(HttpUtils.HDRS_ERROR_MESSAGE, HttpUtils.convertListToString(errorMessages));
        logger.debug(HttpUtils.MSG_RECORD_FAILURE + " message processed");
    }

    @VisibleForTesting
    static void setCloseSessionResponse(final HttpServletResponse response, JNLLog logger)
    {
        //Sets logger
        if (logger == null)
        {
            logger = new JNLLogger(Logger.getLogger(MessageProcessor.class));
        }

        response.setHeader(HttpUtils.HDRS_MESSAGE, HttpUtils.MSG_CLOSE_SESSION_RESPONSE);
        logger.debug(HttpUtils.MSG_CLOSE_SESSION_RESPONSE + " message processed");
    }

    @VisibleForTesting
    static void setDigestChallengeResponse(final String jalId, final DigestResult digestResult, final HttpServletResponse response, JNLLog logger)
    {
        //Sets logger
        if (logger == null)
        {
            logger = new JNLLogger(Logger.getLogger(MessageProcessor.class));
        }

        response.setHeader(HttpUtils.HDRS_MESSAGE, HttpUtils.MSG_DIGEST_CHALLENGE);
        response.setHeader(HttpUtils.HDRS_NONCE, jalId);
        response.setHeader(HttpUtils.HDRS_DIGEST_VALUE, digestResult.getDigest());
        logger.debug(HttpUtils.MSG_DIGEST_CHALLENGE + " message processed");
    }

    @VisibleForTesting
    static void setSyncFailureResponse(final String jalId, final List<String> errorMessages, final HttpServletResponse response, JNLLog logger)
    {
        //Sets logger
        if (logger == null)
        {
            logger = new JNLLogger(Logger.getLogger(MessageProcessor.class));
        }

        response.setHeader(HttpUtils.HDRS_MESSAGE, HttpUtils.MSG_SYNC_FAILURE);
        response.setHeader(HttpUtils.HDRS_NONCE, jalId);
        response.setHeader(HttpUtils.HDRS_ERROR_MESSAGE, HttpUtils.convertListToString(errorMessages));
        logger.debug(HttpUtils.MSG_SYNC_FAILURE + " message processed");
    }

    @VisibleForTesting
    static void setSyncResponse(final String jalId, final HttpServletResponse response, JNLLog logger)
    {
        //Sets logger
        if (logger == null)
        {
            logger = new JNLLogger(Logger.getLogger(MessageProcessor.class));
        }

        response.setHeader(HttpUtils.HDRS_MESSAGE, HttpUtils.MSG_SYNC);
        response.setHeader(HttpUtils.HDRS_NONCE, jalId);
        logger.debug(HttpUtils.MSG_SYNC + " message processed");
    }

    @VisibleForTesting
    static void setSessionFailureResponse(final List<String> errorMessages, final HttpServletResponse response, final String sessionId, JNLLog logger, final String jalId)
    {
        //Sets logger
        if (logger == null)
        {
            logger = new JNLLogger(Logger.getLogger(MessageProcessor.class));
        }

        response.setHeader(HttpUtils.HDRS_MESSAGE, HttpUtils.MSG_SESSION_FAILURE);
        response.setHeader(HttpUtils.HDRS_ERROR_MESSAGE, HttpUtils.convertListToString(errorMessages));
        response.setHeader(HttpUtils.HDRS_NONCE, jalId);
        response.setHeader(HttpUtils.HDRS_SESSION_ID, sessionId);
        logger.debug(HttpUtils.MSG_SESSION_FAILURE + " message processed");
    }


    @VisibleForTesting
    static Boolean setJournalResumeMessage(final String nonce,
            final long offset, HashMap<String, String> successResponseHeaders, JNLLog logger, List<String> errorResponseHeaders) {
        HttpUtils.checkForEmptyString(nonce);

        //Sets logger
        if (logger == null)
        {
            logger = new JNLLogger(Logger.getLogger(MessageProcessor.class));
        }

        if (offset < 0) {
            logger.error("offset for '"
                    + HttpUtils.MSG_JOURNAL_RESUME + "' must be positive");

            errorResponseHeaders.add(HttpUtils.HDRS_INVALID_JOURNAL_OFFSET);
            return false;
        }

        //Sets JAL-Id to indicate journal resume
        successResponseHeaders.put(HttpUtils.HDRS_NONCE, nonce);
        successResponseHeaders.put(HttpUtils.HDRS_JOURNAL_OFFSET, Long.toString(offset));

        return true;
    }

    @VisibleForTesting
    static void setErrorResponse(List<String> errorMessages, HttpServletResponse response)
    {
        response.setHeader(HttpUtils.HDRS_ERROR_MESSAGE, HttpUtils.convertListToString(errorMessages));
    }

    public static void handleRequest(HttpServletRequest request, HttpServletResponse response, RecordType supportedRecType, HttpUtils httpUtils)
    {
        // Set the Content-Type header in response message.
        response.setHeader(HttpUtils.HDRS_CONTENT_TYPE, HttpUtils.DEFAULT_CONTENT_TYPE);

        if (httpUtils == null)
        {
            throw new IllegalArgumentException("httpUtils is required");
        }

        if (httpUtils.getSubscriber() == null)
        {
            throw new IllegalArgumentException("httpUtils subscriber is required");
        }

        //Sets logger
        JNLLog logger = null;
        if (httpUtils.getExternalLogger() == null)
        {
            logger = new JNLLogger(Logger.getLogger(MessageProcessor.class));
        }
        else
        {
            logger = httpUtils.getExternalLogger();
        }

        //Used to capture all error messages that occur during the processing of this message
        List<String> errorMessages = new ArrayList<>();
        try
        {
            // Gets all the headers from the request
            TreeMap<String, String> currHeaders = httpUtils.parseHttpHeaders(request);

            if (httpUtils != null)
            {
                Integer currRequestCount = httpUtils.requestCount.incrementAndGet();

                logger.debug("request: " + currRequestCount.toString() + " started");
            }

            // Init message
            String messageType = request.getHeader(HttpUtils.HDRS_MESSAGE);
            if (messageType.equalsIgnoreCase(HttpUtils.MSG_INIT))
            {
                HashMap<String, String> successResponseHeaders = new HashMap<String, String>();
                if (!MessageProcessor.processInitializeMessage(currHeaders, supportedRecType, successResponseHeaders, httpUtils, errorMessages))
                {
                    //Send initialize-nack on error
                    logger.info("Initialize message is invalid, sending intialize-nack");
                    MessageProcessor.setInitializeNackResponse(errorMessages, response);
                }
                else
                {
                    //Send initialize-ack on success
                    MessageProcessor.setInitializeAckResponse(successResponseHeaders, response);
                }
            }
            else //if not an init message
            {
                //Gets the session Id from header
                String sessionIdStr = currHeaders.get(HttpUtils.HDRS_SESSION_ID);

                SubscriberAndSession subscriberAndSession = httpUtils.validateSessionId(sessionIdStr, errorMessages);

                if (subscriberAndSession == null)
                {
                    String errMsg = "JAL message failed due to invalid Session ID value of: " + sessionIdStr;
                    logger.error(errMsg);
                    JNLSessionInvalidException jsie = new JNLSessionInvalidException("Session ID is either invalid or not found.");
                    jsie.setSessionId(sessionIdStr);
                    jsie.setJalId(currHeaders.get(HttpUtils.HDRS_NONCE));
                    throw jsie;
                }

                SubscriberHttpSessionImpl currSession = (SubscriberHttpSessionImpl)subscriberAndSession.getSession();

                if (messageType.equalsIgnoreCase(HttpUtils.MSG_LOG) || messageType.equalsIgnoreCase(HttpUtils.MSG_JOURNAL)
                        || messageType.equalsIgnoreCase(HttpUtils.MSG_AUDIT)) // process
                    // binary if jal
                    // record data
                {
                    DigestResult digestResult = new DigestResult();

                    updateSessionTimestamp(currSession);
                    if (!MessageProcessor.processJALRecordMessage(currHeaders, request.getInputStream(),
                            supportedRecType, subscriberAndSession, digestResult, httpUtils.getSubscriber(), httpUtils.getExternalLogger(), errorMessages))
                    {
                        updateSessionTimestamp(currSession);

                        //If digest was performed send digest-challenge-failed, otherwise send sync-failed
                        if (!digestResult.getFailedDueToSync())
                        {
                            MessageProcessor.setRecordFailureResponse(digestResult.getJalId(), errorMessages, response, logger);
                        }
                        else
                        {
                            MessageProcessor.setSyncFailureResponse(digestResult.getJalId(), errorMessages, response, logger);
                        }
                    }
                    else
                    {
                        updateSessionTimestamp(currSession);

                        //If digest was performed send digest challenge otherwise send sync
                        if (digestResult.getPerformDigest())
                        {
                            // Set digest-challenge response
                            MessageProcessor.setDigestChallengeResponse(digestResult.getJalId(), digestResult, response, logger);
                        }
                        else
                        {
                            //Send sync message
                            MessageProcessor.setSyncResponse(digestResult.getJalId(), response, logger);
                        }
                    }
                }
                else if (messageType.equalsIgnoreCase(HttpUtils.MSG_JOURNAL_MISSING))
                {
                    updateSessionTimestamp(currSession);

                    DigestResult digestResult = new DigestResult();
                    if (!MessageProcessor.processJournalMissingMessage(currHeaders, supportedRecType, subscriberAndSession, digestResult, httpUtils.getSubscriber(), logger, errorMessages))
                    {
                        //Send record failure
                        MessageProcessor.setRecordFailureResponse(digestResult.getJalId(), errorMessages, response, logger);
                    }
                    else
                    {
                        //Send journal missing response
                        MessageProcessor.setJournalMissingResponse(digestResult.getJalId(), response, logger);
                    }

                    updateSessionTimestamp(currSession);
                }
                else if (messageType.equalsIgnoreCase(HttpUtils.MSG_DIGEST_RESP))
                {
                    DigestResult digestResult = new DigestResult();

                    updateSessionTimestamp(currSession);
                    if (!MessageProcessor.processDigestResponseMessage(currHeaders, subscriberAndSession, digestResult, httpUtils.getSubscriber(), logger, errorMessages))
                    {
                        updateSessionTimestamp(currSession);

                        //Determine if it failed due to a sync failure or record failure
                        if (digestResult.getFailedDueToSync())
                        {
                            MessageProcessor.setSyncFailureResponse(digestResult.getJalId(), errorMessages, response, logger);
                        }
                        else
                        {
                            MessageProcessor.setRecordFailureResponse(digestResult.getJalId(), errorMessages, response, logger);
                        }
                    }
                    else
                    {
                        updateSessionTimestamp(currSession);

                        //Send sync message
                        MessageProcessor.setSyncResponse(digestResult.getJalId(), response, logger);
                    }
                }
                else if (messageType.equalsIgnoreCase(HttpUtils.MSG_CLOSE_SESSION))
                {
                    if (!MessageProcessor.processCloseSessionMessage(sessionIdStr, httpUtils.getSubscriber()))
                    {
                        String errMsg = "JAL message failed due to invalid Session ID value of: " + sessionIdStr;
                        logger.error(errMsg);
                        JNLSessionInvalidException jsie = new JNLSessionInvalidException("Session ID is either invalid or not found.");
                        jsie.setSessionId(sessionIdStr);
                        jsie.setJalId(currHeaders.get(HttpUtils.HDRS_NONCE));
                        throw jsie;
                    }
                    else
                    {
                        //Send close session response message
                        MessageProcessor.setCloseSessionResponse(response, logger);
                    }
                }
                else
                {
                    logger.error("Invalid message received: " + messageType + " , returning server error");
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    return;
                }
            }
        }
        catch (JNLSessionInvalidException e)
        {
            logger.error("Failed to process message. Cause: " + e);
            MessageProcessor.setSessionFailureResponse(errorMessages, response, HttpUtils.checkForEmptyString(e.getSessionId()), logger, HttpUtils.checkForEmptyString(e.getJalId()));
        }
        catch (IOException ioe)
        {
            logger.error("An io exception occurred:", ioe);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
        catch (IllegalArgumentException iae)
        {
            logger.error("An illegal argument exception occurred: ", iae);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
        catch (Exception e)
        {
            //Global catch all to prevent exceptions from ever being returned in the http response.
            logger.error("A general excpetion occurred: ", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
        httpUtils.parseHttpResponseHeaders(response);
    }
}
