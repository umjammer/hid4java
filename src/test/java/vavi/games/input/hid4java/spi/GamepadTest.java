/*
 * Copyright (c) 2024 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.games.input.hid4java.spi;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;

import net.java.games.input.Event;
import net.java.games.input.InputEvent;
import net.java.games.input.WrappedComponent;
import net.java.games.input.osx.plugin.DualShock4Plugin;
import net.java.games.input.usb.HidController;
import net.java.games.input.usb.HidInputEvent;
import net.java.games.input.usb.parser.Field;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;


/**
 * GamepadTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2024-01-28 nsano initial version <br>
 */
@PropsEntity(url = "file:local.properties")
public class GamepadTest {

    static boolean localPropertiesExists() {
        return Files.exists(Paths.get("local.properties"));
    }

    @Property(name = "mid")
    String mid;
    @Property(name = "pid")
    String pid;

    int vendorId;
    int productId;

    @BeforeEach
    void setup() throws Exception {
        if (localPropertiesExists()) {
            PropsEntity.Util.bind(this);

            vendorId = Integer.decode(mid);
            productId = Integer.decode(pid);
        }
    }

    static {
        // for fixing table rows count
        System.setProperty("net.java.games.input.InputEvent.fillAll", "true");
    }

    static class GamepadConsole {

        /**  */
        private final HidController controller;

        public GamepadConsole(HidController controller) throws IOException {
            this.controller = controller;
        }

        /**  */
        public void start() throws IOException {
            controller.addInputEventListener(GamepadConsole::print);
            controller.open();
        }

        /** analyze data by jinput */
        static void print(InputEvent e) {
            Event event = new Event();
            System.out.println("\033[2J");
            while (e.getNextEvent(event)) {
                System.out.println(String.format("%30s:  % 10.3f  %s%s",
                        event.getComponent().getName(),
                        event.getValue(),
                        ((WrappedComponent<Field>) event.getComponent()).getWrappedObject().getDump(((HidInputEvent) e).getData()),
                        " ".repeat(50)));
            }
            System.out.flush();
        }

        /** analyze data directly */
        static void print2(InputEvent e) {
            System.out.println("\033[2J");
            DualShock4Plugin.display(((HidInputEvent) e).getData(), System.out);
            System.out.flush();
        }
    }

    @Test
    @EnabledIfSystemProperty(named = "vavi.test", matches = "ide")
    void test1() throws Exception {
        Hid4JavaEnvironmentPlugin environment = new Hid4JavaEnvironmentPlugin();
        HidController controller = environment.getController(vendorId, productId);

        GamepadConsole app = new GamepadConsole(controller);
        app.start();

        CountDownLatch cdl = new CountDownLatch(1);
        cdl.await();
    }

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {
        GamepadTest app = new GamepadTest();
        PropsEntity.Util.bind(app);
        app.vendorId = Integer.decode(app.mid);
        app.productId = Integer.decode(app.pid);
        app.test1();
    }
}
