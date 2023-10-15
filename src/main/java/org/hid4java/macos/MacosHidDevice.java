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
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Logger;

import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import org.hid4java.HidDevice;
import org.hid4java.HidException;
import org.hid4java.InputReportEvent;
import org.hid4java.InputReportListener;
import org.hid4java.NativeHidDevice;
import org.hid4java.SyncPoint;
import vavix.rococoa.corefoundation.CFIndex;
import vavix.rococoa.corefoundation.CFLib;
import vavix.rococoa.corefoundation.CFRunLoop;
import vavix.rococoa.corefoundation.CFString;
import vavix.rococoa.iokit.IOKitLib;

import static org.hid4java.HidDevice.logTraffic;
import static vavix.rococoa.corefoundation.CFLib.kCFRunLoopDefaultMode;
import static vavix.rococoa.iokit.IOKitLib.kIOReturnSuccess;


/**
 * JNA library interface to act as the proxy for the underlying native library
 * This approach removes the need for any JNI or native code
 *
 * @since 0.1.0
 */
public class MacosHidDevice implements NativeHidDevice {

    private static final Logger logger = Logger.getLogger(MacosHidDevice.class.getName());

    /**
     * Default length for wide string buffer
     */
    private static final int WSTR_LEN = 512;

    IOHIDDevice deviceHandle;
    int/*IOOptionBits*/ openOptions;
    boolean disconnected;
    CFString runLoopMode;
    CFRunLoop runLoop;
    Pointer /*CFRunLoopSourceRef*/ source;
    byte[] inputData;
    Memory inputReportBuffer;
    int maxInputReportLength;
    HidDevice.Info deviceInfo;

    Thread/*pthread_t*/ thread;
    /** Ensures correct startup sequence */
    SyncPoint barrier;
    /** Ensures correct shutdown sequence */
    SyncPoint shutdownBarrier;
    boolean shutdownThread;

    Consumer<MacosHidDevice> closer;

    /**
     * Initialise the HID API library. Should always be called before using any other API calls.
     */
    public MacosHidDevice(int device_open_options) {
        this.openOptions = device_open_options;
        this.disconnected = false;
        this.shutdownThread = false;

        // Thread objects
        this.barrier = new SyncPoint(2);
        this.shutdownBarrier = new SyncPoint(2);
    }

    /** input report listeners */
    private final List<InputReportListener> inputReportListeners = new ArrayList<>();

    @Override
    public synchronized void addReportInputListener(InputReportListener listener) {
        inputReportListeners.add(listener);
    }

    /** fires an input report */
    protected synchronized void fireInputReport(InputReportEvent event) {
        inputReportListeners.forEach(l -> l.onInputReport(event));
    }

    @Override
    public void close() {
logger.fine("here20.0:");

        // Disconnect the report callback before close.
        // See comment below.
        if (MacosHidDeviceManager.is_macos_10_10_or_greater || !this.disconnected) {

            UserObjectContext.ByReference object_context = UserObjectContext.createContext(this);
            IOKitLib.INSTANCE.IOHIDDeviceRegisterInputReportCallback(
                    this.deviceHandle.device, this.inputReportBuffer, CFIndex.of(this.maxInputReportLength),
                    null, object_context);
            IOKitLib.INSTANCE.IOHIDDeviceRegisterRemovalCallback(this.deviceHandle.device, null, object_context);
            IOKitLib.INSTANCE.IOHIDDeviceUnscheduleFromRunLoop(this.deviceHandle.device, this.runLoop, this.runLoopMode);
            IOKitLib.INSTANCE.IOHIDDeviceScheduleWithRunLoop(this.deviceHandle.device, CFLib.INSTANCE.CFRunLoopGetMain(), kCFRunLoopDefaultMode);
logger.finest("here20.1: removal callback null, unschedule run loop");
        }

        // Cause read_thread() to stop.
        this.shutdownThread = true;

        // Wake up the run thread's event loop so that the thread can exit.
        CFLib.INSTANCE.CFRunLoopSourceSignal(this.source);
        CFLib.INSTANCE.CFRunLoopWakeUp(this.runLoop);
logger.finest("here20.2: wake up run loop: @" + this.runLoop.hashCode());

        // Notify the read thread that it can shut down now.
logger.finest("here20.3: " + Thread.currentThread() + ", " + this.thread);
        if (Thread.currentThread() != this.thread) {
logger.finest("here20.4: notify shutdownBarrier -1");
            this.shutdownBarrier.waitAndSync();

            // Wait for read_thread() to end.
logger.finest("here20.5: join...");
            try {
                this.thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // Close the OS handle to the device, but only if it's not
        // been unplugged. If it's been unplugged, then calling
        // IOHIDDeviceClose() will crash.
        //
        // UPD: The crash part was true in/until some version of macOS.
        // Starting with macOS 10.15, there is an opposite effect in some environments:
        // crash happenes if IOHIDDeviceClose() is not called.
        // Not leaking a resource in all tested environments.

        if (MacosHidDeviceManager.is_macos_10_10_or_greater || !this.disconnected) {
            IOKitLib.INSTANCE.IOHIDDeviceClose(this.deviceHandle.device, this.openOptions);
logger.finest("here20.6: native device close: @" + this.deviceHandle.device.hashCode());
        }

        CFLib.INSTANCE.CFRelease(this.deviceHandle.device);
logger.finest("here20.7: native device release");

        closer.accept(this);
logger.finest("here20.8: close done");
    }

    /** */
    private int set_report(int/*IOHIDReportType*/ type, byte[] data, int length) throws HidException {
        int dataP = 0; // data
        int length_to_send = length;
        int/*IOReturn*/ res;
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
    private int get_report(int /* IOHIDReportType */ type, byte[] data, int length) throws HidException {
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
        System.arraycopy(data, dataP, report, 0, report.length);
        res = IOKitLib.INSTANCE.IOHIDDeviceGetReport(this.deviceHandle.device,
                type,
                CFIndex.of(report_id),
                report, CFIndex.of(report_length));

        if (res != kIOReturnSuccess) {
            throw new HidException(String.format("IOHIDDeviceGetReport failed: (0x%08X): %s", res, this.deviceInfo.path));
        }

        if (report_id == 0x0) { // 0 report number still present at the beginning
            report_length++;
        }

        return report_length;
    }

    @Override
    public int write(byte[] data, int len, byte reportId) throws IOException {

        // Fail fast
        if (data == null) {
            throw new IllegalArgumentException("data is null");
        }

        // Precondition checks
        if (data.length < len) {
            len = data.length;
        }

        byte[] report;

        // Put report ID into position 0 and fill out buffer
        report = new byte[len + 1];
        report[0] = reportId;
        if (len >= 1) {
            System.arraycopy(data, 0, report, 1, len);
        }

        logTraffic(report, true);

        return set_report(IOKitLib.kIOHIDReportTypeOutput, report, report.length);
    }

    @Override
    public int getFeatureReport(byte[] data, int length, byte reportId) throws IOException {
        // Create a large buffer
        byte[] report = new byte[WSTR_LEN];
        report[0] = reportId;
        int res = get_report(IOKitLib.kIOHIDReportTypeFeature, report, data.length + 1);

        if (res == -1) {
            return res;
        }

        // Avoid index out of bounds exception
        System.arraycopy(report, 1, data, 0, Math.min(res, data.length));

        logTraffic(report, false);

        return res;
    }

    @Override
    public int sendFeatureReport(byte[] data, int length, byte reportId) throws IOException {
        if (data == null) {
            throw new IllegalArgumentException("data is null");
        }

        byte[] report = new byte[data.length + 1];
        report[0] = reportId;

        System.arraycopy(data, 0, report, 1, data.length);

        logTraffic(report, true);

        return set_report(IOKitLib.kIOHIDReportTypeFeature, report, report.length);
    }
}