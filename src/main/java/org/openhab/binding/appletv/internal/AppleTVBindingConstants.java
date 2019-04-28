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

import java.util.Collections;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.thing.ThingTypeUID;

/**
 * The {@link AppleTVBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Markus Michels - Initial contribution
 */
@NonNullByDefault
public class AppleTVBindingConstants {

    private static final String BINDING_ID = "appletv";

    // List of all Thing Type UIDs
    public static final ThingTypeUID THING_TYPE_APPLETV = new ThingTypeUID(BINDING_ID, "device");
    public static final Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Collections.singleton(THING_TYPE_APPLETV);

    // Thing properties
    public static final String PROPERTY_ID = "deviceId";
    public static final String PROPERTY_IP = "ipAddress";
    public static final String PROPERTY_LOGIN_ID = "loginId";

    // List of all Channel ids
    public static final String CHGROUP_CONTROL = "control";
    public static final String CHGROUP_STATUS = "playStatus";
    public static final String CHGROUP_MEDIA = "mediaInformation";

    public static final String CHANNEL_REMOTE_KEY = "remoteKey";
    public static final String CHANNEL_REMOTE_SEQ = "keysSequence";
    public static final String CHANNEL_PLAY_MODE = "playMode";
    public static final String CHANNEL_MEDIA_TYPE = "mediaType";
    public static final String CHANNEL_REPEAT_STATE = "repeatState";
    public static final String CHANNEL_ALBUM = "album";

    public static final String JPY_DEVICE_FILE = "/tmp/ohpyatv-devices.json";
}
