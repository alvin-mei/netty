package io.netty.example.echo.reactor;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author meijingling
 * @date 18/11/19
 */
public class SubReactorGroup {
    private final static int DEFAULT_COUNT = 8;
    private SubReactor[] subReactors;
    private int reactorCount;
    private AtomicInteger index;


    public SubReactorGroup() {
        this.reactorCount = DEFAULT_COUNT;
        this.index = new AtomicInteger(0);
        subReactors = new SubReactor[DEFAULT_COUNT];
        for(int i = 0; i < reactorCount; i++) {
            subReactors[i] = new SubReactor();
        }
    }

    public SubReactor next() {
        return subReactors[index.getAndIncrement() & reactorCount - 1];
    }
}
