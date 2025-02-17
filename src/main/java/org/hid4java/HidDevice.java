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

import java.io.IOException;
import java.util.Arrays;
import java.util.StringJoiner;
import java.util.logging.Logger;


/**
 * High level wrapper to provide the following to API consumers:
 *
 * <ul>
 * <li>Simplified access to the underlying JNA HidDeviceStructure</li>
 * </ul>
 *
 * @since 0.0.1
 */
public class HidDevice {

    private static final Logger logger = Logger.getLogger(HidDevice.class.getName());

    private final Runnable afterWrite;
    private final Info info;
    private final NativeHidDevice nativeDevice;

    private boolean isOpen;

    /**
     * Maximum expected HID Report descriptor size in bytes.
     *
     * @since version 0.13.0
     */
    static int HID_API_MAX_REPORT_DESCRIPTOR_SIZE = 4096;

    public static class Info {

        public enum HidBusType {
            /** Unknown bus type */
            BUS_UNKNOWN,
            /**
             * USB bus
             *
             * @see "https://usb.org/hid"
             */
            BUS_USB,
            /**
             * Bluetooth or Bluetooth LE bus
             *
             * @see "https://www.bluetooth.com/specifications/specs/human-interface-device-profile-1-1-1/"
             * @see "https://www.bluetooth.com/specifications/specs/hid-service-1-0/"
             * @see "https://www.bluetooth.com/specifications/specs/hid-over-gatt-profile-1-0/"
             */
            BUS_BLUETOOTH,
            /**
             * I2C bus
             *
             * @see "https://docs.microsoft.com/previous-versions/windows/hardware/design/dn642101(v=vs.85)"
             */
            BUS_I2C,
            /**
             * SPI bus
             *
             * @see "https://www.microsoft.com/download/details.aspx?id=103325"
             */
            BUS_SPI
        }

        /** Platform-specific device path */
        public String path;
        /** Device Vendor ID */
        public int vendorId;
        /** Device Product ID */
        public int productId;
        /** Serial Number */
        public String serialNumber;
        /**
         * Device Release Number in binary-coded decimal,
         * also known as Device Version Number
         */
        public int releaseNumber;
        /** Manufacturer String */
        public String manufacturer;
        /** Product string */
        public String product;
        /**
         * Usage Page for this Device/Interface
         * (Windows/Mac/hidraw only)
         */
        public int usagePage;
        /**
         * Usage for this Device/Interface
         * (Windows/Mac/hidraw only)
         */
        public int usage;
        /**
         * The USB interface which this logical device
         * represents.
         * <p>
         * Valid only if the device is a USB HID device.
         * Set to -1 in all other cases.
         */
        public int interfaceNumber;

        /**
         * Underlying bus type
         * Since version 0.13.0, @ref HID_API_VERSION >= HID_API_MAKE_VERSION(0, 13, 0)
         */
        public HidBusType busType;

        @Override public String toString() {
            return new StringJoiner(", ", Info.class.getSimpleName() + "[", "]")
                    .add("path='" + path + "'")
                    .add("vendorId=" + vendorId)
                    .add("productId=" + productId)
                    .add("serialNumber='" + serialNumber + "'")
                    .add("releaseNumber=" + releaseNumber)
                    .add("manufacturer='" + manufacturer + "'")
                    .add("product='" + product + "'")
                    .add("usagePage=" + usagePage)
                    .add("usage=" + usage)
                    .add("interfaceNumber=" + interfaceNumber)
                    .add("busType=" + busType)
                    .toString();
        }
    }

    /**
     * @param info          The HID device info structure providing details
     * @param afterWrite The HID device afterWrite providing access to device enumeration for post IO scanning
     * @since 0.1.0
     */
    public HidDevice(Info info, NativeHidDevice nativeDevice, Runnable afterWrite) throws IOException {

        this.afterWrite = afterWrite;

        this.info = info;

        // Note that the low-level HidDeviceInfoStructure is directly written to by
        // the JNA library and implies an unsigned short which is not available in Java.
        // The bitmask converts from [-32768, 32767] to [0,65535]
        // In Java 8 Short.toUnsignedInt() is available.
        this.info.vendorId = this.info.vendorId & 0xffff;
        this.info.productId = this.info.productId & 0xffff;

        this.nativeDevice = nativeDevice;
logger.finest(getPath() + "(@" + hashCode() + "): " + nativeDevice);
    }

    /**
     * The "path" is well-supported across Windows, Mac and Linux so makes a
     * better choice for a unique ID
     * <p>
     * See #8 for details
     *
     * @return A unique device ID made up from vendor ID, product ID and serial number
     * @since 0.1.0
     */
    public String getId() {
        return info.path;
    }

    /**
     * @return The device path
     * @since 0.1.0
     */
    public String getPath() {
        return info.path;
    }

    /**
     * @return Int version of vendor ID
     * @since 0.1.0
     */
    public int getVendorId() {
        return info.vendorId;
    }

    /**
     * @return Int version of product ID
     * @since 0.1.0
     */
    public int getProductId() {
        return info.productId;
    }

    /**
     * @return The device serial number
     * @since 0.1.0
     */
    public String getSerialNumber() {
        return info.serialNumber;
    }

    /**
     * @return The release number
     * @since 0.1.0
     */
    public int getReleaseNumber() {
        return info.releaseNumber;
    }

    /**
     * @return The manufacturer
     * @since 0.1.0
     */
    public String getManufacturer() {
        return info.manufacturer;
    }

    /**
     * @return The product
     * @since 0.1.0
     */
    public String getProduct() {
        return info.product;
    }

    /**
     * @return The usage page
     * @since 0.1.0
     */
    public int getUsagePage() {
        return info.usagePage;
    }

    /**
     * @return The usage information
     * @since 0.1.0
     */
    public int getUsage() {
        return info.usage;
    }

    public int getInterfaceNumber() {
        return info.interfaceNumber;
    }

    /**
     * Starts device event system.
     *
     * @since 0.1.0
     */
    public void open() throws IOException {
        nativeDevice.open();
        isOpen = true;
    }

    /**
     * @return True if the device event system started
     * @since 0.1.0
     */
    public boolean isOpen() {
        return isOpen;
    }

    /**
     * Close this device freeing the HidDeviceManager resources
     *
     * @since 0.1.0
     */
    public void close() throws IOException {
logger.finest("close native: " + nativeDevice);
        // Close the Hidapi reference
        nativeDevice.close();
        isOpen = false;
    }

    /**
     * Sets input report listener.
     */
    public void addInputReportListener(HidDeviceListener l) {
        nativeDevice.addInputReportListener(l);
    }

    /**
     * Get a feature report from a HID device
     * <p>
     * Under the covers the HID library will set the first byte of data[] to the
     * Report ID of the report to be read. Upon return, the first byte will
     * still contain the Report ID, and the report data will start in data[1]
     * <p>
     * This method handles all the wide string and array manipulation for you
     *
     * @param data     The buffer to contain the report
     * @param reportId The report ID (or (byte) 0x00)
     * @return The number of bytes read plus one for the report ID (which has
     * been removed from the first byte), or -1 on error.
     * @since 0.1.0
     */
    public int getFeatureReport(byte[] data, int reportId) throws IOException {
        return nativeDevice.getFeatureReport(data, (byte) reportId);
    }

    /**
     * Send a Feature report to the device
     * <p>
     * Under the covers, feature reports are sent over the Control endpoint as a
     * Set_Report transfer. The first byte of data[] must contain the Report ID.
     * For devices which only support a single report, this must be set to 0x0.
     * The remaining bytes contain the report data
     * <p>
     * Since the Report ID is mandatory, calls to hid_send_feature_report() will
     * always contain one more byte than the report contains. For example, if a
     * hid report is 16 bytes long, 17 bytes must be passed to
     * hid_send_feature_report(): the Report ID (or 0x0, for devices which do
     * not use numbered reports), followed by the report data (16 bytes). In
     * this example, the length passed in would be 17
     * <p>
     * This method handles all the array manipulation for you
     *
     * @param data     The feature report data (will be widened and have the report
     *                 ID pre-pended)
     * @param reportId The report ID (or (byte) 0x00)
     * @return This function returns the actual number of bytes written and -1
     * on error.
     * @since 0.1.0
     */
    public int sendFeatureReport(byte[] data, int reportId) throws IOException {
        return nativeDevice.sendFeatureReport(data, (byte) reportId);
    }

    /**
     * Get a string from a HID device, based on its string index
     *
     * @param index The index
     * @return The string
     * @since 0.1.0
     */
    public String getIndexedString(int index) throws IOException {
        throw new UnsupportedOperationException("getIndexedString: not available on this platform");
    }

    /**
     * Write the message to the HID API without zero byte padding.
     * <p>
     * Note that the report ID will be prefixed to the HID packet as per HID rules.
     *
     * @param message      The message
     * @param packetLength The packet length
     * @param reportId     The report ID (will be prefixed to the HID packet)
     * @return The number of bytes written (including report ID), or -1 if an error occurs
     * @since 0.1.0
     */
    public int write(byte[] message, int packetLength, int reportId) throws IOException {
        return write(message, packetLength, (byte) reportId, false);
    }

    /**
     * Write the message to the HID API with optional zero byte padding to packet length.
     * <p>
     * Note that the report ID will be prefixed to the HID packet as per HID rules.
     *
     * @param message      The message
     * @param packetLength The packet length
     * @param reportId     The report ID
     * @param applyPadding True if the message should be filled with zero bytes to the packet length
     * @return The number of bytes written (including report ID), or -1 if an error occurs
     * @since 0.8.0
     */
    public int write(byte[] message, int packetLength, int reportId, boolean applyPadding) throws IOException {
        if (applyPadding) {
            message = Arrays.copyOf(message, packetLength + 1);
        }

        int result = nativeDevice.write(message, packetLength, (byte) reportId);
        // Update HID afterWrite
        afterWrite.run();
        return result;
    }

    /**
     * @param vendorId     The vendor ID
     * @param productId    The product ID
     * @param serialNumber The serial number
     * @return True if the device matches the given the combination with vendorId, productId being zero acting as a wildcard
     * @since 0.1.0
     */
    public boolean isVidPidSerial(int vendorId, int productId, String serialNumber) {
        if (serialNumber == null)
            return (vendorId == 0 || this.info.vendorId == vendorId)
                    && (productId == 0 || this.info.productId == productId);
        else
            return (vendorId == 0 || this.info.vendorId == vendorId)
                    && (productId == 0 || this.info.productId == productId)
                    && (this.info.serialNumber.equals(serialNumber));
    }

    /** */
    public int getReportDescriptor(byte[] report) throws IOException {
        return nativeDevice.getReportDescriptor(report);
    }

    /** */
    public int getInputDescriptor(byte[] report, int reportId) throws IOException {
        return nativeDevice.getInputReport(report, (byte) reportId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        HidDevice hidDevice = (HidDevice) o;

        return getPath().equals(hidDevice.getPath());

    }

    @Override
    public int hashCode() {
        return info.path.hashCode();
    }

    @Override
    public String toString() {
        return "HidDevice [path=" + info.path
                + String.format(", usagePage=0x%04x", info.usagePage)
                + String.format(", usage=0x%04x", info.usage)
                + String.format(", vendorId=0x%04x", info.vendorId)
                + String.format(", productId=0x%04x", info.productId)
                + ", product=" + info.product
                + ", manufacturer=" + info.manufacturer
                + ", interfaceNumber=" + info.interfaceNumber
                + ", serialNumber=" + info.serialNumber
                + ", releaseNumber=0x" + Integer.toHexString(info.releaseNumber)
                + "]";
    }

    /**
     * @param buffer  The buffer to serialise for traffic
     * @param isWrite True if writing (from host to device)
     */
    public static String logTraffic(byte[] buffer, boolean isWrite) {
        StringBuilder sb = new StringBuilder();
        if (buffer != null && buffer.length > 0) {
            if (isWrite) {
                sb.append("> ");
            } else {
                sb.append("< ");
            }
            sb.append(String.format("[%02x]:", buffer.length));
            for (byte b : buffer) {
                sb.append(String.format(" %02x", b));
            }
            sb.append('\n');
        }
        return sb.toString();
    }
}
