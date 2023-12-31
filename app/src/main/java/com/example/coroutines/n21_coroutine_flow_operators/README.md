# Flow. Операторы channelFlow, flowOn, buffer, produceIn.

Так делать нельзя - произойдет ошибка.

```kotlin
val flow = flow {
    coroutineScope {
        launch {
            delay(1000)
            emit(1)
        }

        launch {
            delay(1000)
            emit(2)
        }

        launch {
            delay(1000)
            emit(3)
        }
    }
}
```

1. не потокобезопасно.
2. смена контекста. Мы (как получатель) ожидаем, что блок `collect` выполнится в нашей текущей
   корутине (где мы запускаем метод `collect()`), а он выполнится неизвестно в какой корутине,
   которую запустил блок `flow`. Запуская метод `collect()` в корутине с `Main` потоком мы ожидаем,
   что и блок `collect` будет выполнен в этой же корутине. А если блок flow вызовет `emit()` (а
   следовательно и блок `collect`) из какой-то своей корутины (например, с `IO` диспетчером), то наш
   код по выводу данных на экран не сработает, т.к. будет выполнен не в `Main` потоке.

   В блоке flow нельзя вызывать emit из корутин. Для решения этой проблемы есть специальный билдер -
   channelFlow.

## channelFlow

Позволит обойти проблему описанную выше:
Можно написать пример как работает `channelFlow`

```kotlin
flow {
    coroutineScope {

        val channel = produce<Int> {
            launch {
                delay(1000)
                send(1)
            }
            launch {
                delay(1000)
                send(2)
            }
            launch {
                delay(1000)
                send(3)
            }
        }
        channel.consumeEach {
            emit(it)
        }
    }
}
```

Когда мы запустим его методом `collect()`, данные будут создаваться в отдельной корутине и с помощью
канала вернутся в нашу текущую корутину.

Таким образом работает `channelFlow`

```kotlin
val flow = channelFlow {
    launch {
        delay(1000)
        send(1)
    }
    launch {
        delay(1000)
        send(2)
    }
    launch {
        delay(1000)
        send(3)
    }
}
```

## callbackFlow

В корутине мы подписываемся на какой-то колбэк. И мы уже не определяем, когда именно придут данные.
А значит, корутина не должна пока заканчиваться и закрывать канал. Она должна ждать, пока канал не
будет закрыт. Для этого используется метод `awaitClose`. Корутина не завершится, пока канал не будет
закрыт.

А канал будет закрыт, когда получатель данных отпишется от `Flow` (который является результатом
билдера `callbackFlow`). Это приведет к срабатыванию `awaitClose` (в котором мы отписываемся от
колбэка) и корутина завершит свою работу.

`callbackFlow` отличается от `channelFlow` тем, что он по завершении кода корутины проверяет, что
канал закрыт, т.е. что мы не забыли вызвать `awaitClose` в корутине. Если мы забудем, то сразу же
получим ошибку. А в channelFlow мы получим ошибку, только когда колбэк решит что-нить отправить.

```kotlin
fun flowFrom(api: CallbackBasedApi): Flow<T> = callbackFlow {
    val callback = object : Callback { // Implementation of some callback interface
        override fun onNextValue(value: T) {
            // To avoid blocking you can configure channel capacity using
            // either buffer(Channel.CONFLATED) or buffer(Channel.UNLIMITED) to avoid overfill
            trySendBlocking(value)
                .onFailure { throwable ->
                    // Downstream has been cancelled or failed, can log here
                }
        }
        override fun onApiError(cause: Throwable) {
            cancel(CancellationException("API Error", cause))
        }
        override fun onCompleted() = channel.close()
    }
    api.register(callback)
    /*
     * Suspends until either 'onCompleted'/'onApiError' from the callback is invoked
     * or flow collector is cancelled (e.g. by 'take(1)' or because a collector's coroutine was cancelled).
     * In both cases, callback will be properly unregistered.
     */
        awaitClose { api.unregister(callback) }
    }
```

[Подробнее]()

## flowOn (задает контекст и запускает flow)

Оператор `channelFlow` позволял нам самим создать `Flow`, чтобы выполнять его код в корутине с
нужным нам контекстом. Но что, если нам дали уже готовый `Flow`. Как нам попросить выполнить его
блок `flow` в отдельной корутине?

Можно написать свой код:

```kotlin
flow {
    coroutineScope {
        // передаем нужный контекст
        val channel = produce<Int>(neededContext) {
            flow.collect {
                send(it)
            }
        }
        channel.consumeEach {
            emit(it)
        }
    }
}
```

или использовать `flowOn`

```kotlin
val ioFlow = flow.flowOn(Dispatchers.IO)
```

Вызов `ioFlow.collect()` приведет к тому, что `flow` начнет работу в `IO` потоке. А все данные мы
будем получать в текущей корутине.

`flowOn` можно использовать в любом месте после операторов

```kotlin
    flow {
    // ... IO thread
}
    .map {
        // ... IO thread
    }
    .flowOn(Dispatchers.IO)
    .onEach {
        // ... Main thread
    }.flowOn(Dispatchers.Main)
```

## buffer

Копит данные в буфере, если не успевает получатель.
Схема такая же как с `flowOn`. Отличаются только цели использования `produce`

```kotlin
flow {
    coroutineScope {
        // передаем нужный размер буфера
        val channel = produce<Int>(capacity) {
            flow.collect {
                send(it)
            }
        }
        channel.consumeEach {
            emit(it)
        }
    }
}   
```

      Оператор buffer, как и оператор flowOn, запустит новую корутину для запуска тех Flow, которые
      находятся перед ним в цепочке операторов. Потому что он использует produce. Но в отличие от flowOn
      он не будет менять контекст. Т.е. корутина будет новая, но диспетчер останется прежним.

Тот же код используя `buffer`

```kotlin
val flow = flow {
    //...
}.buffer(1)
```

Операторы `buffer` и `flowOn` можно комбинировать. При этом оператор `flowOn` не будет создавать
новый `produce`. Он увидит, что прямо перед ним находится `buffer` и просто добавит свой контекст в
вызов `produce` внутри `buffer`.

```kotlin
val flow = flow {
// ...
}
    .buffer(5)
    .flowOn(Dispatchers.IO)
```

## produceIn

Оператор `produceIn` тоже использует `produce`, чтобы отправить данный ему `Flow` работать с каналом
в новой корутине. Но при этом он сам не будет получать данные из канала. Он вернет этот канал нам.

Под копотом:

```kotlin
val channel = scope.produce<Int> {
    flow.collect {
        send(it)
    }
}
```

Никакая Flow обертка не создается. Вместо этого мы получаем канал. Таким образом Flow конвертируется
в канал.

```kotlin
val channel = flow {
    // emit
}.produceIn(scope)
```

Три важных момента:

1. В `produceIn` надо передавать `scope`, который будет использован для запуска `produce`.
2. Получившийся канал (в отличие от обычного Flow) не является Cold. Т.е. он начнет работу сразу же,
   как мы его создадим.
3. Если нам надо указать свой контекст или параметры буфера, мы используем операторы flowOn и
   buffer:

```kotlin
val channel = flow {
    // emit
}
    .buffer(5)
    .flowOn(Dispatchers.IO)
    .produceIn(scope)
```

