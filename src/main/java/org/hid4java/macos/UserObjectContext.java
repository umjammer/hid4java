/*
 * Copyright (c) 2023 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package org.hid4java.macos;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;


/**
 * UserObjectContext.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2023-10-06 nsano initial version <br>
 */
public class UserObjectContext extends Structure {

    public int objectID;

    /** */
    public UserObjectContext() {
    }

    /** */
    public UserObjectContext(Pointer p) {
        super(p);
        // TODO why?
        objectID = getPointer().getInt(0);
    }

    /** */
    public static class ByReference extends UserObjectContext implements Structure.ByReference {
    }

    /** */
    public static class ByValue extends UserObjectContext implements Structure.ByValue {
    }

    @Override
    protected List<String> getFieldOrder() {
        return List.of("objectID");
    }

    /** */
    private static final Map<Integer, Object> objects = new HashMap<>();

    /** */
    private static int objectIDMaster = 0;

    /** */
    public static ByReference createContext(Object o) {
        ByReference object_context = new ByReference();
        object_context.objectID = objectIDMaster++;
        objects.put(object_context.objectID, o);
        return object_context;
    }

    /** */
    public static Object getObjectFromContext(Pointer context) {
        UserObjectContext object_context = new UserObjectContext(context);
        return UserObjectContext.objects.get(object_context.objectID);
    }
}
