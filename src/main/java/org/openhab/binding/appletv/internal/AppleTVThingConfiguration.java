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

/**
 * The {@link AppleTVThingConfiguration} class contains fields mapping thing configuration parameters.
 *
 * @author markus7017 - Initial contribution
 */
public class AppleTVThingConfiguration {

    public String ipAddress;
    public String loginId;

    public boolean doPairing;
    public String pairingPIN;
    public String authenticationPIN;

    public String keyMovie;
    public String keyTVShow;
    public String keyMusic;
}
