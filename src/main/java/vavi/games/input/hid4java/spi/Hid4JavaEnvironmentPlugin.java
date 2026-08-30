/*
 * Copyright (c) 2023 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.games.input.hid4java.spi;

import java.io.Closeable;
import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

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
import org.hid4java.HidDevices;
import org.hid4java.HidDevicesEvent;
import org.hid4java.HidDevicesListener;
import org.hid4java.HidSpecification;

import static java.lang.System.getLogger;


/**
 * The Hid4Java ControllerEnvironment.
 * <p>
 * system property
 * <li>{@code vavi.games.input.hid4java.darwinOpenDevicesNonExclusive} ... {@code false}</li>
 *
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 230927 nsano initial version <br>
 */
public final class Hid4JavaEnvironmentPlugin extends ControllerListenerSupport implements HidControllerEnvironment, Closeable {

    private static final Logger logger = getLogger(Hid4JavaEnvironmentPlugin.class.getName());

    /** */
    private List<Hid4JavaController> controllers;

    /** */
    private HidDevices getHidService() throws IOException {
        HidSpecification hidSpecification = new HidSpecification();
        // Use the v0.7.0 manual start feature to get immediate attach events
        hidSpecification.setAutoStart(false);
        hidSpecification.setAutoShutdown(false);
        hidSpecification.darwinOpenDevicesNonExclusive = Boolean.getBoolean("vavi.games.input.hid4java.darwinOpenDevicesNonExclusive");

        // Get HID services using custom specification
        return hidDevices = new HidDevices(hidSpecification);
    }

    /** */
    private void startListening() {
        hidDevices.addHidServicesListener(new HidDevicesListener() {
            @Override
            public void hidDeviceAttached(HidDevicesEvent event) {
                try {
logger.log(Level.DEBUG, "+++ Device attached: " + event);
                    Hid4JavaController c = attach(event.getHidDevice());
                    if (c != null) {
logger.log(Level.DEBUG, "controllerListeners: " + controllerListeners.size());

                        Hid4JavaEnvironmentPlugin.this.fireControllerAdded(c);
                    }
                } catch (Exception e) {
logger.log(Level.ERROR, e.getMessage(), e);
                }
            }

            @Override
            public void hidDeviceDetached(HidDevicesEvent event) {
                try {
logger.log(Level.DEBUG, "--- Device detached: " + event);
                    Hid4JavaController c = detach(event.getHidDevice());
                    if (c != null) {
                        Hid4JavaEnvironmentPlugin.this.fireControllerRemoved(c);
                    }
                } catch (Exception e) {
logger.log(Level.ERROR, e.getMessage(), e);
                }
            }
        });
    }

    private void enumerate() throws IOException {
        boolean r = isSupported(); // don't touch, instantiates hidDevices
logger.log(Level.DEBUG, "isSupported: " + r);
        controllers = new ArrayList<>();
logger.log(Level.DEBUG, "devices: " + hidDevices.getHidDevices().size());
        hidDevices.getHidDevices().forEach(hidDevice -> {
            try {
                attach(hidDevice);
            } catch (IOException e) {
                logger.log(Level.ERROR, e.getMessage(), e);
            }
        });
    }

    /** */
    private Hid4JavaController attach(HidDevice hidDevice) throws IOException {
logger.log(Level.TRACE, "usagePage %4x, usage: %s(0x%02x), mid: %4$d(0x%4$x), pid: %5$d(0x%5$x)".formatted(hidDevice.getUsagePage() & 0xffff, GenericDesktopUsageId.map(hidDevice.getUsage()), hidDevice.getUsage(), hidDevice.getVendorId(), hidDevice.getProductId()));
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
logger.log(Level.TRACE, "getFields: " + parser.parse(desk, r).enumerateFields().size());
            parser.parse(desk, r).enumerateFields().forEach(f -> {
logger.log(Level.TRACE, "UsagePage: " + UsagePage.map(f.getUsagePage()) + ", " + f.getUsageId());
                if (UsagePage.map(f.getUsagePage()) != null) {
                    switch (UsagePage.map(f.getUsagePage())) {
                        case GENERIC_DESKTOP, BUTTON -> {
                            UsagePage usagePage = UsagePage.map(f.getUsagePage());
                            UsageId usageId = usagePage.mapUsage(f.getUsageId());
                            components.add(new Hid4JavaComponent(usageId.toString(), usageId.getIdentifier(), f));
logger.log(Level.TRACE, "add: " + components.get(components.size() - 1));
                        }
                        default -> {
                        }
                    }
                }
            });

            // extra elements by plugin
            for (DeviceSupportPlugin plugin : DeviceSupportPlugin.getPlugins()) {
logger.log(Level.TRACE, "plugin: " + plugin + ", " + plugin.match(hidDevice));
                if (plugin.match(hidDevice)) {
logger.log(Level.DEBUG, "@@@ plugin for extra: " + plugin.getClass().getName());
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
logger.log(Level.DEBUG, "@@@@@@@@@@@ add: %s/%s ... %d".formatted(hidDevice.getManufacturer(), hidDevice.getProduct(), controllers.size()));
logger.log(Level.DEBUG, "    components: %d, %s".formatted(components.size(), components));
//logger.log(Level.TRACE, "    children: %d".formatted(children.size()));
logger.log(Level.DEBUG, "    rumblers: %d, %s".formatted(rumblers.size(), rumblers));
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
logger.log(Level.DEBUG, "@@@@@@@@@@@ remove: %s/%s ... %d".formatted(hidDevice.getManufacturer(), hidDevice.getProduct(), controllers.size()));
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
                hidDevices.start();
                startListening();
            } catch (IOException e) {
logger.log(Level.ERROR, e.getMessage(), e);
                return new Hid4JavaController[0];
            }
        }
        return controllers.toArray(Hid4JavaController[]::new);
    }

    @Override
    public boolean isSupported() {
        try {
            if (hidDevices == null) {
                hidDevices = getHidService();
logger.log(Level.DEBUG, "starting HID services.");
                hidDevices.start();
            }
            return true;
        } catch (IOException e) {
logger.log(Level.ERROR, e.getMessage(), e);
            return false;
        }
    }

    /** */
    private HidDevices hidDevices;

    @Override
    public void close() {
        hidDevices.shutdown();
    }

    /**
     * @throws NoSuchElementException no matched device of mid and pid
     */
    @Override
    public Hid4JavaController getController(int mid, int pid) {
logger.log(Level.DEBUG, "controllers: " + getControllers().length);
        for (Hid4JavaController controller : getControllers()) {
logger.log(Level.DEBUG, "%s: %4x, %4x".formatted(controller.getName(), controller.getVendorId(), controller.getProductId()));
            if (controller.getVendorId() == mid && controller.getProductId() == pid) {
                return controller;
            }
        }
        throw new NoSuchElementException("no device: mid: %1$d(0x%1$x), pid: %2$d(0x%2$x))".formatted(mid, pid));
    }
}
