/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014-2015 Gary Rowe
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

package org.hid4java.event;

import java.util.Arrays;

import org.hid4java.HidDevice;


/**
 * Event to provide the following to API consumers:
 * <ul>
 * <li>Provision of HID device information</li>
 * </ul>
 *
 * @since 0.0.1
 */
public class HidServicesEvent {

    private final HidDevice hidDevice;
    private final byte[] dataReceived;

    /**
     * @param device The HidDevice involved in the event
     */
    public HidServicesEvent(HidDevice device) {
        hidDevice = device;
        dataReceived = null;
    }

    /**
     * @param device       The HidDevice involved in the event
     * @param dataReceived The contents of all data read
     * @since 0.8.0
     */
    public HidServicesEvent(HidDevice device, byte[] dataReceived) {
        hidDevice = device;
        this.dataReceived = Arrays.copyOf(dataReceived, dataReceived.length);
    }

    /**
     * @return The associated HidDevice
     */
    public HidDevice getHidDevice() {
        return hidDevice;
    }

    /**
     * @return The data received (might be multiple packets of data)
     */
    public byte[] getDataReceived() {
        return dataReceived;
    }

    @Override
    public String toString() {
        return "HidServicesEvent{" +
                "hidDevice=" + hidDevice +
                '}';
    }
}
