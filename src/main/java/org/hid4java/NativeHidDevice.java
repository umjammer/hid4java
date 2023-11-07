package org.hid4java;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


/**
 * interface to act as hidapi for the underlying native library
 * This approach removes the need for any native library
 *
 * @since 0.1.0
 */
public interface NativeHidDevice {

    /** input report listeners */
    List<InputReportListener> inputReportListeners = new ArrayList<>();

    /** adds ReportInputListener */
    default void addInputReportListener(InputReportListener listener) {
        inputReportListeners.add(listener);
    }

    /** */
    default void fireOnInputReport(InputReportEvent event) {
        for (InputReportListener listener : inputReportListeners)
            listener.onInputReport(event);
    }

    /**
     * Starts a HID device event system.
     */
    void open() throws IOException;

    /**
     * Close a HID device
     */
    void close() throws IOException;

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
     * @param reportId The report ID (or (byte) 0x00)
     * @return The number of bytes read plus one for the report ID (which is still in the first byte)
     */
    int getFeatureReport(byte[] data, byte reportId) throws IOException;

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
     * @param reportId The report ID (or (byte) 0x00)
     * @return The actual number of bytes written, -1 on error
     */
    int sendFeatureReport(byte[] data, byte reportId) throws IOException;

    /**
     * Get a report descriptor from a HID device.
     * <p>
     * Since version 0.14.0, @ref HID_API_VERSION >= HID_API_MAKE_VERSION(0, 14, 0)
     * <p>
     * User has to provide a preallocated buffer where descriptor will be copied to.
     * The recommended size for preallocated buffer is @ref HID_API_MAX_REPORT_DESCRIPTOR_SIZE bytes.
     *
     * @param report The buffer to copy descriptor into.
     * @return This function returns non-negative number of bytes actually copied, or -1 on error.
     */
    int getReportDescriptor(byte[] report) throws IOException;

    /**
     * Get an input report from a HID device.
     * <p>
     * Set the first byte of @p data[] to the Report ID of the
     * report to be read. Make sure to allow space for this
     * extra byte in @p data[]. Upon return, the first byte will
     * still contain the Report ID, and the report data will
     * start in data[1].
     *
     * @param data   A buffer to put the read data into, including
     *               the Report ID. Set the first byte of @p data[] to the
     *               Report ID of the report to be read, or set it to zero
     *               if your device does not use numbered reports.
     * @return This function returns the number of bytes read plus one for the report ID
     * (which is still in the first byte), or -1 on error. Call hid_error(dev) to get the failure reason.
     * @since version 0.10.0
     */
    int getInputReport(byte[] data, byte reportId) throws IOException;
}
