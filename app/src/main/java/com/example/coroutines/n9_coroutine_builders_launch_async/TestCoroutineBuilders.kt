package com.example.coroutines.n9_coroutine_builders_launch_async

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val formatter = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
class TestCoroutineBuilders : ComponentActivity() {

    private val scopeNormal = CoroutineScope(Job())
    private val scopeJoin = CoroutineScope(Job())
    private val scopeLazy = CoroutineScope(Job())
    private val scopeAsync = CoroutineScope(Job())
    private val scopeParallel = CoroutineScope(Job())
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {

                RunNormalCoroutine(scope = scopeNormal)

                Spacer(modifier = Modifier.height(100.dp))

                RunJoinCoroutine(scope = scopeJoin)

                Spacer(modifier = Modifier.height(100.dp))

                RunLazyCoroutine(scope = scopeLazy)

                Spacer(modifier = Modifier.height(100.dp))

                RunAsyncCoroutine(scope = scopeAsync)

                Spacer(modifier = Modifier.height(100.dp))

                RunAsyncCoroutineParallelization(scope = scopeParallel)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        log("normal", "onDestroy")
        scopeNormal.cancel()
    }
}


@Composable
fun RunNormalCoroutine(scope: CoroutineScope) {
    val tag = "normal"
    Button(onClick = {
        /*
        Хоть родительская корутина и выполнила сразу же весь свой код,
          но ее статус поменяется на Завершена только когда выполнится дочерняя корутина.
          Потому что родительская корутина подписывается на дочерние и ждет их завершения,
          прежде чем официально завершиться. Т.е. то, что корутина выполнила свой код,
           может и не означать, что она имеет статус Завершена.
         */
        scope.launch {
            log(tag, "launch coroutine, start")

            launch {
                log(tag, "child coroutine, start")
                delay(1000)
                log(tag, "child coroutine, end")
            }

            log(tag, "parent coroutine, end")
        }
    }) {
        Text(text = "RUN NORMAL")
    }
}

@Composable
fun RunJoinCoroutine(scope: CoroutineScope) {
    val tag = "join"

    Button(onClick = {
        /*
         Что бы приостоновит выполнение родительской корутины, нужно использовать метод join()
         */
        scope.launch {
            log(tag, "method - launch coroutine, start")

            val job = launch {
                log(tag, "method - child coroutine, start")
                delay(3000)
                log(tag, "method - child coroutine, end")
            }
            val job2 = launch {
                log(tag, "method - child coroutine, start")
                delay(2000)
                log(tag, "method - child coroutine, end")
            }
            log(tag, "parent coroutine, wait until child completes")
            job.join()
            job2.join()

            log(tag, "method - parent coroutine, end")
        }
    }) {
        Text(text = "JOIN")
    }
}

@Composable
fun RunLazyCoroutine(scope: CoroutineScope){

    val tag = "lazy"
    lateinit var job: Job

    Button(onClick = {
        log(tag, "COROUTINE IS CREATED")
        job = scope.launch(start = CoroutineStart.LAZY) {
            log(tag, "COROUTINE IS STARTED")
            delay(2000)
            log(tag, "COROUTINE, END")
        }

        log(tag, "COROUTINE, END")

    }) {
        Text(text = "CREATE LAZY")
    }
    Button(onClick = {
        log(tag, "COROUTINE, RUN BUTTON")
        job.start()
        log(tag, "COROUTINE, END (RUN BUTTON)")

    }) {
        Text(text = "START LAZY")
    }

}


@Composable
fun RunAsyncCoroutine(scope: CoroutineScope){

    val tag = "async"
    Button(onClick = {
        scope.launch {
            log(tag, "COROUTINE, RUN")

            val deferred = async {
                log(tag, "COROUTINE IS STARTED")
                delay(2000)
                log(tag, "COROUTINE, END")

                "Async result"
            }

            log(tag, "parent coroutine, wait until child returns result")
            val result = deferred.await() // приостанавливает код и выдает результат
            log(tag, "parent coroutine, child returns: $result")

            log(tag, "parent coroutine, end")

        }

    }) {
        Text(text = "RUN ASYNC")
    }
}


@Composable
fun RunAsyncCoroutineParallelization(scope: CoroutineScope){

    val tag = "parallel"
    Button(onClick = {
        scope.launch {
            log(tag, "COROUTINE, RUN")

            // время суммируется = 1500+3500 = 5000 (5 сек на время выполнения)
//            val data1 = getData(1500, "data1")
//            val data2 = getData(3500, "data2")

            // выполение праллельноб, общее время = 3500 (3,5 сек)
            val data1 = async { getData(1500, "data1") }
            val data2 = async { getData(3500, "data2") }

            log(tag, "parent coroutine, wait until child returns result")
            val result = "${data1.await()}, ${data2.await()}"
            log(tag, "parent coroutine, child returns: $result")

            log(tag, "parent coroutine, end")

        }

    }) {
        Text(text = "RUN PARALLEL")
    }
}

private suspend fun getData(delay: Long, result: String): String {
    delay(delay)
    return result
}

private fun log(tag: String, text: String) {
    Log.d(
        tag,
        "${formatter.format(Date())} $text [${Thread.currentThread().name}]"
    )
}

/**
 * А теперь снова представьте, что родительская корутина выполняется в main потоке.
 * А suspend-функции - это получение данных с сервера с помощью, например, Retrofit.
 * Вы выполняете запросы в фоновых потоках, получаете данные в main потоке и отображаете
 * их не в логе, а на экране. И никаких колбэков. В этом и есть одна из основных возможностей
 * корутин - лаконичный асинхронный код.
 */