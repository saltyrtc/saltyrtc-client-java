/*
 * Copyright (c) 2016 Threema GmbH / SaltyRTC Contributors
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.client.tests.nonce;

import org.junit.Test;
import org.saltyrtc.client.exceptions.OverflowException;
import org.saltyrtc.client.nonce.CombinedSequence;
import org.saltyrtc.client.nonce.CombinedSequenceSnapshot;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BrokenBarrierException;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CombinedSequenceTest {

    @Test
    public void testRandomSequenceNumber() {
        // The generated sequence numbers should always be in the correct range
        Set<Long> randomNumbers = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            final CombinedSequence cs = new CombinedSequence();
            assertEquals(cs.getOverflow(), 0);
            final long seq = cs.getSequenceNumber();
            assertTrue(seq >= 0L);
            assertTrue(seq <= CombinedSequence.SEQUENCE_NUMBER_MAX);
            randomNumbers.add(seq);
        }

        // The 100 sequence numbers should be distinct
        assertEquals(100, randomNumbers.size());
    }

    @Test
    public void testSequenceIncrement() throws OverflowException {
        CombinedSequence cs;
        boolean valid;
        do {
            cs = new CombinedSequence();
            valid = cs.getSequenceNumber() != CombinedSequence.SEQUENCE_NUMBER_MAX &&
                    cs.getOverflow() != CombinedSequence.OVERFLOW_MAX;
        } while (!valid);
        final long oldSequence = cs.getSequenceNumber();
        final int oldOverflow = cs.getOverflow();
        CombinedSequenceSnapshot incremented = cs.next();
        assertEquals(oldSequence + 1, incremented.getSequenceNumber());
        assertEquals(oldOverflow, incremented.getOverflow());
    }

    @Test
    public void testSequenceOverflow() throws OverflowException, NoSuchFieldException, IllegalAccessException {
        CombinedSequence cs = new CombinedSequence();
        final int oldOverflow = cs.getOverflow();
        Field field = CombinedSequence.class.getDeclaredField("sequenceNumber");
        field.setAccessible(true);
        field.setLong(cs, CombinedSequence.SEQUENCE_NUMBER_MAX - 1);
        cs.next();
        assertEquals(0, cs.getSequenceNumber());
        assertEquals(oldOverflow + 1, cs.getOverflow());
    }

    @Test(expected = OverflowException.class)
    public void testOverflowOverflow() throws OverflowException, NoSuchFieldException, IllegalAccessException {
        CombinedSequence cs = new CombinedSequence();

        Field sequenceField = CombinedSequence.class.getDeclaredField("sequenceNumber");
        sequenceField.setAccessible(true);
        sequenceField.setLong(cs, CombinedSequence.SEQUENCE_NUMBER_MAX - 1);

        Field overflowField = CombinedSequence.class.getDeclaredField("overflow");
        overflowField.setAccessible(true);
        overflowField.setInt(cs, CombinedSequence.OVERFLOW_MAX - 1);

        // This will throw
        cs.next();
    }

    /**
     * Make sure the next() method is thread safe.
     */
    @Test
    public void testThreadSafety() throws InterruptedException, BrokenBarrierException {
        final int THREAD_COUNT = 100;

        final CombinedSequence cs = new CombinedSequence(0, 0);
        final List<Long> list = Collections.synchronizedList(new ArrayList<Long>());
        final List<Thread> threads = Collections.synchronizedList(new ArrayList<Thread>());
        final List<Long> expected = new ArrayList<>();

        class Incrementor extends Thread {
            public void run() {
                try {
                    list.add(cs.next().getCombinedSequence());
                } catch (OverflowException e) {
                    e.printStackTrace();
                }
            }
        }

        for (Long i = 1L; i <= THREAD_COUNT; i++) {
            threads.add(new Incrementor());
            expected.add(i);
        }
        for (Thread incrementor : threads) {
            incrementor.start();
        }
        for (Thread incrementor : threads) {
            incrementor.join();
        }

        Collections.sort(list);
        assertArrayEquals("List was " + list, expected.toArray(), list.toArray());
    }
}
