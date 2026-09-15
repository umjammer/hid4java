/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.games.input.hid4java.spi;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


/**
 * UsagesTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-16 nsano initial version <br>
 */
class UsagesTest {

    @Test
    @DisplayName("vavi.games.input.hid4java.usages")
    void test1() throws Exception {
        List<int[]> usages = Hid4JavaEnvironmentPlugin.parseUsages("0x01:0x05, 1:6 ,");
        assertEquals(2, usages.size());
        assertArrayEquals(new int[] {1, 5}, usages.get(0));
        assertArrayEquals(new int[] {1, 6}, usages.get(1));

        assertThrows(IllegalArgumentException.class, () -> Hid4JavaEnvironmentPlugin.parseUsages("0x01"));
    }
}
