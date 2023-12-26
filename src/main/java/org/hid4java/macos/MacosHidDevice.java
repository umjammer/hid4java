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
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import org.hid4java.HidDevice;
import org.hid4java.HidException;
import org.hid4java.InputReportEvent;
import org.hid4java.NativeHidDevice;
import org.hid4java.SyncPoint;
import vavix.rococoa.corefoundation.CFAllocator;
import vavix.rococoa.corefoundation.CFIndex;
import vavix.rococoa.corefoundation.CFLib;
import vavix.rococoa.corefoundation.CFRunLoop;
import vavix.rococoa.corefoundation.CFString;
import vavix.rococoa.iokit.IOKitLib;

import static org.hid4java.HidDevice.logTraffic;
import static vavix.rococoa.corefoundation.CFLib.kCFRunLoopDefaultMode;
import static vavix.rococoa.iokit.IOKitLib.MACH_PORT_NULL;
import static vavix.rococoa.iokit.IOKitLib.kIOHIDOptionsTypeSeizeDevice;
import static vavix.rococoa.iokit.IOKitLib.kIOHIDReportTypeFeature;
import static vavix.rococoa.iokit.IOKitLib.kIOHIDReportTypeInput;
import static vavix.rococoa.iokit.IOKitLib.kIOReturnSuccess;


/**
 * JNA library interface to act as the proxy for the underlying native library
 * This approach removes the need for any JNI or native code
 *
 * @since 0.1.0
 */
public class MacosHidDevice implements NativeHidDevice {

    private static final Logger logger = Logger.getLogger(MacosHidDevice.class.getName());

    private IOHIDDevice deviceHandle;
    private int /* IOOptionBits */ openOptions;
    private boolean disconnected;
    private CFString runLoopMode;
    private CFRunLoop runLoop;
    private Pointer /* CFRunLoopSourceRef */ source;
    private byte[] inputData;
    private Memory inputReportBuffer;
    private int maxInputReportLength;
    HidDevice.Info deviceInfo;

    private Thread thread;
    /** Ensures correct startup sequence */
    private final SyncPoint barrier;
    /** Ensures correct shutdown sequence */
    private final SyncPoint shutdownBarrier;
    private boolean shutdownThread;

    Consumer<String> closer;

    /**
     * Initialise the HID API library. Should always be called before using any other API calls.
     */
    public MacosHidDevice(int deviceOpenOptions) {
        this.openOptions = deviceOpenOptions;
        this.disconnected = false;
        this.shutdownThread = false;

        // Thread objects
        this.barrier = new SyncPoint(2);
        this.shutdownBarrier = new SyncPoint(2);
    }

    /** Stop the Run Loop for this device */
    private static void onDeviceRemovalCallback(Pointer context, int /* IOReturn */ result, Pointer sender) {
        MacosHidDevice dev = (MacosHidDevice) UserObjectContext.get(context);
        if (dev == null) {
logger.fine("here5.1: dev is null");
            return;
        }
logger.fine("here5.2: device_removal_callback: dev: " + dev.deviceInfo.product);

        dev.disconnected = true;
        dev.close();
    }

    /**
     * This gets called when the read_thread's run loop gets signaled by
     * {@link MacosHidDevice#close()}, and serves to stop the read_thread's run loop.
     */
    private static void onSignalCallback(Pointer context) {
        MacosHidDevice dev = (MacosHidDevice) UserObjectContext.get(context);
        if (dev == null) {
            logger.fine("here3.1: dev is null");
            return;
        }
logger.finer("here3.2: signal_callback: dev: " + dev.deviceInfo.product);

        CFLib.INSTANCE.CFRunLoopStop(dev.runLoop); // TODO CFRunLoopGetCurrent()
logger.finest("here3.3: stop run loop: @" + dev.runLoop.hashCode());
    }

    /**
     * The Run Loop calls this function for each input report received.
     * This function puts the data into a linked list to be picked up by
     * {@link org.hid4java.InputReportListener#onInputReport(InputReportEvent)}.
     *
     * @see IOKitLib.IOHIDReportCallback
     */
    private static void onReportCallback(Pointer context, int /* IOReturn */ result, Pointer sender, int /* IOHIDReportType */ report_type, int report_id, Pointer report, CFIndex reportLength) {
        MacosHidDevice dev = (MacosHidDevice) UserObjectContext.get(context);
        if (dev == null) {
logger.fine("here4.1: dev is null: " + UserObjectContext.objectIDMaster);
            return;
        }
logger.finest("here4.2: report_callback: dev: " + dev.deviceInfo.product);

        // Make a new Input Report object
        int length = reportLength.intValue();
        report.read(0, dev.inputData, 0, length);

        // Signal a waiting thread that there is data.
        dev.fireOnInputReport(new InputReportEvent(dev, report_id, dev.inputData, length));
logger.finest("here4.3: report: " + length + ", " + Thread.currentThread());
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
        logger.finer("here80.0: path: " + path.substring(10));
        if (path.startsWith("DevSrvsID:")) {
            long entryId = Long.parseLong(path.substring(10));
            logger.finer("here80.1: " + entryId);
            return IOKitLib.INSTANCE.IOServiceGetMatchingService(/* mach_port_t */ Pointer.NULL, IOKitLib.INSTANCE.IORegistryEntryIDMatching(entryId).asDict());
        } else {
            // Fallback to older format of the path
            ByteBuffer bb = ByteBuffer.wrap(path.getBytes());
            logger.finer("here80.2");
            return IOKitLib.INSTANCE.IORegistryEntryFromPath(/* mach_port_t */ Pointer.NULL, bb);
        }
    }

    private void internalOpen() throws IOException {
        if (deviceHandle != null) {
            return;
        }

        Pointer/* io_registry_entry_t */ entry = null;
        try {
            // Get the IORegistry entry for the given path
            entry = openServiceRegistryFromPath(this.deviceInfo.path);
            if (entry == MACH_PORT_NULL) {
                // Path wasn't valid (maybe device was removed?)
                throw new IOException("create: device mach entry not found with the given path: " + this.deviceInfo.path);
            }
            logger.finer("here00.2: entry: " + entry + ", openOptions: " + this.openOptions);

            // Create an IOHIDDevice for the entry
            Pointer /* IOHIDDevice */ deviceHandle = IOKitLib.INSTANCE.IOHIDDeviceCreate(CFAllocator.kCFAllocatorDefault, entry);
            if (deviceHandle == null) {
                // Error creating the HID device
                throw new IOException("create: failed to create IOHIDDevice from the mach entry");
            }

            // Open the IOHIDDevice
            int /* IOReturn */ ret = IOKitLib.INSTANCE.IOHIDDeviceOpen(deviceHandle, this.openOptions);
            if (ret != kIOReturnSuccess) {
                logger.finer("here00.3: " + this.deviceInfo.path);
                throw new IOException(String.format("create: failed to open IOHIDDevice from mach entry: (0x%08X)", ret));
            }

            this.deviceHandle = new IOHIDDevice(deviceHandle);

            // Create the buffers for receiving data
            this.maxInputReportLength = this.deviceHandle.get_int_property(IOKitLib.kIOHIDMaxInputReportSizeKey);
            if (this.maxInputReportLength > 0) {
                this.inputData = new byte[this.maxInputReportLength];
                this.inputReportBuffer = new Memory(this.maxInputReportLength);
            }

            IOKitLib.INSTANCE.IOObjectRelease(entry);


        } catch (Exception e) {
            logger.log(Level.SEVERE, e.toString(), e);
            if (this.deviceHandle != null)
                CFLib.INSTANCE.CFRelease(this.deviceHandle.device);

            if (entry != null && entry != MACH_PORT_NULL)
                IOKitLib.INSTANCE.IOObjectRelease(entry);

            throw e;
        }
    }

    @Override
    public void open() throws IOException {
        internalOpen();

        // Create the Run Loop Mode for this device
        // printing the reference seems to work.
        String str = String.format("HIDAPI_%x", Pointer.nativeValue(this.deviceHandle.device));
        this.runLoopMode = CFLib.INSTANCE.CFStringCreateWithCString(null, str.getBytes(StandardCharsets.US_ASCII), CFLib.kCFStringEncodingASCII);
logger.finer("here00.2: str: " + str + ", " + this.runLoopMode.getString());

        // Attach the device to a Run Loop
        UserObjectContext.ByReference object_context = UserObjectContext.create(this);
        if (this.maxInputReportLength > 0) {
            IOKitLib.INSTANCE.IOHIDDeviceRegisterInputReportCallback(
                    this.deviceHandle.device, this.inputReportBuffer, CFIndex.of(this.maxInputReportLength),
                    MacosHidDevice::onReportCallback, object_context);
logger.finer("here00.3: start report");
        }
        IOKitLib.INSTANCE.IOHIDDeviceRegisterRemovalCallback(this.deviceHandle.device, MacosHidDevice::onDeviceRemovalCallback, object_context);

        // Start the read thread
        this.thread = new Thread(() -> {
logger.finest("here50.0: thread start");

            // Move the device's run loop to this thread.
            IOKitLib.INSTANCE.IOHIDDeviceScheduleWithRunLoop(this.deviceHandle.device, CFLib.INSTANCE.CFRunLoopGetCurrent(), this.runLoopMode);

            // Create the RunLoopSource which is used to signal the
            // event loop to stop when hid_close() is called.
            CFLib.CFRunLoopSourceContext.ByReference ctx = new CFLib.CFRunLoopSourceContext.ByReference();
            ctx.version = CFIndex.of(0);
            ctx.info = object_context.getPointer();
            ctx.perform = MacosHidDevice::onSignalCallback;
            this.source = CFLib.INSTANCE.CFRunLoopSourceCreate(CFAllocator.kCFAllocatorDefault, CFIndex.of(0) /* order */, ctx);
            CFLib.INSTANCE.CFRunLoopAddSource(CFLib.INSTANCE.CFRunLoopGetCurrent(), this.source, this.runLoopMode);

            // Store off the Run Loop so it can be stopped from hid_close()
            // and on device disconnection.
            this.runLoop = CFLib.INSTANCE.CFRunLoopGetCurrent();

            // Notify the main thread that the read thread is up and running.
logger.finest("here50.1: notify barrier -1");
            this.barrier.waitAndSync();

            // Run the Event Loop. CFRunLoopRunInMode() will dispatch HID input
            // reports into the hid_report_callback().
            int code;
logger.finer("here50.2: dev.shutdownThread: " + !this.shutdownThread + ", !dev.disconnected: " + !this.disconnected);
            while (!this.shutdownThread && !this.disconnected) {
                code = CFLib.INSTANCE.CFRunLoopRunInMode(this.runLoopMode, 1/* sec */, false);
                // Return if the device has been disconnected
                if (code == CFLib.kCFRunLoopRunFinished || code == CFLib.kCFRunLoopRunStopped) {
                    this.disconnected = true;
logger.finer("here50.3: dev.disconnected: " + this.disconnected + " cause run loop: " + code);
                    break;
                }

                // Break if The Run Loop returns Finished or Stopped.
                if (code != CFLib.kCFRunLoopRunTimedOut && code != CFLib.kCFRunLoopRunHandledSource) {
                    // There was some kind of error. Setting
                    // shutdown seems to make sense, but
                    // there may be something else more appropriate
logger.finer("here50.4: dev.disconnected: " + this.disconnected);
                    this.shutdownThread = true;
                    break;
                }
            }

            // Wait here until hid_close() is called and makes it past
            // the call to CFRunLoopWakeUp(). This thread still needs to
            // be valid when that function is called on the other thread.
logger.finer("here50.5: notify shutdownBarrier -1");
            this.shutdownBarrier.waitAndSync();
logger.finer("here50.6: thread done");
        }, str);
        this.thread.start();

        // Wait here for the read thread to be initialized.
        this.barrier.waitAndSync();
    }

    @Override
    public void close() {
logger.finer("here20.0: " + deviceInfo.path);

        // Disconnect the report callback before close.
        // See comment below.
        if (MacosHidDeviceManager.is_macos_10_10_or_greater || !this.disconnected) {

logger.finer("here20.1: removal callback null, unschedule run loop start");
            IOKitLib.INSTANCE.IOHIDDeviceRegisterInputReportCallback(
                    this.deviceHandle.device, this.inputReportBuffer, CFIndex.of(this.maxInputReportLength),
                    null, null);
            IOKitLib.INSTANCE.IOHIDDeviceRegisterRemovalCallback(this.deviceHandle.device, null, null);
            IOKitLib.INSTANCE.IOHIDDeviceUnscheduleFromRunLoop(this.deviceHandle.device, this.runLoop, this.runLoopMode);
            IOKitLib.INSTANCE.IOHIDDeviceScheduleWithRunLoop(this.deviceHandle.device, CFLib.INSTANCE.CFRunLoopGetMain(), kCFRunLoopDefaultMode);
logger.finer("here20.2: removal callback null, unschedule run loop done");
        }

        // Cause read_thread() to stop.
        this.shutdownThread = true;

        // Wake up the run thread's event loop so that the thread can close.
        CFLib.INSTANCE.CFRunLoopSourceSignal(this.source);
        CFLib.INSTANCE.CFRunLoopWakeUp(this.runLoop);
logger.finest("here20.3: wake up run loop: @" + this.runLoop.hashCode()); // TODO <- not here, until 20.2

        // Notify the read thread that it can shut down now.
logger.finer("here20.4: " + Thread.currentThread() + ", " + this.thread);
logger.finest("here20.5: notify shutdownBarrier -1");
        this.shutdownBarrier.waitAndSync();

        // Wait for read_thread() to end.
logger.finer("here20.6: join...: " + this.thread);
        try { this.thread.join(); } catch (InterruptedException ignore) {}

        // Close the OS handle to the device, but only if it's not
        // been unplugged. If it's been unplugged, then calling
        // IOHIDDeviceClose() will crash.
        //
        // UPD: The crash part was true in/until some version of macOS.
        // Starting with macOS 10.15, there is an opposite effect in some environments:
        // crash happenes if IOHIDDeviceClose() is not called.
        // Not leaking a resource in all tested environments.

        if (MacosHidDeviceManager.is_macos_10_10_or_greater || !this.disconnected) {
            IOKitLib.INSTANCE.IOHIDDeviceClose(this.deviceHandle.device, kIOHIDOptionsTypeSeizeDevice);
logger.finer("here20.7: native device close: @" + this.deviceHandle.device.hashCode());
        }

        if (this.runLoopMode != null)
            CFLib.INSTANCE.CFRelease(this.runLoopMode);
        if (this.source != null)
            CFLib.INSTANCE.CFRelease(this.source);

        CFLib.INSTANCE.CFRelease(this.deviceHandle.device);
logger.finest("here20.8: native device release");

        closer.accept(this.deviceInfo.path);
logger.finer("here20.9: close done");
    }

    /** */
    private int setReport(int /* IOHIDReportType */ type, byte[] data, int length) throws HidException {
        int dataP = 0; // data
        int length_to_send = length;
        int /* IOReturn */ res;
        int report_id;

        if (data == null || (length == 0)) {
            throw new HidException("data is null or length is zero: " + this.deviceInfo.path);
        }

        report_id = data[0] & 0xff;

        if (report_id == 0x0) {
            // Not using numbered Reports.
            // Don't send the report number.
            dataP = 1;
            length_to_send = length - 1;
        }

        // Avoid crash if the device has been unplugged.
        if (this.disconnected) {
            throw new HidException("Device is disconnected: " + this.deviceInfo.path);
        }

        byte[] data_to_send = new byte[length_to_send];
        System.arraycopy(data, dataP, data_to_send, 0, data_to_send.length);
        res = IOKitLib.INSTANCE.IOHIDDeviceSetReport(this.deviceHandle.device,
                type,
                CFIndex.of(report_id),
                data_to_send, CFIndex.of(length_to_send));

        if (res != kIOReturnSuccess) {
            throw new HidException(String.format("IOHIDDeviceSetReport failed: (0x%08X): %s", res, this.deviceInfo.path));
        }

        return length;
    }

    /** */
    private int getReport(int /* IOHIDReportType */ type, byte[] data, int length) throws HidException {
        int dataP = 0;
        int report_length = length;
        int /* IOReturn */ res;
        int report_id = data[0] & 0xff;

        if (report_id == 0x0) {
            // Not using numbered Reports.
            // Don't send the report number.
            dataP = 1;
            report_length = length - 1;
        }

        // Avoid crash if the device has been unplugged.
        if (this.disconnected) {
            throw new HidException("Device is disconnected: " + this.deviceInfo.path);
        }

        byte[] report = new byte[report_length];
        System.arraycopy(data, dataP, report, 0, report_length);
        CFIndex[] rl = new CFIndex[] { CFIndex.of(report_length) };
        res = IOKitLib.INSTANCE.IOHIDDeviceGetReport(this.deviceHandle.device,
                type,
                CFIndex.of(report_id),
                report, rl);
        report_length = rl[0].intValue();

        if (res != kIOReturnSuccess) {
            throw new HidException(String.format("IOHIDDeviceGetReport failed: (0x%08X): %s", res, this.deviceInfo.path));
        }

        if (report_id == 0x0) { // 0 report number still present at the beginning
            report_length++;
        }

        return report_length;
    }

    @Override
    public int write(byte[] data, int length, byte reportId) throws IOException {
        internalOpen();

        // Fail fast
        if (data == null) {
            throw new IllegalArgumentException("data is null");
        }

        // Precondition checks
        if (data.length < length) {
            length = data.length;
        }

        byte[] report;

        // Put report ID into position 0 and fill out buffer
        report = new byte[length + 1];
        report[0] = reportId;
        if (length >= 1) {
            System.arraycopy(data, 0, report, 1, length);
        }

        logTraffic(report, true);

        return setReport(IOKitLib.kIOHIDReportTypeOutput, report, report.length);
    }

    @Override
    public int getFeatureReport(byte[] data, byte reportId) throws IOException {
        internalOpen();

        // Create a large buffer
        byte[] report = new byte[data.length + 1];
        report[0] = reportId;
        int res = getReport(kIOHIDReportTypeFeature, report, data.length + 1);

        // Avoid index out of bounds exception
        System.arraycopy(report, 1, data, 0, Math.min(res, data.length));

        logTraffic(report, false);

        return res;
    }

    @Override
    public int sendFeatureReport(byte[] data, byte reportId) throws IOException {
        if (data == null) {
            throw new IllegalArgumentException("data is null");
        }

        internalOpen();

        byte[] report = new byte[data.length + 1];
        report[0] = reportId;

        System.arraycopy(data, 0, report, 1, data.length);

        logTraffic(report, true);

        return setReport(kIOHIDReportTypeFeature, report, report.length);
    }

    @Override
    public int getReportDescriptor(byte[] report) throws IOException {
        internalOpen();

        return this.deviceHandle.hid_get_report_descriptor(report, report.length);
    }

    @Override
    public int getInputReport(byte[] data, byte reportId) throws IOException {
        internalOpen();

        byte[] report = new byte[data.length + 1];
        report[0] = reportId;
        int res = getReport(kIOHIDReportTypeInput, report, data.length + 1);

        System.arraycopy(report, 1, data, 0, Math.min(res, data.length));

        logTraffic(report, false);

        return res;
    }
}
