SimpleSpinLock.java
---

http://gee.cs.oswego.edu/cgi-bin/viewcvs.cgi/jsr166/src/test/loops/SimpleSpinLockLoops.java?view=co

От 1 до 100 тредов (1, 2, 3, 4, 6, 8, 12, 16, 24, 32, 48, 64, 96 тредов) одновременно стартуют и сражаются за CAS-lock в течение 2 000 000 итераций. На каждой итерации содержательно происходит
```
while (!lock.compareAndSet(0, 1)) ;
USELESS_COMPUTATIONS (0-3 арифметических действия)
lock.set(0);
if ((x += readBarrier) == 0)
    ++readBarrier;
USELESS_COMPUTATIONS (0-1 арифметических действия)
```
где `lock` - это `AtomicInteger`, а `readBarrier` - это `volatile int`.

Зачем здесь volatile read/write через `readBarrier`? Если без него, то в тесте просто интересно бы померить среднее время, за которое происходит захват-отдача лока, если за лок одновременно сражается от 1 до 100 тредов.

Я набросал код этого теста под JMH: [src/main/java/org/sample/SimpleSpinLock.java](src/main/java/org/sample/SimpleSpinLock.java)

Вопросы:

1. Это вообще то, что нужно?
2. Нужно ли было переносить USELESS_COMPUTATIONS?
3. Нужно ли было переносить `readBarrier`?
4. Можно ли сделать `@Param` для `@GroupThreads` в JMH, или как удобнее всего гонять этот тест с линейно возрастающим количеством тредов, чтобы строить графики?
5. Этот тест имеет какой-то физический смысл?

Ответ @shipilev
---

Привет,

1. Это вообще то, что нужно?

Да, в этом направлении надо копать.

2. Нужно ли было переносить USELESS_COMPUTATIONS?

Я думаю, что это такая форма backoff-а в стиле DL [1]. В JMH на этот
случай есть Blackhole.consumeCPU. Вообще с backoff-ами связана
интересная задача по поводу измерения производительности под
contention'ом, которая в том числе прилежит к задаче правильного
измерения latency. См. также:
 http://shipilev.net/blog/2014/nanotrusting-nanotime/

3. Нужно ли было переносить readBarrier?

Думаю, что нет. Наверняка достаточно вкорёжить тот же
Blackhole.consumeCPU. Однако, надо бы в сгенерированном коде проверить,
что компилер не переставил местами CAS, consumeCPU, set, consumeCPU. Для
этого проще всего взять Linux и запустить на нём JMH с "-prof perfasm".

4. Можно ли сделать @Param для @GroupThreads в JMH, или как удобнее
всего гонять этот тест с линейно возрастающим количеством тредов, чтобы
строить графики?

Во-первых, в твоём случае нужен не @GroupThreads, а просто @Threads. А
во-вторых, на аннотациях этого собрать пока нельзя:
 https://bugs.openjdk.java.net/browse/CODETOOLS-7901012

Для таких случаев можно использовать Java API, типа:
 https://github.com/shipilev/timers-bench/blob/master/src/main/java/net/shipilev/TimersBench.java


5. Этот тест имеет какой-то физический смысл?

Да, имеет. Он по факту мерит скорость единичного обмена между кешами, и
является важным baseline-ом.

-Л.

[1] DL = Doug Lea
