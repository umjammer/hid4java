/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014-2015 Gary Rowe
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package org.hid4java;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hid4java.HidSpecification.ScanMode;


/**
 * JNA bridge class to provide the following to USB HID:
 * <ul>
 * <li>Access to the native api via JNA</li>
 * </ul>
 * Requires the hidapi to be present on the classpath or the system library search path.
 *
 * @since 0.0.1
 */
public class HidDevices {

    private static final Logger logger = Logger.getLogger(HidDevices.class.getName());

    static {
        try {
            try (InputStream is = HidDevices.class.getResourceAsStream("/META-INF/maven/org.hid4java/hid4java/pom.properties")) {
                if (is != null) {
                    Properties props = new Properties();
                    props.load(is);
                    version = props.getProperty("version", "undefined in pom.properties");
                } else {
                    version = System.getProperty("vavi.test.version", "undefined");
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static final String version;

    /**
     * The native device provider.
     */
    private final NativeHidDevices nativeManager;

    /**
     * The HID services specification providing configuration parameters
     */
    private final HidSpecification hidSpecification;

    /**
     * The currently attached devices keyed on ID
     */
    private final Map<String, HidDevice> attachedDevices = Collections.synchronizedMap(new HashMap<>());

    /**
     * The device enumeration thread
     * <p>
     * We use a Thread instead of Executor since it may be stopped/paused/restarted frequently
     * and executors are more heavyweight in this regard
     */
    private ExecutorService scanThread;

    /**
     * The HID services listeners for receiving attach/detach events etc
     */
    private final HidDevicesListenerSupport listeners = new HidDevicesListenerSupport();

    /**
     * Jar entry point to allow for version interrogation
     *
     * @param args Nothing required
     */
    public static void main(String[] args) {
        System.out.println("Version: " + version);
    }

    /**
     * Initialise with a default HID specification
     *
     * @throws HidException If something goes wrong (see {@link HidDevices#HidDevices(HidSpecification)}
     */
    public HidDevices() throws IOException {
        this(new HidSpecification());
    }

    /**
     * @param hidSpecification Provides various parameters for configuring HID services
     * @throws HidException If something goes wrong
     */
    public HidDevices(HidSpecification hidSpecification) throws IOException {
        this.hidSpecification = hidSpecification;

        // Attempt to initialise and fail fast
        try {
            for (NativeHidDevices manager : ServiceLoader.load(NativeHidDevices.class)) {
                if (manager.isSupported()) {
                    nativeManager = manager;
logger.finer("native device manager: " + nativeManager.getClass().getName());
                    // Check for automatic start (default behaviour for 0.6.0 and below)
                    // which will prevent an attachment event firing if the device is already
                    // attached since listeners will not have been registered at this point
                    if (hidSpecification.isAutoStart()) {
                        start();
                    }

                    if (hidSpecification.isAutoShutdown()) {
                        // Ensure we release resources during shutdown
                        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
                    }
                    return;
                }
            }
            throw new NoSuchElementException("no suitable native device maneger");
        } catch (Throwable t) {
            // Typically this is a linking issue with the native library
            throw new HidException("Hidapi did not initialise: " + t.getMessage(), t);
        }
    }

    /**
     * Stop all device threads and shut down the {@link NativeHidDevice}
     */
    public void shutdown() {
logger.finer("shutdown: start shutdown...");
//new Exception().printStackTrace();
        try {
            stop();
        } catch (Throwable e) {
            // Silently fail (user will already have been given an exception)
logger.log(Level.FINER, e.getMessage(), e);
        }
        try {
            nativeManager.close();
        } catch (Throwable e) {
            // Silently fail (user will already have been given an exception)
logger.log(Level.FINER, e.getMessage(), e);
        }
if (logger.isLoggable(Level.FINER)) {
 Thread.getAllStackTraces().keySet().forEach(System.err::println);
}
    }

    /**
     * Stop all threads (enumeration, data read etc), close all devices
     * and clear all listeners
     * <p>
     * Normally part of an application shutdown
     */
    public void stop() throws IOException {
logger.finer("stop: start stopping...");
        stopScanThread();

        // Close all attached devices
        for (HidDevice hidDevice : attachedDevices.values()) {
            hidDevice.close();
        }

        this.listeners.clear();
    }

    /**
     * Start all threads (enumeration, data read etc) as configured
     */
    public void start() throws IOException {
        nativeManager.open(hidSpecification);

        // Check for previous start
        if (this.isScanning()) {
            return;
        }

        // Perform a one-off scan to populate attached devices
        scan();

        // Ensure we have a scan thread available
        configureScanThread(getScanRunnable());
    }

    /**
     * @param listener The listener to add
     */
    public void addHidServicesListener(HidDevicesListener listener) {
        this.listeners.add(listener);
    }

    /**
     * @param listener The listener to remove
     */
    public void removeHidServicesListener(HidDevicesListener listener) {
        this.listeners.remove(listener);
    }

    /**
     * Manually scans for HID device connection changes and triggers listener events as required
     */
    public void scan() throws IOException {
        List<String> removeList = new ArrayList<>();

        List<HidDevice> attachedHidDeviceList = getHidDevices();

        for (HidDevice attachedDevice : attachedHidDeviceList) {

            if (!this.attachedDevices.containsKey(attachedDevice.getId())) {

logger.finest("device: " + attachedDevice.getProductId() + "," + attachedDevice);
                // Device has become attached so add it but do not create
                attachedDevices.put(attachedDevice.getId(), attachedDevice);

                // Fire the event on a separate thread
                listeners.fireHidDeviceAttached(attachedDevice);
            }
        }

        for (Map.Entry<String, HidDevice> entry : attachedDevices.entrySet()) {

            String deviceId = entry.getKey();
            HidDevice hidDevice = entry.getValue();

            if (!attachedHidDeviceList.contains(hidDevice)) {

                // Keep track of removals
                removeList.add(deviceId);

                // Fire the event on a separate thread
                listeners.fireHidDeviceDetached(this.attachedDevices.get(deviceId));
            }
        }

        if (!removeList.isEmpty()) {
            // Update the attached devices map
            removeList.forEach(this.attachedDevices.keySet()::remove);
        }
    }

    /**
     * @return A list of all attached HID devices
     */
    public List<HidDevice> getHidDevices() throws IOException {
        List<HidDevice> hidDeviceList = new ArrayList<>();

        List<HidDevice.Info> infos;
        try {
            // Use 0,0 to list all attached devices
            // This comes back as a linked list from hidapi
            infos = nativeManager.enumerate(0, 0);
        } catch (Throwable e) {
logger.log(Level.FINE, "hid_enumerate", e);
            // Could not initialise hidapi (possibly an unknown platform)
            // Trigger a general stop as something serious has happened
            stop();
            // Inform the caller that something serious has gone wrong
            throw new HidException("Unable to start HidDeviceManager: " + e.getMessage());
        }

        if (!infos.isEmpty()) {

            for (HidDevice.Info hidDeviceInfo : infos) {
                // Wrap in HidDevice
                hidDeviceList.add(new HidDevice(
                        hidDeviceInfo,
                        nativeManager.create(hidDeviceInfo),
                        this::afterDeviceWrite
                ));
                // Move to the next in the linked list
            }
        }

        return hidDeviceList;
    }

    /**
     * Indicate that a device write has occurred which may require a change in scanning frequency
     */
    private void afterDeviceWrite() {

        if (ScanMode.SCAN_AT_FIXED_INTERVAL_WITH_PAUSE_AFTER_WRITE == hidSpecification.getScanMode() && isScanning()) {
            stopScanThread();
            // Ensure we have a new scan executor service available
            configureScanThread(getScanRunnable());
        }
    }

    /**
     * Returns a not opened device.
     * @param vendorId     The vendor ID
     * @param productId    The product ID
     * @param serialNumber The serial number (use null for wildcard)
     * @return The device if attached, null if detached
     */
    public HidDevice getHidDevice(int vendorId, int productId, String serialNumber) throws IOException {

        List<HidDevice> devices = getHidDevices();
        for (HidDevice device : devices) {
            if (device.isVidPidSerial(vendorId, productId, serialNumber)) {
                return device;
            }
        }

        return null;
    }

    /**
     * @return True if the scan thread is running, false otherwise.
     */
    public boolean isScanning() {
        return scanThread != null && !scanThread.isTerminated();
    }

    /**
     * Stop the scan thread
     */
    private synchronized void stopScanThread() {

        if (isScanning()) {
            scanThread.shutdownNow();
            scanThread = null;
        }
    }

    /**
     * Configures the scan thread to allow recovery from stop or pause
     */
    private synchronized void configureScanThread(Runnable scanRunnable) {

        if (isScanning()) {
            stopScanThread();
        }

        scanThread = Executors.newSingleThreadExecutor();

        // Require a new one
        scanThread.submit(scanRunnable);
    }

    /** */
    private synchronized Runnable getScanRunnable() {

        int scanInterval = hidSpecification.getScanInterval();
        int pauseInterval = hidSpecification.getPauseInterval();

        switch (hidSpecification.getScanMode()) {
            case NO_SCAN:
                return () -> {
                    // Do nothing
                };
            case SCAN_AT_FIXED_INTERVAL:
                return () -> {

                    while (true) {
                        try {
                            //noinspection BusyWait
                            Thread.sleep(scanInterval);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                        try {
                            scan();
                        } catch (IOException e) {
                            logger.fine(e.toString());
                        }
                    }
                };
            case SCAN_AT_FIXED_INTERVAL_WITH_PAUSE_AFTER_WRITE:
                return () -> {
                    // Provide an initial pause
                    try {
                        Thread.sleep(pauseInterval);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }

                    // Switch to continuous running
                    while (true) {
                        try {
                            //noinspection BusyWait
                            Thread.sleep(scanInterval);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                        try {
                            scan();
                        } catch (IOException e) {
                            logger.fine(e.toString());
                        }
                    }
                };
            default:
                return null;
        }
    }

    /** debug */
    NativeHidDevices getNativeHidDevices() {
        return nativeManager;
    }
}
