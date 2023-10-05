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

    /** the report Id number if used or zero */
    private final int reportId;

    /** the report data */
    private final byte[] report;

    public InputReportEvent(Object source, int reportId, byte[] report) {
        super(source);
        this.reportId = reportId;
        this.report = report;
    }

    public int getReportId() {
        return reportId;
    }

    public byte[] getReport() {
        return report;
    }
}
