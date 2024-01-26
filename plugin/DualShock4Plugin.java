/*
 * Copyright (c) 2023 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.awt.joystick.hid4java.plugin;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

import net.java.games.input.Component;
import net.java.games.input.Controller;
import net.java.games.input.DeviceSupportPlugin;
import net.java.games.input.Rumbler;
import net.java.games.input.osx.plugin.DualShock4Plugin.DualShock4Output;
import org.hid4java.HidDevice;
import vavi.awt.joystick.hid4java.Hid4JavaComponent;
import vavi.awt.joystick.hid4java.Hid4JavaRumbler;
import vavi.util.Debug;


/**
 * DualShock4Plugin.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2023-10-30 nsano initial version <br>
 * @see "https://www.psdevwiki.com/ps4/DS4-USB"
 */
public class DualShock4Plugin implements DeviceSupportPlugin {

    /** @param object HidDevice */
    @Override
    public boolean match(Object object) {
        HidDevice device = (HidDevice) object;
Debug.printf(Level.FINER, "%04x, %s, %04x, %s", device.getVendorId(), device.getVendorId() == 0x54c, device.getProductId(), device.getProductId() == 0x9cc);
        return device.getVendorId() == 0x54c && device.getProductId() == 0x9cc;
    }

    @Override
    public Collection<Component> getExtraComponents(Object object) {
        return List.of(
                new Hid4JavaComponent("timestamp", Component.Identifier.Value, 9 * 8, 2 * 8),
                new Hid4JavaComponent("Battery Level", Component.Identifier.Value, 11 * 8, 1 * 8),
                new Hid4JavaComponent("Gyro X", Component.Identifier.Axis.X_ACCELERATION, 12 * 8, 2 * 8),
                new Hid4JavaComponent("Gyro Y", Component.Identifier.Axis.Y_ACCELERATION, 14 * 8, 2 * 8),
                new Hid4JavaComponent("Gyro Z", Component.Identifier.Axis.Z_ACCELERATION, 16 * 8, 2 * 8),
                new Hid4JavaComponent("Accel X", Component.Identifier.Axis.X_VELOCITY, 18 * 8, 2 * 8),
                new Hid4JavaComponent("Accel Y", Component.Identifier.Axis.Y_VELOCITY, 20 * 8, 2 * 8),
                new Hid4JavaComponent("Accel Z", Component.Identifier.Axis.Z_VELOCITY, 22 * 8, 2 * 8),
                new Hid4JavaComponent("EXT/HeadSet/Earset: bitmask", Component.Identifier.Value, 29 * 8, 1 * 8),
                new Hid4JavaComponent("T-PAD event active", Component.Identifier.Value, 32 * 8 + 4, 4),
                new Hid4JavaComponent("T-PAD: tracking numbers No.1", Component.Identifier.Value, 34 * 8, 8),
                new Hid4JavaComponent("T-PAD: finger No.1 X", Component.Identifier.Value, 35 * 8, 12, d -> (d[36] & 0xff) | ((d[37] & 0x0f) << 8)),
                new Hid4JavaComponent("T-PAD: finger No.1 Y", Component.Identifier.Value, 35 * 8 + 12, 12, d -> ((d[38] & 0xff) << 4) | (d[37] & 0xf)),
                new Hid4JavaComponent("T-PAD: tracking numbers No.2", Component.Identifier.Value, 38 * 8, 8),
                new Hid4JavaComponent("T-PAD: finger No.2 X", Component.Identifier.Value, 39 * 8, 12, d -> (d[40] & 0xff) | ((d[41] & 0x0f) << 8)),
                new Hid4JavaComponent("T-PAD: finger No.2 Y", Component.Identifier.Value, 39 * 8 + 12, 12, d -> ((d[42] & 0xff) << 4) | (d[41] & 0xf))
        );
    }

    @Override
    public Collection<Controller> getExtraChildControllers(Object object) {
        return Collections.emptyList();
    }

    @Override
    public Collection<Rumbler> getExtraRumblers(Object object) {
        return List.of(
                new Hid4JavaRumbler(5, DualShock4Output.SMALL_RUMBLE, 3),
                new Hid4JavaRumbler(5, DualShock4Output.BIG_RUMBLE, 4),
                new Hid4JavaRumbler(5, DualShock4Output.LED_RED, 5),
                new Hid4JavaRumbler(5, DualShock4Output.LED_BLUE, 6),
                new Hid4JavaRumbler(5, DualShock4Output.LED_GREEN,7),
                new Hid4JavaRumbler(5, DualShock4Output.FLASH_LED1, 8),
                new Hid4JavaRumbler(5, DualShock4Output.FLASH_LED2, 9)
        );
    }
}
