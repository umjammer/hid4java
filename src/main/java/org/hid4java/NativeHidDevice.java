package org.hid4java;

import java.io.IOException;


/**
 * interface to act as hidapi for the underlying native library
 * This approach removes the need for any native library
 *
 * @since 0.1.0
 */
public interface NativeHidDevice {

    /** adds ReportInputListener */
    void addReportInputListener(InputReportListener listener);

    /**
     * Close a HID device
     */
    void close();

    /**
     * Write an Output report to a HID device.
     * <p>
     * The first byte of data[] must contain the Report ID. For devices which only support a single report, this must be set to 0x0.
     * The remaining bytes contain the report data.
     * <p>
     * Since the Report ID is mandatory, calls to hid_write() will always contain one more byte than the report contains.
     * <p>
     * For example, if a hid report is 16 bytes long, 17 bytes must be passed to hid_write(), the Report ID (or 0x0, for devices with
     * a single report), followed by the report data (16 bytes). In this example, the length passed in would be 17.
     * <p>
     * hid_write() will send the data on the first OUT endpoint, if one exists. If it does not, it will send the data through the
     * Control Endpoint (Endpoint 0).
     *
     * @param data     the data to send, including the report number as the first byte
     * @param len      The length in bytes of the data to send
     * @param reportId The report ID (or (byte) 0x00)
     * @return The actual number of bytes written, -1 on error
     */
    int write(byte[] data, int len, byte reportId) throws IOException;

    /**
     * Get a feature report from a HID device.
     * <p>
     * Set the first byte of data[] to the Report ID of the report to be read. Make sure to allow space for this extra byte in data[].
     * Upon return, the first byte will still contain the Report ID, and the report data will start in data[1].
     *
     * @param data     A buffer to put the read data into, including the Report ID. Set the first byte of data[] to the Report ID of the report to be read, or set it to zero if your device does not use numbered reports.
     * @param length   The number of bytes to read, including an extra byte for the report ID. The buffer can be longer than the actual report.
     * @param reportId The report ID (or (byte) 0x00)
     * @return The number of bytes read plus one for the report ID (which is still in the first byte)
     */
    int getFeatureReport(byte[] data, int length, byte reportId) throws IOException;

    /**
     * Send a Feature report to the device.
     * <p>
     * Feature reports are sent over the Control endpoint as a Set_Report transfer.
     * <p>
     * The first byte of data[] must contain the Report ID. For devices which only support a single report, this must be set to 0x0.
     * <p>
     * The remaining bytes contain the report data.
     * <p>
     * Since the Report ID is mandatory, calls to hid_send_feature_report() will always contain one more byte than the report contains.
     * <p>
     * For example, if a hid report is 16 bytes long, 17 bytes must be passed to hid_send_feature_report():
     * the Report ID (or 0x0, for devices which do not use numbered reports), followed by the report data (16 bytes).
     * In this example, the length passed in would be 17.
     *
     * @param data     The data to send, including the report number as the first byte
     * @param length   The length in bytes of the data to send, including the report number
     * @param reportId The report ID (or (byte) 0x00)
     * @return The actual number of bytes written, -1 on error
     */
    int sendFeatureReport(byte[] data, int length, byte reportId) throws IOException;

    /**
     * Get a string from a HID device, based on its string index.
     *
     * @param idx    The index of the string to get
     * @return 0 on success, -1 on failure
     */
    String getIndexedString(int idx) throws IOException;

    /**
     * Get the manufacturer string from a HID device
     */
    String getManufacturerString() throws IOException;

    /**
     * Get the product number string from a HID device
     */
    String getProductString() throws IOException;

    /**
     * Get the serial number string from a HID device
     */
    String getSerialNumberString() throws IOException;

    /**
     * Set the device handle to be non-blocking.
     * <p>
     * In non-blocking mode calls to hid_read() will return immediately with a value of 0 if there is no data to be read.
     * <p>
     * In blocking mode, hid_read() will wait (block) until there is data to read before returning.
     * <p>
     * Nonblocking can be turned on and off at any time.
     *
     * @param nonblock disables or enables non-blocking
     */
    void setNonblocking(boolean nonblock);
}