# Начало. Scope и его корутины. CoroutineExceptionHandler

## Такой код не сработает

```kotlin
private fun onRunCrash() {
    log(crash, "onRun, start")
    try {
        scope.launch {
            Integer.parseInt("a")
        }
    } catch (e: Exception) {
        log(crash, "error $e")
    }
    log(crash, "onRun, end")
}
```

```kotlin
private fun onRunCrash2() {
    log(crash, "onRun, start")
    try {
        thread {
            Integer.parseInt("a")
        }
    } catch (e: Exception) {
        log(crash, "error $e")
    }
    log(crash, "onRun, end")

}
```

Этот метод использует билдер launch, чтобы создать и запустить корутину, и сам после этого
сразу завершается. А корутина живет своей жизнью в отдельном потоке.
Вот именно поэтому try-catch здесь и не срабатывает.

## Обернуть ошибку в try-catch внутри launch

```kotlin
private fun onRun() {
    log(success, "onRun, start")
    // обернуть ошибку
    scope.launch {
        try {
            Integer.parseInt("a")
        } catch (e: Exception) {
            log(success, "error $e")
        }
        log(success, "onRun, end")
    }
}
```

## CoroutineExceptionHandler

`Job` получает из `Continuation` ошибку. Он сообщает об этом родителю (`scope`), который по такому
поводу отменяет себя и всех своих детей. А сам `Job` пытается передать ошибку
в `CoroutineExceptionHandler`.
Если такого обработчика ему не предоставили, то ошибка уходит в глобальный обработчик, что приводит
к крэшу приложения.

```kotlin
private fun onRunByCoroutineExceptionHandler() {
    val handler = CoroutineExceptionHandler { context, exception ->
        log(successByHandler, "handler, $exception")
    }
    scope.launch(handler) {
        Integer.parseInt("a")
    }
}
```

### Отмена корутины (Ошибка одной дочерней корутины отменяет все остальные дочерние корутины)

Даже несмотря на обработчик ошибок, корутина, в которой произошло исключение, сообщит об ошибке в
родительский scope, а тот отменит все свои дочерние корутины

**Учитывайте, что scope отменяет не только корутины, но и себя. А это означает, что в этом scope мы
больше не сможем запустить корутины.**

```kotlin
private fun onRunCancelAll() {
    val handler = CoroutineExceptionHandler { context, exception ->
        log(successByHandler, "first coroutin exception $exception")
    }
    // контекст можно передать также в сам scope = CoroutineScope(Job() + Dispatchers.Default + handler)

    scope.launch(handler) {
        TimeUnit.MILLISECONDS.sleep(1000)
        Integer.parseInt("a")
    }
    scope.launch {
        repeat(5) {
            TimeUnit.MILLISECONDS.sleep(300)
            log(successByHandler, "second coroutine isActive $isActive")
        }
    }
}
```

```
D  second coroutine isActive true [DefaultDispatcher-worker-2]
D  second coroutine isActive true [DefaultDispatcher-worker-2]
D  second coroutine isActive true [DefaultDispatcher-worker-2]
D  first coroutin exception java.lang.NumberFormatException: For input string: "a" [DefaultDispatcher-worker-1]
D  second coroutine isActive false [DefaultDispatcher-worker-2]
D  second coroutine isActive false [DefaultDispatcher-worker-2]
```

### SupervisorJob - что бы при ошибке не отменялись дочерние корутины

```kotlin
 private fun onRunWithSupervisorJob() {
    val handler = CoroutineExceptionHandler { context, exception ->
        log(successBySupervisorJob, "first coroutin exception $exception")
    }
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default + handler)

    scope.launch {
        TimeUnit.MILLISECONDS.sleep(1000)
        Integer.parseInt("a")
    }

    scope.launch {
        repeat(5) {
            TimeUnit.MILLISECONDS.sleep(300)
            log(successBySupervisorJob, "second coroutine isActive $isActive")
        }
    }
}
```

```
D  second coroutine isActive true [DefaultDispatcher-worker-1]
D  second coroutine isActive true [DefaultDispatcher-worker-1]
D  second coroutine isActive true [DefaultDispatcher-worker-1]
D  first coroutin exception java.lang.NumberFormatException: For input string: "a" [DefaultDispatcher-worker-2]
D  second coroutine isActive true [DefaultDispatcher-worker-1]
D  second coroutine isActive true [DefaultDispatcher-worker-1]
```