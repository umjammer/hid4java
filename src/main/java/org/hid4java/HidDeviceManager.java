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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.ServiceLoader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hid4java.HidServicesSpecification.ScanMode;


/**
 * Manager to provide the following to HID services:
 * <ul>
 * <li>Access to the underlying JNA and hidapi library</li>
 * <li>Device attach/detach detection (if configured)</li>
 * <li>Device data read (if configured)</li>
 * </ul>
 *
 * @since 0.0.1
 */
public class HidDeviceManager {

    private static final Logger logger = Logger.getLogger(HidDeviceManager.class.getName());

    /**
     * The native device provider.
     */
    private final NativeHidDeviceManager nativeManager;

    /**
     * The HID services specification providing configuration parameters
     */
    private final HidServicesSpecification hidServicesSpecification;

    /**
     * The currently attached devices keyed on ID
     */
    private final Map<String, HidDevice> attachedDevices = Collections.synchronizedMap(new HashMap<>());

    /**
     * HID services listener list
     */
    private final HidServicesListenerSupport listenerList;

    /**
     * The device enumeration thread
     * <p>
     * We use a Thread instead of Executor since it may be stopped/paused/restarted frequently
     * and executors are more heavyweight in this regard
     */
    private ExecutorService scanThread;

    /**
     * Constructs a new device manager
     *
     * @param listenerList             The HID services providing access to the event model
     * @param hidServicesSpecification Provides various parameters for configuring HID services
     * @throws HidException If USB HID initialization fails
     */
    HidDeviceManager(HidServicesListenerSupport listenerList, HidServicesSpecification hidServicesSpecification) throws HidException {

        this.listenerList = listenerList;
        this.hidServicesSpecification = hidServicesSpecification;

        // Attempt to initialise and fail fast
        try {
            for (NativeHidDeviceManager manager : ServiceLoader.load(NativeHidDeviceManager.class)) {
                if (manager.isSupported()) {
                    nativeManager = manager;
logger.finer("native device manager: " + nativeManager.getClass().getName());
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
     * Starts the manager
     * <p>
     * If already started (scanning) it will immediately return without doing anything
     * <p>
     * Otherwise this will perform a one-off scan of all devices then if the scan interval
     * is zero will stop there or will start the scanning daemon thread at the required interval.
     *
     * @throws HidException If something goes wrong (such as Hidapi not initialising correctly)
     */
    public void start() throws IOException {

        nativeManager.open(hidServicesSpecification);

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
     * Stop the scan thread and close all attached devices
     * <p>
     * This is normally part of a general application shutdown
     */
    public synchronized void stop() throws IOException {

        stopScanThread();

        // Close all attached devices
        for (HidDevice hidDevice : attachedDevices.values()) {
            hidDevice.close();
        }
    }

    public synchronized NativeHidDevice open(HidDevice.Info info) throws IOException {
        return nativeManager.create(info);
    }

    /**
     * Updates the device list by adding newly connected devices to it and by
     * removing no longer connected devices.
     * <p>
     * Will fire attach/detach events as appropriate.
     */
    public synchronized void scan() throws IOException {

        List<String> removeList = new ArrayList<>();

        List<HidDevice> attachedHidDeviceList = getAttachedHidDevices();

        for (HidDevice attachedDevice : attachedHidDeviceList) {

            if (!this.attachedDevices.containsKey(attachedDevice.getId())) {

logger.finest("device: " + attachedDevice.getProductId() + "," + attachedDevice);
                // Device has become attached so add it but do not create
                attachedDevices.put(attachedDevice.getId(), attachedDevice);

                // Fire the event on a separate thread
                listenerList.fireHidDeviceAttached(attachedDevice);
            }
        }

        for (Map.Entry<String, HidDevice> entry : attachedDevices.entrySet()) {

            String deviceId = entry.getKey();
            HidDevice hidDevice = entry.getValue();

            if (!attachedHidDeviceList.contains(hidDevice)) {

                // Keep track of removals
                removeList.add(deviceId);

                // Fire the event on a separate thread
                listenerList.fireHidDeviceDetached(this.attachedDevices.get(deviceId));
            }
        }

        if (!removeList.isEmpty()) {
            // Update the attached devices map
            removeList.forEach(this.attachedDevices.keySet()::remove);
        }
    }

    /**
     * @return True if the scan thread is running, false otherwise.
     */
    public boolean isScanning() {
        return scanThread != null && !scanThread.isTerminated();
    }

    /**
     * @return A list of all attached HID devices
     */
    public List<HidDevice> getAttachedHidDevices() throws IOException {

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
                        open(hidDeviceInfo),
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

        if (ScanMode.SCAN_AT_FIXED_INTERVAL_WITH_PAUSE_AFTER_WRITE == hidServicesSpecification.getScanMode() && isScanning()) {
            stopScanThread();
            // Ensure we have a new scan executor service available
            configureScanThread(getScanRunnable());
        }
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

        int scanInterval = hidServicesSpecification.getScanInterval();
        int pauseInterval = hidServicesSpecification.getPauseInterval();

        switch (hidServicesSpecification.getScanMode()) {
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

    /** */
    public void shutdown() {
logger.finer("shutdown.0");
        if (isScanning()) {
            stopScanThread();
        }
logger.finer("shutdown.1");
        nativeManager.close();
logger.finer("shutdown.2");
    }

    /** debug */
    NativeHidDeviceManager getNativeHidDeviceManager() {
        return nativeManager;
    }
}