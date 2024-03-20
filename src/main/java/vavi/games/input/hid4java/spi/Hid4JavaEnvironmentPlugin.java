/*
 * Copyright (c) 2023 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.games.input.hid4java.spi;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.logging.Level;

import net.java.games.input.Component;
import net.java.games.input.Controller;
import net.java.games.input.ControllerListenerSupport;
import net.java.games.input.DeviceSupportPlugin;
import net.java.games.input.Rumbler;
import net.java.games.input.usb.GenericDesktopUsageId;
import net.java.games.input.usb.HidControllerEnvironment;
import net.java.games.input.usb.UsageId;
import net.java.games.input.usb.UsagePage;
import net.java.games.input.usb.parser.HidParser;
import org.hid4java.HidDevice;
import org.hid4java.HidManager;
import org.hid4java.HidServices;
import org.hid4java.HidServicesEvent;
import org.hid4java.HidServicesListener;
import org.hid4java.HidServicesSpecification;
import vavi.util.Debug;


/**
 * The Hid4Java ControllerEnvironment.
 *
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 230927 nsano initial version <br>
 */
public final class Hid4JavaEnvironmentPlugin extends ControllerListenerSupport implements HidControllerEnvironment, Closeable {

    /** */
    private List<Hid4JavaController> controllers;

    /** */
    private HidServices getHidService() throws IOException {
        HidServicesSpecification hidServicesSpecification = new HidServicesSpecification();
        // Use the v0.7.0 manual start feature to get immediate attach events
        hidServicesSpecification.setAutoStart(false);
        hidServicesSpecification.setAutoShutdown(false);

        // Get HID services using custom specification
        return hidServices = HidManager.getHidServices(hidServicesSpecification);
    }

    /** */
    private void startListening() {
        hidServices.addHidServicesListener(new HidServicesListener() {
            @Override
            public void hidDeviceAttached(HidServicesEvent event) {
                try {
Debug.println(Level.FINE, "+++ Device attached: " + event);
                    Hid4JavaController c = attach(event.getHidDevice());
                    if (c != null) {
Debug.println(Level.FINE, "controllerListeners: " + controllerListeners.size());

                        Hid4JavaEnvironmentPlugin.this.fireControllerAdded(c);
                    }
                } catch (Exception e) {
Debug.printStackTrace(Level.FINE, e);
                }
            }

            @Override
            public void hidDeviceDetached(HidServicesEvent event) {
                try {
Debug.println(Level.FINE, "--- Device detached: " + event);
                    Hid4JavaController c = detach(event.getHidDevice());
                    if (c != null) {
                        Hid4JavaEnvironmentPlugin.this.fireControllerRemoved(c);
                    }
                } catch (Exception e) {
Debug.printStackTrace(Level.FINE, e);
                }
            }
        });
    }

    private void enumerate() throws IOException {
        boolean r = isSupported(); // don't touch, instantiates hidServices
Debug.println(Level.FINE, "isSupported: " + r);
        controllers = new ArrayList<>();
Debug.println(Level.FINE, "devices: " + hidServices.getAttachedHidDevices().size());
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
Debug.printf(Level.FINER, "usagePage %4x, usage: %s(0x%02x), mid: %4$d(0x%4$x), pid: %5$d(0x%5$x)%n", hidDevice.getUsagePage() & 0xffff, GenericDesktopUsageId.map(hidDevice.getUsage()), hidDevice.getUsage(), hidDevice.getVendorId(), hidDevice.getProductId());
        // TODO out source filter
        if ((hidDevice.getUsagePage() & 0xffff) == /* Generic Desktop Controls */ 0x01 &&
                GenericDesktopUsageId.map(hidDevice.getUsage()) == GenericDesktopUsageId.GAME_PAD) {

            List<Component> components = new ArrayList<>();
            List<Controller> children = new ArrayList<>();
            List<Rumbler> rumblers = new ArrayList<>();

            byte[] desk = new byte[4096];
            int r = hidDevice.getReportDescriptor(desk);
//UsbUtil.dump_report_desc(desk, r);
            HidParser parser = new HidParser();
Debug.println(Level.FINER, "getFields: " + parser.parse(desk, r).enumerateFields().size());
            parser.parse(desk, r).enumerateFields().forEach(f -> {
Debug.println(Level.FINER, "UsagePage: " + UsagePage.map(f.getUsagePage()) + ", " + f.getUsageId());
                if (UsagePage.map(f.getUsagePage()) != null) {
                    switch (UsagePage.map(f.getUsagePage())) {
                        case GENERIC_DESKTOP, BUTTON -> {
                            UsagePage usagePage = UsagePage.map(f.getUsagePage());
                            UsageId usageId = usagePage.mapUsage(f.getUsageId());
                            components.add(new Hid4JavaComponent(usageId.toString(), usageId.getIdentifier(), f));
Debug.println(Level.FINER, "add: " + components.get(components.size() - 1));
                        }
                        default -> {
                        }
                    }
                }
            });

            // extra elements by plugin
            for (DeviceSupportPlugin plugin : DeviceSupportPlugin.getPlugins()) {
Debug.println(Level.FINER, "plugin: " + plugin + ", " + plugin.match(hidDevice));
                if (plugin.match(hidDevice)) {
Debug.println(Level.FINE, "@@@ plugin for extra: " + plugin.getClass().getName());
                    components.addAll(plugin.getExtraComponents(hidDevice));
                    children.addAll(plugin.getExtraChildControllers(hidDevice));
                    rumblers.addAll(plugin.getExtraRumblers(hidDevice));
                }
            }

            Hid4JavaController controller = new Hid4JavaController(hidDevice,
                    components.toArray(Component[]::new),
                    children.toArray(Controller[]::new),
                    rumblers.toArray(Rumbler[]::new));
            controllers.add(controller);
Debug.printf(Level.FINE, "@@@@@@@@@@@ add: %s/%s ... %d", hidDevice.getManufacturer(), hidDevice.getProduct(), controllers.size());
Debug.printf(Level.FINE, "    components: %d, %s", components.size(), components);
//Debug.printf(Level.FINE, "    children: %d", children.size());
Debug.printf(Level.FINE, "    rumblers: %d, %s", rumblers.size(), rumblers);
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
Debug.printf(Level.FINE, "@@@@@@@@@@@ remove: %s/%s ... %d%n", hidDevice.getManufacturer(), hidDevice.getProduct(), controllers.size());
                return c;
            }
        }
        return null;
    }

    @Override
    public Hid4JavaController[] getControllers() {
        if (controllers == null) {
            try {
                enumerate();
                hidServices.start();
                startListening();
            } catch (IOException e) {
Debug.printStackTrace(e);
                return new Hid4JavaController[0];
            }
        }
        return controllers.toArray(Hid4JavaController[]::new);
    }

    @Override
    public boolean isSupported() {
        try {
            if (hidServices == null) {
                hidServices = getHidService();
Debug.println(Level.FINE, "starting HID services.");
                hidServices.start();
            }
            return true;
        } catch (IOException e) {
Debug.println(e);
            return false;
        }
    }

    /** */
    private HidServices hidServices;

    @Override
    public void close() {
        hidServices.shutdown();
    }

    /**
     * @throws NoSuchElementException no matched device of mid and pid
     */
    @Override
    public Hid4JavaController getController(int mid, int pid) {
Debug.println(Level.FINE, "controllers: " + getControllers().length);
        for (Hid4JavaController controller : getControllers()) {
Debug.printf(Level.FINE, "%s: %4x, %4x%n", controller.getName(), controller.getVendorId(), controller.getProductId());
            if (controller.getVendorId() == mid && controller.getProductId() == pid) {
                return controller;
            }
        }
        throw new NoSuchElementException(String.format("no device: mid: %1$d(0x%1$x), pid: %2$d(0x%2$x))", mid, pid));
    }
}

/* */
