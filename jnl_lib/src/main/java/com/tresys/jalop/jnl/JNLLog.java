/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tresys.jalop.jnl;

/**
 * <p>
 * The JJNLog interface provides a mechanism to allow external logging implementations
 * to be used within JJNL.
 * </li>
 * </ul>
 */
public interface JNLLog {

    boolean isWarnEnabled();

    boolean isTraceEnabled();

    boolean isInfoEnabled();

    boolean isErrorEnabled();

    boolean isDebugEnabled();

    void warn(String msg, Throwable t);

    void warn(String msg);

    void trace(String msg, Throwable t);

    void trace(String msg);

    void info(String msg, Throwable t);

    void info(String msg);

    void error(String msg, Throwable t);

    void error(String msg);

    void debug(String msg, Throwable t);

    void debug(String msg);
}
