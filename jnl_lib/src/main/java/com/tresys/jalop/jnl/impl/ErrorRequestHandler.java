package com.tresys.jalop.jnl.impl;

import org.apache.log4j.Logger;
import org.beepcore.beep.core.MessageMSG;
import org.beepcore.beep.core.RequestHandler;

/**
 * Request Handler for invalid channels which shouldn't be getting any messages.
 */
public class ErrorRequestHandler implements RequestHandler {

	static Logger log = Logger.getLogger(ErrorRequestHandler.class);

	@Override
	public void receiveMSG(final MessageMSG msg) {
		log.error("Error - Message sent on a closed channel: " + msg.getChannel().getNumber());
	}

}
