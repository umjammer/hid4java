/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014-2020 Gary Rowe
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package org.hid4java.examples;

import java.io.IOException;

import org.hid4java.HidDevice;
import org.hid4java.HidDeviceManager;
import org.hid4java.HidManager;
import org.hid4java.HidServices;
import org.hid4java.HidServicesSpecification;
import org.junit.jupiter.api.Test;


/**
 * Demonstrate the USB HID interface with older libusb Linux library variant
 *
 * @since 0.7.0
 */
public class LibusbEnumerationExample extends BaseExample {

    public static void main(String[] args) throws Exception {
        LibusbEnumerationExample example = new LibusbEnumerationExample();
        example.executeExample();
    }

    @Test
    void executeExample() throws IOException {
        printPlatform();

        // Configure to use default specification
        HidServicesSpecification hidServicesSpecification = new HidServicesSpecification();

        // Set the libusb variant (only needed for older Linux platforms)
        hidServicesSpecification.useLibUsbVariant = true;

        // Get HID services using custom specification
        HidServices hidServices = HidManager.getHidServices(hidServicesSpecification);
        hidServices.addHidServicesListener(this);

        System.out.println(ANSI_GREEN + "Enumerating attached devices..." + ANSI_RESET);

        // Provide a list of attached devices
        for (HidDevice hidDevice : hidServices.getAttachedHidDevices()) {
            System.out.println(hidDevice);
        }

        waitAndShutdown(hidServices, 10);
    }
}
