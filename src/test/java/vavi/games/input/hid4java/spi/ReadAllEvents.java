package vavi.games.input.hid4java.spi;

import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;
import net.java.games.input.osx.plugin.DualShock4Plugin;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;


/**
 * This class shows how to use the event queue system in JInput. It will show
 * how to get the controllers, how to get the event queue for a controller, and
 * how to read and process events from the queue.
 *
 * @author Endolf
 */
public class ReadAllEvents {

    static {
        // to avoid conflict with jinput bundled spis.
        System.setProperty("net.java.games.input.ControllerEnvironment.excludes", "net.java.games.input");
    }

    /** the most typical test for jinput */
    @Test
    @EnabledIfSystemProperty(named = "vavi.test", matches = "ide")
    @DisplayName("jinput sample powered by hid4java spi")
    void readAllEvents() throws Exception {
        /* Get the available controllers */
        Controller[] controllers = ControllerEnvironment.getDefaultEnvironment().getControllers();
        if (controllers.length == 0) {
            throw new IllegalStateException("Found no controllers.");
        }
        for (Controller controller : controllers) {
            // hid4java controller needs to start event listener system
            if (!controller.isOpen()) {
                controller.addInputEventListener(e -> {
                    if (e instanceof Hid4JavaInputEvent he) {
//try {
// byte[] d = he.getData();
// OutputStream os = Files.newOutputStream(Path.of("src/test/resources/ds4_ir.dat"));
// os.write(d, 0, d.length);
// os.flush();
// os.close();
// System.exit(0);
//} catch (IOException f) {}
                        DualShock4Plugin.display(he.getData(), System.out);
                    }
                });
                controller.open();
            }
        }
        Thread.sleep(180 * 1000);
    }
}
