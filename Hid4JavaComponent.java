/*
 * Copyright (c) 2023 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.awt.joystick.hid4java;

import net.java.games.input.AbstractComponent;
import vavi.hid.parser.Field;
import vavi.hid.parser.HidParser.Feature;

import static vavi.hid.parser.HidParser.Feature.RELATIVE;


/**
 * Hid4JavaComponent.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2023-11-06 nsano initial version <br>
 */
public class Hid4JavaComponent extends AbstractComponent {

    private final Field field;

    /**
     * Protected constructor
     *
     * @param name A name for the axis
     * @param field an input report descriptor fragment.
     */
    protected Hid4JavaComponent(String name, Identifier id, Field field) {
        super(name, id);
        this.field = field;
    }

    /**
     * @param offset bits
     * @param size bit length
     */
    public Hid4JavaComponent(String name, Identifier id, int offset, int size) {
        super(name, id);
        this.field = new Field(offset, size);
    }

    @Override
    public boolean isRelative() {
        return field != null && Feature.containsIn(RELATIVE, field.getFeature());
    }

    /** by hid input report */
    public boolean isValueChanged(byte[] data) {
        return getEventValue() != getValue(data);
    }

    /** */
    public float getValue() {
        return getEventValue();
    }

    /** by hid input report */
    private int getValue(byte[] data) {
        return field.getValue(data);
    }

    /** by hid input report */
    public void setValue(byte[] data) {
        setEventValue(field.getValue(data));
    }
}
