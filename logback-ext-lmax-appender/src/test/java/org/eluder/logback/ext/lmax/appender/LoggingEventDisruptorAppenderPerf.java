package org.eluder.logback.ext.lmax.appender;

import org.junit.Test;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

public class LoggingEventDisruptorAppenderPerf {

    @Test
    public void benchmarkTest() throws Exception {
        Options options = new OptionsBuilder()
                .include(AsyncAppenderBenchmark.class.getSimpleName())
                .include(DisruptorAppenderBenchmark.class.getSimpleName())
                .measurementIterations(10)
                .forks(1)
                .build();
        new Runner(options).run();
    }

}
