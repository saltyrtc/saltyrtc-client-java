/*
 * Copyright (c) 2016 Threema GmbH / SaltyRTC Contributors
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.client.tests.messages;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.saltyrtc.client.messages.ResponderServerAuth;
import org.saltyrtc.client.messages.ServerHello;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * Parametrized test to see if all messages validate their type.
 */
@RunWith(Parameterized.class)
public class MessageTypeTest {

    @Parameterized.Parameters(name = "{index}: {1}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                { ServerHello.class, "server-hello" },
                { ResponderServerAuth.class, "server-auth" }
        });
    }

    private Class klass;
    private String type;

    public MessageTypeTest(Class klass, String type) {
        this.klass = klass;
        this.type = type;
    }

    /**
     * Warning: Some hacky metaprogramming ahead :)
     */
    @Test
    public void testException() throws IllegalAccessException, InvocationTargetException, InstantiationException {
        Map<String, Object> map = new HashMap<>();
        map.put("type", "invalid-type");
        try {
            // Find the constructor that takes a map and invoke it.
            for (Constructor ctor : this.klass.getConstructors()) {
                if (ctor.getParameterCount() == 1 && ctor.getParameters()[0].getType() == Map.class) {
                    ctor.newInstance(map);
                }
            }
        } catch (InvocationTargetException e) {
            // Because of the use of reflection, the IllegalArgumentException we want
            // is wrapped inside an InvocationTargetException.
            assertEquals(IllegalArgumentException.class, e.getCause().getClass());
            assertEquals("Type must be '" + this.type + "'", e.getCause().getMessage());
        }
    }

}