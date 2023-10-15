/*
 * Copyright (c) 2023 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.awt.joystick.hid4java;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.logging.Level;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import net.java.games.input.Component;
import org.hid4java.HidDevice;
import org.hid4java.jna.HidDeviceStructure;
import org.hid4java.jna.WideStringBuffer;
import vavi.beans.BeanUtil;
import vavi.util.Debug;


/**
 * JNA library interface to act as the proxy for the underlying native library
 * This approach removes the need for any JNI or native code
 */
public interface HidApiLibraryEX extends Library {

    HidApiLibraryEX INSTANCE = Native.load("hidapi", HidApiLibraryEX.class);

    /** @see "https://www.usb.org/sites/default/files/hut1_22.pdf" */
    static String getUsagePagesString(int usagePage) {
        usagePage = usagePage & 0xffff; // TODO HidDevice#getUsagePage() should be unsigned short
        return switch (usagePage) {
        case 0x00 -> "Undefined";
        case 0x01 -> "Generic Desktop";
        case 0x02 -> "Simulation Controls";
        case 0x03 -> "VR Controls";
        case 0x04 -> "Sport Controls";
        case 0x05 -> "Game Controls";
        case 0x06 -> "Generic Device Controls";
        case 0x07 -> "Keyboard / Keypad Page";
        case 0x08 -> "LED";
        case 0x09 -> "Button";
        case 0x0A -> "Ordinal";
        case 0x0B -> "Telephony Device";
        case 0x0C -> "Consumer";
        case 0x0D -> "Digitizers";
        case 0x0E -> "Haptics";
        case 0x0F -> "PID";
        case 0x10 -> "Unicode";
        case 0x12 -> "Eye and Head Trackers";
        case 0x14 -> "Auxiliary Display";
        case 0x20 -> "Sensors";
        case 0x40 -> "Medical Instrument";
        case 0x41 -> "Braille Display";
        case 0x59 -> "Lighting And Illumination";
        case 0x80, 0x81, 0x82, 0x83 -> "Monitor";
        case 0x84, 0x85, 0x86, 0x87 -> "Power";
        case 0x8C -> "Bar Code Scanner";
        case 0x8D -> "Scale";
        case 0x8E -> "Magnetic Stripe Reading(MSR) Devices";
        case 0x8F -> "Reserved Point of Sale";
        case 0x90 -> "Camera Control";
        case 0x91 -> "Arcade";
        case 0x92 -> "Gaming Device";
        case 0xF1D0 -> "FIDO Alliance";
        default -> 0xFF00 <= usagePage && usagePage <= 0xFFFF ? "Vendor - defined" : "Reserved";
        };
    }

    /** 0x01 */
    enum GenericDesktopPage {
        UNKNOWN(0),
        /** Physical Collection */
        POINTER(0x01),
        /** Application Collection */
        MOUSE(0x02),
        // 0x03 Reserved
        /** Application Collection */
        JOYSTICK(0x04),
        /** Application Collection */
        GAME_PAD(0x05),
        /** Application Collection */
        KEYBOARD(0x06),
        /** Application Collection */
        KEYPAD(0x07),
        /** Application Collection */
        MULTI_AXIS_CONTROLLER(0x08),
        // 0x09 - 0x2F Reserved
        /** Dynamic Value */
        X(0x30),
        /** Dynamic Value */
        Y(0x31),
        /** Dynamic Value */
        Z(0x32),
        /** Dynamic Value */
        RX(0x33),
        /** Dynamic Value */
        RY(0x34),
        /** Dynamic Value */
        RZ(0x35),
        /** Dynamic Value */
        SLIDER(0x36),
        /** Dynamic Value */
        DIAL(0x37),
        /** Dynamic Value */
        WHEEL(0x38),
        /** Dynamic Value */
        HATSWITCH(0x39),
        /** Logical Collection */
        COUNTED_BUFFER(0x3A),
        /** Dynamic Value */
        BYTE_COUNT(0x3B),
        /** One-Shot Control */
        MOTION_WAKEUP(0x3C),
        /** On/Off Control */
        START(0x3D),
        /** On/Off Control */
        SELECT(0x3E),
        // 0x3F Reserved
        /** Dynamic Value */
        VX(0x40),
        /** Dynamic Value */
        VY(0x41),
        /** Dynamic Value */
        VZ(0x42),
        /** Dynamic Value */
        VBRX(0x43),
        /** Dynamic Value */
        VBRY(0x44),
        /** Dynamic Value */
        VBRZ(0x45),
        /** Dynamic Value */
        VNO(0x46),
        // 0x47 - 0x7F Reserved
        /** Application Collection */
        SYSTEM_CONTROL(0x80),
        /** One-Shot Control */
        SYSTEM_POWER_DOWN(0x81),
        /** One-Shot Control */
        SYSTEM_SLEEP(0x82),
        /** One-Shot Control */
        SYSTEM_WAKE_UP(0x83),
        /** One-Shot Control */
        SYSTEM_CONTEXT_MENU(0x84),
        /** One-Shot Control */
        SYSTEM_MAIN_MENU(0x85),
        /** One-Shot Control */
        SYSTEM_APP_MENU(0x86),
        /** One-Shot Control */
        SYSTEM_MENU_HELP(0x87),
        /** One-Shot Control */
        SYSTEM_MENU_EXIT(0x88),
        /** Selector */
        SYSTEM_MENU(0x89),
        /** Re-Trigger Control */
        SYSTEM_MENU_RIGHT(0x8A),
        /** Re-Trigger Control */
        SYSTEM_MENU_LEFT(0x8B),
        /** Re-Trigger Control */
        SYSTEM_MENU_UP(0x8C),
        /** Re-Trigger Control */
        SYSTEM_MENU_DOWN(0x8D),
        // 0x8E - 0x8F Reserved
        /** On/Off Control */
        DPAD_UP(0x90),
        /** On/Off Control */
        DPAD_DOWN(0x91),
        /** On/Off Control */
        DPAD_RIGHT(0x92),
        /** On/Off Control */
        DPAD_LEFT(0x93);
        /** 0x94 - 0xFFFF Reserved */

        private final int usage_id;

        GenericDesktopPage(int usage_id) {
            this.usage_id = usage_id;
        }

        /**
         * @throws java.util.NoSuchElementException no usage_id
         */
        static GenericDesktopPage valueOf(int usage_id) {
            return Arrays.stream(values()).filter(e -> e.usage_id == usage_id).findFirst().orElse(UNKNOWN);
        }

        public Component.Identifier toIdentifier() {
            if (this == X) {
                return Component.Identifier.Axis.X;
            } else if (this == Y) {
                return Component.Identifier.Axis.Y;
            } else if (this == Z || this == WHEEL) {
                return Component.Identifier.Axis.Z;
            } else if (this == RX) {
                return Component.Identifier.Axis.RX;
            } else if (this == RY) {
                return Component.Identifier.Axis.RY;
            } else if (this == RZ) {
                return Component.Identifier.Axis.RZ;
            } else if (this == SLIDER) {
                return Component.Identifier.Axis.SLIDER;
            } else if (this == HATSWITCH) {
                return Component.Identifier.Axis.POV;
            } else if (this == SELECT) {
                return Component.Identifier.Button.SELECT;
            } else
                throw new IllegalStateException("not defined: " + usage_id);
        }
    }

    /**
     * Maximum expected HID Report descriptor size in bytes.
     *
     * @since version 0.13.0
     */
    int HID_API_MAX_REPORT_DESCRIPTOR_SIZE = 4096;

    /**
     * Get an input report from a HID device.
     * <p>
     * Set the first byte of @p data[] to the Report ID of the
     * report to be read. Make sure to allow space for this
     * extra byte in @p data[]. Upon return, the first byte will
     * still contain the Report ID, and the report data will
     * start in data[1].
     *
     * @param dev    A device handle returned from hid_open().
     * @param data   A buffer to put the read data into, including
     *               the Report ID. Set the first byte of @p data[] to the
     *               Report ID of the report to be read, or set it to zero
     *               if your device does not use numbered reports.
     * @param length The number of bytes to read, including an
     *               extra byte for the report ID. The buffer can be longer
     *               than the actual report.
     * @return This function returns the number of bytes read plus one for the report ID
     * (which is still in the first byte), or -1 on error. Call hid_error(dev) to get the failure reason.
     * @since version 0.10.0
     */
    int hid_get_input_report(Pointer dev, WideStringBuffer.ByReference data, int length);

    /**
     * Get a report descriptor from a HID device.
     * <p>
     * User has to provide a preallocated buffer where descriptor will be copied to.
     * The recommended size for preallocated buffer is @ref HID_API_MAX_REPORT_DESCRIPTOR_SIZE bytes.
     *
     * @param device A device handle returned from hid_open().
     * @param data   The buffer to copy descriptor into.
     * @param length The size of the buffer in bytes.
     * @return This function returns non-negative number of bytes actually copied, or -1 on error.
     * @since version 0.14.0
     */
    int hid_get_report_descriptor(Pointer device, WideStringBuffer.ByReference data, int length);

    /**
     * @param device needs to be opened
     */
    static byte[] getDescriptor(HidDevice device) {
        HidDeviceStructure deviceStructure = (HidDeviceStructure) BeanUtil.getValue("hidDeviceStructure", device);
        assert deviceStructure != null : "devise must be opened";
Debug.println(Level.FINER, deviceStructure);

        byte[] d = new byte[HidApiLibraryEX.HID_API_MAX_REPORT_DESCRIPTOR_SIZE];
        WideStringBuffer report = new WideStringBuffer(HidApiLibraryEX.HID_API_MAX_REPORT_DESCRIPTOR_SIZE);
        int r = HidApiLibraryEX.INSTANCE.hid_get_report_descriptor(deviceStructure.ptr(), report, d.length);
        if (r == -1) throw new IllegalStateException("hid_get_report_descriptor");

        System.arraycopy(report.buffer, 0, d, 0, Math.min(r, d.length));
        return d;
    }

    /**
     * @param device needs to be opened
     */
    static byte[] getInputDescriptor(HidDevice device, byte reportId) {
        HidDeviceStructure deviceStructure = (HidDeviceStructure) BeanUtil.getValue("hidDeviceStructure", device);
        assert deviceStructure != null : "devise must be opened";
Debug.println(Level.FINER, deviceStructure);

        byte[] d = new byte[HidApiLibraryEX.HID_API_MAX_REPORT_DESCRIPTOR_SIZE];
        WideStringBuffer report = new WideStringBuffer(HidApiLibraryEX.HID_API_MAX_REPORT_DESCRIPTOR_SIZE + 1);
        report.buffer[0] = reportId;
        int r = HidApiLibraryEX.INSTANCE.hid_get_input_report(deviceStructure.ptr(), report, d.length + 1);
        if (r == -1) throw new IllegalStateException("hid_get_input_report");

        System.arraycopy(report.buffer, 1, d, 0, Math.min(r, d.length));
        return d;
    }
}