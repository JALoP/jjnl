package com.tresys.jalop.utils.jnltest;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;
import javax.xml.crypto.dsig.DigestMethod;

/**
 * Utility class for creating and parsing JALoP/HTTP messages.
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

    public static void setInitializeNackResponse(HashMap<String, String> responseHeaders, HttpServletResponse response)
    {
        response.setHeader(HttpUtils.HDRS_MESSAGE, HttpUtils.MSG_INIT_NACK);

        for (Map.Entry<String, String> entry : responseHeaders.entrySet()) {
            response.setHeader(entry.getKey(), entry.getValue());
        }
    }

    public static void setInitializeAckResponse(HashMap<String, String> responseHeaders, HttpServletResponse response)
    {
        response.setHeader(HttpUtils.HDRS_MESSAGE, HttpUtils.MSG_INIT_ACK);

        for (Map.Entry<String, String> entry : responseHeaders.entrySet()) {
            response.setHeader(entry.getKey(), entry.getValue());
        }
    }

    //Validates mode, must be publish live or publish archive
    public static boolean validateMode(String mode, HashMap<String, String> errorResponseHeaders)
    {
        String currMode = checkForEmptyString(mode, HDRS_MODE);

        if (currMode == null)
        {
            errorResponseHeaders.put(HDRS_UNSUPPORTED_MODE, "");
            return false;
        }

        //Checks if supported mode
        if (!currMode.equals(MSG_PUBLISH_LIVE) && !currMode.equals(MSG_PUBLISH_ARCHIVE))
        {
            errorResponseHeaders.put(HDRS_UNSUPPORTED_MODE, "");
            return false;
        }

        return true;
    }

    public static List<String> parseHeaderList(String currHeader)
    {
        String[] splitHeader = currHeader.split(",");
        for (int i = 0; i < splitHeader.length; i++)
        {
            splitHeader[i] = splitHeader[i].trim();
        }

        return Arrays.asList(splitHeader);
    }

    //Validates supported digest
    public static boolean validateDigests(String digests,  HashMap<String, String> successResponseHeaders, HashMap<String, String> errorResponseHeaders)
    {
        String currDigests = checkForEmptyString(digests, HDRS_ACCEPT_DIGEST);

        if (currDigests == null)
        {
            errorResponseHeaders.put(HDRS_UNSUPPORTED_DIGEST, "");
            return false;
        }

        List<String> acceptDigests = parseHeaderList(digests);

        //Check to ensure the digest is valid, the first one found is the preferred value (currently only SHA256)
        for (String currDigest : acceptDigests)
        {
            if (currDigest.equals(DigestMethod.SHA256))
            {
                successResponseHeaders.put(HDRS_DIGEST, DigestMethod.SHA256);
                return true;
            }
        }

        errorResponseHeaders.put(HDRS_UNSUPPORTED_DIGEST, "");
        return false;
    }

    //Validates supported xml compression
    public static boolean validateXmlCompression(String xmlCompressions,  HashMap<String, String> successResponseHeaders, HashMap<String, String> errorResponseHeaders)
    {
        String currXmlCompressions = checkForEmptyString(xmlCompressions, HDRS_ACCEPT_XML_COMPRESSION);

        if (currXmlCompressions == null)
        {
            errorResponseHeaders.put(HDRS_UNSUPPORTED_XML_COMPRESSION, "");
            return false;
        }

        List<String> acceptedXmlCompressions = parseHeaderList(currXmlCompressions);

        //Check to ensure the encoding is valid, only xml accepted, the first one found is the preferred value
        List<String> supportedXmlCompressionList = Arrays.asList(SUPPORTED_XML_COMPRESSIONS);
        for (String currXmlCompression : acceptedXmlCompressions)
        {

            if (supportedXmlCompressionList.contains(HttpUtils.checkForEmptyString(currXmlCompression, "")))
            {
                successResponseHeaders.put(HDRS_XML_COMPRESSION, currXmlCompression);
                return true;
            }
        }

        errorResponseHeaders.put(HDRS_UNSUPPORTED_XML_COMPRESSION, "");
        return false;
    }

    //Validates dataClass, must be journal, audit, or log
    public static boolean validateDataClass(String dataClass)
    {
        String currDataClass = checkForEmptyString(dataClass, HDRS_DATA_CLASS);

        if (currDataClass == null)
        {
            //TODO, no header response for invalid data class currently exists, before exception was just thrown.
            return false;
        }

        //Checks if supported data class.
        if (!currDataClass.equals(JOURNAL) && !currDataClass.equals(AUDIT) && !currDataClass.equals(LOG))
        {
            //TODO, no header response for invalid data class currently exists, before exception was just thrown.
            return false;
        }

        //TODO check to only allow journal/audit/log based upon config file

        return true;
    }

    //Validates version, must be 2.0
    public static boolean validateVersion(String version, HashMap<String, String> errorResponseHeaders)
    {
        String currVersion = checkForEmptyString(version, HDRS_VERSION);

        //Checks if supported version, only 2.0 is currently supported
        if (currVersion == null || !currVersion.equals(SUPPORTED_VERSION))
        {
            errorResponseHeaders.put(HDRS_UNSUPPORTED_VERSION, "");
            return false;
        }

        return true;
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
            return null;
        }
        toCheck = toCheck.trim();
        if (toCheck.length() == 0) {
            return null;
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
}
