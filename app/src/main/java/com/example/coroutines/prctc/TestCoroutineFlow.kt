package com.example.coroutines.prctc

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.coroutines.n3_suspend_fun.download
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext

fun log(msg: String) = println("[${Thread.currentThread()}] $msg")

@OptIn(ExperimentalStdlibApi::class)
fun main() {
//    println("without runBlocking")
//    main1()

    println("with runBlocking")
    main2()
}

@kotlin.ExperimentalStdlibApi
fun main2(): Unit = runBlocking {
    val scope = CoroutineScope(Job() + Dispatchers.IO)

    log("scope, ${contextToString(scope.coroutineContext)}")
    scope.launch {
        log("coroutine level 1, ${contextToString(coroutineContext)}")

        launch(Dispatchers.Default) {
            log("coroutine level 2, ${contextToString(coroutineContext)}")

            launch {
                log("coroutine level 3, ${contextToString(coroutineContext)}")
            }
        }
    }
}

@kotlin.ExperimentalStdlibApi
private fun contextToString(context: CoroutineContext) =
    "Job: ${context[Job]}, Dispatcher: ${context[CoroutineDispatcher.Key]} or ${context[ContinuationInterceptor]}"


@kotlin.ExperimentalStdlibApi
fun main1(): Unit {
    val scope = CoroutineScope(Job() + Dispatchers.IO)

    log("scope, ${contextToString(scope.coroutineContext)}")
    scope.launch {
        log("coroutine level 1, ${contextToString(coroutineContext)}")

        launch(Dispatchers.Default) {
            log("coroutine level 2, ${contextToString(coroutineContext)}")

            launch {
                log("coroutine level 3, ${contextToString(coroutineContext)}")
            }
        }
    }
}
