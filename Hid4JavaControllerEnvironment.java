/*
 * Copyright (c) 2023 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.awt.joystick.hid4java;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

import net.java.games.input.Component;
import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;
import net.java.games.input.Rumbler;
import org.hid4java.HidDevice;
import org.hid4java.HidManager;
import org.hid4java.HidServices;
import org.hid4java.HidServicesListener;
import org.hid4java.HidServicesSpecification;
import org.hid4java.event.HidServicesEvent;
import vavi.awt.joystick.hid4java.HidApiLibraryEX.GenericDesktopPage;
import vavi.usb.UsbUtil;
import vavi.util.Debug;


/**
 * The Hid4Java ControllerEnvironment.
 *
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 230927 nsano initial version <br>
 */
public final class Hid4JavaControllerEnvironment extends ControllerEnvironment {

    /** */
    private final List<Hid4javaController> controllers = new ArrayList<>();

    /** */
    public Hid4JavaControllerEnvironment() {
        if (hidServices == null) {
            HidServicesSpecification hidServicesSpecification = new HidServicesSpecification();
            // Use the v0.7.0 manual start feature to get immediate attach events
            hidServicesSpecification.setAutoStart(false);

            // Get HID services using custom specification
            hidServices = HidManager.getHidServices(hidServicesSpecification);
            hidServices.addHidServicesListener(new HidServicesListener() {
                /** @param event ⚠⚠⚠ a device got by #getHidDevice() is not opened */
                @Override
                public void hidDeviceAttached(HidServicesEvent event) {
                    try {
Debug.println(Level.FINE, "HID attached: " + event);
                        Hid4javaController c = attatch(event.getHidDevice());
                        if (c != null) {
Debug.println(Level.INFO, "controllerListeners: " + controllerListeners.size());

                            Hid4JavaControllerEnvironment.this.fireControllerAdded(c);
                        }
                    } catch (Exception e) {
Debug.printStackTrace(Level.FINE, e);
                    }
                }

                @Override
                public void hidDeviceDetached(HidServicesEvent event) {
                    try {
Debug.println(Level.FINE, "Device detached: " + event);
                        Hid4javaController c = detach(event.getHidDevice());
                        if (c != null) {
                            Hid4JavaControllerEnvironment.this.fireControllerRemoved(c);
                        }
                    } catch (Exception e) {
                        Debug.printStackTrace(Level.FINE, e);
                    }
                }

                @Override
                public void hidFailure(HidServicesEvent event) {
Debug.println("HID failure: " + event);
                }

                @Override
                public void hidDataReceived(HidServicesEvent event) {
Debug.printf("Data received:%n");
                    byte[] dataReceived = event.getDataReceived();
                    System.out.printf("< [%02x]:", dataReceived.length);
                    for (byte b : dataReceived) {
                        System.out.printf(" %02x", b);
                    }
                    System.out.println();
                }
            });

            Runtime.getRuntime().addShutdownHook(new Thread(() -> hidServices.shutdown()));

Debug.println("starting HID services.");
            hidServices.start();
        }
    }

    /** */
    private Hid4javaController attatch(HidDevice hidDevice) {
Debug.printf(Level.FINE, "uagePage %4x, usage: %s(0x%02x), mid: %4$d(0x%4$x), pid: %5$d(0x%5$x)%n", hidDevice.getUsagePage() & 0xffff, GenericDesktopPage.valueOf(hidDevice.getUsage()), hidDevice.getUsage(), hidDevice.getVendorId(), hidDevice.getProductId());
        if ((hidDevice.getUsagePage() & 0xffff) == /* Generic Desktop Controls */ 0x01 &&
                GenericDesktopPage.valueOf(hidDevice.getUsage()) == GenericDesktopPage.GAME_PAD) {








            List<Component> cs = new ArrayList<>();

            // not opened device is not set a device pointer itself.
Debug.println("open?: " + !hidDevice.isClosed());
            if (hidDevice.isClosed()) {
                hidDevice.open();
            }
Debug.println("open2?: " + !hidDevice.isClosed());

            byte[] desk = HidApiLibraryEX.getDescriptor(hidDevice);
UsbUtil.dump_report_desc(desk, desk.length);



            Hid4javaController c = new Hid4javaController(hidDevice, cs.toArray(Component[]::new), new Controller[0], new Rumbler[0]);
            controllers.add(c);
Debug.printf("@@@@@@@@@@@ add: %s/%s ... %d%n", hidDevice.getManufacturer(), hidDevice.getProduct(), controllers.size());
            return c;
        }
        return null;
    }

    /** */
    @SuppressWarnings("WhileLoopReplaceableByForEach") // for remove
    private Hid4javaController detach(HidDevice hidDevice) {
        Iterator<Hid4javaController> i = controllers.iterator();
        while (i.hasNext()) {
            Hid4javaController c = i.next();
            if (c.getProductId() == hidDevice.getProductId() && c.getManufacturerId() == hidDevice.getVendorId()) {
                controllers.remove(c);
Debug.printf("@@@@@@@@@@@ remove: %s/%s ... %d%n", hidDevice.getManufacturer(), hidDevice.getProduct(), controllers.size());
                return c;
            }
        }
        return null;
    }

    @Override
    public Controller[] getControllers() {
        return controllers.toArray(Controller[]::new);
    }

    @Override
    public boolean isSupported() {
        return hidServices != null;
    }

    /** */
    private static HidServices hidServices;

    /**
     * @throws IllegalArgumentException no matched device of mid and pid
     */
    public Controller getController(int mid, int pid) {
Debug.println("controllers: " + controllers.size());
        for (Hid4javaController controller : controllers) {
Debug.printf("%s: %4x, %4x%n", controller.getName(), controller.getManufacturerId(), controller.getProductId());
            if (controller.getManufacturerId() == mid && controller.getProductId() == pid) {
                return controller;
            }
        }
        throw new IllegalArgumentException(String.format("no device: mid: %1$d(0x%1$x), pid: %2$d(0x%2$x))", mid, pid));
    }
}

/* */
