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
import java.util.NoSuchElementException;
import java.util.Scanner;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.platform.linux.Udev;
import com.sun.jna.ptr.IntByReference;
import net.java.games.input.linux.LinuxIO;
import net.java.games.input.linux.LinuxIO.hidraw_report_descriptor;
import org.hid4java.HidDevice;
import org.hid4java.NativeHidDevice;
import org.hid4java.NativeHidDeviceManager;

import static com.sun.jna.platform.linux.Fcntl.O_RDONLY;
import static net.java.games.input.linux.LinuxIO.HIDIOCGRDESCSIZE;
import static net.java.games.input.linux.LinuxIO.HID_MAX_DESCRIPTOR_SIZE;
import static net.java.games.input.linux.LinuxIO.O_CLOEXEC;
import static net.java.games.input.linux.LinuxIO.O_RDWR;
import static org.hid4java.HidDevice.Info.HidBusType.BUS_BLUETOOTH;
import static org.hid4java.HidDevice.Info.HidBusType.BUS_I2C;
import static org.hid4java.HidDevice.Info.HidBusType.BUS_SPI;
import static org.hid4java.HidDevice.Info.HidBusType.BUS_USB;


/**
 * WindowsHidDeviceManager.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2023-10-31 nsano initial version <br>
 */
public class LinuxHidDeviceManager implements NativeHidDeviceManager {

    /**
     * Gets the size of the HID item at the given position
     * Returns 1 if successful, 0 if an invalid key
     * Sets dataLen and keySize when successful
     */
    private static int getHidItemSize(byte[] reportDescriptor, int size, int pos, int[] dataLen, int[] keySize) {
        int key = reportDescriptor[pos];
        int sizeCode;

        // This is a Long Item. The next byte contains the
        // length of the data section (value) for this key.
        // See the HID specification, version 1.11, section
        // 6.2.2.3, titled "Long Items."
        if ((key & 0xf0) == 0xf0) {
            if (pos + 1 < size) {
                dataLen[0] = reportDescriptor[pos + 1];
                keySize[0] = 3;
                return 1;
            }
            dataLen[0] = 0; /* malformed report */
            keySize[0] = 0;
        }

        // This is a Short Item. The bottom two bits of the
        // key contain the size code for the data section
        // (value) for this key. Refer to the HID
        // specification, version 1.11, section 6.2.2.2,
        // titled "Short Items."
        sizeCode = key & 0x3;
        switch (sizeCode) {
        case 0:
        case 1:
        case 2:
            dataLen[0] = sizeCode;
            keySize[0] = 1;
            return 1;
        case 3:
            dataLen[0] = 4;
            keySize[0] = 1;
            return 1;
        default:
            // Can't ever happen since sizeCode is & 0x3
            dataLen[0] = 0;
            keySize[0] = 0;
            break;
        }

        /* malformed report */
        return 0;
    }

    /**
     * Get bytes from a HID Report Descriptor.
     * Only call with a numBytes of 0, 1, 2, or 4.
     */
    private static int getHidReportBytes(byte[] rpt, int len, int numBytes, int cur) {
        // Return if there aren't enough bytes.
        if (cur + numBytes >= len)
            return 0;

        if (numBytes == 0)
            return 0;
        else if (numBytes == 1)
            return rpt[cur + 1];
        else if (numBytes == 2)
            return (rpt[cur + 2] * 256 + rpt[cur + 1]);
        else if (numBytes == 4)
            return (rpt[cur + 4] * 0x01000000 +
                    rpt[cur + 3] * 0x00010000 +
                    rpt[cur + 2] * 0x00000100 +
                    rpt[cur + 1] * 0x00000001
            );
        else
            return 0;
    }

    /**
     * Iterates until the end of a Collection.
     * Assumes that *pos is exactly at the beginning of a Collection.
     * Skips all nested Collection, i.e. iterates until the end of current level Collection.
     * <p>
     * The return value is non-0 when an end of current Collection is found,
     * 0 when error is occured (broken Descriptor, end of a Collection is found before its begin,
     * or no Collection is found at all).
     */
    private static int hidIterateOverCollection(byte[] reportDescriptor, int size, int[] pos, int[] dataLen, int[] keySize) {
        int collectionLevel = 0;

        while (pos[0] < size) {
            int key = reportDescriptor[pos[0]];
            int keyCmd = key & 0xfc;

            // Determine dataLen and keySize
            if (getHidItemSize(reportDescriptor, size, pos[0], dataLen, keySize) == 0)
                return 0; // malformed report

            switch (keyCmd) {
            case 0xa0: // Collection 6.2.2.4 (Main)
                collectionLevel++;
                break;
            case 0xc0: // End Collection 6.2.2.4 (Main)
                collectionLevel--;
                break;
            }

            if (collectionLevel < 0) {
                // Broken descriptor or someone is using this function wrong,
                // i.e. should be called exactly at the collection start
                return 0;
            }

            if (collectionLevel == 0) {
                // Found it!
                // Also possible when called not at the collection start, but should not happen if used correctly
                return 1;
            }

            pos[0] += dataLen[0] + keySize[0];
        }

        return 0; // Did not find the end of a Collection
    }

    static class HidUsageIterator {

        int[] pos = new int[1];
        int usagePageFound;
        int usagePage;
    }

    /**
     * Retrieves the device's Usage Page and Usage from the report descriptor.
     * The algorithm returns the current Usage Page/Usage pair whenever a new
     * Collection is found and a Usage Local Item is currently in scope.
     * Usage Local Items are consumed by each Main Item (See. 6.2.2.8).
     * The algorithm should give similar results as Apple's:
     * https://developer.apple.com/documentation/iokit/kiohiddeviceusagepairskey?language=objc
     * Physical Collections are also matched (macOS does the same).
     * <p>
     * This function can be called repeatedly until it returns non-0
     * Usage is found. pos is the starting point (initially 0) and will be updated
     * to the next search position.
     * <p>
     * The return value is 0 when a pair is found.
     * 1 when finished processing descriptor.
     * -1 on a malformed report.
     */
    private static int getNextHidUsage(byte[] reportDescriptor, int size, HidUsageIterator ctx, int[] usagePage, int[] usage) {
        int[] dataLen = new int[1], keySize = new int[1];
        boolean initial = ctx.pos[0] == 0; // Used to handle case where no top-level application collection is defined

        int usageFound = 0;

        while (ctx.pos[0] < size) {
            int key = reportDescriptor[ctx.pos[0]];
            int keyCmd = key & 0xfc;

            // Determine dataLen and keySize
            if (getHidItemSize(reportDescriptor, size, ctx.pos[0], dataLen, keySize) == 0)
                return -1; // malformed report

            switch (keyCmd) {
            case 0x4: // Usage Page 6.2.2.7 (Global)
                ctx.usagePage = getHidReportBytes(reportDescriptor, size, dataLen[0], ctx.pos[0]);
                ctx.usagePageFound = 1;
                break;

            case 0x8: // Usage 6.2.2.8 (Local)
                if (dataLen[0] == 4) { // Usages 5.5 / Usage Page 6.2.2.7
                    ctx.usagePage = getHidReportBytes(reportDescriptor, size, 2, ctx.pos[0] + 2);
                    ctx.usagePageFound = 1;
                    usage[0] = getHidReportBytes(reportDescriptor, size, 2, ctx.pos[0]);
                    usageFound = 1;
                } else {
                    usage[0] = getHidReportBytes(reportDescriptor, size, dataLen[0], ctx.pos[0]);
                    usageFound = 1;
                }
                break;

            case 0xa0: // Collection 6.2.2.4 (Main)
                if (hidIterateOverCollection(reportDescriptor, size, ctx.pos, dataLen, keySize) == 0) {
                    return -1;
                }

                // A pair is valid - to be reported when Collection is found
                if (usageFound != 0 && ctx.usagePageFound != 0) {
                    usagePage[0] = ctx.usagePage;
                    return 0;
                }

                break;
            }

            // Skip over this key and its associated data
            ctx.pos[0] += dataLen[0] + keySize[0];
        }

        // If no top-level application collection is found and usage page/usage pair is found, pair is valid
        // https://docs.microsoft.com/en-us/windows-hardware/drivers/hid/top-level-collections */
        if (initial && usageFound != 0 && ctx.usagePageFound != 0) {
            usagePage[0] = ctx.usagePage;
            return 0; // success
        }

        return 1; // finished processing
    }

    /**
     * Retrieves the hidraw report descriptor from a file.
     * When using this form, <sysfs_path>/device/report_descriptor, elevated privileges are not required.
     */
    private static int getHidReportDescriptor(String rptPath, hidraw_report_descriptor rptDesc) throws IOException {
        int rptHandle;
        int res;

        rptHandle = LinuxIO.INSTANCE.open(rptPath, O_RDONLY | O_CLOEXEC);
        if (rptHandle < 0) {
            throw new IOException(String.format("create failed (%s): %s", rptPath, Native.getLastError()));
        }

        // Read in the Report Descriptor
        // The sysfs file has a maximum size of 4096 (which is the same as HID_MAX_DESCRIPTOR_SIZE) so we should always
        // be ok when reading the descriptor.
        // In practice if the HID descriptor is any larger I suspect many other things will break.
        Memory memory = new Memory(HID_MAX_DESCRIPTOR_SIZE);
        res = LinuxIO.INSTANCE.read(rptHandle, memory, new NativeLong(HID_MAX_DESCRIPTOR_SIZE)).intValue();
        if (res < 0) {
            throw new IOException(String.format("read failed (%s): %s", rptPath, Native.getLastError()));
        }
        memory.read(0, rptDesc.value, 0, res);
        rptDesc.size = res;

        LinuxIO.INSTANCE.close(rptHandle);
        return (int) res;
    }

    /* return size of the descriptor, or -1 on failure */
    private static int getHidReportDescriptorFromSysfs(String sysfsPath, hidraw_report_descriptor rptDesc) throws IOException {
        /* Con<sysfsPath>/device/report_descriptor */
        String rptPath = String.format("%s/device/report_descriptor", sysfsPath);

        return getHidReportDescriptor(rptPath, rptDesc);
    }

    /** return non-zero if successfully parsed */
    private static boolean parseHidVidPidFromUevent(String uevent, int[] busType, short[] vendorId, short[] productId) {

        Scanner s = new Scanner(uevent);
        while (s.hasNextLine()) {
            String line = s.nextLine();
            /* line: "KEY=value" */
            String[] pair = line.split("=");
            if (pair.length < 2) {
                continue;
            }
            String key = pair[0];
            String value = pair[1];

            if (key.equals("HID_ID")) {
                //        type vendor   product
                // HID_ID=0003:000005AC:00008242
                String[] ret = value.split(":");
                if (ret.length == 3) {
                    busType[0] = Integer.decode(ret[0]);
                    vendorId[0] = Short.decode(ret[1]);
                    productId[0] = Short.decode(ret[2]);
                    return true;
                }
            }
        }

//        throw new NoSuchElementException("Couldn't find/parse HID_ID");
        return false;
    }

    /** return non-zero if successfully parsed */
    private static boolean parseHidVidPidFromUeventPath(String ueventPath, int[] busType, short[] vendorId, short[] productId) throws IOException {
        int handle;
        int res;

        handle = LinuxIO.INSTANCE.open(ueventPath, O_RDONLY | O_CLOEXEC);
        if (handle < 0) {
            throw new IOException(String.format("create failed (%s): %s", ueventPath, Native.getLastError()));
        }

        Memory buf = new Memory(1024);
        res = LinuxIO.INSTANCE.read(handle, buf, new NativeLong(buf.size() - 1)).intValue(); // -1 for '\0' at the end
        LinuxIO.INSTANCE.close(handle);

        if (res < 0) {
            throw new IOException(String.format("read failed (%s): %s", ueventPath, Native.getLastError()));
        }

        return parseHidVidPidFromUevent(new String(buf.getByteArray(0, res)), busType, vendorId, productId);
    }

    /** return non-zero if successfully read/parsed */
    private boolean parseHidVidPidFromSysfs(String sysfsPath, int[] busType, short[] vendorId, short[] productId) throws IOException {
        // Con<sysfsPath>/device/uevent
        String ueventPath = String.format("%s/device/uevent", sysfsPath);

        return parseHidVidPidFromUeventPath(ueventPath, busType, vendorId, productId);
    }

    /**
     * The caller is responsible for free()ing the (newly-allocated) character
     * strings pointed to by serialNumber and productName after use.
     */
    private static boolean parseUeventInfo(String uevent, int[] busType,
                                           short[] vendorId, short[] productId,
                                           String[] serialNumber, String[] productName) {
        if (uevent == null) {
            return false;
        }

        boolean foundId = false;
        boolean foundSerial = false;
        boolean foundName = false;

        Scanner s = new Scanner(uevent);
        while (s.hasNextLine()) {
            String line = s.nextLine();
            /* line: "KEY=value" */
            String[] pair = line.split("=");
            if (pair.length < 2) {
                continue;
            }
            String key = pair[0];
            String value = pair[1];

            switch (key) {
            case "HID_ID":
                //        type vendor   product
                // HID_ID=0003:000005AC:00008242
                String[] ret = value.split(":");
                if (ret.length == 3) {
                    foundId = true;
                    busType[0] = Integer.decode(ret[0]);
                    vendorId[0] = Short.decode(ret[1]);
                    productId[0] = Short.decode(ret[2]);
                }
                break;
            case "HID_NAME":
                // The caller has to free the product name
                productName[0] = value;
                foundName = true;
                break;
            case "HID_UNIQ":
                // The caller has to free the serial number
                serialNumber[0] = value;
                foundSerial = true;
                break;
            }
        }

        return foundId && foundName && foundSerial;
    }

    /**
     * Get an attribute value from a udev_device and return it as a whar_t
     * string. The returned string must be freed with free() when done.
     */
    private static String copyUdevString(Udev.UdevDevice dev, String udevName) {
        return Udev.INSTANCE.udev_device_get_sysattr_value(dev, udevName);
    }

    /** */
    static HidDevice.Info createDeviceInfoForDevice(Udev.UdevDevice rawDev) throws IOException {
        List<HidDevice.Info> root = new ArrayList<>();

        Udev.UdevDevice usbDev; // The device's USB udev node.
        Udev.UdevDevice intfDev; // The device's interface (in the USB sense).
        hidraw_report_descriptor reportDesc = new hidraw_report_descriptor();

        String sysfsPath = Udev.INSTANCE.udev_device_get_syspath(rawDev);
        String devPath = Udev.INSTANCE.udev_device_get_devnode(rawDev);

        // The device's HID udev node.
        Udev.UdevDevice hidDev = Udev.INSTANCE.udev_device_get_parent_with_subsystem_devtype(
                rawDev,
                "hid",
                null);
        if (hidDev == null) {
            throw new IOException("Unable to find parent hid device.");
        }

        short[] devVid = new short[1];
        short[] devPid = new short[1];
        String[] serialNumber = new String[1];
        String[] productName = new String[1];
        int[] busType = new int[1];
        boolean result = parseUeventInfo(
                Udev.INSTANCE.udev_device_get_sysattr_value(hidDev, "uevent"),
                busType,
                devVid,
                devPid,
                serialNumber,
                productName);
        if (!result) {
            throw new IOException("parse_uevent_info() failed for at least one field.");
        }

        // Filter out unhandled devices right away
        switch (HidDevice.Info.HidBusType.values()[busType[0]]) {
        case BUS_BLUETOOTH:
        case BUS_I2C:
        case BUS_USB:
        case BUS_SPI:
            break;

        default:
            throw new IllegalArgumentException("unknown bus type: " + busType[0]);
        }

        // Create the record.
        HidDevice.Info curDev = new HidDevice.Info();

        // Fill out the record
        curDev.path = devPath;

        // VID/PID
        curDev.vendorId = devVid[0] & 0xffff;
        curDev.productId = devPid[0] & 0xffff;

        // Serial Number
        curDev.serialNumber = serialNumber[0];

        // Release Number
        curDev.releaseNumber = 0x0;

        // Interface Number
        curDev.interfaceNumber = -1;

        switch (HidDevice.Info.HidBusType.values()[busType[0]]) {
        case BUS_USB:
            // The device pointed to by rawDev contains information about
            // the hidraw device. In order to get information about the
            // USB device, get the parent device with the
            // subsystem/devtype pair of "usb"/"usb_device". This will
            // be several levels up the tree, but the function will find
            // it.
            usbDev = Udev.INSTANCE.udev_device_get_parent_with_subsystem_devtype(
                    rawDev,
                    "usb",
                    "usb_device");

            // uhid USB devices
            // Since this is a virtual hid interface, no USB information will
            // be available.
            if (usbDev == null) {
                /* Manufacturer and Product strings */
                curDev.manufacturer = "";
                curDev.product = productName[0];
                break;
            }

            curDev.manufacturer = copyUdevString(usbDev, "manufacturer");
            curDev.product = copyUdevString(usbDev, "product");

            curDev.busType = BUS_USB;

            String str = Udev.INSTANCE.udev_device_get_sysattr_value(usbDev, "bcdDevice");
            curDev.releaseNumber = str != null ? Integer.parseInt(str, 16) : 0x0;

            // Get a handle to the interface's udev node.
            intfDev = Udev.INSTANCE.udev_device_get_parent_with_subsystem_devtype(
                    rawDev,
                    "usb",
                    "usb_interface");
            if (intfDev != null) {
                str = Udev.INSTANCE.udev_device_get_sysattr_value(intfDev, "bInterfaceNumber");
                curDev.interfaceNumber = str != null ? Integer.parseInt(str, 16) : -1;
            }

            break;

        case BUS_BLUETOOTH:
            curDev.manufacturer = "";
            curDev.product = productName[0];

            curDev.busType = BUS_BLUETOOTH;

            break;
        case BUS_I2C:
            curDev.manufacturer = "";
            curDev.product = productName[0];

            curDev.busType = BUS_I2C;

            break;

        case BUS_SPI:
            curDev.manufacturer = "";
            curDev.product = productName[0];

            curDev.busType = BUS_SPI;

            break;

        default:
            // Unknown device type - this should never happen, as we
            // check for USB and Bluetooth devices above
            break;
        }

        // Usage Page and Usage
        int res = getHidReportDescriptorFromSysfs(sysfsPath, reportDesc);
        if (res >= 0) {
            int[] page = new int[1], usage = new int[1];
            HidUsageIterator usageIterator = new HidUsageIterator();

            // Parse the first usage and usage page
            // out of the report descriptor.
            if (getNextHidUsage(reportDesc.value, reportDesc.size, usageIterator, page, usage) == 0) {
                curDev.usagePage = page[0];
                curDev.usage = usage[0];
            }

            // Parse any additional usage and usage pages
            // out of the report descriptor.
            while (getNextHidUsage(reportDesc.value, reportDesc.size, usageIterator, page, usage) == 0) {
                // Create new record for additional usage pairs
                HidDevice.Info prevDev = curDev;

                curDev = new HidDevice.Info();

                // Update fields
                curDev.path = devPath;
                curDev.vendorId = devVid[0];
                curDev.productId = devPid[0];
                curDev.serialNumber = prevDev.serialNumber != null ? prevDev.serialNumber : null;
                curDev.releaseNumber = prevDev.releaseNumber;
                curDev.interfaceNumber = prevDev.interfaceNumber;
                curDev.manufacturer = prevDev.manufacturer != null ? prevDev.manufacturer : null;
                curDev.product = prevDev.product != null ? prevDev.product : null;
                curDev.usagePage = page[0];
                curDev.usage = usage[0];
                curDev.busType = prevDev.busType;

                root.add(curDev);
            }
        }

        return root.get(0);
    }

    @Override
    public void open() {
    }

    @Override
    public void close() {
    }

    @Override
    public NativeHidDevice create(int vendorId, int productId, String serialNumber) throws IOException {

        // register_global_error: global error is reset by hid_enumerate/hid_init
        List<HidDevice.Info> devs = enumerate(vendorId, productId);
        if (devs == null) {
            // register_global_error: global error is already set by hid_enumerate
            return null;
        }

        HidDevice.Info toOpen = null;
        for (HidDevice.Info curDev : devs) {
            if (curDev.vendorId == vendorId && curDev.productId == productId) {
                if (serialNumber != null) {
                    if (serialNumber.equals(curDev.serialNumber)) {
                        toOpen = curDev;
                        break;
                    }
                } else {
                    toOpen = curDev;
                    break;
                }
            }
        }

        if (toOpen != null) {
            // Open the device
            return create(toOpen);
        } else {
            throw new NoSuchElementException("Device with requested VID/PID/(SerialNumber) not found");
        }
    }

    @Override
    public List<HidDevice.Info> enumerate(int vendorId, int productId) throws IOException {

        List<HidDevice.Info> root = new ArrayList<>(); // return object

        // Create the udev object
        Udev.UdevContext udev = Udev.INSTANCE.udev_new();
        if (udev == null) {
            throw new IOException("Couldn't create udev context");
        }

        // Create a list of the devices in the 'hidraw' subsystem.
        Udev.UdevEnumerate enumerate = Udev.INSTANCE.udev_enumerate_new(udev);
        Udev.INSTANCE.udev_enumerate_add_match_subsystem(enumerate, "hidraw");
        Udev.INSTANCE.udev_enumerate_scan_devices(enumerate);
        Udev.UdevListEntry devices = Udev.INSTANCE.udev_enumerate_get_list_entry(enumerate);
        // For each item, see if it matches the vid/pid, and if so
        // create an udev_device record for it
        Udev.UdevListEntry devListEntry;
        while ((devListEntry = devices.getNext()) != null) {
            String sysfsPath;
            short[] devVid = new short[1];
            short[] devPid = new short[1];
            int[] busType = new int[1];
            Udev.UdevDevice rawDev; // The device's hidraw udev node.

            // Get the filename of the /sys entry for the device
            // and create an udev_device object (dev) representing it
            sysfsPath = Udev.INSTANCE.udev_list_entry_get_name(devListEntry);
            if (sysfsPath == null)
                continue;

            if (vendorId != 0 || productId != 0) {
                if (!parseHidVidPidFromSysfs(sysfsPath, busType, devVid, devPid))
                    continue;

                if (vendorId != 0 && vendorId != devVid[0])
                    continue;
                if (productId != 0 && productId != devPid[0])
                    continue;
            }

            rawDev = Udev.INSTANCE.udev_device_new_from_syspath(udev, sysfsPath);
            if (rawDev == null)
                continue;

            root.add(createDeviceInfoForDevice(rawDev));

            Udev.INSTANCE.udev_device_unref(rawDev);
        }

        // Free the enumerator and udev objects.
        Udev.INSTANCE.udev_enumerate_unref(enumerate);
        Udev.INSTANCE.udev_unref(udev);

        if (root.isEmpty()) {
            if (vendorId == 0 && productId == 0) {
                throw new IOException("No HID devices found in the system.");
            } else {
                throw new IOException("No HID devices with requested VID/PID found in the system.");
            }
        }

        return root;
    }

    @Override
    public NativeHidDevice create(HidDevice.Info info) throws IOException {
        LinuxHidDevice dev = new LinuxHidDevice();

        dev.deviceHandle = LinuxIO.INSTANCE.open(info.path, O_RDWR | O_CLOEXEC);

        if (dev.deviceHandle >= 0) {
            // Make sure this is a HIDRAW device - responds to HIDIOCGRDESCSIZE
            IntByReference descSize = new IntByReference();
            int res = LinuxIO.INSTANCE.ioctl(dev.deviceHandle, HIDIOCGRDESCSIZE, descSize);
            if (res < 0) {
                dev.close();
                throw new IOException(String.format("ioctl(GRDESCSIZE) error for '%s', not a HIDRAW device?: %s", info.path, Native.getLastError()));
            }

            return dev;
        } else {
            // Unable to create a device.
            throw new IOException(String.format("Failed to create a device with path '%s': %s", info.path, Native.getLastError()));
        }
    }

    @Override
    public boolean isSupported() {
        String os = System.getProperty("os.name").toLowerCase();
        return os.contains("nix") || os.contains("nux") || os.indexOf("aix") > 0;
    }
}
