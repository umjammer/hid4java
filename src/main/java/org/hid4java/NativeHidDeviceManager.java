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
     * If serialNumber is NULL, the first device with the specified VID and PID is opened.
     *
     * @param vendorId     The vendor ID
     * @param productId    The product ID
     * @param serialNumber The serial number (or null for wildcard)
     * @return A pointer to a HidDevice on success or null on failure
     */
    NativeHidDevice open(int vendorId, int productId, String serialNumber) throws IOException;

    /**
     * Enumerate the HID Devices.
     * <p>
     * This function returns a linked list of all the HID devices attached to the system which match vendorId and productId.
     * <p>
     * If vendorId is set to 0 then any vendor matches. If productId is set to 0 then any product matches.
     * <p>
     * If vendorId and productId are both set to 0, then all HID devices will be returned.
     *
     * @param vendorId  The vendor ID
     * @param productId The product ID
     * @return A linked list of all discovered matching devices
     */
    List<HidDevice.Info> enumerate(int vendorId, int productId) throws IOException;

    /**
     * Open a HID device by its path name.
     * <p>
     * The path name be determined by calling hid_enumerate(), or a platform-specific path name can be used (eg: "/dev/hidraw0" on Linux).
     *
     * @param info The path name
     * @return The pointer if successful or null
     */
    NativeHidDevice open(HidDevice.Info info) throws IOException;
}
