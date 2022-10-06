/*
 * Source code in 3rd-party is licensed and owned by their respective
 * copyright holders.
 *
 * All other source code is copyright Tresys Technology and licensed as below.
 *
 * Copyright (c) 2014 Tresys Technology LLC, Columbia, Maryland, USA
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
 * Enum to distinguish between the different publish modes of JALoP.
 *
 */
public enum Mode {
	/**
	 * Specifies the mode is not set, such as before a session has finished
	 * negotiation.
	 */
	Unset,
	/**
	 * Indicates the mode of JALoP Publisher. Publisher should send
	 * all data, including historical data.
	 */
	Archive,
	/**
	 * Indicates the mode of JALoP Publisher. Publisher should send
	 * only data as of the time of the Subscribe request.
	 */
	Live,
}
