package com.example.coroutines.n11_coroutine_dispatcher

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

class N11Dispatchers: ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent{
//            defaultDispatcher()
//            ioDispatcher()
            executorDispatcher()
        }
    }
}

fun defaultDispatcher() {
    val scope = CoroutineScope(Dispatchers.Default)
    repeat(6) {
        scope.launch {
            log("defaultLOG", "coroutine $it, start.")
            delay(1000)
            log("defaultLOG", "coroutine $it, end.")
        }
    }
}

fun ioDispatcher() {
    val scope = CoroutineScope(Dispatchers.IO)
    repeat(6) {
        scope.launch {
            log("ioLOG", "coroutine $it, start.")
            delay(1000)
            log("ioLOG", "coroutine $it, end.")
        }
    }
}

fun executorDispatcher() {
    val scope = CoroutineScope(
        Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    )

    repeat(6){
        scope.launch {
            log("executorDispatcher", "coroutine $it, start.")
            delay(1000)
            log("executorDispatcher", "coroutine $it, end")
        }
    }
}

private fun log(tag: String, text: String) {
    Log.d(
        tag,
        "$text [${Thread.currentThread().name}]"
    )
}
