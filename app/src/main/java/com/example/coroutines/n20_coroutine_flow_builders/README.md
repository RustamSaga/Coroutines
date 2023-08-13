# Flow, билдеры и простые операторы

Способы создания `Flow`, простые операторы и создание своих операторов.

## Builders

### Основной способ:

```kotlin
val flow = flow {
    emit("a")
    emit("b")
    emit("c")
}
```

### билдеры-обертки asFlow и flowOf

`asFlow`

```kotlin
val flow = listOf("a", "b", "c").asFlow()

//...

public fun <T> Iterable<T>.asFlow(): Flow<T> = flow {
    forEach { value ->
        emit(value)
    }
}
```

`flowOf`

```kotlin
val flow = flowOf("a", "b", "c")
```

    А если у нас есть suspend функция, которая ничего не принимает на вход и возвращает результат:

    suspend fun getData(): Data

    то можно использовать ссылку на нее, чтобы создать Flow:

    val flow = ::getData.asFlow()

## Операторы

1. Intermediate - добавляют в `Flow` различные преобразования данных, но не запускают его.
2. Terminal - запускают Flow и работают с результатом его работы.

### Intermediate

Примеры Intermediate операторов - `map`, `filter`, `take`, `zip`, `combine`, `withIndex`, `scan`,
`debounce`, `distinctUntilChanged`, `drop`, `sample`.

Мы же используем свой оператор.
У нас есть `flow`

```kotlin
val flowStrings = flow {
    emit("abc")
    emit("def")
    emit("ghi")
}
```

Нам нужен оператор который будет переводит строки в верхний реестр
И вместо этого:

```kotlin
flowStrings.collect {
    log(it.toUpperCase())
}
```

Мы используем свой оператор:

```kotlin
fun Flow<String>.toUpperCase(): Flow<String> = flow {
    collect {
        emit(it.toUpperCase())
    }
}
```

Запуск и получение данных с преобразованием (используя свой оператор):

```kotlin
flowString.toUpperCase().collect {
    log(it)
}
```

    Важный момент. Когда вы используете map, filter или создаете свой оператор, не забывайте, что
    используемый в них код преобразования данных будет выполнен в suspend функции collect, которая
    начнет всю работу Flow. Это значит что ваш код не должен быть тяжелым или блокировать поток. Не
    забывайте, что suspend функция может быть запущена и в main потоке.

### Terminal

Примеры Terminal операторов - `collect`, `single`, `reduce`, `count`, `first`, `toList`, `toSet`,
`fold`.

`Intermediate` операторы берут `Flow` и преобразуют его данные и возвращают новый `Flow`. Но они **
не
запускают** `Flow` и не получают результаты его работы.

`Terminal` операторы **запускают** `Flow` так же, как это делает `collect`. Соответственно,
результатом их работы является не `Flow`, а данные, полученные из `Flow` и обработанные определенным
образом.

Пример:

```kotlin
val count = flowOf("a", "b", "c").count() // запускает и возвращает количество элементов flow

// исходники
public suspend fun <T> Flow<T>.count(): Int {
    var i = 0
    collect {
        ++i
    }
    return i
}
```

Пример своего terminal оператора:

для этого `flow` нужно создать оператор который склеет все строки в одину единую

```kotlin
val flowStrings = flow {
    emit("abc")
    emit("def")
    emit("ghi")
}
```

```kotlin

suspend fun Flow<String>.join(): String {
    val sb = StringBuilder()

    collect {
        sb.append(it).append(",")
    }
    return sb.toString()
}
```

Вызов нашего терминала:

```kotlin
val result = flowStrings.join()
```

### Другие операторы

`onEach`, `onStart`, `OnCompletion`, `onEmpty`. Все они - `Intermediate`.
`transform` - рекомендуется к использованию для создания своих `Intermediate` операторов.



    