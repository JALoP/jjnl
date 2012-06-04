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
 * An enum to specify the type of a JAL record.
 */
public enum RecordType {
	/** Indicates the record type is unknown or has not been set */
	Unset,
	/**
	 * Indicates Journal records. Journal records are (potentially) large text
	 * or binary records.
	 */
	Journal,
	/**
	 * Indicates Audit records. Audit records are XML documents that conform to
	 * the Mitre CEE schema.
	 */
	Audit,
	/**
	 * Indicates Log records. Log records are typically small text entries, such
	 * as the text that is Generated through logging frameworks like log4j.
	 */
	Log
}
