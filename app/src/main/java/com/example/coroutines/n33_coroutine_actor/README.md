# Оператор Actor

Это корутина с каналом внутри.

Т.е. при вызове оператора actor стартует корутина и создается канал, доступный внутри этой корутины.
Этот канал мы получаем наружу, как результат вызова actor, и можем отправлять в него данные. А
корутина будет эти данные получать и что-то с ними делать.

В итоге получается корутина, в которую мы можем потокобезопасно передавать данные посредством
канала.

*** 
Пример - защита от множественных нажатий. У нас есть кнопка, по нажатию на которую, мы в корутине
грузим данные с сервера. Нам надо сделать так, чтобы каждое нажатие не приводило к запуску новой
корутины с загрузкой данных, пока текущая корутина не завершилась.

```kotlin
private val actorChannel =
    viewModelScope.actor<Unit> { // Unit, потому что в его канал нам не надо передавать никаких реальных данных, только события кликов.
        for (click in channel) {
            val data = apiService.getData() // фейк-api - ждет 1000 мсек и возвращает строку
            log("data = $data")
        }
    }
```

При нажатие на кнопку мы вызываем его метод

```kotlin
fun onButtonClick() {
    log("click")
    actorChannel.trySend(Unit) // метод trySend просто выполнится, ничего не отправив, если получатель не готов
    // Именно это и дает нам защиту от повторных нажатий
    // Клики будут игнорироваться пока загружаются данные.
}
```

```
7:07:54.115 click
17:07:54.754 click
17:07:55.116 data = <some data>

17:08:00.706 click
17:08:01.030 click
17:08:01.239 click
17:08:01.475 click
17:08:01.699 click
17:08:01.707 data = <some data>

17:08:01.915 click
17:08:02.074 click
17:08:02.232 click
17:08:02.917 data = <some data>

17:08:05.599 click
17:08:05.772 click
17:08:05.923 click
17:08:06.081 click
17:08:06.240 click
17:08:06.399 click
17:08:06.600 data = <some data>```

```

# Как остановить работу actor

```kotlin
actorchannel.close() // Корутина доработает текущую итерацию for до конца и завершится.

// or

scope.close()
```

# Параметры

* `context` и `start` - аналогичны одноименным параметрам у launch

* `capacity` - аналогичен этому же параметру у каналов, т.е. устанавливает размер буфера канала.

* `onCompletion` - сюда можно передать блок кода, который будет выполнен по завершению работы actor.
  Сработает в обоих выше рассмотренных случаях остановки.
