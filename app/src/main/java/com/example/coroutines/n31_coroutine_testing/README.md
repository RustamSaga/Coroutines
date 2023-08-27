# Тестирование

## runBlocking. Нужен что бы правильно закончить coroutines. Иначе тест завершитсья прежде coroutines

Если нам надо протестировать Flow, то мы будем вызывать метод Flow.collect, чтобы получить данные и
проверить их. И т.к. collect - это suspend функция, то для ее запуска в тестах мы также будем
использовать runBlocking.

## runTest - "перемотка" времени получения данных

Время работы теста этой функции в runBlocking займет 5 секунд. А в runTest - миллисекунды

```kotlin
suspend fun someMethod(): String {
    delay(5000)
    return "abc"
}
```

### runCurrent

У нас есть код

```kotlin
val loading = MutableStateFlow(false)

fun fetchData() {
    loading.value = true // loading получил true

    viewModelScope.launch() {   // диспетчер отправил корутину в scheduler
        data = serviceApi.getData()
        loading.value = false
    }
} // метод fetchData завершился
```

Под капотом `viewModelScope` работает диспетчер `Main`. В Android такой диспетчер есть, а вот в
unit-тестах - нет. Поэтому тест при запуске будет выдавать ошибку:

`Exception in thread "Test worker @coroutine#1" java.lang.IllegalStateException: Module with the Main
dispatcher had failed to initialize. For tests Dispatchers.setMain from kotlinx-coroutines-test
module can be used`

```kotlin
private val dispatcher = StandardTestDispatcher()

@Before
fun setUp() {
    Dispatchers.setMain(dispatcher) //чтобы глобально заменить Main диспетчер на время запуска теста
}

@After
fun tearDown() {
    Dispatchers.resetMain() //  и сбрасываем после
}
```

Затем вызываем

```kotlin
@Test
fun test(): Unit = runTest {
        myViewModel.fetchData()
        assertFalse(myViewModel.loading.value)
    }
```

Но это не сработает - т.к тестовый диспетчер `StandardTestDispatcher` сам не запускает корутины.
Он их отправляет в scheduler, который нужно явно попросить запустить корутину.

Поэтому мы просим его запустить корутину

```kotlin
@Test
fun test() = runTest {
        myViewModel.fetchData()
        testScheduler.runCurrent()  // запуск корутины
        assertFalse(myViewModel.loading.value)
    }
```

### advanceTimeBy

Если вызов delay - scheduler дает возможность автоматической и ручной перемотки времени.

```kotlin
val showDialog = MutableStateFlow(false)

fun showAndHideDialog() {
    showDialog.value = false

    viewModelScope.launch {
        delay(1000)
        showDialog.value = true
        delay(3000)
        showDialog.value = false
    }
}
```

Test code:

```kotlin
@Test
fun test() = runTest {
        myViewModel.showAndHideDialog() // поместит false в showDialog и отправит корутину в scheduler.
        assertFalse(myViewModel.showDialog.value)

        testScheduler.advanceTimeBy(1001) // перемотает вирутальное время на 1001 мсек, чтобы сработал код после delay(1000), который поместит true в showDialog.
        assertTrue(myViewModel.showDialog.value)

        testScheduler.advanceTimeBy(3000) // перематываем время на 3000, чтобы пропустить delay(3000) и поймать false в loading
        assertFalse(myViewModel.loading.value)
    }
```

Т.е. вызвав `advanceTimeBy(1001)` мы переместились в милисекунду 1001. Далее вызвав
`advanceTimeBy(3000)` мы переместились в милисекунду 4001. Этого хватило, чтобы дойти до кода,
который был вызван после `delay(1000)` + `delay(3000)`.

* `currentTime` - Оно отображает виртуальное время - пример кода выше - 4001 мсек. (1001+3000)
* почему не `runCurrent` а `advanceUntilIdle` - Потому что `runCurrent` просто просит `scheduler`
  запустить отложенные корутины. Но когда в корутине есть delay, то корутина выполняет весь код до
  этого delay, а затем снова отправляется в scheduler на запуск, чтобы продолжить работу. И нам
  снова надо просить scheduler запустить ее. И при этом надо еще и объяснять, что уже прошло 1000
  мсек и вот теперь уже точно пора. runCurrent так не умеет. Поэтому использовали advanceTimeBy.

  Кроме advanceTimeBy есть метод advanceUntilIdle. Он будет пинать scheduler, чтобы тот снова и
  снова запускал корутину, пока в ней не пройдут все delay.
* `UnconfinedTestDispatcher` - в отличие от `StandardTestDispatcher`, не будет ничего откладывать на
  потом, а выполнит код корутины в момент вызова launch.
* `TestScope` - Это может быть полезным, когда объекту, который мы тестируем, необходим scope, чтобы
  внутри себя запускать корутины. Мы можем создать testScope предоставить его объекту, и
  использовать при запуске runTest.
```kotlin
val scope = TestScope()
val myClass = MyClass(scope)
 
@Test
fun test() = scope.runTest {
   myClass.someMethod()
}
// весь код будет выполняться в рамках одного scope, одним диспетчером.
```

* Dispatcher Inject - диспетчеры надо инджектить, а не использовать напрямую.

