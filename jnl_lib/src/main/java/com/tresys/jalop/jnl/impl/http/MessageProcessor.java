package com.tresys.jalop.jnl.impl.http;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Utility class for creating and parsing JALoP/HTTP messages.
 */
public class MessageProcessor {
    public static AtomicInteger requestCount = new AtomicInteger();

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
   // public static final String HDRS_ACCEPT_ENCODING = "JAL-Accept-Encoding"; //This is actually accept-xml-compression, this was changed in the jalop specification, but not the code
    public static final String HDRS_AGENT = "JAL-Agent";
    public static final String HDRS_APP_META_LEN = "JAL-Application-Metadata-Length";
    public static final String HDRS_AUDIT_LEN = "JAL-Audit-Length";
    public static final String HDRS_CONTENT_TXFR_ENCODING = "Content-Transfer-Encoding";
    public static final String HDRS_CONTENT_TYPE = "Content-Type";
    public static final String HDRS_COUNT = "JAL-Count";
    public static final String HDRS_DATA_CLASS = "JAL-Data-Class";
    public static final String HDRS_DIGEST = "JAL-Digest";
   /// public static final String HDRS_ENCODING = "JAL-Encoding";  //This is actually JAL-XML-Compression, this was changed in the jalop spec, but not the code.
    public static final String HDRS_JOURNAL_LEN = "JAL-Journal-Length";
    public static final String HDRS_JOURNAL_OFFSET = "JAL-Journal-Offset";
    public static final String HDRS_LOG_LEN = "JAL-Log-Length";
    public static final String HDRS_MESSAGE = "JAL-Message";
    public static final String HDRS_MODE = "JAL-Mode";
    public static final String HDRS_NONCE = "JAL-Id";
    public static final String HDRS_SYS_META_LEN = "JAL-System-Metadata-Length";
    public static final String HDRS_UNAUTHORIZED_MODE = "JAL-Unauthorized-Mode";
    public static final String HDRS_UNSUPPORTED_DIGEST = "JAL-Unsupported-Digest";
    //public static final String HDRS_UNSUPPORTED_ENCODING = "JAL-Unsupported-Encoding"; //This is actually unsupported xml-compression, this was changed in the jalop specification, but not the code
    public static final String HDRS_UNSUPPORTED_MODE = "JAL-Unsupported-Mode";
    public static final String HDRS_UNSUPPORTED_VERSION = "JAL-Unsupported-Version";

    //New headers added for http, not in original Utils.java, however listed in Jalop specification doc
    public static final String HDRS_VERSION = "JAL-Version";
    public static final String HDRS_ACCEPT_XML_COMPRESSION="JAL-Accept-XML-Compression";
    public static final String HDRS_UNSUPPORTED_XML_COMPRESSION = "JAL-Unsupported-XML-Compression";
    public static final String HDRS_XML_COMPRESSION = "JAL-XML-Compression";
    public static final String HDRS_UNSUPPORTED_DATACLASS = "JAL-Unsupported-Data-Class";
    public static final String HDRS_ERROR_MESSAGE = "JAL-Error-Message";

    //Additional constants
    public static final String[] SUPPORTED_XML_COMPRESSIONS = new String[] {"none", "exi-1.0", "deflate"};
    public static final String SUPPORTED_VERSION = "2.0";


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
            if (!HttpUtils.validateDigests(digestStr, successResponseHeaders, errorResponseHeaders))
            {
                System.out.println("Initialize message failed due to none of the following digests supported: " + digestStr);
                MessageProcessor.setInitializeNackResponse(errorResponseHeaders, response);
                return;
            }

            //Validates supported xml compression
            String xmlCompressionsStr = request.getHeader(HttpUtils.HDRS_ACCEPT_XML_COMPRESSION);
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

            //If no errors, return initialize-ack with supported digest/encoding
            System.out.println("Initialize message is valid, sending intialize-ack");
            MessageProcessor.setInitializeAckResponse(successResponseHeaders, response);
        }
        else if (messageType.equals(HttpUtils.MSG_LOG) || messageType.equals(HttpUtils.MSG_JOURNAL) || messageType.equals(HttpUtils.MSG_AUDIT)) //process binary if jal record data
        {
            //writes out binary length
            System.out.println("Binary data length is: " + request.getHeader(HttpUtils.HDRS_CONTENT_LENGTH));

            ///TODO - Handle the body of the http post, needed for record binary transfer, demo file transfer currently
            String digest = HttpUtils.readBinaryDataFromRequest(request, currRequestCount);
            System.out.println("The digest value is: " + digest);

            //Sets digest in the response
            response.setHeader(HttpUtils.HDRS_DIGEST, digest);
        }
        else
        {
            System.out.println("Invalid message received: " + messageType + " , returning server error");
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }
    }
}
