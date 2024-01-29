/*
 * Copyright (c) 2023 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package org.hid4java.macos;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;
import com.sun.jna.ptr.NativeLongByReference;
import com.sun.jna.ptr.PointerByReference;
import org.hid4java.HidDevice;
import vavix.rococoa.corefoundation.CFAllocator;
import vavix.rococoa.corefoundation.CFArray;
import vavix.rococoa.corefoundation.CFData;
import vavix.rococoa.corefoundation.CFIndex;
import vavix.rococoa.corefoundation.CFLib;
import vavix.rococoa.corefoundation.CFRange;
import vavix.rococoa.corefoundation.CFString;
import vavix.rococoa.corefoundation.CFType;
import vavix.rococoa.iokit.IOKitLib;

import static vavix.rococoa.corefoundation.CFLib.CFNumberType.kCFNumberSInt32Type;
import static vavix.rococoa.corefoundation.CFString.CFSTR;
import static vavix.rococoa.iokit.IOKitLib.kIOHIDDeviceUsageKey;
import static vavix.rococoa.iokit.IOKitLib.kIOHIDDeviceUsagePageKey;
import static vavix.rococoa.iokit.IOKitLib.kIOHIDManufacturerKey;
import static vavix.rococoa.iokit.IOKitLib.kIOHIDProductIDKey;
import static vavix.rococoa.iokit.IOKitLib.kIOHIDProductKey;
import static vavix.rococoa.iokit.IOKitLib.kIOHIDSerialNumberKey;
import static vavix.rococoa.iokit.IOKitLib.kIOHIDTransportBluetoothValue;
import static vavix.rococoa.iokit.IOKitLib.kIOHIDTransportI2CValue;
import static vavix.rococoa.iokit.IOKitLib.kIOHIDTransportKey;
import static vavix.rococoa.iokit.IOKitLib.kIOHIDTransportSPIValue;
import static vavix.rococoa.iokit.IOKitLib.kIOHIDTransportUSBValue;
import static vavix.rococoa.iokit.IOKitLib.kIOHIDVendorIDKey;
import static vavix.rococoa.iokit.IOKitLib.kIOHIDVersionNumberKey;


/**
 * IOHIDDevice.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2023-10-06 nsano initial version <br>
 */
class IOHIDDevice {

    private static final Logger logger = Logger.getLogger(IOHIDDevice.class.getName());

    /** */
    Pointer /* IOHIDDeviceRef */ device;

    /** */
    public IOHIDDevice(Pointer /* IOHIDDeviceRef */ device) {
        this.device = device;
    }

    /** @return buffer length */
    private int getStringProperty(CFString prop, byte[] buf, int len) {
        if (len == 0)
            throw new IllegalArgumentException("len is zero");

//logger.fine("prop: " + prop.getString());
        CFType ret = IOKitLib.INSTANCE.IOHIDDeviceGetProperty(this.device, prop);
        if (ret == null) {
logger.finest("no prop value: " + prop.getString());
            return -1;
        }

        CFString str = ret.asString();

        buf[0] = 0;

        if (CFLib.INSTANCE.CFGetTypeID(str).equals(CFLib.INSTANCE.CFStringGetTypeID())) {
            int strLen = CFLib.INSTANCE.CFStringGetLength(str).intValue();
            CFRange.ByValue range = new CFRange.ByValue();
            NativeLongByReference usedBufferLength = new NativeLongByReference();
            int charsCopied;

            len--;

            range.location = new NativeLong(0);
            range.length = new NativeLong(Math.min(strLen, len));
            charsCopied = CFLib.INSTANCE.CFStringGetBytes(str,
                    range,
                    CFLib.kCFStringEncodingUTF8,
                    (byte) '?',
                    false,
                    buf,
                    CFIndex.of(len * Short.BYTES),
                    usedBufferLength).intValue();

            return usedBufferLength.getValue().intValue();
        } else {
logger.fine("not string: " + prop.getString());
            return -1;
        }
    }

    /** */
    private static byte[] duplicate(byte[] s, int len) {
        if (len == -1) len = 0;
        byte[] ret = new byte[len];
        System.arraycopy(s, 0, ret, 0, len);
        return ret;
    }

    /** */
    public String getStringProperty(String key, byte[] buf, int len) {
        int l = getStringProperty(CFSTR(key), buf, len);
        return new String(duplicate(buf, l));
    }

    /** */
    private static boolean tryGetIORegistryIntProperty(Pointer /* io_service_t */ service, CFString property, IntByReference outVal) {
        boolean result = false;
        CFType ref = IOKitLib.INSTANCE.IORegistryEntryCreateCFProperty(service, property, CFAllocator.kCFAllocatorDefault, 0);
        if (ref != null) {
            if (CFLib.INSTANCE.CFGetTypeID(ref).equals(CFLib.INSTANCE.CFNumberGetTypeID())) {
                result = CFLib.INSTANCE.CFNumberGetValue(ref.asNumber(), kCFNumberSInt32Type, outVal);
            }

            CFLib.INSTANCE.CFRelease(ref);
        }
        return result;
    }

    /** */
    private static int readUsbInterfaceFromHidServiceParent(Pointer /* io_service_t */ hidService) {
        int result = -1;
        boolean success;
        PointerByReference /* io_registry_entry_t */ current = new PointerByReference(IOKitLib.IO_OBJECT_NULL);
        int /* kern_return_t */ res;
        int parentNumber = 0;

        res = IOKitLib.INSTANCE.IORegistryEntryGetParentEntry(hidService, ByteBuffer.wrap(IOKitLib.kIOServicePlane.getBytes()), current);
        while (IOKitLib.KERN_SUCCESS == res
                // Only search up to 3 parent entries.
                // With the default driver - the parent-of-interest supposed to be the first one,
                // but lets assume some custom drivers or so, with deeper tree.
                && parentNumber < 3) {
            PointerByReference /* io_registry_entry_t */ parent = new PointerByReference(IOKitLib.IO_OBJECT_NULL);
            IntByReference interface_number = new IntByReference();
            parentNumber++;

            success = tryGetIORegistryIntProperty(current.getValue(), CFSTR(IOKitLib.kUSBInterfaceNumber), interface_number);
            if (success) {
                result = interface_number.getValue();
                break;
            }

            res = IOKitLib.INSTANCE.IORegistryEntryGetParentEntry(current.getValue(), ByteBuffer.wrap(IOKitLib.kIOServicePlane.getBytes()), parent);
            if (parent.getValue() != Pointer.NULL) {
                IOKitLib.INSTANCE.IOObjectRelease(current.getValue());
                current = parent;
            }

        }

        if (current.getValue() != Pointer.NULL) {
            IOKitLib.INSTANCE.IOObjectRelease(current.getValue());
            current.setValue(IOKitLib.IO_OBJECT_NULL);
        }

        return result;
    }

    /** */
    private boolean tryGetIntProperty(CFString key, IntByReference outVal) {
        boolean result = false;
        CFType ref = IOKitLib.INSTANCE.IOHIDDeviceGetProperty(this.device, key);
        if (ref != null) {
            if (CFLib.INSTANCE.CFGetTypeID(ref).equals(CFLib.INSTANCE.CFNumberGetTypeID())) {
                result = CFLib.INSTANCE.CFNumberGetValue(ref.asNumber(), kCFNumberSInt32Type, outVal);
            }
        }
        return result;
    }

    Pointer /* io_service_t */ getService() {
        return IOKitLib.INSTANCE.IOHIDDeviceGetService(this.device);
    }

    /** */
    String getPath() {
        Pointer /* io_service_t */ hidService = getService();
        LongByReference entryId = new LongByReference();
        int /* kern_return_t */ res;
        if (hidService != null) {
            res = IOKitLib.INSTANCE.IORegistryEntryGetRegistryEntryID(hidService, entryId);
        } else {
            res = IOKitLib.KERN_INVALID_ARGUMENT;
        }

        String path = null;
        if (res == IOKitLib.KERN_SUCCESS) {
            // max value of entryId(uint64_t) is 18446744073709551615 which is 20 characters long,
            // so for (max) "path" string 'DevSrvsID:18446744073709551615' we would need
            // 9+1+20+1=31 bytes buffer, but allocate 32 for simple alignment
            path = String.format("DevSrvsID:%d", entryId.getValue());
        }

        if (path == null) {
            // for whatever reason, trying to keep it a non-null string
            path = "";
        }

        return path;
    }

    /** */
    private HidDevice.Info createDeviceInfoWithUsage(int usage_page, int usage) {
        int devVid;
        int devPid;
        int BUF_LEN = 256;
        byte[] buf = new byte[BUF_LEN];
        CFType transportProp;

        HidDevice.Info curInfo;

        curInfo = new HidDevice.Info();

        devVid = getIntProperty(kIOHIDVendorIDKey) & 0xffff;
        devPid = getIntProperty(kIOHIDProductIDKey) & 0xffff;

        curInfo.usagePage = usage_page;
        curInfo.usage = usage;

        // Fill in the path (as a unique ID of the service entry)
        curInfo.path = getPath();

        // Serial Number
        curInfo.serialNumber = getStringProperty(kIOHIDSerialNumberKey, buf, BUF_LEN);

        // Manufacturer and Product strings
        curInfo.manufacturer = getStringProperty(kIOHIDManufacturerKey, buf, BUF_LEN);
        curInfo.product = getStringProperty(kIOHIDProductKey, buf, BUF_LEN);

        // VID/PID
        curInfo.vendorId = devVid;
        curInfo.productId = devPid;

        // Release Number
        curInfo.releaseNumber = getIntProperty(kIOHIDVersionNumberKey);

        // Interface Number.
        // We can only retrieve the interface number for USB HID devices.
        // See below
        curInfo.interfaceNumber = -1;

        // Bus Type
        transportProp = IOKitLib.INSTANCE.IOHIDDeviceGetProperty(this.device, CFSTR(kIOHIDTransportKey));

        if (transportProp != null && CFLib.INSTANCE.CFGetTypeID(transportProp).equals(CFLib.INSTANCE.CFStringGetTypeID())) {
            if (CFLib.INSTANCE.CFStringCompare(transportProp.asString(), CFSTR(kIOHIDTransportUSBValue), 0).intValue() == CFLib.CFComparisonResult.kCFCompareEqualTo) {
                IntByReference interface_number = new IntByReference();
                curInfo.busType = HidDevice.Info.HidBusType.BUS_USB;

                // A IOHIDDeviceRef used to have this simple property,
                // until macOS 13.3 - we will try to use it.
                if (tryGetIntProperty(CFSTR(IOKitLib.kUSBInterfaceNumber), interface_number)) {
                    curInfo.interfaceNumber = interface_number.getValue();
                } else {
                    // Otherwise fallback to io_service_t property.
                    // (of one of the parent services).
                    curInfo.interfaceNumber = readUsbInterfaceFromHidServiceParent(getService());

                    // If the above doesn't work -
                    // no (known) fallback exists at this point.
                }

                // Match "Bluetooth", "BluetoothLowEnergy" and "Bluetooth Low Energy" strings
            } else if (CFLib.INSTANCE.CFStringHasPrefix(transportProp.asString(), CFSTR(kIOHIDTransportBluetoothValue))) {
                curInfo.busType = HidDevice.Info.HidBusType.BUS_BLUETOOTH;
            } else if (CFLib.INSTANCE.CFStringCompare(transportProp.asString(), CFSTR(kIOHIDTransportI2CValue), 0).intValue() == CFLib.CFComparisonResult.kCFCompareEqualTo) {
                curInfo.busType = HidDevice.Info.HidBusType.BUS_I2C;
            } else if (CFLib.INSTANCE.CFStringCompare(transportProp.asString(), CFSTR(kIOHIDTransportSPIValue), 0).intValue() == CFLib.CFComparisonResult.kCFCompareEqualTo) {
                curInfo.busType = HidDevice.Info.HidBusType.BUS_SPI;
            }
        }

        return curInfo;
    }

    /** */
    private CFArray getArrayProperty(CFString key) {
        CFType ref = IOKitLib.INSTANCE.IOHIDDeviceGetProperty(this.device, key);
        if (ref != null && CFLib.INSTANCE.CFGetTypeID(ref).equals(CFLib.INSTANCE.CFArrayGetTypeID())) {
            return ref.asArray();
        } else {
            return null;
        }
    }

    /** */
    private CFArray getUsagePairs() {
        return getArrayProperty(CFSTR(IOKitLib.kIOHIDDeviceUsagePairsKey));
    }

    /** */
    public List<HidDevice.Info> createDeviceInfo() {
        int primaryUsagePage = getIntProperty(CFSTR(IOKitLib.kIOHIDPrimaryUsagePageKey));
        int primaryUsage = getIntProperty(CFSTR(IOKitLib.kIOHIDPrimaryUsageKey));

        // Primary should always be first, to match previous behavior.
        List<HidDevice.Info> deviceInfos = new ArrayList<>();
        HidDevice.Info cur = createDeviceInfoWithUsage(primaryUsagePage, primaryUsage);
        deviceInfos.add(cur);

        CFArray usage_pairs = getUsagePairs();

        if (usage_pairs != null) {
            HidDevice.Info next;
            for (int i = 0; i < CFLib.INSTANCE.CFArrayGetCount(usage_pairs).intValue(); i++) {
                CFType dict = CFLib.INSTANCE.CFArrayGetValueAtIndex(usage_pairs, i);
                if (!CFLib.INSTANCE.CFGetTypeID(dict).equals(CFLib.INSTANCE.CFDictionaryGetTypeID())) {
                    continue;
                }

                CFType[] usagePageRef = new CFType[1], usageRef = new CFType[1];
                IntByReference usagePageP = new IntByReference(), usageP = new IntByReference();

                if (!CFLib.INSTANCE.CFDictionaryGetValueIfPresent(dict.asDict(), CFSTR(kIOHIDDeviceUsagePageKey), usagePageRef) ||
                        !CFLib.INSTANCE.CFDictionaryGetValueIfPresent(dict.asDict(), CFSTR(kIOHIDDeviceUsageKey), usageRef) ||
                        !CFLib.INSTANCE.CFGetTypeID(usagePageRef[0]).equals(CFLib.INSTANCE.CFNumberGetTypeID()) ||
                        !CFLib.INSTANCE.CFGetTypeID(usageRef[0]).equals(CFLib.INSTANCE.CFNumberGetTypeID()) ||
                        !CFLib.INSTANCE.CFNumberGetValue(usagePageRef[0].asNumber(), kCFNumberSInt32Type, usagePageP) ||
                        !CFLib.INSTANCE.CFNumberGetValue(usageRef[0].asNumber(), kCFNumberSInt32Type, usageP)) {
                    continue;
                }
                int usagePage = usagePageP.getValue();
                int usage = usageP.getValue();
                if (usagePage == primaryUsagePage && usage == primaryUsage) {
logger.finest("same usagePage: " + usagePage + ", usage: " + usage);
                    continue; // Already added.
                }

                next = createDeviceInfoWithUsage(usagePage, usage);
                deviceInfos.add(next);
            }
        }

logger.finest("infos: " + deviceInfos.size());
        return deviceInfos;
    }

    /** */
    private int getIntProperty(CFString key) {
        CFType ref = IOKitLib.INSTANCE.IOHIDDeviceGetProperty(this.device, key);
        if (ref != null) {
            if (CFLib.INSTANCE.CFGetTypeID(ref).equals(CFLib.INSTANCE.CFNumberGetTypeID())) {
                IntByReference p = new IntByReference();
                CFLib.INSTANCE.CFNumberGetValue(ref.asNumber(), kCFNumberSInt32Type, p);
                return p.getValue();
            }
        }
        return 0;
    }

    /** */
    int getIntProperty(String key) {
        return getIntProperty(CFSTR(key));
    }

    /**
     * @return read length
     * @throws IllegalStateException
     */
    int hidGetReportDescriptor(byte[] buf, int bufSize) {
        CFType ref = IOKitLib.INSTANCE.IOHIDDeviceGetProperty(this.device, CFSTR(IOKitLib.kIOHIDReportDescriptorKey));
        if (ref != null && CFLib.INSTANCE.CFGetTypeID(ref).equals(CFLib.INSTANCE.CFDataGetTypeID())) {
            CFData reportDescriptor = ref.asData();
            Pointer descriptorBuf = CFLib.INSTANCE.CFDataGetBytePtr(reportDescriptor);
            NativeLong descriptorBufLen = CFLib.INSTANCE.CFDataGetLength(reportDescriptor);
            int copyLen = descriptorBufLen.intValue();

            if (descriptorBuf == null || descriptorBufLen.intValue() < 0) {
                throw new IllegalStateException("Zero buffer/length");
            }

            if (bufSize < copyLen) {
                copyLen = bufSize;
            }

            descriptorBuf.read(0, buf, 0, copyLen);
            return copyLen;
        } else {
            throw new IllegalStateException("Failed to get kIOHIDReportDescriptorKey property");
        }
    }
}
