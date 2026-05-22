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

package com.tresys.jalop.jnl.impl;

import org.apache.log4j.Logger;
import org.apache.logging.log4j.spi.LoggerContext;

import com.tresys.jalop.jnl.JNLLog;

public class JNLLogger implements JNLLog {

    private Logger logger;
    public JNLLogger(Logger logger)
    {
        if (logger == null)
        {
            throw new IllegalArgumentException("logger cannot be null.");
        }
        this.logger = logger;
    }

    public void warn(String msg, Throwable t) {
        logger.warn(msg, t);
    }

    public void warn(String msg) {
        logger.warn(msg);
    }

    public void trace(String msg, Throwable t) {
        logger.trace(msg, t);
    }

    public void trace(String msg) {
        logger.trace(msg);
    }

    public void info(String msg, Throwable t) {
        logger.info(msg, t);
    }

    public void info(String msg) {
        logger.info(msg);
    }

    public void error(String msg, Throwable t) {
        logger.error(msg, t);
    }

    public void error(String msg) {
        logger.error(msg);
    }

    public void debug(String msg, Throwable t) {
        logger.debug(msg, t);
    }

    public void debug(String msg) {
        logger.debug(msg);
    }

    public boolean isWarnEnabled() {
        return logger.isWarnEnabled();
    }

    public boolean isTraceEnabled() {
        return logger.isTraceEnabled();
    }

    public boolean isInfoEnabled() {
        return logger.isInfoEnabled();
    }

    public boolean isErrorEnabled() {
        return logger.isErrorEnabled();
    }

    public boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }
}