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

import net.ssehub.kernel_haven.util.Logger;

/**
 * 
 * A thread that reverts temporary fixes done by the {@link KconfigReaderWrapper}, which were applied in order to handle
 * older Linux versions.
 * This threat waits a specified time before applying the undo operation to avoid concurrent operations.
 * This is required since the operations of the {@link KconfigReaderWrapper} are run in separate {@link Process}es.
 * @author El-Sharkawy
 */
public abstract class UndoThread extends Thread {
    
    private long waitTime;
    
    /**
     * Creates a new {@link UndoThread} instance to specify a new undo operation.
     * @param waitTime Specifies how long to wait (in ms) before applying {@link #revertOperation()}.
     */
    public UndoThread(long waitTime) {
        this.waitTime = waitTime;
    }
    
    /**
     * Specifies the undo operation to be applied after waiting.
     */
    public abstract void revertOperation();
    
    @Override
    public void run() {
        long started = System.currentTimeMillis();
        long now;
        
        // Wait
        do {
            now = System.currentTimeMillis();
            try {
                Thread.sleep(waitTime);
            } catch (InterruptedException e) {
                // Very unlikely that this happens.
                Logger.get().logException("UndoThread interrupted while waiting", e);
            }
        } while (now - started < waitTime);
        
        // Undo
        revertOperation();
    }
    
    /**
     * Short hand to start and join the {@link UndoThread.
     */
    public void runAndJoin() {
        start();
        try {
            join();
        } catch (InterruptedException e) {
            // Very unlikely that this happens.
            Logger.get().logException("UndoThread could not be joined", e);
        }
    }
}
