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

import java.io.InputStream;

/**
 * {@link Publisher}s are responsible for creating an object that implements
 * this interface in order to send JAL records to a remote JALoP Network Store.
 * 
 * @see Publisher#getNextRecord(PublisherSession, String)
 * @see Publisher#onJournalResume(PublisherSession, String, long, MimeHeaders)
 */
public interface SourceRecord extends RecordInfo {
	/**
	 * The library will call this method to obtain an {@link InputStream} for
	 * the system meta-data. The library will not execute
	 * {@link InputStream#close()}, and it is recommended that the
	 * {@link Publisher} do this when the library calls
	 * {@link Publisher#onRecordComplete(PublisherSession, String, SourceRecord)}
	 * .
	 * 
	 * @return an {@link InputStream} for the system meta-data.
	 */
	InputStream getSysMetadata();

	/**
	 * The library will call this method to obtain an {@link InputStream} for
	 * the application meta-data. The library will not execute
	 * {@link InputStream#close()}, and it is recommended that the
	 * {@link Publisher} do this when the library calls
	 * {@link Publisher#onRecordComplete(PublisherSession, String, SourceRecord)}
	 * .
	 * 
	 * @return an {@link InputStream} for the application meta-data.
	 */
	InputStream getAppMetadata();

	/**
	 * The library will call this method to obtain an {@link InputStream} for
	 * the record payload. The library will not execute
	 * {@link InputStream#close()}, and it is recommended that the
	 * {@link Publisher} do this when the library calls
	 * {@link Publisher#onRecordComplete(PublisherSession, String, SourceRecord)}
	 * .
	 * 
	 * @return an {@link InputStream} for the payload.
	 */
	InputStream getPayload();
}
