SimpleSpinLock.java
---

http://gee.cs.oswego.edu/cgi-bin/viewcvs.cgi/jsr166/src/test/loops/SimpleSpinLockLoops.java?view=co

От 1 до 100 тредов одновременно стартуют и сражаются за CAS-lock в течение 2000000 итераций. На каждой итерации содержательно происходит
```
while (!lock.compareAndSet(0, 1)) ;
USELESS_COMPUTATIONS
lock.set(0);
if ((x += readBarrier) == 0)
    ++readBarrier;
USELESS_COMPUTATIONS
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


Заметки
------

StringMapLoops.java - тестирует производительность операций в среднем, где операции - это случайные добавления/удаления в мап. В чем ценность такого теста?

UncheckedLockLoops.java - сравнение нескольких видов локов, выглядит интересно, надо переписать.

Вопросы
---

1. Это - идиома "loop unrolling prevention"?
```
if (i == k) {
    k = i << 1;
    i = i + (i >>> 1);
}
else
    i = k;
```
