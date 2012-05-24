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

package com.tresys.jalop.jnl;

import java.net.InetAddress;
import java.util.Set;

/**
 * This represents a 'connect-nack' message from the remote JALoP Network Store.
 * 
 * @see ConnectionHandler#connectNack(Session, ConnectNack)
 */
public interface ConnectNack {
	/**
	 * Retrieve the {@link InetAddress} of the remote JALoP Network Store that
	 * rejected the connection.
	 * 
	 * @return the {@link InetAddress} address of the remote JALoP Network
	 *         Store.
	 */
	InetAddress getAddress();

	/**
	 * Retrieve the set of errors that were indicated in the 'connect-ack'
	 * message.
	 * 
	 * @return A set of errors.
	 */
	Set<String> getErrors();

	/**
	 * Retrieve all the MIME headers that were sent as part of the connect-nack
	 * message.
	 * 
	 * @return The MIME headers.
	 */
	MimeHeaders getHeaders();
}
