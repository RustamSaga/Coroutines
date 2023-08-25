# **Обработка исключений вложенных корутин**

```kotlin
scope.launch {
    launch {
        launch { }
        launch { }
    }
}
```

## try-catch

неправильно:

```kotlin
scope.launch { // coroutine 1
    try {
        launch { Integer.parseInt("a") } // coroutine 1.1
    } catch (e: Exception) {
    }
    launch {}   // coroutine 1.2
}
```

правильно:

```kotlin
scope.launch { // coroutine 1

    launch {  // coroutine 1.1
        try {
            Integer.parseInt("a")
        } catch (e: Exception) {
        }
    }
    launch {}   // coroutine 1.2
}
```

### Передача ошибки родителю

Дочерняя корутина всегда спрашивает родительскую, сможет ли родитель обработать ошибку и так по
цепочке, пока не дойдет до `scope`. Scope скажет что не сможет.
При этом, каждый радитель который принял от дочернего ошибку будет отменяться, в итоге все корутины
перестанут работать.

проверим

```kotlin
private fun coroutineHandlerTest() {
    val handler = CoroutineExceptionHandler { context, exception ->
        log(tag, "$exception was handled in Coroutine_${context[CoroutineName]?.name}")
    }

    val scope = CoroutineScope(Job() + Dispatchers.Default + handler)

    scope.launch(CoroutineName("1")) {

        launch(CoroutineName("1_1")) {
            TimeUnit.MILLISECONDS.sleep(500)
            Integer.parseInt("a")
        }

        launch(CoroutineName("1_2")) {
            TimeUnit.MILLISECONDS.sleep(1000)
        }
    }

    scope.launch(CoroutineName("2")) {

        launch(CoroutineName("2_1")) {
            TimeUnit.MILLISECONDS.sleep(1000)
        }

        launch(CoroutineName("2_2")) {
            TimeUnit.MILLISECONDS.sleep(1000)
        }
    }
}
```

`CoroutineName` - это элемент контекста. Помещаем в него имя корутины и передаем в `launch` билдеры.
Теперь в каждой корутине мы сможем из контекста достать этот элемент и узнать, какая это корутина. В
обработчике CoroutineExceptionHandler мы на вход получаем ошибку и контекст корутины, которая
передала ошибку в этот обработчик. Мы из контекста достаем `CoroutineName` и с его помощью узнаем,
какая корутина обработала ошибку. Обработчик мы помещаем в `scope`, поэтому он будет передан во все
корутины. В `Coroutine_1_1` выбросим исключение через 500 мсек. Остальные корутины просто ничего не
делают 1000 мсек.

вывод:

```
myTag              D  java.lang.NumberFormatException: For input string: "a" was handled in Coroutine_1 [DefaultDispatcher-worker-1]
```

Корутина 1_1, в которой произошла ошибка, сама не стала его обрабатывать, а передала родителю -
корутине 1

### SupervisorJob - то как мы использовали его раньше не поможет в этом случае.

```kotlin
scope.launch {
    launch { launch {} launch { ошибка } } // перестанет работать все
    launch { launch {} launch {} } // эта останется
    launch { launch {} launch {} } // эта останется

}
```

либо использовать кастыль
parentJob > supervisorJob > childJob

```kotlin
scope.launch { // parent coroutine

    launch(SupervisorJob(coroutineContext[Job])) { // child coroutine

    }
}
```

так неправильно - потеряется связь между радителем и ребенком
supervisorJob > childJob
А это уже нарушение принципа structured concurrency и связи между корутинами.

```kotlin
// don't do this 
scope.launch { // parent coroutine
    launch(SupervisorJob()) { // child coroutine

    }
}
```
    