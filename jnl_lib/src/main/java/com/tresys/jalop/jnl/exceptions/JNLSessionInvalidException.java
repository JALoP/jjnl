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

package com.tresys.jalop.jnl.exceptions;

import java.io.Serializable;

/**
 * This class represents that something went wrong when processing a message. This class can be extended to encompass specific types of
 * message processing errors.
 */
public class JNLSessionInvalidException extends JNLException {

    /**
     * the serial version, because {@link Exception} implements
     * {@link Serializable}
     */
    private static final long serialVersionUID = 1L;

    private String sessionId = "";
    private String jalId = "";

    /**
     * Create an Exception that is specific to the JALoP Network Library and processing a message
     *
     * @param string    The message that describes the exception.
     */
    public JNLSessionInvalidException(final String string) {
        super(string);
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getJalId() {
        return jalId;
    }

    public void setJalId(String jalId) {
        this.jalId = jalId;
    }

}
