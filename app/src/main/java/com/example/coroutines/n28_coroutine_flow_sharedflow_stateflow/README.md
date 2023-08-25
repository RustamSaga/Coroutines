# Flow. SharedFlow. StateFlow

У `SharedFlow` есть два отдельных интерфейса:

* `MutableSharedFlow` - для отправителей,
* `SharedFlow` - для получателей (аналогично с MutableLiveData и LiveData).

Для отправителей мы создаем `MutableSharedFlow`:

```kotlin
val _eventBus = MutableSharedFlow<Event>()
```

Методом emit можно отправлять данные:

```kotlin
_eventBus.emit(event) // emit is suspend fun
```

Получатели используют suspend метод collect, чтобы подписаться на получение данных.

```kotlin
eventBus.collect {
// ...
}

```

**`SharedFlow` все время жив и готов принимать данные от отправителей**Если со стороны получателя
необходимо **отменить подписку** и перестать получать данные, то мы просто **отменяем корутину**, в
которой
вызван collect получателя.

## Параметры MutableSharedFlow

### параметр replay. Кэш и буфер

Параметр `replay` включает буфер указанного размера.
`Буфер хранит элементы для медленных получателей, чтобы не задерживать всех остальных. Быстрые
получатели будут получать данные сразу, минуя буфер.`

```kotlin
val _eventBus = MutableSharedFlow<Event>(replay = 3)
```

Кроме буфера, параметр `replay` включает кэш (хранит список последних полученных данных).
Размер кэша = значению `replay`. Кэш обновляется с каждым получением новых данных.
Каждый новый подписчик сразу получит эти значения.

Буфер и кэш не являются одним и тем же. Буфер может быть пустым, если все получатели достаточно
быстрые, но кэш при этом будет полным.

`Т.е. буфер нужен, чтобы компенсировать работу медленных получателей. А кэш нужен, чтобы новые
получатели сразу смогли получить несколько пришедших ранее данных.`

```kotlin
eventBus.replayCache // посмотреть содержимое кэша

_eventBus.resetReplayCache() // отчистка кэша. Может только отправитель (MutableSharedFlow)
```

### Параметр extraBufferCapacity. Увеличение буфера, с учетом replay

```kotlin
// размер буфера будет равен сумме значений этих параметров: 3+4=7. Но размер кэша будет равен 3.
val _eventBus = MutableSharedFlow<Event>(replay = 3, extraBufferCapacity = 4)


// Размер буфера в этом случае будет равен 4. А размер кэша - 0.
val _eventBus = MutableSharedFlow<Event>(extraBufferCapacity = 4)
```

### Параметр onBufferOverflow. задает поведение SharedFlow

Задает поведение `SharedFlow`, когда отправитель шлет данные, но буфер уже заполнен.

Возможны три режима:

1. `SUSPEND` - метод emit на стороне отправителя будет приостанавливать корутину, пока не появятся
   свободные слоты в буфере. Т.е. пока самый медленный получатель не получит значение, данные не
   будут удалены из буфера. Даже быстрым получателям придется ждать новых данных. Данные просто не
   смогут пройти минуя буфер. **Этот режим используется по умолчанию.** Можно избежать приостановки
   используя `tryEmit`(не suspend). Метор вернет true если в буфере есть места, и false если нет.

2. `DROP_OLDEST` - метод emit будет удалять из заполненного буфера наиболее старые элементы и
   добавлять новые. Плюс в том, что методу emit больше не придется ждать. Отправка будет мгновенной.
   С другой стороны, до медленных получателей дойдут не все отправленные данные.

3. `DROP_LATEST` - метод emit не будет отправлять новые значения, если буфер заполнен. Метод emit в
   этом случае также не приостанавливает корутину и отрабатывает быстро. Но все получатели будут
   пропускать новые данные, если особо медленные получатели все обрабатывают старые данные.

### Параметр subscriptionCount. Следит за количеством подписчиков SharedFlow.

Для этого надо подписаться на `StateFlow`, который мы получаем в `subscriptionCount`.

```kotlin
sharedFlow.subscriptionCount
    .map { count -> count > 0 } // преобразует количество подписчиков в boolean флаг (есть подписчики или нет)
    .distinctUntilChanged() // уберет срабатывания, если значение флага не менялось (чтобы при каждом новом подписчике мы не получали значение true)
    .onEach { isActive -> // configure an action
        if (isActive) onActive() else onInactive()
    }
    .launchIn(scope) // launch it

//В итоге метод onActive будет вызываться при появлении первого подписчика, а onInactive - при отключении последнего.
```

### Параметр shareIn. Из Flow сделать SharedFlow.

Пример - `Flow`, который возвращает текущие координаты.

Он подключается к системному сервису, получает от него данные и возвращает нам в виде `Flow`.
Учитывая то как работает `Flow`, каждый кто вызовет `collect` будет создан отдельный `Flow`.
И все эти Flow будут передавать абсолютно одни и те же данные.

С помощью `shareIn` можно сделать так, чтобы был только один `Flow` на всех подписчиков:

```kotlin
val locationSharedFlow = locationFlow.shareIn(
    scope = viewModelScope,
    started = SharingStarted.Lazily,
    replay = 3
)
```

#### Параметры shareIn

1. scope - чтобы запустить корутину нужен scope (ЖЦ SharedFlow = ЖЦ scope)
2. started - когда стартовать.
    * **Eagerly** - работа в Flow стартует сразу при создании SharedFlow, даже если еще нет
      подписчиков. В этом случае данные пойдут в никуда (и в кэш).
    * **Lazily** - стартует при появлении первого подписчика.
    * **WhileSubscribed** - стартует при появлении первого подписчика. При уходе последнего
      подписчика - останавливается. Т.е. отменяется подкапотная корутина, в которой работал
      оригинальный Flow.
      Имеет свои параметры:
        * stopTimeoutMillis - сколько времени ждать до остановки работы с момента ухода последнего
          подписчика
        * replayExpirationMillis - как долго живет replay кэш после остановки работы

3. replay - Задает размер буфера и кэша.
   Обратите внимание, что нельзя явно задать параметры extraBufferCapacity и onBufferOverflow. По
   умолчанию они будут иметь значения (64 - replay) и SUSPEND. Но есть возможность задать эти
   параметры неявно, с помощью оператора buffer перед shareIn:

```kotlin
val locationSharedFlow = locationFlow
    .buffer(capacity = 4, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    .shareIn( /*...*/)
```

В получившемся SharedFlow будут применены параметры из buffer.

**BroadcastChannel**
SharedFlow позиционируется как замена BroadcastChannel.
В [официальной документации](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/-shared-flow/)
можно найти инструкцию по миграции:

- конструктор BroadcastChannel(capacity) меняем на MutableSharedFlow(0,
  extraBufferCapacity=capacity)
- вызовы send и offer меняем соответственно на emit и tryEmit
- код подписчиков меняем на вызов collect

## StateFlow - частный случай SharedFlow

```kotlin
// SharedFlow с поведением StateFlow
val shared = MutableSharedFlow(
    replay = 1,  // буфер и кэш = 1
    onBufferOverflow = BufferOverflow.DROP_OLDEST // новые данные будут заменять старые
)
shared.tryEmit(initialValue) // постим начальное значение
val state = shared.distinctUntilChanged() // отсеивает повторы данных
```

`StateFlow` - всегда хранит в кэше одно (последнее полученное) значение, которое будет получать
каждый новый подписчик. А вновь пришедшее значение всегда будет в буфере заменять старое.

Получателям мы отдаем StateFlow версию:

```kotlin
val progress = _progress.asStateFlow()

// получатель вызывает collect
progress.collect {
// ...
}

// последнее значение отправленное в StateFlow, мы используем value:
val currentProgressValue = progress.value
```

`У SharedFlow был метод replayCache, чтобы посмотреть содержимое кэша. В случае StateFlow этот метод
просто вернет список из одного элемента value.`

#### stateIn - Из обычного Flow можно сделать StateFlow

```kotlin
flow {
// ...
}.stateIn(viewModelScope, Eagerly, 0)
```

#### ConflatedBroadcastChannel

`StateFlow` позиционируется как замена `ConflatedBroadcastChannel`. В официальной документации можно
найти инструкцию по миграции:

- конструктор `ConflatedBroadcastChannel(capacity)` меняем на `MutableStateFlow(initialValue)`
- вызовы send и `offer` меняем на `value`
- код подписчиков меняем на вызов `collect`

# Заключение

Вся разница между `Flow` и `SharedFlow` `(StateFlow)` сводится к разнице между **cold** и **hot**
источниками данных.

`SharedFlow` может иметь несколько получателей (и отправителей) данных, и будет работать в одном
экземпляре. А обычный Flow будет создавать новый экземпляр себя под каждого нового подписчика. В
RxJava похожую разницу имеют Flowable и Subject.

А также одним из мощных достоинств `SharedFlow` является то, что он потокобезопасен. Мы свободно
можем использовать его в разных корутинах.

