/*
 * Copyright (c) 2023 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.awt.joystick.hid4java;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.logging.Level;

import net.java.games.input.AbstractComponent;
import net.java.games.input.AbstractController;
import net.java.games.input.Component;
import net.java.games.input.Controller;
import net.java.games.input.Event;
import net.java.games.input.Rumbler;
import net.java.games.input.osx.plugin.DualShock4Plugin;
import net.java.games.input.usb.HidController;
import org.hid4java.HidDevice;
import vavi.util.Debug;


/**
 * Hid4JavaController.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2023-09-18 nsano initial version <br>
 */
public class Hid4JavaController extends AbstractController implements HidController {

    /** */
    private final HidDevice device;

    Deque<byte[]> reports = new ArrayDeque<>(EVENT_QUEUE_DEPTH);

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
    public void open() throws IOException {
        // not opened device is not set a device pointer itself.
Debug.println("open?: " + device.isOpen());
        if (!device.isOpen()) {
            device.open();
            super.open();
        }
Debug.println("open2?: " + device.isOpen());

        device.addInputReportListener(event -> {
//Debug.println(">>> event: " + e.getReportId() + ", " + Thread.currentThread().getName());
            while (reports.size() > EVENT_QUEUE_DEPTH) {
                if (reports.peek() != null)
                    reports.pop();
            }
            reports.push(event.getReport());
//Debug.println(">>> event: " + reports.size());
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
    protected boolean getNextDeviceEvent(Event event) throws IOException {
Debug.println(Level.FINER, "getNextDeviceEvent: " + reports.size());
        if (reports.peek() == null) {
            return false;
        }
        byte[] data = reports.poll();
        DualShock4Plugin.display(data);

        event.set(new AbstractComponent("dummy", Component.Identifier.Button.A) {
            @Override public boolean isRelative() {
                return false;
            }
            @Override protected float poll() throws IOException {
                return 0;
            }
        }, 0, 0);
        return true;
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
    protected void pollDevice() throws IOException {
Debug.println(Level.FINER, "pollDevice: reports: " + reports.size());
    }
}
