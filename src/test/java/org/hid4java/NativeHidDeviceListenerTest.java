/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package org.hid4java;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * NativeHidDeviceListenerTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-16 nsano initial version <br>
 */
class NativeHidDeviceListenerTest {

    static class DummyDevice implements NativeHidDevice {
        @Override public void open() {}
        @Override public void close() {}
        @Override public int write(byte[] data, int len, byte reportId) { return 0; }
        @Override public int getFeatureReport(byte[] data, byte reportId) { return 0; }
        @Override public int sendFeatureReport(byte[] data, byte reportId) { return 0; }
        @Override public int getReportDescriptor(byte[] report) { return 0; }
        @Override public int getInputReport(byte[] data, byte reportId) { return 0; }
    }

    @Test
    @DisplayName("a report of a device must not be delivered to listeners of other devices")
    void test1() throws Exception {
        DummyDevice gamepad = new DummyDevice();
        DummyDevice keyboard = new DummyDevice();
        List<Object> gamepadReceived = new ArrayList<>();
        List<Object> keyboardReceived = new ArrayList<>();
        gamepad.addInputReportListener(e -> gamepadReceived.add(e.getSource()));
        keyboard.addInputReportListener(e -> keyboardReceived.add(e.getSource()));

        gamepad.fireOnInputReport(new HidDeviceEvent(gamepad).set(1, new byte[64], 64));
        gamepad.fireOnInputReport(new HidDeviceEvent(gamepad).set(1, new byte[64], 64));
        keyboard.fireOnInputReport(new HidDeviceEvent(keyboard).set(1, new byte[9], 9));

        assertEquals(List.of(gamepad, gamepad), gamepadReceived);
        assertEquals(List.of(keyboard), keyboardReceived);
    }
}
