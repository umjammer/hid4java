/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package org.hid4java;

import java.util.Arrays;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * HidDeviceReportTest. {@link HidDevice} speaks report bodies, {@link NativeHidDevice}
 * speaks hidapi's buffers with the report id in front.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-10-07 nsano initial version <br>
 */
class HidDeviceReportTest {

    /** answers a feature report as hidapi does, keeps what was sent */
    static class Native extends NativeHidDeviceListenerTest.DummyDevice {
        byte[] sent;

        @Override public int getFeatureReport(byte[] data, byte reportId) {
            assertEquals(reportId, data[0]);
            data[1] = 0x21;
            data[2] = 0x28;
            return 3; // id + 2 bytes
        }

        @Override public int sendFeatureReport(byte[] data, byte reportId) {
            sent = data.clone();
            return data.length;
        }

        @Override public int getInputReport(byte[] data, byte reportId) {
            return getFeatureReport(data, reportId);
        }
    }

    @Test
    @DisplayName("the report id is put in front and taken off")
    void test1() throws Exception {
        Native nativeDevice = new Native();
        HidDevice.Info info = new HidDevice.Info();
        info.path = "test";
        HidDevice device = new HidDevice(info, nativeDevice, () -> {});

        byte[] buffer = new byte[4];
        assertEquals(3, device.getFeatureReport(buffer, 3));
        assertArrayEquals(new byte[] {0x21, 0x28, 0, 0}, buffer);

        Arrays.fill(buffer, (byte) 0);
        assertEquals(3, device.getInputReport(buffer, 1));
        assertArrayEquals(new byte[] {0x21, 0x28, 0, 0}, buffer);

        assertEquals(3, device.sendFeatureReport(new byte[] {1, 2}, (byte) 0xf0));
        assertArrayEquals(new byte[] {(byte) 0xf0, 1, 2}, nativeDevice.sent);
    }
}
