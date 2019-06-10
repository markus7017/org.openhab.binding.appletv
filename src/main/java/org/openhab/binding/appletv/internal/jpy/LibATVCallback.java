/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.appletv.internal.jpy;

import org.eclipse.jdt.annotation.NonNull;

/**
 * The {@link LibATVCallback} defines the callback interface every class needs to implement receiving those callbacks.
 *
 * @author Markus Michels - Initial contribution
 */
public interface LibATVCallback {
    /**
     * This function will be called from the PyATV module to display an info message
     *
     * @param message
     */
    public void info(@NonNull String message);

    /**
     * This function will be called from the PyATV module to display a debug message
     *
     * @param message
     */
    public void debug(@NonNull String message);

    /**
     * This function will be called from the PyATV module when device discovery is completed
     *
     * @param json Discovered devices in JSON format
     */
    public void devicesDiscovered(@NonNull String json);

    /**
     * This function will be called from the PyATV to pass the unique device id
     *
     * @param id unique device id
     */
    public void generatedDeviceId(@NonNull String id);

    /**
     * PyATV posts an status update
     *
     * @param prop  key
     * @param input value
     */
    public void statusEvent(@NonNull String prop, @NonNull String input);

    /**
     * PyATV callback to report pairing result
     *
     * @param result  true: pairing was successful, false: pairing failed
     * @param message an additional information
     */
    public void pairingResult(boolean result, @NonNull String message);
}
