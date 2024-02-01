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

package org.hid4java.macos;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.sun.jna.Pointer;
import com.sun.jna.ptr.ShortByReference;
import org.hid4java.HidDevice;
import org.hid4java.HidException;
import org.hid4java.HidServicesSpecification;
import org.hid4java.NativeHidDeviceManager;
import vavix.rococoa.corefoundation.CFAllocator;
import vavix.rococoa.corefoundation.CFDictionary;
import vavix.rococoa.corefoundation.CFIndex;
import vavix.rococoa.corefoundation.CFLib;
import vavix.rococoa.corefoundation.CFNumber;
import vavix.rococoa.corefoundation.CFType;
import vavix.rococoa.iokit.IOKitLib;

import static vavix.rococoa.corefoundation.CFLib.kCFCoreFoundationVersionNumber;
import static vavix.rococoa.corefoundation.CFLib.kCFRunLoopDefaultMode;
import static vavix.rococoa.corefoundation.CFString.CFSTR;
import static vavix.rococoa.iokit.IOKitLib.kIOHIDOptionsTypeNone;
import static vavix.rococoa.iokit.IOKitLib.kIOHIDOptionsTypeSeizeDevice;


/**
 * JNA utility class to provide the following to low level operations:
 * <ul>
 * <li>Direct access to the HID API library through JNA</li>
 * </ul>
 *
 * @since 0.0.1
 */
public class MacosHidDeviceManager implements NativeHidDeviceManager {

    private static final Logger logger = Logger.getLogger(MacosHidDeviceManager.class.getName());

    /** */
    private final Map<String, MacosHidDevice> devices = new HashMap<>();

    /** Run context */
    private Pointer /* IOHIDManagerRef */ manager;

    /** */
    static final boolean is_macos_10_10_or_greater = kCFCoreFoundationVersionNumber >= 1151.16;

    /** */
    private int /* IOOptionBits */ deviceOpenOptions = 0;

    /**
     * Changes the behavior of all further calls to {@link #create(int, int, String)} or {@link NativeHidDeviceManager#create(HidDevice.Info)}.
     * <p>
     * All devices opened by HIDAPI with {@link #create(int, int, String)} or {@link NativeHidDeviceManager#create(HidDevice.Info)}
     * are opened in exclusive mode per default.
     * <p>
     *
     * @param openExclusive When set to true - all further devices will be opened in non-exclusive mode.
     *                      Otherwise - all further devices will be opened in exclusive mode.
     */
    private void hidDarwinSetOpenExclusive(boolean openExclusive) {
        deviceOpenOptions = !openExclusive ? kIOHIDOptionsTypeNone : kIOHIDOptionsTypeSeizeDevice;
    }

    @Override
    public void open(HidServicesSpecification specification) {
logger.finer("is_macos_10_10_or_greater: " + is_macos_10_10_or_greater);
        hidDarwinSetOpenExclusive(!specification.darwinOpenDevicesNonExclusive); // Backward compatibility

        //
        manager = IOKitLib.INSTANCE.IOHIDManagerCreate(CFAllocator.kCFAllocatorDefault, kIOHIDOptionsTypeNone);
        if (manager != null) {
            IOKitLib.INSTANCE.IOHIDManagerSetDeviceMatching(manager, null);
            IOKitLib.INSTANCE.IOHIDManagerScheduleWithRunLoop(manager, CFLib.INSTANCE.CFRunLoopGetCurrent(), kCFRunLoopDefaultMode);
        } else {
            throw new IllegalStateException("Failed to create IOHIDManager");
        }
    }

    @Override
    @SuppressWarnings("WhileLoopReplaceableByForEach") // for ConcurrentModificationException
    public void close() {
logger.finer("here10.0: hid_exit");
        Iterator<MacosHidDevice> i = devices.values().iterator();
        while (i.hasNext()) {
            i.next().close();
        }

        if (manager != null) {
            /* Close the HID manager. */
logger.finer("here10.1: manager close");
            IOKitLib.INSTANCE.IOHIDManagerClose(manager, kIOHIDOptionsTypeNone);
            CFLib.INSTANCE.CFRelease(manager);
            manager = null;
logger.finer("here10.2: manager = null");
        }
    }

    @Override
    public MacosHidDevice create(int vendorId, int productId, String serialNumber) throws IOException {
        // This function is identical to the Linux version. Platform independent.

        List<HidDevice.Info> infos;
        HidDevice.Info infoToOpen = null;
        MacosHidDevice device;

        // throw new RuntimeException: global error is reset by hid_enumerate/hid_init
        infos = enumerate(vendorId, productId);
        if (infos.isEmpty()) {
            // throw new RuntimeException: global error is already set by hid_enumerate
            return null;
        }

        for (HidDevice.Info info : infos) {
            if (info.vendorId == vendorId &&
                    info.productId == productId) {
                if (serialNumber != null) {
                    if (serialNumber.equals(info.serialNumber)) {
                        infoToOpen = info;
                        break;
                    }
                } else {
                    infoToOpen = info;
                    break;
                }
            }
        }

        if (infoToOpen != null) {
            device = create(infoToOpen);
        } else {
            throw new HidException("Device with requested VID/PID/(SerialNumber) not found");
        }

        return device;
    }

    /** */
    private static void processPendingEvents() {
        int res;
        do {
            res = CFLib.INSTANCE.CFRunLoopRunInMode(kCFRunLoopDefaultMode, 0.001, false);
        } while (res != CFLib.kCFRunLoopRunFinished && res != CFLib.kCFRunLoopRunTimedOut);
    }

    @Override
    public List<HidDevice.Info> enumerate(int vendorId, int productId) throws IOException {
        List<HidDevice.Info> deviceInfos = new ArrayList<>(); // return object
        int numDevices;

        // give the IOHIDManager a chance to update itself
        processPendingEvents();

        // Get a list of the Devices
        CFDictionary /* CFMutableDictionaryRef */ matching = null;
        if (vendorId != 0 || productId != 0) {
            matching = CFLib.INSTANCE.CFDictionaryCreateMutable(CFAllocator.kCFAllocatorDefault,
                    CFIndex.of(kIOHIDOptionsTypeNone), CFLib.kCFTypeDictionaryKeyCallBacks, CFLib.kCFTypeDictionaryValueCallBacks);

            if (matching != null && vendorId != 0) {
                ShortByReference r = new ShortByReference((short) vendorId);
                CFNumber v = CFLib.INSTANCE.CFNumberCreate(CFAllocator.kCFAllocatorDefault, CFLib.CFNumberType.kCFNumberShortType, r);
                CFLib.INSTANCE.CFDictionarySetValue(matching, CFSTR(IOKitLib.kIOHIDVendorIDKey), v);
                CFLib.INSTANCE.CFRelease(v);
            }

            if (matching != null && productId != 0) {
                ShortByReference r = new ShortByReference((short) productId);
                CFNumber p = CFLib.INSTANCE.CFNumberCreate(CFAllocator.kCFAllocatorDefault, CFLib.CFNumberType.kCFNumberShortType, r);
                CFLib.INSTANCE.CFDictionarySetValue(matching, CFSTR(IOKitLib.kIOHIDProductIDKey), p);
                CFLib.INSTANCE.CFRelease(p);
            }
        }
logger.finest("here7.0: " + matching + ", " + Thread.currentThread() + ", " + manager);
        IOKitLib.INSTANCE.IOHIDManagerSetDeviceMatching(manager, matching);
        if (matching != null) {
logger.finer("here7.1: matching null");
            CFLib.INSTANCE.CFRelease(matching);
        }

        CFType /* CFSetRef */ deviceSet = IOKitLib.INSTANCE.IOHIDManagerCopyDevices(manager);

        Pointer[] /* IOHIDDeviceRef */ devices;

        if (deviceSet != null) {
            // Convert the list into a C array so we can iterate easily.
            numDevices = CFLib.INSTANCE.CFSetGetCount(deviceSet).intValue();
logger.finest("num_devices: " + numDevices);
            devices = new Pointer[(int) numDevices];
            CFLib.INSTANCE.CFSetGetValues(deviceSet, devices);
        } else {
            throw new HidException("IOHIDManagerCopyDevices");
        }

        // Iterate over each device, making an entry for it.
        for (int i = 0; i < numDevices; i++) {

            Pointer /* IOHIDDeviceRef */ dev = devices[i];
            if (dev == null) {
logger.fine("device null: " + devices[i]);
                continue;
            }

            IOHIDDevice nativeDevice = new IOHIDDevice(dev);
            List<HidDevice.Info> infos = nativeDevice.createDeviceInfo();
            if (infos.isEmpty()) {
logger.fine("empty");
                continue;
            }

            deviceInfos.addAll(infos);
        }

        CFLib.INSTANCE.CFRelease(deviceSet);

        if (deviceInfos.isEmpty()) {
            if (vendorId == 0 && productId == 0) {
                throw new HidException("No HID devices found in the system.");
            } else {
                throw new HidException("No HID devices with requested VID/PID found in the system.");
            }
        }

        return deviceInfos;
    }

    @Override
    public MacosHidDevice create(HidDevice.Info info) throws IOException {
logger.finest("here00.0: path: " + info.path);
        MacosHidDevice device = devices.get(info.path);
        if (device != null) {
logger.finest("here00.1: devices: cached: " + device);
            device.deviceInfo = info;
            return device;
        }

        device = new MacosHidDevice(deviceOpenOptions);
        device.deviceInfo = info;

        device.closer = devices::remove;
        devices.put(device.deviceInfo.path, device);
logger.finest("here00.E: devices: +: " + device.deviceInfo.path + " / " + devices.size());
        return device;
    }

    @Override
    public boolean isSupported() {
        return System.getProperty("os.name").toLowerCase().contains("mac");
    }
}
