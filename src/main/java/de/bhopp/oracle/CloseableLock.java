package de.bhopp.oracle;

import java.io.Closeable;
import java.util.concurrent.locks.Lock;

class CloseableLock implements Closeable {

    private final Lock lock;

    public static CloseableLock of(Lock lock){
        return new CloseableLock(lock);
    }

    private CloseableLock(Lock lock) {
        this.lock = lock;
        lock.lock();
    }

    public void close() {
        lock.unlock();
    }
}
