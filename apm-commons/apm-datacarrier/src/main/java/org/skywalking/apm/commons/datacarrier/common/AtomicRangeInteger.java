package org.skywalking.apm.commons.datacarrier.common;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by wusheng on 2016/10/25.
 */
public class AtomicRangeInteger extends Number implements Serializable {
    private static final long serialVersionUID = -4099792402691141643L;
    private AtomicInteger value;
    private int startValue;
    private int endValue;

    public AtomicRangeInteger(int startValue, int maxValue) {
        this.value = new AtomicInteger(startValue);
        this.startValue = startValue;
        this.endValue = maxValue - 1;
    }

    public final int getAndIncrement() {
        int current;
        int next;
        do {
            current = this.value.get();
            next = current >= this.endValue ? this.startValue : current + 1;
        }
        while (!this.value.compareAndSet(current, next));

        return current;
    }

    public final int get() {
        return this.value.get();
    }

    public int intValue() {
        return this.value.intValue();
    }

    public long longValue() {
        return this.value.longValue();
    }

    public float floatValue() {
        return this.value.floatValue();
    }

    public double doubleValue() {
        return this.value.doubleValue();
    }
}
