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
import org.beepcore.beep.core.MimeHeaders;
import org.beepcore.beep.core.OutputDataStream;
import org.beepcore.beep.util.BufferSegment;

/**
 * Extension of the {@link OutputDataStream} class to limit the number of
 * {@link BufferSegment}s that can be added. Once the maximum number of buffers
 * has been reached this will wait until one had been removed to allow more additions.
 */
public class JNLOutputDataStream extends OutputDataStream {

	static Logger log = Logger.getLogger(JNLOutputDataStream.class);

	private final int maxBuffers;

	/**
	 * Create a JNLOutputDataStream with the given headers.
	 *
	 * @param headers
	 * 				The {@link MimeHeaders} for this stream.
	 */
	public JNLOutputDataStream (final MimeHeaders headers, final int maxBuffers) {
		super(headers);
		this.maxBuffers = maxBuffers;
	}

	@Override
	protected BufferSegment getNextSegment(final int maxLength) {
		final BufferSegment toReturn = super.getNextSegment(maxLength);
		synchronized(this) {
			this.notifyAll();
		}
		return toReturn;
	}

	@Override
	public void add(final BufferSegment segment) {
		synchronized(this) {
			while (this.getNumSegments() >= this.maxBuffers) {
				try {
					this.wait();
				} catch (final InterruptedException e) {
					if (log.isEnabledFor(Level.ERROR)) {
						log.error("Error: " + e.getMessage());
					}
				}
			}
		}
        super.add(segment);
    }
}
