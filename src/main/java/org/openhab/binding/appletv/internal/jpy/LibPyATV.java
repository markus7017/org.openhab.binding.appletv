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
package org.openhab.binding.appletv.internal.jpy;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.jpy.PyLib;
import org.jpy.PyModule;
import org.jpy.PyObject;
import org.openhab.binding.appletv.internal.AppleTVConfiguration;
import org.openhab.binding.appletv.internal.AppleTVHandlerFactory;
import org.openhab.binding.appletv.internal.AppleTVLogger;
import org.openhab.binding.appletv.internal.handler.AppleTVHandler;

/**
 * The {@link LibPyATV} wraps the PyATV library
 *
 * @author markus7017 - Initial contribution
 */
public class LibPyATV {
    public interface PyATVProxy {
        PyObject check();

        PyObject exec(String[] arg);

        PyObject start_update_listener();

        PyObject stop_update_listener();
    }

    private final AppleTVLogger logger = new AppleTVLogger(AppleTVHandlerFactory.class, "PyATV");
    private AppleTVConfiguration config;

    private Path libPath;
    private PyATVProxy pyATV;

    private boolean started = false;

    public LibPyATV(AppleTVConfiguration thingConfig) {

        try {
            this.config = thingConfig;

            // if ((config.libPath == null) || config.libPath.isEmpty()) {
            libPath = Files.createTempDirectory("ohlib-");
            config.libPath = libPath.toString();
            // } else {
            // libPath = Paths.get(config.libPath);
            // }

            String os = System.getProperty("os.name").toLowerCase();
            String platform = System.getProperty("os.arch").toLowerCase();
            logger.debug("Platform info: '{}', architecture: '{}'", os, platform);

            String jpyPath = libPath.toString();
            String jpyLib = "";
            if (os.contains("mac")) {
                jpyLib = "lib/jpy/lib.macosx-x86_64-3.6";
                System.setProperty("jpy.pythonLib",
                        "/usr/local/Cellar/python/3.6.5/Frameworks/Python.framework/Versions/3.6/lib/libpython3.6.dylib");
            } else if (os.contains("linux") && platform.contains("arm")) { // Raspberry
                jpyLib = "lib/jpy/lib.linux-armv7l-3.6";
                System.setProperty("jpy.pythonLib",
                        // "/usr/lib/python3.5/config-3.5m-arm-linux-gnueabihf/libpython3.5.so");
                        "/usr/local/lib/libpython3.6m.a");
            }

            List<String> extraPaths = new ArrayList<>();
            extraPaths.add("classpath:lib/pyatv.zip"); // PyATV library
            extraPaths.add("classpath:lib/jpy-0.10.0-SNAPSHOT.jar");

            System.setProperty("jpy.pythonExecutable", "/usr/bin/python");
            System.setProperty("jpy.pythonPrefix", "/usr");
            System.setProperty("jpy.jpyLib", jpyPath + "/jpy.so");
            System.setProperty("jpy.jdlLib", jpyPath + "/jdl.so");
            System.setProperty("jpy.debug", "true");

            logger.debug("jpy.jpyLib: {}", System.getProperty("jpy.jpyLib"));
            logger.debug("jpy.jdlLib: {}", System.getProperty("jpy.jdlLib"));
            logger.debug("jpy.pythonLib: {}", System.getProperty("jpy.pythonLib"));
            logger.debug("jpy.pythonPrefix: {}", System.getProperty("jpy.pythonPrefix"));
            logger.debug("jpy.pythonExecutable: {}", System.getProperty("jpy.pythonExecutable"));

            extraPaths.add("classpath:" + jpyLib + "/jpy.so"); // python_runtime_module
            extraPaths.add("classpath:" + jpyLib + "/jdl.so"); // python_runtime_module
            extraPaths.add("classpath:" + jpyLib + "/jpyutil.py"); // python util

            // Runtime.getRuntime()
            // .addShutdownHook(new Thread(() -> FileSystemUtils.deleteRecursively(tempDirectory.toFile())));
            List<String> cleanedExtraPaths = new ArrayList<>();
            extraPaths.forEach(lib -> {
                if (lib.startsWith("classpath:")) {
                    try {
                        String finalLib = lib.replace("classpath:", "");
                        String targetName = finalLib.contains("/") ? StringUtils.substringAfterLast(lib, "/")
                                : finalLib;
                        Path target = Paths.get(libPath.toString(), targetName);
                        try (InputStream stream = AppleTVHandler.class.getClassLoader().getResourceAsStream(finalLib)) {
                            Files.copy(stream, target, StandardCopyOption.REPLACE_EXISTING);
                        }
                        if (finalLib.endsWith(".zip")) {
                            ZipUtils.extract(target.toFile(), libPath.toFile());
                            String zipPath = libPath + "/" + targetName.replaceAll(".zip", "");
                            cleanedExtraPaths.add(zipPath);
                        }
                    } catch (Exception e) {
                        logger.error("Unable to install PyATV library: {} ({})", e.getMessage(), e.getClass());
                    }
                } else {
                    cleanedExtraPaths.add(lib);
                }
            });
            cleanedExtraPaths.add(libPath.toString());
            cleanedExtraPaths.add("/usr/local/lib/python3.6/site-packages");
            cleanedExtraPaths.add("/usr/local/lib/python3.6/dist-packages");

            if (!PyLib.isPythonRunning()) {
                logger.debug("Starting Python");
                PyLib.startPython(cleanedExtraPaths.toArray(new String[] {}));
                started = true;
            }

            // Proxify the call to a python class
            logger.info("load PyATV");
            PyModule pyModule = PyModule.importModule("pyatv_api");
            PyObject pyObject = pyModule.call("PyATV");
            pyATV = pyObject.createProxy(PyATVProxy.class);
            if (pyATV == null) {
                throw new Exception("Unable to initialize PyATV access");
            }
            pyATV.check();
            sendKeys("top_menu");
        } catch (Exception e) {
            logger.error("Unable to start Python (jpy): {} ({})", e.getMessage(), e.getClass());
        }
    }

    public String getLibPath() {
        return config.libPath;
    }

    public boolean sendKeys(String keys) {
        try {
            logger.debug("Sending remote key(s): {}", keys);

            String[] args = new String[6];
            // PyObject res = plugIn.exec("--address 192.168.6.123 --login_id 0xCF670625989EE1EC top_menu");
            args[0] = "--debug";
            args[1] = "--address";
            args[2] = config.ipAddress;
            args[3] = "--login_id";
            args[4] = config.loginId;
            args[5] = keys;
            return (pyATV.exec(args).getIntValue() == 0);
        } catch (Exception e) {
            logger.error("Exception on PyATV call: {} ({})", e.getMessage(), e.getClass());
            return false;
        }
    }

    void dispose() {
        if (started) {
            PyLib.Diag.setFlags(PyLib.Diag.F_OFF);
            PyLib.stopPython();
        }
    }
}
