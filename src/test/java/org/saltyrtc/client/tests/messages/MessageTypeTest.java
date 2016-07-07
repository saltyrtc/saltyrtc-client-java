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
import org.saltyrtc.client.exceptions.ValidationError;
import org.saltyrtc.client.messages.Auth;
import org.saltyrtc.client.messages.ClientAuth;
import org.saltyrtc.client.messages.ClientHello;
import org.saltyrtc.client.messages.Data;
import org.saltyrtc.client.messages.DropResponder;
import org.saltyrtc.client.messages.InitiatorServerAuth;
import org.saltyrtc.client.messages.Key;
import org.saltyrtc.client.messages.NewInitiator;
import org.saltyrtc.client.messages.NewResponder;
import org.saltyrtc.client.messages.ResponderServerAuth;
import org.saltyrtc.client.messages.Restart;
import org.saltyrtc.client.messages.SendError;
import org.saltyrtc.client.messages.ServerHello;
import org.saltyrtc.client.messages.Token;

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
                { ClientHello.class, "client-hello" },
                { ClientAuth.class, "client-auth" },
                { InitiatorServerAuth.class, "server-auth" },
                { ResponderServerAuth.class, "server-auth" },
                { NewInitiator.class, "new-initiator" },
                { NewResponder.class, "new-responder" },
                { DropResponder.class, "drop-responder" },
                { SendError.class, "send-error" },
                { Token.class, "token" },
                { Key.class, "key" },
                { Auth.class, "auth" },
                { Restart.class, "restart" },
                { Data.class, "data" }
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
                final Class[] paramTypes = ctor.getParameterTypes();
                if (paramTypes.length == 1 && paramTypes[0] == Map.class) {
                    ctor.newInstance(map);
                }
            }
        } catch (InvocationTargetException e) {
            // Because of the use of reflection, the IllegalArgumentException we want
            // is wrapped inside an InvocationTargetException.
            assertEquals(ValidationError.class, e.getCause().getClass());
            assertEquals("Type must be '" + this.type + "'", e.getCause().getMessage());
        }
    }

}
