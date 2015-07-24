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

import org.hid4java.event.HidServicesEvent;

import java.util.EventListener;

/**
 * <p>Interface to provide the following to API consumers:</p>
 * <ul>
 * <li>Notification of a HID event</li>
 * </ul>
 *
 * @since 0.0.1
 *  
 */
public interface HidServicesListener extends EventListener {
  /**
   * A HID device was attached
   *
   * @param event The event
   */
  public void hidDeviceAttached(HidServicesEvent event);

  /**
   * A HID device was detached
   *
   * @param event The event
   */
  public void hidDeviceDetached(HidServicesEvent event);

  /**
   * A HID failure occurred during scanning
   *
   * @param event The event
   */
  public void hidFailure(HidServicesEvent event);

}