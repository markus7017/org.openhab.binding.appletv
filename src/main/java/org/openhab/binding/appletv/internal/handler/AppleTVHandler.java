/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.appletv.internal.handler;

import static org.openhab.binding.appletv.internal.AppleTVBindingConstants.*;

import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.openhab.binding.appletv.internal.AppleTVConfiguration;
import org.openhab.binding.appletv.internal.AppleTVLogger;
import org.openhab.binding.appletv.internal.jpy.LibPyATV;

/**
 * The {@link AppleTVHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author markus7017 - Initial contribution
 */
public class AppleTVHandler extends BaseThingHandler {

    private final AppleTVLogger logger = new AppleTVLogger(AppleTVHandler.class, "Handler");

    private AppleTVConfiguration config;
    private LibPyATV pyATV = null;

    public AppleTVHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void initialize() {

        // set the thing status to UNKNOWN temporarily and let the background task decide for the real status.
        // the framework is then able to reuse the resources from the thing handler initialization.
        // we set this upfront to reliably check status updates in unit tests.
        updateStatus(ThingStatus.UNKNOWN);

        // Example for background initialization:
        scheduler.execute(() -> {
            logger.info("Initializing AppleTV");
            config = getConfigAs(AppleTVConfiguration.class);

            try {
                pyATV = new LibPyATV(config);

                logger.debug("PyATV installation path: {}", config.libPath);
                if (!pyATV.getLibPath().equals(config.libPath)) {
                    logger.info("PyATV Library installed in {}", config.libPath);
                    Configuration configuration = this.getConfig();
                    configuration.remove("libPath");
                    configuration.put("libPath", config.libPath);
                    this.updateConfiguration(configuration);
                    logger.debug("Configuration updated.");
                }
                updateStatus(ThingStatus.ONLINE);
            } catch (Exception e) {
                logger.error("Call to PyATV failed: {} ({})", e.getMessage(), e.getClass());
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                        "Uneable to initialize thing: " + e.getMessage());
            }
        });
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (getThing().getStatus() != ThingStatus.ONLINE) {
            // control.login();
        }

        if (command instanceof RefreshType) {
            // TODO: handle data refresh
        } else {
            // command
            switch (channelUID.getIdWithoutGroup()) {
                case CHANNEL_REMOTE_KEY:
                case CHANNEL_REMOTE_SEQ:
                    if (command instanceof StringType) {
                        pyATV.sendKeys(command.toString());
                    }
                    break;
            }
        }
    }
}
