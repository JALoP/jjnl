/**
 * Copyright (C) 2026 Concurrent Technologies Corporation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/

package com.tresys.jalop.jnl.impl.http;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.tresys.jalop.jnl.RecordType;

@SuppressWarnings("serial")
public class JNLLogServlet extends HttpServlet implements JNLServlet
{
    private HttpUtils httpUtils;

    /**
     * Create a JNLTest object based on the specified configuration.
     *
     * @param config
     *            A {@link Config}
     */
    public JNLLogServlet() {};

    public void setHttpUtils(HttpUtils httpUtils) {
        this.httpUtils = httpUtils;
    }

    @Override
    protected void doPost(HttpServletRequest request,
            HttpServletResponse response)
                    throws ServletException, IOException {

        MessageProcessor.handleRequest(request, response, getSupportedRecordType(), httpUtils);
    }

    @Override
    public RecordType getSupportedRecordType(){
        return RecordType.Log;
    }
}

