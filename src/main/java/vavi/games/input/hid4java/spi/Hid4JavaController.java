/*
 * Copyright (c) 2023 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.games.input.hid4java.spi;

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.Arrays;

import net.java.games.input.AbstractController;
import net.java.games.input.Component;
import net.java.games.input.Controller;
import net.java.games.input.Rumbler;
import net.java.games.input.usb.GenericDesktopUsageId;
import net.java.games.input.usb.HidController;
import net.java.games.input.usb.HidReportType;
import org.hid4java.HidDevice;
import vavi.util.StringUtil;

import static java.lang.System.getLogger;


/**
 * Hid4JavaController.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2023-09-18 nsano initial version <br>
 */
public class Hid4JavaController extends AbstractController implements HidController {

    private static final Logger logger = getLogger(Hid4JavaController.class.getName());

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
logger.log(Level.DEBUG,"device: " + device + ", " + device.isOpen());
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

        Hid4JavaComponent[] hid4JavaComponents = Arrays.stream(getComponents()).map(Hid4JavaComponent.class::cast).toArray(Hid4JavaComponent[]::new);
        Hid4JavaInputEvent hid4JavaInputEvent = new Hid4JavaInputEvent(this);

        device.open();
        device.addInputReportListener(event -> {
            byte[] data = event.getReport();
            fireOnInput(hid4JavaInputEvent.set(hid4JavaComponents, data));
        });
    }

    @Override
    public void close() throws IOException {
        device.close();
        super.close();
    }

    /** a composite device has same mid and pid for interfaces, this distinguishes them */
    HidDevice getDevice() {
        return device;
    }

    @Override
    public Type getType() {
        if ((device.getUsagePage() & 0xffff) == /* Generic Desktop Controls */ 0x01) {
            GenericDesktopUsageId usageId = GenericDesktopUsageId.map(device.getUsage() & 0xffff);
            if (usageId != null) {
                return switch (usageId) {
                    case KEYBOARD, KEYPAD -> Type.KEYBOARD;
                    case MOUSE -> Type.MOUSE;
                    case JOYSTICK -> Type.STICK;
                    default -> Type.GAMEPAD;
                };
            }
        }
        return Type.GAMEPAD;
    }

    @Override
    public void output(Report report) throws IOException {
        ((HidReport) report).setup(getRumblers());

        int reportId = ((HidReport) report).getReportId();
        byte[] data = ((HidReport) report).getData();
logger.log(Level.TRACE, "reportId: " + reportId + "\n" + StringUtil.getDump(data));
        int r = device.write(data, data.length, reportId);
        if (r == -1) {
            throw new IOException("write returns -1");
        }
    }

    @Override
    public byte[] getReportDescriptor() {
        try {
            byte[] descriptor = new byte[4096];
            int r = device.getReportDescriptor(descriptor);
            return r > 0 ? Arrays.copyOf(descriptor, r) : null;
        } catch (IOException | IllegalStateException e) {
logger.log(Level.DEBUG, "getReportDescriptor: " + e);
            return null;
        }
    }

    @Override
    public void writeReport(HidReportType type, int reportId, byte[] data) throws IOException {
        int r = switch (type) {
            case OUTPUT -> device.write(data, data.length, reportId);
            case FEATURE -> device.sendFeatureReport(data, reportId);
            default -> throw new IllegalArgumentException("an input report cannot be written");
        };
        if (r == -1) {
            throw new IOException("write %s report %d returns -1".formatted(type, reportId));
        }
    }

    @Override
    public int readReport(HidReportType type, int reportId, byte[] buffer) throws IOException {
        int r = switch (type) {
            case INPUT -> device.getInputReport(buffer, reportId);
            case FEATURE -> device.getFeatureReport(buffer, reportId);
            default -> throw new IllegalArgumentException("an output report cannot be read");
        };
        if (r == -1) {
            throw new IOException("read %s report %d returns -1".formatted(type, reportId));
        }
        // the count includes the report id, which is not in the buffer
        return Math.max(0, Math.min(r - 1, buffer.length));
    }
}
