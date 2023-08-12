# suspend fun

`suspend fun` - Это просто маркер того, что данная функция умеет (и должна) работать
с `Continuation`,
чтобы приостановить выполнение корутины не блокируя поток. (маркер для одной операции switch)

Чтобы suspend функция могла приостановить код не блокируя поток, ей нужен `Continuation`,
выполнение которого она возобновит по завершению своей работы.

Чтобы получить `Continuation`, используется функция `suspendCoroutine`

Note: Мы не сможем запустить suspend функцию вне корутины, компилятор выдаст ошибку.
Потому что компилятор знает, что suspend функции нужен будет Continuation, который есть только в
корутине
И вот именно для реализации этого ограничения и используется слово suspend.
Обычная функция, которая не является suspend, не сможет такого приостоновить код.
Она либо заблокирует поток корутины, либо попросит дать ей колбэк.

```kotlin
suspend fun download(url: String): File {
    val networkService = NetworkService()

    // suspendCoroutine - для старых легаси с колбеками
    return suspendCoroutine { continuation ->

        val file = getFileFromCache(url)

        if (file != null) {
            continuation.resume(file)
        } else {
            networkService.download(url, object : NetworkService.Callback {
                override fun onSuccess(result: File) {
                    continuation.resume(result)
                }

                override fun onFailure(error: Exception) {
                    continuation.resumeWithException(error)
                }

            })
        }
    }
}
```
этот код будет приобразован в java code

Функция возвращает `Object`. Она должна вернуть результат работы (файл), если он был передан в
`continuation` сразу из кэша (синхронный режим). Или она должна вернуть `COROUTINE_SUSPENDED`, если в
`continuation` сразу ничего не было передано (асинхронный режим). Для обеспечения этой двухрежимной
логики используется `SafeContinuation`-обертка для нашего `Continuation`.

```
Object download(String url, Continuation continuation) {

        // SafeContinuation - обертка для нашего Continuation.
        // Этот класс нужен для двухрежимной логики - передать результат который запрашивался
        // в билдере либо передать COROUTINE_SUSPENDED - который говорит что корутина еще не закончила выполнение
        SafeContinuation safeContinuation = new SafeContinuation(continuation);

        File file = getFileFromCache(url);

        if (file != null) {
            safeContinuation.resume(file);
        } else {
            networkService.download(url, new NetworkService.Callback() {
                @Override
                public void onSuccess(File result) {
                    safeContinuation.resume(result);
                }

                @Override
                public void onFailure(Exception error) {
                    safeContinuation.resumeWithException(error);
                }
            });
        }
        
        return safeContinuation.getOrThrow();
    }
```

`getOrThrow()` вернет либо запрашиваемый файл, либо `COROUTINE_SUSPENDED`, либо выбросит исключение

1. вернет файл в двух случая:
    * если файл уже был, его не надо загружать и соответственно запускать корутину нет нужды.
    * когда корутина запустилась что бы загрузить файл, при завершении загрузки возвращает файл
2. вернет `COROUTINE_SUSPENDED` - когда в теле билдера прописана `suspend fun` которая
   приостанавливает код (в данном случае для загрузки файла)
3. вернет исключение (ошибку) - если вдруг связь оборвалась или нет интернета.

