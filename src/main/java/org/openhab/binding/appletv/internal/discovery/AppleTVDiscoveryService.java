/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.appletv.internal.discovery;

import static org.eclipse.smarthome.core.thing.Thing.PROPERTY_VENDOR;
import static org.openhab.binding.appletv.internal.AppleTVBindingConstants.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.config.discovery.AbstractDiscoveryService;
import org.eclipse.smarthome.config.discovery.DiscoveryResult;
import org.eclipse.smarthome.config.discovery.DiscoveryResultBuilder;
import org.eclipse.smarthome.config.discovery.DiscoveryService;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.openhab.binding.appletv.internal.AppleTVBindingConfiguration;
import org.openhab.binding.appletv.internal.AppleTVHandler;
import org.openhab.binding.appletv.internal.AppleTVHandlerFactory;
import org.openhab.binding.appletv.internal.AppleTVLogger;
import org.openhab.binding.appletv.internal.jpy.LibATVCallback;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.google.gson.Gson;

/**
 * The {@link AppleTVDiscoveryService} is used to discover AppleTV devices.
 *
 * @author Markus Michels - Initial contribution
 */
@Component(service = DiscoveryService.class, immediate = true, configurationPid = "discovery.appletv")
public class AppleTVDiscoveryService extends AbstractDiscoveryService implements LibATVCallback {
    private final AppleTVLogger logger = new AppleTVLogger(AppleTVHandler.class, "Discovery");
    private AppleTVHandlerFactory handlerFactory = null;
    private ScheduledFuture<?> discoveryJob;
    private final AppleTVBindingConfiguration bindingConfig = new AppleTVBindingConfiguration();

    class ATVDevice {
        String deviceId = "";
        String name = "";
        String ipAddress = "";
        String loginId = "";
    }

    class ATVDeviceList {
        ArrayList<ATVDevice> devices = new ArrayList<>();
    }

    public AppleTVDiscoveryService() {
        super(SUPPORTED_THING_TYPES_UIDS, 15, true);
        String uids = SUPPORTED_THING_TYPES_UIDS.toString();
        logger.debug("Apple-TV: thing types: {} registered.", uids);
    }

    /**
     * Called on component activation.
     */
    @Override
    protected void activate(@Nullable Map<@NonNull String, @Nullable Object> configProperties) {
        logger.debug("Config Parameters: {}", configProperties);
        super.activate(configProperties); // starts background discovery
        bindingConfig.update(new Configuration(configProperties).as(AppleTVBindingConfiguration.class));
    }

    @Override
    @Modified
    protected void modified(Map<String, Object> configProperties) {
        super.modified(configProperties);
        // We update instead of replace the configuration object, so that if the user updates the
        // configuration, the values are automatically available in all handlers. Because they all
        // share the same instance.
        bindingConfig.update(new Configuration(configProperties).as(AppleTVBindingConfiguration.class));
    }

    @Override
    public void deactivate() {
        super.deactivate();
    }

    @Override
    public Set<@NonNull ThingTypeUID> getSupportedThingTypes() {
        return SUPPORTED_THING_TYPES_UIDS;
    }

    /**
     * Start device discovery
     */
    @Override
    protected void startScan() {
        if (handlerFactory == null) {
            logger.info("Factory not initialized, skip scan");
            return;
        }

        // Example for background initialization:
        logger.info("Starting Apple-TV discovery");
        try {
            String jsonDevices = handlerFactory.scanDevices();

            Gson gson = new Gson();
            ATVDeviceList devList = gson.fromJson(jsonDevices, ATVDeviceList.class);
            for (ATVDevice dev : devList.devices) {
                handlerFactory.sendCommands(COMMAND_DEVICE_ID, handlerFactory, dev.ipAddress, dev.loginId);
                dev.deviceId = handlerFactory.getLastDeviceId(); // set by callback
                logger.info("Device {} discovered: ipAddress={}, deviceId={}, loginId={}", dev.name, dev.ipAddress,
                        dev.deviceId, dev.loginId);

                Map<String, Object> properties = new HashMap<>();
                properties.put(PROPERTY_ID, dev.deviceId);

                properties.put(PROPERTY_VENDOR, "Apple");
                properties.put(PROPERTY_IP, dev.ipAddress);
                properties.put(PROPERTY_LOGIN_ID, dev.loginId);
                thingDiscovered(createDiscoveryResult(dev, properties));
            }
        } catch (Exception e) {
            logger.debug("Discovery failed");
        }
        logger.info("Apple-TV discovery completed");
    }

    protected void autoDiscover() {
        if (!bindingConfig.autoDiscovery) {
            logger.trace("Auto discovery is disabled, skip scan");
            return;
        }
        startScan();
    }

    private DiscoveryResult createDiscoveryResult(ATVDevice device, Map<String, Object> properties) {
        ThingUID thingUID = createThingUID(device);
        return DiscoveryResultBuilder.create(thingUID).withLabel(device.name).withProperties(properties)
                .withRepresentationProperty(PROPERTY_ID).build();
    }

    private ThingUID createThingUID(ATVDevice device) {
        return new ThingUID(THING_TYPE_APPLETV, device.deviceId);
    }

    @Override
    public synchronized void stopScan() {
        if (discoveryJob != null) {
            discoveryJob.cancel(true);
        }

        if (handlerFactory == null) {
            return;
        }

        logger.info("Stopping Apple-TV discovery scan");
        super.stopScan();
    }

    @Override
    protected void startBackgroundDiscovery() {
        // logger.debug("startBackgroundDiscovery");
    }

    @Override
    protected void stopBackgroundDiscovery() {
        logger.info("Stopping background discovery");
        stopScan();
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    public void setAppleTVHandlerFactory(AppleTVHandlerFactory handlerFactory) {
        if (handlerFactory != null) {
            this.handlerFactory = handlerFactory;
            logger.debug("HandlerFactory bound to AppleTVDiscoveryService");
            handlerFactory.setBindingConfig(bindingConfig);

            logger.info("Starting background discovery");
            if (discoveryJob == null || discoveryJob.isCancelled()) {
                discoveryJob = scheduler.scheduleWithFixedDelay(this::autoDiscover, 20, 15 * 60, TimeUnit.SECONDS);
            }
        }
    }

    public void unsetAppleTVHandlerFactory(AppleTVHandlerFactory handlerFactory) {
        this.handlerFactory = null;
    }

    @Override
    public void info(String message) {
        logger.info("{}", message);
    }

    @Override
    public void debug(String message) {
        logger.debug("{}", message);
    }

    @Override
    public void statusEvent(String prop, String input) {
        logger.debug("Unexpected call to AppleTVDiscoveryService.statusEvent()");

    }

    @Override
    public void devicesDiscovered(@NonNull String json) {
        logger.debug("Unexpected call to AppleTVDiscoveryService.devicesDiscovered()");

    }

    @Override
    public void generatedDeviceId(@NonNull String id) {
        logger.debug("Unexpected call to AppleTVDiscoveryService.generatedDeviceId()");
    }

    @Override
    public void pairingResult(boolean result, @NonNull String message) {
        logger.debug("Unexpected call to AppleTVDiscoveryService.pairingResult()");
    }

}
