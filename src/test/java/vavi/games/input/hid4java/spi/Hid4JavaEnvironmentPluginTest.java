/*
 * Copyright (c) 2024 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.games.input.hid4java.spi;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

import net.java.games.input.Component;
import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;
import net.java.games.input.ControllerEvent;
import net.java.games.input.ControllerListener;
import net.java.games.input.Event;
import net.java.games.input.osx.plugin.DualShock4Plugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import vavi.util.Debug;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;


/**
 * HidapiEnvironmentPluginTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2024-01-28 nsano initial version <br>
 */
@EnabledIf("localPropertiesExists")
@PropsEntity(url = "file:local.properties")
class Hid4JavaEnvironmentPluginTest {

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

    @Test
    @DisplayName("rumbler")
    void test1() throws Exception {
        Hid4JavaEnvironmentPlugin environment = new Hid4JavaEnvironmentPlugin();
        Hid4JavaController controller = environment.getController(vendorId, productId);
        controller.open();

        Random random = new Random();

        DualShock4Plugin.Report5 report = new DualShock4Plugin.Report5();
        report.smallRumble = 20;
        report.bigRumble = 0;
        report.ledRed = random.nextInt(255);
        report.ledGreen = random.nextInt(255);
        report.ledBlue = random.nextInt(255);
        report.flashLed1 = 80;
        report.flashLed2 = 80;
Debug.printf("R: %02x, G: %02x, B: %02x", report.ledRed, report.ledGreen, report.ledBlue);

        controller.output(report);

        environment.close();
    }

    @Test
    @DisplayName("event")
    @EnabledIfSystemProperty(named = "vavi.test", matches = "ide")
    void test2() throws Exception {
        Hid4JavaEnvironmentPlugin environment = new Hid4JavaEnvironmentPlugin();
        Controller controller = environment.getController(vendorId, productId);
        Event event = new Event();
        controller.addInputEventListener(e -> {
            while (e.getNextEvent(event)) {
                System.out.println(event.getComponent().getName() + ": " + event.getValue());
            }
        });
        controller.open();

        new CountDownLatch(1).await();
    }

    @Test
    @DisplayName("hid4java spi directly")
    @EnabledIfSystemProperty(named = "vavi.test", matches = "ide")
    void test3() throws Exception {
        CountDownLatch cdl = new CountDownLatch(1);

        ControllerEnvironment ce = new Hid4JavaEnvironmentPlugin();
        ce.addControllerListener(new ControllerListener() {
            @Override
            public void controllerRemoved(ControllerEvent ev) {
Debug.println("➖ controllerRemoved: " + ev.getController());
            }

            @Override
            public void controllerAdded(ControllerEvent ev) {
Debug.println("➕ controllerAdded: " + ev.getController());
            }
        });

        Hid4JavaController controller = Arrays.stream(ce.getControllers())
                .filter(c -> c instanceof Hid4JavaController)
                .map(c -> (Hid4JavaController) c)
                .filter(c -> c.getVendorId() == vendorId && c.getProductId() == productId)
                .findFirst().get();

        // Create an event object for the underlying plugin to populate
        Event event = new Event();

        controller.addInputEventListener(e -> {

            // For each object in the queue
            while (e.getNextEvent(event)) {

                // Create a string buffer and put in it, the controller name,
                // the time stamp of the event, the name of the component
                // that changed and the new value.
                //
                // Note that the timestamp is a relative thing, not
                // absolute, we can tell what order events happened in
                // across controllers this way. We can not use it to tell
                // exactly *when* an event happened just the order.
                StringBuilder sb = new StringBuilder(controller.getName());
                sb.append(" at ");
                sb.append(event.getNanos()).append(", ");
                Component component = event.getComponent();
                sb.append(component.getName()).append(" changed to ");
                float value = event.getValue();

                // Check the type of the component and display an
                // appropriate value
                if (component.isAnalog()) {
                    sb.append(value);
                } else {
                    if (value == 1.0f) {
                        sb.append("On");
                    } else {
                        sb.append("Off");
                    }
                }
                System.out.println(sb);
            }
        });

        controller.open();

        new CountDownLatch(1).await();
    }

    @Test
    @DisplayName("hid4java spi directly")
    void test5() throws Exception {
        CountDownLatch cdl = new CountDownLatch(1);

        ControllerEnvironment ce = new Hid4JavaEnvironmentPlugin();
        ce.addControllerListener(new ControllerListener() {
            @Override
            public void controllerRemoved(ControllerEvent ev) {
Debug.println("➖ controllerRemoved: " + ev.getController());
            }

            @Override
            public void controllerAdded(ControllerEvent ev) {
Debug.println("➕ controllerAdded: " + ev.getController());
            }
        });

        Hid4JavaController controller = Arrays.stream(ce.getControllers())
                .filter(c -> c instanceof Hid4JavaController)
                .map(c -> (Hid4JavaController) c)
                .filter(c -> c.getVendorId() == vendorId && c.getProductId() == productId)
                .findFirst().get();

Debug.println("controller: " + controller);
    }
}