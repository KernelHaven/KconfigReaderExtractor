/*
 * Copyright 2020 University of Hildesheim, Software Systems Engineering
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.ssehub.kernel_haven.kconfigreader;

import java.util.concurrent.atomic.AtomicLong;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests the {@link UndoOperationTest}.
 * @author El-Sharkawy
 */
public class UndoOperationTest {

    /**
     * Tests that {@link UndoThread#revertOperation()} is applied after the specified <tt>waitTime</tt>.
     */
    @Test
    public void testRevertOperation() {
        AtomicLong counter = new AtomicLong();
        long waitTime = 500;
        
        // Specifies undo operation
        UndoThread undo = new UndoThread(waitTime) {
            
            @Override
            public void revertOperation() {
                long now = System.currentTimeMillis();
                counter.set(now);
            }
        };
        
        // Measures that undo is applied after wait time
        long now = System.currentTimeMillis();
        counter.set(now);
        undo.runAndJoin();  
        long newCounterValue = counter.get();
        
        // Tests
        Assert.assertTrue("The UndoThread either did not perform the undo operation or did it to early",
            newCounterValue >= now + waitTime);
    }

}
