# launch and async

## Launch

Билдер в котором пришется код и который запускает этот код.
Имеет ряд возможностей:

* приостановить родительскую корутину
* запустить отложенно
* остановить

обычная корутина в корутине:

```kotlin
scope.launch {
    log(tag, "launch coroutine, start")

    launch {
        log(tag, "child coroutine, start")
        delay(1000)
        log(tag, "child coroutine, end")
    }

    log(tag, "parent coroutine, end")
}
```

Хоть родительская корутина и выполнила сразу же весь свой код,
но ее статус поменяется на Завершена только когда выполнится дочерняя корутина.
Потому что родительская корутина подписывается на дочерние и ждет их завершения,
прежде чем официально завершиться. Т.е. то, что корутина выполнила свой код,
может и не означать, что она имеет статус Завершена.

### функция приостановления  join()

Что бы приостоновит выполнение родительской корутины, нужно использовать метод join()

```kotlin
scope.launch {
    log(tag, "method - launch coroutine, start")

    val job = launch {
        log(tag, "method - child coroutine, start")
        delay(3000)
        log(tag, "method - child coroutine, end")
    }
    val job2 = launch {
        log(tag, "method - child coroutine, start")
        delay(2000)
        log(tag, "method - child coroutine, end")
    }
    log(tag, "parent coroutine, wait until child completes")
    job.join()
    job2.join()

    log(tag, "method - parent coroutine, end")
}
```

### отложенный запуск

Для отложенного старата необходимо использовать константу в билдере - start = CoroutineStart.LAZY

```kotlin
 Button(onClick = {
    log(tag, "COROUTINE IS CREATED")
    job = scope.launch(start = CoroutineStart.LAZY) {
        log(tag, "COROUTINE IS STARTED")
        delay(2000)
        log(tag, "COROUTINE, END")
    }

    log(tag, "COROUTINE, END")

}) {
    Text(text = "CREATE LAZY")
}
Button(onClick = {
    log(tag, "COROUTINE, RUN BUTTON")
    job.start()
    log(tag, "COROUTINE, END (RUN BUTTON)")

}) {
    Text(text = "START LAZY")
}
```

## Async

Билдер async похож на `launch`. Он также создает и стартует корутину. Но если `launch` корутина
делала свою работу и ничего не возвращала в ответ, то async корутина может вернуть результат своей
работы.

Вместо `Job` мы получаем `Deferred`. Это наследник `Job`, поэтому имеет все те же методы. Но
дополнительно
у него есть методы для получения результата работы корутины. Один из них - метод `await`.

```kotlin
scope.launch {
    log(tag, "COROUTINE, RUN")

    val deferred = async {
        log(tag, "COROUTINE IS STARTED")
        delay(2000)
        log(tag, "COROUTINE, END")

        "Async result"
    }

    log(tag, "parent coroutine, wait until child returns result")
    val result = deferred.await() // приостанавливает код и выдает результат
    log(tag, "parent coroutine, child returns: $result")

    log(tag, "parent coroutine, end")

}
```

## Параллельная работа

```kotlin
scope.launch {
    log(tag, "COROUTINE, RUN")

    // время суммируется = 1500+3500 = 5000 (5 сек на время выполнения)
//            val data1 = getData(1500, "data1")
//            val data2 = getData(3500, "data2")

    // выполение праллельно, общее время = 3500 (3,5 сек)
    val data1 = async { getData(1500, "data1") }
    val data2 = async { getData(3500, "data2") }

    log(tag, "parent coroutine, wait until child returns result")
    val result = "${data1.await()}, ${data2.await()}"
    log(tag, "parent coroutine, child returns: $result")

    log(tag, "parent coroutine, end")

}
```

## Lazy
`async` корутину также можно запускать в режиме `Lazy`. Метод `await` стартует ее выполнение, или 
`start` - чтобы просто запустить, и выполнить код до метода `await`.

```kotlin
suspend fun main() = coroutineScope{
 
    // корутина создана, но не запущена
    val sum = async(start = CoroutineStart.LAZY){ sum(1, 2)}
 
    delay(1000L)
    println("Actions after the coroutine creation")
    sum.start()                      // запуск корутины
    // some code
    println("sum: ${sum.await()}")   // получаем результат
}
fun sum(a: Int, b: Int) : Int{
    println("Coroutine has started")
    return a + b
}
```