# Dispatchers

Корутина при запуске ищет в своем контексте **диспетчер**, чтобы получить **поток** для выполнения
работы.

## Default

Если корутина не находит в своем контексте `dispatcher`, то она использует `dispatcher` по
умолчанию. Этот диспетчер представляет собой пул потоков. Количество потоков равно количеству ядер
процессора + виртуальные (если есть).

Он не подходит для `IO` операций, но сгодится для интенсивных вычислений.

Одновременно запускаем все корутины. Они пойдут к диспетчеру, но он сможет предоставить им только 4
потока. Так что кому-то явно придется подождать.

```kotlin
val scope = CoroutineScope(Dispatchers.Default)
repeat(6) {
    scope.launch {
        log("defaultLOG", "coroutine $it, start.")
        delay(1000)
        log("defaultLOG", "coroutine $it, end.")
    }
}
```

log

```
19:56:52.658 coroutine 0, start [DefaultDispatcher-worker-1]
19:56:52.658 coroutine 2, start [DefaultDispatcher-worker-3]
19:56:52.658 coroutine 3, start [DefaultDispatcher-worker-4]
19:56:52.658 coroutine 1, start [DefaultDispatcher-worker-2]
19:56:52.761 coroutine 0, end [DefaultDispatcher-worker-1]
19:56:52.761 coroutine 3, end [DefaultDispatcher-worker-4]
19:56:52.761 coroutine 2, end [DefaultDispatcher-worker-3]
19:56:52.761 coroutine 1, end [DefaultDispatcher-worker-2]
19:56:52.763 coroutine 4, start [DefaultDispatcher-worker-4]
19:56:52.763 coroutine 5, start [DefaultDispatcher-worker-3]
19:56:52.865 coroutine 5, end [DefaultDispatcher-worker-3]
19:56:52.865 coroutine 4, end [DefaultDispatcher-worker-4]
```

## IO

Использует тот же пул потоков, что и диспетчер по умолчанию. Но его лимит на потоки равен 64 (или
числу ядер процессора, если их больше 64).

Этот диспетчер подходит для выполнения IO операций (запросы в сеть, чтение с диска и т.п.).

тот же пример но с IO

```kotlin
val scope = CoroutineScope(Dispatchers.IO)
repeat(6) {
    scope.launch {
        log("coroutine $it, start")
        TimeUnit.MILLISECONDS.sleep(100)
        log("coroutine $it, end")
    }
}
```

log

```
19:59:49.503 coroutine 3, start [DefaultDispatcher-worker-4]
19:59:49.503 coroutine 2, start [DefaultDispatcher-worker-3]
19:59:49.503 coroutine 1, start [DefaultDispatcher-worker-1]
19:59:49.506 coroutine 4, start [DefaultDispatcher-worker-6]
19:59:49.503 coroutine 0, start [DefaultDispatcher-worker-2]
19:59:49.508 coroutine 5, start [DefaultDispatcher-worker-5]
19:59:49.608 coroutine 3, end [DefaultDispatcher-worker-4]
19:59:49.608 coroutine 0, end [DefaultDispatcher-worker-2]
19:59:49.609 coroutine 2, end [DefaultDispatcher-worker-3]
19:59:49.608 coroutine 4, end [DefaultDispatcher-worker-6]
19:59:49.610 coroutine 5, end [DefaultDispatcher-worker-5]
19:59:49.615 coroutine 1, end [DefaultDispatcher-worker-1]
```

Все корутины работали одновременно и никому не пришлось ждать.

## В чем разница между IO и Default?

**Почему Default не подходит для IO операций?**
`IO` операции не требуют больших затрат CPU. Мы можем запустить их хоть 10 или 100 одновременно,
чтобы они суммарно выполнились быстрее. И при этом мы не особо нагрузим процессор, потому что
основное время там тратится на ожидание и работу с дискоим или сетью.

Если мы будем запускать такие операции в `Default` диспетчере, то мы тем самым ограничим количество
одновременно выполняемых операций. Напомню, что у этого диспетчера количество потоков равно
количеству ядер процессора. В итоге все `IO` операции выстроятся в очередь и будут ждать. С точки
зрения производительности это будет очень неэффективно. Их надо параллелить как можно шире.

**Почему IO не подходит для тяжелых вычислительных операций?**
Вычислительные операции требуют больших затрат CPU. Если мы паралельно запустим много таких
операций, то CPU будет перегружен и не сможет выполнять другие задачи. Поэтому `IO` диспетчер тут не
подходит, ему доступно слишком много потоков.

Когда мы запускаем такие тяжелые операции в `Default` диспетчере, то они выстраиваются в очередь и
ждут, а CPU не будет перегружен.

## Executor

Мы можем создать свой диспетчер на базе `Executor`.

```kotlin
val scope = CoroutineScope(
    Executors.newSingleThreadExecutor().asCoroutineDispatcher()
)

repeat(6) {
    scope.launch {
        log("coroutine $it, start")
        TimeUnit.MILLISECONDS.sleep(100)
        log("coroutine $it, end")
    }
}
```

6 корутин придут к диспетчеру, у которого есть только один поток.

Log

```
20:26:39.502 coroutine 0, start [pool-1-thread-1]
20:26:39.604 coroutine 0, end [pool-1-thread-1]
20:26:39.605 coroutine 1, start [pool-1-thread-1]
20:26:39.706 coroutine 1, end [pool-1-thread-1]
20:26:39.707 coroutine 2, start [pool-1-thread-1]
20:26:39.808 coroutine 2, end [pool-1-thread-1]
20:26:39.808 coroutine 3, start [pool-1-thread-1]
20:26:39.911 coroutine 3, end [pool-1-thread-1]
20:26:39.911 coroutine 4, start [pool-1-thread-1]
20:26:40.013 coroutine 4, end [pool-1-thread-1]
20:26:40.013 coroutine 5, start [pool-1-thread-1]
20:26:40.114 coroutine 5, end [pool-1-thread-1]
```

## Main

`Main` диспетчер запустит корутину в основном потоке.

Код внутри `launch` выполнится в `Main` потоке. Когда мы вызываем метод `getData`, поток не будет
заблокирован. Но и код не пойдет дальше, пока данные не будут получены. Suspend функция
приостанавливает выполнение кода, не блокируя поток. Когда она сделает свою работу, выполнение кода
возобновится.
Основной смысл в том, что в корутине, которая выполняется в `Main` потоке, мы можем спокойно писать
обычный (типичный для Activity или фрагментов) код, который работает с UI. Но при этом мы можем в
этом же коде вызывать suspend функции, которые будут асинхронно получать данные с сервера или БД. И
нам не нужны будут колбэки и переключения потоков. Все это скрыто под капотом корутин, а наш код
выглядит чище и лаконичнее.

```kotlin
class MainActivity : AppCompatActivity() {

    private val scope = CoroutineScope(Dispatchers.Main)
    private var job: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<View>(R.id.btnRun).setOnClickListener { onRun() }
    }

    private fun onRun() {
        if (job?.isActive == true) return
        job = scope.launch {
            val data = getData()
            updateUI(data)
        }
    }

    private fun updateUI(data: String) {
        findViewById<TextView>(R.id.label).text = data
    }

    private suspend fun getData(): String =
        suspendCoroutine {
            thread {
                TimeUnit.MILLISECONDS.sleep(3000)
                it.resume("Data")
            }
        }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

}
```

На что здесь надо обратить внимание:

1) Не забываем вызов scope.cancel() в onDestroy. В этом конкретном примере он ни на что не повлияет.
   Но предполагается, что вместо getData с sleep-заглушкой, мы используем реальную suspend функцию,
   которая умеет корректно реагировать на отмену корутины.
2) Перед тем, как запустить корутину, выполняется проверка, что она не в работе в данный момент. Это
   позволяет избежать лишних запусков.

## Как используется диспетчер в корутине

Когда билдер создает `Continuation`, то он оборачивает его в `DispatchedContinuation` и туда же
передает диспетчер корутины. Т.е. обертка `DispatchedContinuation` знает, какой код (`Continuation`)
и в каком потоке (`Dispatcher`) необходимо выполнить.

Именно эту обертку использует билдер, чтобы стартовать выполнение кода корутины Т.е. билдер вызывает
не `continuation.resume` напрямую, а `dispatchedContinuation.resume`. А уже в этом методе диспетчеру
поручается выделить поток и вызвать в нем `continuation.resume`. В результате код `Continuation` (он
же код корутины) выполняется в потоке диспетчера корутины.

С suspend функцией все аналогично. Она при запуске получает не `Continuation`, а
`DispatchedContinuation`. По завершению своей работы она запускает `dispatchedContinuation.resume`,
что приводит к запуску `continuation.resume` в потоке корутины.

```kotlin
fun traceTheChain() {

    val scope = CoroutineScope(Dispatchers.Default)

    scope.launch {
        //Билдер создает dispatchedContinuation и вызывает его метод resume. Диспетчер находит
        // свободный поток (DefaultDispatcher-worker-N) и отправляет туда Continuation на выполнение
        log("tracerTheChain", "start coroutine")

        // Внутри этой функции есть thread -  создает отдельный поток (Thread-N) и уходит туда
        val info = getInfo("tracerTheChain")
        // окончив работу suspend fun вызывает во второй раз continuation.resume
        // Диспетчер отправил его в поток DefaultDispatcher-worker-N. В этом потоке выполнилась оставшаяся часть кода.
        log("tracerTheChain", "end coroutine")

        // Поэтому поток начала работы корутины не совпадает с потоком окончания.
    }

}

suspend fun getInfo(tag: String): String {
    return suspendCoroutine {
        log(tag, "suspend fun, start")
        thread { // выделяется отдельный поток.
            log(tag, "suspend fun, background work")
            TimeUnit.MILLISECONDS.sleep(3000)
            it.resume("Info!")
            //чтобы возобновить выполнение корутины. Диспетчер находит свободный поток (DefaultDispatcher-worker-N) и
            // отправляет туда Continuation на продолжение выполнения.
        }
    }
}
```

От сюда становиться понятно почему код который был запущен в одном потоке в конце (после
преостановок) может продолжиться в другом потоке.
Однако если использовать поток Main, тогда у корутины нет другого выхода как вернуться в этот поток.

## Unconfined

Прежде чем вызвать `dispatchedContinuation.resume`  выполняется проверка - а надо ли тут
использовать диспетчер. Для этого вызывается метод `dispatcher.isDispatchNeeded`. Т.е. диспетчер сам
решает, должен ли он использоваться. Большинство диспетчеров отвечают на этот вопрос утвердительно.
Но есть диспетчер, который добровольно отказывается. Это диспетчер `Unconfined`.

У диспетчера `Unconfined` метод `isDispatchNeeded` возвращает `false`. Это приводит к тому, что при
старте и возобновлении выполнения кода `Continuation` не происходит смены потока.

Т.е. при старте корутина выполняется в том потоке, где был вызван билдер, который эту корутину
создал и запустил. А при возобновлении выполнения из suspend функции, корутина выполняется в потоке,
который использовался в suspend функции для выполнения фоновой работы. Ведь именно в этом потоке мы
вызываем `continuation.resume`.

```kotlin
val scope = CoroutineScope(Dispatchers.Unconfined)
scope.launch() {
    log("start coroutine")
    val data = getData()
    log("end coroutine")
}
```

log

```
start coroutine [main]
suspend function, start [main]
suspend function, background work [Thread-2]
end coroutine [Thread-2]
```

Билдер корутины был вызван в Main потоке. При старте корутины диспетчер `Unconfined` не стал
отправлять ее в отдельный поток. Выполнение продолжилось в Main потоке.

Далее в этом же потоке произошел вызов suspend функции (suspend function, start). Suspend функция
создала отдельный поток (Thread-2) для своей работы и ушла работать туда (suspend function,
background work). Когда она закончила, она в этом же потоке вызвала метод resume, чтобы продолжить
выполнение корутины. Диспетчер `Unconfined` не стал отправлять корутину в отдельный поток, поэтому
корутина продолжила (end coroutine) свое выполнение в потоке `Thread-2`.
 

