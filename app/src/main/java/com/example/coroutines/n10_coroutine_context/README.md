# CoroutineContext

`CoroutineContext` чем-то похож на `Map`. Он хранит в себе элементы, и их можно достать по ключу.

В качестве примера получения элемента из `CoroutineContext` можно привести исходники метода cancel
у `scope`:

```kotlin
public fun CoroutineScope.cancel(cause: CancellationException? = null) {
    val job = coroutineContext[Job]
        ?: error("Scope cannot be cancelled because it does not have a job: $this")
    job.cancel(cause)
}
```

Из контекста `scope` здесь достается `Job` и вызывается его метод `cancel`.

Чаще всего в coroutineContext храняться `job` & `dispatcher`, возможно поместить и другие объекты.
Однако поместить в `CoroutineContext` можно только объект специального
класса `AbstractCoroutineContextElement`. Об этом ниже.

## `CoroutineContext` нужен при создании `scope`

Создание `coroutineContext`а:

```kotlin
val context = Job() + Dispatchers.Default
val scope = CoroutineScope(context)

// or
val scope = CoroutineScope(Job() + Dispatchers.Default)
```

## CoroutineContext из одного элемента

```kotlin
private val scope = CoroutineScope(Job())
// or
private val scope = CoroutineScope(Dispatchers.Default)
```

Вместо `Context` мы передавали `Job`. Это работает, т.к. любой элемент, который можно поместить в
`context`, сам по себе также является `context`-ом. Т.е. `Job` - это просто `Context` с одним
элементом.

Аналогично и `Dispatchers.Default` - это Context с одним элементом.

## Job для scope будет создан, если его нет в контексте

Когда мы создаем scope и передаем ему контекст, выполняется проверка, что этот контекст содержит
Job. И если не содержит, то Job будет создан.

```kotlin
// исходник
public fun CoroutineScope(context: CoroutineContext): CoroutineScope =
    ContextScope(if (context[Job] != null) context else context + Job())
```

## Создание своего элемента для CoroutineContext'а

```kotlin
data class UserData(
    val id: Int,
    val name: String,
    val age: Int
) : AbstractCoroutineContextElement(UserData) {
    companion object Key : CoroutineContext.Key<UserData>
}
```

Помещаем наш объект в `coroutineContext`

```kotlin
val userData = UserData(1, "Name", 27)
val context = Job() + Dispatchers.Default + userData
```

А достать его из `coroutineContext` можно так:

```kotlin
val userData = coroutineContext[UserData]
```

## Передача данных coroutineContext'а при создании корутин

При создании корутины `job` не передается дочерним корутинам. Однако если в билдере не было
`Dispatcher`a, тогда передается `dispatcher` по умолчанию. Если в билдере был `dispatcher` - тогда
он и используется.

также в билдер можно добавить свой элемент `coroutineContext`

```kotlin
val userData = UserData(2, "Malay", 32)
scope.launch {
    Log.d("transferElement", "parent scope = ${this.coroutineContext}")

    launch(userData) {
        Log.d("transferElement", "child scope = ${this.coroutineContext}")
        val user = this.coroutineContext[UserData]
        Log.d("transferElement", "user = $user")
    }
}
```

