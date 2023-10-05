package org.hid4java;

import org.junit.jupiter.api.Test;
import vavi.util.Debug;
import vavix.rococoa.corefoundation.CFLib;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


class HidDeviceTest {

    HidDevice.Info mockStructure = new HidDevice.Info();

    @Test
    void isVidPidSerial_UnsignedShort_Simple() {

        // Arrange
        mockStructure.vendorId = 0x01;
        mockStructure.productId = 0x02;
        mockStructure.serialNumber = "1234";

        // Act
        HidDevice testObject = new HidDevice(mockStructure, null, new HidServicesSpecification());

        // Assert
        assertTrue(testObject.isVidPidSerial(0x01, 0x02, "1234"));
    }

    @Test
    void isVidPidSerial_UnsignedShort_Overflow() {

        // Arrange
        mockStructure.vendorId = 0xffff8001;
        mockStructure.productId = 0xffff8002;
        mockStructure.serialNumber = "1234";

        // Act
        HidDevice testObject = new HidDevice(mockStructure, null, new HidServicesSpecification());

        // Assert
        assertTrue(testObject.isVidPidSerial(0x8001, 0x8002, "1234"));
    }

    @Test
    void verifyFields() {

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
        HidDevice testObject = new HidDevice(mockStructure, null, new HidServicesSpecification());

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
}