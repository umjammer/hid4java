/*
 * HIDAPI - Multi-Platform library for
 * communication with HID devices.
 *
 * Alan Ott
 * Signal 11 Software
 *
 * libusb/hidapi Team
 *
 * Copyright 2022, All Rights Reserved.
 *
 * At the discretion of the user of this library,
 * this software may be licensed under the terms of the
 * GNU General Public License v3, a BSD-Style license, or the
 * original HIDAPI license as outlined in the LICENSE.txt,
 * LICENSE-gpl3.txt, LICENSE-bsd.txt, and LICENSE-orig.txt
 * files located at the root of the source distribution.
 * These files may also be found in the public source
 * code repository located at:
 * https://github.com/libusb/hidapi .
 */

package org.hid4java.windows;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinBase.OVERLAPPED;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import net.java.games.input.windows.WinAPI.Hid;
import net.java.games.input.windows.WinAPI.Kernel32Ex;
import org.hid4java.HidDevice;
import org.hid4java.InputReportEvent;
import org.hid4java.NativeHidDevice;

import static com.sun.jna.platform.win32.WinBase.INVALID_HANDLE_VALUE;
import static com.sun.jna.platform.win32.WinError.ERROR_IO_PENDING;
import static net.java.games.input.windows.WinAPI.IOCTL_HID_GET_FEATURE;
import static net.java.games.input.windows.WinAPI.IOCTL_HID_GET_INPUT_REPORT;
import static org.hid4java.windows.DescriptorReconstructor.hidWinapiDescriptorReconstructPpData;


/**
 * WindowsHidDevice.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2023-10-31 nsano initial version <br>
 */
public class WindowsHidDevice implements NativeHidDevice {

    HANDLE deviceHandle;
    private final boolean blocking;
    short outputReportLength;
    private byte[] writeBuf;
    short featureReportLength;
    int inputReportLength;
    private byte[] featureBuf;
    private boolean readPending;
    byte[] readBuf;
    private OVERLAPPED ol;
    private OVERLAPPED writeOl;
    HidDevice.Info deviceInfo;

    private ScheduledExecutorService ses = Executors.newSingleThreadScheduledExecutor();

    WindowsHidDevice() {
        this.deviceHandle = INVALID_HANDLE_VALUE;
        this.blocking = true;
        this.outputReportLength = 0;
        this.writeBuf = null;
        this.inputReportLength = 0;
        this.featureReportLength = 0;
        this.featureBuf = null;
        this.readPending = false;
        this.readBuf = null;
        this.ol = new OVERLAPPED();
        this.ol.hEvent = Kernel32.INSTANCE.CreateEvent(null, false, false /*initial state f=nonsignaled*/, null);
        this.writeOl = new OVERLAPPED();
        this.writeOl.hEvent = Kernel32.INSTANCE.CreateEvent(null, false, false /*initial state f=nonsignaled*/, null);
        this.deviceInfo = null;
    }

    @Override
    public void open() {
        ses.schedule(() -> {
            try {
                byte[] b = new byte[64]; // TODO reuse
                int r = read(b, b.length);
                fireOnInputReport(new InputReportEvent(this, b[0], b, r));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, 20, TimeUnit.MILLISECONDS); // TODO interval
    }

    @Override
    public void close() {
        ses.shutdownNow();

        Kernel32Ex.INSTANCE.CancelIo(this.deviceHandle);
    }

    @Override
    public int write(byte[] data, int len, byte reportId) throws IOException {
        IntByReference bytesWritten = new IntByReference();
        int functionResult = -1;
        boolean overlapped = false;

        if (data == null || len == 0) {
            throw new IllegalArgumentException("Zero buffer/length");
        }

        // Make sure the right number of bytes are passed to WriteFile. Windows
        // expects the number of bytes which are in the _longest_ report (plus
        // one for the report number) bytes even if the data is a report
        // which is shorter than that. Windows gives us this value in
        // caps.OutputReportByteLength. If a user passes in fewer bytes than this,
        // use cached temporary buffer which is the proper size.
        byte[] buf;
        if (len >= this.outputReportLength) {
            // The user passed the right number of bytes. Use the buffer as-is.
            buf = new byte[data.length];
        } else {
            if (this.writeBuf == null)
                this.writeBuf = new byte[this.outputReportLength];
            buf = this.writeBuf;
            System.arraycopy(data, 0, buf, 0, len);
            Arrays.fill(buf, len, this.outputReportLength, (byte) 0);
            len = this.outputReportLength;
        }

        boolean res = Kernel32.INSTANCE.WriteFile(this.deviceHandle, buf, len, null, this.writeOl);
        if (!res) {
            if (Native.getLastError() != ERROR_IO_PENDING) {
                /* WriteFile() failed. Return error. */
                throw new IOException("WriteFile");
            }
            overlapped = true;
        }

        if (overlapped) {
            // Wait for the transaction to complete. This makes
            // hid_write() synchronous.
            int result = Kernel32.INSTANCE.WaitForSingleObject(this.writeOl.hEvent, 1000);
            if (result != Kernel32.WAIT_OBJECT_0) {
                // There was a Timeout.
                throw new IOException("hid_write/WaitForSingleObject");
            }

            /* Get the result. */
            res = Kernel32Ex.INSTANCE.GetOverlappedResult(this.deviceHandle, this.writeOl.getPointer(), bytesWritten, /* wait */ false);
            if (res) {
                functionResult = bytesWritten.getValue();
            }
            else {
                /* The Write operation failed. */
                throw new IOException("hid_write/GetOverlappedResult");
            }
        }

        return functionResult;
    }

    int read(byte[] data, int length) throws IOException {
        IntByReference bytesRead = new IntByReference();
        int copyLen = 0;
        boolean res = false;
        boolean overlapped = false;

        if (data == null || length == 0) {
            throw new IllegalArgumentException("Zero buffer/length");
        }

        // Copy the handle for convenience.
        HANDLE ev = this.ol.hEvent;

        if (!this.readPending) {
            // Start an Overlapped I/O read.
            this.readPending = true;
            Arrays.fill(this.readBuf, 0, this.inputReportLength, (byte) 0);
            Kernel32Ex.INSTANCE.ResetEvent(ev);
            res = Kernel32Ex.INSTANCE.ReadFile(this.deviceHandle, this.readBuf, this.inputReportLength, bytesRead, this.ol);

            if (!res) {
                if (Native.getLastError() != ERROR_IO_PENDING) {
                    // ReadFile() has failed.
				    // Clean up and return error.
                    Kernel32Ex.INSTANCE.CancelIo(this.deviceHandle);
                    this.readPending = false;
                    throw new IOException("ReadFile");
                }
                overlapped = true;
            }
        }
        else {
            overlapped = true;
        }

        if (overlapped) {

            // Either WaitForSingleObject() told us that ReadFile has completed, or
		    // we are in non-blocking mode. Get the number of bytes read. The actual
		    // data has been copied to the data[] array which was passed to ReadFile().
            res = Kernel32Ex.INSTANCE.GetOverlappedResult(this.deviceHandle, this.ol.getPointer(), bytesRead, true /* wait */);
        }
        // Set pending back to false, even if GetOverlappedResult() returned error.
        this.readPending = false;

        if (res && bytesRead.getValue() > 0) {
            if (this.readBuf[0] == 0x0) {
                // If report numbers aren't being used, but Windows sticks a report
                // number (0x0) on the beginning of the report anyway. To make this
                // work like the other platforms, and to make it work more like the
                // HID spec, we'll skip over this byte. */
                bytesRead.setValue(bytesRead.getValue() - 1);
                copyLen = Math.min(length, bytesRead.getValue());
                System.arraycopy(this.readBuf, 1, data, 0, copyLen);
            }
            else {
                /* Copy the whole buffer, report number and all. */
                copyLen = Math.min(length, bytesRead.getValue());
                System.arraycopy(this.readBuf, 0, data, 0, copyLen);
            }
        }
        if (!res) {
            throw new IOException("hid_read_timeout/GetOverlappedResult");
        }

        return copyLen;
    }

    @Override
    public int getFeatureReport(byte[] data, byte reportId) throws IOException {
        // We could use HidD_GetFeature() instead, but it doesn't give us an actual length, unfortunately
        return hidGetReport(IOCTL_HID_GET_FEATURE, data, data.length);
    }

    @Override
    public int sendFeatureReport(byte[] data, byte reportId) throws IOException {

        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("Zero buffer/length");
        }

        // Windows expects at least caps.FeatureReportByteLength bytes passed
        // to HidD_SetFeature(), even if the report is shorter. Any less sent and
        // the function fails with error ERROR_INVALID_PARAMETER set. Any more
        // and HidD_SetFeature() silently truncates the data sent in the report
        // to caps.FeatureReportByteLength.
        byte[] buf;
        int lengthToSend;
        if (data.length >= this.featureReportLength) {
            buf = new byte[data.length];
            lengthToSend = data.length;
        } else {
            if (this.featureBuf == null)
                this.featureBuf = new byte[this.featureReportLength];
            buf = this.featureBuf;
            System.arraycopy(data, 0, buf, 0, data.length);
            Arrays.fill(buf, data.length, this.featureReportLength, (byte) 0);
            lengthToSend = this.featureReportLength;
        }

        boolean res = Hid.INSTANCE.HidD_SetFeature(this.deviceHandle, buf, lengthToSend);
        if (!res) {
            throw new IOException("HidD_SetFeature");
        }

        return data.length;
    }

    private int hidGetReport(int reportType, byte[] data, int length) throws IOException {

        if (data == null || length == 0) {
            throw new IllegalArgumentException("Zero buffer/length");
        }

        Memory m = new Memory(length);
        m.write(0, data, 0, length);
        IntByReference bytesReturned = new IntByReference();
        OVERLAPPED ol = new OVERLAPPED();
        boolean res = Kernel32.INSTANCE.DeviceIoControl(this.deviceHandle,
                reportType,
                m, length,
                m, length,
                bytesReturned, ol.getPointer());

        if (!res) {
            if (Native.getLastError() != ERROR_IO_PENDING) {
                // DeviceIoControl() failed. Return error.
                throw new IOException("Get Input/Feature Report DeviceIoControl");
            }
        }

        // Wait here until to write is done. This makes
	    // hid_get_feature_report() synchronous.
        res = Kernel32Ex.INSTANCE.GetOverlappedResult(this.deviceHandle, ol.getPointer(), bytesReturned, /* wait */ true);
        if (!res) {
            // The operation failed.
            throw new IOException("Get Input/Feature Report GetOverLappedResult");
        }

        // When numbered reports aren't used,
	    // bytesReturned seem to include only what is actually received from the device
	    // (not including the first byte with 0, as an indication "no numbered reports").
        if (data[0] == 0x0) {
            bytesReturned.setValue(bytesReturned.getValue() + 1);
        }

        return bytesReturned.getValue();
    }

    @Override
    public int getReportDescriptor(byte[] report) throws IOException {

        PointerByReference /* PHIDP_PREPARSED_DATA */ ppData = new PointerByReference();

        // TODO purejavahidapi uses DeviceIoControl that returns raw descriptor, so no need to reconstruct
        if (!Hid.INSTANCE.HidD_GetPreparsedData(this.deviceHandle, ppData) || ppData.getValue() == Pointer.NULL) {
            throw new IOException("HidD_GetPreparsedData");
        }

        int res = hidWinapiDescriptorReconstructPpData(ppData.getValue(), report, report.length);

        Hid.INSTANCE.HidD_FreePreparsedData(ppData.getValue());

        return res;
    }

    @Override
    public int getInputReport(byte[] data, byte reportId) throws IOException {
        // We could use HidD_GetInputReport() instead, but it doesn't give us an actual length, unfortunately
        return hidGetReport(IOCTL_HID_GET_INPUT_REPORT, data, data.length);
    }
}
