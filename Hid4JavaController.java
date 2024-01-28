/*
 * Copyright (c) 2023 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.awt.joystick.hid4java;

import java.io.IOException;
import java.util.logging.Level;

import net.java.games.input.AbstractController;
import net.java.games.input.Component;
import net.java.games.input.Controller;
import net.java.games.input.Rumbler;
import net.java.games.input.usb.HidController;
import net.java.games.input.usb.HidRumbler;
import org.hid4java.HidDevice;
import vavi.util.Debug;
import vavi.util.StringUtil;


/**
 * Hid4JavaController.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2023-09-18 nsano initial version <br>
 */
public class Hid4JavaController extends AbstractController implements HidController {

    /** */
    private final HidDevice device;

    /**
     * Protected constructor for a controller containing the specified
     * axes, child controllers, and rumblers
     *
     * @param device     the controller
     * @param components components for the controller
     * @param children   child controllers for the controller
     * @param rumblers   rumblers for the controller
     */
    protected Hid4JavaController(HidDevice device, Component[] components, Controller[] children, Rumbler[] rumblers) {
        super(device.getManufacturer() + "/" + device.getProduct(), components, children, rumblers);
        this.device = device;
Debug.println("device: " + device + ", " + device.isOpen());
    }

    @Override
    public int getVendorId() {
        return device.getVendorId();
    }

    @Override
    public int getProductId() {
        return device.getProductId();
    }

    @Override
    public void open() throws IOException {
        super.open();

        device.open();
        device.addInputReportListener(event -> {
            byte[] data = event.getReport();
            fireOnInput(new Hid4JavaInputEvent(Hid4JavaController.this, getComponents(), data));
        });
    }

    @Override
    public void close() throws IOException {
        device.close();
        super.close();
    }

    @Override
    public Type getType() {
        return Type.GAMEPAD;
    }

    @Override
    public void output(Report report) throws IOException {
        ((HidReport) report).setup(getRumblers());

        int reportId = ((HidReport) report).getReportId();
        byte[] data = ((HidReport) report).getData();
Debug.println(Level.FINER, "reportId: " + reportId + "\n" + StringUtil.getDump(data));
        int r = device.write(data, data.length, reportId);
        if (r == -1) {
            throw new IOException("write returns -1");
        }
    }
}
