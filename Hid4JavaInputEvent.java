/*
 * Copyright (c) 2023 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.awt.joystick.hid4java;

import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedList;

import net.java.games.input.Component;
import net.java.games.input.Event;
import net.java.games.input.InputEvent;


/**
 * Hid4JavaInputEvent.
 * <p>
 * <h4>system property</h4>
 * <li>"net.java.games.input.InputEvent.fillAll" ... determine to fill all events (true) or events which value is changed (false)</li>
 * </p>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2023-11-07 nsano initial version <br>
 */
public class Hid4JavaInputEvent extends InputEvent {

    /** which value is changed only */
    private final Deque<Hid4JavaComponent> deque = new LinkedList<>();

    /** the time when got an event */
    private final long time;

    /** source */
    private final byte[] data;

    /** for debug */
    public byte[] getData() {
        return data;
    }

    /** */
    public Hid4JavaInputEvent(Object source, Component[] components, byte[] data) {
        super(source);
        this.data = data;
        this.time = System.nanoTime();

        boolean fillAll = Boolean.parseBoolean(System.getProperty("net.java.games.input.InputEvent.fillAll", "false"));

        deque.clear();
        for (Hid4JavaComponent component : Arrays.stream(components).map(Hid4JavaComponent.class::cast).toArray(Hid4JavaComponent[]::new)) {
            if (fillAll || component.isValueChanged(data)) {
                component.setValue(data);
                deque.offer(component);
            }
        }
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
