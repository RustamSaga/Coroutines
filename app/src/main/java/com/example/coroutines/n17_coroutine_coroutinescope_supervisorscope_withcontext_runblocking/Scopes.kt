package com.example.coroutines.n17_coroutine_coroutinescope_supervisorscope_withcontext_runblocking

import com.example.coroutines.n11_coroutine_dispatcher.log
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

suspend fun myScope() {
    val handler = CoroutineExceptionHandler { context, exception ->
        log("Handler", "handler, $exception")
    }
    supervisorScope {
        launch(handler) {

        }
    }
}