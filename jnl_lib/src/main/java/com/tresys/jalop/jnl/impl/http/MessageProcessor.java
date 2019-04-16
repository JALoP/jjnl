package com.tresys.jalop.jnl.impl.http;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.crypto.dsig.DigestMethod;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.log4j.Logger;

import com.tresys.jalop.jnl.Mode;
import com.tresys.jalop.jnl.RecordType;
import com.tresys.jalop.jnl.SubscribeRequest;
import com.tresys.jalop.jnl.Subscriber;
import com.tresys.jalop.jnl.exceptions.JNLMessageProcessingException;
import com.tresys.jalop.jnl.impl.subscriber.SubscriberHttpSessionImpl;

/**
 * Utility class for creating and parsing JALoP/HTTP messages.
 */
public class MessageProcessor {

    /** Logger for this class */
    private static final Logger logger = Logger.getLogger(MessageProcessor.class);

    /**
     * Null-safe way to add strings to an arraylist of strings. This is specially intended for the use of adding errorMessages.
     * @param errorMessages : The arrayList storing the error messages
     * @param errorMessage : The new error message to add to the list.
     */
    private static void addErrorMessage(Collection<String> errorMessages, String errorMessage)
    {
        if (errorMessages == null)
        {
            errorMessages = new ArrayList<>();
        }
        errorMessages.add(errorMessage);
    }

    private static void processJournalMissingMessage(final RecordType supportedRecType, final HttpServletResponse response, final Collection<String> errorMessages) throws JNLMessageProcessingException
    {
        logger.info(HttpUtils.MSG_JOURNAL_MISSING + " message received with record type " + supportedRecType);
        if (!RecordType.Journal.equals(supportedRecType))
        {
            addErrorMessage(errorMessages, "Expected an " + RecordType.Journal + " record type but received " + supportedRecType);
        }
        if (!CollectionUtils.isEmpty(errorMessages))
        {
            throw new JNLMessageProcessingException("Failed to process Journal Missing Message.");
        }
        else
        {
            response.setStatus(HttpServletResponse.SC_OK);
            response.setHeader(HttpUtils.HDRS_MESSAGE, HttpUtils.MSG_JOURNAL_MISSING_RESPONSE);
            logger.info(HttpUtils.MSG_JOURNAL_MISSING + " message processed");
        }
    }

    public static boolean processInitializeMessage(HashMap<String, String> requestHeaders, RecordType supportedRecType, String certFingerprint, HashMap<String, String> successResponseHeaders, List<String> errorMessages) throws IOException
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

        if (certFingerprint == null)
        {
            throw new IllegalArgumentException("certFingerprint is required");
        }

        logger.info(HttpUtils.MSG_INIT + " message received.");

        String publisherIdStr = requestHeaders.get(HttpUtils.HDRS_PUBLISHER_ID);
        if (!HttpUtils.validatePublisherId(publisherIdStr, errorMessages))
        {
            logger.error("Initialize message failed due to invalid Publisher ID value of: " + publisherIdStr);
            return false;
        }

        //Validates mode, must be live or archive, sets any error in response.
        String modeStr = requestHeaders.get(HttpUtils.HDRS_MODE);
        if (!HttpUtils.validateMode(modeStr, errorMessages))
        {
            logger.error("Initialize message failed due to invalid mode value of: " + modeStr);
            return false;
        }

        //Validates supported digest
        String digestStr =requestHeaders.get(HttpUtils.HDRS_ACCEPT_DIGEST);
        if (digestStr == null || digestStr.isEmpty())
        {
            digestStr = DigestMethod.SHA256;
        }

        String selectedDigest = HttpUtils.validateDigests(digestStr, successResponseHeaders, errorMessages);
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

        //Validates data class
        String dataClassStr = requestHeaders.get(HttpUtils.HDRS_DATA_CLASS);
        if (!HttpUtils.validateDataClass(dataClassStr, supportedRecType, errorMessages))
        {
            logger.error("Initialize message failed due to unsupported data class: " + dataClassStr);
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
            confDigestChallengeStr = HttpUtils.MSG_CONFIGURE_DIGEST_ON;
        }

        String selectedConfDigestChallenge = HttpUtils.validateConfigureDigestChallenge(confDigestChallengeStr, successResponseHeaders, errorMessages);
        if (selectedConfDigestChallenge == null)
        {
            logger.error("Initialize message failed due to unsupported configure digest challenge: " + confDigestChallengeStr);
            return false;
        }

        boolean performDigest = true;
        if (selectedConfDigestChallenge.equals(HttpUtils.MSG_CONFIGURE_DIGEST_OFF))
        {
            performDigest = false;
        }

        //Sets up subscriber session and determines if journal resume applies.
        final Subscriber subscriber = HttpUtils.getSubscriber();

        //Checks if session already exists for the specific publisher/record type, if so then return initialize-nack
        JNLSubscriber jnlSubscriber = (JNLSubscriber)subscriber;
        SubscriberHttpSessionImpl currSession = (SubscriberHttpSessionImpl)jnlSubscriber.getSessionByPublisherId(publisherIdStr, HttpUtils.getRecordType(dataClassStr), HttpUtils.getMode(modeStr));

        if (currSession != null)
        {
            //Checks if cert fingerprints match, if not then initialize-nack, otherwise remove old session and create new
            if (!certFingerprint.equals(currSession.getCertFingerprint()))
            {

                //TODO determine if initialize-nack or just return a new session, right now will return a new session
               // logger.error("Session already exists for publisher: " + publisherIdStr);
               // errorMessages.add(HttpUtils.HDRS_SESSION_ALREADY_EXISTS);

               // return false;
            }
            else
            {
                //remove old session
                jnlSubscriber.removeSession(currSession.getSessionId());
            }
        }

        //TODO don't know if we need the default digest timeout and message values, set to 1 for both since currently we just digest the message immediately and return in the response.
        UUID sessionUUID = UUID.randomUUID();
        final String sessionId = sessionUUID.toString();
        final SubscriberHttpSessionImpl sessionImpl = new SubscriberHttpSessionImpl(publisherIdStr, sessionId,
                supportedRecType, HttpUtils.getMode(modeStr), subscriber, selectedDigest,
                selectedXmlCompression, 1, //contextImpl.getDefaultDigestTimeout(),
                1, performDigest, certFingerprint/*contextImpl.getDefaultPendingDigestMax()*/);

        final SubscribeRequest subRequest = subscriber.getSubscribeRequest(sessionImpl);

        //If there is a Journal Offset, the Record Type is journal, and the communication Mode is Archive,
        //Send a journal resume message
        if( subRequest.getResumeOffset() > 0 &&
            RecordType.Journal.equals(supportedRecType) &&
            Mode.Archive.equals(HttpUtils.getMode(modeStr))) {
            if(logger.isDebugEnabled()) {
                logger.debug("Sending a journal resume message.");
            }

            //Adds journal resume header
            if (!createJournalResumeMessage(subRequest.getNonce(), subRequest.getResumeOffset(), successResponseHeaders, errorMessages))
            {
                logger.error("Create Journal resume message failed.");
                return false;
            }
        } else {
            //If no errors, return initialize-ack with supported digest/encoding
            logger.info("Initialize message is valid, sending intialize-ack");

            //Add the session ID before we send the initialize-ack message
            successResponseHeaders.put(HttpUtils.HDRS_SESSION_ID, sessionId);
        }

        return true;
    }

    public static Boolean createJournalResumeMessage(final String nonce,
            final long offset, HashMap<String, String> successResponseHeaders, List<String> errorResponseHeaders) {
        HttpUtils.checkForEmptyString(nonce, HttpUtils.HDRS_NONCE);

        if (offset < 0) {
            logger.error("offset for '"
                    + HttpUtils.MSG_JOURNAL_RESUME + "' must be positive");

            errorResponseHeaders.add(HttpUtils.HDRS_INVALID_JOURNAL_OFFSET);
            return false;
        }

        //Sets JAL-Id to indicate journal resume
        //TODO - need to determine if this is correct, this is how the java code worked before, however this only handles resuming one record.
        //if multiple record resumes are required then this will not work.
        successResponseHeaders.put(HttpUtils.HDRS_NONCE, nonce);
        successResponseHeaders.put(HttpUtils.HDRS_JOURNAL_OFFSET, Long.toString(offset));

        return true;
    }

    public static void setErrorResponse(List<String> errorMessages, HttpServletResponse response)
    {
        response.setHeader(HttpUtils.HDRS_ERROR_MESSAGE, HttpUtils.convertListToString(errorMessages));
    }

    public static boolean processJALRecordMessage(HashMap<String, String> requestHeaders, InputStream requestInputStream, RecordType supportedRecType, DigestResult digestResult, List<String> errorMessages)
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

        String sessionIdStr = requestHeaders.get(HttpUtils.HDRS_SESSION_ID);

        // Get the segment lengths from the header
        Long sysMetadataSize = new Long(0);
        try
        {
            sysMetadataSize = new Long(requestHeaders.get(HttpUtils.HDRS_SYS_META_LEN)).longValue();

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

        Long appMetadataSize = new Long(0);
        try
        {
            appMetadataSize = new Long(requestHeaders.get(HttpUtils.HDRS_APP_META_LEN)).longValue();

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
        Long payloadSize = new Long(0);
        if (payloadType.equalsIgnoreCase(HttpUtils.MSG_LOG) && RecordType.Log.equals(supportedRecType))
        {
            try
            {
                payloadSize = new Long(requestHeaders.get(HttpUtils.HDRS_LOG_LEN)).longValue();

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
            recType = RecordType.Log;
        }
        else if (payloadType.equalsIgnoreCase(HttpUtils.MSG_AUDIT) && RecordType.Audit.equals(supportedRecType))
        {
            try
            {
                payloadSize = new Long(requestHeaders.get(HttpUtils.HDRS_AUDIT_LEN)).longValue();

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
        }
        else if (payloadType.equalsIgnoreCase(HttpUtils.MSG_JOURNAL) && RecordType.Journal.equals(supportedRecType))
        {
            try
            {
                payloadSize = new Long(requestHeaders.get(HttpUtils.HDRS_JOURNAL_LEN)).longValue();

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
            errorMessages.add(HttpUtils.HDRS_UNSUPPORTED_DATACLASS);
            return false;
        }

        //Gets JAL-Id
        String jalId = requestHeaders.get(HttpUtils.HDRS_NONCE);

        //Verifies not empty
        jalId = HttpUtils.checkForEmptyString(jalId, HttpUtils.HDRS_NONCE);
        if (jalId == null)
        {
            errorMessages.add(HttpUtils.HDRS_INVALID_JAL_ID);
            return false;
        }

        //Lookup the correct session based upon session id and process the record
        final JNLSubscriber subscriber = (JNLSubscriber)HttpUtils.getSubscriber();
        SubscriberHttpSessionImpl sess = (SubscriberHttpSessionImpl)subscriber.getSessionBySessionId(sessionIdStr);

        //If null then active session does not exist for this publisher, return error
        if (sess == null)
        {
            errorMessages.add(HttpUtils.HDRS_UNSUPPORTED_SESSION_ID);
            return false;
        }

        //Process the JAL record
        SubscriberHttpANSHandler subscriberHandler = sess.getsubscriberHandler();
        String digest = subscriberHandler.handleJALRecord(sysMetadataSize, appMetadataSize, payloadSize, payloadType, recType, jalId, requestInputStream);

        //If null, then failure occurred
        if (digest == null)
        {
            errorMessages.add(HttpUtils.HDRS_RECORD_FAILED);
            return false;
        }

        //Sets digest return value
        digestResult.setDigest(digest);

        return true;
    }

    public static void setInitializeNackResponse(List<String> errorMessages, HttpServletResponse response)
    {
        response.setHeader(HttpUtils.HDRS_MESSAGE, HttpUtils.MSG_INIT_NACK);
        response.setHeader(HttpUtils.HDRS_ERROR_MESSAGE, HttpUtils.convertListToString(errorMessages));
    }

    public static void setInitializeAckResponse(HashMap<String, String> responseHeaders, HttpServletResponse response)
    {
        response.setHeader(HttpUtils.HDRS_MESSAGE, HttpUtils.MSG_INIT_ACK);

        for (Map.Entry<String, String> entry : responseHeaders.entrySet()) {
            response.setHeader(entry.getKey(), entry.getValue());
        }
    }

    public static void handleRequest(HttpServletRequest request, HttpServletResponse response, RecordType supportedRecType)
    {
        //Used to capture all error messages that occur during the processing of this message
        List<String> errorMessages = new ArrayList<>();
        try
        {
            // Gets all the headers from the request
            HashMap<String, String> currHeaders = HttpUtils.parseHttpHeaders(request);

            Integer currRequestCount = HttpUtils.requestCount.incrementAndGet();

            logger.info("request: " + currRequestCount.toString() + " started");

            // Init message
            String messageType = request.getHeader(HttpUtils.HDRS_MESSAGE);
            if (messageType.equals(HttpUtils.MSG_INIT))
            {
                //Gets the cert from the header and ensure successful cert fingerprint extraction otherwise initialize-nack
                String cert = request.getHeader("X-Client-Certificate");
                String certFingerprint = HttpUtils.getCertFingerprintFromHeader(cert);

                if (certFingerprint == null)
                {
                    //Send initialize-nack on error
                    errorMessages.add(HttpUtils.HDRS_INVALID_USER_CERT);
                    MessageProcessor.setInitializeNackResponse(errorMessages, response);
                    return;
                }

                HashMap<String, String> successResponseHeaders = new HashMap<String, String>();
                if (!MessageProcessor.processInitializeMessage(currHeaders, supportedRecType, certFingerprint, successResponseHeaders, errorMessages))
                {
                    //Send initialize-nack on error
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
                if (!HttpUtils.validateSessionId(sessionIdStr, errorMessages))
                {
                    String errMsg = "JAL Record message failed due to invalid Session ID value of: " + sessionIdStr;
                    logger.error(errMsg);
                    throw new JNLMessageProcessingException("Session ID is either invalid or not found.");
                }

                if (messageType.equals(HttpUtils.MSG_LOG) || messageType.equals(HttpUtils.MSG_JOURNAL)
                    || messageType.equals(HttpUtils.MSG_AUDIT)) // process
                                                                // binary if jal
                                                                // record data
                {
                    DigestResult digestResult = new DigestResult();
                    if (!MessageProcessor.processJALRecordMessage(currHeaders, request.getInputStream(),
                            supportedRecType, digestResult, errorMessages))
                    {
                        // Set error message in the header
                        MessageProcessor.setErrorResponse(errorMessages, response);
                    }
                    else
                    {
                        // Sets digest in the header if successful
                        response.setHeader(HttpUtils.HDRS_DIGEST, digestResult.getDigest());
                    }
                }
                else if (messageType.equals(HttpUtils.MSG_JOURNAL_MISSING))
                {
                    MessageProcessor.processJournalMissingMessage(supportedRecType, response, errorMessages);
                }
                else
                {
                    logger.error("Invalid message received: " + messageType + " , returning server error");
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    return;
                }
            }
        }
        catch (JNLMessageProcessingException e)
        {
            logger.error("Failed to process message. Cause: " + e);
            MessageProcessor.setErrorResponse(errorMessages, response);
        }
        catch (IOException ioe)
        {
            logger.error(ioe);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
        catch (IllegalArgumentException iae)
        {
            logger.error(iae);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
        catch (Exception e)
        {
            //Global catch all to prevent exceptions from ever being returned in the http response.
            logger.error(e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
