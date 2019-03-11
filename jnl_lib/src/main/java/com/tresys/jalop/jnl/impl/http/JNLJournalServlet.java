package com.tresys.jalop.jnl.impl.http;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;


@SuppressWarnings("serial")
public class JNLJournalServlet extends HttpServlet implements JNLServlet
{
    /** Logger for this class */
    private static final Logger logger = Logger.getLogger(JNLJournalServlet.class);


    static final int BUFFER_SIZE = 4096;

    /**
     * Create a JNLTest object based on the specified configuration.
     *
     * @param config
     *            A {@link Config}
     */
    public JNLJournalServlet() {

    }

    @Override
    protected void doPost(HttpServletRequest request,
            HttpServletResponse response)
                    throws ServletException, IOException {

        HttpUtils.handleRequest(request, response, getSupportedDataClass());
    }

    @Override
    public String getSupportedDataClass() {
        return "journal";
    }
}

