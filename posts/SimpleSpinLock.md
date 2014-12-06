SimpleSpinLock
=======

Problem statement
-----

Given the following microbenchmark:
```java
@State(Scope.Benchmark)
public class SimpleSpinLock {

    final int NUM_THREADS = 2;
    final int LOCK_INSIDE_BACKOFF = 3;
    final int LOCK_OUTSIDE_BACKOFF = 4;

    AtomicInteger lock = new AtomicInteger(0);

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    @Threads(NUM_THREADS)
    public void measureSpinLockToggleUnderContention() {
        while (!lock.compareAndSet(0, 1));
        Blackhole.consumeCPU(LOCK_INSIDE_BACKOFF);
        lock.set(0);
        Blackhole.consumeCPU(LOCK_OUTSIDE_BACKOFF);
    }

}
```

That's my first benchmark to analyze. Firstly, I'd like to gather sample time measurements for the trivial case: with only one thread being run. There still should be CAS instruction being emitted and run on HW, so I expect to measure the cost of some cache flush on my HW.

Hardware
---

By default I do all the experiments on my Mac (). I also use Linux VPS, though I doubt it's a good idea.

Sample time measurements for the single thread
---

Let's run it on my Mac.

```java
final int NUM_THREADS = 1;
final int LOCK_INSIDE_BACKOFF = 1;
final int LOCK_OUTSIDE_BACKOFF = 1;
```

I run it with the following command-line arguments:
```
java -jar target/benchmarks.jar -wi 10 -i 10 -f 3
```

I get the following output:
```
# VM invoker: /Library/Java/JavaVirtualMachines/jdk1.8.0_05.jdk/Contents/Home/jre/bin/java
# VM options: <none>
# Warmup: 10 iterations, 1 s each
# Measurement: 10 iterations, 1 s each
# Timeout: 10 min per iteration
# Threads: 1 thread, will synchronize iterations
# Benchmark mode: Average time, time/op
# Benchmark: org.sample.SimpleSpinLock.measureSpinLockToggleUnderContention

# Run progress: 0,00% complete, ETA 00:01:00
# Fork: 1 of 3
# Warmup Iteration   1: 26,340 ns/op
# Warmup Iteration   2: 28,441 ns/op
# Warmup Iteration   3: 25,553 ns/op
# Warmup Iteration   4: 25,189 ns/op
# Warmup Iteration   5: 25,389 ns/op
# Warmup Iteration   6: 25,217 ns/op
# Warmup Iteration   7: 25,020 ns/op
# Warmup Iteration   8: 25,277 ns/op
# Warmup Iteration   9: 25,145 ns/op
# Warmup Iteration  10: 25,614 ns/op
Iteration   1: 22,968 ns/op
Iteration   2: 23,354 ns/op
Iteration   3: 23,825 ns/op
Iteration   4: 22,100 ns/op
Iteration   5: 21,276 ns/op
Iteration   6: 21,396 ns/op
Iteration   7: 21,384 ns/op
Iteration   8: 21,032 ns/op
Iteration   9: 20,578 ns/op
Iteration  10: 20,987 ns/op

# Run progress: 33,33% complete, ETA 00:00:41
# Fork: 2 of 3
# Warmup Iteration   1: 23,719 ns/op
# Warmup Iteration   2: 25,079 ns/op
# Warmup Iteration   3: 20,694 ns/op
# Warmup Iteration   4: 20,873 ns/op
# Warmup Iteration   5: 20,454 ns/op
# Warmup Iteration   6: 20,639 ns/op
# Warmup Iteration   7: 20,584 ns/op
# Warmup Iteration   8: 21,176 ns/op
# Warmup Iteration   9: 20,755 ns/op
# Warmup Iteration  10: 21,128 ns/op
Iteration   1: 20,934 ns/op
Iteration   2: 20,342 ns/op
Iteration   3: 20,157 ns/op
Iteration   4: 20,364 ns/op
Iteration   5: 20,437 ns/op
Iteration   6: 20,574 ns/op
Iteration   7: 20,222 ns/op
Iteration   8: 20,068 ns/op
Iteration   9: 20,332 ns/op
Iteration  10: 20,408 ns/op

# Run progress: 66,67% complete, ETA 00:00:20
# Fork: 3 of 3
# Warmup Iteration   1: 23,970 ns/op
# Warmup Iteration   2: 24,197 ns/op
# Warmup Iteration   3: 20,417 ns/op
# Warmup Iteration   4: 20,123 ns/op
# Warmup Iteration   5: 20,309 ns/op
# Warmup Iteration   6: 20,404 ns/op
# Warmup Iteration   7: 20,234 ns/op
# Warmup Iteration   8: 20,233 ns/op
# Warmup Iteration   9: 20,458 ns/op
# Warmup Iteration  10: 20,453 ns/op
Iteration   1: 20,297 ns/op
Iteration   2: 19,979 ns/op
Iteration   3: 20,451 ns/op
Iteration   4: 20,533 ns/op
Iteration   5: 20,414 ns/op
Iteration   6: 20,321 ns/op
Iteration   7: 20,532 ns/op
Iteration   8: 20,889 ns/op
Iteration   9: 20,581 ns/op
Iteration  10: 20,504 ns/op


Result: 20,908 ±(99.9%) 0,642 ns/op [Average]
  Statistics: (min, avg, max) = (19,979, 20,908, 23,825), stdev = 0,960
  Confidence interval (99.9%): [20,266, 21,550]


# Run complete. Total time: 00:01:01

Benchmark                                                  Mode  Samples   Score   Error  Units
o.s.SimpleSpinLock.measureSpinLockToggleUnderContention    avgt       30  20,908 ± 0,642  ns/op
```

Ok, looks like the most interesting part is the final score, if only I see that in all forks I've reached "steady state" while doing warmup iterations. That's not the case for the fork 1 from above, but it seems to be true for the forks 2 and 3. So let's re-run it with `-wi 20`:

```
Benchmark                                                  Mode  Samples   Score   Error  Units
o.s.SimpleSpinLock.measureSpinLockToggleUnderContention    avgt       30  21,078 ± 0,259  ns/op
```

Then I tried to run it on my Linux VPS on DigitalOcean (with `-wi 10`) and was very disappointed, because:
```
Result: 557.724 ±(99.9%) 112.817 ns/op [Average]
  Statistics: (min, avg, max) = (367.401, 557.724, 986.130), stdev = 168.859
  Confidence interval (99.9%): [444.907, 670.541]


# Run complete. Total time: 00:06:23

Benchmark                                                  Mode  Samples    Score     Error  Units
o.s.SimpleSpinLock.measureSpinLockToggleUnderContention    avgt       30  557.724 ± 112.817  ns/op
```

So it doesn't seem to be reasonable to run the perf tests on VPS. Alright, then I should set up my own true Linux somewhere else.

Anyway, let's grab perfasm data from Linux machine. Hm, no, it doesn't work the same way as it doesn't work under Vagrant on my laptop:
```
$ java -jar target/benchmarks.jar -wi 10 -i 10 -f 1 -prof perfasm
# VM invoker: /usr/lib/jvm/java-8-oracle/jre/bin/java
# VM options: <none>
# Warmup: 10 iterations, 1 s each
# Measurement: 10 iterations, 1 s each
# Timeout: 10 min per iteration
# Threads: 100 threads, will synchronize iterations
# Benchmark mode: Average time, time/op
# Benchmark: org.sample.SimpleSpinLock.measureSpinLockToggleUnderContention

# Run progress: 0.00% complete, ETA 00:00:20
# Fork: 1 of 1
# Preparing profilers: perfasm 
# Profilers consume stdout and stderr from target VM, use -v EXTRA to copy to console
<forked VM failed with exit code 255>
<stdout last='20 lines'>
</stdout>
<stderr last='20 lines'>
WARNING: Kernel address maps (/proc/{kallsyms,modules}) are restricted,
check /proc/sys/kernel/kptr_restrict.

Samples in kernel functions may not be resolved if a suitable vmlinux
file is not found in the buildid cache or in the vmlinux path.

Samples in kernel modules won't be resolved at all.

If some relocation was applied (e.g. kexec) symbols may be misresolved
even with a suitable vmlinux or kallsyms file.

Error:
The instructions event is not supported.
/usr/lib/jvm/java-8-oracle/jre/bin/java: Terminated
</stderr>

# Run complete. Total time: 00:00:00

Benchmark    Mode  Samples  Score   Error  Units
```

Single thread
---

Being run on a single thread this benchmark should work as follows:
- `lock.compareAndSet(0, 1)` sets the variable




Multiple threads agenda
---

I also gather perfasm along with it. Then I give some naïve model of what time observations should we expect when we change the parameters. Then I plot graphs for the two simple cases and discuss the results. Then I try to correct the model and do more experiments.