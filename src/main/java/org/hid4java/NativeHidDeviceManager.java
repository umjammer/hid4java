/*
 * Copyright (c) 2023 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package org.hid4java;

import java.io.IOException;
import java.util.List;


/**
 * org.hid4java.NativeHidDeviceManager.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2023-10-06 nsano initial version <br>
 */
public interface NativeHidDeviceManager {

    /**
     * Finalize the HIDAPI library.
     * <p>
     * This function frees all of the static data associated with HIDAPI. It should be called
     * at the end of execution to avoid memory leaks.
     */
    void exit();

    /**
     * Open a HID device using a Vendor ID (VID), Product ID (PID) and optionally a serial number.
     * <p>
     * If serial_number is NULL, the first device with the specified VID and PID is opened.
     *
     * @param vendor_id     The vendor ID
     * @param product_id    The product ID
     * @param serial_number The serial number (or null for wildcard)
     * @return A pointer to a HidDevice on success or null on failure
     */
    NativeHidDevice open(int vendor_id, int product_id, String serial_number) throws IOException;

    /**
     * Enumerate the HID Devices.
     * <p>
     * This function returns a linked list of all the HID devices attached to the system which match vendor_id and product_id.
     * <p>
     * If vendor_id is set to 0 then any vendor matches. If product_id is set to 0 then any product matches.
     * <p>
     * If vendor_id and product_id are both set to 0, then all HID devices will be returned.
     *
     * @param vendor_id  The vendor ID
     * @param product_id The product ID
     * @return A linked list of all discovered matching devices
     */
    List<HidDevice.Info> enumerate(int vendor_id, int product_id) throws IOException;

    /**
     * Open a HID device by its path name.
     * <p>
     * The path name be determined by calling hid_enumerate(), or a platform-specific path name can be used (eg: "/dev/hidraw0" on Linux).
     *
     * @param path The path name
     * @return The pointer if successful or null
     */
    NativeHidDevice openByPath(String path) throws IOException;

    /**
     * Get version of hidapi library
     *
     * @return Version in major.minor.patch format
     */
    String version();
}
