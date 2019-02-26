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
#include <sys/stat.h>
#include <curl/curl.h>


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
    return numbytes;
}

int main(void)
{
    CURL *curl;
    CURLcode res;

    //Path to where the binary file data return in the http response will be written to.
    static const char *outputFilename = "output.iso";
    FILE *outputFile;

    FILE *fd;
    struct stat file_info;

    //Source file to post to the jetty http servlet
    char *inputFilename = "test.iso";
    fd = fopen(inputFilename, "rb");
    if (!fd)
    {
        fprintf(stderr, "Could not open file.\n");
        return 1;
    }

    if (fstat(fileno(fd), &file_info) != 0)
    {
        fprintf(stderr, "Could not get file information.\n");
        return 1;
    }

    curl = curl_easy_init();
    if(curl)
    {
        struct curl_slist *headers=NULL;
        headers = curl_slist_append(headers, "Content-Type: application/octet-stream");

        //Set Jalop initialize message
        headers = curl_slist_append(headers, "JAL-Message: initialize");

        //Valid initialize headers
        headers = curl_slist_append(headers, "JAL-Mode: publish-live");
        headers = curl_slist_append(headers, "JAL-Accept-Digest: junk digest,http://www.w3.org/2001/04/xmlenc#sha256");
        headers = curl_slist_append(headers, "JAL-Accept-XML-Compression: junk compression, none");
        headers = curl_slist_append(headers, "JAL-Data-Class: journal");
        headers = curl_slist_append(headers, "JAL-Version: 2.0");

        /* set URL to get here */
        //  curl_easy_setopt(curl, CURLOPT_URL, "http://localhost:8080/JalopHttpServer/JalopHttpServlet");
        //URL to the servlet processing the post
        curl_easy_setopt(curl, CURLOPT_URL, "https://localhost:8444/");
        curl_easy_setopt(curl, CURLOPT_VERBOSE, 1L);

        //Disable verify server cert with known CAs to use for development with self signed certs
        curl_easy_setopt(curl, CURLOPT_SSL_VERIFYPEER, 0);

        curl_easy_setopt(curl, CURLOPT_SSLCERT, "./cert.pem");
        curl_easy_setopt(curl, CURLOPT_SSLKEY, "./key.pem");

        //post binary data
        // curl_easy_setopt(easyhandle, CURLOPT_POSTFIELDS, binaryptr);
        curl_easy_setopt(curl, CURLOPT_READDATA, fd);
        curl_easy_setopt(curl, CURLOPT_POST, 1L);

        //set the size of the postfields data
        curl_easy_setopt(curl, CURLOPT_POSTFIELDSIZE, file_info.st_size);

        // pass our list of custom made headers
        curl_easy_setopt(curl, CURLOPT_HTTPHEADER, headers);

        /* send all data to this function  */
        curl_easy_setopt(curl, CURLOPT_WRITEFUNCTION, write_data);

        //Function to read response headers
        curl_easy_setopt(curl, CURLOPT_HEADERFUNCTION, header_callback);

        /* open the file */
        outputFile = fopen(outputFilename, "wb");
        if(outputFile)
        {
            /* write the page body to this file handle */
            curl_easy_setopt(curl, CURLOPT_WRITEDATA, outputFile);

            curl_easy_perform(curl); /* post away! */

            /* Check for errors */
            if(res != CURLE_OK)
            {
                fprintf(stderr, "curl_easy_perform() failed: %s\n",
                curl_easy_strerror(res));
            }
        }

        /* always cleanup */
        curl_easy_cleanup(curl);
    }
    return 0;
}
