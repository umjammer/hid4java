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
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sun.jna.Memory;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.ShortByReference;
import org.hid4java.HidDevice;
import org.hid4java.HidException;
import org.hid4java.InputReportEvent;
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
import static vavix.rococoa.iokit.IOKitLib.kIOHIDOptionsTypeNone;
import static vavix.rococoa.iokit.IOKitLib.kIOHIDOptionsTypeSeizeDevice;
import static vavix.rococoa.iokit.IOKitLib.kIOReturnSuccess;


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

    /**  */
    private List<MacosHidDevice> devices = new ArrayList<>();

    /**
     * When false - all devices will be opened in exclusive mode. (Default)
     * When true - all devices will be opened in non-exclusive mode.
     * <p>
     * See {@link #hid_darwin_set_open_exclusive(int)} for more information.
     */
    public static boolean darwinOpenDevicesNonExclusive = false;

    private static final Pointer MACH_PORT_NULL = Pointer.NULL;

    // Run context
    private Pointer /* IOHIDManagerRef */ manager;
    static final boolean is_macos_10_10_or_greater = kCFCoreFoundationVersionNumber >= 1151.16;

    /**  */
    private int /* IOOptionBits */ device_open_options = 0;

    /**
     * Changes the behavior of all further calls to {@link #open(int, int, String)} or {@link NativeHidDeviceManager#open(HidDevice.Info)}.
     * <p>
     * All devices opened by HIDAPI with {@link #open(int, int, String)} or {@link NativeHidDeviceManager#open(HidDevice.Info)}
     * are opened in exclusive mode per default.
     * <p>
     * Calling this function before {@link #MacosHidDeviceManager()} or after {@link #exit()} has no effect.
     *
     * @param openExclusive When set to 0 - all further devices will be opened in non-exclusive mode.
     *                      Otherwise - all further devices will be opened in exclusive mode.
     * @since hidapi 0.12.0
     */
    private void hid_darwin_set_open_exclusive(int openExclusive) {
        device_open_options = (openExclusive == 0) ? kIOHIDOptionsTypeNone : kIOHIDOptionsTypeSeizeDevice;
    }

    /**
     * Initialize all the HID Manager Objects
     *
     * @throws IllegalStateException when failed
     */
    public MacosHidDeviceManager() {
logger.fine("is_macos_10_10_or_greater: " + is_macos_10_10_or_greater);
        hid_darwin_set_open_exclusive(darwinOpenDevicesNonExclusive ? 0 : 1); // Backward compatibility

        manager = IOKitLib.INSTANCE.IOHIDManagerCreate(CFAllocator.kCFAllocatorDefault, kIOHIDOptionsTypeNone);
        if (manager != null) {
            IOKitLib.INSTANCE.IOHIDManagerSetDeviceMatching(manager, null);
            IOKitLib.INSTANCE.IOHIDManagerScheduleWithRunLoop(manager, CFLib.INSTANCE.CFRunLoopGetCurrent(), kCFRunLoopDefaultMode);
        } else {
            throw new IllegalStateException("Failed to create IOHIDManager");
        }
    }

    @Override
    public void exit() {
logger.finest("here10.0: hid_exit");
        devices.forEach(MacosHidDevice::close);

        if (manager != null) {
            /* Close the HID manager. */
logger.fine("here10.1");
            IOKitLib.INSTANCE.IOHIDManagerClose(manager, kIOHIDOptionsTypeNone);
            CFLib.INSTANCE.CFRelease(manager);
            manager = null;
logger.fine("here10.2");
//Thread.getAllStackTraces().keySet().forEach(System.err::println);
        }
    }

    @Override
    public MacosHidDevice open(int vendor_id, int product_id, String serial_number) throws IOException {
        // This function is identical to the Linux version. Platform independent.

        List<HidDevice.Info> infos;
        HidDevice.Info infoToOpen = null;
        MacosHidDevice device;

        // throw new RuntimeException: global error is reset by hid_enumerate/hid_init
        infos = enumerate(vendor_id, product_id);
        if (infos.isEmpty()) {
            // throw new RuntimeException: global error is already set by hid_enumerate
            return null;
        }

        for (HidDevice.Info info : infos) {
            if (info.vendorId == vendor_id &&
                    info.productId == product_id) {
                if (serial_number != null) {
                    if (serial_number.equals(info.serialNumber)) {
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
            device = open(infoToOpen);
        } else {
            throw new HidException("Device with requested VID/PID/(SerialNumber) not found");
        }

        return device;
    }

    /**  */
    private void processPendingEvents() {
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
                    new NativeLong(kIOHIDOptionsTypeNone), CFLib.kCFTypeDictionaryKeyCallBacks, CFLib.kCFTypeDictionaryValueCallBacks);

            if (matching != null && vendorId != 0) {
                ShortByReference r = new ShortByReference((short) vendorId);
                CFNumber v = CFLib.INSTANCE.CFNumberCreate(CFAllocator.kCFAllocatorDefault, CFLib.CFNumberType.kCFNumberShortType, r);
                CFLib.INSTANCE.CFDictionarySetValue(matching, CFLib.INSTANCE.__CFStringMakeConstantString(IOKitLib.kIOHIDVendorIDKey), v);
                CFLib.INSTANCE.CFRelease(v);
            }

            if (matching != null && productId != 0) {
                ShortByReference r = new ShortByReference((short) productId);
                CFNumber p = CFLib.INSTANCE.CFNumberCreate(CFAllocator.kCFAllocatorDefault, CFLib.CFNumberType.kCFNumberShortType, r);
                CFLib.INSTANCE.CFDictionarySetValue(matching, CFLib.INSTANCE.__CFStringMakeConstantString(IOKitLib.kIOHIDProductIDKey), p);
                CFLib.INSTANCE.CFRelease(p);
            }
        }
logger.finest("here7.0: " + matching + ", " + Thread.currentThread());
        IOKitLib.INSTANCE.IOHIDManagerSetDeviceMatching(manager, matching);
logger.finest("here7.1");
        if (matching != null) {
            CFLib.INSTANCE.CFRelease(matching);
        }

        CFType /* CFSetRef */ deviceSet = IOKitLib.INSTANCE.IOHIDManagerCopyDevices(manager);

        Pointer[] /* IOHIDDeviceRef */ devices;

        if (deviceSet != null) {
            // Convert the list into a C array so we can iterate easily.
            numDevices = CFLib.INSTANCE.CFSetGetCount(deviceSet).intValue();
logger.finer("num_devices: " + numDevices);
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
            List<HidDevice.Info> infos = nativeDevice.create_device_info();
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

    /**
     * path must be one of:
     * - in format 'DevSrvsID:<RegistryEntryID>' (as returned by hid_enumerate);
     * - a valid path to an IOHIDDevice in the IOService plane (as returned by IORegistryEntryGetPath,
     * e.g.: "IOService:/AppleACPIPlatformExpert/PCI0@0/AppleACPIPCI/EHC1@1D,7/AppleUSBEHCI/PLAYSTATION(R)3 Controller@fd120000/IOUSBInterface@0/IOUSBHIDDriver");
     * Second format is for compatibility with paths accepted by older versions of HIDAPI.
     */
    private Pointer /* io_registry_entry_t */ openServiceRegistryFromPath(String path) {
        if (path == null)
            return MACH_PORT_NULL;

        // Get the IORegistry entry for the given path
logger.finer("path: " + path.substring(10));
        if (path.startsWith("DevSrvsID:")) {
            long entryId = Long.parseLong(path.substring(10));
            return IOKitLib.INSTANCE.IOServiceGetMatchingService(/* mach_port_t */ Pointer.NULL, IOKitLib.INSTANCE.IORegistryEntryIDMatching(entryId).asDict());
        } else {
            // Fallback to older format of the path
            ByteBuffer bb = ByteBuffer.wrap(path.getBytes());
            return IOKitLib.INSTANCE.IORegistryEntryFromPath(/* mach_port_t */ Pointer.NULL, bb);
        }
    }

    /** Stop the Run Loop for this device */
    private void onDeviceRemovalCallback(Pointer context, int /* IOReturn */ result, Pointer sender) {
        MacosHidDevice dev = (MacosHidDevice) UserObjectContext.getObjectFromContext(context);
        if (dev == null) {
logger.finer("here5.0: dev is null");
            return;
        }
logger.fine("here5: device_removal_callback: dev: " + dev.deviceInfo.product);

        dev.disconnected = true;
        dev.close();
    }

    /**
     * This gets called when the read_thread's run loop gets signaled by
     * {@link MacosHidDevice#close()}, and serves to stop the read_thread's run loop.
     */
    private void onSignalCallback(Pointer context) {
        MacosHidDevice dev = (MacosHidDevice) UserObjectContext.getObjectFromContext(context);
        if (dev == null) {
logger.finer("here3.0: dev is null");
            return;
        }
logger.fine("here3.1: signal_callback: dev: " + dev.deviceInfo.product);

        CFLib.INSTANCE.CFRunLoopStop(dev.runLoop); // TODO CFRunLoopGetCurrent()
logger.finest("here3.2: stop run loop: @" + dev.runLoop.hashCode());
    }

    /**
     * The Run Loop calls this function for each input report received.
     * This function puts the data into a linked list to be picked up by
     * {@link org.hid4java.InputReportListener#onInputReport(InputReportEvent)}.
     *
     * @see IOKitLib.IOHIDReportCallback
     */
    private void onReportCallback(Pointer context, int/*IOReturn*/ result, Pointer sender, int/*IOHIDReportType*/ report_type, int report_id, Pointer report, CFIndex reportRength) {
        MacosHidDevice dev = (MacosHidDevice) UserObjectContext.getObjectFromContext(context);
        if (dev == null) {
logger.finer("here4.0: dev is null");
            return;
        }
logger.finest("here4: report_callback: dev: " + dev.deviceInfo.product);

        // Make a new Input Report object
        dev.inputData = new byte[reportRength.intValue()];
        report.read(0, dev.inputData, 0, dev.inputData.length);

        // Signal a waiting thread that there is data.
        dev.fireInputReport(new InputReportEvent(this, report_id, dev.inputData));
logger.finest("here4.1: report: " + reportRength.intValue());
    }

    @Override
    public MacosHidDevice open(HidDevice.Info info) throws IOException {
logger.finer("here00.0: path: " + info);
        Pointer/* io_registry_entry_t */ entry = null;
        MacosHidDevice device = new MacosHidDevice(device_open_options);
        device.deviceInfo = info;
        try {
            // Get the IORegistry entry for the given path
            entry = openServiceRegistryFromPath(info.path);
            if (entry == MACH_PORT_NULL) {
                // Path wasn't valid (maybe device was removed?)
                throw new IOException("hid_open_path: device mach entry not found with the given path");
            }
logger.finest("here00.1: entry: " + entry);

            // Create an IOHIDDevice for the entry
            Pointer /* IOHIDDevice */ deviceHandle = IOKitLib.INSTANCE.IOHIDDeviceCreate(CFAllocator.kCFAllocatorDefault, entry);
            if (deviceHandle == null) {
                // Error creating the HID device
                throw new IOException("hid_open_path: failed to create IOHIDDevice from the mach entry");
            }

            // Open the IOHIDDevice
            int /* IOReturn */ ret = IOKitLib.INSTANCE.IOHIDDeviceOpen(deviceHandle, device.openOptions);
            if (ret != kIOReturnSuccess) {
                throw new IOException(String.format("hid_open_path: failed to open IOHIDDevice from mach entry: (0x%08X)", ret));
            }

            device.deviceHandle = new IOHIDDevice(deviceHandle);

            // Create the buffers for receiving data
            device.maxInputReportLength = device.deviceHandle.get_int_property(IOKitLib.kIOHIDMaxInputReportSizeKey);
            if (device.maxInputReportLength > 0) {
                device.inputData = new byte[device.maxInputReportLength];
                device.inputReportBuffer = new Memory(device.maxInputReportLength);
            }

            // Create the Run Loop Mode for this device.
            // printing the reference seems to work.
            String str = String.format("HIDAPI_%x", Pointer.nativeValue(device.deviceHandle.device));
logger.finest("here00.2: str: " + str);
            device.runLoopMode = CFLib.INSTANCE.CFStringCreateWithCString(null, str.getBytes(StandardCharsets.US_ASCII), CFLib.kCFStringEncodingASCII);

            // Attach the device to a Run Loop
            UserObjectContext.ByReference object_context = UserObjectContext.createContext(device);
            if (device.maxInputReportLength > 0) {
                IOKitLib.INSTANCE.IOHIDDeviceRegisterInputReportCallback(
                        device.deviceHandle.device, device.inputReportBuffer, CFIndex.of(device.maxInputReportLength),
                        this::onReportCallback, object_context);
            }

            IOKitLib.INSTANCE.IOHIDDeviceRegisterRemovalCallback(device.deviceHandle.device, this::onDeviceRemovalCallback, object_context);

            // Start the read thread
            device.thread = new Thread(() -> {
logger.finest("here50.0: thread start");

                // Move the device's run loop to this thread.
                IOKitLib.INSTANCE.IOHIDDeviceScheduleWithRunLoop(device.deviceHandle.device, CFLib.INSTANCE.CFRunLoopGetCurrent(), device.runLoopMode);

                // Create the RunLoopSource which is used to signal the
                // event loop to stop when hid_close() is called.
                CFLib.CFRunLoopSourceContext.ByReference ctx = new CFLib.CFRunLoopSourceContext.ByReference();
                ctx.version = CFIndex.of(0);
                ctx.info = UserObjectContext.createContext(device).getPointer();
                ctx.perform = this::onSignalCallback;
                device.source = CFLib.INSTANCE.CFRunLoopSourceCreate(CFAllocator.kCFAllocatorDefault, CFIndex.of(0) /* order */, ctx);
                CFLib.INSTANCE.CFRunLoopAddSource(CFLib.INSTANCE.CFRunLoopGetCurrent(), device.source, device.runLoopMode);

                // Store off the Run Loop so it can be stopped from hid_close()
                // and on device disconnection.
                device.runLoop = CFLib.INSTANCE.CFRunLoopGetCurrent();

                // Notify the main thread that the read thread is up and running.
logger.finest("here50.4: notify barrier -1");
                device.barrier.waitAndSync();

                // Run the Event Loop. CFRunLoopRunInMode() will dispatch HID input
                // reports into the hid_report_callback().
                int code;
logger.finest("here50.5: dev.shutdownThread: " + !device.shutdownThread + ", !dev.disconnected: " + !device.disconnected);
                while (!device.shutdownThread && !device.disconnected) {
                    code = CFLib.INSTANCE.CFRunLoopRunInMode(device.runLoopMode, 1000 /* sec */, false);
                    // Return if the device has been disconnected
                    if (code == CFLib.kCFRunLoopRunFinished || code == CFLib.kCFRunLoopRunStopped) {
                        device.disconnected = true;
logger.finest("here50.6: dev.disconnected: " + device.disconnected + " cause run loop: " + code);
                        break;
                    }

                    // Break if The Run Loop returns Finished or Stopped.
                    if (code != CFLib.kCFRunLoopRunTimedOut && code != CFLib.kCFRunLoopRunHandledSource) {
                        // There was some kind of error. Setting
                        // shutdown seems to make sense, but
                        // there may be something else more appropriate
logger.finest("here50.7: dev.disconnected: " + device.disconnected);
                        device.shutdownThread = true;
                        break;
                    }
                }

                // Wait here until hid_close() is called and makes it past
                // the call to CFRunLoopWakeUp(). This thread still needs to
                // be valid when that function is called on the other thread.
logger.finest("here50.8: notify shutdownBarrier -1");
                device.shutdownBarrier.waitAndSync();
logger.finest("here50.9: thread done");
            }, str);
            device.thread.start();

            // Wait here for the read thread to be initialized.
            device.barrier.waitAndSync();

            IOKitLib.INSTANCE.IOObjectRelease(entry);

            device.closer = devices::remove;
            devices.add(device);
logger.finest("here00.3: devices: +: " + device + " / " + devices.size());
            return device;
        } catch (IOException e) {
logger.log(Level.SEVERE, e.toString(), e);
            if (device.deviceHandle != null)
                CFLib.INSTANCE.CFRelease(device.deviceHandle.device);

            if (entry != null)
                IOKitLib.INSTANCE.IOObjectRelease(entry);

            throw e;
        }
    }
}
