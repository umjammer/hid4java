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
import java.util.ArrayList;
import java.util.List;

import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Cfgmgr32;
import com.sun.jna.platform.win32.Guid.GUID;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;
import com.sun.jna.ptr.PointerByReference;
import net.java.games.input.windows.WinAPI.Cfgmgr32Ex;
import net.java.games.input.windows.WinAPI.DEVPROPKEY;
import net.java.games.input.windows.WinAPI.HIDD_ATTRIBUTES;
import net.java.games.input.windows.WinAPI.HIDP_CAPS;
import net.java.games.input.windows.WinAPI.Hid;
import org.hid4java.HidDevice;
import org.hid4java.NativeHidDevice;
import org.hid4java.NativeHidDeviceManager;

import static com.sun.jna.platform.win32.Cfgmgr32.CR_BUFFER_SMALL;
import static com.sun.jna.platform.win32.Cfgmgr32.CR_SUCCESS;
import static com.sun.jna.platform.win32.WinBase.INVALID_HANDLE_VALUE;
import static com.sun.jna.platform.win32.WinNT.FILE_FLAG_OVERLAPPED;
import static com.sun.jna.platform.win32.WinNT.FILE_SHARE_READ;
import static com.sun.jna.platform.win32.WinNT.FILE_SHARE_WRITE;
import static com.sun.jna.platform.win32.WinNT.GENERIC_READ;
import static com.sun.jna.platform.win32.WinNT.GENERIC_WRITE;
import static com.sun.jna.platform.win32.WinNT.OPEN_EXISTING;
import static net.java.games.input.windows.WinAPI.Cfgmgr32Ex.CM_GET_DEVICE_INTERFACE_LIST_PRESENT;
import static net.java.games.input.windows.WinAPI.DEVPKEY_Device_CompatibleIds;
import static net.java.games.input.windows.WinAPI.DEVPKEY_Device_HardwareIds;
import static net.java.games.input.windows.WinAPI.DEVPKEY_Device_InstanceId;
import static net.java.games.input.windows.WinAPI.DEVPKEY_Device_Manufacturer;
import static net.java.games.input.windows.WinAPI.DEVPKEY_NAME;
import static net.java.games.input.windows.WinAPI.DEVPROP_TYPE_STRING;
import static net.java.games.input.windows.WinAPI.DEVPROP_TYPE_STRING_LIST;
import static net.java.games.input.windows.WinAPI.Hid.HIDP_STATUS_SUCCESS;
import static net.java.games.input.windows.WinAPI.MAX_STRING_WCHARS;
import static net.java.games.input.windows.WinAPI.PKEY_DeviceInterface_Bluetooth_DeviceAddress;
import static net.java.games.input.windows.WinAPI.PKEY_DeviceInterface_Bluetooth_Manufacturer;
import static net.java.games.input.windows.WinAPI.PKEY_DeviceInterface_Bluetooth_ModelNumber;
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
public class WindowsHidDeviceManager implements NativeHidDeviceManager {

    /** @return nullable */
    private byte[] hid_internal_get_devnode_property(int /* DEVINST */ devNode, DEVPROPKEY propertyKey, int /* DEVPROPTYPE */ expectedPropertyType) {
        IntByReference /* DEVPROPTYPE */ propertyType = new IntByReference();

        LongByReference len = new LongByReference();
        int /* CONFIGRET */ cr = Cfgmgr32Ex.INSTANCE.CM_Get_DevNode_Property(devNode, propertyKey, propertyType, null, len, 0);
        if (cr != CR_BUFFER_SMALL || propertyType.getValue() != expectedPropertyType)
            return null;

        Memory propertyValue = new Memory(len.getValue());
        cr = Cfgmgr32Ex.INSTANCE.CM_Get_DevNode_Property(devNode, propertyKey, propertyType, propertyValue, len, 0);
        if (cr != CR_SUCCESS) {
            return null;
        }

        return propertyValue.getByteArray(0L, (int) len.getValue());
    }

    /** @return nullable */
    private byte[] hid_internal_get_device_interface_property(String interfacePath, DEVPROPKEY propertyKey, int /* DEVPROPTYPE */ expectedPropertyType) {
        IntByReference /* DEVPROPTYPE */ propertyType = new IntByReference();

        LongByReference len = new LongByReference();
        int /* CONFIGRET */ cr = Cfgmgr32Ex.INSTANCE.CM_Get_Device_Interface_Property(interfacePath, propertyKey, propertyType, null, len, 0);
        if (cr != CR_BUFFER_SMALL || propertyType.getValue() != expectedPropertyType)
            return null;

        Memory propertyValue = new Memory(len.getValue());
        cr = Cfgmgr32Ex.INSTANCE.CM_Get_Device_Interface_Property(interfacePath, propertyKey, propertyType, propertyValue, len, 0);
        if (cr != CR_SUCCESS) {
            return null;
        }

        return propertyValue.getByteArray(0L, (int) len.getValue());
    }

    private int hid_internal_extract_int_token_value(String string, String token) {

        int startptr = string.indexOf(token);
        if (startptr < 0)
            return -1;

        startptr += token.length();
        return Integer.parseInt(string.substring(startptr), 16);
    }

    private static int strlen(byte[] b, int p) {
        int c = p;
        while (b[c] != 0) c++;
        return c - p;
    }

    private void hid_internal_get_usb_info(HidDevice.Info dev, IntByReference /* DEVINST */ devNode) throws IOException {

        byte[] _deviceId = hid_internal_get_devnode_property(devNode.getValue(), DEVPKEY_Device_InstanceId, DEVPROP_TYPE_STRING);
        if (_deviceId == null)
            throw new IOException("hid_internal_get_devnode_property");

        // Normalize to upper case
        String deviceId = new String(_deviceId).toUpperCase();

        // Check for Xbox Common Controller class (XUSB) device.
	    // https://docs.microsoft.com/windows/win32/xinput/directinput-and-xusb-devices
        // https://docs.microsoft.com/windows/win32/xinput/xinput-and-directinput
	    if (hid_internal_extract_int_token_value(deviceId, "IG_") != -1) {
            /* Get devnode parent to reach out USB device. */
            if (Cfgmgr32.INSTANCE.CM_Get_Parent(devNode, devNode.getValue(), 0) != CR_SUCCESS)
                throw new IOException();
        }

        // Get the hardware ids from devnode
        byte[] hardwareIds = hid_internal_get_devnode_property(devNode.getValue(), DEVPKEY_Device_HardwareIds, DEVPROP_TYPE_STRING_LIST);
        if (hardwareIds == null)
            throw new IOException();

        // Get additional information from USB device's Hardware ID
        // https://docs.microsoft.com/windows-hardware/drivers/install/standard-usb-identifiers
        // https://docs.microsoft.com/windows-hardware/drivers/usbcon/enumeration-of-interfaces-not-grouped-in-collections
	    for (int p = 0; hardwareIds[p] != 0; p += strlen(hardwareIds, p)) {
            /* Normalize to upper case */
            String hardwareId = new String(hardwareIds, p, strlen(hardwareIds, p)).toUpperCase();

            if (dev.releaseNumber == 0) {
                // USB_DEVICE_DESCRIPTOR.bcdDevice value.
                int releaseNumber = hid_internal_extract_int_token_value(hardwareId, "REV_");
                if (releaseNumber != -1) {
                    dev.releaseNumber = (short) releaseNumber;
                }
            }

            if (dev.interfaceNumber == -1) {
                // USB_INTERFACE_DESCRIPTOR.bInterfaceNumber value.
                int interfaceNumber = hid_internal_extract_int_token_value(hardwareId, "MI_");
                if (interfaceNumber != -1) {
                    dev.interfaceNumber = interfaceNumber;
                }
            }
        }

        // Try to get USB device manufacturer string if not provided by HidD_GetManufacturerString.
        if (dev.manufacturer == null || dev.manufacturer.isEmpty()) {
            byte[] _manufacturer = hid_internal_get_devnode_property(devNode.getValue(), DEVPKEY_Device_Manufacturer, DEVPROP_TYPE_STRING);
            if (_manufacturer != null) {
                dev.manufacturer = new String(_manufacturer);
            }
        }

        // Try to get USB device serial number if not provided by HidD_GetSerialNumberString.
        if (dev.serialNumber == null || dev.serialNumber.isEmpty()) {
            IntByReference /* DEVINST */ usb_dev_node = devNode;
            if (dev.interfaceNumber != -1) {
                // Get devnode parent to reach out composite parent USB device.
                // https://docs.microsoft.com/windows-hardware/drivers/usbcon/enumeration-of-the-composite-parent-device
                if (Cfgmgr32.INSTANCE.CM_Get_Parent(usb_dev_node, devNode.getValue(), 0) != CR_SUCCESS)
                    throw new IOException();
            }

            // Get the device id of the USB device.
            _deviceId = hid_internal_get_devnode_property(usb_dev_node.getValue(), DEVPKEY_Device_InstanceId, DEVPROP_TYPE_STRING);
            if (_deviceId == null)
                throw new IOException("hid_internal_get_devnode_property");

            // Extract substring after last '\\' of Instance ID.
            // For USB devices it may contain device's serial number.
            // https://docs.microsoft.com/windows-hardware/drivers/install/instance-ids
            for (int ptr = _deviceId.length - 1; ptr >= 0; --ptr) {
                // Instance ID is unique only within the scope of the bus.
                // For USB devices it means that serial number is not available. Skip.
                if (_deviceId[ptr] == '&')
                    break;

                if (_deviceId[ptr] == '\\') {
                    dev.serialNumber = new String(_deviceId, ptr + 1, _deviceId.length);
                    break;
                }
            }
        }

        // If we can't get the interface number, it means that there is only one interface.
        if (dev.interfaceNumber == -1)
            dev.interfaceNumber = 0;
    }

    /**
     * HidD_GetProductString/HidD_GetManufacturerString/HidD_GetSerialNumberString is not working for BLE HID devices
     * Request this info via dev node properties instead.
     * https://docs.microsoft.com/answers/questions/401236/hidd-getproductstring-with-ble-hid-device.html
     */
    private void hid_internal_get_ble_info(HidDevice.Info dev, int /* DEVINST */ dev_node) {
        if (dev.manufacturer == null || dev.manufacturer.isEmpty()) {
            /* Manufacturer Name String (UUID: 0x2A29) */
            byte[] _manufacturer = hid_internal_get_devnode_property(dev_node, PKEY_DeviceInterface_Bluetooth_Manufacturer, DEVPROP_TYPE_STRING);
            if (_manufacturer != null) {
                dev.manufacturer = new String(_manufacturer);
            }
        }

        if (dev.serialNumber == null || dev.serialNumber.isEmpty()) {
            // Serial Number String (UUID: 0x2A25)
            byte[] _serial_number = hid_internal_get_devnode_property(dev_node, PKEY_DeviceInterface_Bluetooth_DeviceAddress, DEVPROP_TYPE_STRING);
            if (_serial_number != null) {
                dev.serialNumber = new String(_serial_number);
            }
        }

        if (dev.product == null || dev.product.isEmpty()) {
            // Model Number String (UUID: 0x2A24)
            byte[] _product = hid_internal_get_devnode_property(dev_node, PKEY_DeviceInterface_Bluetooth_ModelNumber, DEVPROP_TYPE_STRING);
            if (_product == null) {
                IntByReference /* DEVINST */ parent_dev_node = new IntByReference();
                // Fallback: Get devnode grandparent to reach out Bluetooth LE device node
                if (Cfgmgr32.INSTANCE.CM_Get_Parent(parent_dev_node, dev_node, 0) == CR_SUCCESS) {
                    // Device Name (UUID: 0x2A00)
                    _product = hid_internal_get_devnode_property(parent_dev_node.getValue(), DEVPKEY_NAME, DEVPROP_TYPE_STRING);
                }
            }

            if (_product != null) {
                dev.product = new String(_product);
            }
        }
    }

    private void hid_internal_get_info(String interface_path, HidDevice.Info dev) throws IOException {

        // Get the device id from interface path
        byte[] _device_id = hid_internal_get_device_interface_property(interface_path, DEVPKEY_Device_InstanceId, DEVPROP_TYPE_STRING);
        if (_device_id == null)
            throw new IOException("hid_internal_get_device_interface_property");
        String device_id = new String(_device_id);

        // Open devnode from device id
        IntByReference /* DEVINST */ dev_node = new IntByReference();
        int /* CONFIGRET */ cr = Cfgmgr32.INSTANCE.CM_Locate_DevNode(dev_node, /* DEVINSTID_W */ device_id, Cfgmgr32.CM_LOCATE_DEVNODE_NORMAL);
        if (cr != CR_SUCCESS)
            throw new IOException();

        // Get devnode parent
        cr = Cfgmgr32.INSTANCE.CM_Get_Parent(dev_node, dev_node.getValue(), 0);
        if (cr != CR_SUCCESS)
            throw new IOException();

        // Get the compatible ids from parent devnode
        byte[] _compatible_ids = hid_internal_get_devnode_property(dev_node.getValue(), DEVPKEY_Device_CompatibleIds, DEVPROP_TYPE_STRING_LIST);
        if (_compatible_ids == null)
            throw new IOException("hid_internal_get_devnode_property");
        String compatible_ids = new String(_compatible_ids).toUpperCase();

        // Now we can parse parent's compatible IDs to find out the device bus type
        switch (compatible_ids) {
        case "USB":
            // USB devices
            // https://docs.microsoft.com/windows-hardware/drivers/hid/plug-and-play-support
            // https://docs.microsoft.com/windows-hardware/drivers/install/standard-usb-identifiers
            dev.busType = BUS_USB;
            hid_internal_get_usb_info(dev, dev_node);
            break;
        case "BTHENUM":
            // Bluetooth devices
            // https://docs.microsoft.com/windows-hardware/drivers/bluetooth/installing-a-bluetooth-device
            dev.busType = BUS_BLUETOOTH;
            break;
        case "BTHLEDEVICE":
            // Bluetooth LE devices
            dev.busType = BUS_BLUETOOTH;
            hid_internal_get_ble_info(dev, dev_node.getValue());
            break;
        case "PNP0C50":
            // I2C devices
            // https://docs.microsoft.com/windows-hardware/drivers/hid/plug-and-play-support-and-power-management
            dev.busType = BUS_I2C;
            break;
        case "PNP0C51":
            // SPI devices
            // https://docs.microsoft.com/windows-hardware/drivers/hid/plug-and-play-for-spi
            dev.busType = BUS_SPI;
            break;
        }
    }

    private HidDevice.Info hid_internal_get_device_info(String path, HANDLE handle) throws IOException {
        HIDD_ATTRIBUTES attrib = new HIDD_ATTRIBUTES();
        PointerByReference /* PHIDP_PREPARSED_DATA */ pp_data = new PointerByReference();
        HIDP_CAPS caps = new HIDP_CAPS();
        byte[] string = new byte[MAX_STRING_WCHARS];

        // Create the record.
        HidDevice.Info dev = new HidDevice.Info(); // return object

        // Fill out the record
        dev.path = path;
        dev.interfaceNumber = -1;

        attrib.Size = attrib.size();
        if (Hid.INSTANCE.HidD_GetAttributes(handle, attrib)) {
            // VID/PID
            dev.vendorId = attrib.VendorID;
            dev.productId = attrib.ProductID;

            // Release Number
            dev.releaseNumber = attrib.VersionNumber;
        }

        // Get the Usage Page and Usage for this device.
        if (Hid.INSTANCE.HidD_GetPreparsedData(handle, pp_data)) {
            if (Hid.INSTANCE.HidP_GetCaps(pp_data.getValue(), caps) == HIDP_STATUS_SUCCESS) {
                dev.usagePage = caps.UsagePage;
                dev.usage = caps.Usage;
            }

            Hid.INSTANCE.HidD_FreePreparsedData(pp_data.getValue());
        }

        // Serial Number
        string[0] = 0;
        Hid.INSTANCE.HidD_GetSerialNumberString(handle, string, string.length);
        string[MAX_STRING_WCHARS - 1] = 0;
        dev.serialNumber = new String(string).replace("\u0000", "");

        // Manufacturer String
        string[0] = 0;
        Hid.INSTANCE.HidD_GetManufacturerString(handle, string, string.length);
        string[MAX_STRING_WCHARS - 1] = 0;
        dev.manufacturer = new String(string).replace("\u0000", "");

        // Product String
        string[0] = 0;
        Hid.INSTANCE.HidD_GetProductString(handle, string, string.length);
        string[MAX_STRING_WCHARS - 1] = 0;
        dev.product = new String(string).replace("\u0000", "");

        hid_internal_get_info(path, dev);

        return dev;
    }

    @Override
    public void close() {
    }

    private static HANDLE open_device(String path, boolean openRw) {
        return Kernel32.INSTANCE.CreateFile(path,
                openRw ? GENERIC_WRITE | GENERIC_READ : 0,
                FILE_SHARE_READ | FILE_SHARE_WRITE,
                null,
                OPEN_EXISTING,
                FILE_FLAG_OVERLAPPED, /* FILE_ATTRIBUTE_NORMAL, */
                null);
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
        for (HidDevice.Info cur_dev : devs) {
            if (cur_dev.vendorId == vendorId && cur_dev.productId == productId) {
                if (serialNumber != null) {
                    if (serialNumber.equals(cur_dev.serialNumber)) {
                        toOpen = cur_dev;
                        break;
                    }
                } else {
                    toOpen = cur_dev;
                    break;
                }
            }
        }

        if (toOpen != null) {
            /* Open the device */
            return create(toOpen);
        } else {
            throw new IOException("Device with requested VID/PID/(SerialNumber) not found");
        }
    }

    @Override
    public List<HidDevice.Info> enumerate(int vendorId, int productId) throws IOException {
        List<HidDevice.Info> root = new ArrayList<>(); // return object

        // Retrieve HID Interface Class GUID
	    // https://docs.microsoft.com/windows-hardware/drivers/install/guid-devinterface-hid
        GUID interface_class_guid = new GUID();
        Hid.INSTANCE.HidD_GetHidGuid(interface_class_guid);

        // Get the list of all device interfaces belonging to the HID class.
        // Retry in case of list was changed between calls to
        // CM_Get_Device_Interface_List_SizeW and CM_Get_Device_Interface_ListW
        int /* CONFIGRET */ cr;
        Memory device_interface_list;
        do {
            LongByReference len = new LongByReference();
            cr = Cfgmgr32Ex.INSTANCE.CM_Get_Device_Interface_List_Size(len, interface_class_guid, null, CM_GET_DEVICE_INTERFACE_LIST_PRESENT);
            if (cr != CR_SUCCESS) {
                throw new IOException("Failed to get size of HID device interface list");
            }

            device_interface_list = new Memory(len.getValue());
            cr = Cfgmgr32Ex.INSTANCE.CM_Get_Device_Interface_List(interface_class_guid, null, device_interface_list, len.getValue(), CM_GET_DEVICE_INTERFACE_LIST_PRESENT);
            if (cr != CR_SUCCESS && cr != CR_BUFFER_SMALL) {
                throw new IOException("Failed to get HID device interface list");
            }
        } while (cr == CR_BUFFER_SMALL);

        // Iterate over each device interface in the HID class, looking for the right one.
        for (String device_interface : device_interface_list.getStringArray(0)) {
            HIDD_ATTRIBUTES attrib = new HIDD_ATTRIBUTES();

            // Open read-only handle to the device
            HANDLE device_handle = open_device(device_interface, false);

            // Check validity of device_handle.
            if (device_handle == INVALID_HANDLE_VALUE) {
                // Unable to create the device.
                continue;
            }

            // Get the Vendor ID and Product ID for this device.
            attrib.Size = attrib.size();
            if (!Hid.INSTANCE.HidD_GetAttributes(device_handle, attrib)) {
                Kernel32.INSTANCE.CloseHandle(device_handle);
                continue;
            }

            // Check the VID/PID to see if we should add this
            // device to the enumeration list.
            if ((vendorId == 0x0 || attrib.VendorID == vendorId) &&
                    (productId == 0x0 || attrib.ProductID == productId)) {

                /* VID/PID match. Create the record. */
                root.add(hid_internal_get_device_info(device_interface, device_handle));
            }

            Kernel32.INSTANCE.CloseHandle(device_handle);
        }

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
        HANDLE device_handle = null;
        PointerByReference /* PHIDP_PREPARSED_DATA */ pp_data = null;

        try {
            pp_data = new PointerByReference();

            // Open a handle to the device
            device_handle = open_device(info.path, true);

            // Check validity of write_handle.
            if (device_handle == INVALID_HANDLE_VALUE) {
                // System devices, such as keyboards and mice, cannot be opened in
                // read-write mode, because the system takes exclusive control over
                // them.  This is to prevent keyloggers.  However, feature reports
                // can still be sent and received.  Retry opening the device, but
                // without read/write access.
                device_handle = open_device(info.path, false);

                // Check the validity of the limited device_handle.
                if (device_handle == INVALID_HANDLE_VALUE) {
                    throw new IOException("open_device");
                }
            }

            // Set the Input Report buffer size to 64 reports.
            if (!Hid.INSTANCE.HidD_SetNumInputBuffers(device_handle, 64)) {
                throw new IOException("set input buffers");
            }

            // Get the Input Report length for the device.
            if (!Hid.INSTANCE.HidD_GetPreparsedData(device_handle, pp_data)) {
                throw new IOException("get preparsed data");
            }

            HIDP_CAPS caps = new HIDP_CAPS();
            if (Hid.INSTANCE.HidP_GetCaps(pp_data.getValue(), caps) != HIDP_STATUS_SUCCESS) {
                throw new IOException("HidP_GetCaps");
            }

            WindowsHidDevice dev = new WindowsHidDevice();

            dev.device_handle = device_handle;
            device_handle = INVALID_HANDLE_VALUE;

            dev.output_report_length = caps.OutputReportByteLength;
            dev.input_report_length = caps.InputReportByteLength;
            dev.feature_report_length = caps.FeatureReportByteLength;
            dev.read_buf = new byte[dev.input_report_length];
            dev.device_info = hid_internal_get_device_info(info.path, dev.device_handle);

            return dev;
        } catch (IOException e) {
            Kernel32.INSTANCE.CloseHandle(device_handle);

            if (pp_data.getValue() != Pointer.NULL) {
                Hid.INSTANCE.HidD_FreePreparsedData(pp_data.getValue());
            }

            throw e;
        }
    }

    @Override
    public boolean isSupported() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }
}
