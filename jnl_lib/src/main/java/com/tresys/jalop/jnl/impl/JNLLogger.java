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