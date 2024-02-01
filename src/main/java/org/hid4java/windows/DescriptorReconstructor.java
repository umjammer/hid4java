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
        int writeShortItem(Items item, long data) {
            if ((item.v & 0x03) != 0) {
                // Invalid input data, last to bits are reserved for data size
                return -1;
            }

            if (item == main_collection_end) {
                // Item without data (1Byte prefix only)
                byte oneBytePrefix = (byte) (item.v + 0x00);
                appendByte(oneBytePrefix);
            } else if ((item == global_logical_minimum) ||
                    (item == global_logical_maximum) ||
                    (item == global_physical_minimum) ||
                    (item == global_physical_maximum)) {
                // Item with signed integer data
                if ((data >= -128) && (data <= 127)) {
                    // 1Byte prefix + 1Byte data
                    byte oneBytePrefix = (byte) (item.v + 0x01);
                    char localData = (char) data;
                    appendByte(oneBytePrefix);
                    appendByte((byte) (localData & 0xFF));
                } else if ((data >= -32768) && (data <= 32767)) {
                    // 1Byte prefix + 2Byte data
                    byte oneBytePrefix = (byte) (item.v + 0x02);
                    short localData = (short) data;
                    appendByte(oneBytePrefix);
                    appendByte((byte) (localData & 0xFF));
                    appendByte((byte) (localData >> 8 & 0xFF));
                } else if ((data >= -2147483648L) && (data <= 2147483647)) {
                    // 1Byte prefix + 4Byte data
                    byte oneBytePrefix = (byte) (item.v + 0x03);
                    int localData = (int) data;
                    appendByte(oneBytePrefix);
                    appendByte((byte) (localData & 0xFF));
                    appendByte((byte) (localData >> 8 & 0xFF));
                    appendByte((byte) (localData >> 16 & 0xFF));
                    appendByte((byte) (localData >> 24 & 0xFF));
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
                    appendByte(oneBytePrefix);
                    appendByte((byte) (localData & 0xFF));
                } else if ((data >= 0) && (data <= 0xFFFF)) {
                    // 1Byte prefix + 2Byte data
                    byte oneBytePrefix = (byte) (item.v + 0x02);
                    short localData = (short) data;
                    appendByte(oneBytePrefix);
                    appendByte((byte) (localData & 0xFF));
                    appendByte((byte) (localData >> 8 & 0xFF));
                } else if ((data >= 0) && (data <= 0xFFFFFFFFL)) {
                    // 1Byte prefix + 4Byte data
                    byte oneBytePrefix = (byte) (item.v + 0x03);
                    int localData = (int) data;
                    appendByte(oneBytePrefix);
                    appendByte((byte) (localData & 0xFF));
                    appendByte((byte) (localData >> 8 & 0xFF));
                    appendByte((byte) (localData >> 16 & 0xFF));
                    appendByte((byte) (localData >> 24 & 0xFF));
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
        void appendByte(byte b) {
            if (this.byteIdx < this.bufSize) {
                this.buf[this.byteIdx] = b;
                this.byteIdx++;
            }
        }
    }

    /** Determine first INPUT/OUTPUT/FEATURE main item, where the last bit position is equal or greater than the search bit position */
    private static int searchMainItemListForBitPosition(int searchBit, MainItems mainItemType, byte reportId, MainItemNode[] list, int index) {

        for (int i = index; i < list.length; i++) {
            MainItemNode curr = list[i];
            if ((curr.mainItemType.ordinal() != collection.ordinal()) &&
                (curr.mainItemType.ordinal() != collection_end.ordinal()) &&
                !((curr.lastBit >= searchBit) &&
                        (curr.reportID == reportId) &&
                        (curr.mainItemType == mainItemType))) {
                return i;
            }
        }
        return 0;
    }

    static int hidWinapiDescriptorReconstructPpData(Pointer preparsedData, byte[] buf, int bufSize) {
        hidp_preparsed_data ppData = new hidp_preparsed_data(preparsedData);

        // Check if MagicKey is correct, to ensure that ppData points to a valid preparse data structure
        if (Arrays.compare(ppData.MagicKey, "HidP KDR".getBytes()) != 0) {
            return -1;
        }

        Buffer rptDesc = new Buffer();
        rptDesc.buf = buf;
        rptDesc.bufSize = bufSize;
        rptDesc.byteIdx = 0;

        // Set pointer to the first node of linkCollectionNodes
        Pointer pLinkCollectionNodes = new Pointer(Pointer.nativeValue(ppData.u.getPointer()) + ppData.FirstByteOfLinkCollectionArray);
        hid_pp_link_collection_node[] linkCollectionNodes = new hid_pp_link_collection_node[ppData.NumberLinkCollectionNodes];

        //
        // Create lookup tables for the bit range of each report per collection (position of first bit and last bit in each collection)
        // collBitRange[COLLECTION_INDEX][REPORT_ID][INPUT/OUTPUT/FEATURE]
        //

        // Allocate memory and initialize lookup table
        BitRange[][][] collBitRange;
        collBitRange = new BitRange[ppData.NumberLinkCollectionNodes][][];
        for (int collectionNodeIdx = 0; collectionNodeIdx < ppData.NumberLinkCollectionNodes; collectionNodeIdx++) {
            collBitRange[collectionNodeIdx] = new BitRange[256 * collBitRange[0].length][]; // 256 possible report IDs (incl. 0x00)
            for (int reportIdIdx = 0; reportIdIdx < 256; reportIdIdx++) {
                collBitRange[collectionNodeIdx][reportIdIdx] = new BitRange[NUM_OF_HIDP_REPORT_TYPES * collBitRange[0][0].length];
                for (int /* HIDP_REPORT_TYPE */ rtIdx = 0; rtIdx < NUM_OF_HIDP_REPORT_TYPES; rtIdx++) {
                    collBitRange[collectionNodeIdx][reportIdIdx][rtIdx] = new BitRange();
                    collBitRange[collectionNodeIdx][reportIdIdx][rtIdx].firstBit = -1;
                    collBitRange[collectionNodeIdx][reportIdIdx][rtIdx].lastBit = -1;
                }
            }
        }

        // Fill the lookup table where caps exist
        for (int /* HIDP_REPORT_TYPE */ rtIdx = 0; rtIdx < NUM_OF_HIDP_REPORT_TYPES; rtIdx++) {
            for (int capsIdx = ppData.caps_info[rtIdx].FirstCap; capsIdx < ppData.caps_info[rtIdx].LastCap; capsIdx++) {
                int firstBit, lastBit;
                firstBit = (ppData.u.caps[capsIdx].BytePosition - 1) * 8
                        + ppData.u.caps[capsIdx].BitPosition;
                lastBit = firstBit + ppData.u.caps[capsIdx].ReportSize
                        * ppData.u.caps[capsIdx].ReportCount - 1;
                if (collBitRange[ppData.u.caps[capsIdx].LinkCollection][ppData.u.caps[capsIdx].ReportID][rtIdx].firstBit == -1 ||
                        collBitRange[ppData.u.caps[capsIdx].LinkCollection][ppData.u.caps[capsIdx].ReportID][rtIdx].firstBit > firstBit) {
                    collBitRange[ppData.u.caps[capsIdx].LinkCollection][ppData.u.caps[capsIdx].ReportID][rtIdx].firstBit = firstBit;
                }
                if (collBitRange[ppData.u.caps[capsIdx].LinkCollection][ppData.u.caps[capsIdx].ReportID][rtIdx].lastBit < lastBit) {
                    collBitRange[ppData.u.caps[capsIdx].LinkCollection][ppData.u.caps[capsIdx].ReportID][rtIdx].lastBit = lastBit;
                }
            }
        }

        //
        // -Determine hierarchy levels of each collection and store it in:
        //  collLevels[COLLECTION_INDEX]
        // -Determine number of direct children of each collection and store it in:
        //  collNumberOfDirectChildren[COLLECTION_INDEX]
        //
        int maxCollLevel = 0;
        int[] collLevels = new int[ppData.NumberLinkCollectionNodes];
        int[] collNumberOfDirectChildren = new int[ppData.NumberLinkCollectionNodes];
        for (int collectionNodeIdx = 0; collectionNodeIdx < ppData.NumberLinkCollectionNodes; collectionNodeIdx++) {
            collLevels[collectionNodeIdx] = -1;
            collNumberOfDirectChildren[collectionNodeIdx] = 0;
        }

        {
            int actualCollLevel = 0;
            int collectionNodeIdx = 0;
            while (actualCollLevel >= 0) {
                collLevels[collectionNodeIdx] = actualCollLevel;
                linkCollectionNodes[collectionNodeIdx] = new hid_pp_link_collection_node(pLinkCollectionNodes);
                pLinkCollectionNodes = new Pointer(Pointer.nativeValue(pLinkCollectionNodes) + linkCollectionNodes[collectionNodeIdx].size());
                if ((linkCollectionNodes[collectionNodeIdx].NumberOfChildren > 0) &&
                        (collLevels[linkCollectionNodes[collectionNodeIdx].FirstChild] == -1)) {
                    actualCollLevel++;
                    collLevels[collectionNodeIdx] = actualCollLevel;
                    if (maxCollLevel < actualCollLevel) {
                        maxCollLevel = actualCollLevel;
                    }
                    collNumberOfDirectChildren[collectionNodeIdx]++;
                    collectionNodeIdx = linkCollectionNodes[collectionNodeIdx].FirstChild;
                } else if (linkCollectionNodes[collectionNodeIdx].NextSibling != 0) {
                    collNumberOfDirectChildren[linkCollectionNodes[collectionNodeIdx].Parent]++;
                    collectionNodeIdx = linkCollectionNodes[collectionNodeIdx].NextSibling;
                } else {
                    actualCollLevel--;
                    if (actualCollLevel >= 0) {
                        collectionNodeIdx = linkCollectionNodes[collectionNodeIdx].Parent;
                    }
                }
            }
        }

        //
        // Propagate the bit range of each report from the child collections to their parent
        // and store the merged result for the parent
        //
        for (int actualCollLevel = maxCollLevel - 1; actualCollLevel >= 0; actualCollLevel--) {
            for (int collectionNodeIdx = 0; collectionNodeIdx < ppData.NumberLinkCollectionNodes; collectionNodeIdx++) {
                if (collLevels[collectionNodeIdx] == actualCollLevel) {
                    int childIdx = linkCollectionNodes[collectionNodeIdx].FirstChild;
                    while (childIdx != 0) {
                        for (int reportIdIdx = 0; reportIdIdx < 256; reportIdIdx++) {
                            for (int /* HIDP_REPORT_TYPE */ rtIdx = 0; rtIdx < NUM_OF_HIDP_REPORT_TYPES; rtIdx++) {
                                // Merge bit range from children
                                if ((collBitRange[childIdx][reportIdIdx][rtIdx].firstBit != -1) &&
                                        (collBitRange[collectionNodeIdx][reportIdIdx][rtIdx].firstBit > collBitRange[childIdx][reportIdIdx][rtIdx].firstBit)) {
                                    collBitRange[collectionNodeIdx][reportIdIdx][rtIdx].firstBit = collBitRange[childIdx][reportIdIdx][rtIdx].firstBit;
                                }
                                if (collBitRange[collectionNodeIdx][reportIdIdx][rtIdx].lastBit < collBitRange[childIdx][reportIdIdx][rtIdx].lastBit) {
                                    collBitRange[collectionNodeIdx][reportIdIdx][rtIdx].lastBit = collBitRange[childIdx][reportIdIdx][rtIdx].lastBit;
                                }
                                childIdx = linkCollectionNodes[childIdx].NextSibling;
                            }
                        }
                    }
                }
            }
        }

        //
        // Determine child collection order of the whole hierarchy, based on previously determined bit ranges
        // and store it this index collChildOrder[COLLECTION_INDEX][DIRECT_CHILD_INDEX]
        //
        int[][] collChildOrder;
        collChildOrder = new int[ppData.NumberLinkCollectionNodes][];
        {
            boolean[] collParsedFlag = new boolean[ppData.NumberLinkCollectionNodes];
            Arrays.fill(collParsedFlag, false);
            int actualCollLevel = 0;
            int collectionNodeIdx = 0;
            while (actualCollLevel >= 0) {
                if ((collNumberOfDirectChildren[collectionNodeIdx] != 0) &&
                        (!collParsedFlag[linkCollectionNodes[collectionNodeIdx].FirstChild])) {
                    collParsedFlag[linkCollectionNodes[collectionNodeIdx].FirstChild] = true;
                    collChildOrder[collectionNodeIdx] = new int[collNumberOfDirectChildren[collectionNodeIdx]];

                    {
                        // Create list of child collection indices
                        // sorted reverse to the order returned to HidP_GetLinkCollectionNodeschild
                        // which seems to match the original order, as long as no bit position needs to be considered
                        int childIdx = linkCollectionNodes[collectionNodeIdx].FirstChild;
                        int childCount = collNumberOfDirectChildren[collectionNodeIdx] - 1;
                        collChildOrder[collectionNodeIdx][childCount] = childIdx;
                        while (linkCollectionNodes[childIdx].NextSibling != 0) {
                            childCount--;
                            childIdx = linkCollectionNodes[childIdx].NextSibling;
                            collChildOrder[collectionNodeIdx][childCount] = childIdx;
                        }
                    }

                    if (collNumberOfDirectChildren[collectionNodeIdx] > 1) {
                        // Sort child collections indices by bit positions
                        for (int /* HIDP_REPORT_TYPE */ rtIdx = 0; rtIdx < NUM_OF_HIDP_REPORT_TYPES; rtIdx++) {
                            for (int reportIdIdx = 0; reportIdIdx < 256; reportIdIdx++) {
                                for (int childIdx = 1; childIdx < collNumberOfDirectChildren[collectionNodeIdx]; childIdx++) {
                                    // since the collBitRange array is not sorted, we need to reference the collection index in
                                    // our sorted collChildOrder array, and look up the corresponding bit ranges for comparing values to sort
                                    int prevCollIdx = collChildOrder[collectionNodeIdx][childIdx - 1];
                                    int curCollIdx = collChildOrder[collectionNodeIdx][childIdx];
                                    if ((collBitRange[prevCollIdx][reportIdIdx][rtIdx].firstBit != -1) &&
                                            (collBitRange[curCollIdx][reportIdIdx][rtIdx].firstBit != -1) &&
                                            (collBitRange[prevCollIdx][reportIdIdx][rtIdx].firstBit > collBitRange[curCollIdx][reportIdIdx][rtIdx].firstBit)) {
                                        // Swap position indices of the two compared child collections
                                        int idx_latch = collChildOrder[collectionNodeIdx][childIdx - 1];
                                        collChildOrder[collectionNodeIdx][childIdx - 1] = collChildOrder[collectionNodeIdx][childIdx];
                                        collChildOrder[collectionNodeIdx][childIdx] = idx_latch;
                                    }
                                }
                            }
                        }
                    }
                    actualCollLevel++;
                    collectionNodeIdx = linkCollectionNodes[collectionNodeIdx].FirstChild;
                } else if (linkCollectionNodes[collectionNodeIdx].NextSibling != 0) {
                    collectionNodeIdx = linkCollectionNodes[collectionNodeIdx].NextSibling;
                } else {
                    actualCollLevel--;
                    if (actualCollLevel >= 0) {
                        collectionNodeIdx = linkCollectionNodes[collectionNodeIdx].Parent;
                    }
                }
            }
        }

        //
        // Create sorted mainItemNodes containing all the Collection and CollectionEnd main items
        //
        List<MainItemNode> mainItemNodes = new ArrayList<>(); // List root
        int pMainItemList = 0;
        // Lookup table to find the Collection items in the list by index
        MainItemNode[] collBeginLookup = new MainItemNode[ppData.NumberLinkCollectionNodes];
        MainItemNode[] collEndLookup = new MainItemNode[ppData.NumberLinkCollectionNodes];
        {
            int[] collLastWrittenChild = new int[ppData.NumberLinkCollectionNodes];
            Arrays.fill(collLastWrittenChild, -1);

            int actualCollLevel = 0;
            int collectionNodeIdx = 0;
            int firstDelimiterNode = -1;
            int delimiterCloseNode = -1;
            collBeginLookup[0] = new MainItemNode(0, 0, item_node_collection, 0, collectionNodeIdx, collection, (byte) 0);
            mainItemNodes.add(collBeginLookup[0]);
            while (actualCollLevel >= 0) {
                if ((collNumberOfDirectChildren[collectionNodeIdx] != 0) &&
                        (collLastWrittenChild[collectionNodeIdx] == -1)) {
                    // Collection has child collections, but none is written to the list yet

                    collLastWrittenChild[collectionNodeIdx] = collChildOrder[collectionNodeIdx][0];
                    collectionNodeIdx = collChildOrder[collectionNodeIdx][0];

                    // In a HID Report Descriptor, the first usage declared is the most preferred usage for the control.
                    // While the order in the WIN32 capability strutures is the opposite:
                    // Here the preferred usage is the last aliased usage in the sequence.

                    if ((linkCollectionNodes[collectionNodeIdx].bits & IsAlias) != 0 && (firstDelimiterNode == -1)) {
                        // Aliased Collection (First node in linkCollectionNodes . Last entry in report descriptor output)
                        firstDelimiterNode = mainItemNodes.size() - 1;
                        collBeginLookup[collectionNodeIdx] = new MainItemNode(0, 0, item_node_collection, 0, collectionNodeIdx, delimiter_usage, (byte) 0);
                        mainItemNodes.add(collBeginLookup[collectionNodeIdx]);
                        collBeginLookup[collectionNodeIdx] = new MainItemNode(0, 0, item_node_collection, 0, collectionNodeIdx, delimiter_close, (byte) 0);
                        mainItemNodes.add(collBeginLookup[collectionNodeIdx]);
                        delimiterCloseNode = mainItemNodes.size() - 1;
                    } else {
                        // Normal not aliased collection
                        collBeginLookup[collectionNodeIdx] = new MainItemNode(0, 0, item_node_collection, 0, collectionNodeIdx, collection, (byte) 0);
                        mainItemNodes.add(collBeginLookup[collectionNodeIdx]);
                        actualCollLevel++;
                    }

                } else if ((collNumberOfDirectChildren[collectionNodeIdx] > 1) &&
                        (collLastWrittenChild[collectionNodeIdx] != collChildOrder[collectionNodeIdx][collNumberOfDirectChildren[collectionNodeIdx] - 1])) {
                    // Collection has child collections, and this is not the first child

                    int nextChild = 1;
                    while (collLastWrittenChild[collectionNodeIdx] != collChildOrder[collectionNodeIdx][nextChild - 1]) {
                        nextChild++;
                    }
                    collLastWrittenChild[collectionNodeIdx] = collChildOrder[collectionNodeIdx][nextChild];
                    collectionNodeIdx = collChildOrder[collectionNodeIdx][nextChild];

                    if ((linkCollectionNodes[collectionNodeIdx].bits & IsAlias) != 0 && (firstDelimiterNode == -1)) {
                        // Aliased Collection (First node in linkCollectionNodes . Last entry in report descriptor output)
                        firstDelimiterNode = mainItemNodes.size() - 1;
                        collBeginLookup[collectionNodeIdx] = new MainItemNode(0, 0, item_node_collection, 0, collectionNodeIdx, delimiter_usage, (byte) 0);
                        mainItemNodes.add(collBeginLookup[collectionNodeIdx]);
                        collBeginLookup[collectionNodeIdx] = new MainItemNode(0, 0, item_node_collection, 0, collectionNodeIdx, delimiter_close, (byte) 0);
                        mainItemNodes.add(collBeginLookup[collectionNodeIdx]);
                        delimiterCloseNode = mainItemNodes.size() - 1;
                    } else if ((linkCollectionNodes[collectionNodeIdx].bits & IsAlias) != 0 && (firstDelimiterNode != -1)) {
                        // Insert item after the main item node referenced by list
                        MainItemNode newNode = new MainItemNode(0, 0, item_node_collection, 0, collectionNodeIdx, delimiter_usage, (byte) 0);
                        mainItemNodes.add(firstDelimiterNode - pMainItemList, newNode);
                        collBeginLookup[collectionNodeIdx] = newNode;
                    } else if ((linkCollectionNodes[collectionNodeIdx].bits & IsAlias) == 0 && (firstDelimiterNode != -1)) {
                        // Insert item after the main item node referenced by list
                        MainItemNode newNode1 = new MainItemNode(0, 0, item_node_collection, 0, collectionNodeIdx, delimiter_usage, (byte) 0);
                        mainItemNodes.add(firstDelimiterNode - pMainItemList, newNode1);
                        collBeginLookup[collectionNodeIdx] = newNode1;
                        // Insert item after the main item node referenced by list
                        MainItemNode newNode = new MainItemNode(0, 0, item_node_collection, 0, collectionNodeIdx, delimiter_open, (byte) 0);
                        mainItemNodes.add(firstDelimiterNode - pMainItemList, newNode);
                        collBeginLookup[collectionNodeIdx] = newNode;
                        firstDelimiterNode = -1;
                        pMainItemList = delimiterCloseNode;
                        delimiterCloseNode = -1; // Last entry of alias has .IsAlias == false
                    }
                    if ((linkCollectionNodes[collectionNodeIdx].bits & IsAlias) == 0) {
                        collBeginLookup[collectionNodeIdx] = new MainItemNode(0, 0, item_node_collection, 0, collectionNodeIdx, collection, (byte) 0);
                        mainItemNodes.add(collBeginLookup[collectionNodeIdx]);
                        actualCollLevel++;
                    }
                } else {
                    actualCollLevel--;
                    collEndLookup[collectionNodeIdx] = new MainItemNode(0, 0, item_node_collection, 0, collectionNodeIdx, collection_end, (byte) 0);
                    mainItemNodes.add(collEndLookup[collectionNodeIdx]);
                    collectionNodeIdx = linkCollectionNodes[collectionNodeIdx].Parent;
                }
            }
        }

        //
        // Inserted Input/Output/Feature main items into the mainItemNodes
        // in order of reconstructed bit positions
        //
        for (int /* HIDP_REPORT_TYPE */ rtIdx = 0; rtIdx < NUM_OF_HIDP_REPORT_TYPES; rtIdx++) {
            // Add all value caps to node list
            int firstDelimiterNode = -1;
            int delimiterCloseNode = -1;
            for (int capsIdx = ppData.caps_info[rtIdx].FirstCap; capsIdx < ppData.caps_info[rtIdx].LastCap; capsIdx++) {
                int collBegin = ppData.u.caps[capsIdx].LinkCollection;
                int firstBit, lastBit;
                firstBit = (ppData.u.caps[capsIdx].BytePosition - 1) * 8 +
                        ppData.u.caps[capsIdx].BitPosition;
                lastBit = firstBit + ppData.u.caps[capsIdx].ReportSize *
                        ppData.u.caps[capsIdx].ReportCount - 1;

                for (int childIdx = 0; childIdx < collNumberOfDirectChildren[ppData.u.caps[capsIdx].LinkCollection]; childIdx++) {
                    // Determine in which section before/between/after child collection the item should be inserted
                    if (firstBit < collBitRange[collChildOrder[ppData.u.caps[capsIdx].LinkCollection][childIdx]][ppData.u.caps[capsIdx].ReportID][rtIdx].firstBit) {
                        // Note, that the default value for undefined collBitRange is -1, which can't be greater than the bit position
                        break;
                    }
                    collBegin = collChildOrder[ppData.u.caps[capsIdx].LinkCollection][childIdx];
                }
                int listNodeP = searchMainItemListForBitPosition(firstBit, MainItems.values()[rtIdx], (byte) ppData.u.caps[capsIdx].ReportID, collEndLookup, collBegin);
                List<MainItemNode> listNode = List.of(collEndLookup);

                // In a HID Report Descriptor, the first usage declared is the most preferred usage for the control.
                // While the order in the WIN32 capability strutures is the opposite:
                // Here the preferred usage is the last aliased usage in the sequence.

                if ((ppData.u.caps[capsIdx].flags & hid_pp_cap.IsAlias) != 0 && (firstDelimiterNode == -1)) {
                    // Aliased Usage (First node in ppData.u.caps . Last entry in report descriptor output)
                    firstDelimiterNode = listNode.size() - 1;
                    // Insert item after the main item node referenced by list
                    MainItemNode newNode1 = new MainItemNode(firstBit, lastBit, item_node_cap, capsIdx, ppData.u.caps[capsIdx].LinkCollection, delimiter_usage, (byte) ppData.u.caps[capsIdx].ReportID);
                    listNode.add(listNodeP, newNode1);
                    // Insert item after the main item node referenced by list
                    MainItemNode newNode = new MainItemNode(firstBit, lastBit, item_node_cap, capsIdx, ppData.u.caps[capsIdx].LinkCollection, delimiter_close, (byte) ppData.u.caps[capsIdx].ReportID);
                    listNode.add(listNodeP, newNode);
                    delimiterCloseNode = listNode.size() - 1;
                } else if ((ppData.u.caps[capsIdx].flags & hid_pp_cap.IsAlias) != 0 && (firstDelimiterNode != -1)) {
                    // Insert item after the main item node referenced by list
                    MainItemNode newNode = new MainItemNode(firstBit, lastBit, item_node_cap, capsIdx, ppData.u.caps[capsIdx].LinkCollection, delimiter_usage, (byte) ppData.u.caps[capsIdx].ReportID);
                    listNode.add(listNodeP, newNode);
                } else if ((ppData.u.caps[capsIdx].flags & hid_pp_cap.IsAlias) == 0 && (firstDelimiterNode != -1)) {
                    // Aliased Collection (Last node in ppData.u.caps . First entry in report descriptor output)
                    // Insert item after the main item node referenced by list
                    MainItemNode newNode1 = new MainItemNode(firstBit, lastBit, item_node_cap, capsIdx, ppData.u.caps[capsIdx].LinkCollection, delimiter_usage, (byte) ppData.u.caps[capsIdx].ReportID);
                    listNode.add(listNodeP, newNode1);
                    // Insert item after the main item node referenced by list
                    MainItemNode newNode = new MainItemNode(firstBit, lastBit, item_node_cap, capsIdx, ppData.u.caps[capsIdx].LinkCollection, delimiter_open, (byte) ppData.u.caps[capsIdx].ReportID);
                    listNode.add(listNodeP, newNode);
                    firstDelimiterNode = -1;
                    listNodeP = delimiterCloseNode;
                    delimiterCloseNode = -1; // Last entry of alias has .IsAlias == false
                }
                if ((ppData.u.caps[capsIdx].flags & hid_pp_cap.IsAlias) == 0) {
                    // Insert item after the main item node referenced by list
                    MainItemNode newNode = new MainItemNode(firstBit, lastBit, item_node_cap, capsIdx, (int) ppData.u.caps[capsIdx].LinkCollection, MainItems.values()[rtIdx], (byte) ppData.u.caps[capsIdx].ReportID);
                    listNode.add(listNodeP, newNode);
                }
            }
        }

        //
        // Add const main items for padding to mainItemNodes
        // -To fill all bit gaps
        // -At each report end for 8bit padding
        //  Note that information about the padding at the report end,
        //  is not stored in the preparsed data, but in practice all
        //  report descriptors seem to have it, as assumed here.
        //
        {
            int[][] lastBitPosition = new int[NUM_OF_HIDP_REPORT_TYPES][256];
            MainItemNode[][] lastReportItemLookup = new MainItemNode[NUM_OF_HIDP_REPORT_TYPES][256];
            for (int /* HIDP_REPORT_TYPE */ rtIdx = 0; rtIdx < NUM_OF_HIDP_REPORT_TYPES; rtIdx++) {
                for (int reportIdIdx = 0; reportIdIdx < 256; reportIdIdx++) {
                    lastBitPosition[rtIdx][reportIdIdx] = -1;
                    lastReportItemLookup[rtIdx][reportIdIdx] = null;
                }
            }

            for (MainItemNode curr : mainItemNodes) {
                if ((curr.mainItemType.ordinal() >= input.ordinal()) &&
                        (curr.mainItemType.ordinal() <= feature.ordinal())) {
                    // INPUT, OUTPUT or FEATURE
                    if (curr.firstBit != -1) {
                        if ((lastBitPosition[curr.mainItemType.ordinal()][curr.reportID] + 1 != curr.firstBit) &&
                                (lastReportItemLookup[curr.mainItemType.ordinal()][curr.reportID] != null) &&
                                (lastReportItemLookup[curr.mainItemType.ordinal()][curr.reportID].firstBit != curr.firstBit) // Happens in case of IsMultipleItemsForArray for multiple dedicated usages for a multi-button array
                        ) {
                            int index = searchMainItemListForBitPosition(lastBitPosition[curr.mainItemType.ordinal()][curr.reportID], curr.mainItemType, curr.reportID, lastReportItemLookup[curr.mainItemType.ordinal()], curr.reportID);
                            List<MainItemNode> list_node = List.of(lastReportItemLookup[curr.mainItemType.ordinal()]);
                            // Insert item after the main item node referenced by list
                            MainItemNode newNode = new MainItemNode(lastBitPosition[curr.mainItemType.ordinal()][curr.reportID] + 1, curr.firstBit - 1, item_node_padding, -1, 0, curr.mainItemType, curr.reportID);
                            list_node.add(index, newNode);
                        }
                        lastBitPosition[curr.mainItemType.ordinal()][curr.reportID] = curr.lastBit;
                        lastReportItemLookup[curr.mainItemType.ordinal()][curr.reportID] = curr;
                    }
                }
            }
            // Add 8 bit padding at each report end
            for (int /* HIDP_REPORT_TYPE */ rtIdx = 0; rtIdx < NUM_OF_HIDP_REPORT_TYPES; rtIdx++) {
                for (int reportIdIdx = 0; reportIdIdx < 256; reportIdIdx++) {
                    if (lastBitPosition[rtIdx][reportIdIdx] != -1) {
                        int padding = 8 - ((lastBitPosition[rtIdx][reportIdIdx] + 1) % 8);
                        if (padding < 8) {
                            // Insert padding item after item referenced in lastReportItemLookup
                            List<MainItemNode> listNode = List.of(lastReportItemLookup[rtIdx]);
                            // Insert item after the main item node referenced by list
                            MainItemNode newNode = new MainItemNode(lastBitPosition[rtIdx][reportIdIdx] + 1, lastBitPosition[rtIdx][reportIdIdx] + padding, item_node_padding, -1, 0, MainItems.values()[rtIdx], (byte) reportIdIdx);
                            listNode.add(reportIdIdx, newNode);
                        }
                    }
                }
            }
        }

        //
        // Encode the report descriptor output
        //
        byte lastReportId = 0;
        short lastUsagePage = 0;
        long lastPhysicalMin = 0;// If both, Physical Minimum and Physical Maximum are 0, the logical limits should be taken as physical limits according USB HID spec 1.11 chapter 6.2.2.7
        long lastPhysicalMax = 0;
        long lastUnitExponent = 0; // If Unit Exponent is Undefined it should be considered as 0 according USB HID spec 1.11 chapter 6.2.2.7
        long lastUnit = 0; // If the first nibble is 7, or second nibble of Unit is 0, the unit is None according USB HID spec 1.11 chapter 6.2.2.7
        boolean inhibitWriteOfUsage = false; // Needed in case of delimited usage print, before the normal collection or cap
        int reportCount = 0;
        for (int i = 0; i < mainItemNodes.size(); i++) {
            MainItemNode currItemList = mainItemNodes.get(i);
            int rtIdx = currItemList.mainItemType.ordinal();
            int capsIdx = currItemList.capsIndex;
            if (currItemList.mainItemType == collection) {
                if (lastUsagePage != linkCollectionNodes[currItemList.collectionIndex].LinkUsagePage) {
                    // Write "Usage Page" at the beginning of a collection - except it refers the same table as wrote last
                    rptDesc.writeShortItem(global_usage_page, linkCollectionNodes[currItemList.collectionIndex].LinkUsagePage);
                    lastUsagePage = linkCollectionNodes[currItemList.collectionIndex].LinkUsagePage;
                }
                if (inhibitWriteOfUsage) {
                    // Inhibit only once after DELIMITER statement
                    inhibitWriteOfUsage = false;
                } else {
                    // Write "Usage" of collection
                    rptDesc.writeShortItem(local_usage, linkCollectionNodes[currItemList.collectionIndex].LinkUsage);
                }
                // Write begin of "Collection"
                rptDesc.writeShortItem(main_collection, linkCollectionNodes[currItemList.collectionIndex].bits & CollectionType);
            } else if (currItemList.mainItemType == collection_end) {
                // Write "End Collection"
                rptDesc.writeShortItem(main_collection_end, 0);
            } else if (currItemList.mainItemType == delimiter_open) {
                if (currItemList.collectionIndex != -1) {
                    // Write "Usage Page" inside a collection delimiter section
                    if (lastUsagePage != linkCollectionNodes[currItemList.collectionIndex].LinkUsagePage) {
                        rptDesc.writeShortItem(global_usage_page, linkCollectionNodes[currItemList.collectionIndex].LinkUsagePage);
                        lastUsagePage = linkCollectionNodes[currItemList.collectionIndex].LinkUsagePage;
                    }
                } else if (currItemList.capsIndex != 0) {
                    // Write "Usage Page" inside a main item delimiter section
                    if (ppData.u.caps[capsIdx].UsagePage != lastUsagePage) {
                        rptDesc.writeShortItem(global_usage_page, ppData.u.caps[capsIdx].UsagePage);
                        lastUsagePage = ppData.u.caps[capsIdx].UsagePage;
                    }
                }
                // Write "Delimiter Open"
                rptDesc.writeShortItem(local_delimiter, 1); // 1 = create set of aliased usages
            } else if (currItemList.mainItemType == delimiter_usage) {
                if (currItemList.collectionIndex != -1) {
                    // Write aliased collection "Usage"
                    rptDesc.writeShortItem(local_usage, linkCollectionNodes[currItemList.collectionIndex].LinkUsage);
                }
                if (currItemList.capsIndex != 0) {
                    // Write aliased main item range from "Usage Minimum" to "Usage Maximum"
                    if ((ppData.u.caps[capsIdx].flags & IsRange) != 0) {
                        rptDesc.writeShortItem(local_usage_minimum, ppData.u.caps[capsIdx].u1.range.UsageMin);
                        rptDesc.writeShortItem(local_usage_maximum, ppData.u.caps[capsIdx].u1.range.UsageMax);
                    } else {
                        // Write single aliased main item "Usage"
                        rptDesc.writeShortItem(local_usage, ppData.u.caps[capsIdx].u1.notRange.Usage);
                    }
                }
            } else if (currItemList.mainItemType == delimiter_close) {
                // Write "Delimiter Close"
                rptDesc.writeShortItem(local_delimiter, 0); // 0 = close set of aliased usages
                // Inhibit next usage write
                inhibitWriteOfUsage = true;
            } else if (currItemList.typeOfNode == item_node_padding) {
                // Padding
                // The preparsed data doesn't contain any information about padding. Therefore all undefined gaps
                // in the reports are filled with the same style of constant padding.

                // Write "Report Size" with number of padding bits
                rptDesc.writeShortItem(global_report_size, (currItemList.lastBit - currItemList.firstBit + 1));

                // Write "Report Count" for padding always as 1
                rptDesc.writeShortItem(global_report_count, 1);

                if (rtIdx == HidP_Input.ordinal()) {
                    // Write "Input" main item - We know it's Constant - We can only guess the other bits, but they don't matter in case of const
                    rptDesc.writeShortItem(main_input, 0x03); // Const / Abs
                } else if (rtIdx == HidP_Output.ordinal()) {
                    // Write "Output" main item - We know it's Constant - We can only guess the other bits, but they don't matter in case of const
                    rptDesc.writeShortItem(main_output, 0x03); // Const / Abs
                } else if (rtIdx == HidP_Feature.ordinal()) {
                    // Write "Feature" main item - We know it's Constant - We can only guess the other bits, but they don't matter in case of const
                    rptDesc.writeShortItem(main_feature, 0x03); // Const / Abs
                }
                reportCount = 0;
            } else if ((ppData.u.caps[capsIdx].flags & IsButtonCap) != 0) {
                // Button
                // (The preparsed data contain different data for 1 bit Button caps, then for parametric Value caps)

                if (lastReportId != ppData.u.caps[capsIdx].ReportID) {
                    // Write "Report ID" if changed
                    rptDesc.writeShortItem(global_report_id, ppData.u.caps[capsIdx].ReportID);
                    lastReportId = (byte) ppData.u.caps[capsIdx].ReportID;
                }

                // Write "Usage Page" when changed
                if (ppData.u.caps[capsIdx].UsagePage != lastUsagePage) {
                    rptDesc.writeShortItem(global_usage_page, ppData.u.caps[capsIdx].UsagePage);
                    lastUsagePage = ppData.u.caps[capsIdx].UsagePage;
                }

                // Write only local report items for each cap, if ReportCount > 1
                if ((ppData.u.caps[capsIdx].flags & IsRange) != 0) {
                    reportCount += (ppData.u.caps[capsIdx].u1.range.DataIndexMax - ppData.u.caps[capsIdx].u1.range.DataIndexMin);
                }

                if (inhibitWriteOfUsage) {
                    // Inhibit only once after Delimiter - Reset flags
                    inhibitWriteOfUsage = false;
                } else {
                    if ((ppData.u.caps[capsIdx].flags & IsRange) != 0) {
                        // Write range from "Usage Minimum" to "Usage Maximum"
                        rptDesc.writeShortItem(local_usage_minimum, ppData.u.caps[capsIdx].u1.range.UsageMin);
                        rptDesc.writeShortItem(local_usage_maximum, ppData.u.caps[capsIdx].u1.range.UsageMax);
                    } else {
                        // Write single "Usage"
                        rptDesc.writeShortItem(local_usage, ppData.u.caps[capsIdx].u1.notRange.Usage);
                    }
                }

                if ((ppData.u.caps[capsIdx].flags & IsDesignatorRange) == 0) {
                    // Write physical descriptor indices range from "Designator Minimum" to "Designator Maximum"
                    rptDesc.writeShortItem(local_designator_minimum, ppData.u.caps[capsIdx].u1.range.DesignatorMin);
                    rptDesc.writeShortItem(local_designator_maximum, ppData.u.caps[capsIdx].u1.range.DesignatorMax);
                } else if (ppData.u.caps[capsIdx].u1.notRange.DesignatorIndex != 0) {
                    // Designator set 0 is a special descriptor set (of the HID Physical Descriptor),
                    // that specifies the number of additional descriptor sets.
                    // Therefore Designator Index 0 can never be a useful reference for a control and we can inhibit it.
                    // Write single "Designator Index"
                    rptDesc.writeShortItem(local_designator_index, ppData.u.caps[capsIdx].u1.notRange.DesignatorIndex);
                }

                if ((ppData.u.caps[capsIdx].flags & IsStringRange) != 0) {
                    // Write range of indices of the USB string descriptor, from "String Minimum" to "String Maximum"
                    rptDesc.writeShortItem(local_string_minimum, ppData.u.caps[capsIdx].u1.range.StringMin);
                    rptDesc.writeShortItem(local_string_maximum, ppData.u.caps[capsIdx].u1.range.StringMax);
                } else if (ppData.u.caps[capsIdx].u1.notRange.StringIndex != 0) {
                    // String Index 0 is a special entry of the USB string descriptor, that contains a list of supported languages,
                    // therefore Designator Index 0 can never be a useful reference for a control and we can inhibit it.
                    // Write single "String Index"
                    rptDesc.writeShortItem(local_string, ppData.u.caps[capsIdx].u1.notRange.StringIndex);
                }

                if ((i < mainItemNodes.size() - 1) &&
                        (mainItemNodes.get(i + 1).mainItemType.ordinal() == rtIdx) &&
                        (mainItemNodes.get(i + 1).typeOfNode == item_node_cap) &&
                        ((ppData.u.caps[mainItemNodes.get(i + 1).capsIndex].flags & IsButtonCap) != 0) &&
                        ((ppData.u.caps[capsIdx].flags & IsRange) == 0) && // This node in list is no array
                        ((ppData.u.caps[mainItemNodes.get(i + 1).capsIndex].flags & IsRange) == 0) && // Next node in list is no array
                        (ppData.u.caps[mainItemNodes.get(i + 1).capsIndex].UsagePage == ppData.u.caps[capsIdx].UsagePage) &&
                        (ppData.u.caps[mainItemNodes.get(i + 1).capsIndex].ReportID == ppData.u.caps[capsIdx].ReportID) &&
                        (ppData.u.caps[mainItemNodes.get(i + 1).capsIndex].BitField == ppData.u.caps[capsIdx].BitField)
                ) {
                    if (mainItemNodes.get(i + 1).firstBit != currItemList.firstBit) {
                        // In case of IsMultipleItemsForArray for multiple dedicated usages for a multi-button array, the report count should be incremented

                        // Skip global items until any of them changes, than use ReportCount item to write the count of identical report fields
                        reportCount++;
                    }
                } else {

                    if ((ppData.u.caps[capsIdx].u2.button.LogicalMin == 0) &&
                            (ppData.u.caps[capsIdx].u2.button.LogicalMax == 0)) {
                        // While a HID report descriptor must always contain LogicalMinimum and LogicalMaximum,
                        // the preparsed data contain both fields set to zero, for the case of simple buttons
                        // Write "Logical Minimum" set to 0 and "Logical Maximum" set to 1
                        rptDesc.writeShortItem(global_logical_minimum, 0);
                        rptDesc.writeShortItem(global_logical_maximum, 1);
                    } else {
                        // Write logical range from "Logical Minimum" to "Logical Maximum"
                        rptDesc.writeShortItem(global_logical_minimum, ppData.u.caps[capsIdx].u2.button.LogicalMin);
                        rptDesc.writeShortItem(global_logical_maximum, ppData.u.caps[capsIdx].u2.button.LogicalMax);
                    }

                    // Write "Report Size"
                    rptDesc.writeShortItem(global_report_size, ppData.u.caps[capsIdx].ReportSize);

                    // Write "Report Count"
                    if ((ppData.u.caps[capsIdx].flags & IsRange) == 0) {
                        // Variable bit field with one bit per button
                        // In case of multiple usages with the same items, only "Usage" is written per cap, and "Report Count" is incremented
                        rptDesc.writeShortItem(global_report_count, ppData.u.caps[capsIdx].ReportCount + reportCount);
                    } else {
                        // Button array of "Report Size" x "Report Count
                        rptDesc.writeShortItem(global_report_count, ppData.u.caps[capsIdx].ReportCount);
                    }


                    // Buttons have only 1 bit and therefore no physical limits/units . Set to undefined state
                    if (lastPhysicalMin != 0) {
                        // Write "Physical Minimum", but only if changed
                        lastPhysicalMin = 0;
                        rptDesc.writeShortItem(global_physical_minimum, lastPhysicalMin);
                    }
                    if (lastPhysicalMax != 0) {
                        // Write "Physical Maximum", but only if changed
                        lastPhysicalMax = 0;
                        rptDesc.writeShortItem(global_physical_maximum, lastPhysicalMax);
                    }
                    if (lastUnitExponent != 0) {
                        // Write "Unit Exponent", but only if changed
                        lastUnitExponent = 0;
                        rptDesc.writeShortItem(global_unit_exponent, lastUnitExponent);
                    }
                    if (lastUnit != 0) {
                        // Write "Unit",but only if changed
                        lastUnit = 0;
                        rptDesc.writeShortItem(global_unit, lastUnit);
                    }

                    // Write "Input" main item
                    if (rtIdx == HidP_Input.ordinal()) {
                        rptDesc.writeShortItem(main_input, ppData.u.caps[capsIdx].BitField);
                    }
                    // Write "Output" main item
                    else if (rtIdx == HidP_Output.ordinal()) {
                        rptDesc.writeShortItem(main_output, ppData.u.caps[capsIdx].BitField);
                    }
                    // Write "Feature" main item
                    else if (rtIdx == HidP_Feature.ordinal()) {
                        rptDesc.writeShortItem(main_feature, ppData.u.caps[capsIdx].BitField);
                    }
                    reportCount = 0;
                }
            } else {

                if (lastReportId != ppData.u.caps[capsIdx].ReportID) {
                    // Write "Report ID" if changed
                    rptDesc.writeShortItem(global_report_id, ppData.u.caps[capsIdx].ReportID);
                    lastReportId = (byte) ppData.u.caps[capsIdx].ReportID;
                }

                // Write "Usage Page" if changed
                if (ppData.u.caps[capsIdx].UsagePage != lastUsagePage) {
                    rptDesc.writeShortItem(global_usage_page, ppData.u.caps[capsIdx].UsagePage);
                    lastUsagePage = ppData.u.caps[capsIdx].UsagePage;
                }

                if (inhibitWriteOfUsage) {
                    // Inhibit only once after Delimiter - Reset flags
                    inhibitWriteOfUsage = false;
                } else {
                    if ((ppData.u.caps[capsIdx].flags & hid_pp_cap.IsRange) != 0) {
                        // Write usage range from "Usage Minimum" to "Usage Maximum"
                        rptDesc.writeShortItem(local_usage_minimum, ppData.u.caps[capsIdx].u1.range.UsageMin);
                        rptDesc.writeShortItem(local_usage_maximum, ppData.u.caps[capsIdx].u1.range.UsageMax);
                    } else {
                        // Write single "Usage"
                        rptDesc.writeShortItem(local_usage, ppData.u.caps[capsIdx].u1.notRange.Usage);
                    }
                }

                if ((ppData.u.caps[capsIdx].flags & IsDesignatorRange) != 0) {
                    // Write physical descriptor indices range from "Designator Minimum" to "Designator Maximum"
                    rptDesc.writeShortItem(local_designator_minimum, ppData.u.caps[capsIdx].u1.range.DesignatorMin);
                    rptDesc.writeShortItem(local_designator_maximum, ppData.u.caps[capsIdx].u1.range.DesignatorMax);
                } else if (ppData.u.caps[capsIdx].u1.notRange.DesignatorIndex != 0) {
                    // Designator set 0 is a special descriptor set (of the HID Physical Descriptor),
                    // that specifies the number of additional descriptor sets.
                    // Therefore Designator Index 0 can never be a useful reference for a control and we can inhibit it.
                    // Write single "Designator Index"
                    rptDesc.writeShortItem(local_designator_index, ppData.u.caps[capsIdx].u1.notRange.DesignatorIndex);
                }

                if ((ppData.u.caps[capsIdx].flags & hid_pp_cap.IsStringRange) != 0) {
                    // Write range of indices of the USB string descriptor, from "String Minimum" to "String Maximum"
                    rptDesc.writeShortItem(local_string_minimum, ppData.u.caps[capsIdx].u1.range.StringMin);
                    rptDesc.writeShortItem(local_string_maximum, ppData.u.caps[capsIdx].u1.range.StringMax);
                } else if (ppData.u.caps[capsIdx].u1.notRange.StringIndex != 0) {
                    // String Index 0 is a special entry of the USB string descriptor, that contains a list of supported languages,
                    // therefore Designator Index 0 can never be a useful reference for a control and we can inhibit it.
                    // Write single "String Index"
                    rptDesc.writeShortItem(local_string, ppData.u.caps[capsIdx].u1.notRange.StringIndex);
                }

                if ((ppData.u.caps[capsIdx].BitField & 0x02) != 0x02) {
                    // In case of a value array overwrite "Report Count"
                    ppData.u.caps[capsIdx].ReportCount = (short) (ppData.u.caps[capsIdx].u1.range.DataIndexMax - ppData.u.caps[capsIdx].u1.range.DataIndexMin + 1);
                }


                // Print only local report items for each cap, if ReportCount > 1
                if ((mainItemNodes.get(i + 1).mainItemType.ordinal() == rtIdx) &&
                        (mainItemNodes.get(i + 1).typeOfNode == item_node_cap) &&
                        ((ppData.u.caps[mainItemNodes.get(i + 1).capsIndex].flags & IsButtonCap) == 0) &&
                        ((ppData.u.caps[capsIdx].flags & IsRange) == 0) && // This node in list is no array
                        ((ppData.u.caps[mainItemNodes.get(i + 1).capsIndex].flags & IsRange) == 0) && // Next node in list is no array
                        (ppData.u.caps[mainItemNodes.get(i + 1).capsIndex].UsagePage == ppData.u.caps[capsIdx].UsagePage) &&
                        (ppData.u.caps[mainItemNodes.get(i + 1).capsIndex].u2.notButton.LogicalMin == ppData.u.caps[capsIdx].u2.notButton.LogicalMin) &&
                        (ppData.u.caps[mainItemNodes.get(i + 1).capsIndex].u2.notButton.LogicalMax == ppData.u.caps[capsIdx].u2.notButton.LogicalMax) &&
                        (ppData.u.caps[mainItemNodes.get(i + 1).capsIndex].u2.notButton.PhysicalMin == ppData.u.caps[capsIdx].u2.notButton.PhysicalMin) &&
                        (ppData.u.caps[mainItemNodes.get(i + 1).capsIndex].u2.notButton.PhysicalMax == ppData.u.caps[capsIdx].u2.notButton.PhysicalMax) &&
                        (ppData.u.caps[mainItemNodes.get(i + 1).capsIndex].UnitsExp == ppData.u.caps[capsIdx].UnitsExp) &&
                        (ppData.u.caps[mainItemNodes.get(i + 1).capsIndex].Units == ppData.u.caps[capsIdx].Units) &&
                        (ppData.u.caps[mainItemNodes.get(i + 1).capsIndex].ReportSize == ppData.u.caps[capsIdx].ReportSize) &&
                        (ppData.u.caps[mainItemNodes.get(i + 1).capsIndex].ReportID == ppData.u.caps[capsIdx].ReportID) &&
                        (ppData.u.caps[mainItemNodes.get(i + 1).capsIndex].BitField == ppData.u.caps[capsIdx].BitField) &&
                        (ppData.u.caps[mainItemNodes.get(i + 1).capsIndex].ReportCount == 1) &&
                        (ppData.u.caps[capsIdx].ReportCount == 1)
                ) {
                    // Skip global items until any of them changes, than use ReportCount item to write the count of identical report fields
                    reportCount++;
                } else {
                    // Value

                    // Write logical range from "Logical Minimum" to "Logical Maximum"
                    rptDesc.writeShortItem(global_logical_minimum, ppData.u.caps[capsIdx].u2.notButton.LogicalMin);
                    rptDesc.writeShortItem(global_logical_maximum, ppData.u.caps[capsIdx].u2.notButton.LogicalMax);

                    if ((lastPhysicalMin != ppData.u.caps[capsIdx].u2.notButton.PhysicalMin) ||
                            (lastPhysicalMax != ppData.u.caps[capsIdx].u2.notButton.PhysicalMax)) {
                        // Write range from "Physical Minimum" to " Physical Maximum", but only if one of them changed
                        rptDesc.writeShortItem(global_physical_minimum, ppData.u.caps[capsIdx].u2.notButton.PhysicalMin);
                        lastPhysicalMin = ppData.u.caps[capsIdx].u2.notButton.PhysicalMin;
                        rptDesc.writeShortItem(global_physical_maximum, ppData.u.caps[capsIdx].u2.notButton.PhysicalMax);
                        lastPhysicalMax = ppData.u.caps[capsIdx].u2.notButton.PhysicalMax;
                    }

                    if (lastUnitExponent != ppData.u.caps[capsIdx].UnitsExp) {
                        // Write "Unit Exponent", but only if changed
                        rptDesc.writeShortItem(global_unit_exponent, ppData.u.caps[capsIdx].UnitsExp);
                        lastUnitExponent = ppData.u.caps[capsIdx].UnitsExp;
                    }

                    if (lastUnit != ppData.u.caps[capsIdx].Units) {
                        // Write physical "Unit", but only if changed
                        rptDesc.writeShortItem(global_unit, ppData.u.caps[capsIdx].Units);
                        lastUnit = ppData.u.caps[capsIdx].Units;
                    }

                    // Write "Report Size"
                    rptDesc.writeShortItem(global_report_size, ppData.u.caps[capsIdx].ReportSize);

                    // Write "Report Count"
                    rptDesc.writeShortItem(global_report_count, ppData.u.caps[capsIdx].ReportCount + reportCount);

                    if (rtIdx == HidP_Input.ordinal()) {
                        // Write "Input" main item
                        rptDesc.writeShortItem(main_input, ppData.u.caps[capsIdx].BitField);
                    } else if (rtIdx == HidP_Output.ordinal()) {
                        // Write "Output" main item
                        rptDesc.writeShortItem(main_output, ppData.u.caps[capsIdx].BitField);
                    } else if (rtIdx == HidP_Feature.ordinal()) {
                        // Write "Feature" main item
                        rptDesc.writeShortItem(main_feature, ppData.u.caps[capsIdx].BitField);
                    }
                    reportCount = 0;
                }
            }
        }

        return rptDesc.byteIdx;
    }
}
