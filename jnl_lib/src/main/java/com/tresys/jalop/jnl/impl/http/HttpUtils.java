package com.tresys.jalop.jnl.impl.http;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.http.HttpServletRequest;
import javax.xml.crypto.dsig.DigestMethod;

import com.tresys.jalop.jnl.RecordType;
import com.tresys.jalop.jnl.Subscriber;

/**
 * Utility class for creating and parsing JALoP/HTTP messages.
 */
public class HttpUtils {
    public static AtomicInteger requestCount = new AtomicInteger();
    private static final int BUFFER_SIZE = 4096;

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

    public static final String HDRS_ACCEPT_CONFIGURE_DIGEST_CHALLENGE = "JAL-Accept-Configure-Digest-Challenge";
    public static final String HDRS_ACCEPT_DIGEST = "JAL-Accept-Digest";
    public static final String HDRS_AGENT = "JAL-Agent";
    public static final String HDRS_APP_META_LEN = "JAL-Application-Metadata-Length";
    public static final String HDRS_AUDIT_LEN = "JAL-Audit-Length";
    public static final String HDRS_CONFIGURE_DIGEST_CHALLENGE = "JAL-Configure-Digest-Challenge";
    public static final String HDRS_CONTENT_TXFR_ENCODING = "Content-Transfer-Encoding";
    public static final String HDRS_CONTENT_TYPE = "Content-Type";
    public static final String HDRS_CONTENT_LENGTH = "Content-Length";
    public static final String HDRS_COUNT = "JAL-Count";
    public static final String HDRS_DATA_CLASS = "JAL-Data-Class";
    public static final String HDRS_DIGEST = "JAL-Digest";
    public static final String HDRS_JOURNAL_LEN = "JAL-Journal-Length";
    public static final String HDRS_JOURNAL_OFFSET = "JAL-Journal-Offset";
    public static final String HDRS_LOG_LEN = "JAL-Log-Length";
    public static final String HDRS_MESSAGE = "JAL-Message";
    public static final String HDRS_MODE = "JAL-Mode";
    public static final String HDRS_NONCE = "JAL-Id";
    public static final String HDRS_PUBLISHER_ID = "JAL-Publisher-Id";
    public static final String HDRS_SYS_META_LEN = "JAL-System-Metadata-Length";
    public static final String HDRS_UNAUTHORIZED_MODE = "JAL-Unauthorized-Mode";
    public static final String HDRS_UNSUPPORTED_CONFIGURE_DIGEST_CHALLENGE = "JAL-Unsupported-Configure-Digest-Challenge";
    public static final String HDRS_UNSUPPORTED_DIGEST = "JAL-Unsupported-Digest";
    public static final String HDRS_UNSUPPORTED_MODE = "JAL-Unsupported-Mode";
    public static final String HRDS_UNSUPPORTED_PUBLISHER_ID = "JAL-Unsupported-Publisher-Id";
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
    public static final String[] SUPPORTED_VERSIONS = new String[] {"2.0.0.0"};


    public static final String DEFAULT_CONTENT_TYPE =
            "application/octet-stream";

    /**
     * The default <code>DataStream</code> content transfer encoding
     * ("binary").
     */
    public static final String DEFAULT_CONTENT_TRANSFER_ENCODING = "binary";

    public static final String MSG_AUDIT = "audit-record";
    public static final String MSG_CONFIGURE_DIGEST_OFF = "off";
    public static final String MSG_CONFIGURE_DIGEST_ON = "on";
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

    public static String JOURNAL_ENDPOINT = "/journal";
    public static String AUDIT_ENDPOINT = "/audit";
    public static String LOG_ENDPOINT = "/log";

    private static List<String> allowedConfigureDigests;

    private static Subscriber subscriber;

    public static List<String> getAllowedConfigureDigests() {

        if (allowedConfigureDigests == null)
        {
            allowedConfigureDigests = new ArrayList<String>();
        }

        //Minimally "on" is always supported
        if (allowedConfigureDigests.size() == 0)
        {
            allowedConfigureDigests.add(HttpUtils.MSG_CONFIGURE_DIGEST_ON);
        }

        return allowedConfigureDigests;
    }

    public static void setAllowedConfigureDigests(List<String> allowedConfigureDigests) {
        HttpUtils.allowedConfigureDigests = allowedConfigureDigests;
    }

    public static String getDigest(MessageDigest md)
    {
        byte[] digest = md.digest();
        String hexDgst = "";
        for (byte b : digest) {
            hexDgst = hexDgst + String.format("%02x",b);
        }

        return hexDgst;
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

    public static String convertListToString(List<String> messageList)
    {
        String headerStr = "";
        if (messageList != null)
        {
            headerStr = String.join("|", messageList);
        }

        return headerStr;
    }

    public static HashMap<String, String> parseHttpHeaders(HttpServletRequest request)
    {
        HashMap<String, String> currHeaders = new HashMap<String, String>();
        Enumeration<String> headerNames = request.getHeaderNames();

        while (headerNames.hasMoreElements()) {

            String headerName = headerNames.nextElement();
            String headerValue = request.getHeader(headerName);
            System.out.println("Header Name: " + headerName + " Header Value: " + headerValue);

            currHeaders.put(headerName, headerValue);
        }

        return currHeaders;
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

    // Validates Publisher ID, must be UUID
    public static boolean validatePublisherId(String publisherId, List<String> errorResponseHeaders)
    {
        String currPublisherId = checkForEmptyString(publisherId, HDRS_PUBLISHER_ID);

        if (currPublisherId == null)
        {
            errorResponseHeaders.add(HRDS_UNSUPPORTED_PUBLISHER_ID);
            return false;
        }

        //Regular expression to match UUID format
        String uuidPattern = "[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$";
        if (!currPublisherId.matches(uuidPattern))
        {
            errorResponseHeaders.add(HRDS_UNSUPPORTED_PUBLISHER_ID);
            return false;
        }

        return true;
    }

    //Validates mode, must be publish live or publish archive
    public static boolean validateMode(String mode, List<String> errorResponseHeaders)
    {
        String currMode = checkForEmptyString(mode, HDRS_MODE);

        if (currMode == null)
        {
            errorResponseHeaders.add(HDRS_UNSUPPORTED_MODE);
            return false;
        }

        //Checks if supported mode
        if (!currMode.equals(MSG_PUBLISH_LIVE) && !currMode.equals(MSG_PUBLISH_ARCHIVE))
        {
            errorResponseHeaders.add(HDRS_UNSUPPORTED_MODE);
            return false;
        }

        return true;
    }

    //Validates supported digest
    public static String validateDigests(String digests,  HashMap<String, String> successResponseHeaders, List<String> errorResponseHeaders)
    {
        String currDigests = checkForEmptyString(digests, HDRS_ACCEPT_DIGEST);
        String selectedDigest = null;

        if (currDigests == null)
        {
            errorResponseHeaders.add(HDRS_UNSUPPORTED_DIGEST);
            return null;
        }

        List<String> acceptDigests = parseHeaderList(digests);

        //Check to ensure the digest is valid, the first one found is the preferred value (currently only SHA256)
        for (String currDigest : acceptDigests)
        {
            if (currDigest.equals(DigestMethod.SHA256))
            {
                selectedDigest = DigestMethod.SHA256;
                successResponseHeaders.put(HDRS_DIGEST, selectedDigest);
                return selectedDigest;
            }
        }

        errorResponseHeaders.add(HDRS_UNSUPPORTED_DIGEST);
        return null;
    }

    //Validates supported xml compression
    public static String validateXmlCompression(String xmlCompressions,  HashMap<String, String> successResponseHeaders, List<String> errorResponseHeaders)
    {
        String selectedXmlCompression = null;
        String currXmlCompressions = checkForEmptyString(xmlCompressions, HDRS_ACCEPT_XML_COMPRESSION);

        if (currXmlCompressions == null)
        {
            errorResponseHeaders.add(HDRS_UNSUPPORTED_XML_COMPRESSION);
            return null;
        }

        List<String> acceptedXmlCompressions = parseHeaderList(currXmlCompressions);

        //Check to ensure the encoding is valid, only xml accepted, the first one found is the preferred value
        List<String> supportedXmlCompressionList = Arrays.asList(SUPPORTED_XML_COMPRESSIONS);
        for (String currXmlCompression : acceptedXmlCompressions)
        {

            if (supportedXmlCompressionList.contains(HttpUtils.checkForEmptyString(currXmlCompression, "")))
            {
                selectedXmlCompression = currXmlCompression;
                successResponseHeaders.put(HDRS_XML_COMPRESSION, selectedXmlCompression);
                return selectedXmlCompression;
            }
        }

        errorResponseHeaders.add(HDRS_UNSUPPORTED_XML_COMPRESSION);
        return null;
    }


    //Validates dataClass, must be journal, audit, or log
    public static boolean validateDataClass(String dataClass, String supportedDataClass, List<String> errorResponseHeaders)
    {
        String currDataClass = checkForEmptyString(dataClass, HDRS_DATA_CLASS);

        if (currDataClass == null)
        {
            errorResponseHeaders.add(HDRS_UNSUPPORTED_DATACLASS);
            return false;
        }

        //Checks if supported data class.
        if (!currDataClass.equals(supportedDataClass))
        {
            errorResponseHeaders.add(HDRS_UNSUPPORTED_DATACLASS);
            return false;
        }

        //TODO check to only allow journal/audit/log based upon config file

        return true;
    }

    //Validates version, must be 2.0
    public static boolean validateVersion(String version, List<String> errorResponseHeaders)
    {
        String currVersion = checkForEmptyString(version, HDRS_VERSION);

        //Checks if supported version, only 2.0 is currently supported
        List<String> supportedVersionsList = Arrays.asList(SUPPORTED_VERSIONS);
        if (currVersion == null || !supportedVersionsList.contains(HttpUtils.checkForEmptyString(currVersion, "")))
        {
            errorResponseHeaders.add(HDRS_UNSUPPORTED_VERSION);
            return false;
        }

        return true;
    }

    //Validates configure digest challenge, must be on/off
    public static String validateConfigureDigestChallenge(String configureDigestChallenge, HashMap<String, String> successResponseHeaders, List<String> errorResponseHeaders)
    {
        String currConfigDigests = checkForEmptyString(configureDigestChallenge, HDRS_ACCEPT_CONFIGURE_DIGEST_CHALLENGE);
        String selectedConfigureDigest = null;

        //Checks if supported configure digest challenge, only on/off is supported
        if (currConfigDigests == null)
        {
            errorResponseHeaders.add(HDRS_UNSUPPORTED_CONFIGURE_DIGEST_CHALLENGE);
            return null;
        }

        List<String> acceptedConfigDigests = parseHeaderList(currConfigDigests);

        //Check to ensure the configre digest challenge is valid, only on/off, the first one found is the preferred value
        for (String currConfigDigest : acceptedConfigDigests)
        {
            if (HttpUtils.getAllowedConfigureDigests().contains(HttpUtils.checkForEmptyString(currConfigDigest, "")))
            {
                selectedConfigureDigest = currConfigDigest;
                successResponseHeaders.put(HDRS_CONFIGURE_DIGEST_CHALLENGE, currConfigDigest);
                return selectedConfigureDigest;
            }
        }

        errorResponseHeaders.add(HDRS_UNSUPPORTED_CONFIGURE_DIGEST_CHALLENGE);
        return null;
    }

    public static String readBinaryDataFromRequest(HttpServletRequest request, int currRequestCount) throws IOException
    {
        try
        {
            MessageDigest md = MessageDigest.getInstance("SHA-256");

            //Handle the binary data that was posted from the client in the request
            byte[] readRequestBuffer = new byte[BUFFER_SIZE];
            String outputDirStr = System.getProperty("user.home") + "/jalop/jjnl/subscriberOutput/";
            File outputDir = new File(outputDirStr);
            if (!outputDir.isDirectory())
            {
                outputDir.mkdirs();
            }
            File fileUpload = new File(outputDirStr + currRequestCount +".bin");
            try (
                InputStream input = request.getInputStream(); //Reading the binary post data in the request
                OutputStream output = new FileOutputStream(fileUpload);
                )
            {
                for (int length = 0; (length = input.read(readRequestBuffer)) > 0;)
                {
                    //Builds digest
                    md.update(readRequestBuffer, 0, length);
                    output.write(readRequestBuffer, 0, length);
                }
            }

            //Generates digest
            return HttpUtils.getDigest(md);
        }
        catch (NoSuchAlgorithmException nsae)
        {
            System.out.println("Digest algorithm not supported");
            return null;
        }
    }

    public static Subscriber getSubscriber() {
        return subscriber;
    }

    public static void setSubscriber(Subscriber subscriber) {
        HttpUtils.subscriber = subscriber;
    }

    public static RecordType getRecordType(String dataClass)
    {
        RecordType recordType = RecordType.Unset;

        if (dataClass != null)
        {
            if (dataClass.equalsIgnoreCase(JOURNAL)) {
                recordType = RecordType.Journal;
            } else if (dataClass.equalsIgnoreCase(AUDIT)) {
                recordType = RecordType.Audit;
            } else if (dataClass.equalsIgnoreCase(LOG)) {
                recordType = RecordType.Log;
            }
        }

        return recordType;
    }
}
