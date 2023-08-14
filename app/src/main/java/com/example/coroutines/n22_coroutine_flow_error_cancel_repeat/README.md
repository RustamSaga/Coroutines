# Flow. Ошибка, отмена, повтор.

Что случиться если возникнет ошибка в нутри `flow`?
Произойдет тоже что и при обычной ошибке в suspend fun. Если ни кто не ловит, то произойдет креш.

```kotlin
var flow = flow {
    delay(500)
    emit("1")
    delay(500)
    emit("2")

    val a = 1 / 0

    delay(500)
    emit("3")
    delay(500)
    emit("4")
}
```

Как вариант можно `try-catch`

```kotlin
launch {
    try {
        flow.collect {
            log("collect $it")
        }
    } catch (e: Exception) {
        log("exception $e")
    }
}
```

Либо использовать специальные операторы

## catch - аналог стандартного try-catch

```kotlin
launch {
    flow
        .catch { log("catch $it") }
        .collect {
            log("collect $it")
        }
}
```

Однако, если ошибка возникла в операторе, который в цепочке находится после `catch`, то она не
будет поймана этим `catch`.

Также можно использовать несколько операторов `cacth`

```kotlin
launch {
    flow
        .catch { log("catch $it") } // ловит ошибки из flow
        .map { it.toInt() }
        .catch { log("catch2 $it") } // довит ошибки из map
        .collect {
            log("collect $it")
        }
}
```

Также в блоке кода catch нам доступен метод `emit()`. Т.е. мы можем отправить какие-то свои данные
получателю в случае возникновения ошибки.

## retry

Перезапускает `flow` если возникла ошибка, работает точно также как `cacth` (отношение код)

```kotlin
launch {
    flow
        .retry(2) // в параметры принимает количество попыток, по умолчанию = Long.MAX_VALUE
//        .catch { log("catch2 $it") } // если нужно поймать ошибку
        .collect {
            log("collect $it")
        }
}
```

Если все попытки закончатся ошибкой, то `retry` просто ретранслирует эту ошибку дальше. Поэтому
добавляйте `catch` после `retry` если хотите ошибку в итоге поймать.

Также в параметры принимает блок кода - в которую оператор передает пойманную ошибку.
А от нас он ждет Boolean ответ, который определяет, надо ли пытаться перезапускать Flow.

```kotlin
launch {
    flow
        .retry(2) {
            /*
                Если это не ArithmeticException, то вернется true - надо перезапускать Flow.
                А если ошибка-ArithmeticException, то вернется false-Flow не будет перезапущен,
                а ошибка пойдет дальше, и ее надо будет ловить.
            */
            it !is ArithmeticException
        }
        .collect {
            log("collect $it")
        }
}
```

```kotlin
launch {
    flow
        .retry(2) {
            if (it is NetworkErrorException) {
                delay(5000) // если нужна пауза прежде чем перезапустить.
                true
            }
            false
        }
        .collect {
            log("collect $it")
        }
}
```

## retryWhen

Оператор `retryWhen` работает примерно так же, как и `retry`. Но он не принимает на вход количество
попыток. Вместо этого он дает нам в блок кода номер текущей попытки и ошибку. А нам уже надо решить

- возвращать `true` или `false`.

```kotlin
launch {
    flow
        .retryWhen { cause, attempt ->
            cause is NetworkErrorException && attempt < 5
            // emit(...) - если необходимо 
        }
        .collect {
            log("collect $it")
        }
}
```

## Отмена

Метод на стороне получателя не относится непосредственно к Flow. Это стандартный метод отмены
корутины.

```kotlin
launch {
    flow
        .collect {
            if (it == "3") cancel() // относится к корутине, отменит корутину
            log("collect $it")
        }
}
```

Метод `emit` в блоке `flow` всегда проверяет это и выбросит `CancellationException` (как стандартная
`cancellable suspend` функция).

Если в нашей корутине есть код после метода `collect` и нам надо, чтобы он был выполнен даже после
вызова `cancel`, то вызов `collect` надо оборачивать в `try-catch`. И в последующем коде учитывайте, что
корутина будет находиться в отмененном состоянии. Т.е. isActive будет возвращать false, а
cancellable suspend функции (типа delay) будут выбрасывать CancellationException исключение.

На заметку: `asFlow` не будет сам проверять корутину на отмену. Поэтому нужно его научить.
```kotlin
(1..3).asFlow().cancellable() // добавить метод cancellable()
```