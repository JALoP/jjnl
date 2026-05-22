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
