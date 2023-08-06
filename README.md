# Coroutine

// ссылки на readme по темам

## Scope

`CoroutineScope` - это то, что позволяет запустить корутину, `scope` бывают разные,
например `viewModelScope`

## Job

`Job` - это то, что управляет состоянием корутин

## CoroutineContext

`CoroutineContext` - это хранилище в `scope` - хранит в себе ссылки на `job` и `dispatcher`,
все они наследуются от coroutineContext.
Кроме этого можно создать свой элемент контеста наследуясь от AbstractCoroutineContextElement

```kotlin
data class UserData(
    val id: Int,
    val name: String,
    val age: Int
) : AbstractCoroutineContextElement(UserData) {
    companion object Key : CoroutineContext.Key<UserData>
}
```

Этот элемент можно передать в контекст при создании `scope` или в билдер

```kotlin
val userData = UserData(1, "BatIr", 30)
val scope = CoroutineScope(Job() + Dispatchers.Main + userData)
scope.launch {
    Log.d("transferElement", "parent scope = ${this.coroutineContext}")

    launch(userData) {
        Log.d("transferElement", "child scope = ${this.coroutineContext}")
        val user = this.coroutineContext[UserData]
        Log.d("transferElement", "user = $user")
    }
}
```

# Dispatcher

Если корутина не находит в своем контексте диспетчер, то она использует диспетчер по умолчанию.
Этот диспетчер представляет собой пул потоков. Количество потоков равно количеству ядер процессора.

Он не подходит для IO операций, но сгодится для интенсивных вычислений.

### Default

Если корутина не находит в своем контексте диспетчер, то она использует диспетчер по умолчанию.
Этот диспетчер представляет собой пул потоков. Количество потоков равно количеству ядер процессора.

Он не подходит для IO операций, но сгодится для интенсивных вычислений.

```kotlin
val scope = CoroutineScope(Dispatchers.Default)

repeat(6) {
    scope.launch {
        log("coroutine $it, start")
        TimeUnit.MILLISECONDS.sleep(100)
        log("coroutine $it, end")
    }
}
```

`logs`

```
D  coroutine 0, start. [DefaultDispatcher-worker-1]
D  coroutine 1, start. [DefaultDispatcher-worker-2]
D  coroutine 2, start. [DefaultDispatcher-worker-4]
D  coroutine 3, start. [DefaultDispatcher-worker-3]
D  coroutine 4, start. [DefaultDispatcher-worker-2]
D  coroutine 5, start. [DefaultDispatcher-worker-4]
D  coroutine 0, end. [DefaultDispatcher-worker-4]
D  coroutine 1, end. [DefaultDispatcher-worker-2]
D  coroutine 3, end. [DefaultDispatcher-worker-3]
D  coroutine 2, end. [DefaultDispatcher-worker-4]
D  coroutine 4, end. [DefaultDispatcher-worker-4]
D  coroutine 5, end. [DefaultDispatcher-worker-3]
```

Корутины 0,2,3 и 1 начали работу. Диспетчер выдал им потоки DefaultDispatcher-worker 1, 3, 4 и 2.
На этом свободные потоки закончились, и корутинам 5 и 4 пришлось ждать, пока потоки освободятся.

### OI

Использует тот же пул потоков, что и диспетчер по умолчанию.
Но его лимит на потоки равен 64 (или числу ядер процессора, если их больше 64).

Этот диспетчер подходит для выполнения IO операций (запросы в сеть, чтение с диска и т.п.).

```kotlin
val scope = CoroutineScope(Dispatchers.IO)
repeat(6) {
    scope.launch {
        log("defaultLOG", "coroutine $it, start.")
        delay(1000)
        log("defaultLOG", "coroutine $it, end.")
    }
}
```

`logs`

```
D  coroutine 0, start. [DefaultDispatcher-worker-1]
D  coroutine 1, start. [DefaultDispatcher-worker-2]
D  coroutine 2, start. [DefaultDispatcher-worker-6]
D  coroutine 3, start. [DefaultDispatcher-worker-3]
D  coroutine 5, start. [DefaultDispatcher-worker-5]
D  coroutine 4, start. [DefaultDispatcher-worker-6]
D  coroutine 0, end. [DefaultDispatcher-worker-6]
D  coroutine 1, end. [DefaultDispatcher-worker-1]
D  coroutine 2, end. [DefaultDispatcher-worker-1]
D  coroutine 3, end. [DefaultDispatcher-worker-9]
D  coroutine 5, end. [DefaultDispatcher-worker-6]
D  coroutine 4, end. [DefaultDispatcher-worker-5]
```

`Все корутины работали одновременно и никому не пришлось ждать.`

#### Почему Default не подходит для IO операций?

IO операции не требуют больших затрат CPU. Мы можем запустить их хоть 10 или 100 одновременно,
чтобы они суммарно выполнились быстрее. И при этом мы не особо нагрузим процессор, потому что
основное время там тратится на ожидание и работу с диском или сетью.

Если мы будем запускать такие операции в Default диспетчере, то мы тем самым ограничим количество
одновременно выполняемых операций. Напомню, что у этого диспетчера количество потоков равно
количеству
ядер процессора. В итоге все IO операции выстроятся в очередь и будут ждать. С точки зрения
производительности это будет очень неэффективно. Их надо параллелить как можно шире.

#### Почему IO не подходит для тяжелых вычислительных операций? ####

Вычислительные операции требуют больших затрат CPU. Если мы паралельно запустим много таких
операций,
то CPU будет перегружен и не сможет выполнять другие задачи. Поэтому IO диспетчер тут не подходит,
ему доступно слишком много потоков.

Когда мы запускаем такие тяжелые операции в Default диспетчере, то они выстраиваются в очередь и
ждут,
а CPU не будет перегружен.

### Executor

Мы можем создать свой диспетчер на базе Executor.

```kotlin
val scope = CoroutineScope(
    Executors.newSingleThreadExecutor().asCoroutineDispatcher()
)

repeat(6) {
    scope.launch {
        log("executorDispatcher", "coroutine $it, start.")
        delay(1000)
        log("executorDispatcher", "coroutine $it, end")
    }
}
```

6 корутин придут к диспетчеру, у которого есть только один поток.
Корутины выполнятся последовательно.

`log`

```
D  coroutine 0, start. [pool-2-thread-1]
D  coroutine 1, start. [pool-2-thread-1]
D  coroutine 2, start. [pool-2-thread-1]
D  coroutine 3, start. [pool-2-thread-1]
D  coroutine 4, start. [pool-2-thread-1]
D  coroutine 5, start. [pool-2-thread-1]
D  coroutine 0, end [pool-2-thread-1]
D  coroutine 1, end [pool-2-thread-1]
D  coroutine 2, end [pool-2-thread-1]
D  coroutine 3, end [pool-2-thread-1]
D  coroutine 4, end [pool-2-thread-1]
D  coroutine 5, end [pool-2-thread-1]
```

### Main

Main диспетчер запустит корутину в основном потоке.

### Unconfined

dispatcher.isDispatchNeeded = false
При старте корутина выполняется в том потоке, где был вызван билдер, который эту корутину создал
и запустил. А при возобновлении выполнения из suspend функции, корутина выполняется в потоке,
который использовался в suspend функции для выполнения фоновой работы. Ведь именно в этом потоке
мы вызываем continuation.resume.

### newSingleThreadContext и newFixedThreadPoolContext

Позволяют вручную задать поток/пул для выполнения корутины
