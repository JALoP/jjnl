/*
 * Source code in 3rd-party is licensed and owned by their respective
 * copyright holders.
 *
 * All other source code is copyright Tresys Technology and licensed as below.
 *
 * Copyright (c) 2012,2014 Tresys Technology LLC, Columbia, Maryland, USA
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
 * A {@link RecordInfo} provides information about a specific JAL record.
 */
public interface RecordInfo {
	/**
	 * Get the nonce of the record. Nonces are always determined by the
	 * publisher.
	 * 
	 * @return the nonce of the record.
	 */
	String getNonce();

	/**
	 * Get the offset of the journal record. 
	 * 
	 * @return the offset of the journal record.
	 */
	long getOffset();

	/**
	 * Get the type of JAL record.
	 * 
	 * @return The type of record.
	 */
	RecordType getRecordType();

	/**
	 * Get the size of the system meta-data.
	 * 
	 * @return the size (in bytes) of the system meta-data.
	 */
	long getSysMetaLength();

	/**
	 * Get the size of the application meta-data.
	 * 
	 * @return the size (in bytes) of the application meta-data.
	 */
	long getAppMetaLength();

	/**
	 * Get the size of the payload.
	 * 
	 * @return the size (in bytes) of the payload.
	 */
	long getPayloadLength();
}
