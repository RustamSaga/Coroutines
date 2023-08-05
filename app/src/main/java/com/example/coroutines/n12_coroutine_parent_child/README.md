
# Связь между родительской и дочерней корутиной

1. создание context'а дочерней корутины
2. создание job'а дочерней корутины
3. создание связи с родительской корутиной
4. запуск дочерней корутины (создание Continuation и его отправка в диспетчер)

### 1. Создание context'а дочерней корутины

    У нас есть родительская корутина у которой есть context - назовем `parentContex`
    Также есть launch, в нее тоже передается контекст - назавем `launchContext`
    Из этих двух создается дочерний контекст - `childContext`

Шаги:

1. К `parentContext` добавляется `launchContext`, если в `launchContext` был `dispatcher` то он же
   остается. Получившийся контекст назовем `newContext`.

2. Если в `newContext` нет диспетчера, то в него добавляется диспетчер по умолчанию.
3. Создается `job` (можно назвать также `CoroutineScope`) дочерней корутины. Контекст `newContext`
   передается в его конструктор
4. `Job` добавляет к `newContext` сам себя (`this`), получая тем самым новый контекст, который и
   является итоговым `childContext`. Контекст `newContext` при этом никуда из джоба не делся.

В итоге, в `job`е дочерней корутины роль основного контекста играет `childContext`. Но кроме него,
там хранится и `newContext`. Он содержит `Job` родителя, который **понадобится** при создании связи
с родительской корутиной.

### 2. Создание job'а дочерней корутины

### 3. Создание связи с родительской корутиной

Как было написано выше в `childJob : CoroutineScope` хранятся `childContext` и `newContext`
`childJob` вызывает метод `attachChild(childJob)` из `parentJob` который храниться в `newContext`
`parenJob` создает объект `ChildHandleNode` который хранит у себя и передает `childJob`у

Код не настоящий, но дает представление.

```kotlin
class ChildJob : CoroutineScope {
    private val newContext: CoroutineContext
    private val childContext: CoroutineContext

    //    Для дочерней он нужен, чтобы разорвать связь, когда работа завершена
    val childHindleNode = newContext.parentJob.attachChild(this)
}

class ParentJob {
    //    Для родителя это способ узнать, что есть незавершенные дочерние корутины. 
    var childHandleNode: ChildHandleNode? = null
    suspend fun attachChild(childJob: Job): ChildHandleNode {
        childHandleNode = ChildHandleNode(childJob)
        return childHandleNode
    }
}
```

            Как ведет себя родительская корутина

1. Если завершается позже дочерней корутины, используя `launch`
   ![](https://github.com/RustamSaga/Coroutines/blob/master/imageres/first%20.jpeg)
2. Если завершается раньше дочерней корутины, используя `launch`
   ![](https://github.com/RustamSaga/Coroutines/blob/master/imageres/second.jpeg)
3. Если у радительской более одной дочерней корутины, используя `launch`
    * Оно представляет собой смесь первого и второго сценария.
    * Каждая корутина создает свою отдельную связь с родительской `ChildHandleNode`, и обе начинают
      выполнять свой код.

4.

### 4. Запуск дочерней корутины
