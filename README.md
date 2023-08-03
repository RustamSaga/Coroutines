# Coroutine

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