package org.hid4java;

import java.util.Arrays;
import java.util.List;

import org.hid4java.HidDevice;
import org.junit.jupiter.api.Test;


class HidDeviceInfoStructureTest {

    @Test
    void getFieldOrder() {

        // Arrange
        HidDevice.Info testObject = new HidDevice.Info();
        List<String> expectedFieldOrder = Arrays.asList(
                "path",
                "vendor_id",
                "product_id",
                "serial_number",
                "release_number",
                "manufacturer_string",
                "product_string",
                "usage_page",
                "usage",
                "interface_number",
                "next"
        );
    }

    @Test
    void show() {

        // Arrange
        HidDevice.Info testObject = new HidDevice.Info();
        testObject.path = "path";
        testObject.vendorId = 0x01;
        testObject.productId = 0x02;
        testObject.serialNumber = "serial";
        testObject.releaseNumber = 0x03;
        testObject.manufacturer = "manufacturer";
        testObject.product = "product";
        testObject.usagePage = 0x04;
        testObject.usage = 0x05;
        testObject.interfaceNumber = 0x06;

        String expectedShow = """
                HidDevice
                \tpath:path>
                \tvendor_id: 1
                \tproduct_id: 2
                \tserial_number: serial>
                \trelease_number: 3
                \tmanufacturer_string: manufacturer>
                \tproduct_string: product>
                \tusage_page: 4
                \tusage: 5
                \tinterface_number: 6
                """;
    }
}