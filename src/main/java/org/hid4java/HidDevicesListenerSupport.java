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

package org.hid4java;

import java.util.ArrayList;
import java.util.List;


/**
 * HID services listener list
 *
 * @since 0.0.1
 */
public class HidDevicesListenerSupport {

    /**
     * The list with registered listeners
     */
    private final List<HidDevicesListener> listeners = new ArrayList<>();

    /**
     * @param listener The listener to add (same instance are not duplicated)
     */
    public final void add(HidDevicesListener listener) {
        if (this.listeners.contains(listener)) {
            return;
        }
        this.listeners.add(listener);
    }

    /**
     * @param listener The listener to remove
     */
    public final void remove(HidDevicesListener listener) {
        this.listeners.remove(listener);
    }

    /**
     * Removes all listeners
     */
    public final void clear() {
        this.listeners.clear();
    }

    /**
     * Fire the HID device attached event
     *
     * @param hidDevice The device that was attached
     */
    public void fireHidDeviceAttached(HidDevice hidDevice) {
        HidDevicesEvent event = new HidDevicesEvent(hidDevice);

        for (HidDevicesListener listener : listeners) {
            listener.hidDeviceAttached(event);
        }
    }

    /**
     * Fire the HID device detached event
     *
     * @param hidDevice The device that was detached
     */
    public void fireHidDeviceDetached(HidDevice hidDevice) {
        HidDevicesEvent event = new HidDevicesEvent(hidDevice);

        for (HidDevicesListener listener : listeners) {
            listener.hidDeviceDetached(event);
        }
    }
}

