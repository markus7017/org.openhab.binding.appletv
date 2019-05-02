/**
 * Copyright (c) 2010-2019 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.appletv.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link AppleTVLogger} implements logging
 *
 * @author markus7017 - Initial contribution
 */
public class AppleTVLogger {

    /**
     * The {@link AppleTVLogger} class implements a wrapper for slf4j.Logger and
     * inserts a prefix to every messages, which makes it easier to read between all
     * the other messages.
     *
     * @author Markus Michels - Initial contribution (markus7017)
     */
    private Logger logger;
    private static final String binding = "Apple-TV";
    private final String prefix;

    public AppleTVLogger(Class<?> clazz, String module) {
        logger = LoggerFactory.getLogger(clazz);
        prefix = "Apple-TV." + module + ": ";
    }

    public void error(String message, Object... a) {
        logger.error(prefix + message, a);
    }

    public void info(String message, Object... a) {
        logger.info(prefix + message, a);
    }

    public void debug(String message, Object... a) {
        logger.debug(prefix + message, a);
    }

    public void trace(String message, Object... a) {
        logger.trace(prefix + message, a);
    }
}
