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
import org.hid4java.HidManager;
import org.hid4java.HidServices;
import org.hid4java.HidServicesSpecification;
import vavi.util.ByteUtil;
import vavi.util.Debug;


/**
 * Demonstrate the USB HID interface using a Satoshi Labs Trezor
 *
 * @since 0.0.1
 */
public class UsbHidEnumerationExample extends BaseExample {

    public static void main(String[] args) throws Exception {
        UsbHidEnumerationExample example = new UsbHidEnumerationExample();
        example.executeExample();
    }

    private void executeExample() throws IOException {

        printPlatform();

        // Configure to use custom specification
        HidServicesSpecification hidServicesSpecification = new HidServicesSpecification();
        // Use the v0.7.0 manual start feature to get immediate attach events
        hidServicesSpecification.setAutoStart(false);
        hidServicesSpecification.setAutoShutdown(false);

        // Get HID services using custom specification
        HidServices hidServices = HidManager.getHidServices(hidServicesSpecification);
        hidServices.addHidServicesListener(this);

        // Manually start the services to get attachment event
        System.out.println(ANSI_GREEN + "Manually starting HID services." + ANSI_RESET);
        hidServices.start();

        System.out.println(ANSI_GREEN + "Enumerating attached devices..." + ANSI_RESET);

        // Provide a list of attached devices
        for (HidDevice hidDevice : hidServices.getAttachedHidDevices()) {
            System.out.printf("%s/%s ... %x%n", hidDevice.getManufacturer(), hidDevice.getProduct(), hidDevice.getUsagePage());
        }

        HidDevice device = hidServices.getHidDevice(0x54c, 0x9cc, null);
Debug.println(device + "\nopen?: " + !device.isClosed());
//        boolean r = device.open();
//Debug.println("open device: " + r);

        device.setInputReportListener(e -> display(e.getReport()));

        waitAndShutdown(hidServices);
    }

    static void display(byte[] data) {
        display2(data);
    }

    static void display1(byte[] data) {
        System.out.print("< [");
        for (int i = 0; i < 25; i++) {
            System.out.printf(" %02x", data[i]);
        }
        System.out.println("]");
    }

    static int c = 0;

    static void display2(byte[] d) {
        int l3x = d[1] & 0xff;
        int l3y = d[2] & 0xff;
        int r3x = d[3] & 0xff;
        int r3y = d[4] & 0xff;

        boolean tri	= (d[5] & 0x80) != 0;
        boolean cir	= (d[5] & 0x40) != 0;
        boolean x = (d[5] & 0x20) != 0;
        boolean sqr = (d[5] & 0x10) != 0;
        int dPad = d[5] & 0x0f;

        enum Hat {
            N("↑"), NE("↗"), E("→"), SE("↘"), S("↓"), SW("↙"), W("←"), NW("↖"), Released(" "); final String s; Hat(String s) { this.s = s; }
        }

        boolean r3 = (d[6] & 0x80) != 0;
        boolean l3 = (d[6] & 0x40) != 0;
        boolean opt = (d[6] & 0x20) != 0;
        boolean share = (d[6] & 0x10) != 0;
        boolean r2 = (d[6] & 0x08) != 0;
        boolean l2 = (d[6] & 0x04) != 0;
        boolean r1 = (d[6] & 0x02) != 0;
        boolean l1 = (d[6] & 0x01) != 0;

        int counter = (d[7] >> 2) & 0x3f;
        boolean tPad = (d[7] & 0x02) != 0;
        boolean ps = (d[7] & 0x01) != 0;

        int lTrigger = d[8] & 0xff;
        int rTrigger = d[9] & 0xff;

        int timestump = ByteUtil.readLeShort(d, 10) & 0xffff;
        int temperature = d[10] & 0xff;

        int gyroX = ByteUtil.readLeShort(d, 13) & 0xffff;
        int gyroY = ByteUtil.readLeShort(d, 15) & 0xffff;
        int gyroZ = ByteUtil.readLeShort(d, 17) & 0xffff;

        int accelX = ByteUtil.readLeShort(d, 19) & 0xffff;
        int accelY = ByteUtil.readLeShort(d, 21) & 0xffff;
        int accelZ = ByteUtil.readLeShort(d, 23) & 0xffff;

        boolean extension_detection = (d[30] & 0x01) != 0;
        int battery_info = (d[30] >> 3) & 0x1f;

        System.out.printf("L3 x:%02x y:%02x R3 x:%02x y:%02x (%d)%n", l3x, l3y, r3x, r3y, c++);
        System.out.printf("%3s %3s %3s %3s %5s %2s %s%n", tri ? "▲" : "", cir ? "●" : "", x ? "✖" : "", sqr ? "■" : "", tPad ? "T-PAD" : "", ps ? "PS" : "", Hat.values()[dPad].s);
        System.out.printf("gyro x:%04x y:%04x z:%04x, accel x:%04x y:%04x z:%04x%n%n", gyroX, gyroY, gyroZ, accelX, accelY, accelZ);
    }
}