# Listener to suspend function

Мы меняем текст у кнопки и сразу после этого хотим узнать ее новую ширину.

```kotlin
btn.text = "New text"
log("new width is ${btn.width}")
```

Этот код не сработает как ожидалось. Мы получим старую ширину кнопки. Потому что setText не
выполнится сразу в момент вызова кода. Реальное обновление кнопки будет помещено в очередь и
произойдет чуть позже.

Создаем listener и вешаем его на кнопку. И выносим в отдельную suspend fun.

```kotlin
suspend fun View.awaitLayoutChange() = suspendCancellableCoroutine<Unit> { cont ->
    val listener = object : View.OnLayoutChangeListener {
        override fun onLayoutChange(
            view: View?,
            left: Int,
            top: Int,
            right: Int,
            bottom: Int,
            oldLeft: Int,
            oldTop: Int,
            oldRight: Int,
            oldBottom: Int
        ) {
            view?.removeOnLayoutChangeListener(this)
            cont.resume(Unit)
        }
    }
    addOnLayoutChangeListener(listener)
    cont.invokeOnCancellation { removeOnLayoutChangeListener(listener) }
}
```

Код ui

```kotlin
lifecycleScope.launch {
    btn.text = "New text"
    log("old width is ${btn.width}")
    btn.awaitLayoutChange() // приостановит код корутины, пока кнопка не будет реально обновлена.
    log("new width is ${btn.width}")
}
```

Аналогично можно обернуть listener анимации, чтобы можно было запускать анимацию и дожидаться ее
окончания:

```kotlin
lifecycleScope.launch {
    val animator = ObjectAnimator.ofFloat(imageView, View.ALPHA, 0f, 1f)
    animator.start()
    animator.awaitEnd()
}
```

`awaitEnd` - suspend функция, которая приостановит корутину, пока анимация не завершится. Если у нас
есть несколько анимаций, то мы сможем запускать их параллельно, последовательно, с задержкой, с
повторами и т.д. В этом помогут корутины и подобные suspend функции. Более подробно об этом можно
прочесть в [статье](https://chrisbanes.me/posts/suspending-views/). Если вас интересуют более
сложные примеры с анимацией и RecyclerView, то посмотрите
эту [статью](https://chrisbanes.me/posts/suspending-views-example/).
Там используется та же схема, что и в простом примере, который мы рассмотрели.

# Listener to Flow

```kotlin
val flow = callbackFlow {
    btn.setOnClickListener {
        trySend(Unit)
    }
    awaitClose { btn.setOnClickListener(null) }
}
```

Еще один распространенный пример, это отслеживание изменений в EditText:

```kotlin
val flow = callbackFlow<String> {
    val textWatcher = object : TextWatcher {
        override fun afterTextChanged(editable: Editable?) {
            editable?.toString()?.let { trySend(it) }
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    }

    editText.addTextChangedListener(textWatcher)
    awaitClose { editText.removeTextChangedListener(textWatcher) }
}
```

Схема та же, просто другой listener, который мониторит EditText и отправляет новые значения
получателю.

# Listener to Flow in ViewModel

Чтобы не создавать Flow на стороне View, мы можем создать его в модели.

```kotlin
val _searchQuery = MutableStateFlow("")

fun search(query: String) {
    _searchQuery.value = query
}
```

В View мы вешаем listener на EditText и в нем просто вызываем метод модели search и передаем туда
данные:

```kotlin
editText.addTextChangedListener { viewModel.search(it.toString()) }
```

Мы можем добавить к этому Flow операторов и попросить при каждом новом значении выполнять поиск,
например, с помощью UseCase:

```kotlin
val searchResultFlow = _searchQuery.asStateFlow()
    .debounce(500)
    .filter { it.length > 3 }
    .mapLatest { query ->
        searchUseCase.execute(query)
    }
```
