package com.example.coroutines.n11_coroutine_dispatcher

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class N11Dispatchers : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
//            defaultDispatcher()
//            ioDispatcher()
//            executorDispatcher()
//            MainThread()
            traceTheChain()
            traceTheCainUnconfinedDispatcher()
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

    repeat(6) {
        scope.launch {
            log("executorDispatcher", "coroutine $it, start.")
            delay(1000)
            log("executorDispatcher", "coroutine $it, end")
        }
    }
}

fun log(tag: String, text: String) {
    Log.d(
        tag,
        "$text [${Thread.currentThread().name}]"
    )
}


/**
 * Код внутри launch выполнится в Main потоке. Когда мы вызываем метод getData, поток не будет заблокирован. Но и код не пойдет дальше, пока данные не будут получены. Suspend функция приостанавливает выполнение кода, не блокируя поток. Когда она сделает свою работу, выполнение кода возобновится. Полученные данные мы передадим в updateUI.
 *
 * Основной смысл в том, что в корутине, которая выполняется в Main потоке, мы можем спокойно писать обычный (типичный для Activity или фрагментов) код, который работает с UI. Но при этом мы можем в этом же коде вызывать suspend функции, которые будут асинхронно получать данные с сервера или БД. И нам не нужны будут колбэки и переключения потоков. Все это скрыто под капотом корутин, а наш код выглядит чище и лаконичнее.
 */
@SuppressLint("CoroutineCreationDuringComposition")
@Composable
fun MainThread() {

    val context = LocalContext.current
    val text = remember {
        mutableStateOf("Null")
    }
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = text.value, style = MaterialTheme.typography.titleLarge)
        Button(onClick = {
            Toast.makeText(context, "видишь, поток не блокируется", Toast.LENGTH_LONG).show()
        }) {
            Text(text = "Button")
        }
    }
    val scope = CoroutineScope(Dispatchers.Main)
    scope.launch {
        val data = getData()
        text.value = data
    }
}

suspend fun getData(): String {
    return suspendCoroutine {
        thread {
            TimeUnit.MILLISECONDS.sleep(5000)
            it.resume("Data")
        }
    }
}
