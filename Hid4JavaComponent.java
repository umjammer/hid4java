/*
 * Copyright (c) 2023 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.awt.joystick.hid4java;

import java.util.function.Function;

import net.java.games.input.AbstractComponent;
import net.java.games.input.WrappedComponent;
import net.java.games.input.usb.HidComponent;
import net.java.games.input.usb.parser.Field;
import net.java.games.input.usb.parser.HidParser.Feature;

import static net.java.games.input.usb.parser.HidParser.Feature.RELATIVE;


/**
 * Hid4JavaComponent.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2023-11-06 nsano initial version <br>
 */
public class Hid4JavaComponent extends AbstractComponent implements HidComponent, WrappedComponent<Field> {

    /** an input report definition */
    private final Field field;

    /** special data picker (e.g. for touch x, y) */
    private Function<byte[], Integer> function;

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
     * @param offset bits (must be excluded first one byte (8 bits) for report id)
     * @param size bit length
     */
    public Hid4JavaComponent(String name, Identifier id, int offset, int size) {
        this(name, id, offset, size, null);
    }

    /**
     * @param offset bits (must be excluded first one byte (8 bits) for report id)
     * @param size bit length
     */
    public Hid4JavaComponent(String name, Identifier id, int offset, int size, Function<byte[], Integer> function) {
        super(name, id);
        this.field = new Field(offset, size);
        this.function = function;
    }

    @Override
    public boolean isRelative() {
        return field != null && Feature.containsIn(RELATIVE, field.getFeature());
    }

    @Override
    public boolean isValueChanged(byte[] data) {
        return getEventValue() != getValue(data);
    }

    @Override
    public float getValue() {
        return getEventValue();
    }

    /** by hid input report */
    private int getValue(byte[] data) {
        int value = field.getValue(data);
        return function != null ? function.apply(data) : value;
    }

    @Override
    public void setValue(byte[] data) {
        setEventValue(field.getValue(data));
    }

    @Override
    public Field getWrappedObject() {
        return field;
    }
}
