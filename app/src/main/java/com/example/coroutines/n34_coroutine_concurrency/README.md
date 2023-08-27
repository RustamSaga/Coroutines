# Concurrency

Несколько простых примеров, в которых две корутины меняют одну переменную. К чему это приводит, и
какие есть способы сделать это правильно.

Пример: не заработает

```kotlin
viewModelScope.launch {

    var i = 0

    val job1 = launch(Dispatchers.Default) {
        repeat(100000) {
            i++
        }
    }

    val job2 = launch(Dispatchers.Default) {
        repeat(100000) {
            i++
        }
    }

    job1.join()
    job2.join()

    log("i = $i")
}
```

Нам нужно параллельно работать (изменять) с одной переменно из нескольких корутин

## Можно использовать Atomic

```kotlin
var i = AtomicInteger()

val job1 = launch(Dispatchers.Default) {
    repeat(100000) {
        i.incrementAndGet()
    }
}

val job2 = launch(Dispatchers.Default) {
    repeat(100000) {
        i.incrementAndGet()
    }
}

job1.join()
job2.join()

log("i = $i")
```

## synchronized

```kotlin
var i = 0

@Synchronized
fun increment() {
    i++
}

val job1 = launch(Dispatchers.Default) {
    repeat(100000) {
        increment()
    }
}

val job2 = launch(Dispatchers.Default) {
    repeat(100000) {
        increment()
    }
}

job1.join()
job2.join()

log("i = $i")
```

// Но! Если где-то в корутинах вызываются suspend функции, которые приостанавливают выполнение кода,
то в этот момент synchronized блокировки снимаются, и вся synchronized логика рушится.

## Mutex - вместо synchronized

```kotlin
val mutex = Mutex()
var i = 0

suspend fun increment() {
    mutex.withLock {
        i++
    }
}

val job1 = launch(Dispatchers.Default) {
    repeat(100000) {
        increment()
    }
}

val job2 = launch(Dispatchers.Default) {
    repeat(100000) {
        increment()
    }
}

job1.join()
job2.join()

log("i = $i")
```

## Actor

капотом работает Channel, который был создан для безопасной передачи данных между корутинами.

```kotlin
private val actorChannel = viewModelScope.actor<Unit> {
    for (e in channel) {
        i++
    }
}

val job1 = launch(Dispatchers.Default) {
    repeat(100000) {
        actorChannel.send(Unit)
    }
}

val job2 = launch(Dispatchers.Default) {
    repeat(100000) {
        actorChannel.send(Unit)
    }
}

job1.join()
job2.join()

actorChannel.close()

log("i = $i")
```

Результат будет правильный, но время работы - достаточно долгое. Этот вариант не особо подходит,
если корутины мало работают сами, а только и делают, что отправляют данные в канал