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

    /**
     * Create an Exception that is specific to the JALoP Network Library and processing a message
     *
     * @param string    The message that describes the exception.
     */
    public JNLSessionInvalidException(final String string) {
        super(string);
    }

}
