# coroutineScope, supervisorScope, withContext, runBlocking.

## CoroutineScope

2 основных назначения:

1. Создание `scope` (позволяет ограничить распространение ошибок)
2. Позволяет выносить вызов корутин в отдельные (suspend) функции

// 1.2 вызовит ошибку и тогда все корутины отменятся

```kotlin
scope.launch(CoroutineName("1")) {

    launch(CoroutineName("1.1")) {
        //...
    }
    launch(CoroutineName("1.2")) {
        // exception
    }
    launch(CoroutineName("1.3")) {
        //...
    }
    launch(CoroutineName("1.4")) {
        //...
    }
}
```

// это можно обойти (если необходимо), образуя связь `Coroutine_1 > ScopeCoroutine > Coroutine_1_2`

// также нужно обернуть `coroutineScope` в `try-catch`, иначе случиться краш.

```kotlin
scope.launch(CoroutineName("1")) {

    try {
        coroutineScope { // this: new ScopeCoroutine
            launch(CoroutineName("1.1")) {
                //...
            }
            launch(CoroutineName("1.2")) {
                // exception
            }
        }
    } catch (e: Exception) {
        //...
    }


    launch(CoroutineName("1.3")) {
        //...
    }
    launch(CoroutineName("1.4")) {
        //...
    }
}
```

также, `coroutineScope` можем вынести в отдельную suspend fun

```kotlin
suspend fun coroutineException() {
    coroutineScope { // this: new ScopeCoroutine
        launch(CoroutineName("1.1")) {
            //...
        }
        launch(CoroutineName("1.2")) {
            // exception
        }
    }
}

////////////////////////////////

scope.launch {
    try {
        coroutineException()
    } catch (e: Exception) {
        //...
    }

    launch(CoroutineName("1.3")) {
        //...
    }
    launch(CoroutineName("1.4")) {
        //...
    }
}
```

### Заметки

* вызвав `scope.cancel`, мы каскадно отменим все корутины, в том числе и те, которые находятся
  внутри
  `coroutineScope`.
* Обработчики `CoroutineExceptionHandler` не работают внутри `coroutineScope`, потому
  что `ScopeCoroutine`
  принимает ошибку от корутины 1.2 и говорит, что сможет ее обработать
* `coroutineScope` - может вернуть нужное значение

```kotlin
val result = coroutineScope {
    // ...
    "result value"
}
```

* приостановка корутины
  т.к `coroutineScope` это suspend fun - то ее вызов приостановит поток текущей корутины.
  Если необходимо можно обойти это следующим образом:

```kotlin
launch {
    coroutineScope {
        // ...
    }
}

// or

async {
    coroutineScope {
        //...
    }
}
```

**Не забывайте при этом оборачивать `coroutineScope` в `try-catch`, если хотите, чтобы ошибка не
распространялась вверх.**

* Потоки
  Код `coroutineScope` выполняется в том же потоке, что и вызвавшая его корутина. А вот продолжить
  вызвавшую корутину он может в другом потоке (но в том же диспетчере)

## supervisorScope

Все тоже самое как в `coroutineScope` с отличием - не принимает ошибку от дочерних.

Однако вместе с `supervisorScope` имеет смысл использовать `CoroutineExceptionHandler` внутри supervisorScope. В этом случае
ошибка попадет в этот обработчик и на этом все закончится. Функция supervisorScope не выбросит
исключение и не отменит остальные корутины внутри себя (т.е. корутину 1_1 в примере выше).