package com.example.coroutines.n7_coroutine_scope

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

const val TAG = "scope"
fun n7coroutineScope() {

    /**
     * Scope объект можно создать самостоятельно.
     * Мы будем определять, когда следует отменять его и все его корутины.
     */
    val scope = CoroutineScope(Job())
    val scope2 = CoroutineScope(Dispatchers.Default)
    val scope1 = CoroutineScope(Job() + Dispatchers.Default)
    val scope3 = CoroutineScope(Job())

    scope.launch {
        Log.d(TAG, "first coroutine")
    }

    scope.launch {
        Log.d(TAG, "second coroutine")
    }

    scope.launch {
        Log.d(TAG, "third coroutine")
    }


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