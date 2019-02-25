/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
            logger.debug("Initialize PyATV (current installation path: '{}')", thingConfig.libPath);
            this.config = thingConfig;

            // if ((config.libPath == null) || config.libPath.isEmpty()) {
            libPath = Files.createTempDirectory("ohlib-");
            config.libPath = libPath.toString();
            logger.info("Modules will be installed in '{}'", config.libPath);
            // } else {
            // libPath = Paths.get(config.libPath);
            // }

            String os = System.getProperty("os.name").toLowerCase();
            String platform = System.getProperty("os.arch").toLowerCase();
            logger.debug("Platform info: '{}', architecture: '{}'", os, platform);

            List<String> cleanedExtraPaths = new ArrayList<>();
            cleanedExtraPaths.add(libPath.toString());

            String jpyPath = libPath.toString();
            String jpyLib = "";
            System.setProperty("jpy.debug", "true");
            if (os.contains("linux") || os.contains("mac")) {
                System.setProperty("jpy.pythonExecutable", "/usr/bin/python3");
                System.setProperty("jpy.pythonPrefix", "/usr");
            } else {
                throw new Exception(
                        "OS '" + os + "' not supported yet, please contact the author and provde platform information");
            }

            if (os.contains("mac")) {
                System.setProperty("jpy.pythonLib",
                        "/usr/local/Cellar/python/3.6.5/Frameworks/Python.framework/Versions/3.6/lib/libpython3.6.dylib");
                jpyLib = "lib/jpy/lib.macosx-x86_64-3.6";
                System.setProperty("jpy.jpyLib", jpyPath + "/jpy.so");
                System.setProperty("jpy.jdlLib", jpyPath + "/jdl.so");
            } else if (os.contains("linux") && platform.contains("arm")) { // Raspberry
                // System.setProperty("jpy.jpyLib", "jpy.cpython-35m-arm-linux-gnueabihf.so");
                // System.setProperty("jpy.jdlLib", "jdl.cpython-35m-arm-linux-gnueabihf.so");
                jpyLib = "lib/jpy/lib.linux-armv7l-3.5";
                System.setProperty("jpy.pythonLib", "/usr/lib/arm-linux-gnueabihf/libpython3.5m.so");
                // jpyLib = "lib/jpy/lib.linux-armv7l-3.6";
                // System.setProperty("jpy.pythonLib", "/usr/local/lib/libpython3.6m.so");
                System.setProperty("jpy.jpyLib", jpyPath + "/jpy.so");
                System.setProperty("jpy.jdlLib", jpyPath + "/jdl.so");
            } else if (os.contains("linux") && (platform.contains("x86_64") || platform.contains("amd64"))) { // Synology
                jpyLib = "lib/jpy/lib.synology-x86-64-3.5";
                System.setProperty("jpy.jpyLib", jpyPath + "/jpy.so");
                System.setProperty("jpy.jdlLib", jpyPath + "/jdl.so");
                System.setProperty("jpy.pythonLib", "/usr/lib/x86_64-linux-gnu/libpython3.5m.so");
            } else {
                throw new Exception(
                        "Architecture not supported yet, please contact the author and provde platform information");
            }
            // cleanedExtraPaths.add("/usr/local/lib/python3.6/dist-packages");
            // cleanedExtraPaths.add("/usr/local/lib/python3.6/site-packages");
            cleanedExtraPaths.add("/usr/local/lib/python3.5/dist-packages");
            cleanedExtraPaths.add("/usr/local/lib/python3.5/site-packages");
            cleanedExtraPaths.add("/usr/lib/python3/dist-packages");
            cleanedExtraPaths.add("/usr/lib/python3/site-packages");

            List<String> extraPaths = new ArrayList<>();
            extraPaths.add("classpath:lib/pyatv.zip"); // PyATV library
            extraPaths.add("classpath:lib/jpy-0.10.0-SNAPSHOT.jar");
            if (!jpyLib.isEmpty()) {
                logger.debug("jpyLib={}", jpyLib);
                extraPaths.add("classpath:" + jpyLib + "/jpy.so"); // python_runtime_module
                extraPaths.add("classpath:" + jpyLib + "/jdl.so"); // python_runtime_module
                extraPaths.add("classpath:" + jpyLib + "/jpyutil.py"); // python util
            }

            logger.debug("jpy.pythonExecutable: {}", System.getProperty("jpy.pythonExecutable"));
            logger.debug("jpy.pythonLib: {}", System.getProperty("jpy.pythonLib"));
            logger.debug("jpy.jpyLib: {}", System.getProperty("jpy.jpyLib"));
            logger.debug("jpy.jdlLib: {}", System.getProperty("jpy.jdlLib"));
            logger.debug("jpy.pythonPrefix: {}", System.getProperty("jpy.pythonPrefix"));

            // Runtime.getRuntime()
            // .addShutdownHook(new Thread(() -> FileSystemUtils.deleteRecursively(tempDirectory.toFile())));
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

    public boolean sendKeys(String commands) {
        try {
            logger.info("Sending command {} to ip {}, lid {}", commands, config.ipAddress, config.loginId);

            String[] args = new String[6];
            // PyObject res = plugIn.exec("--address 192.168.x.y --login_id 0xXXXXXXXXXXXXXXXX top_menu");
            args[0] = "--debug";
            args[1] = "--address";
            args[2] = config.ipAddress;
            args[3] = "--login_id";
            args[4] = config.loginId;
            args[5] = commands;
            return (pyATV.exec(args).getIntValue() == 0);
        } catch (Exception e) {
            logger.error("Exception on PyATV call: {} ({})", e.getMessage(), e.getClass());
            return false;
        }
    }

    public boolean scan() {
        try {
            logger.info("Scan for AppleTV devices");

            String[] args = new String[1];
            args[0] = "scan";
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
