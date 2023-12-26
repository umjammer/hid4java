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

package org.hid4java.linux;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.platform.linux.Udev;
import com.sun.jna.ptr.IntByReference;
import net.java.games.input.linux.LinuxIO;
import net.java.games.input.linux.LinuxIO.hidraw_report_descriptor;
import net.java.games.input.linux.LinuxIO.stat;
import org.hid4java.HidDevice;
import org.hid4java.InputReportEvent;
import org.hid4java.NativeHidDevice;

import static com.sun.jna.platform.linux.ErrNo.EAGAIN;
import static com.sun.jna.platform.linux.ErrNo.EINPROGRESS;
import static com.sun.jna.platform.linux.ErrNo.EINVAL;
import static net.java.games.input.linux.LinuxIO.HIDIOCGFEATURE;
import static net.java.games.input.linux.LinuxIO.HIDIOCGINPUT;
import static net.java.games.input.linux.LinuxIO.HIDIOCGRDESC;
import static net.java.games.input.linux.LinuxIO.HIDIOCGRDESCSIZE;
import static net.java.games.input.linux.LinuxIO.HIDIOCSFEATURE;
import static net.java.games.input.linux.LinuxIO._IOC;
import static net.java.games.input.linux.LinuxIO._IOC_READ;
import static net.java.games.input.linux.LinuxIO._IOC_WRITE;
import static net.java.games.input.linux.LinuxIO._IOR;
import static org.hid4java.linux.LinuxHidDeviceManager.create_device_info_for_device;


/**
 * WindowsHidDevice.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2023-10-31 nsano initial version <br>
 */
public class LinuxHidDevice implements NativeHidDevice {

    int deviceHandle;
    HidDevice.Info deviceInfo;

    private final Memory inputReportBuffer = new Memory(64);

    private ScheduledExecutorService ses = Executors.newSingleThreadScheduledExecutor();

    LinuxHidDevice() {
        this.deviceHandle = -1;
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

    int hid_get_manufacturer_string(byte[] string, int maxlen) throws IOException {
        if (string == null || maxlen == 0) {
            throw new IllegalArgumentException("Zero buffer/length");
        }

        HidDevice.Info info = hid_get_device_info();
        if (info == null) {
            // hid_get_device_info will have set an error already
            return -1;
        }

        info.manufacturer = new String(string);

        return 0;
    }

    int hid_get_product_string(byte[] string, int maxlen) throws IOException {
        if (string == null || maxlen == 0) {
            throw new IllegalArgumentException("Zero buffer/length");
        }

        HidDevice.Info info = hid_get_device_info();
        if (info == null) {
            // hid_get_device_info will have set an error already
            return -1;
        }

        info.product = new String(string);

        return 0;
    }

    int hid_get_serial_number_string(byte[] string, int maxlen) throws IOException {
        if (string == null || maxlen == 0) {
            throw new IllegalArgumentException("Zero buffer/length");
        }

        HidDevice.Info info = hid_get_device_info();
        if (info == null) {
            // hid_get_device_info will have set an error already
            return -1;
        }

        info.serialNumber = new String(string);

        return 0;
    }

    private HidDevice.Info create_device_info_for_hid_device() throws IOException {
        List<HidDevice.Info> root = new ArrayList<>();

        // Get the dev_t (major/minor numbers) from the file handle.
        stat s = new stat();
        int ret = LinuxIO.INSTANCE.fstat(this.deviceHandle, s);
        if (-1 == ret) {
            throw new IOException("Failed to stat device handle");
        }

        // Create the udev object
        Udev.UdevContext udev = Udev.INSTANCE.udev_new();
        if (udev == null) {
            throw new IOException("Couldn't create udev context");
        }

        // Open a udev device from the dev_t. 'c' means character device.
        Udev.UdevDevice udev_dev = LinuxIO.UdevEx.INSTANCE.udev_device_new_from_devnum(udev, (byte) 'c', s.st_rdev);
        if (udev_dev != null) {
            root.add(create_device_info_for_device(udev_dev));
        }

        if (root.isEmpty()) {
            // TODO: have a better error reporting via create_device_info_for_device
            throw new IOException("Couldn't create hid_device_info");
        }

        Udev.INSTANCE.udev_device_unref(udev_dev);
        Udev.INSTANCE.udev_unref(udev);

        return root.get(0);
    }

    HidDevice.Info hid_get_device_info() throws IOException {
        if (deviceInfo == null) {
            // Lazy initialize deviceInfo
            deviceInfo = create_device_info_for_hid_device();
        }

        // create_device_info_for_hid_device will set an error if needed
        return deviceInfo;
    }

    @Override
    public void close() {
        ses.shutdownNow();
        inputReportBuffer.close();

        LinuxIO.INSTANCE.close(deviceHandle);
    }

    @Override
    public int write(byte[] data, int len, byte reportId) throws IOException {
        int bytes_written;

        if (data == null || len == 0) {
            throw new IllegalArgumentException(String.valueOf(EINVAL));
        }

        Memory memory = new Memory(len);
        memory.write(0, data, 0, len);
        bytes_written = LinuxIO.INSTANCE.write(deviceHandle, memory, new NativeLong(len)).intValue();
        if (bytes_written == -1)
            throw new IOException(String.valueOf(Native.getLastError()));

        return bytes_written;
    }

    int read(byte[] data, int length) throws IOException {
        int bytes_read = LinuxIO.INSTANCE.read(this.deviceHandle, inputReportBuffer, new NativeLong(length)).intValue();
        if (bytes_read < 0) {
            if (Native.getLastError() == EAGAIN || Native.getLastError() == EINPROGRESS)
                bytes_read = 0;
            else
                throw new IOException(String.valueOf(Native.getLastError()));
        }
        inputReportBuffer.read(0, data, 0, length);
        return bytes_read;
    }

    @Override
    public int getFeatureReport(byte[] data, byte reportId) throws IOException {
        int res;

        res = LinuxIO.INSTANCE.ioctl(deviceHandle, HIDIOCGFEATURE(data.length), data);
        if (res < 0)
            throw new IOException(String.format("ioctl (GFEATURE): %s", Native.getLastError()));

        return res;
    }

    @Override
    public int sendFeatureReport(byte[] data, byte reportId) throws IOException {
        int res;

        res = LinuxIO.INSTANCE.ioctl(deviceHandle, HIDIOCSFEATURE(data.length), data);
        if (res < 0)
            throw new IOException(String.format("ioctl (SFEATURE): %s", Native.getLastError()));

        return res;
    }

    private int get_hid_report_descriptor_from_hidraw(hidraw_report_descriptor rptDesc) throws IOException {
        IntByReference descSize = new IntByReference();

        // Get Report Descriptor Size
        int res = LinuxIO.INSTANCE.ioctl(this.deviceHandle, HIDIOCGRDESCSIZE, descSize);
        if (res < 0) {
            throw new IOException(String.format("ioctl(GRDESCSIZE): %s", Native.getLastError()));
        }

        // Get Report Descriptor
        rptDesc.size = descSize.getValue();
        res = LinuxIO.INSTANCE.ioctl(this.deviceHandle, HIDIOCGRDESC(rptDesc.size()), rptDesc.getPointer());
        if (res < 0) {
            throw new IOException(String.format("ioctl(GRDESC): %s", Native.getLastError()));
        }

        return res;
    }

    @Override
    public int getReportDescriptor(byte[] report) throws IOException {
        hidraw_report_descriptor rpt_desc = new hidraw_report_descriptor();
        int res = get_hid_report_descriptor_from_hidraw(rpt_desc);
        if (res < 0) {
            // error already registered
            return res;
        }

        int len = Math.min(rpt_desc.size, report.length);
        System.arraycopy(rpt_desc.value, 0, report, 0, len);

        return len;
    }

    @Override
    public int getInputReport(byte[] data, byte reportId) throws IOException {
        int res;

        res = LinuxIO.INSTANCE.ioctl(deviceHandle, HIDIOCGINPUT(data.length), data);
        if (res < 0)
            throw new IOException(String.format("ioctl (GINPUT): %s", Native.getLastError()));

        return res;
    }
}
