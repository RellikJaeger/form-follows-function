/*
 * Copyright 2010 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

package org.f3.jdi.event;

import org.f3.jdi.F3ObjectReference;
import org.f3.jdi.F3VirtualMachine;
import org.f3.jdi.F3Wrapper;
import com.sun.jdi.event.MonitorContendedEnteredEvent;

/**
 *
 * @author sundar
 */
public class F3MonitorContendedEnteredEvent extends F3LocatableEvent
        implements MonitorContendedEnteredEvent {
    public F3MonitorContendedEnteredEvent(F3VirtualMachine f3vm, MonitorContendedEnteredEvent underlying) {
        super(f3vm, underlying);
    }

    public F3ObjectReference monitor() {
        return F3Wrapper.wrap(virtualMachine(), underlying().monitor());
    }

    @Override
    protected MonitorContendedEnteredEvent underlying() {
        return (MonitorContendedEnteredEvent) super.underlying();
    }
}
