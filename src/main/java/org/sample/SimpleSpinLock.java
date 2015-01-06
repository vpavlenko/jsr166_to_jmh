package org.sample;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.VerboseMode;

import java.io.PrintWriter;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Objects.requireNonNull;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 10, time = 200, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 10, time = 200, timeUnit = TimeUnit.MILLISECONDS)
@Fork(5)
@State(Scope.Benchmark)
public class SimpleSpinLock {

    final int NUM_THREADS = 1;

    @Param({"0", "10", "20", "30", "40", "50", "60", "70", "80", "90", "100"})
    public int num_tokens;

    AtomicInteger lock = new AtomicInteger(0);

    @Benchmark
    @Threads(NUM_THREADS)
    public void measureSpinLockToggleUnderContention() {
        while (!lock.compareAndSet(0, 1));
        Blackhole.consumeCPU(num_tokens);
        lock.set(0);
        Blackhole.consumeCPU(num_tokens);
    }

    public static void main(String[] args) throws RunnerException, InterruptedException {
        PrintWriter pw = new PrintWriter(System.out, true);

        pw.println(System.getProperty("java.runtime.name") + ", " + System.getProperty("java.runtime.version"));
        pw.println(System.getProperty("java.vm.name") + ", " + System.getProperty("java.vm.version"));
        pw.println(System.getProperty("os.name") + ", " + System.getProperty("os.version") + ", " + System.getProperty("os.arch"));

        runSingleBenchmark(pw);
        pw.println();

        pw.flush();
        pw.close();
    }

    private static void runSingleBenchmark(PrintWriter pw) throws RunnerException {
        Options opts = new OptionsBuilder()
                .verbosity(VerboseMode.SILENT)
                .build();

        Collection<RunResult> results = new Runner(opts).run();
        for (RunResult r : results) {
            String name = simpleName(r.getParams().getBenchmark());
            double score = r.getPrimaryResult().getScore();
            double scoreError = r.getPrimaryResult().getStatistics().getMeanErrorAt(0.99);
            pw.printf("%30s: %11.3f ± %10.3f ns%n", name, score, scoreError);
        }
    }

    private static String simpleName(String qName) {
        int lastDot = requireNonNull(qName).lastIndexOf('.');
        return lastDot < 0 ? qName : qName.substring(lastDot + 1);
    }

}
