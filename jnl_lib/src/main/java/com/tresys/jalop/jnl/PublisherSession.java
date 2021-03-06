/*
 * Source code in 3rd-party is licensed and owned by their respective
 * copyright holders.
 *
 * All other source code is copyright Tresys Technology and licensed as below.
 *
 * Copyright (c) 2012, 2014 Tresys Technology LLC, Columbia, Maryland, USA
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

/**
 * This represents a JALoP session acting as a publisher. At present, there are
 * no additional methods defined for a {@link Publisher}.
 */
public interface PublisherSession extends Session {
	/**
	 * The network store executes this method to give a record to the library
	 * to be sent across the network.
	 *
	 * @param rec
	 *            The record to send
	 */
	void sendRecord(final SourceRecord rec);

	/**
	 * Called by the publishing network store to indicate it is done sending records 
	 */
	void complete();
}
