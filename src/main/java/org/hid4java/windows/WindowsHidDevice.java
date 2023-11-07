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
import static org.hid4java.windows.DescriptorReconstructor.hid_winapi_descriptor_reconstruct_pp_data;


/**
 * WindowsHidDevice.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2023-10-31 nsano initial version <br>
 */
public class WindowsHidDevice implements NativeHidDevice {

    HANDLE device_handle;
    boolean blocking;
    short output_report_length;
    byte[] write_buf;
    int input_report_length;
    short feature_report_length;
    byte[] feature_buf;
    boolean read_pending;
    byte[] read_buf;
    OVERLAPPED ol;
    OVERLAPPED write_ol;
    HidDevice.Info device_info;

    private ScheduledExecutorService ses = Executors.newSingleThreadScheduledExecutor();

    WindowsHidDevice() {
        this.device_handle = INVALID_HANDLE_VALUE;
        this.blocking = true;
        this.output_report_length = 0;
        this.write_buf = null;
        this.input_report_length = 0;
        this.feature_report_length = 0;
        this.feature_buf = null;
        this.read_pending = false;
        this.read_buf = null;
        this.ol = new OVERLAPPED();
        this.ol.hEvent = Kernel32.INSTANCE.CreateEvent(null, false, false /*initial state f=nonsignaled*/, null);
        this.write_ol = new OVERLAPPED();
        this.write_ol.hEvent = Kernel32.INSTANCE.CreateEvent(null, false, false /*initial state f=nonsignaled*/, null);
        this.device_info = null;
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

        Kernel32Ex.INSTANCE.CancelIo(this.device_handle);
    }

    @Override
    public int write(byte[] data, int len, byte reportId) throws IOException {
        IntByReference bytes_written = new IntByReference();
        int function_result = -1;
        boolean res;
        boolean overlapped = false;

        byte[] buf;

        if (data == null || len == 0) {
            throw new IllegalArgumentException("Zero buffer/length");
        }

        // Make sure the right number of bytes are passed to WriteFile. Windows
        // expects the number of bytes which are in the _longest_ report (plus
        // one for the report number) bytes even if the data is a report
        // which is shorter than that. Windows gives us this value in
        // caps.OutputReportByteLength. If a user passes in fewer bytes than this,
        // use cached temporary buffer which is the proper size.
        if (len >= this.output_report_length) {
            // The user passed the right number of bytes. Use the buffer as-is.
            buf = new byte[data.length];
        } else {
            if (this.write_buf == null)
                this.write_buf = new byte[this.output_report_length];
            buf = this.write_buf;
            System.arraycopy(data, 0, buf, 0, len);
            Arrays.fill(buf, len, this.output_report_length, (byte) 0);
            len = this.output_report_length;
        }

        res = Kernel32.INSTANCE.WriteFile(this.device_handle, buf, len, null, this.write_ol);
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
            int result = Kernel32.INSTANCE.WaitForSingleObject(this.write_ol.hEvent, 1000);
            if (result != Kernel32.WAIT_OBJECT_0) {
                // There was a Timeout.
                throw new IOException("hid_write/WaitForSingleObject");
            }

            /* Get the result. */
            res = Kernel32Ex.INSTANCE.GetOverlappedResult(this.device_handle, this.write_ol.getPointer(), bytes_written, /* wait */ false);
            if (res) {
                function_result = bytes_written.getValue();
            }
            else {
                /* The Write operation failed. */
                throw new IOException("hid_write/GetOverlappedResult");
            }
        }

        return function_result;
    }

    int read(byte[] data, int length) throws IOException {
        IntByReference bytes_read = new IntByReference();
        int copy_len = 0;
        boolean res = false;
        boolean overlapped = false;

        if (data == null || length == 0) {
            throw new IllegalArgumentException("Zero buffer/length");
        }

        // Copy the handle for convenience.
        HANDLE ev = this.ol.hEvent;

        if (!this.read_pending) {
            // Start an Overlapped I/O read.
            this.read_pending = true;
            Arrays.fill(this.read_buf, 0, this.input_report_length, (byte) 0);
            Kernel32Ex.INSTANCE.ResetEvent(ev);
            res = Kernel32Ex.INSTANCE.ReadFile(this.device_handle, this.read_buf, this.input_report_length, bytes_read, this.ol);

            if (!res) {
                if (Native.getLastError() != ERROR_IO_PENDING) {
                    // ReadFile() has failed.
				    // Clean up and return error.
                    Kernel32Ex.INSTANCE.CancelIo(this.device_handle);
                    this.read_pending = false;
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
            res = Kernel32Ex.INSTANCE.GetOverlappedResult(this.device_handle, this.ol.getPointer(), bytes_read, true /* wait */);
        }
        // Set pending back to false, even if GetOverlappedResult() returned error.
        this.read_pending = false;

        if (res && bytes_read.getValue() > 0) {
            if (this.read_buf[0] == 0x0) {
                // If report numbers aren't being used, but Windows sticks a report
                // number (0x0) on the beginning of the report anyway. To make this
                // work like the other platforms, and to make it work more like the
                // HID spec, we'll skip over this byte. */
                bytes_read.setValue(bytes_read.getValue() - 1);
                copy_len = Math.min(length, bytes_read.getValue());
                System.arraycopy(this.read_buf, 1, data, 0, copy_len);
            }
            else {
                /* Copy the whole buffer, report number and all. */
                copy_len = Math.min(length, bytes_read.getValue());
                System.arraycopy(this.read_buf, 0, data, 0, copy_len);
            }
        }
        if (!res) {
            throw new IOException("hid_read_timeout/GetOverlappedResult");
        }

        return copy_len;
    }

    @Override
    public int getFeatureReport(byte[] data, byte reportId) throws IOException {
        // We could use HidD_GetFeature() instead, but it doesn't give us an actual length, unfortunately
        return hid_get_report(IOCTL_HID_GET_FEATURE, data, data.length);
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
        int length_to_send;
        if (data.length >= this.feature_report_length) {
            buf = new byte[data.length];
            length_to_send = data.length;
        } else {
            if (this.feature_buf == null)
                this.feature_buf = new byte[this.feature_report_length];
            buf = this.feature_buf;
            System.arraycopy(data, 0, buf, 0, data.length);
            Arrays.fill(buf, data.length, this.feature_report_length, (byte) 0);
            length_to_send = this.feature_report_length;
        }

        boolean res = Hid.INSTANCE.HidD_SetFeature(this.device_handle, buf, length_to_send);
        if (!res) {
            throw new IOException("HidD_SetFeature");
        }

        return data.length;
    }

    private int hid_get_report(int report_type, byte[] data, int length) throws IOException {

        if (data == null || length == 0) {
            throw new IllegalArgumentException("Zero buffer/length");
        }

        Memory m = new Memory(length);
        m.write(0, data, 0, length);
        IntByReference bytes_returned = new IntByReference();
        OVERLAPPED ol = new OVERLAPPED();
        boolean res = Kernel32.INSTANCE.DeviceIoControl(this.device_handle,
                report_type,
                m, length,
                m, length,
                bytes_returned, ol.getPointer());

        if (!res) {
            if (Native.getLastError() != ERROR_IO_PENDING) {
                // DeviceIoControl() failed. Return error.
                throw new IOException("Get Input/Feature Report DeviceIoControl");
            }
        }

        // Wait here until to write is done. This makes
	    // hid_get_feature_report() synchronous.
        res = Kernel32Ex.INSTANCE.GetOverlappedResult(this.device_handle, ol.getPointer(), bytes_returned, /* wait */ true);
        if (!res) {
            // The operation failed.
            throw new IOException("Get Input/Feature Report GetOverLappedResult");
        }

        // When numbered reports aren't used,
	    // bytes_returned seem to include only what is actually received from the device
	    // (not including the first byte with 0, as an indication "no numbered reports").
        if (data[0] == 0x0) {
            bytes_returned.setValue(bytes_returned.getValue() + 1);
        }

        return bytes_returned.getValue();
    }

    @Override
    public int getReportDescriptor(byte[] report) throws IOException {

        PointerByReference /* PHIDP_PREPARSED_DATA */ pp_data = new PointerByReference();

        // TODO purejavahidapi uses DeviceIoControl that returns raw descriptor, so no need to reconstruct
        if (!Hid.INSTANCE.HidD_GetPreparsedData(this.device_handle, pp_data) || pp_data.getValue() == Pointer.NULL) {
            throw new IOException("HidD_GetPreparsedData");
        }

        int res = hid_winapi_descriptor_reconstruct_pp_data(pp_data.getValue(), report, report.length);

        Hid.INSTANCE.HidD_FreePreparsedData(pp_data.getValue());

        return res;
    }

    @Override
    public int getInputReport(byte[] data, byte reportId) throws IOException {
        // We could use HidD_GetInputReport() instead, but it doesn't give us an actual length, unfortunately
        return hid_get_report(IOCTL_HID_GET_INPUT_REPORT, data, data.length);
    }
}
