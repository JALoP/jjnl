package com.tresys.jalop.jnl.impl.http;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.crypto.dsig.DigestMethod;

import org.apache.log4j.Logger;

import com.tresys.jalop.jnl.RecordType;
import com.tresys.jalop.jnl.SubscribeRequest;
import com.tresys.jalop.jnl.Subscriber;
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

        if (!HttpUtils.validateConfigureDigestChallenge(confDigestChallengeStr, successResponseHeaders, errorResponseHeaders))
        {
            System.out.println("Initialize message failed due to unsupported configure digest challenge: " + confDigestChallengeStr);
            MessageProcessor.setInitializeNackResponse(errorResponseHeaders, response);
            return;
        }

        //Sets up subscriber session and determines if journal resume applies.
        final Subscriber subscriber = HttpUtils.getSubscriber();

        //TODO need to pass in publisher uuid sent from publisher in header, right now putting random uuid in.
        //TODO don't know if we need the default digest timeout and message values, set to 1 for both since currently we just digest the message immediately and return in the response.
        final SubscriberHttpSessionImpl sessionImpl = new SubscriberHttpSessionImpl("publisher_id",
                HttpUtils.getRecordType(supportedDataClass), subscriber, selectedDigest,
                selectedXmlCompression, 1, //contextImpl.getDefaultDigestTimeout(),
                1 /*contextImpl.getDefaultPendingDigestMax()*/);

      //  this.contextImpl.addSession(message.getChannel().getSession(), sessionImpl);

        final SubscribeRequest subRequest = subscriber.getSubscribeRequest(sessionImpl);

        if(subRequest.getResumeOffset() > 0 && RecordType.Journal.equals(supportedDataClass)) {
            if(logger.isDebugEnabled()) {
                logger.debug("Sending a journal resume message.");
            }

            //Adds journal resume header
            if (!createJournalResumeMessage(subRequest.getNonce(), subRequest.getResumeOffset(), successResponseHeaders))
            {
                //TODO getNonce (JAL record id) should never be less than zero, but need to handle with initialize-nack if this fails?
                //Something needs put in the JAL-Error-Message list here
                errorResponseHeaders.add("Invalid Jal-Id");
                MessageProcessor.setInitializeNackResponse(errorResponseHeaders, response);
                return;
            }
        } else {
            //If no Jal-Id in header, then no journal resume.
        }

        //If no errors, return initialize-ack with supported digest/encoding
        System.out.println("Initialize message is valid, sending intialize-ack");
        MessageProcessor.setInitializeAckResponse(successResponseHeaders, response);
    }

    public static Boolean createJournalResumeMessage(final String nonce,
            final long offset, HashMap<String, String> headers) {
        HttpUtils.checkForEmptyString(nonce, HttpUtils.HDRS_NONCE);

        if (offset < 0) {
            //TODO general comment through out, need to change all System.out.println to use log4j logger instead.
            System.out.println("offset for '"
                    + HttpUtils.MSG_JOURNAL_RESUME + "' must be positive");
            return false;
        }

        //Sets JAL-Id to indicate journal resume
        headers.put(HttpUtils.HDRS_NONCE, nonce);
        headers.put(HttpUtils.HDRS_JOURNAL_OFFSET, Long.toString(offset));

        return false;
    }

    public static void processJALRecordMessage(HttpServletRequest request, HttpServletResponse response, String supportedDataClass, Integer currRequestCount) throws IOException
    {
        //writes out binary length
        System.out.println("Binary data length is: " + request.getHeader(HttpUtils.HDRS_CONTENT_LENGTH));

        ///TODO - Handle the body of the http post, needed for record binary transfer, demo file transfer currently
        String digest = HttpUtils.readBinaryDataFromRequest(request, currRequestCount);
        System.out.println("The digest value is: " + digest);

        //Sets digest in the response
        response.setHeader(HttpUtils.HDRS_DIGEST, digest);
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
