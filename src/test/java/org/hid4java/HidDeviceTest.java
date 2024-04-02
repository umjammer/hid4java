package org.hid4java;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;


@PropsEntity(url = "file:local.properties")
class HidDeviceTest {

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

    HidDevice.Info mockStructure = new HidDevice.Info();

    @Test
    @EnabledIf("localPropertiesExists")
    void test1() throws Exception {
        HidSpecification hidSpecification = new HidSpecification();
        hidSpecification.setAutoStart(false);
        hidSpecification.setAutoShutdown(false);

        HidDevices hidDevices = new HidDevices(hidSpecification);
        hidDevices.start();

        List<HidDevice.Info> devices = hidDevices.getNativeHidDevices().enumerate(vendorId, productId);
        assertEquals(1, devices.size());
    }
}
