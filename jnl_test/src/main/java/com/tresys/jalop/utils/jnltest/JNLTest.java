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
package com.tresys.jalop.utils.jnltest;

import java.io.IOException;

import org.json.simple.parser.ParseException;

import com.tresys.jalop.utils.jnltest.Config.Config;
import com.tresys.jalop.utils.jnltest.Config.ConfigurationException;

/**
 * Main class for JNLTest
 */
public class JNLTest {
	/**
	 * Configuration for this instance of JNLTest.
	 */
	private Config config;

	/**
	 * Create a JNLTest object based on the specified configuration.
	 * 
	 * @param config
	 *            A {@link Config}
	 */
	public JNLTest(Config config) {
		this.config = config;
	}

	/**
	 * Main entry point for the JNLTest program. This takes a single argument,
	 * the full path to a configuration file to use.
	 * 
	 * @param args
	 *            The command line arguments
	 */
	public static void main(String[] args) {
		if (args.length != 1) {
			System.exit(1);
			System.err.println("Must specify exactly one argument that is "
					+ "the configuration file use");
		}
		Config config;
		try {
			config = Config.parse(args[0]);
		} catch (IOException e) {
			System.err.println("Caught IO exception: " + e.getMessage());
			System.exit(1);
			throw new RuntimeException("Failed to call exit()");
		} catch (ParseException e) {
			System.err.print(e.toString());
			System.exit(1);
			throw new RuntimeException("Failed to call exit()");
		} catch (ConfigurationException e) {
			System.err.println("Exception processing the config file: "
					+ e.getMessage());
			System.exit(1);
			throw new RuntimeException("Failed to call exit()");
		}
		JNLTest jt = new JNLTest(config);
	}

}
