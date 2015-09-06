package org.eluder.logback.ext.lmax.appender;

import org.junit.Test;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

public class LoggingEventDisruptorAppenderPerf {

    @Test
    public void benchmarkTest() throws Exception {
        Options options = new OptionsBuilder()
                .include(AsyncAppenderBenchmark.class.getSimpleName())
                .include(DisruptorAppenderBenchmark.class.getSimpleName())
                .warmupTime(TimeValue.seconds(5))
                .warmupIterations(2)
                .measurementTime(TimeValue.seconds(10))
                .measurementIterations(10)
                .threads(16)
                .forks(1)
                .build();
        new Runner(options).run();
    }

}
