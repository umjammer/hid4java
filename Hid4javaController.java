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
import net.java.games.input.Event;
import net.java.games.input.Rumbler;
import org.hid4java.HidDevice;
import vavi.util.ByteUtil;
import vavi.util.Debug;


/**
 * Hid4javaController.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2023-09-18 nsano initial version <br>
 */
public class Hid4javaController extends AbstractController {

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
    protected Hid4javaController(HidDevice device, Component[] components, Controller[] children, Rumbler[] rumblers) {
        super(device.getManufacturer() + "/" + device.getProduct(), components, children, rumblers);
        this.device = device;

//        inputLength = device.getInputReportLength();
//        Runtime.getRuntime().addShutdownHook(new Thread(device::close)); // TODO crash
    }

    @Override
    public Type getType() {
        return Type.GAMEPAD;
    }

    @Override
    protected boolean getNextDeviceEvent(Event event) throws IOException {
Debug.println("getNextDeviceEvent: here");
        int l3x = data[1] & 0xff;
        int l3y = data[2] & 0xff;
        int r3x = data[3] & 0xff;
        int r3y = data[4] & 0xff;

        boolean tri	= (data[5] & 0x80) != 0;
        boolean cir	= (data[5] & 0x40) != 0;
        boolean x = (data[5] & 0x20) != 0;
        boolean sqr = (data[5] & 0x10) != 0;
        int dPad = data[5] & 0x0f;

        enum Hat {
            N("↑"), NE("↗"), E("→"), SE("↘"), S("↓"), SW("↙"), W("←"), NW("↖"), Released(" "); final String s; Hat(String s) { this.s = s; }
        }

        boolean r3 = (data[6] & 0x80) != 0;
        boolean l3 = (data[6] & 0x40) != 0;
        boolean opt = (data[6] & 0x20) != 0;
        boolean share = (data[6] & 0x10) != 0;
        boolean r2 = (data[6] & 0x08) != 0;
        boolean l2 = (data[6] & 0x04) != 0;
        boolean r1 = (data[6] & 0x02) != 0;
        boolean l1 = (data[6] & 0x01) != 0;

        int counter = (data[7] >> 2) & 0x3f;
        boolean tPad = (data[7] & 0x02) != 0;
        boolean ps = (data[7] & 0x01) != 0;

        int lTrigger = data[8] & 0xff;
        int rTrigger = data[9] & 0xff;

        int timestump = ByteUtil.readLeShort(data, 10) & 0xffff;
        int temperature = data[10] & 0xff;

        int gyroX = ByteUtil.readLeShort(data, 13) & 0xffff;
        int gyroY = ByteUtil.readLeShort(data, 15) & 0xffff;
        int gyroZ = ByteUtil.readLeShort(data, 17) & 0xffff;

        int accelX = ByteUtil.readLeShort(data, 19) & 0xffff;
        int accelY = ByteUtil.readLeShort(data, 21) & 0xffff;
        int accelZ = ByteUtil.readLeShort(data, 23) & 0xffff;

        boolean extension_detection = (data[30] & 0x01) != 0;
        int battery_info = (data[30] >> 3) & 0x1f;

System.out.printf("L3 x:%02x y:%02x R3 x:%02x y:%02x%n", l3x, l3y, r3x, r3y);
System.out.printf("%3s %3s %3s %3s %5s %2s %s%n", tri ? "▲" : "", cir ? "●" : "", x ? "✖" : "", sqr ? "■" : "", tPad ? "T-PAD" : "", ps ? "PS" : "", Hat.values()[dPad].s);
System.out.printf("gyro x:%04x y:%04x z:%04x, accel x:%04x y:%04x z:%04x%n%n", gyroX, gyroY, gyroZ, accelX, accelY, accelZ);

        event.set(null, 0, 0);
        return true;
    }

    /** */
    private byte[] data;

    /** Returns device manufacturer id */
    public int getManufacturerId() {
        return device.getVendorId();
    }

    /** Returns device product id */
    public int getProductId() {
        return device.getProductId();
    }

    /** */
    private int inputLength;

    @Override
    protected void pollDevice() throws IOException {
Debug.println("pollDevice: here");
        byte[] data = new byte[inputLength];
        switch (device.read(data)) {
        case -1 -> throw new IOException(device.getLastErrorMessage());
        case 0 -> Debug.println(Level.FINE, "no more poll data.");
        default -> this.data = data;
        }
    }
}
