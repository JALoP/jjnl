/***************************************************************************
 *                                  _   _ ____  _
 *  Project                     ___| | | |  _ \| |
 *                             / __| | | | |_) | |
 *                            | (__| |_| |  _ <| |___
 *                             \___|\___/|_| \_\_____|
 *
 * Copyright (C) 1998 - 2018, Daniel Stenberg, <daniel@haxx.se>, et al.
 *
 * This software is licensed as described in the file COPYING, which
 * you should have received as part of this distribution. The terms
 * are also available at https://curl.haxx.se/docs/copyright.html.
 *
 * You may opt to use, copy, modify, merge, publish, distribute and/or sell
 * copies of the Software, and permit persons to whom the Software is
 * furnished to do so, under the terms of the COPYING file.
 *
 * This software is distributed on an "AS IS" basis, WITHOUT WARRANTY OF ANY
 * KIND, either express or implied.
 *
 ***************************************************************************/
/* <DESC>
 * Very simple HTTP GET
 * </DESC>
 */
#include <stdio.h>
#include <iostream>
#include <sys/stat.h>
#include <curl/curl.h>
#include <stdlib.h>
#include <string>
#include <vector>
#include <unordered_map>
#include <boost/algorithm/string.hpp>
#include <boost/lexical_cast.hpp>
#include <fstream>
#include <sstream>
#include <algorithm>
#include <vector>
#include <fcntl.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <sys/stat.h>


std::unordered_map<std::string, std::string> headerMap;

std::string auditSessionId;
std::string journalSessionId;
std::string logSessionId;
CURL *curl;

bool sendLogRecords = false;
bool sendAuditRecords = false;
bool sendJournalRecords = false;
bool performDigestResponse = true;
bool performCloseSession = true;

long recordCount = 0;

const std::string AUDIT = "audit";
const std::string JOURNAL = "journal";
const std::string LOG = "log";

long recordSize = 0;

std::string subscriberUrl = "http://localhost:8080";

void usage()
{
    std::cout << std::endl << "Usage: " << std::endl << std::endl <<
              "test_publisher <record size to send in kb> <record types to send, a=audit, log=log, j=journal> (ex: a,l,j for all three record types) <number of records per record type to send, put 'i' for infinite record sending> <Send digest response 1=yes, 0=no> <Perform close session 1=yes, 0=no> <OPTIONAL: url of publisher> (default if left empty: http://localhost:8080)"
                                                                                     << std::endl << std::endl;
}

bool executeCommand(const std::string& command, std::vector<char *>& cmdArgs, bool wait, bool isDebug)
{
    pid_t childpid;
    int status = -1;
    {
        if ((childpid = fork()) == 0)
        {
            if (!isDebug)
            {
                 /* open /dev/null for writing, hides all stdout and stderr if not debug*/
                int fd = open("/dev/null", O_WRONLY);

                dup2(fd, 1);    /* make stdout a copy of fd (> /dev/null) */
                dup2(fd, 2);    /* ...and same with stderr */
                close(fd);      /* close fd */
            }
            execve(command.c_str(), &cmdArgs[0], environ);

            exit(1);
        }

        if (wait)
        {
            waitpid(childpid, &status, 0);

            if (status != 0)
            {
                return false;
            }
            else
            {
                return true;
            }
        }
        else
        {
            return true;
        }
    }
}

bool generateJalRecord(std::string recordSize)
{
    std::vector<char *> cmdArgs;
    std::string executablePath = "/bin/sh";
    cmdArgs.push_back((char*)executablePath.c_str());
    cmdArgs.push_back((char*)"generate_record.sh");
    cmdArgs.push_back((char*)recordSize.c_str());
    cmdArgs.push_back((char*)0);

    return executeCommand(executablePath, cmdArgs, true, false);
}

std::string getSessionIdByRecordType(std::string recordType)
{
    if (AUDIT == recordType)
    {
        return auditSessionId;
    }
    else if (JOURNAL == recordType)
    {
        return journalSessionId;
    }
    else if (LOG == recordType)
    {
        return logSessionId;
    }
}

void setSessionIdByRecordType(std::string sessionId, std::string recordType)
{
    if (AUDIT == recordType)
    {
        auditSessionId = sessionId;
    }
    else if (JOURNAL == recordType)
    {
        journalSessionId = sessionId;
    }
    else if (LOG == recordType)
    {
        logSessionId = sessionId;
    }
}

static size_t write_data(void *ptr, size_t size, size_t nmemb, void *stream)
{
    size_t written = fwrite(ptr, size, nmemb, (FILE *)stream);
    return written;
}

static size_t header_callback(char *buffer, size_t size,
                              size_t nitems, void *userdata)
{
    size_t numbytes = size * nitems;
    printf("%.*s\n", numbytes, buffer);

    //Handle headers, split on comma
    std::vector<std::string> result;
    std::string input = std::string(buffer);
    boost::split(result, input, boost::is_any_of(":"));

    //Must only be two for header key/value pair
    if (result.size() == 2)
    {
        std::string headerKey = result[0];
        std::string headerValue = result[1];

        boost::algorithm::trim(headerKey);
        boost::algorithm::trim(headerValue);
        //printf("header key: %s\n", headerKey.c_str());
        //printf("header value: %s\n", headerValue.c_str());

        headerMap[headerKey] = headerValue;
    }

    return numbytes;
}

struct curl_slist * getInitializeHeaders(std::string recordType)
{
    struct curl_slist *headers=NULL;
    headers = curl_slist_append(headers, "Content-Type: application/http+jalop");
    headers = curl_slist_append(headers, "Transfer-Encoding: binary");

    //Set Jalop initialize message
    headers = curl_slist_append(headers, "JAL-Message: initialize");

    //Valid initialize headers
    headers = curl_slist_append(headers, "JAL-Mode: archival");
    headers = curl_slist_append(headers, "JAL-Publisher-Id: ae8a54d7-dd7c-4c50-a7e7-f948a140c556");
    headers = curl_slist_append(headers, "JAL-Accept-Digest: junk digest,http://www.w3.org/2001/04/xmlenc#sha256");
    headers = curl_slist_append(headers, "JAL-Accept-XML-Compression: junk compression, none");

    std::string recordTypeMsg = "JAL-Record-Type: " + recordType;
    headers = curl_slist_append(headers, recordTypeMsg.c_str());
    headers = curl_slist_append(headers, "JAL-Version: 2.0.0.0");
    headers = curl_slist_append(headers, "JAL-Accept-Configure-Digest-Challenge: on");

    return headers;
}

struct curl_slist * getCloseSessionHeaders(std::string recordType)
{
    struct curl_slist *headers=NULL;

    headers = curl_slist_append(headers, "JAL-Message: close-session");

    std::string sessionHeader = "JAL-Session-Id: " + getSessionIdByRecordType(recordType);
    headers = curl_slist_append(headers, sessionHeader.c_str());

    return headers;
}

struct curl_slist * getJALRecordHeaders(std::string recordType, std::string jalId, std::string sysMetadataLength, std::string appMetadataLength, std::string payloadLength)
{
    struct curl_slist *headers=NULL;

    //Sets session id
    std::string sessionIdStr = "JAL-Session-Id: " + getSessionIdByRecordType(recordType);
    headers = curl_slist_append(headers, sessionIdStr.c_str());

    headers = curl_slist_append(headers, "Content-Type: application/http+jalop");
    headers = curl_slist_append(headers, "Transfer-Encoding: binary");

    //Adds JAL-Audit-Format if audit record
    if (recordType == AUDIT)
    {
        headers = curl_slist_append(headers, "JAL-Audit-Format: xml");
    }

    //JAL record headers
    std::string jalIdStr = "JAL-Id: " + jalId;
    headers = curl_slist_append(headers, jalIdStr.c_str());

    std::string sysMetadataStr = "JAL-System-Metadata-Length: " + sysMetadataLength;
    headers = curl_slist_append(headers, sysMetadataStr.c_str());

    std::string appMetadataStr = "JAL-Application-Metadata-Length: " + appMetadataLength;
    headers = curl_slist_append(headers, appMetadataStr.c_str());

    std::string payloadStr;
    if (AUDIT == recordType)
    {
        payloadStr = "JAL-Audit-Length: ";
    }
    else if (JOURNAL == recordType)
    {
        payloadStr = "JAL-Journal-Length: ";
    }
    else
    {
        payloadStr = "JAL-Log-Length: ";
    }
    payloadStr = payloadStr + payloadLength;
    headers = curl_slist_append(headers, payloadStr.c_str());

    std::string recordTypeMsg = "JAL-Message: " + recordType + "-record";
    headers = curl_slist_append(headers, recordTypeMsg.c_str());

    return headers;
}

struct curl_slist * getDigestResponseHeaders(std::string jalId, std::string recordType)
{
    struct curl_slist *headers=NULL;
    headers = curl_slist_append(headers, "Content-Type: application/http+jalop");
    headers = curl_slist_append(headers, "Transfer-Encoding: binary");

    //Set Jalop initialize message
    headers = curl_slist_append(headers, "JAL-Message: digest-response");

    //Valid initialize headers
    std::string jalIdStr = "JAL-Id: " + jalId;
    headers = curl_slist_append(headers, jalIdStr.c_str());
    headers = curl_slist_append(headers, "JAL-Digest-Status: confirmed");


    //Sets session id
    std::string sessionIdStr = "JAL-Session-Id: " + getSessionIdByRecordType(recordType);
    headers = curl_slist_append(headers, sessionIdStr.c_str());

    return headers;
}

bool performHttpPost(struct curl_slist *headers, bool sendBinaryData, std::string recordType, std::string filename)
{
    CURLcode res;


    if(curl)
    {
        //URL to the servlet processing the post
        std::string postUrl = subscriberUrl + "/" + recordType;
        curl_easy_setopt(curl, CURLOPT_URL, postUrl.c_str());
        curl_easy_setopt(curl, CURLOPT_VERBOSE, 1L);

        //Disable verify server cert with known CAs to use for development with self signed certs
        curl_easy_setopt(curl, CURLOPT_SSL_VERIFYPEER, 0);

        curl_easy_setopt(curl, CURLOPT_SSLCERT, "./cert.pem");
        curl_easy_setopt(curl, CURLOPT_SSLKEY, "./key.pem");

        FILE *fd = NULL;

        //post binary data - just for testing right now, sending file
        if (sendBinaryData)
        {
            struct stat file_info;

            //Source file to post to the jetty http servlet
            char *inputFilename = (char*)filename.c_str();
            fd = fopen(inputFilename, "rb");
            if (!fd)
            {
                fprintf(stderr, "Could not open file.\n");
                if (fd != NULL)
                {
                    fclose(fd);
                }
                return 1;
            }

            if (fstat(fileno(fd), &file_info) != 0)
            {
                fprintf(stderr, "Could not get file information.\n");
                if (fd != NULL)
                {
                    fclose(fd);
                }
                return 1;
            }
            // curl_easy_setopt(easyhandle, CURLOPT_POSTFIELDS, binaryptr);
            curl_easy_setopt(curl, CURLOPT_READDATA, fd);
            curl_easy_setopt(curl, CURLOPT_POST, 1L);

            //set the size of the postfields data
            curl_easy_setopt(curl, CURLOPT_POSTFIELDSIZE, file_info.st_size);
        }
        else
        {
            //No binary data in body of post, just sending header message.
            curl_easy_setopt(curl, CURLOPT_POST, 1L);

            //set the size of the postfields data
            curl_easy_setopt(curl, CURLOPT_POSTFIELDSIZE, 0);
        }

        // pass our list of custom made headers
        curl_easy_setopt(curl, CURLOPT_HTTPHEADER, headers);

        /* send all data to this function  */
        curl_easy_setopt(curl, CURLOPT_WRITEFUNCTION, write_data);

        //Function to read response headers
        curl_easy_setopt(curl, CURLOPT_HEADERFUNCTION, header_callback);

        //Perform the post
        res = curl_easy_perform(curl); /* post away! */

        /* Check for errors */
        bool success = false;
        if(res != CURLE_OK)
        {
            success = false;
            fprintf(stderr, "curl_easy_perform() failed: %s\n",
            curl_easy_strerror(res));
        }
        else
        {
            success = true;
            fprintf(stdout, "Request was sucessful\n");
        }

        /* always cleanup */
        curl_easy_reset(curl);

        if (fd != NULL)
        {
            fclose(fd);
        }
        return success;
    }
}

bool processJALRecordResponse(std::string recordType, std::string jalId)
{
    //Process response header
    if (headerMap.find("JAL-Message") != headerMap.end())
    {
        fprintf(stdout, "%s\n", headerMap["JAL-Message"].c_str());

        if (performDigestResponse)
        {
            return performHttpPost(getDigestResponseHeaders(jalId, recordType), false, recordType, "");
        }
        else
        {
            return true;
        }
    }
    else
    {
        fprintf(stdout, "Missing JAL-Message header in response.\n");
        return false;
    }

    return true;
}

bool sendJalRecords(std::string recordType)
{
    //Sending jal record after successful initialize
    fprintf(stdout, "Received initialize-ack, sending jal record\n");
    std::string jalId = "2ef4e71c-5971-4349-9169-d1e8a2e9450b_2013-11-22T16:09:46.43660-05:00_20705_3167946496";

    //If record size was passed in via command line, then a jal record named jal_record.txt was generated with that specific payload size
    //If no record size was passed in, use the default 1kb jal record file
    std::string recordFilename = "good_audit_input.txt";
    std::string payloadSize = "1040";
    if (recordSize > 0)
    {
        payloadSize = boost::lexical_cast<std::string>(recordSize);
        recordFilename = "jal_record.txt";
    }

    struct curl_slist *rec1headers = getJALRecordHeaders(recordType, jalId, "3083", "1179", payloadSize);

    if (!performHttpPost(rec1headers, true, recordType, recordFilename))
    {
        fprintf(stdout, "JAL-record post failed.\n");
        return false;
    }
    processJALRecordResponse(recordType, jalId);
}

bool processInitializeResponse(std::string recordType)
{
    //Process response header
    if (headerMap.find("JAL-Message") != headerMap.end())
    {
        fprintf(stdout, "%s\n", headerMap["JAL-Message"].c_str());
        if (headerMap["JAL-Message"] == "initialize-ack")
        {
            //Sending jal record after successful initialize
            fprintf(stdout, "Received initialize-ack, sending jal record\n");
            return sendJalRecords(recordType);
        }
        else
        {
            fprintf(stdout, "initialize-nack received. Jalop connection rejected.\n");
            return false;
        }
    }
    else
    {
        fprintf(stdout, "Missing JAL-Message header in response.\n");
        return false;
    }

    return true;
}

int main(int argc, char *argv[])
{
    //parse record size arg in bytes
    if (argc > 1 && argv[1] != NULL)
    {
        std::string recordSizeKBStr = argv[1];
        long recordSizeKB = atol(recordSizeKBStr.c_str());
        if (recordSizeKB != 0)
        {
            //Converts kb to bytes
            recordSize = recordSizeKB * 1024;
            std::string recordSizeBytes = boost::lexical_cast<std::string>(recordSize);
            fprintf(stdout, "Using record size of %s bytes\n", recordSizeBytes.c_str());

            //Generates record
            if (!generateJalRecord(recordSizeKBStr))
            {
                fprintf(stdout, "Failed to generate JAL record with size of %s", recordSizeBytes.c_str());
                exit(1);
            }
        }
    }
    else
    {
        std::cout << "ERROR: missing record size parameter" << std::endl;
        usage();
        exit(1);
    }

    //gets record types
    std::string recordTypes;
    if (argc > 2 && argv[2] != NULL)
    {
        recordTypes = argv[2];

        if (recordTypes.find("l") != std::string::npos)
        {
            sendLogRecords = true;
        }

        if (recordTypes.find("j") != std::string::npos)
        {
            sendJournalRecords = true;
        }

        if (recordTypes.find("a") != std::string::npos)
        {
            sendAuditRecords = true;
        }

        if (!sendLogRecords && !sendAuditRecords && sendJournalRecords)
        {
            std::cout << "ERROR: missing or incorrect record type parameter" << std::endl;
            usage();
            exit(1);
        }

    }
    else
    {
        std::cout << "ERROR: missing record type parameter" << std::endl;
        usage();
        exit(1);
    }

    //gets record count
    std::string recordCountStr;
    if (argc > 3 && argv[3] != NULL)
    {
        recordCountStr = argv[3];

        recordCount = atol(recordCountStr.c_str());
    }
    else
    {
        std::cout << "ERROR: missing record count parameter" << std::endl;
        usage();
        exit(1);
    }

    //gets perform digest response
    std::string performDigestResponseStr;
    if (argc > 4 && argv[4] != NULL)
    {
        performDigestResponseStr = argv[4];

        if ("1" == performDigestResponseStr)
        {
            performDigestResponse = true;
        }
        else if ("0" == performDigestResponseStr)
        {
            performDigestResponse = false;
        }
        else
        {
            std::cout << "ERROR: missing or incorrect perform digest response parameter" << std::endl;
            usage();
            exit(1);
        }
    }
    else
    {
        std::cout << "ERROR: missing perform digest response parameter" << std::endl;
        usage();
        exit(1);
    }

    std::string performCloseSessionStr;
    if (argc > 5 && argv[5] != NULL)
    {
        performCloseSessionStr = argv[5];

        if ("1" == performCloseSessionStr)
        {
            performCloseSession = true;
        }
        else if ("0" == performCloseSessionStr)
        {
            performCloseSession = false;
        }
        else
        {
            std::cout << "ERROR: missing or incorrect perform close session parameter" << std::endl;
            usage();
            exit(1);
        }
    }
    else
    {
        std::cout << "ERROR: missing perform close session parameter" << std::endl;
        usage();
        exit(1);
    }

    //optional param for subscriber url
    if (argc > 6 && argv[6] != NULL)
    {
        subscriberUrl = argv[6];
    }

    curl = curl_easy_init();
    std::string currRecordType = "journal";

    if (sendJournalRecords)
    {
        //Send initialize message to journal channel
        if (!performHttpPost(getInitializeHeaders(currRecordType), false, currRecordType, ""))
        {
            fprintf(stdout, "Initialize HTTP post failed.\n");
            exit(1);
        }

        std::string sessionId = headerMap["JAL-Session-Id"];
        setSessionIdByRecordType(sessionId, currRecordType);
    }

    if (sendAuditRecords)
    {
        //Send initialize message to audit channel
        currRecordType = "audit";
        if (!performHttpPost(getInitializeHeaders(currRecordType), false, currRecordType, ""))
        {
            fprintf(stdout, "Initialize HTTP post failed.\n");
            exit(1);
        }

        std::string sessionId = headerMap["JAL-Session-Id"];
        setSessionIdByRecordType(sessionId, currRecordType);
    }

    if (sendLogRecords)
    {
        //Send initialize message to log channel
        currRecordType = "log";
        if (!performHttpPost(getInitializeHeaders(currRecordType), false, currRecordType, ""))
        {
            fprintf(stdout, "Initialize HTTP post failed.\n");
            exit(1);
        }
        std::string  sessionId = headerMap["JAL-Session-Id"];
        setSessionIdByRecordType(sessionId, currRecordType);
    }

    //Send record loop
    long currRecordCount = 0;
    while(1)
    {
        if (sendJournalRecords)
        {
            sendJalRecords(JOURNAL);
        }

        //If successful post, then proccess response
        if (sendAuditRecords)
        {
            sendJalRecords(AUDIT);
        }

        //If successful post, then proccess response
        if (sendLogRecords)
        {
            sendJalRecords(LOG);
        }

        currRecordCount ++;

        //Checks if record count has been reached, if 0 then infinitely send records
        if (recordCount != 0)
        {
            if (currRecordCount == recordCount)
            {
                break;
            }
        }
    }

    //Performs close session if configured
    if (performCloseSession)
    {
        if (sendJournalRecords)
        {
            performHttpPost(getCloseSessionHeaders(JOURNAL), false, JOURNAL, "");
        }

        if (sendAuditRecords)
        {
            performHttpPost(getCloseSessionHeaders(AUDIT), false, AUDIT, "");
        }

        if (sendLogRecords)
        {
            performHttpPost(getCloseSessionHeaders(LOG), false, LOG, "");
        }
    }

    return 0;
}
