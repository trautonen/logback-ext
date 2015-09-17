package org.eluder.logback.ext.lmax.appender;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.BusySpinWaitStrategy;
import com.lmax.disruptor.SleepingWaitStrategy;
import com.lmax.disruptor.WaitStrategy;
import com.lmax.disruptor.YieldingWaitStrategy;

public class WaitStrategyFactory {

    public static final WaitStrategy DEFAULT_WAIT_STRATEGY = new BlockingWaitStrategy();

    public static WaitStrategy createFromType(String name) {
        if ("BusySpin".equalsIgnoreCase(name)) {
            return new BusySpinWaitStrategy();
        } else if ("Blocking".equalsIgnoreCase(name)) {
            return new BlockingWaitStrategy();
        } else if ("Yielding".equalsIgnoreCase(name)) {
            return new YieldingWaitStrategy();
        } else if ("Sleeping".equalsIgnoreCase(name)) {
            return new SleepingWaitStrategy();
        } else {
            throw new IllegalArgumentException("Invalid or unsupported wait strategy type '" + name + "'");
        }
    }

}
