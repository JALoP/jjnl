package com.tresys.jalop.jnl.impl.http;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.crypto.dsig.DigestMethod;

import org.apache.log4j.Logger;

import com.tresys.jalop.jnl.RecordType;
import com.tresys.jalop.jnl.Session;
import com.tresys.jalop.jnl.SubscribeRequest;
import com.tresys.jalop.jnl.Subscriber;
import com.tresys.jalop.jnl.impl.messages.Utils;
import com.tresys.jalop.jnl.impl.subscriber.SubscriberHttpSessionImpl;

/**
 * Utility class for creating and parsing JALoP/HTTP messages.
 */
public class MessageProcessor {

    /** Logger for this class */
    private static final Logger logger = Logger.getLogger(MessageProcessor.class);

    public static void processInitializeMessage(HttpServletRequest request, HttpServletResponse response, String supportedDataClass) throws IOException
    {
        System.out.println(HttpUtils.MSG_INIT + " message received.");

        HashMap <String,String> successResponseHeaders = new HashMap<String,String>();
        List <String> errorResponseHeaders = new ArrayList<String>();

        String publisherIdStr = request.getHeader(HttpUtils.HDRS_PUBLISHER_ID);
        if (!HttpUtils.validatePublisherId(publisherIdStr, errorResponseHeaders))
        {
            System.out.println("Initialize message failed due to invalid Publisher ID value of: " + publisherIdStr);
            MessageProcessor.setInitializeNackResponse(errorResponseHeaders, response);
            return;
        }

        //Validates mode, must be publish live or publish archive, sets any error in response.
        String modeStr = request.getHeader(HttpUtils.HDRS_MODE);
        if (!HttpUtils.validateMode(modeStr, errorResponseHeaders))
        {
            System.out.println("Initialize message failed due to invalid mode value of: " + modeStr);
            MessageProcessor.setInitializeNackResponse(errorResponseHeaders, response);
            return;
        }

        //Validates supported digest
        String digestStr = request.getHeader(HttpUtils.HDRS_ACCEPT_DIGEST);
        if (digestStr == null || digestStr.isEmpty())
        {
            digestStr = DigestMethod.SHA256;
        }

        String selectedDigest = HttpUtils.validateDigests(digestStr, successResponseHeaders, errorResponseHeaders);
        if (selectedDigest == null)
        {
            System.out.println("Initialize message failed due to none of the following digests supported: " + digestStr);
            MessageProcessor.setInitializeNackResponse(errorResponseHeaders, response);
            return;
        }

        //Validates supported xml compression
        String xmlCompressionsStr = request.getHeader(HttpUtils.HDRS_ACCEPT_XML_COMPRESSION);
        if (xmlCompressionsStr == null || xmlCompressionsStr.isEmpty())
        {
            xmlCompressionsStr = HttpUtils.SUPPORTED_XML_COMPRESSIONS[0];
        }

        String selectedXmlCompression = HttpUtils.validateXmlCompression(xmlCompressionsStr, successResponseHeaders, errorResponseHeaders);
        if (selectedXmlCompression == null)
        {
            System.out.println("Initialize message failed due to none of the following xml compressions supported: " + xmlCompressionsStr);
            MessageProcessor.setInitializeNackResponse(errorResponseHeaders, response);
            return;
        }

        //Validates data class
        String dataClassStr = request.getHeader(HttpUtils.HDRS_DATA_CLASS);
        if (!HttpUtils.validateDataClass(dataClassStr, supportedDataClass, errorResponseHeaders))
        {
            System.out.println("Initialize message failed due to unsupported data class: " + dataClassStr);
            MessageProcessor.setInitializeNackResponse(errorResponseHeaders, response);
            return;
        }

        //Validates version
        String versionStr = request.getHeader(HttpUtils.HDRS_VERSION);
        if (!HttpUtils.validateVersion(versionStr, errorResponseHeaders))
        {
            System.out.println("Initialize message failed due to unsupported version: " + versionStr);
            MessageProcessor.setInitializeNackResponse(errorResponseHeaders, response);
            return;
        }

        //Validates configure digest challenge
        String confDigestChallengeStr = request.getHeader(HttpUtils.HDRS_ACCEPT_CONFIGURE_DIGEST_CHALLENGE);
        if (confDigestChallengeStr == null || confDigestChallengeStr.isEmpty())
        {
            confDigestChallengeStr = HttpUtils.MSG_CONFIGURE_DIGEST_ON;
        }

        String selectedConfDigestChallenge = HttpUtils.validateConfigureDigestChallenge(confDigestChallengeStr, successResponseHeaders, errorResponseHeaders);
        if (selectedConfDigestChallenge == null)
        {
            System.out.println("Initialize message failed due to unsupported configure digest challenge: " + confDigestChallengeStr);
            MessageProcessor.setInitializeNackResponse(errorResponseHeaders, response);
            return;
        }

        //Sets up subscriber session and determines if journal resume applies.
        final Subscriber subscriber = HttpUtils.getSubscriber();

        //Checks if session already exists for the specific publisher/record type, if so then return initialize-nack
        JNLSubscriber jnlSubscriber = (JNLSubscriber)subscriber;
        Session currSession = jnlSubscriber.getSessionByPublisherId(publisherIdStr, HttpUtils.getRecordType(dataClassStr));

        //TODO need to determine what the behavior should be if session already exists. Initialize-nack?  What results in a duplicate session error?
        //Same publisher id and record type?
        //Same publisher id, record type and mode (live/archive)?
        if (currSession != null)
        {
            System.out.println("Session already exists for publisher: " + publisherIdStr);
            errorResponseHeaders.add(HttpUtils.HDRS_SESSION_ALREADY_EXISTS);
            MessageProcessor.setInitializeNackResponse(errorResponseHeaders, response);

            //TODO - sending session id in initialize-nack for testing, so test publisher can still send until this logic is finalized.
            //Add the session ID before we send the initialize-ack message
            response.setHeader(HttpUtils.HDRS_SESSION_ID, ((SubscriberHttpSessionImpl)currSession).getSessionId());

            return;
        }

        //TODO don't know if we need the default digest timeout and message values, set to 1 for both since currently we just digest the message immediately and return in the response.
        UUID sessionUUID = UUID.randomUUID();
        final String sessionId = sessionUUID.toString();
        final SubscriberHttpSessionImpl sessionImpl = new SubscriberHttpSessionImpl(publisherIdStr, sessionId,
                HttpUtils.getRecordType(supportedDataClass), subscriber, selectedDigest,
                selectedXmlCompression, 1, //contextImpl.getDefaultDigestTimeout(),
                1, selectedConfDigestChallenge/*contextImpl.getDefaultPendingDigestMax()*/);

        final SubscribeRequest subRequest = subscriber.getSubscribeRequest(sessionImpl);

        if(subRequest.getResumeOffset() > 0 && RecordType.Journal.equals(HttpUtils.getRecordType(supportedDataClass))) {
            if(logger.isDebugEnabled()) {
                logger.debug("Sending a journal resume message.");
            }

            //Adds journal resume header
            if (!createJournalResumeMessage(subRequest.getNonce(), subRequest.getResumeOffset(), successResponseHeaders, errorResponseHeaders))
            {
                MessageProcessor.setInitializeNackResponse(errorResponseHeaders, response);
                return;
            }
        } else {
            //If no errors, return initialize-ack with supported digest/encoding
            System.out.println("Initialize message is valid, sending intialize-ack");

            //Add the session ID before we send the initialize-ack message
            successResponseHeaders.put(HttpUtils.HDRS_SESSION_ID, sessionId);
            MessageProcessor.setInitializeAckResponse(successResponseHeaders, response);
        }
    }

    public static Boolean createJournalResumeMessage(final String nonce,
            final long offset, HashMap<String, String> successResponseHeaders, List<String> errorResponseHeaders) {
        HttpUtils.checkForEmptyString(nonce, HttpUtils.HDRS_NONCE);

        if (offset < 0) {
            //TODO general comment through out, need to change all System.out.println to use log4j logger instead.
            System.out.println("offset for '"
                    + HttpUtils.MSG_JOURNAL_RESUME + "' must be positive");

            errorResponseHeaders.add("Invalid JAL-Journal-Offset");
            return false;
        }

        //Sets JAL-Id to indicate journal resume
        successResponseHeaders.put(HttpUtils.HDRS_NONCE, nonce);
        successResponseHeaders.put(HttpUtils.HDRS_JOURNAL_OFFSET, Long.toString(offset));

        return true;
    }

    public static void setErrorResponse(List<String> errorMessages, HttpServletResponse response)
    {
        response.setHeader(HttpUtils.HDRS_ERROR_MESSAGE, HttpUtils.convertListToString(errorMessages));
    }

    public static void setErrorResponse(String errorMessage, HttpServletResponse response)
    {
        response.setHeader(HttpUtils.HDRS_ERROR_MESSAGE, errorMessage);
    }

    public static boolean processJALRecordMessage(HttpServletRequest request, HttpServletResponse response, String supportedDataClass, Integer currRequestCount) throws IOException
    {
        //Gets the session Id from header
        String sessionIdStr = request.getHeader(HttpUtils.HDRS_SESSION_ID);
        List<String> errorMessages = new ArrayList<String>();
        if (!HttpUtils.validateSessionId(sessionIdStr, errorMessages))
        {
            System.out.println("Initialize message failed due to invalid Session ID value of: " + sessionIdStr);
            MessageProcessor.setErrorResponse(errorMessages, response);
            return false;
        }

        // Get the segment lengths from the header
        Long sysMetadataSize = new Long(0);
        try
        {
            sysMetadataSize = new Long(request.getHeader(Utils.HDRS_SYS_META_LEN)).longValue();

            if (sysMetadataSize < 0)
            {
                MessageProcessor.setErrorResponse(HttpUtils.HDRS_INVALID_SYS_META_LEN, response);
                return false;
            }
        }
        catch(NumberFormatException nfe )
        {
            MessageProcessor.setErrorResponse(HttpUtils.HDRS_INVALID_SYS_META_LEN, response);
            return false;
        }

        Long appMetadataSize = new Long(0);
        try
        {
            appMetadataSize = new Long(request.getHeader(Utils.HDRS_APP_META_LEN)).longValue();

            if (appMetadataSize < 0)
            {
                MessageProcessor.setErrorResponse(HttpUtils.HDRS_INVALID_APP_META_LEN, response);
                return false;
            }
        }
        catch(NumberFormatException nfe )
        {
            MessageProcessor.setErrorResponse(HttpUtils.HDRS_INVALID_APP_META_LEN, response);
            return false;
        }

        RecordType recType = RecordType.Unset;
        String payloadType = request.getHeader(Utils.HDRS_MESSAGE);
        Long payloadSize = new Long(0);
        if (payloadType.equalsIgnoreCase(HttpUtils.MSG_LOG) && RecordType.Log.equals(HttpUtils.getRecordType(supportedDataClass)))
        {
            try
            {
                payloadSize = new Long(request.getHeader(Utils.HDRS_LOG_LEN)).longValue();

                if (payloadSize < 0)
                {
                    MessageProcessor.setErrorResponse(HttpUtils.HDRS_INVALID_LOG_LEN, response);
                    return false;
                }
            }
            catch (NumberFormatException nfe)
            {
                MessageProcessor.setErrorResponse(HttpUtils.HDRS_INVALID_LOG_LEN, response);
                return false;
            }
            recType = RecordType.Log;
        }
        else if (payloadType.equalsIgnoreCase(HttpUtils.MSG_AUDIT) && RecordType.Audit.equals(HttpUtils.getRecordType(supportedDataClass)))
        {
            try
            {
                payloadSize = new Long(request.getHeader(Utils.HDRS_AUDIT_LEN)).longValue();

                if (payloadSize < 0)
                {
                    MessageProcessor.setErrorResponse(HttpUtils.HDRS_INVALID_AUDIT_LEN, response);
                    return false;
                }
            }
            catch (NumberFormatException nfe)
            {
                MessageProcessor.setErrorResponse(HttpUtils.HDRS_INVALID_AUDIT_LEN, response);
                return false;
            }
            recType = RecordType.Audit;
        }
        else if (payloadType.equalsIgnoreCase(HttpUtils.MSG_JOURNAL) && RecordType.Journal.equals(HttpUtils.getRecordType(supportedDataClass)))
        {
            try
            {
                payloadSize = new Long(request.getHeader(Utils.HDRS_JOURNAL_LEN)).longValue();

                if (payloadSize < 0)
                {
                    MessageProcessor.setErrorResponse(HttpUtils.HDRS_INVALID_JOURNAL_LEN, response);
                    return false;
                }
            }
            catch (NumberFormatException nfe)
            {
                MessageProcessor.setErrorResponse(HttpUtils.HDRS_INVALID_JOURNAL_LEN, response);
                return false;
            }
            recType = RecordType.Journal;
        }
        else
        {
            MessageProcessor.setErrorResponse(HttpUtils.HDRS_UNSUPPORTED_DATACLASS, response);
            return false;
        }

        //Gets JAL-Id
        String jalId = request.getHeader(HttpUtils.HDRS_NONCE);

        //Verifies not empty
        try
        {
            jalId = HttpUtils.checkForEmptyString(jalId, HttpUtils.HDRS_NONCE);
        }
        catch(IllegalArgumentException iae )
        {
            MessageProcessor.setErrorResponse(HttpUtils.HDRS_INVALID_JAL_ID, response);
            return false;
        }

        //Lookup the correct session based upon session id and process the record
        final JNLSubscriber subscriber = (JNLSubscriber)HttpUtils.getSubscriber();
        SubscriberHttpSessionImpl sess = (SubscriberHttpSessionImpl)subscriber.getSessionBySessionId(sessionIdStr);

        //If null then active session does not exist for this publisher, return error
        if (sess == null)
        {
            MessageProcessor.setErrorResponse(HttpUtils.HRDS_UNSUPPORTED_SESSION_ID, response);
            return false;
        }

        //Process the JAL record
        SubscriberHttpANSHandler subscriberHandler = sess.getsubscriberHandler();
        String digest = subscriberHandler.handleJALRecord(sysMetadataSize, appMetadataSize, payloadSize, payloadType, recType, jalId, request.getInputStream());

        //If null, then failure occurred
        if (digest == null)
        {
            //TODO for now put empty digest in response, which will indicate a failure, might want to return a specific error header?
            response.setHeader(HttpUtils.HDRS_DIGEST, "");
        }
        else
        {
            //Put digest in the response
            response.setHeader(HttpUtils.HDRS_DIGEST, digest);
        }

        /*
        //writes out binary length
        System.out.println("Binary data length is: " + request.getHeader(HttpUtils.HDRS_CONTENT_LENGTH));

        ///TODO - Handle the body of the http post, needed for record binary transfer, demo file transfer currently
        String digest = HttpUtils.readBinaryDataFromRequest(request, currRequestCount);
        System.out.println("The digest value is: " + digest);

        //Sets digest in the response
        response.setHeader(HttpUtils.HDRS_DIGEST, digest); */

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

    public static void handleRequest(HttpServletRequest request, HttpServletResponse response, String supportedDataClass) throws IOException
    {
        //Mainly for debugging right now, prints out headers to console.
        HttpUtils.parseHttpHeaders(request);

        Integer currRequestCount = HttpUtils.requestCount.incrementAndGet();

        System.out.println("request: " + currRequestCount.toString() + " started");

        //Handles messages

        //Init message
        String messageType = request.getHeader(HttpUtils.HDRS_MESSAGE);
        if (messageType.equals(HttpUtils.MSG_INIT))
        {
            MessageProcessor.processInitializeMessage(request, response, supportedDataClass);
        }
        else if (messageType.equals(HttpUtils.MSG_LOG) || messageType.equals(HttpUtils.MSG_JOURNAL) || messageType.equals(HttpUtils.MSG_AUDIT)) //process binary if jal record data
        {
            MessageProcessor.processJALRecordMessage(request, response, supportedDataClass, currRequestCount);
        }
        else
        {
            System.out.println("Invalid message received: " + messageType + " , returning server error");
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }
    }
}
