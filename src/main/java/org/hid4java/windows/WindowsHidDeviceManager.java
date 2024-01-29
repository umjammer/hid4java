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
import org.hid4java.HidServicesSpecification;
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

    private HidServicesSpecification specification;

    /** @return nullable */
    private static byte[] hidInternalGetDevnodeProperty(int /* DEVINST */ devNode, DEVPROPKEY propertyKey, int /* DEVPROPTYPE */ expectedPropertyType) {
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
    private static byte[] hidInternalGetDeviceInterfaceProperty(String interfacePath, DEVPROPKEY propertyKey, int /* DEVPROPTYPE */ expectedPropertyType) {
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

    private static int hidInternalExtractIntTokenValue(String string, String token) {

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

    private static void hidInternalGetUsbInfo(HidDevice.Info dev, IntByReference /* DEVINST */ devNode) throws IOException {

        byte[] _deviceId = hidInternalGetDevnodeProperty(devNode.getValue(), DEVPKEY_Device_InstanceId, DEVPROP_TYPE_STRING);
        if (_deviceId == null)
            throw new IOException("hidInternalGetDevnodeProperty");

        // Normalize to upper case
        String deviceId = new String(_deviceId).toUpperCase();

        // Check for Xbox Common Controller class (XUSB) device.
	    // https://docs.microsoft.com/windows/win32/xinput/directinput-and-xusb-devices
        // https://docs.microsoft.com/windows/win32/xinput/xinput-and-directinput
	    if (hidInternalExtractIntTokenValue(deviceId, "IG_") != -1) {
            /* Get devnode parent to reach out USB device. */
            if (Cfgmgr32.INSTANCE.CM_Get_Parent(devNode, devNode.getValue(), 0) != CR_SUCCESS)
                throw new IOException();
        }

        // Get the hardware ids from devnode
        byte[] hardwareIds = hidInternalGetDevnodeProperty(devNode.getValue(), DEVPKEY_Device_HardwareIds, DEVPROP_TYPE_STRING_LIST);
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
                int releaseNumber = hidInternalExtractIntTokenValue(hardwareId, "REV_");
                if (releaseNumber != -1) {
                    dev.releaseNumber = (short) releaseNumber;
                }
            }

            if (dev.interfaceNumber == -1) {
                // USB_INTERFACE_DESCRIPTOR.bInterfaceNumber value.
                int interfaceNumber = hidInternalExtractIntTokenValue(hardwareId, "MI_");
                if (interfaceNumber != -1) {
                    dev.interfaceNumber = interfaceNumber;
                }
            }
        }

        // Try to get USB device manufacturer string if not provided by HidD_GetManufacturerString.
        if (dev.manufacturer == null || dev.manufacturer.isEmpty()) {
            byte[] manufacturer = hidInternalGetDevnodeProperty(devNode.getValue(), DEVPKEY_Device_Manufacturer, DEVPROP_TYPE_STRING);
            if (manufacturer != null) {
                dev.manufacturer = new String(manufacturer);
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
            _deviceId = hidInternalGetDevnodeProperty(usb_dev_node.getValue(), DEVPKEY_Device_InstanceId, DEVPROP_TYPE_STRING);
            if (_deviceId == null)
                throw new IOException("hidInternalGetDevnodeProperty");

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
    private static void hid_internal_get_ble_info(HidDevice.Info dev, int /* DEVINST */ dev_node) {
        if (dev.manufacturer == null || dev.manufacturer.isEmpty()) {
            /* Manufacturer Name String (UUID: 0x2A29) */
            byte[] manufacturer = hidInternalGetDevnodeProperty(dev_node, PKEY_DeviceInterface_Bluetooth_Manufacturer, DEVPROP_TYPE_STRING);
            if (manufacturer != null) {
                dev.manufacturer = new String(manufacturer);
            }
        }

        if (dev.serialNumber == null || dev.serialNumber.isEmpty()) {
            // Serial Number String (UUID: 0x2A25)
            byte[] serialNumber = hidInternalGetDevnodeProperty(dev_node, PKEY_DeviceInterface_Bluetooth_DeviceAddress, DEVPROP_TYPE_STRING);
            if (serialNumber != null) {
                dev.serialNumber = new String(serialNumber);
            }
        }

        if (dev.product == null || dev.product.isEmpty()) {
            // Model Number String (UUID: 0x2A24)
            byte[] product = hidInternalGetDevnodeProperty(dev_node, PKEY_DeviceInterface_Bluetooth_ModelNumber, DEVPROP_TYPE_STRING);
            if (product == null) {
                IntByReference /* DEVINST */ parentDevNode = new IntByReference();
                // Fallback: Get devnode grandparent to reach out Bluetooth LE device node
                if (Cfgmgr32.INSTANCE.CM_Get_Parent(parentDevNode, dev_node, 0) == CR_SUCCESS) {
                    // Device Name (UUID: 0x2A00)
                    product = hidInternalGetDevnodeProperty(parentDevNode.getValue(), DEVPKEY_NAME, DEVPROP_TYPE_STRING);
                }
            }

            if (product != null) {
                dev.product = new String(product);
            }
        }
    }

    private void hidInternalGetInfo(String interface_path, HidDevice.Info dev) throws IOException {

        // Get the device id from interface path
        byte[] deviceId = hidInternalGetDeviceInterfaceProperty(interface_path, DEVPKEY_Device_InstanceId, DEVPROP_TYPE_STRING);
        if (deviceId == null)
            throw new IOException("hidInternalGetDeviceInterfaceProperty");
        String device_id = new String(deviceId);

        // Open devnode from device id
        IntByReference /* DEVINST */ devNode = new IntByReference();
        int /* CONFIGRET */ cr = Cfgmgr32.INSTANCE.CM_Locate_DevNode(devNode, /* DEVINSTID_W */ device_id, Cfgmgr32.CM_LOCATE_DEVNODE_NORMAL);
        if (cr != CR_SUCCESS)
            throw new IOException();

        // Get devnode parent
        cr = Cfgmgr32.INSTANCE.CM_Get_Parent(devNode, devNode.getValue(), 0);
        if (cr != CR_SUCCESS)
            throw new IOException();

        // Get the compatible ids from parent devnode
        byte[] compatibleIds_ = hidInternalGetDevnodeProperty(devNode.getValue(), DEVPKEY_Device_CompatibleIds, DEVPROP_TYPE_STRING_LIST);
        if (compatibleIds_ == null)
            throw new IOException("hidInternalGetDevnodeProperty");
        String compatibleIds = new String(compatibleIds_).toUpperCase();

        // Now we can parse parent's compatible IDs to find out the device bus type
        switch (compatibleIds) {
        case "USB":
            // USB devices
            // https://docs.microsoft.com/windows-hardware/drivers/hid/plug-and-play-support
            // https://docs.microsoft.com/windows-hardware/drivers/install/standard-usb-identifiers
            dev.busType = BUS_USB;
            hidInternalGetUsbInfo(dev, devNode);
            break;
        case "BTHENUM":
            // Bluetooth devices
            // https://docs.microsoft.com/windows-hardware/drivers/bluetooth/installing-a-bluetooth-device
            dev.busType = BUS_BLUETOOTH;
            break;
        case "BTHLEDEVICE":
            // Bluetooth LE devices
            dev.busType = BUS_BLUETOOTH;
            hid_internal_get_ble_info(dev, devNode.getValue());
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

    private HidDevice.Info hidInternalGetDeviceInfo(String path, HANDLE handle) throws IOException {
        HIDD_ATTRIBUTES attrib = new HIDD_ATTRIBUTES();
        PointerByReference /* PHIDP_PREPARSED_DATA */ ppData = new PointerByReference();
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
        if (Hid.INSTANCE.HidD_GetPreparsedData(handle, ppData)) {
            if (Hid.INSTANCE.HidP_GetCaps(ppData.getValue(), caps) == HIDP_STATUS_SUCCESS) {
                dev.usagePage = caps.UsagePage;
                dev.usage = caps.Usage;
            }

            Hid.INSTANCE.HidD_FreePreparsedData(ppData.getValue());
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

        hidInternalGetInfo(path, dev);

        return dev;
    }

    @Override
    public void open(HidServicesSpecification specification) {
        this.specification = specification;
    }

    @Override
    public void close() {
    }

    private static HANDLE openDevice(String path, boolean openRw) {
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
        GUID interfaceClassGuid = new GUID();
        Hid.INSTANCE.HidD_GetHidGuid(interfaceClassGuid);

        // Get the list of all device interfaces belonging to the HID class.
        // Retry in case of list was changed between calls to
        // CM_Get_Device_Interface_List_SizeW and CM_Get_Device_Interface_ListW
        int /* CONFIGRET */ cr;
        Memory deviceInterfaceList;
        do {
            LongByReference len = new LongByReference();
            cr = Cfgmgr32Ex.INSTANCE.CM_Get_Device_Interface_List_Size(len, interfaceClassGuid, null, CM_GET_DEVICE_INTERFACE_LIST_PRESENT);
            if (cr != CR_SUCCESS) {
                throw new IOException("Failed to get size of HID device interface list");
            }

            deviceInterfaceList = new Memory(len.getValue());
            cr = Cfgmgr32Ex.INSTANCE.CM_Get_Device_Interface_List(interfaceClassGuid, null, deviceInterfaceList, len.getValue(), CM_GET_DEVICE_INTERFACE_LIST_PRESENT);
            if (cr != CR_SUCCESS && cr != CR_BUFFER_SMALL) {
                throw new IOException("Failed to get HID device interface list");
            }
        } while (cr == CR_BUFFER_SMALL);

        // Iterate over each device interface in the HID class, looking for the right one.
        for (String deviceInterface : deviceInterfaceList.getStringArray(0)) {
            HIDD_ATTRIBUTES attrib = new HIDD_ATTRIBUTES();

            // Open read-only handle to the device
            HANDLE deviceHandle = openDevice(deviceInterface, false);

            // Check validity of deviceHandle.
            if (deviceHandle == INVALID_HANDLE_VALUE) {
                // Unable to create the device.
                continue;
            }

            // Get the Vendor ID and Product ID for this device.
            attrib.Size = attrib.size();
            if (!Hid.INSTANCE.HidD_GetAttributes(deviceHandle, attrib)) {
                Kernel32.INSTANCE.CloseHandle(deviceHandle);
                continue;
            }

            // Check the VID/PID to see if we should add this
            // device to the enumeration list.
            if ((vendorId == 0x0 || attrib.VendorID == vendorId) &&
                    (productId == 0x0 || attrib.ProductID == productId)) {

                /* VID/PID match. Create the record. */
                root.add(hidInternalGetDeviceInfo(deviceInterface, deviceHandle));
            }

            Kernel32.INSTANCE.CloseHandle(deviceHandle);
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
        HANDLE deviceHandle = null;
        PointerByReference /* PHIDP_PREPARSED_DATA */ ppData = null;

        try {
            ppData = new PointerByReference();

            // Open a handle to the device
            deviceHandle = openDevice(info.path, true);

            // Check validity of write_handle.
            if (deviceHandle == INVALID_HANDLE_VALUE) {
                // System devices, such as keyboards and mice, cannot be opened in
                // read-write mode, because the system takes exclusive control over
                // them.  This is to prevent keyloggers.  However, feature reports
                // can still be sent and received.  Retry opening the device, but
                // without read/write access.
                deviceHandle = openDevice(info.path, false);

                // Check the validity of the limited deviceHandle.
                if (deviceHandle == INVALID_HANDLE_VALUE) {
                    throw new IOException("openDevice");
                }
            }

            // Set the Input Report buffer size to 64 reports.
            if (!Hid.INSTANCE.HidD_SetNumInputBuffers(deviceHandle, 64)) {
                throw new IOException("set input buffers");
            }

            // Get the Input Report length for the device.
            if (!Hid.INSTANCE.HidD_GetPreparsedData(deviceHandle, ppData)) {
                throw new IOException("get preparsed data");
            }

            HIDP_CAPS caps = new HIDP_CAPS();
            if (Hid.INSTANCE.HidP_GetCaps(ppData.getValue(), caps) != HIDP_STATUS_SUCCESS) {
                throw new IOException("HidP_GetCaps");
            }

            WindowsHidDevice dev = new WindowsHidDevice(specification);

            dev.deviceHandle = deviceHandle;
            deviceHandle = INVALID_HANDLE_VALUE;

            dev.outputReportLength = caps.OutputReportByteLength;
            dev.inputReportLength = caps.InputReportByteLength;
            dev.featureReportLength = caps.FeatureReportByteLength;
            dev.readBuf = new byte[dev.inputReportLength];
            dev.deviceInfo = hidInternalGetDeviceInfo(info.path, dev.deviceHandle);

            return dev;
        } catch (IOException e) {
            Kernel32.INSTANCE.CloseHandle(deviceHandle);

            if (ppData.getValue() != Pointer.NULL) {
                Hid.INSTANCE.HidD_FreePreparsedData(ppData.getValue());
            }

            throw e;
        }
    }

    @Override
    public boolean isSupported() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }
}
