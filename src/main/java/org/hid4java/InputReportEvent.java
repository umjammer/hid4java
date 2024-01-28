/*
 * Copyright (c) 2023 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package org.hid4java;

import java.util.EventObject;


/**
 * InputReportEvent.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2023-10-08 nsano initial version <br>
 */
public class InputReportEvent extends EventObject {

    /** the report id number if used or zero */
    private final int reportId;

    /** the report data that contains first report id */
    private final byte[] report;
    private final int length;

    /** @param report a report data that contains first report id */
    public InputReportEvent(Object source, int reportId, byte[] report, int length) {
        super(source);
        this.reportId = reportId;
        this.report = report;
        this.length = length;
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
