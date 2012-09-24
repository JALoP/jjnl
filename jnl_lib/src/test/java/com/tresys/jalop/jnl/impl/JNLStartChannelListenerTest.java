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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import mockit.NonStrictExpectations;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.beepcore.beep.core.BEEPException;
import org.beepcore.beep.core.Channel;
import org.beepcore.beep.core.Session;
import org.beepcore.beep.core.SessionTuningProperties;
import org.beepcore.beep.core.StartChannelException;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;


public class JNLStartChannelListenerTest {

	@Before
	public void setUp() {
		// Disable logging so the build doesn't get spammed.
		Logger.getRootLogger().setLevel(Level.OFF);
	}

	@BeforeClass
	public static void initializeClasses() {
		//Need to initialize SessionTuningProperties before trying to mock it
		try {
			new SessionTuningProperties();
		} catch(final Exception e) {
			//do nothing
		}
	}

	@Test
	public void testAdvertiseProfileTrueWhenEncrypted(final Session sess, final SessionTuningProperties tuning) throws BEEPException {
		final JNLStartChannelListener jscl = new JNLStartChannelListener();

		new NonStrictExpectations() {
			{
				sess.getTuningProperties(); result = tuning;
				tuning.getEncrypted(); result = true;
			}
		};

		assertTrue(jscl.advertiseProfile(sess));
	}

	@Test
	public void testAdvertiseProfileFalseWhenNotEncrypted(final Session sess, final SessionTuningProperties tuning) throws BEEPException {
		final JNLStartChannelListener jscl = new JNLStartChannelListener();

		new NonStrictExpectations() {
			{
				sess.getTuningProperties(); result = tuning;
				tuning.getEncrypted(); result = false;
			}
		};

		assertFalse(jscl.advertiseProfile(sess));
	}

	@Test
	public void testCloseChannelWorks(final Session sess, final Channel channel) throws BEEPException {
		final JNLStartChannelListener jscl = new JNLStartChannelListener();
		jscl.closeChannel(channel);
	}

	@Test(expected = StartChannelException.class)
	public void testStartChannelThrowsException(final Session sess, final Channel channel) throws StartChannelException {
		final JNLStartChannelListener jscl = new JNLStartChannelListener();
		jscl.startChannel(channel, "", "");
	}
}
