package com.example.coroutines.n13_coroutine_exception_handling

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import com.example.coroutines.n11_coroutine_dispatcher.log
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class CoroutineExceptionN13 : ComponentActivity() {
    private val crash = "onRunCrash"
    private val success = "onRunSuccess"
    private val successByHandler = "onRunHandler"
    private val successBySupervisorJob = "onRunBySupervisorJob"

    private val scope = CoroutineScope(Job() + Dispatchers.Default)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Surface(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.wrapContentSize()){
                    Button(onClick = {
                        // не заработает
//                    onRunCrash()
//                    onRunCrash2()

                        // заработает но отменит все дочерние корутины
//                    onRun()
//                    onRunByCoroutineExceptionHandler()
//                    onRunCancelAll()

                        // заработает и не отменит дочерние корутины
                        onRunWithSupervisorJob()
                    }) {
                        Text(text = "RUN")
                    }
                }

            }
        }
    }

    private fun onRun() {
        log(success, "onRun, start")
        // обернуть ошибку
        scope.launch {
            try {
                Integer.parseInt("a")
            } catch (e: Exception) {
                log(success, "error $e")
            }
            log(success, "onRun, end")
        }
    }

    private fun onRunByCoroutineExceptionHandler() {
        val handler = CoroutineExceptionHandler { context, exception ->
            log(successByHandler, "handler, $exception")
        }
        scope.launch(handler) {
            Integer.parseInt("a")
        }
    }

    // не заработает
    private fun onRunCrash() {
        log(crash, "onRun, start")
        try {
            scope.launch {
                Integer.parseInt("a")
            }
        } catch (e: Exception) {
            log(crash, "error $e")
        }
        log(crash, "onRun, end")
    }

    // это метод использует билдер launch, чтобы создать и запустить корутину, и сам после этого
// сразу завершается. А корутина живет своей жизнью в отдельном потоке.
// Вот именно поэтому try-catch здесь и не срабатывает.
    // такой же принцип
    private fun onRunCrash2() {
        log(crash, "onRun, start")
        try {
            thread {
                Integer.parseInt("a")
            }
        } catch (e: Exception) {
            log(crash, "error $e")
        }
        log(crash, "onRun, end")

    }


    private fun onRunCancelAll() {
        val handler = CoroutineExceptionHandler { context, exception ->
            log(successByHandler, "first coroutin exception $exception")
        }

        scope.launch(handler) {
            TimeUnit.MILLISECONDS.sleep(1000)
            Integer.parseInt("a")
        }
        scope.launch {
            repeat(5) {
                TimeUnit.MILLISECONDS.sleep(300)
                log(successByHandler, "second coroutine isActive $isActive")
            }
        }
    }

    private fun onRunWithSupervisorJob() {
        val handler = CoroutineExceptionHandler { context, exception ->
            log(successBySupervisorJob, "first coroutin exception $exception")
        }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default + handler)

        scope.launch {
            TimeUnit.MILLISECONDS.sleep(1000)
            Integer.parseInt("a")
        }

        scope.launch {
            repeat(5) {
                TimeUnit.MILLISECONDS.sleep(300)
                log(successBySupervisorJob, "second coroutine isActive $isActive")
            }
        }
    }
}