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
import vavix.rococoa.corefoundation.CFIndex;
import vavix.rococoa.corefoundation.CFLib;
import vavix.rococoa.corefoundation.CFNumber;
import vavix.rococoa.corefoundation.CFRange;
import vavix.rococoa.corefoundation.CFString;
import vavix.rococoa.corefoundation.CFType;
import vavix.rococoa.iokit.IOKitLib;

import static vavix.rococoa.corefoundation.CFLib.CFNumberType.kCFNumberSInt32Type;
import static vavix.rococoa.iokit.IOKitLib.kIOHIDManufacturerKey;
import static vavix.rococoa.iokit.IOKitLib.kIOHIDProductKey;
import static vavix.rococoa.iokit.IOKitLib.kIOHIDSerialNumberKey;
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
    Pointer/*IOHIDDeviceRef*/ device;

    /** */
    public IOHIDDevice(Pointer/*IOHIDDeviceRef*/ device) {
        this.device = device;
    }

    /** @return buffer length */
    private int get_string_property(CFString prop, byte[] buf, int len) {
        if (len == 0)
            throw new IllegalArgumentException("len is zero");

//logger.fine("prop: " + prop.getString());
        CFType ret = IOKitLib.INSTANCE.IOHIDDeviceGetProperty(this.device, prop);
        if (ret == null) {
logger.finer("no prop value: " + prop.getString());
            return -1;
        }

        CFString str = ret.asString();

        buf[0] = 0;

        if (CFLib.INSTANCE.CFGetTypeID(str).equals(CFLib.INSTANCE.CFStringGetTypeID())) {
            int str_len = CFLib.INSTANCE.CFStringGetLength(str).intValue();
            CFRange.ByValue range = new CFRange.ByValue();
            NativeLongByReference used_buf_len = new NativeLongByReference();
            int chars_copied;

            len--;

            range.location = new NativeLong(0);
            range.length = new NativeLong(Math.min(str_len, len));
            chars_copied = CFLib.INSTANCE.CFStringGetBytes(str,
                    range,
                    CFLib.kCFStringEncodingUTF8,
                    (byte) '?',
                    false,
                    buf,
                    CFIndex.of(len * Short.BYTES),
                    used_buf_len).intValue();

            return used_buf_len.getValue().intValue();
        } else {
logger.fine("not string: " + prop.getString());
            return -1;
        }
    }

    /** */
    private static byte[] dup_wcs(byte[] s, int len) {
        if (len == -1) len = 0;
        byte[] ret = new byte[len];
        System.arraycopy(s, 0, ret, 0, len);
        return ret;
    }

    /** */
    public String get_string_property(String key, byte[] buf, int len) {
        int l = get_string_property(CFLib.INSTANCE.__CFStringMakeConstantString(key), buf, len);
        return new String(dup_wcs(buf, l));
    }

    /** */
    private boolean try_get_ioregistry_int_property(Pointer/*io_service_t*/ service, CFString property, IntByReference out_val) {
        boolean result = false;
        CFType ref = IOKitLib.INSTANCE.IORegistryEntryCreateCFProperty(service, property, CFAllocator.kCFAllocatorDefault, 0);
        if (ref != null) {
            if (CFLib.INSTANCE.CFGetTypeID(ref).equals(CFLib.INSTANCE.CFNumberGetTypeID())) {
                result = CFLib.INSTANCE.CFNumberGetValue((CFNumber) ref, kCFNumberSInt32Type, out_val);
            }

            CFLib.INSTANCE.CFRelease(ref);
        }
        return result;
    }

    /** */
    private int read_usb_interface_from_hid_service_parent(Pointer/*io_service_t*/ hid_service) {
        int result = -1;
        boolean success;
        PointerByReference/*io_registry_entry_t*/ current = new PointerByReference(IOKitLib.IO_OBJECT_NULL);
        int /*kern_return_t*/ res;
        int parent_number = 0;

        res = IOKitLib.INSTANCE.IORegistryEntryGetParentEntry(hid_service, ByteBuffer.wrap(IOKitLib.kIOServicePlane.getBytes()), current);
        while (IOKitLib.KERN_SUCCESS == res
                /* Only search up to 3 parent entries.
                 * With the default driver - the parent-of-interest supposed to be the first one,
                 * but lets assume some custom drivers or so, with deeper tree. */
                && parent_number < 3) {
            PointerByReference/*io_registry_entry_t*/ parent = new PointerByReference(IOKitLib.IO_OBJECT_NULL);
            IntByReference interface_number = new IntByReference();
            parent_number++;

            success = try_get_ioregistry_int_property(current.getValue(), CFLib.INSTANCE.__CFStringMakeConstantString(IOKitLib.kUSBInterfaceNumber), interface_number);
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
    private boolean try_get_int_property(CFString key, IntByReference out_val) {
        boolean result = false;
        CFType ref = IOKitLib.INSTANCE.IOHIDDeviceGetProperty(this.device, key);
        if (ref != null) {
            if (CFLib.INSTANCE.CFGetTypeID(ref).equals(CFLib.INSTANCE.CFNumberGetTypeID())) {
                result = CFLib.INSTANCE.CFNumberGetValue(ref.asNumber(), kCFNumberSInt32Type, out_val);
            }
        }
        return result;
    }

    /** */
    private HidDevice.Info create_device_info_with_usage(int usage_page, int usage) {
        int dev_vid;
        int dev_pid;
        int BUF_LEN = 256;
        byte[] buf = new byte[BUF_LEN];
        CFType transport_prop;

        HidDevice.Info cur_info;
        Pointer/*io_service_t*/ hid_service;
        int/*kern_return_t*/ res;
        LongByReference entry_id = new LongByReference();

        cur_info = new HidDevice.Info();

        dev_vid = get_int_property(IOKitLib.kIOHIDVendorIDKey) & 0xffff;
        dev_pid = get_int_property(IOKitLib.kIOHIDProductIDKey) & 0xffff;

        cur_info.usagePage = usage_page;
        cur_info.usage = usage;

        // Fill in the path (as a unique ID of the service entry)
        cur_info.path = null;
        hid_service = IOKitLib.INSTANCE.IOHIDDeviceGetService(this.device);
        if (hid_service != null) {
            res = IOKitLib.INSTANCE.IORegistryEntryGetRegistryEntryID(hid_service, entry_id);
        } else {
            res = IOKitLib.KERN_INVALID_ARGUMENT;
        }

        if (res == IOKitLib.KERN_SUCCESS) {
            // max value of entry_id(uint64_t) is 18446744073709551615 which is 20 characters long,
            // so for (max) "path" string 'DevSrvsID:18446744073709551615' we would need
            // 9+1+20+1=31 bytes buffer, but allocate 32 for simple alignment */
            int path_len = 32;
            cur_info.path = String.format("DevSrvsID:%d", entry_id.getValue());
        }

        if (cur_info.path == null) {
            /* for whatever reason, trying to keep it a non-null string */
            cur_info.path = "";
        }

        // Serial Number
        cur_info.serialNumber = get_string_property(kIOHIDSerialNumberKey, buf, BUF_LEN);

        // Manufacturer and Product strings
        cur_info.manufacturer = get_string_property(kIOHIDManufacturerKey, buf, BUF_LEN);
        cur_info.product = get_string_property(kIOHIDProductKey, buf, BUF_LEN);

        // VID/PID
        cur_info.vendorId = dev_vid;
        cur_info.productId = dev_pid;

        // Release Number
        cur_info.releaseNumber = get_int_property(kIOHIDVersionNumberKey);

        // Interface Number.
        // We can only retrieve the interface number for USB HID devices.
        // See below
        cur_info.interfaceNumber = -1;

        // Bus Type
        transport_prop = IOKitLib.INSTANCE.IOHIDDeviceGetProperty(this.device, CFLib.INSTANCE.__CFStringMakeConstantString(IOKitLib.kIOHIDTransportKey));

        if (transport_prop != null && CFLib.INSTANCE.CFGetTypeID(transport_prop).equals(CFLib.INSTANCE.CFStringGetTypeID())) {
            if (CFLib.INSTANCE.CFStringCompare(transport_prop.asString(), CFLib.INSTANCE.__CFStringMakeConstantString(IOKitLib.kIOHIDTransportUSBValue), 0).intValue() == CFLib.CFComparisonResult.kCFCompareEqualTo) {
                IntByReference interface_number = new IntByReference();
                cur_info.bus_type = HidDevice.Info.hid_bus_type.HID_API_BUS_USB;

                // A IOHIDDeviceRef used to have this simple property,
                // until macOS 13.3 - we will try to use it. */
                if (try_get_int_property(CFLib.INSTANCE.__CFStringMakeConstantString(IOKitLib.kUSBInterfaceNumber), interface_number)) {
                    cur_info.interfaceNumber = interface_number.getValue();
                } else {
                    // Otherwise fallback to io_service_t property.
                    // (of one of the parent services). */
                    cur_info.interfaceNumber = read_usb_interface_from_hid_service_parent(hid_service);

                    // If the above doesn't work -
                    // no (known) fallback exists at this point. */
                }

                // Match "Bluetooth", "BluetoothLowEnergy" and "Bluetooth Low Energy" strings
            } else if (CFLib.INSTANCE.CFStringHasPrefix(transport_prop.asString(), CFLib.INSTANCE.__CFStringMakeConstantString(IOKitLib.kIOHIDTransportBluetoothValue))) {
                cur_info.bus_type = HidDevice.Info.hid_bus_type.HID_API_BUS_BLUETOOTH;
            } else if (CFLib.INSTANCE.CFStringCompare(transport_prop.asString(), CFLib.INSTANCE.__CFStringMakeConstantString(IOKitLib.kIOHIDTransportI2CValue), 0).intValue() == CFLib.CFComparisonResult.kCFCompareEqualTo) {
                cur_info.bus_type = HidDevice.Info.hid_bus_type.HID_API_BUS_I2C;
            } else if (CFLib.INSTANCE.CFStringCompare(transport_prop.asString(), CFLib.INSTANCE.__CFStringMakeConstantString(IOKitLib.kIOHIDTransportSPIValue), 0).intValue() == CFLib.CFComparisonResult.kCFCompareEqualTo) {
                cur_info.bus_type = HidDevice.Info.hid_bus_type.HID_API_BUS_SPI;
            }
        }

        return cur_info;
    }

    /** */
    private CFArray get_array_property(CFString key) {
        CFType ref = IOKitLib.INSTANCE.IOHIDDeviceGetProperty(this.device, key);
        if (ref != null && CFLib.INSTANCE.CFGetTypeID(ref).equals(CFLib.INSTANCE.CFArrayGetTypeID())) {
            return ref.asArray();
        } else {
            return null;
        }
    }

    /** */
    private CFArray get_usage_pairs() {
        return get_array_property(CFLib.INSTANCE.__CFStringMakeConstantString(IOKitLib.kIOHIDDeviceUsagePairsKey));
    }

    /** */
    public List<HidDevice.Info> create_device_info() {
        int primary_usage_page = get_int_property(CFLib.INSTANCE.__CFStringMakeConstantString(IOKitLib.kIOHIDPrimaryUsagePageKey));
        int primary_usage = get_int_property(CFLib.INSTANCE.__CFStringMakeConstantString(IOKitLib.kIOHIDPrimaryUsageKey));

        // Primary should always be first, to match previous behavior.
        List<HidDevice.Info> deviceInfos = new ArrayList<>();
        HidDevice.Info cur = create_device_info_with_usage(primary_usage_page, primary_usage);
        deviceInfos.add(cur);

        CFArray usage_pairs = get_usage_pairs();

        if (usage_pairs != null) {
            HidDevice.Info next;
            for (int i = 0; i < CFLib.INSTANCE.CFArrayGetCount(usage_pairs).intValue(); i++) {
                CFType dict = CFLib.INSTANCE.CFArrayGetValueAtIndex(usage_pairs, i);
                if (!CFLib.INSTANCE.CFGetTypeID(dict).equals(CFLib.INSTANCE.CFDictionaryGetTypeID())) {
                    continue;
                }

                CFType[] usage_page_ref = new CFType[1], usage_ref = new CFType[1];
                IntByReference usage_pageP = new IntByReference(), usageP = new IntByReference();

                if (!CFLib.INSTANCE.CFDictionaryGetValueIfPresent(dict.asDict(), CFLib.INSTANCE.__CFStringMakeConstantString(IOKitLib.kIOHIDDeviceUsagePageKey), usage_page_ref) ||
                        !CFLib.INSTANCE.CFDictionaryGetValueIfPresent(dict.asDict(), CFLib.INSTANCE.__CFStringMakeConstantString(IOKitLib.kIOHIDDeviceUsageKey), usage_ref) ||
                        !CFLib.INSTANCE.CFGetTypeID(usage_page_ref[0]).equals(CFLib.INSTANCE.CFNumberGetTypeID()) ||
                        !CFLib.INSTANCE.CFGetTypeID(usage_ref[0]).equals(CFLib.INSTANCE.CFNumberGetTypeID()) ||
                        !CFLib.INSTANCE.CFNumberGetValue(usage_page_ref[0].asNumber(), kCFNumberSInt32Type, usage_pageP) ||
                        !CFLib.INSTANCE.CFNumberGetValue(usage_ref[0].asNumber(), kCFNumberSInt32Type, usageP)) {
                    continue;
                }
                int usage_page = usage_pageP.getValue();
                int usage = usageP.getValue();
                if (usage_page == primary_usage_page && usage == primary_usage) {
logger.finer("same usage_page: " + usage_page + ", usage: " + usage);
                    continue; // Already added.
                }

                next = create_device_info_with_usage(usage_page, usage);
                deviceInfos.add(next);
            }
        }

logger.finer("infos: " + deviceInfos.size());
        return deviceInfos;
    }

    /** */
    private int get_int_property(CFString key) {
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
    int get_int_property(String key) {
        return get_int_property(CFLib.INSTANCE.__CFStringMakeConstantString(key));
    }
}
