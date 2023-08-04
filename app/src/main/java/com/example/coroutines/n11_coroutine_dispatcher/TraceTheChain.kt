package com.example.coroutines.n11_coroutine_dispatcher

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

fun traceTheChain() {

    val scope = CoroutineScope(Dispatchers.Default)

    scope.launch {
        //Билдер создает dispatchedContinuation и вызывает его метод resume. Диспетчер находит
        // свободный поток (DefaultDispatcher-worker-N) и отправляет туда Continuation на выполнение
        log("tracerTheChain", "start coroutine")

        // Внутри этой функции есть thread -  создает отдельный поток (Thread-N) и уходит туда
        val info = getInfo("tracerTheChain")
        // окончив работу suspend fun вызывает во второй раз continuation.resume
        // Диспетчер отправил его в поток DefaultDispatcher-worker-N. В этом потоке выполнилась оставшаяся часть кода.
        log("tracerTheChain", "end coroutine")

        // Поэтому поток начала работы корутины не совпадает с потоком окончания.
    }

}

suspend fun getInfo(tag: String): String {
    return suspendCoroutine {
        log(tag, "suspend fun, start")
        thread { // выделяется отдульный поток.
            log(tag, "suspend fun, background work")
            TimeUnit.MILLISECONDS.sleep(3000)
            it.resume("Info!")
            //чтобы возобновить выполнение корутины. Диспетчер находит свободный поток (DefaultDispatcher-worker-N) и
        // отправляет туда Continuation на продолжение выполнения.
        }
    }
}
