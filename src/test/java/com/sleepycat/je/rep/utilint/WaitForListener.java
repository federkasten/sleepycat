/*-
 * See the file LICENSE for redistribution information.
 *
 * Copyright (c) 2002, 2014 Oracle and/or its affiliates.  All rights reserved.
 *
 */

package com.sleepycat.je.rep.utilint;

import static com.sleepycat.je.rep.ReplicatedEnvironment.State.MASTER;
import static com.sleepycat.je.rep.ReplicatedEnvironment.State.REPLICA;
import static com.sleepycat.je.rep.ReplicatedEnvironment.State.UNKNOWN;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import com.sleepycat.je.rep.ReplicatedEnvironment.State;
import com.sleepycat.je.rep.StateChangeEvent;
import com.sleepycat.je.rep.StateChangeListener;

/**
 * Utility class to wait for one of a set of state change events.
 *
 * This is the preferred class to use as an alternative to the WaitForXXX
 * sequence of classes in this package.
 */
public class WaitForListener implements StateChangeListener {

    final CountDownLatch latch = new CountDownLatch(1);
    final Set<State>  waitStates;

    private boolean success = true;

    public WaitForListener(State... states) {
        waitStates = new HashSet<State>(Arrays.asList(states));
    }

    public void stateChange(StateChangeEvent stateChangeEvent) {

        if (waitStates.contains(stateChangeEvent.getState())) {
            latch.countDown();
            return;
        }

        if (stateChangeEvent.getState().isDetached()) {
            /* It will never transition out of this state. */
            success = false;
            latch.countDown();
            return;
        }

        /* Some other intermediate state, not of interest; ignore it. */
    }

    public boolean await()
        throws InterruptedException {

        latch.await();
        return success;
    }

    /**
     * Specialized listener for Active states
     */
    public static class Active extends WaitForListener {
        public Active() {
            super(MASTER, REPLICA);
        }
    }

    /**
     * Specialized listener for transition to UNKNOWN
     */
    public static class Unknown extends WaitForListener {
        public Unknown() {
            super(UNKNOWN);
        }
    }
}
