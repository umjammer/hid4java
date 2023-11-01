/*
 * HIDAPI - Multi-Platform library for
 * communication with HID devices.
 *
 * libusb/hidapi Team
 *
 * Copyright 2022, All Rights Reserved.
 *
 * At the discretion of the user of this library,
 * this software may be licensed under the terms of the
 * GNU General Public License v3, a BSD-Style license, or the
 * original HIDAPI license as outlined in the LICENSE.txt,
 * LICENSE-gpl3.txt, LICENSE-bsd.txt, and LICENSE-orig.txt
 * files located at the root of the source distribution.
 * These files may also be found in the public source
 * code repository located at:
 * https://github.com/libusb/hidapi .
 */

package org.hid4java.windows;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.Union;

import static net.java.games.input.windows.WinAPI.HIDP_REPORT_TYPE.HidP_Feature;
import static net.java.games.input.windows.WinAPI.HIDP_REPORT_TYPE.HidP_Input;
import static net.java.games.input.windows.WinAPI.HIDP_REPORT_TYPE.HidP_Output;
import static org.hid4java.windows.DescriptorReconstructor.hid_pp_cap.IsButtonCap;
import static org.hid4java.windows.DescriptorReconstructor.hid_pp_cap.IsDesignatorRange;
import static org.hid4java.windows.DescriptorReconstructor.hid_pp_cap.IsRange;
import static org.hid4java.windows.DescriptorReconstructor.hid_pp_cap.IsStringRange;
import static org.hid4java.windows.DescriptorReconstructor.hid_pp_link_collection_node.CollectionType;
import static org.hid4java.windows.DescriptorReconstructor.hid_pp_link_collection_node.IsAlias;
import static org.hid4java.windows.DescriptorReconstructor.Items.global_logical_maximum;
import static org.hid4java.windows.DescriptorReconstructor.Items.global_logical_minimum;
import static org.hid4java.windows.DescriptorReconstructor.Items.global_physical_maximum;
import static org.hid4java.windows.DescriptorReconstructor.Items.global_physical_minimum;
import static org.hid4java.windows.DescriptorReconstructor.Items.global_report_count;
import static org.hid4java.windows.DescriptorReconstructor.Items.global_report_id;
import static org.hid4java.windows.DescriptorReconstructor.Items.global_report_size;
import static org.hid4java.windows.DescriptorReconstructor.Items.global_unit;
import static org.hid4java.windows.DescriptorReconstructor.Items.global_unit_exponent;
import static org.hid4java.windows.DescriptorReconstructor.Items.global_usage_page;
import static org.hid4java.windows.DescriptorReconstructor.Items.local_delimiter;
import static org.hid4java.windows.DescriptorReconstructor.Items.local_designator_index;
import static org.hid4java.windows.DescriptorReconstructor.Items.local_designator_maximum;
import static org.hid4java.windows.DescriptorReconstructor.Items.local_designator_minimum;
import static org.hid4java.windows.DescriptorReconstructor.Items.local_string;
import static org.hid4java.windows.DescriptorReconstructor.Items.local_string_maximum;
import static org.hid4java.windows.DescriptorReconstructor.Items.local_string_minimum;
import static org.hid4java.windows.DescriptorReconstructor.Items.local_usage;
import static org.hid4java.windows.DescriptorReconstructor.Items.local_usage_maximum;
import static org.hid4java.windows.DescriptorReconstructor.Items.local_usage_minimum;
import static org.hid4java.windows.DescriptorReconstructor.Items.main_collection;
import static org.hid4java.windows.DescriptorReconstructor.Items.main_collection_end;
import static org.hid4java.windows.DescriptorReconstructor.Items.main_feature;
import static org.hid4java.windows.DescriptorReconstructor.Items.main_input;
import static org.hid4java.windows.DescriptorReconstructor.Items.main_output;
import static org.hid4java.windows.DescriptorReconstructor.MainItems.collection;
import static org.hid4java.windows.DescriptorReconstructor.MainItems.collection_end;
import static org.hid4java.windows.DescriptorReconstructor.MainItems.delimiter_close;
import static org.hid4java.windows.DescriptorReconstructor.MainItems.delimiter_open;
import static org.hid4java.windows.DescriptorReconstructor.MainItems.delimiter_usage;
import static org.hid4java.windows.DescriptorReconstructor.MainItems.feature;
import static org.hid4java.windows.DescriptorReconstructor.MainItems.input;
import static org.hid4java.windows.DescriptorReconstructor.NodeType.item_node_cap;
import static org.hid4java.windows.DescriptorReconstructor.NodeType.item_node_collection;
import static org.hid4java.windows.DescriptorReconstructor.NodeType.item_node_padding;


/**
 * DescriptorReconstructor.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2023-11-01 nsano initial version <br>
 */
public class DescriptorReconstructor {

    static final int NUM_OF_HIDP_REPORT_TYPES = 3;

    enum Items {
        /** 1000 00 nn */
        main_input(0x80),
        /** 1001 00 nn */
        main_output(0x90),
        /** 1011 00 nn */
        main_feature(0xB0),
        /** 1010 00 nn */
        main_collection(0xA0),
        /** 1100 00 nn */
        main_collection_end(0xC0),
        /** 0000 01 nn */
        global_usage_page(0x04),
        /** 0001 01 nn */
        global_logical_minimum(0x14),
        /** 0010 01 nn */
        global_logical_maximum(0x24),
        /** 0011 01 nn */
        global_physical_minimum(0x34),
        /** 0100 01 nn */
        global_physical_maximum(0x44),
        /** 0101 01 nn */
        global_unit_exponent(0x54),
        /** 0110 01 nn */
        global_unit(0x64),
        /** 0111 01 nn */
        global_report_size(0x74),
        /** 1000 01 nn */
        global_report_id(0x84),
        /** 1001 01 nn */
        global_report_count(0x94),
        /** 1010 01 nn */
        global_push(0xA4),
        /** 1011 01 nn */
        global_pop(0xB4),
        /** 0000 10 nn */
        local_usage(0x08),
        /** 0001 10 nn */
        local_usage_minimum(0x18),
        /** 0010 10 nn */
        local_usage_maximum(0x28),
        /** 0011 10 nn */
        local_designator_index(0x38),
        /** 0100 10 nn */
        local_designator_minimum(0x48),
        /** 0101 10 nn */
        local_designator_maximum(0x58),
        /** 0111 10 nn */
        local_string(0x78),
        /** 1000 10 nn */
        local_string_minimum(0x88),
        /** 1001 10 nn */
        local_string_maximum(0x98),
        /** 1010 10 nn */
        local_delimiter(0xA8);
        final int v;

        Items(int v) {
            this.v = v;
        }
    }

    enum MainItems {
        input /* HidP_Input */,
        output /* HidP_Output */,
        feature /* HidP_Feature */,
        collection,
        collection_end,
        delimiter_open,
        delimiter_usage,
        delimiter_close;
    }

    static class BitRange {

        int firstBit;
        int lastBit;
    }

    enum NodeType {
        item_node_cap,
        item_node_padding,
        item_node_collection,
    }

    static class MainItemNode {

        /** Position of first bit in report (counting from 0) */
        int firstBit;
        /** Position of last bit in report (counting from 0) */
        int lastBit;
        /**
         * Information if caps index refers to the array of button caps, value caps,
         * or if the node is just a padding element to fill unused bit positions.
         * The node can also be a collection node without any bits in the report.
         */
        NodeType typeOfNode;
        /** Index in the array of caps */
        int capsIndex;
        /** Index in the array of link collections */
        int collectionIndex;
        /** Input, Output, Feature, Collection or Collection End */
        MainItems mainItemType;
        byte reportID;

        MainItemNode(int firstBit, int lastBit, NodeType typeOfNode, int capsIndex, int collectionIndex, MainItems mainItemType, byte reportId) {
            this.firstBit = firstBit;
            this.lastBit = lastBit;
            this.typeOfNode = typeOfNode;
            this.capsIndex = capsIndex;
            this.collectionIndex = collectionIndex;
            this.mainItemType = mainItemType;
            this.reportID = reportId;
        }
    }

    static class hid_pp_caps_info extends Structure {

        public short FirstCap;
        public short NumberOfCaps; // Includes empty caps after LastCap
        public short LastCap;
        public short ReportByteLength;

        @Override
        protected List<String> getFieldOrder() {
            return List.of("FirstCap", "NumberOfCaps", "LastCap", "ReportByteLength");
        }
    }

    static class hid_pp_link_collection_node extends Structure {

        public short /* USAGE */ LinkUsage;
        public short /* USAGE */ LinkUsagePage;
        public short Parent;
        public short NumberOfChildren;
        public short NextSibling;
        public short FirstChild;
        public int bits;
        static final int CollectionType = 0x0000_00ff;
        static final int IsAlias = 0x0001_0000;
        static final int Reserved = 0xfffe_0000;
        public hid_pp_link_collection_node() {}
        hid_pp_link_collection_node(Pointer p) {
            super(p);
            read();
        }
        // Same as the public API structure HIDP_LINK_COLLECTION_NODE, but without PVOID UserContext at the end
        @Override
        protected List<String> getFieldOrder() {
            return List.of("LinkUsage", "LinkUsagePage", "Parent", "NumberOfChildren", "NextSibling", "FirstChild", "flags");
        }
    }

    static class hidp_unknown_token extends Structure {

        /** Specifies the one-byte prefix of a global item. */
        public byte Token;
        public byte[] Reserved = new byte[3];
        /** Specifies the data part of the global item. */
        public int BitField;

        @Override
        protected List<String> getFieldOrder() {
            return List.of("Token", "Reserved", "StringMin");
        }
    }

    static class hid_pp_cap extends Structure {

        public short /* USAGE */ UsagePage;
        public short /* USAGE */ ReportID;
        public byte BitPosition;
        /** WIN32 term for this is BitSize */
        public short ReportSize;
        public short ReportCount;
        public short BytePosition;
        public short BitCount;
        public int BitField;
        public short NextBytePosition;
        public short LinkCollection;
        public short /* USAGE */ LinkUsagePage;
        public short /* USAGE */ LinkUsage;

        // Start of 8 Flags in one byte
        public byte flags;
        static final int IsMultipleItemsForArray = 0x01 << 0;
        static final int IsPadding = 0x01 << 1;
        static final int IsButtonCap = 0x01 << 2;
        static final int IsAbsolute = 0x01 << 3;
        static final int IsRange = 0x01 << 4;
        static final int IsAlias = 0x01 << 5; // IsAlias is set to TRUE in the first n-1 capability structures added to the capability array. IsAlias set to FALSE in the nth capability structure.
        static final int IsStringRange = 0x01 << 6;
        static final int IsDesignatorRange = 0x01 << 7;
        // End of 8 Flags in one byte
        public boolean[] Reserved1 = new boolean[3];

        public hidp_unknown_token[] UnknownTokens = new hidp_unknown_token[4]; // 4 x 8 Byte

        static class u1 extends Union {

            static class Range extends Structure {

                short /* USAGE */ UsageMin;
                short /* USAGE */ UsageMax;
                short StringMin;
                short StringMax;
                short DesignatorMin;
                short DesignatorMax;
                short DataIndexMin;
                short DataIndexMax;

                @Override
                protected List<String> getFieldOrder() {
                    return List.of("UsageMin", "UsageMax", "StringMin", "StringMax", "DesignatorMin", "DesignatorMax", "DataIndexMin", "DataIndexMax");
                }
            }

            public Range range;

            static class NotRange extends Structure {

                short /* USAGE */ Usage;
                short /* USAGE */ Reserved1;
                short StringIndex;
                short Reserved2;
                short DesignatorIndex;
                short Reserved3;
                short DataIndex;
                short Reserved4;

                @Override
                protected List<String> getFieldOrder() {
                    return List.of("Usage", "Reserved1", "StringIndex", "Reserved2", "DesignatorIndex", "Reserved3", "DataIndex", "Reserved4");
                }
            }

            public NotRange notRange;
        }

        public u1 u1;

        static class u2 extends Union {

            static class Button extends Structure {

                public int LogicalMin;
                public int LogicalMax;

                @Override
                protected List<String> getFieldOrder() {
                    return List.of("LogicalMin", "LogicalMax");
                }
            }

            public Button button;

            static class NotButton extends Structure {

                public boolean HasNull;
                public byte[] Reserved4 = new byte[3];
                public int LogicalMin;
                public int LogicalMax;
                public int PhysicalMin;
                public int PhysicalMax;

                @Override
                protected List<String> getFieldOrder() {
                    return List.of("HasNull", "Reserved4", "LogicalMin", "LogicalMax", "PhysicalMin", "PhysicalMax");
                }
            }

            public NotButton notButton;
        }

        public u2 u2;
        public int Units;
        public int UnitsExp;

        @Override
        protected List<String> getFieldOrder() {
            return List.of("UsagePage", "ReportID", "BitPosition", "ReportSize", "ReportCount", "BytePosition",
                    "BitCount", "BitField", "NextBytePosition", "LinkCollection", "LinkUsagePage", "LinkUsage",
                    "flags", "Reserved1", "UnknownTokens", "u1", "u2", "Units", "UnitsExp");
        }
    }

    static class hidp_preparsed_data extends Structure {

        public byte[] MagicKey = new byte[8];
        public short /* USAGE */ Usage;
        public short /* USAGE */ UsagePage;
        public short[] Reserved = new short[2];

        // CAPS structure for Input, Output and Feature
        public hid_pp_caps_info[] caps_info = new hid_pp_caps_info[3];

        public short FirstByteOfLinkCollectionArray;
        public short NumberLinkCollectionNodes;

        static class u extends Union {

            public hid_pp_cap[] caps;
            public hid_pp_link_collection_node[] LinkCollectionArray;

            @Override
            protected List<String> getFieldOrder() {
                return List.of("caps", "LinkCollectionArray");
            }
        }

        public u u;

        hidp_preparsed_data(Pointer p) {
            super(p);
            read();
        }

        @Override
        protected List<String> getFieldOrder() {
            return List.of("MagicKey", "Usage", "UsagePage", "Reserved", "caps_info",
                    "FirstByteOfLinkCollectionArray", "NumberLinkCollectionNodes", "u");
        }
    }

    /**
     * References to report descriptor buffer.
     */
    static class Buffer {

        /** Pointer to the array which stores the reconstructed descriptor */
        byte[] buf;
        /** Size of the buffer in bytes */
        int bufSize;
        /** Index of the next report byte to write to buf array */
        int byteIdx;

        /**
         * Writes a short report descriptor item according USB HID spec 1.11 chapter 6.2.2.2.
         *
         * @param item  Enumeration identifying type (Main, Global, Local) and function (e.g Usage or Report Count) of the item.
         * @param data     Data (Size depends on item 0,1,2 or 4bytes).
         * @return Returns 0 if successful, -1 for error.
         */
        int write_short_item(Items item, long data) {
            if ((item.v & 0x03) != 0) {
                // Invalid input data, last to bits are reserved for data size
                return -1;
            }

            if (item == main_collection_end) {
                // Item without data (1Byte prefix only)
                byte oneBytePrefix = (byte) (item.v + 0x00);
                append_byte(oneBytePrefix);
            } else if ((item == global_logical_minimum) ||
                    (item == global_logical_maximum) ||
                    (item == global_physical_minimum) ||
                    (item == global_physical_maximum)) {
                // Item with signed integer data
                if ((data >= -128) && (data <= 127)) {
                    // 1Byte prefix + 1Byte data
                    byte oneBytePrefix = (byte) (item.v + 0x01);
                    char localData = (char) data;
                    append_byte(oneBytePrefix);
                    append_byte((byte) (localData & 0xFF));
                } else if ((data >= -32768) && (data <= 32767)) {
                    // 1Byte prefix + 2Byte data
                    byte oneBytePrefix = (byte) (item.v + 0x02);
                    short localData = (short) data;
                    append_byte(oneBytePrefix);
                    append_byte((byte) (localData & 0xFF));
                    append_byte((byte) (localData >> 8 & 0xFF));
                } else if ((data >= -2147483648L) && (data <= 2147483647)) {
                    // 1Byte prefix + 4Byte data
                    byte oneBytePrefix = (byte) (item.v + 0x03);
                    int localData = (int) data;
                    append_byte(oneBytePrefix);
                    append_byte((byte) (localData & 0xFF));
                    append_byte((byte) (localData >> 8 & 0xFF));
                    append_byte((byte) (localData >> 16 & 0xFF));
                    append_byte((byte) (localData >> 24 & 0xFF));
                } else {
                    // Data out of 32 bit signed integer range
                    return -1;
                }
            } else {
                // Item with unsigned integer data
                if ((data >= 0) && (data <= 0xFF)) {
                    // 1Byte prefix + 1Byte data
                    byte oneBytePrefix = (byte) (item.v + 0x01);
                    byte localData = (byte) data;
                    append_byte(oneBytePrefix);
                    append_byte((byte) (localData & 0xFF));
                } else if ((data >= 0) && (data <= 0xFFFF)) {
                    // 1Byte prefix + 2Byte data
                    byte oneBytePrefix = (byte) (item.v + 0x02);
                    short localData = (short) data;
                    append_byte(oneBytePrefix);
                    append_byte((byte) (localData & 0xFF));
                    append_byte((byte) (localData >> 8 & 0xFF));
                } else if ((data >= 0) && (data <= 0xFFFFFFFFL)) {
                    // 1Byte prefix + 4Byte data
                    byte oneBytePrefix = (byte) (item.v + 0x03);
                    int localData = (int) data;
                    append_byte(oneBytePrefix);
                    append_byte((byte) (localData & 0xFF));
                    append_byte((byte) (localData >> 8 & 0xFF));
                    append_byte((byte) (localData >> 16 & 0xFF));
                    append_byte((byte) (localData >> 24 & 0xFF));
                } else {
                    // Data out of 32 bit unsigned integer range
                    return -1;
                }
            }
            return 0;
        }

        /**
         * Function that appends a byte to encoded report descriptor buffer.
         *
         * @param b Single byte to append.
         */
        void append_byte(byte b) {
            if (this.byteIdx < this.bufSize) {
                this.buf[this.byteIdx] = b;
                this.byteIdx++;
            }
        }
    }

    /** Determine first INPUT/OUTPUT/FEATURE main item, where the last bit position is equal or greater than the search bit position */
    private static int search_main_item_list_for_bit_position(int search_bit, MainItems main_item_type, byte report_id, MainItemNode[] list, int index) {

        for (int i = index; i < list.length; i++) {
            MainItemNode curr = list[i];
            if ((curr.mainItemType.ordinal() != collection.ordinal()) &&
                (curr.mainItemType.ordinal() != collection_end.ordinal()) &&
                !((curr.lastBit >= search_bit) &&
                        (curr.reportID == report_id) &&
                        (curr.mainItemType == main_item_type))) {
                return i;
            }
        }
        return 0;
    }

    static int hid_winapi_descriptor_reconstruct_pp_data(Pointer preparsed_data, byte[] buf, int buf_size) {
        hidp_preparsed_data pp_data = new hidp_preparsed_data(preparsed_data);

        // Check if MagicKey is correct, to ensure that pp_data points to a valid preparse data structure
        if (Arrays.compare(pp_data.MagicKey, "HidP KDR".getBytes()) != 0) {
            return -1;
        }

        Buffer rpt_desc = new Buffer();
        rpt_desc.buf = buf;
        rpt_desc.bufSize = buf_size;
        rpt_desc.byteIdx = 0;

        // Set pointer to the first node of link_collection_nodes
        Pointer p_link_collection_nodes = new Pointer(Pointer.nativeValue(pp_data.u.getPointer()) + pp_data.FirstByteOfLinkCollectionArray);
        hid_pp_link_collection_node[] link_collection_nodes = new hid_pp_link_collection_node[pp_data.NumberLinkCollectionNodes];

        //
        // Create lookup tables for the bit range of each report per collection (position of first bit and last bit in each collection)
        // coll_bit_range[COLLECTION_INDEX][REPORT_ID][INPUT/OUTPUT/FEATURE]
        //

        // Allocate memory and initialize lookup table
        BitRange[][][] coll_bit_range;
        coll_bit_range = new BitRange[pp_data.NumberLinkCollectionNodes][][];
        for (int collection_node_idx = 0; collection_node_idx < pp_data.NumberLinkCollectionNodes; collection_node_idx++) {
            coll_bit_range[collection_node_idx] = new BitRange[256 * coll_bit_range[0].length][]; // 256 possible report IDs (incl. 0x00)
            for (int reportid_idx = 0; reportid_idx < 256; reportid_idx++) {
                coll_bit_range[collection_node_idx][reportid_idx] = new BitRange[NUM_OF_HIDP_REPORT_TYPES * coll_bit_range[0][0].length];
                for (int /* HIDP_REPORT_TYPE */ rt_idx = 0; rt_idx < NUM_OF_HIDP_REPORT_TYPES; rt_idx++) {
                    coll_bit_range[collection_node_idx][reportid_idx][rt_idx] = new BitRange();
                    coll_bit_range[collection_node_idx][reportid_idx][rt_idx].firstBit = -1;
                    coll_bit_range[collection_node_idx][reportid_idx][rt_idx].lastBit = -1;
                }
            }
        }

        // Fill the lookup table where caps exist
        for (int /* HIDP_REPORT_TYPE */ rt_idx = 0; rt_idx < NUM_OF_HIDP_REPORT_TYPES; rt_idx++) {
            for (int caps_idx = pp_data.caps_info[rt_idx].FirstCap; caps_idx < pp_data.caps_info[rt_idx].LastCap; caps_idx++) {
                int first_bit, last_bit;
                first_bit = (pp_data.u.caps[caps_idx].BytePosition - 1) * 8
                        + pp_data.u.caps[caps_idx].BitPosition;
                last_bit = first_bit + pp_data.u.caps[caps_idx].ReportSize
                        * pp_data.u.caps[caps_idx].ReportCount - 1;
                if (coll_bit_range[pp_data.u.caps[caps_idx].LinkCollection][pp_data.u.caps[caps_idx].ReportID][rt_idx].firstBit == -1 ||
                        coll_bit_range[pp_data.u.caps[caps_idx].LinkCollection][pp_data.u.caps[caps_idx].ReportID][rt_idx].firstBit > first_bit) {
                    coll_bit_range[pp_data.u.caps[caps_idx].LinkCollection][pp_data.u.caps[caps_idx].ReportID][rt_idx].firstBit = first_bit;
                }
                if (coll_bit_range[pp_data.u.caps[caps_idx].LinkCollection][pp_data.u.caps[caps_idx].ReportID][rt_idx].lastBit < last_bit) {
                    coll_bit_range[pp_data.u.caps[caps_idx].LinkCollection][pp_data.u.caps[caps_idx].ReportID][rt_idx].lastBit = last_bit;
                }
            }
        }

        //
        // -Determine hierarchy levels of each collection and store it in:
        //  coll_levels[COLLECTION_INDEX]
        // -Determine number of direct children of each collection and store it in:
        //  coll_number_of_direct_children[COLLECTION_INDEX]
        //
        int max_coll_level = 0;
        int[] coll_levels = new int[pp_data.NumberLinkCollectionNodes];
        int[] coll_number_of_direct_children = new int[pp_data.NumberLinkCollectionNodes];
        for (int collection_node_idx = 0; collection_node_idx < pp_data.NumberLinkCollectionNodes; collection_node_idx++) {
            coll_levels[collection_node_idx] = -1;
            coll_number_of_direct_children[collection_node_idx] = 0;
        }

        {
            int actual_coll_level = 0;
            int collection_node_idx = 0;
            while (actual_coll_level >= 0) {
                coll_levels[collection_node_idx] = actual_coll_level;
                link_collection_nodes[collection_node_idx] = new hid_pp_link_collection_node(p_link_collection_nodes);
                p_link_collection_nodes = new Pointer(Pointer.nativeValue(p_link_collection_nodes) + link_collection_nodes[collection_node_idx].size());
                if ((link_collection_nodes[collection_node_idx].NumberOfChildren > 0) &&
                        (coll_levels[link_collection_nodes[collection_node_idx].FirstChild] == -1)) {
                    actual_coll_level++;
                    coll_levels[collection_node_idx] = actual_coll_level;
                    if (max_coll_level < actual_coll_level) {
                        max_coll_level = actual_coll_level;
                    }
                    coll_number_of_direct_children[collection_node_idx]++;
                    collection_node_idx = link_collection_nodes[collection_node_idx].FirstChild;
                } else if (link_collection_nodes[collection_node_idx].NextSibling != 0) {
                    coll_number_of_direct_children[link_collection_nodes[collection_node_idx].Parent]++;
                    collection_node_idx = link_collection_nodes[collection_node_idx].NextSibling;
                } else {
                    actual_coll_level--;
                    if (actual_coll_level >= 0) {
                        collection_node_idx = link_collection_nodes[collection_node_idx].Parent;
                    }
                }
            }
        }

        //
        // Propagate the bit range of each report from the child collections to their parent
        // and store the merged result for the parent
        //
        for (int actual_coll_level = max_coll_level - 1; actual_coll_level >= 0; actual_coll_level--) {
            for (int collection_node_idx = 0; collection_node_idx < pp_data.NumberLinkCollectionNodes; collection_node_idx++) {
                if (coll_levels[collection_node_idx] == actual_coll_level) {
                    int child_idx = link_collection_nodes[collection_node_idx].FirstChild;
                    while (child_idx != 0) {
                        for (int reportid_idx = 0; reportid_idx < 256; reportid_idx++) {
                            for (int /* HIDP_REPORT_TYPE */ rt_idx = 0; rt_idx < NUM_OF_HIDP_REPORT_TYPES; rt_idx++) {
                                // Merge bit range from children
                                if ((coll_bit_range[child_idx][reportid_idx][rt_idx].firstBit != -1) &&
                                        (coll_bit_range[collection_node_idx][reportid_idx][rt_idx].firstBit > coll_bit_range[child_idx][reportid_idx][rt_idx].firstBit)) {
                                    coll_bit_range[collection_node_idx][reportid_idx][rt_idx].firstBit = coll_bit_range[child_idx][reportid_idx][rt_idx].firstBit;
                                }
                                if (coll_bit_range[collection_node_idx][reportid_idx][rt_idx].lastBit < coll_bit_range[child_idx][reportid_idx][rt_idx].lastBit) {
                                    coll_bit_range[collection_node_idx][reportid_idx][rt_idx].lastBit = coll_bit_range[child_idx][reportid_idx][rt_idx].lastBit;
                                }
                                child_idx = link_collection_nodes[child_idx].NextSibling;
                            }
                        }
                    }
                }
            }
        }

        //
        // Determine child collection order of the whole hierarchy, based on previously determined bit ranges
        // and store it this index coll_child_order[COLLECTION_INDEX][DIRECT_CHILD_INDEX]
        //
        int[][] coll_child_order;
        coll_child_order = new int[pp_data.NumberLinkCollectionNodes][];
        {
            boolean[] coll_parsed_flag = new boolean[pp_data.NumberLinkCollectionNodes];
            Arrays.fill(coll_parsed_flag, false);
            int actual_coll_level = 0;
            int collection_node_idx = 0;
            while (actual_coll_level >= 0) {
                if ((coll_number_of_direct_children[collection_node_idx] != 0) &&
                        (!coll_parsed_flag[link_collection_nodes[collection_node_idx].FirstChild])) {
                    coll_parsed_flag[link_collection_nodes[collection_node_idx].FirstChild] = true;
                    coll_child_order[collection_node_idx] = new int[coll_number_of_direct_children[collection_node_idx]];

                    {
                        // Create list of child collection indices
                        // sorted reverse to the order returned to HidP_GetLinkCollectionNodeschild
                        // which seems to match the original order, as long as no bit position needs to be considered
                        int child_idx = link_collection_nodes[collection_node_idx].FirstChild;
                        int child_count = coll_number_of_direct_children[collection_node_idx] - 1;
                        coll_child_order[collection_node_idx][child_count] = child_idx;
                        while (link_collection_nodes[child_idx].NextSibling != 0) {
                            child_count--;
                            child_idx = link_collection_nodes[child_idx].NextSibling;
                            coll_child_order[collection_node_idx][child_count] = child_idx;
                        }
                    }

                    if (coll_number_of_direct_children[collection_node_idx] > 1) {
                        // Sort child collections indices by bit positions
                        for (int /* HIDP_REPORT_TYPE */ rt_idx = 0; rt_idx < NUM_OF_HIDP_REPORT_TYPES; rt_idx++) {
                            for (int reportid_idx = 0; reportid_idx < 256; reportid_idx++) {
                                for (int child_idx = 1; child_idx < coll_number_of_direct_children[collection_node_idx]; child_idx++) {
                                    // since the coll_bit_range array is not sorted, we need to reference the collection index in
                                    // our sorted coll_child_order array, and look up the corresponding bit ranges for comparing values to sort
                                    int prev_coll_idx = coll_child_order[collection_node_idx][child_idx - 1];
                                    int cur_coll_idx = coll_child_order[collection_node_idx][child_idx];
                                    if ((coll_bit_range[prev_coll_idx][reportid_idx][rt_idx].firstBit != -1) &&
                                            (coll_bit_range[cur_coll_idx][reportid_idx][rt_idx].firstBit != -1) &&
                                            (coll_bit_range[prev_coll_idx][reportid_idx][rt_idx].firstBit > coll_bit_range[cur_coll_idx][reportid_idx][rt_idx].firstBit)) {
                                        // Swap position indices of the two compared child collections
                                        int idx_latch = coll_child_order[collection_node_idx][child_idx - 1];
                                        coll_child_order[collection_node_idx][child_idx - 1] = coll_child_order[collection_node_idx][child_idx];
                                        coll_child_order[collection_node_idx][child_idx] = idx_latch;
                                    }
                                }
                            }
                        }
                    }
                    actual_coll_level++;
                    collection_node_idx = link_collection_nodes[collection_node_idx].FirstChild;
                } else if (link_collection_nodes[collection_node_idx].NextSibling != 0) {
                    collection_node_idx = link_collection_nodes[collection_node_idx].NextSibling;
                } else {
                    actual_coll_level--;
                    if (actual_coll_level >= 0) {
                        collection_node_idx = link_collection_nodes[collection_node_idx].Parent;
                    }
                }
            }
        }

        //
        // Create sorted main_item_list containing all the Collection and CollectionEnd main items
        //
        List<MainItemNode> main_item_list = new ArrayList<>(); // List root
        int p_main_item_list = 0;
        // Lookup table to find the Collection items in the list by index
        MainItemNode[] coll_begin_lookup = new MainItemNode[pp_data.NumberLinkCollectionNodes];
        MainItemNode[] coll_end_lookup = new MainItemNode[pp_data.NumberLinkCollectionNodes];
        {
            int[] coll_last_written_child = new int[pp_data.NumberLinkCollectionNodes];
            Arrays.fill(coll_last_written_child, -1);

            int actual_coll_level = 0;
            int collection_node_idx = 0;
            int firstDelimiterNode = -1;
            int delimiterCloseNode = -1;
            coll_begin_lookup[0] = new MainItemNode(0, 0, item_node_collection, 0, collection_node_idx, collection, (byte) 0);
            main_item_list.add(coll_begin_lookup[0]);
            while (actual_coll_level >= 0) {
                if ((coll_number_of_direct_children[collection_node_idx] != 0) &&
                        (coll_last_written_child[collection_node_idx] == -1)) {
                    // Collection has child collections, but none is written to the list yet

                    coll_last_written_child[collection_node_idx] = coll_child_order[collection_node_idx][0];
                    collection_node_idx = coll_child_order[collection_node_idx][0];

                    // In a HID Report Descriptor, the first usage declared is the most preferred usage for the control.
                    // While the order in the WIN32 capability strutures is the opposite:
                    // Here the preferred usage is the last aliased usage in the sequence.

                    if ((link_collection_nodes[collection_node_idx].bits & IsAlias) != 0 && (firstDelimiterNode == -1)) {
                        // Aliased Collection (First node in link_collection_nodes . Last entry in report descriptor output)
                        firstDelimiterNode = main_item_list.size() - 1;
                        coll_begin_lookup[collection_node_idx] = new MainItemNode(0, 0, item_node_collection, 0, collection_node_idx, delimiter_usage, (byte) 0);
                        main_item_list.add(coll_begin_lookup[collection_node_idx]);
                        coll_begin_lookup[collection_node_idx] = new MainItemNode(0, 0, item_node_collection, 0, collection_node_idx, delimiter_close, (byte) 0);
                        main_item_list.add(coll_begin_lookup[collection_node_idx]);
                        delimiterCloseNode = main_item_list.size() - 1;
                    } else {
                        // Normal not aliased collection
                        coll_begin_lookup[collection_node_idx] = new MainItemNode(0, 0, item_node_collection, 0, collection_node_idx, collection, (byte) 0);
                        main_item_list.add(coll_begin_lookup[collection_node_idx]);
                        actual_coll_level++;
                    }

                } else if ((coll_number_of_direct_children[collection_node_idx] > 1) &&
                        (coll_last_written_child[collection_node_idx] != coll_child_order[collection_node_idx][coll_number_of_direct_children[collection_node_idx] - 1])) {
                    // Collection has child collections, and this is not the first child

                    int nextChild = 1;
                    while (coll_last_written_child[collection_node_idx] != coll_child_order[collection_node_idx][nextChild - 1]) {
                        nextChild++;
                    }
                    coll_last_written_child[collection_node_idx] = coll_child_order[collection_node_idx][nextChild];
                    collection_node_idx = coll_child_order[collection_node_idx][nextChild];

                    if ((link_collection_nodes[collection_node_idx].bits & IsAlias) != 0 && (firstDelimiterNode == -1)) {
                        // Aliased Collection (First node in link_collection_nodes . Last entry in report descriptor output)
                        firstDelimiterNode = main_item_list.size() - 1;
                        coll_begin_lookup[collection_node_idx] = new MainItemNode(0, 0, item_node_collection, 0, collection_node_idx, delimiter_usage, (byte) 0);
                        main_item_list.add(coll_begin_lookup[collection_node_idx]);
                        coll_begin_lookup[collection_node_idx] = new MainItemNode(0, 0, item_node_collection, 0, collection_node_idx, delimiter_close, (byte) 0);
                        main_item_list.add(coll_begin_lookup[collection_node_idx]);
                        delimiterCloseNode = main_item_list.size() - 1;
                    } else if ((link_collection_nodes[collection_node_idx].bits & IsAlias) != 0 && (firstDelimiterNode != -1)) {
                        // Insert item after the main item node referenced by list
                        MainItemNode newNode = new MainItemNode(0, 0, item_node_collection, 0, collection_node_idx, delimiter_usage, (byte) 0);
                        main_item_list.add(firstDelimiterNode - p_main_item_list, newNode);
                        coll_begin_lookup[collection_node_idx] = newNode;
                    } else if ((link_collection_nodes[collection_node_idx].bits & IsAlias) == 0 && (firstDelimiterNode != -1)) {
                        // Insert item after the main item node referenced by list
                        MainItemNode newNode1 = new MainItemNode(0, 0, item_node_collection, 0, collection_node_idx, delimiter_usage, (byte) 0);
                        main_item_list.add(firstDelimiterNode - p_main_item_list, newNode1);
                        coll_begin_lookup[collection_node_idx] = newNode1;
                        // Insert item after the main item node referenced by list
                        MainItemNode newNode = new MainItemNode(0, 0, item_node_collection, 0, collection_node_idx, delimiter_open, (byte) 0);
                        main_item_list.add(firstDelimiterNode - p_main_item_list, newNode);
                        coll_begin_lookup[collection_node_idx] = newNode;
                        firstDelimiterNode = -1;
                        p_main_item_list = delimiterCloseNode;
                        delimiterCloseNode = -1; // Last entry of alias has .IsAlias == false
                    }
                    if ((link_collection_nodes[collection_node_idx].bits & IsAlias) == 0) {
                        coll_begin_lookup[collection_node_idx] = new MainItemNode(0, 0, item_node_collection, 0, collection_node_idx, collection, (byte) 0);
                        main_item_list.add(coll_begin_lookup[collection_node_idx]);
                        actual_coll_level++;
                    }
                } else {
                    actual_coll_level--;
                    coll_end_lookup[collection_node_idx] = new MainItemNode(0, 0, item_node_collection, 0, collection_node_idx, collection_end, (byte) 0);
                    main_item_list.add(coll_end_lookup[collection_node_idx]);
                    collection_node_idx = link_collection_nodes[collection_node_idx].Parent;
                }
            }
        }

        //
        // Inserted Input/Output/Feature main items into the main_item_list
        // in order of reconstructed bit positions
        //
        for (int /* HIDP_REPORT_TYPE */ rt_idx = 0; rt_idx < NUM_OF_HIDP_REPORT_TYPES; rt_idx++) {
            // Add all value caps to node list
            int firstDelimiterNode = -1;
            int delimiterCloseNode = -1;
            for (int caps_idx = pp_data.caps_info[rt_idx].FirstCap; caps_idx < pp_data.caps_info[rt_idx].LastCap; caps_idx++) {
                int coll_begin = pp_data.u.caps[caps_idx].LinkCollection;
                int first_bit, last_bit;
                first_bit = (pp_data.u.caps[caps_idx].BytePosition - 1) * 8 +
                        pp_data.u.caps[caps_idx].BitPosition;
                last_bit = first_bit + pp_data.u.caps[caps_idx].ReportSize *
                        pp_data.u.caps[caps_idx].ReportCount - 1;

                for (int child_idx = 0; child_idx < coll_number_of_direct_children[pp_data.u.caps[caps_idx].LinkCollection]; child_idx++) {
                    // Determine in which section before/between/after child collection the item should be inserted
                    if (first_bit < coll_bit_range[coll_child_order[pp_data.u.caps[caps_idx].LinkCollection][child_idx]][pp_data.u.caps[caps_idx].ReportID][rt_idx].firstBit) {
                        // Note, that the default value for undefined coll_bit_range is -1, which can't be greater than the bit position
                        break;
                    }
                    coll_begin = coll_child_order[pp_data.u.caps[caps_idx].LinkCollection][child_idx];
                }
                int list_node_p = search_main_item_list_for_bit_position(first_bit, MainItems.values()[rt_idx], (byte) pp_data.u.caps[caps_idx].ReportID, coll_end_lookup, coll_begin);
                List<MainItemNode> list_node = List.of(coll_end_lookup);

                // In a HID Report Descriptor, the first usage declared is the most preferred usage for the control.
                // While the order in the WIN32 capability strutures is the opposite:
                // Here the preferred usage is the last aliased usage in the sequence.

                if ((pp_data.u.caps[caps_idx].flags & hid_pp_cap.IsAlias) != 0 && (firstDelimiterNode == -1)) {
                    // Aliased Usage (First node in pp_data.u.caps . Last entry in report descriptor output)
                    firstDelimiterNode = list_node.size() - 1;
                    // Insert item after the main item node referenced by list
                    MainItemNode newNode1 = new MainItemNode(first_bit, last_bit, item_node_cap, caps_idx, pp_data.u.caps[caps_idx].LinkCollection, delimiter_usage, (byte) pp_data.u.caps[caps_idx].ReportID);
                    list_node.add(list_node_p, newNode1);
                    // Insert item after the main item node referenced by list
                    MainItemNode newNode = new MainItemNode(first_bit, last_bit, item_node_cap, caps_idx, pp_data.u.caps[caps_idx].LinkCollection, delimiter_close, (byte) pp_data.u.caps[caps_idx].ReportID);
                    list_node.add(list_node_p, newNode);
                    delimiterCloseNode = list_node.size() - 1;
                } else if ((pp_data.u.caps[caps_idx].flags & hid_pp_cap.IsAlias) != 0 && (firstDelimiterNode != -1)) {
                    // Insert item after the main item node referenced by list
                    MainItemNode newNode = new MainItemNode(first_bit, last_bit, item_node_cap, caps_idx, pp_data.u.caps[caps_idx].LinkCollection, delimiter_usage, (byte) pp_data.u.caps[caps_idx].ReportID);
                    list_node.add(list_node_p, newNode);
                } else if ((pp_data.u.caps[caps_idx].flags & hid_pp_cap.IsAlias) == 0 && (firstDelimiterNode != -1)) {
                    // Aliased Collection (Last node in pp_data.u.caps . First entry in report descriptor output)
                    // Insert item after the main item node referenced by list
                    MainItemNode newNode1 = new MainItemNode(first_bit, last_bit, item_node_cap, caps_idx, pp_data.u.caps[caps_idx].LinkCollection, delimiter_usage, (byte) pp_data.u.caps[caps_idx].ReportID);
                    list_node.add(list_node_p, newNode1);
                    // Insert item after the main item node referenced by list
                    MainItemNode newNode = new MainItemNode(first_bit, last_bit, item_node_cap, caps_idx, pp_data.u.caps[caps_idx].LinkCollection, delimiter_open, (byte) pp_data.u.caps[caps_idx].ReportID);
                    list_node.add(list_node_p, newNode);
                    firstDelimiterNode = -1;
                    list_node_p = delimiterCloseNode;
                    delimiterCloseNode = -1; // Last entry of alias has .IsAlias == false
                }
                if ((pp_data.u.caps[caps_idx].flags & hid_pp_cap.IsAlias) == 0) {
                    // Insert item after the main item node referenced by list
                    MainItemNode newNode = new MainItemNode(first_bit, last_bit, item_node_cap, caps_idx, (int) pp_data.u.caps[caps_idx].LinkCollection, MainItems.values()[rt_idx], (byte) pp_data.u.caps[caps_idx].ReportID);
                    list_node.add(list_node_p, newNode);
                }
            }
        }

        //
        // Add const main items for padding to main_item_list
        // -To fill all bit gaps
        // -At each report end for 8bit padding
        //  Note that information about the padding at the report end,
        //  is not stored in the preparsed data, but in practice all
        //  report descriptors seem to have it, as assumed here.
        //
        {
            int[][] last_bit_position = new int[NUM_OF_HIDP_REPORT_TYPES][256];
            MainItemNode[][] last_report_item_lookup = new MainItemNode[NUM_OF_HIDP_REPORT_TYPES][256];
            for (int /* HIDP_REPORT_TYPE */ rt_idx = 0; rt_idx < NUM_OF_HIDP_REPORT_TYPES; rt_idx++) {
                for (int reportid_idx = 0; reportid_idx < 256; reportid_idx++) {
                    last_bit_position[rt_idx][reportid_idx] = -1;
                    last_report_item_lookup[rt_idx][reportid_idx] = null;
                }
            }

            for (MainItemNode curr : main_item_list) {
                if ((curr.mainItemType.ordinal() >= input.ordinal()) &&
                        (curr.mainItemType.ordinal() <= feature.ordinal())) {
                    // INPUT, OUTPUT or FEATURE
                    if (curr.firstBit != -1) {
                        if ((last_bit_position[curr.mainItemType.ordinal()][curr.reportID] + 1 != curr.firstBit) &&
                                (last_report_item_lookup[curr.mainItemType.ordinal()][curr.reportID] != null) &&
                                (last_report_item_lookup[curr.mainItemType.ordinal()][curr.reportID].firstBit != curr.firstBit) // Happens in case of IsMultipleItemsForArray for multiple dedicated usages for a multi-button array
                        ) {
                            int index = search_main_item_list_for_bit_position(last_bit_position[curr.mainItemType.ordinal()][curr.reportID], curr.mainItemType, curr.reportID, last_report_item_lookup[curr.mainItemType.ordinal()], curr.reportID);
                            List<MainItemNode> list_node = List.of(last_report_item_lookup[curr.mainItemType.ordinal()]);
                            // Insert item after the main item node referenced by list
                            MainItemNode newNode = new MainItemNode(last_bit_position[curr.mainItemType.ordinal()][curr.reportID] + 1, curr.firstBit - 1, item_node_padding, -1, 0, curr.mainItemType, curr.reportID);
                            list_node.add(index, newNode);
                        }
                        last_bit_position[curr.mainItemType.ordinal()][curr.reportID] = curr.lastBit;
                        last_report_item_lookup[curr.mainItemType.ordinal()][curr.reportID] = curr;
                    }
                }
            }
            // Add 8 bit padding at each report end
            for (int /* HIDP_REPORT_TYPE */ rt_idx = 0; rt_idx < NUM_OF_HIDP_REPORT_TYPES; rt_idx++) {
                for (int reportid_idx = 0; reportid_idx < 256; reportid_idx++) {
                    if (last_bit_position[rt_idx][reportid_idx] != -1) {
                        int padding = 8 - ((last_bit_position[rt_idx][reportid_idx] + 1) % 8);
                        if (padding < 8) {
                            // Insert padding item after item referenced in last_report_item_lookup
                            List<MainItemNode> list_node = List.of(last_report_item_lookup[rt_idx]);
                            // Insert item after the main item node referenced by list
                            MainItemNode newNode = new MainItemNode(last_bit_position[rt_idx][reportid_idx] + 1, last_bit_position[rt_idx][reportid_idx] + padding, item_node_padding, -1, 0, MainItems.values()[rt_idx], (byte) reportid_idx);
                            list_node.add(reportid_idx, newNode);
                        }
                    }
                }
            }
        }

        //
        // Encode the report descriptor output
        //
        byte last_report_id = 0;
        short last_usage_page = 0;
        long last_physical_min = 0;// If both, Physical Minimum and Physical Maximum are 0, the logical limits should be taken as physical limits according USB HID spec 1.11 chapter 6.2.2.7
        long last_physical_max = 0;
        long last_unit_exponent = 0; // If Unit Exponent is Undefined it should be considered as 0 according USB HID spec 1.11 chapter 6.2.2.7
        long last_unit = 0; // If the first nibble is 7, or second nibble of Unit is 0, the unit is None according USB HID spec 1.11 chapter 6.2.2.7
        boolean inhibit_write_of_usage = false; // Needed in case of delimited usage print, before the normal collection or cap
        int report_count = 0;
        for (int i = 0; i < main_item_list.size(); i++) {
            MainItemNode curr_item_list = main_item_list.get(i);
            int rt_idx = curr_item_list.mainItemType.ordinal();
            int caps_idx = curr_item_list.capsIndex;
            if (curr_item_list.mainItemType == collection) {
                if (last_usage_page != link_collection_nodes[curr_item_list.collectionIndex].LinkUsagePage) {
                    // Write "Usage Page" at the beginning of a collection - except it refers the same table as wrote last
                    rpt_desc.write_short_item(global_usage_page, link_collection_nodes[curr_item_list.collectionIndex].LinkUsagePage);
                    last_usage_page = link_collection_nodes[curr_item_list.collectionIndex].LinkUsagePage;
                }
                if (inhibit_write_of_usage) {
                    // Inhibit only once after DELIMITER statement
                    inhibit_write_of_usage = false;
                } else {
                    // Write "Usage" of collection
                    rpt_desc.write_short_item(local_usage, link_collection_nodes[curr_item_list.collectionIndex].LinkUsage);
                }
                // Write begin of "Collection"
                rpt_desc.write_short_item(main_collection, link_collection_nodes[curr_item_list.collectionIndex].bits & CollectionType);
            } else if (curr_item_list.mainItemType == collection_end) {
                // Write "End Collection"
                rpt_desc.write_short_item(main_collection_end, 0);
            } else if (curr_item_list.mainItemType == delimiter_open) {
                if (curr_item_list.collectionIndex != -1) {
                    // Write "Usage Page" inside a collection delimiter section
                    if (last_usage_page != link_collection_nodes[curr_item_list.collectionIndex].LinkUsagePage) {
                        rpt_desc.write_short_item(global_usage_page, link_collection_nodes[curr_item_list.collectionIndex].LinkUsagePage);
                        last_usage_page = link_collection_nodes[curr_item_list.collectionIndex].LinkUsagePage;
                    }
                } else if (curr_item_list.capsIndex != 0) {
                    // Write "Usage Page" inside a main item delimiter section
                    if (pp_data.u.caps[caps_idx].UsagePage != last_usage_page) {
                        rpt_desc.write_short_item(global_usage_page, pp_data.u.caps[caps_idx].UsagePage);
                        last_usage_page = pp_data.u.caps[caps_idx].UsagePage;
                    }
                }
                // Write "Delimiter Open"
                rpt_desc.write_short_item(local_delimiter, 1); // 1 = open set of aliased usages
            } else if (curr_item_list.mainItemType == delimiter_usage) {
                if (curr_item_list.collectionIndex != -1) {
                    // Write aliased collection "Usage"
                    rpt_desc.write_short_item(local_usage, link_collection_nodes[curr_item_list.collectionIndex].LinkUsage);
                }
                if (curr_item_list.capsIndex != 0) {
                    // Write aliased main item range from "Usage Minimum" to "Usage Maximum"
                    if ((pp_data.u.caps[caps_idx].flags & IsRange) != 0) {
                        rpt_desc.write_short_item(local_usage_minimum, pp_data.u.caps[caps_idx].u1.range.UsageMin);
                        rpt_desc.write_short_item(local_usage_maximum, pp_data.u.caps[caps_idx].u1.range.UsageMax);
                    } else {
                        // Write single aliased main item "Usage"
                        rpt_desc.write_short_item(local_usage, pp_data.u.caps[caps_idx].u1.notRange.Usage);
                    }
                }
            } else if (curr_item_list.mainItemType == delimiter_close) {
                // Write "Delimiter Close"
                rpt_desc.write_short_item(local_delimiter, 0); // 0 = close set of aliased usages
                // Inhibit next usage write
                inhibit_write_of_usage = true;
            } else if (curr_item_list.typeOfNode == item_node_padding) {
                // Padding
                // The preparsed data doesn't contain any information about padding. Therefore all undefined gaps
                // in the reports are filled with the same style of constant padding.

                // Write "Report Size" with number of padding bits
                rpt_desc.write_short_item(global_report_size, (curr_item_list.lastBit - curr_item_list.firstBit + 1));

                // Write "Report Count" for padding always as 1
                rpt_desc.write_short_item(global_report_count, 1);

                if (rt_idx == HidP_Input.ordinal()) {
                    // Write "Input" main item - We know it's Constant - We can only guess the other bits, but they don't matter in case of const
                    rpt_desc.write_short_item(main_input, 0x03); // Const / Abs
                } else if (rt_idx == HidP_Output.ordinal()) {
                    // Write "Output" main item - We know it's Constant - We can only guess the other bits, but they don't matter in case of const
                    rpt_desc.write_short_item(main_output, 0x03); // Const / Abs
                } else if (rt_idx == HidP_Feature.ordinal()) {
                    // Write "Feature" main item - We know it's Constant - We can only guess the other bits, but they don't matter in case of const
                    rpt_desc.write_short_item(main_feature, 0x03); // Const / Abs
                }
                report_count = 0;
            } else if ((pp_data.u.caps[caps_idx].flags & IsButtonCap) != 0) {
                // Button
                // (The preparsed data contain different data for 1 bit Button caps, than for parametric Value caps)

                if (last_report_id != pp_data.u.caps[caps_idx].ReportID) {
                    // Write "Report ID" if changed
                    rpt_desc.write_short_item(global_report_id, pp_data.u.caps[caps_idx].ReportID);
                    last_report_id = (byte) pp_data.u.caps[caps_idx].ReportID;
                }

                // Write "Usage Page" when changed
                if (pp_data.u.caps[caps_idx].UsagePage != last_usage_page) {
                    rpt_desc.write_short_item(global_usage_page, pp_data.u.caps[caps_idx].UsagePage);
                    last_usage_page = pp_data.u.caps[caps_idx].UsagePage;
                }

                // Write only local report items for each cap, if ReportCount > 1
                if ((pp_data.u.caps[caps_idx].flags & IsRange) != 0) {
                    report_count += (pp_data.u.caps[caps_idx].u1.range.DataIndexMax - pp_data.u.caps[caps_idx].u1.range.DataIndexMin);
                }

                if (inhibit_write_of_usage) {
                    // Inhibit only once after Delimiter - Reset flags
                    inhibit_write_of_usage = false;
                } else {
                    if ((pp_data.u.caps[caps_idx].flags & IsRange) != 0) {
                        // Write range from "Usage Minimum" to "Usage Maximum"
                        rpt_desc.write_short_item(local_usage_minimum, pp_data.u.caps[caps_idx].u1.range.UsageMin);
                        rpt_desc.write_short_item(local_usage_maximum, pp_data.u.caps[caps_idx].u1.range.UsageMax);
                    } else {
                        // Write single "Usage"
                        rpt_desc.write_short_item(local_usage, pp_data.u.caps[caps_idx].u1.notRange.Usage);
                    }
                }

                if ((pp_data.u.caps[caps_idx].flags & IsDesignatorRange) == 0) {
                    // Write physical descriptor indices range from "Designator Minimum" to "Designator Maximum"
                    rpt_desc.write_short_item(local_designator_minimum, pp_data.u.caps[caps_idx].u1.range.DesignatorMin);
                    rpt_desc.write_short_item(local_designator_maximum, pp_data.u.caps[caps_idx].u1.range.DesignatorMax);
                } else if (pp_data.u.caps[caps_idx].u1.notRange.DesignatorIndex != 0) {
                    // Designator set 0 is a special descriptor set (of the HID Physical Descriptor),
                    // that specifies the number of additional descriptor sets.
                    // Therefore Designator Index 0 can never be a useful reference for a control and we can inhibit it.
                    // Write single "Designator Index"
                    rpt_desc.write_short_item(local_designator_index, pp_data.u.caps[caps_idx].u1.notRange.DesignatorIndex);
                }

                if ((pp_data.u.caps[caps_idx].flags & IsStringRange) != 0) {
                    // Write range of indices of the USB string descriptor, from "String Minimum" to "String Maximum"
                    rpt_desc.write_short_item(local_string_minimum, pp_data.u.caps[caps_idx].u1.range.StringMin);
                    rpt_desc.write_short_item(local_string_maximum, pp_data.u.caps[caps_idx].u1.range.StringMax);
                } else if (pp_data.u.caps[caps_idx].u1.notRange.StringIndex != 0) {
                    // String Index 0 is a special entry of the USB string descriptor, that contains a list of supported languages,
                    // therefore Designator Index 0 can never be a useful reference for a control and we can inhibit it.
                    // Write single "String Index"
                    rpt_desc.write_short_item(local_string, pp_data.u.caps[caps_idx].u1.notRange.StringIndex);
                }

                if ((i < main_item_list.size() - 1) &&
                        (main_item_list.get(i + 1).mainItemType.ordinal() == rt_idx) &&
                        (main_item_list.get(i + 1).typeOfNode == item_node_cap) &&
                        ((pp_data.u.caps[main_item_list.get(i + 1).capsIndex].flags & IsButtonCap) != 0) &&
                        ((pp_data.u.caps[caps_idx].flags & IsRange) == 0) && // This node in list is no array
                        ((pp_data.u.caps[main_item_list.get(i + 1).capsIndex].flags & IsRange) == 0) && // Next node in list is no array
                        (pp_data.u.caps[main_item_list.get(i + 1).capsIndex].UsagePage == pp_data.u.caps[caps_idx].UsagePage) &&
                        (pp_data.u.caps[main_item_list.get(i + 1).capsIndex].ReportID == pp_data.u.caps[caps_idx].ReportID) &&
                        (pp_data.u.caps[main_item_list.get(i + 1).capsIndex].BitField == pp_data.u.caps[caps_idx].BitField)
                ) {
                    if (main_item_list.get(i + 1).firstBit != curr_item_list.firstBit) {
                        // In case of IsMultipleItemsForArray for multiple dedicated usages for a multi-button array, the report count should be incremented

                        // Skip global items until any of them changes, than use ReportCount item to write the count of identical report fields
                        report_count++;
                    }
                } else {

                    if ((pp_data.u.caps[caps_idx].u2.button.LogicalMin == 0) &&
                            (pp_data.u.caps[caps_idx].u2.button.LogicalMax == 0)) {
                        // While a HID report descriptor must always contain LogicalMinimum and LogicalMaximum,
                        // the preparsed data contain both fields set to zero, for the case of simple buttons
                        // Write "Logical Minimum" set to 0 and "Logical Maximum" set to 1
                        rpt_desc.write_short_item(global_logical_minimum, 0);
                        rpt_desc.write_short_item(global_logical_maximum, 1);
                    } else {
                        // Write logical range from "Logical Minimum" to "Logical Maximum"
                        rpt_desc.write_short_item(global_logical_minimum, pp_data.u.caps[caps_idx].u2.button.LogicalMin);
                        rpt_desc.write_short_item(global_logical_maximum, pp_data.u.caps[caps_idx].u2.button.LogicalMax);
                    }

                    // Write "Report Size"
                    rpt_desc.write_short_item(global_report_size, pp_data.u.caps[caps_idx].ReportSize);

                    // Write "Report Count"
                    if ((pp_data.u.caps[caps_idx].flags & IsRange) == 0) {
                        // Variable bit field with one bit per button
                        // In case of multiple usages with the same items, only "Usage" is written per cap, and "Report Count" is incremented
                        rpt_desc.write_short_item(global_report_count, pp_data.u.caps[caps_idx].ReportCount + report_count);
                    } else {
                        // Button array of "Report Size" x "Report Count
                        rpt_desc.write_short_item(global_report_count, pp_data.u.caps[caps_idx].ReportCount);
                    }


                    // Buttons have only 1 bit and therefore no physical limits/units . Set to undefined state
                    if (last_physical_min != 0) {
                        // Write "Physical Minimum", but only if changed
                        last_physical_min = 0;
                        rpt_desc.write_short_item(global_physical_minimum, last_physical_min);
                    }
                    if (last_physical_max != 0) {
                        // Write "Physical Maximum", but only if changed
                        last_physical_max = 0;
                        rpt_desc.write_short_item(global_physical_maximum, last_physical_max);
                    }
                    if (last_unit_exponent != 0) {
                        // Write "Unit Exponent", but only if changed
                        last_unit_exponent = 0;
                        rpt_desc.write_short_item(global_unit_exponent, last_unit_exponent);
                    }
                    if (last_unit != 0) {
                        // Write "Unit",but only if changed
                        last_unit = 0;
                        rpt_desc.write_short_item(global_unit, last_unit);
                    }

                    // Write "Input" main item
                    if (rt_idx == HidP_Input.ordinal()) {
                        rpt_desc.write_short_item(main_input, pp_data.u.caps[caps_idx].BitField);
                    }
                    // Write "Output" main item
                    else if (rt_idx == HidP_Output.ordinal()) {
                        rpt_desc.write_short_item(main_output, pp_data.u.caps[caps_idx].BitField);
                    }
                    // Write "Feature" main item
                    else if (rt_idx == HidP_Feature.ordinal()) {
                        rpt_desc.write_short_item(main_feature, pp_data.u.caps[caps_idx].BitField);
                    }
                    report_count = 0;
                }
            } else {

                if (last_report_id != pp_data.u.caps[caps_idx].ReportID) {
                    // Write "Report ID" if changed
                    rpt_desc.write_short_item(global_report_id, pp_data.u.caps[caps_idx].ReportID);
                    last_report_id = (byte) pp_data.u.caps[caps_idx].ReportID;
                }

                // Write "Usage Page" if changed
                if (pp_data.u.caps[caps_idx].UsagePage != last_usage_page) {
                    rpt_desc.write_short_item(global_usage_page, pp_data.u.caps[caps_idx].UsagePage);
                    last_usage_page = pp_data.u.caps[caps_idx].UsagePage;
                }

                if (inhibit_write_of_usage) {
                    // Inhibit only once after Delimiter - Reset flags
                    inhibit_write_of_usage = false;
                } else {
                    if ((pp_data.u.caps[caps_idx].flags & hid_pp_cap.IsRange) != 0) {
                        // Write usage range from "Usage Minimum" to "Usage Maximum"
                        rpt_desc.write_short_item(local_usage_minimum, pp_data.u.caps[caps_idx].u1.range.UsageMin);
                        rpt_desc.write_short_item(local_usage_maximum, pp_data.u.caps[caps_idx].u1.range.UsageMax);
                    } else {
                        // Write single "Usage"
                        rpt_desc.write_short_item(local_usage, pp_data.u.caps[caps_idx].u1.notRange.Usage);
                    }
                }

                if ((pp_data.u.caps[caps_idx].flags & IsDesignatorRange) != 0) {
                    // Write physical descriptor indices range from "Designator Minimum" to "Designator Maximum"
                    rpt_desc.write_short_item(local_designator_minimum, pp_data.u.caps[caps_idx].u1.range.DesignatorMin);
                    rpt_desc.write_short_item(local_designator_maximum, pp_data.u.caps[caps_idx].u1.range.DesignatorMax);
                } else if (pp_data.u.caps[caps_idx].u1.notRange.DesignatorIndex != 0) {
                    // Designator set 0 is a special descriptor set (of the HID Physical Descriptor),
                    // that specifies the number of additional descriptor sets.
                    // Therefore Designator Index 0 can never be a useful reference for a control and we can inhibit it.
                    // Write single "Designator Index"
                    rpt_desc.write_short_item(local_designator_index, pp_data.u.caps[caps_idx].u1.notRange.DesignatorIndex);
                }

                if ((pp_data.u.caps[caps_idx].flags & hid_pp_cap.IsStringRange) != 0) {
                    // Write range of indices of the USB string descriptor, from "String Minimum" to "String Maximum"
                    rpt_desc.write_short_item(local_string_minimum, pp_data.u.caps[caps_idx].u1.range.StringMin);
                    rpt_desc.write_short_item(local_string_maximum, pp_data.u.caps[caps_idx].u1.range.StringMax);
                } else if (pp_data.u.caps[caps_idx].u1.notRange.StringIndex != 0) {
                    // String Index 0 is a special entry of the USB string descriptor, that contains a list of supported languages,
                    // therefore Designator Index 0 can never be a useful reference for a control and we can inhibit it.
                    // Write single "String Index"
                    rpt_desc.write_short_item(local_string, pp_data.u.caps[caps_idx].u1.notRange.StringIndex);
                }

                if ((pp_data.u.caps[caps_idx].BitField & 0x02) != 0x02) {
                    // In case of a value array overwrite "Report Count"
                    pp_data.u.caps[caps_idx].ReportCount = (short) (pp_data.u.caps[caps_idx].u1.range.DataIndexMax - pp_data.u.caps[caps_idx].u1.range.DataIndexMin + 1);
                }


                // Print only local report items for each cap, if ReportCount > 1
                if ((main_item_list.get(i + 1).mainItemType.ordinal() == rt_idx) &&
                        (main_item_list.get(i + 1).typeOfNode == item_node_cap) &&
                        ((pp_data.u.caps[main_item_list.get(i + 1).capsIndex].flags & IsButtonCap) == 0) &&
                        ((pp_data.u.caps[caps_idx].flags & IsRange) == 0) && // This node in list is no array
                        ((pp_data.u.caps[main_item_list.get(i + 1).capsIndex].flags & IsRange) == 0) && // Next node in list is no array
                        (pp_data.u.caps[main_item_list.get(i + 1).capsIndex].UsagePage == pp_data.u.caps[caps_idx].UsagePage) &&
                        (pp_data.u.caps[main_item_list.get(i + 1).capsIndex].u2.notButton.LogicalMin == pp_data.u.caps[caps_idx].u2.notButton.LogicalMin) &&
                        (pp_data.u.caps[main_item_list.get(i + 1).capsIndex].u2.notButton.LogicalMax == pp_data.u.caps[caps_idx].u2.notButton.LogicalMax) &&
                        (pp_data.u.caps[main_item_list.get(i + 1).capsIndex].u2.notButton.PhysicalMin == pp_data.u.caps[caps_idx].u2.notButton.PhysicalMin) &&
                        (pp_data.u.caps[main_item_list.get(i + 1).capsIndex].u2.notButton.PhysicalMax == pp_data.u.caps[caps_idx].u2.notButton.PhysicalMax) &&
                        (pp_data.u.caps[main_item_list.get(i + 1).capsIndex].UnitsExp == pp_data.u.caps[caps_idx].UnitsExp) &&
                        (pp_data.u.caps[main_item_list.get(i + 1).capsIndex].Units == pp_data.u.caps[caps_idx].Units) &&
                        (pp_data.u.caps[main_item_list.get(i + 1).capsIndex].ReportSize == pp_data.u.caps[caps_idx].ReportSize) &&
                        (pp_data.u.caps[main_item_list.get(i + 1).capsIndex].ReportID == pp_data.u.caps[caps_idx].ReportID) &&
                        (pp_data.u.caps[main_item_list.get(i + 1).capsIndex].BitField == pp_data.u.caps[caps_idx].BitField) &&
                        (pp_data.u.caps[main_item_list.get(i + 1).capsIndex].ReportCount == 1) &&
                        (pp_data.u.caps[caps_idx].ReportCount == 1)
                ) {
                    // Skip global items until any of them changes, than use ReportCount item to write the count of identical report fields
                    report_count++;
                } else {
                    // Value

                    // Write logical range from "Logical Minimum" to "Logical Maximum"
                    rpt_desc.write_short_item(global_logical_minimum, pp_data.u.caps[caps_idx].u2.notButton.LogicalMin);
                    rpt_desc.write_short_item(global_logical_maximum, pp_data.u.caps[caps_idx].u2.notButton.LogicalMax);

                    if ((last_physical_min != pp_data.u.caps[caps_idx].u2.notButton.PhysicalMin) ||
                            (last_physical_max != pp_data.u.caps[caps_idx].u2.notButton.PhysicalMax)) {
                        // Write range from "Physical Minimum" to " Physical Maximum", but only if one of them changed
                        rpt_desc.write_short_item(global_physical_minimum, pp_data.u.caps[caps_idx].u2.notButton.PhysicalMin);
                        last_physical_min = pp_data.u.caps[caps_idx].u2.notButton.PhysicalMin;
                        rpt_desc.write_short_item(global_physical_maximum, pp_data.u.caps[caps_idx].u2.notButton.PhysicalMax);
                        last_physical_max = pp_data.u.caps[caps_idx].u2.notButton.PhysicalMax;
                    }

                    if (last_unit_exponent != pp_data.u.caps[caps_idx].UnitsExp) {
                        // Write "Unit Exponent", but only if changed
                        rpt_desc.write_short_item(global_unit_exponent, pp_data.u.caps[caps_idx].UnitsExp);
                        last_unit_exponent = pp_data.u.caps[caps_idx].UnitsExp;
                    }

                    if (last_unit != pp_data.u.caps[caps_idx].Units) {
                        // Write physical "Unit", but only if changed
                        rpt_desc.write_short_item(global_unit, pp_data.u.caps[caps_idx].Units);
                        last_unit = pp_data.u.caps[caps_idx].Units;
                    }

                    // Write "Report Size"
                    rpt_desc.write_short_item(global_report_size, pp_data.u.caps[caps_idx].ReportSize);

                    // Write "Report Count"
                    rpt_desc.write_short_item(global_report_count, pp_data.u.caps[caps_idx].ReportCount + report_count);

                    if (rt_idx == HidP_Input.ordinal()) {
                        // Write "Input" main item
                        rpt_desc.write_short_item(main_input, pp_data.u.caps[caps_idx].BitField);
                    } else if (rt_idx == HidP_Output.ordinal()) {
                        // Write "Output" main item
                        rpt_desc.write_short_item(main_output, pp_data.u.caps[caps_idx].BitField);
                    } else if (rt_idx == HidP_Feature.ordinal()) {
                        // Write "Feature" main item
                        rpt_desc.write_short_item(main_feature, pp_data.u.caps[caps_idx].BitField);
                    }
                    report_count = 0;
                }
            }
        }

        return rpt_desc.byteIdx;
    }
}
