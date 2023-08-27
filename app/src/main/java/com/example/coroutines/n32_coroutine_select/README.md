# Оператор Select

Несколько параллельных async корутин с запросами - `select` поможет получить результат только той
корутины, которая отработает быстрее остальных.

Пример:

```kotlin
launch {

    val async1 = async { /*...*/ }
    val async2 = async { /*...*/ }

    val result = select<String> {

        async1.onAwait { // это специальная форма вызова await для select
            it
        }

        async2.onAwait {
            it
        }
    }
}
```

* Timeout - Кроме пары onAwait, мы можем поместить в select оператор onTimeout:

```kotlin
onTimeout(5000) {
    "timeout"
}
```

Он сработает, если за указанное время так и не пришло результатов от async корутин.

# Что еще можно вызвать в select
[документация](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.selects/select.html)
[статья](https://startandroid.ru/ru/courses/kotlin/29-course/kotlin/627-urok-32-korutiny-operator-select.html)
