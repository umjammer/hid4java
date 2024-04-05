/*
 * Copyright (c) 2023 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.games.input.hid4java.spi;

import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedList;

import net.java.games.input.Component;
import net.java.games.input.Event;
import net.java.games.input.InputEvent;
import net.java.games.input.usb.HidComponent;
import net.java.games.input.usb.HidInputEvent;


/**
 * Hid4JavaInputEvent. Assuming reuse.
 * <p>
 * <h4>system property</h4>
 * <li>"net.java.games.input.InputEvent.fillAll" ... determine to fill all events (true) or events which value is changed (false)</li>
 * </p>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2023-11-07 nsano initial version <br>
 */
public class Hid4JavaInputEvent extends InputEvent implements HidInputEvent {

    /** which value is changed only */
    private final Deque<Hid4JavaComponent> deque = new LinkedList<>();

    /** the time when got an event */
    private long time;

    /** source */
    private byte[] data;

    @Override
    public byte[] getData() {
        return data;
    }

    /** */
    public Hid4JavaInputEvent(Object source) {
        super(source);
    }

    /** */
    public Hid4JavaInputEvent set(Hid4JavaComponent[] components, byte[] data) {
        this.data = data;
        this.time = System.nanoTime();

        boolean fillAll = Boolean.parseBoolean(System.getProperty("net.java.games.input.InputEvent.fillAll", "false"));

        deque.clear();
        for (Hid4JavaComponent component : components) {
            if (fillAll || component.isValueChanged(data)) {
                component.setValue(data);
                deque.offer(component);
            }
        }

        return this;
    }

    @Override
    public boolean getNextEvent(Event event) {
        if (!deque.isEmpty()) {
            Hid4JavaComponent component = deque.poll();
            event.set(component, component.getValue(), time);
            return true;
        } else {
            return false;
        }
    }
}
