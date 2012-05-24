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
 * The {@link DigestStatus} indicates whether or not two JALoP Network Stores
 * calculated the same message digest for a particular JAL record.
 * 
 */
public enum DigestStatus {
	/**
	 * Indicates that both JALoP Network Stores calculated the same message
	 * digest.
	 */
	Confirmed,
	/**
	 * Indicates that the JALoP Network Stores calculated different message
	 * digests.
	 */
	Invalid,
	/**
	 * Indicates that the subscriber sent a message digest for a JAL record that
	 * the publisher does not recognize (i.e. it doesn't have a record for the
	 * particular serial ID).
	 */
	Unknown,
}
