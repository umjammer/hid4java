package org.hid4java;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


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
    @Disabled("path need to be not null")
    void isVidPidSerial_UnsignedShort_Simple() throws Exception {

        HidDeviceManager manager = new HidDeviceManager(null, null);

        // Arrange
        mockStructure.vendorId = 0x01;
        mockStructure.productId = 0x02;
        mockStructure.serialNumber = "1234";

        // Act
        HidDevice testObject = new HidDevice(mockStructure, manager, new HidServicesSpecification());

        // Assert
        assertTrue(testObject.isVidPidSerial(0x01, 0x02, "1234"));
    }

    @Test
    @Disabled("already let code not overflow")
    void isVidPidSerial_UnsignedShort_Overflow() throws IOException {

        HidDeviceManager manager = new HidDeviceManager(null, null);

        // Arrange
        mockStructure.vendorId = 0xffff8001;
        mockStructure.productId = 0xffff8002;
        mockStructure.serialNumber = "1234";

        // Act
        HidDevice testObject = new HidDevice(mockStructure, manager, new HidServicesSpecification());

        // Assert
        assertTrue(testObject.isVidPidSerial(0x8001, 0x8002, "1234"));
    }

    @Test
    void verifyFields() throws IOException {

        HidDeviceManager manager = new HidDeviceManager(null, null);

        // Arrange
        mockStructure.path = "path";
        mockStructure.vendorId = 1;
        mockStructure.productId = 2;
        mockStructure.serialNumber = "serial";
        mockStructure.releaseNumber = 3;
        mockStructure.manufacturer = "manufacturer";
        mockStructure.product = "product";
        mockStructure.usagePage = 4;
        mockStructure.usage = 5;
        mockStructure.interfaceNumber = 6;

        // Act
        HidDevice testObject = new HidDevice(mockStructure, manager, new HidServicesSpecification());

        // Assert
        assertEquals("path", testObject.getPath());
        assertEquals(1, testObject.getVendorId());
        assertEquals(2, testObject.getProductId());
        assertEquals("serial", testObject.getSerialNumber());
        assertEquals(3, testObject.getReleaseNumber());
        assertEquals("manufacturer", testObject.getManufacturer());
        assertEquals("product", testObject.getProduct());
        assertEquals(4, testObject.getUsagePage());
        assertEquals(5, testObject.getUsage());
        assertEquals(6, testObject.getInterfaceNumber());
    }

    @Test
    @EnabledIf("localPropertiesExists")
    void test1() throws Exception {
        HidServicesSpecification hidServicesSpecification = new HidServicesSpecification();
        hidServicesSpecification.setAutoStart(false);
        hidServicesSpecification.setAutoShutdown(false);

        HidServices hidServices = HidManager.getHidServices(hidServicesSpecification);
        hidServices.start();

        List<HidDevice.Info> devices = hidServices.getHidDeviceManager().getNativeHidDeviceManager().enumerate(vendorId, productId);
        assertEquals(1, devices.size());
    }
}
