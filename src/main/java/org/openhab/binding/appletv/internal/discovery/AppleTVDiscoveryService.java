/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.appletv.internal.discovery;

import static org.openhab.binding.appletv.internal.AppleTVBindingConstants.SUPPORTED_THING_TYPES_UIDS;

import java.util.Set;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.smarthome.config.discovery.AbstractDiscoveryService;
import org.eclipse.smarthome.config.discovery.DiscoveryService;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.openhab.binding.appletv.internal.AppleTVHandler;
import org.openhab.binding.appletv.internal.AppleTVHandlerFactory;
import org.openhab.binding.appletv.internal.AppleTVLogger;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

/**
 * The {@link AppleTVDiscoveryService} is used to discover AppleTV devices.
 *
 * @author Markus Michels - Initial contribution
 */
@Component(service = DiscoveryService.class, immediate = true, configurationPid = "binding.appletv")
public class AppleTVDiscoveryService extends AbstractDiscoveryService {
    private final AppleTVLogger logger = new AppleTVLogger(AppleTVHandler.class, "Discovery");
    private AppleTVHandlerFactory handlerFactory = null;

    public AppleTVDiscoveryService() {
        super(SUPPORTED_THING_TYPES_UIDS, 15, true);
        String uids = SUPPORTED_THING_TYPES_UIDS.toString();
        logger.debug("Apple-TV: thing types: {} registered.", uids);
    }

    /**
     * Called on component activation.
     */
    public void activate() {
        super.activate(null);
    }

    @Override
    public void deactivate() {
        super.deactivate();
    }

    @Override
    protected void startScan() {
        if (handlerFactory == null) {
            logger.debug("handlerFactory not initialized, stop");
            return;
        }

        // Example for background initialization:
        logger.info("Starting Appl-TV discovery");
        try {
            handlerFactory.sendCommands("scan", "", "");
        } catch (Exception e) {
            logger.debug("Discovery failed");
        }
        logger.info("Appl-TV discovery completed");
    }

    @Override
    public synchronized void stopScan() {
        if (handlerFactory == null) {
            return;
        }

        logger.info("Stopping Apple-TV discovery scan");
        super.stopScan();
    }

    @Override
    public Set<@NonNull ThingTypeUID> getSupportedThingTypes() {
        return SUPPORTED_THING_TYPES_UIDS;
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    public void setAppleTVHandlerFactory(AppleTVHandlerFactory handlerFactory) {
        if (handlerFactory != null) {
            this.handlerFactory = handlerFactory;
            logger.debug("HandlerFactory bound to AppleTVDiscoveryService");
        }
    }

    public void unsetAppleTVHandlerFactory(AppleTVHandlerFactory handlerFactory) {
        this.handlerFactory = null;
    }

}
