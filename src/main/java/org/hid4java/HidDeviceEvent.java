/*
 * Copyright (c) 2023 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package org.hid4java;

import java.util.EventObject;


/**
 * HidDeviceEvent. Assuming reuse.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2023-10-08 nsano initial version <br>
 */
public class HidDeviceEvent extends EventObject {

    /** the report id number if used or zero */
    private int reportId;

    /** the report data that contains first report id */
    private byte[] report;
    private int length;

    public HidDeviceEvent(Object source) {
        super(source);
    }

    /** @param report a report data that contains first report id */
    public HidDeviceEvent set(int reportId, byte[] report, int length) {
        this.reportId = reportId;
        this.report = report;
        this.length = length;
        return this;
    }

    /** the report data that contains first report id */
    public int getReportId() {
        return reportId;
    }

    public byte[] getReport() {
        return report;
    }

    public int getLength() {
        return length;
    }
}
