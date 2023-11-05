/*
 * Copyright (c) 2023 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.awt.joystick.hid4java;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

import net.java.games.input.Component;
import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;
import net.java.games.input.DeviceSupportPlugin;
import net.java.games.input.Rumbler;
import net.java.games.input.usb.GenericDesktopUsage;
import org.hid4java.HidDevice;
import org.hid4java.HidManager;
import org.hid4java.HidServices;
import org.hid4java.HidServicesSpecification;
import vavi.hid.parser.Collection;
import vavi.hid.parser.HidParser;
import vavi.util.Debug;


/**
 * The Hid4Java ControllerEnvironment.
 *
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 230927 nsano initial version <br>
 */
public final class Hid4JavaControllerEnvironment extends ControllerEnvironment {

    /** */
    private List<Hid4JavaController> controllers;

    /** */
    public Hid4JavaControllerEnvironment() {
        if (hidServices == null) {
            try {
                HidServicesSpecification hidServicesSpecification = new HidServicesSpecification();
                // Use the v0.7.0 manual start feature to get immediate attach events
                hidServicesSpecification.setAutoStart(false);
                hidServicesSpecification.setAutoShutdown(false);

                // Get HID services using custom specification
                hidServices = HidManager.getHidServices(hidServicesSpecification);
//                hidServices.addHidServicesListener(new HidServicesListener() {
//                    /** @param event ⚠⚠⚠ a device got by #getHidDevice() is not opened */
//                    @Override
//                    public void hidDeviceAttached(HidServicesEvent event) {
//                        try {
//Debug.println(Level.FINER, "HID attached: " + event);
//                            Hid4JavaController c = attach(event.getHidDevice());
//                            if (c != null) {
//Debug.println(Level.INFO, "controllerListeners: " + controllerListeners.size());
//
//                                Hid4JavaControllerEnvironment.this.fireControllerAdded(c);
//                            }
//                        } catch (Exception e) {
//                            Debug.printStackTrace(Level.FINE, e);
//                        }
//                    }
//
//                    @Override
//                    public void hidDeviceDetached(HidServicesEvent event) {
//                        try {
//Debug.println(Level.FINE, "Device detached: " + event);
//                            Hid4JavaController c = detach(event.getHidDevice());
//                            if (c != null) {
//                                Hid4JavaControllerEnvironment.this.fireControllerRemoved(c);
//                            }
//                        } catch (Exception e) {
//                            Debug.printStackTrace(Level.FINE, e);
//                        }
//                    }
//
//                    @Override
//                    public void hidFailure(HidServicesEvent event) {
//                        Debug.println("HID failure: " + event);
//                    }
//
//                    @Override
//                    public void hidDataReceived(HidServicesEvent event) {
//Debug.printf("Data received:%n");
//                        byte[] dataReceived = event.getDataReceived();
//                        System.out.printf("< [%02x]:", dataReceived.length);
//                        for (byte b : dataReceived) {
//                            System.out.printf(" %02x", b);
//                        }
//                        System.out.println();
//                    }
//                });

                Runtime.getRuntime().addShutdownHook(new Thread(() -> hidServices.shutdown()));

Debug.println("starting HID services.");
                hidServices.start();
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    private void enumerate() throws IOException {
        controllers = new ArrayList<>();
Debug.println("enumerate: " + hidServices.getAttachedHidDevices().size());
        hidServices.getAttachedHidDevices().forEach(hidDevice -> {
            try {
                attach(hidDevice);
            } catch (IOException e) {
                Debug.printStackTrace(e);
            }
        });
    }

    /** */
    private Hid4JavaController attach(HidDevice hidDevice) throws IOException {
Debug.printf(Level.FINER, "usagePage %4x, usage: %s(0x%02x), mid: %4$d(0x%4$x), pid: %5$d(0x%5$x)%n", hidDevice.getUsagePage() & 0xffff, GenericDesktopUsage.map(hidDevice.getUsage()), hidDevice.getUsage(), hidDevice.getVendorId(), hidDevice.getProductId());
        if ((hidDevice.getUsagePage() & 0xffff) == /* Generic Desktop Controls */ 0x01 &&
                GenericDesktopUsage.map(hidDevice.getUsage()) == GenericDesktopUsage.GAME_PAD) {

            List<Component> components = new ArrayList<>();
            List<Controller> children = new ArrayList<>();
            List<Rumbler> rumblers = new ArrayList<>();


                }
            }










            hidDevice.open();

            byte[] desk = new byte[4096];
            int r = hidDevice.getReportDescriptor(desk);
//UsbUtil.dump_report_desc(desk, r);
            HidParser parser = new HidParser();
            Collection collection = parser.parse(desk, r);
Debug.println("collection: " + collection.getUsagePair());

            Hid4JavaController controller = new Hid4JavaController(hidDevice,
                    components.toArray(Component[]::new),
                    children.toArray(Controller[]::new),
                    rumblers.toArray(Rumbler[]::new));
            controllers.add(controller);
Debug.printf("@@@@@@@@@@@ add: %s/%s ... %d%n", hidDevice.getManufacturer(), hidDevice.getProduct(), controllers.size());
            return controller;
        }
        return null;
    }

    /** */
    @SuppressWarnings("WhileLoopReplaceableByForEach") // for remove
    private Hid4JavaController detach(HidDevice hidDevice) {
        Iterator<Hid4JavaController> i = controllers.iterator();
        while (i.hasNext()) {
            Hid4JavaController c = i.next();
            if (c.getProductId() == hidDevice.getProductId() && c.getVendorId() == hidDevice.getVendorId()) {
                controllers.remove(c);
Debug.printf("@@@@@@@@@@@ remove: %s/%s ... %d%n", hidDevice.getManufacturer(), hidDevice.getProduct(), controllers.size());
                return c;
            }
        }
        return null;
    }

    @Override
    public Controller[] getControllers() {
        if (controllers == null) {
            try {
                enumerate();
            } catch (IOException e) {
Debug.printStackTrace(e);
                return new Controller[0];
            }
        }
        return controllers.toArray(Controller[]::new);
    }

    @Override
    public boolean isSupported() {
Debug.println("isSupported: " + (hidServices != null));
        return hidServices != null;
    }

    /** */
    private static HidServices hidServices;

    /**
     * @throws IllegalArgumentException no matched device of mid and pid
     */
    public Controller getController(int mid, int pid) {
Debug.println("controllers: " + controllers.size());
        for (Hid4JavaController controller : controllers) {
Debug.printf("%s: %4x, %4x%n", controller.getName(), controller.getVendorId(), controller.getProductId());
            if (controller.getVendorId() == mid && controller.getProductId() == pid) {
                return controller;
            }
        }
        throw new IllegalArgumentException(String.format("no device: mid: %1$d(0x%1$x), pid: %2$d(0x%2$x))", mid, pid));
    }
}

/* */
