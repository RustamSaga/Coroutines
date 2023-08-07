# Отмена - это ошибка

**При отмене корутина отменяет все свои дочерние и далее вниз. А наверх отмена не идет.**

Когда мы вызываем метод `cancel`, корутина переходит в состояние `cancelling` (из-за
этого `isActive =
false`). О своей отмене она уведомляет родителя и дочерние корутины. Под капотом
используются те же механизмы уведомления, что и при возникновении ошибок. Но вместо реальной ошибки,
корутина при отмене создает специальную фейковую - CancellationException.

# Отмена в suspend fun

2 типа suspend fun

1. **обыный** - создается оператором `suspendCoroutine`
2. **отменяемый** - создается оператором `suspendCancellableCoroutine`

**Обычные** `suspend` функции не обращают внимания на то, что корутина отменилась. Они работают в
обычном режиме и возвращают результат.

А **отменяемые** `suspend`:

* при старте подписываются на отмену корутины.
* Когда происходит отмена корутины, `suspend` функция получает об этом уведомление
  с `CancellationException` и сразу шлет этот `Exception` в `Continuation`, как результат своей
  работы.
* если `suspend` функция не была обернута в `try-catch` вызов suspend функции в корутине выбрасывает
  CancellationException и работа корутины на этом останавливается.
  Сама suspend функция при этом продолжает работать и даже может потом отправить результат в
  continuation.resume. Но этот результат уже будет никому не интересен.

```kotlin
suspend fun myCancellableSuspendFunction(): String {
    return suspendCancellableCoroutine { continuation ->

      // Чтобы прервать работу suspend функции в случае отмены вызвавшей ее корутины
        continuation.invokeOnCancellation {
            // ...
        }

        // ...

    }
}
```