/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.appletv.internal;

import static org.openhab.binding.appletv.internal.AppleTVBindingConstants.UPDATE_STATUS_INTERVAL;

import org.eclipse.jdt.annotation.NonNull;

/**
 * Contains the binding configuration and default values. The field names represent the configuration names,
 * do not rename them if you don't intend to break the configuration interface.
 *
 * @author Markus Michels - initial contribution
 */
public class AppleTVBindingConfiguration {
    public String remoteName;
    public Integer updateInterval = UPDATE_STATUS_INTERVAL;
    public String libPath = "";

    public void update(@NonNull AppleTVBindingConfiguration newConfiguration) {
        this.remoteName = newConfiguration.remoteName;
        this.libPath = newConfiguration.libPath;
        this.updateInterval = newConfiguration.updateInterval;
    }
}
