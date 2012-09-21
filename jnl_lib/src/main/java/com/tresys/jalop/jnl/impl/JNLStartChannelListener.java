/*
 * Source code in 3rd-party is licensed and owned by their respective
 * copyright holders.
 *
 * All other source code is copyright Tresys Technology and licensed as below.
 *
 * Copyright (c) 2012 Tresys Technology LLC, Columbia, Maryland, USA
 *
 * This software was developed by Tresys Technology LLC
 * with U.S. Government sponsorship.
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

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.beepcore.beep.core.BEEPError;
import org.beepcore.beep.core.BEEPException;
import org.beepcore.beep.core.Channel;
import org.beepcore.beep.core.CloseChannelException;
import org.beepcore.beep.core.Session;
import org.beepcore.beep.core.StartChannelException;
import org.beepcore.beep.core.StartChannelListener;

/**
 * Class to handle close requests for a channel.
 */
public class JNLStartChannelListener implements StartChannelListener {

	static final Logger log = Logger.getLogger(JNLStartChannelListener.class);

	@Override
	public boolean advertiseProfile(final Session session) throws BEEPException {
		return session.getTuningProperties().getEncrypted();
	}

	@Override
	public void closeChannel(final Channel channel) throws CloseChannelException {
		if(log.isDebugEnabled()) {
			log.debug("Closing channel number: " + channel.getNumber());
		}
	}

	@Override
	public void startChannel(final Channel channel, final String encoding, final String data)
			throws StartChannelException {
		if(log.isEnabledFor(Level.ERROR)) {
			log.error("JNLStartChannelListener received a request to start a channel, which shouldn't happen");
		}
		throw new StartChannelException(BEEPError.CODE_REQUESTED_ACTION_ABORTED,
				"Error - Can't start a channel from JNLStartChannelListener");
	}

}
