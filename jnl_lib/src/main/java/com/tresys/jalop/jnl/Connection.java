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

/**
 * This represents the physical connection between 2 JALoP Network Stores.
 * Multiple JALoP Sessions may exist within a single {@link Connection}.
 */
public interface Connection {
	/**
	 * Attempt to gracefully disconnect from the remote JALoP Network Store.
	 */
	void shutdown();

	/**
	 * Immediately (ungracefully) sever the connection to the remote JALoP
	 * Network Store.
	 */
	void close();
}
