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

import static org.openhab.binding.appletv.internal.AppleTVBindingConstants.SUPPORTED_THING_TYPES_UIDS;

import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandlerFactory;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandlerFactory;
import org.openhab.binding.appletv.internal.jpy.LibPyATV;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

/**
 * The {@link AppleTVHandlerFactory} is responsible for creating things and thing
 * handlers.
 *
 * @author markus7017 - Initial contribution
 */
@NonNullByDefault
@Component(service = { ThingHandlerFactory.class, AppleTVHandlerFactory.class }, configurationPid = "binding.appletv")
// @Component(configurationPid = "binding.appletv", service = ThingHandlerFactory.class)
public class AppleTVHandlerFactory extends BaseThingHandlerFactory {
    private final AppleTVLogger logger = new AppleTVLogger(AppleTVHandlerFactory.class, "Factory");
    private @Nullable LibPyATV pyATV = null;

    /**
     * Activate the bundle: save properties
     *
     * @param componentContext
     * @param configProperties set of properties from cfg (use same names as in
     *                             thing config)
     */

    @Activate

    protected void activate(ComponentContext componentContext, Map<String, Object> configProperties) {
        super.activate(componentContext);
        logger.debug("Activate HandlerFactory");
        pyATV = new LibPyATV("");
        logger.debug("PyATV installation path: {}", pyATV.getLibPath());
    }

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
    }

    @Override
    protected @Nullable ThingHandler createHandler(Thing thing) {
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        if (AppleTVBindingConstants.THING_TYPE_APPLETV.equals(thingTypeUID)) {
            return new AppleTVHandler(thing, this);
        }

        return null;
    }

    public boolean sendCommands(String commands, String ipAddress, String loginId) {
        try {
            return pyATV.sendCommands(commands, ipAddress, loginId);
        } catch (RuntimeException e) {
            return false;
        }
    }

    String getLibPath() {
        return pyATV.getLibPath();
    }
}
