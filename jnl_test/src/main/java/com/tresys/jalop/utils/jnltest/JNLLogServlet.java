package com.tresys.jalop.utils.jnltest;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.tresys.jalop.utils.jnltest.Config.Config;


@SuppressWarnings("serial")
public class JNLLogServlet extends HttpServlet implements JNLServlet
{
    /** Logger for this class */
    private static final Logger logger = Logger.getLogger(JNLLogServlet.class);


    static final int BUFFER_SIZE = 4096;

    /**
     * Create a JNLTest object based on the specified configuration.
     *
     * @param config
     *            A {@link Config}
     */
    public JNLLogServlet() {

    }

    @Override
    protected void doPost(HttpServletRequest request,
            HttpServletResponse response)
                    throws ServletException, IOException {

        HttpUtils.handleRequest(request, response, getMode());
    }

    @Override
    public String getMode() {
        return "log";
    }
}

