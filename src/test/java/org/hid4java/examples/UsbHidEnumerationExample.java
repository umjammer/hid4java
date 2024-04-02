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

import net.java.games.input.osx.plugin.DualShock4Plugin;
import org.hid4java.HidDevice;
import org.hid4java.HidDevices;
import org.hid4java.HidSpecification;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;


/**
 * Demonstrate the USB HID interface using a Satoshi Labs Trezor
 *
 * @since 0.0.1
 */
@PropsEntity(url = "file:local.properties")
public class UsbHidEnumerationExample extends BaseExample {

    @Property(name = "mid")
    String mid;
    @Property(name = "pid")
    String pid;

    int vendorId;
    int productId;

    public static void main(String[] args) throws Exception {
        UsbHidEnumerationExample example = new UsbHidEnumerationExample();
        PropsEntity.Util.bind(example);
        example.vendorId = Integer.decode(example.mid);
        example.productId = Integer.decode(example.pid);
        example.executeExample();
    }

    private void executeExample() throws IOException {

        printPlatform();

        // Configure to use custom specification
        HidSpecification hidSpecification = new HidSpecification();
        // Use the v0.7.0 manual start feature to get immediate attach events
        hidSpecification.setAutoStart(false);
        hidSpecification.setAutoShutdown(false);

        // Get HID services using custom specification
        HidDevices hidDevices = new HidDevices(hidSpecification);
        hidDevices.addHidServicesListener(this);

        // Manually start the services to get attachment event
        System.out.println(ANSI_GREEN + "Manually starting HID services." + ANSI_RESET);
        hidDevices.start();

        System.out.println(ANSI_GREEN + "Enumerating attached devices..." + ANSI_RESET);

        // Provide a list of attached devices
        for (HidDevice hidDevice : hidDevices.getHidDevices()) {
            System.out.printf("%s/%s ... %x%n", hidDevice.getManufacturer(), hidDevice.getProduct(), hidDevice.getUsagePage());
        }

        HidDevice device = hidDevices.getHidDevice(vendorId, productId, null);
        device.open();

        device.addInputReportListener(e -> display(e.getReport()));

        waitAndShutdown(hidDevices, 10);
    }

    static void display(byte[] data) {
        DualShock4Plugin.display(data, System.out);
    }

    static void display1(byte[] data) {
        System.out.print("< [");
        for (int i = 0; i < 25; i++) {
            System.out.printf(" %02x", data[i]);
        }
        System.out.println("]");
    }
}