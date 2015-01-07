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
@Warmup(iterations = 10, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 10, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
@Fork(5)
@State(Scope.Benchmark)
public class SimpleSpinLock {

//    @Param({"0", "4", "16", "64", "256", "1024", "4096"})
    @Param({"16384", "65536", "262144"})
    public int numTokens;

    AtomicInteger lock = new AtomicInteger(0);

    @Benchmark
    public void measureSpinLockToggleUnderContention() {
        while (!lock.compareAndSet(0, 1));
        Blackhole.consumeCPU(numTokens);
        lock.set(0);
        Blackhole.consumeCPU(numTokens);
    }

    public static void main(String[] args) throws RunnerException, InterruptedException {
        PrintWriter pw = new PrintWriter(System.out, true);

        pw.println(System.getProperty("java.runtime.name") + ", " + System.getProperty("java.runtime.version"));
        pw.println(System.getProperty("java.vm.name") + ", " + System.getProperty("java.vm.version"));
        pw.println(System.getProperty("os.name") + ", " + System.getProperty("os.version") + ", " + System.getProperty("os.arch"));

        for (int numThreads = 1; numThreads <= 8; ++numThreads) {
            pw.println("Run benchmark with " + numThreads + " threads");
            runSingleBenchmark(pw, numThreads);
        }
        pw.println();

        pw.flush();
        pw.close();
    }

    private static void runSingleBenchmark(PrintWriter pw, int numThreads) throws RunnerException {
        Options opts = new OptionsBuilder()
                .verbosity(VerboseMode.SILENT)
                .threads(numThreads)
                .build();

        Collection<RunResult> results = new Runner(opts).run();
        for (RunResult r : results) {
            int numTokens = Integer.parseInt(r.getParams().getParam("numTokens"));
            String name = simpleName(r.getParams().getBenchmark());
            double score = r.getPrimaryResult().getScore();
            double scoreError = r.getPrimaryResult().getStatistics().getMeanErrorAt(0.99);
            pw.printf("%30s: threads: %2d    tokens: %6d    %11.3f ± %10.3f ns%n", name, numThreads, numTokens,
                    score, scoreError);
        }
    }

    private static String simpleName(String qName) {
        int lastDot = requireNonNull(qName).lastIndexOf('.');
        return lastDot < 0 ? qName : qName.substring(lastDot + 1);
    }

}
