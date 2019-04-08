package com.tresys.jalop.jnl.impl.http;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.tresys.jalop.jnl.RecordType;

@SuppressWarnings("serial")
public class JNLAuditServlet extends HttpServlet implements JNLServlet
{
    /** Logger for this class */
    private static final Logger logger = Logger.getLogger(JNLAuditServlet.class);

    /**
     * Create a JNLTest object based on the specified configuration.
     *
     * @param config
     *            A {@link Config}
     */
    public JNLAuditServlet() {

    }

    @Override
    protected void doPost(HttpServletRequest request,
            HttpServletResponse response)
                    throws ServletException, IOException {

        MessageProcessor.handleRequest(request, response, getSupportedRecordType());
    }

    @Override
    public RecordType getSupportedRecordType() {
        return RecordType.Audit;
    }
}
