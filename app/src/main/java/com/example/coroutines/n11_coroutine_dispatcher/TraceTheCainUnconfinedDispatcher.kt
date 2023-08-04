package com.example.coroutines.n11_coroutine_dispatcher

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

fun traceTheCainUnconfinedDispatcher() {
    val scope = CoroutineScope(Dispatchers.Unconfined)

    scope.launch {
        // coroutine начнет выполнение в Main потоке, т.к был вызван в Activity
        log("tracUnconfined", "start coroutine")
        val info = getInfo("tracUnconfined") // в этом коде выделяется другой поток (смотри код), и продолжает выполняться в нем
        // при окончании выполнения он не будет переключаться в другой поток, а продолжет выполняться в этом же.
        log("tracUnconfined", "end coroutine")
    }
}