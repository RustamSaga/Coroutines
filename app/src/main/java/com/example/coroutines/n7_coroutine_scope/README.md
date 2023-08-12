# Scope

`Scope` - объект без которого корутину не запустить.
`scope` - это родитель для всех корутин. Когда мы отменяем scope, мы отменяем все его дочерние
корутины.
`Scope` нужен для создания класса `Job`(который будет родителем), чтобы создать корутину.
Он (в своем контексте) содержит `Job`. Этот `Job` будет являться родителем для `Job`-ов корутин,
которые мы создаем, вызывая scope.launch.

`Scope` объект можно создать самостоятельно.
Мы будем определять, когда следует отменять его и все его корутины.

```kotlin
val scope = CoroutineScope(Job())
val scope1 = CoroutineScope(Dispatchers.Default)
val scope2 = CoroutineScope(Job() + Dispatchers.Default)
// and other
```

Если бы выполнялась долгая работа, то `scope` помог бы нам ограничить
время жизни этих корутин. Например, мы решаем, что при закрытии экрана все
корутины должны отмениться. Для этого помещаем вызов `scope.cancel` в метод `Activity.onDestroy`

```kotlin
override fun onDestroy() {
    super.onDestroy()
    scope.cancel()
}
```

При вызове `scope.cansel()` отменяются все подписанные на него `Job`-ы корутин.
Соответственно, если не вызвать `scope.cancel`, то корутины этого `scope` не отменятся.
Поэтому не теряйте этот вызов, если вам необходимо отменять корутины.

        - отмена одной корутины - `job.cancel()`
        - отмена всех корутин, которые были вызваны этим scope - `scope.cancel()`

в родительской корутине существует свой `scope`. И это не тот же самый `scope`,
который мы использовали для запуска этой родительской корутины. Каждая корутина
создает внутри себя свой `scope`, чтобы иметь возможность запускать дочерние корутины.
Именно этот `scope` и доступен нам как `this` в блоке кода корутины.

```kotlin
scope.launch {
    // parent coroutine code block
    this.launch {
        // child coroutine code block
    }
}

// or
scope.launch {
    // parent coroutine code block
    launch {
        // child coroutine code block
    }
}
```

`Job` наследует интерфейс `CoroutineScope` и хранит ссылку на `Context`.
Т.к. `Context` должен содержать `Job`, то `Job` просто помещает в `Context` ссылку на себя.
Получается, что `Job` является `scope`, и сам же выступает в качестве `Job` этого scope

```kotlin
val job = scope.launch {
    Log.d(TAG, "scope = $this")
}
Log.d(TAG, "job = $job")
```

Вы увидите, что `job`, возвращаемый билдером, и `scope` в его корутине - это один и тот же объект.