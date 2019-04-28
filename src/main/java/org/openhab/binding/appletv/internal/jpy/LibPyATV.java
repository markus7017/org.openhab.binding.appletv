/**
 * Copyright (c) 2010-2019 by the respective copyright holders.
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
import java.util.StringTokenizer;

import org.apache.commons.lang.StringUtils;
import org.jpy.PyLib;
import org.jpy.PyModule;
import org.jpy.PyObject;
import org.openhab.binding.appletv.internal.AppleTVHandler;
import org.openhab.binding.appletv.internal.AppleTVHandlerFactory;
import org.openhab.binding.appletv.internal.AppleTVLogger;

/**
 * The {@link LibPyATV} wraps the PyATV library
 *
 * @author markus7017 - Initial contribution
 */
public class LibPyATV {
    public interface PyATVProxy {
        PyObject init(LibPyATV lib);

        PyObject exec(String[] arg);

        PyObject start_update_listener();

        PyObject stop_update_listener();
    }

    private final AppleTVLogger logger = new AppleTVLogger(AppleTVHandlerFactory.class, "PyATV");

    private Path libPath;
    private PyATVProxy pyATV;
    private String jsonDevices = "";

    private boolean started = false;

    public LibPyATV() {

    }

    public LibPyATV(String currentLibPath) {

        try {
            logger.debug("Initialize PyATV (current installation path: '{}')", currentLibPath);

            // if ((currentLibPath == null) || currentLibPath.isEmpty()) {
            libPath = Files.createTempDirectory("ohlib-");
            logger.info("Modules will be installed in '{}'", libPath.toString());
            // } else {
            // libPath = Paths.get(currentLibPath);
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
            pyATV.init(this); // pass class instance for callbacks
        } catch (Exception e) {
            logger.error("Unable to start Python (jpy): {} ({})", e.getMessage(), e.getClass());
        }
    }

    /**
     * This function will be called from the PyATV module to display an info message
     *
     * @param message
     */
    public void info(String message) {
        logger.info("{}", message);
    }

    /**
     * This function will be called from the PyATV module to display a debug message
     *
     * @param message
     */
    public void debug(String message) {
        logger.debug("{}", message);
    }

    /**
     * Send one or more commands to the PyATV module.
     *
     * @param commands  command sequence seperated by ' '
     * @param ipAddress IP address of the Apple-TV
     * @param loginId   Login ID resulting from device pairing
     * @return true: successful, false: failed, e.g. exception in the PyATV module
     */
    public boolean sendCommands(String commands, String ipAddress, String loginId) {
        try {
            logger.info("Sending command {} to ip {}, lid {}", commands, ipAddress, loginId);

            String[] args = new String[20];
            // PyObject res = plugIn.exec("--address 192.168.x.y --login_id 0xXXXXXXXXXXXXXXXX top_menu");
            int a = 1;
            args[0] = "--debug";
            if (!ipAddress.isEmpty()) {
                args[a++] = "--address";
                args[a++] = ipAddress;
            }
            if (!loginId.isEmpty()) {
                args[a++] = "--login_id";
                args[a++] = loginId;
            }

            StringTokenizer tokenizer = new StringTokenizer(commands, " ");
            while (tokenizer.hasMoreElements()) {
                args[a++] = tokenizer.nextToken();
            }
            return pyATV.exec(args).getIntValue() == 1;
        } catch (Exception e) {
            logger.error("Exception on PyATV call: {} ({})", e.getMessage(), e.getClass());
            return false;
        }
    }

    /**
     * Calls the PyATV module to scan for devices.
     * Once successful the PyATV module will call devicesDiscovered() to pass the resulting device json
     *
     * @return Device list in JSON format
     */
    public String scanDevices() {
        try {
            logger.info("Scan for AppleTV devices");

            jsonDevices = "";
            String[] args = new String[1];
            args[0] = "scan";
            // the excec(scan) call will do a callback and fills jsonDevices before returning
            if (pyATV.exec(args).getIntValue() == 0) {
                return jsonDevices;
            }
            logger.error("Scanning for Apple-TV devices failed!");
        } catch (Exception e) {
            logger.error("Exception on PyATV call: {} ({})", e.getMessage(), e.getClass());
        }
        return "";
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

    public String getLibPath() {
        return libPath.toString();
    }

    void dispose() {
        if (started) {
            PyLib.Diag.setFlags(PyLib.Diag.F_OFF);
            PyLib.stopPython();
        }
    }
}
