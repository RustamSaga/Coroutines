package com.example.coroutines.n7_coroutine_scope

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 *  Scope - объект без которого корутину не запустить.
 *  scope - это родитель для всех корутин. Когда мы отменяем scope,
 *          мы отменяем все его дочерние корутины.
 *
 *  Scope нужен для создания класса Job(который будет родителем), чтобы создать корутину.
 *          Он (в своем контексте) содержит Job. Этот Job будет являться родителем
 *          для Job-ов корутин, которые мы создаем, вызывая scope.launch.
 */

const val TAG = "scope"
fun n1coroutineScope() {
    /**
     * Scope объект можно создать самостоятельно.
     * Мы будем определять, когда следует отменять его и все его корутины.
     */

    val scope = CoroutineScope(Job())

    scope.launch {
        Log.d(TAG, "first coroutine")
    }

    scope.launch {
        Log.d(TAG, "second coroutine")
    }

    scope.launch {
        Log.d(TAG, "third coroutine")
    }

    /**
        Эти корутины только выводят лог и сами завершаются очень быстро.
        Но если бы там выполнялась долгая работа, то scope помог бы нам ограничить
        время жизни этих корутин. Например, мы решаем, что при закрытии экрана все
        корутины должны отмениться. Для этого помещаем вызов scope.cancel в метод Activity.onDestroy
            override fun onDestroy() {
                super.onDestroy()
                scope.cancel()
            }
        При вызове scope.cansel() отменяются все подписанные на него Job-ы корутин.
        Соответственно, если не вызвать scope.cancel, то корутины этого scope не отменятся.
        Поэтому не теряйте этот вызов, если вам необходимо отменять корутины.
     */

    scope.launch {
        // parent coroutine code block
        this.launch {
            // child coroutine code block
        }
    }
    // or
    scope.launch {
        // parent coroutine code block
        launch {
            // child coroutine code block
        }
    }


    /**
     * Job наследует интерфейс CoroutineScope и хранит ссылку на Context.
     * Т.к. Context должен содержать Job, то Job просто помещает в Context ссылку на себя.
     * Получается, что Job является scope, и сам же выступает в качестве Job этого scope
     */

//    Чтобы это проверить, вот пример:
    val job = scope.launch {
        Log.d(TAG, "scope = $this")
    }
    Thread.sleep(1000)
    Log.d(TAG, "job = $job")

}