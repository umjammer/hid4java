/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.games.input.hid4java.spi.plugin;

import net.java.games.input.plugin.HoriOctaPluginBase;
import org.hid4java.HidDevice;


/**
 * HORI Fighting Commander OCTA support for hid4java.
 * <p>
 * Only the hid modes of the PlayStation model are seen by hidapi (PS4 and PS5 mode),
 * the PC mode and the Xbox model are not hid devices.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-10-07 nsano initial version <br>
 */
public class HoriOctaPlugin extends HoriOctaPluginBase {

    /** @param object HidDevice */
    @Override
    public boolean match(Object object) {
        return object instanceof HidDevice device && isOcta(device.getVendorId(), device.getProductId());
    }
}
