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
public class AppleTVHandlerFactory extends BaseThingHandlerFactory {
    private final AppleTVLogger logger = new AppleTVLogger(AppleTVHandlerFactory.class, "Factory");
    private AppleTVBindingConfiguration bindingConfig = new AppleTVBindingConfiguration();
    private @Nullable LibPyATV pyATV = null;
    private String jsonDevices = "";
    private String lastDeviceId = "";

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

    public void setBindingConfig(AppleTVBindingConfiguration bindingConfig) {
        this.bindingConfig.update(bindingConfig);
        logger.info("Binding configuration refreshed");
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

    @SuppressWarnings("null")
    public void initPyATV(AppleTVHandler thingHandler) throws AppleTVException {
        pyATV.init(thingHandler);
    }

    @SuppressWarnings("null")
    public boolean sendCommands(String commands, Object handler, String ipAddress, String loginId) {
        return pyATV.sendCommands(commands, handler, ipAddress, loginId);
    }

    @SuppressWarnings("null")
    public String scanDevices() {
        try {
            jsonDevices = "";
            pyATV.scanDevices(this);
            return jsonDevices;
        } catch (Exception e) {
            logger.info("Device scan failed!");
        }
        return "";
    }

    @SuppressWarnings("null")
    String getLibPath() {
        return pyATV.getLibPath();

    }

    public String getLastDeviceId() {
        return lastDeviceId;
    }

    /**
     * Callback for PyATV module delivery the device list in JSON format
     *
     * @param json
     */
    public void devicesDiscovered(String json) {
        logger.debug("Discovered devices: {}", json);
        jsonDevices = json;
    }

    public void generatedDeviceId(String id) {
        lastDeviceId = id;
    }

    public void info(String message) {
        logger.info("{}", message);
    }

    public void debug(String message) {
        logger.debug("{}", message);
    }
}
