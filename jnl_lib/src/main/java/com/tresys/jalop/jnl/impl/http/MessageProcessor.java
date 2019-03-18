package com.tresys.jalop.jnl.impl.http;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import javax.xml.crypto.dsig.DigestMethod;

/**
 * Utility class for creating and parsing JALoP/HTTP messages.
 */
public class MessageProcessor {
    public static AtomicInteger requestCount = new AtomicInteger();

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
        if (!HttpUtils.validateDigests(digestStr, successResponseHeaders, errorResponseHeaders))
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
        if (!HttpUtils.validateXmlCompression(xmlCompressionsStr, successResponseHeaders, errorResponseHeaders))
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
        String confDigestChallengeStr = request.getHeader(HttpUtils.HDRS_CONFIGURE_DIGEST_CHALLENGE);
        if (confDigestChallengeStr == null || confDigestChallengeStr.isEmpty())
        {
            confDigestChallengeStr = HttpUtils.SUPPORTED_CONFIGURE_DIGEST_CHALLENGES[0];
        }
        if (!HttpUtils.validateConfigureDigestChallenge(confDigestChallengeStr, successResponseHeaders, errorResponseHeaders))
        {
            System.out.println("Initialize message failed due to unsupported configure digest challenge: " + confDigestChallengeStr);
            MessageProcessor.setInitializeNackResponse(errorResponseHeaders, response);
            return;
        }

        //If no errors, return initialize-ack with supported digest/encoding
        System.out.println("Initialize message is valid, sending intialize-ack");
        MessageProcessor.setInitializeAckResponse(successResponseHeaders, response);
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

        Integer currRequestCount = requestCount.incrementAndGet();

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
